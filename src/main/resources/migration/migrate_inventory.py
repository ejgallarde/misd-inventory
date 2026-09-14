#!/usr/bin/env python3
"""
migrate_inventory.py
=====================
Migrates "Inventory ICT_PPE CY 2022-2024 duplicate.xlsx" (19 region/agency tabs)
into the dar_inventory MySQL schema, AFTER dar_inventory_schema_updates.sql has
been applied.

SCOPE / WHAT THIS SCRIPT DOES NOT TOUCH
----------------------------------------
Only rows that do NOT require a human decision are migrated. Three rows are
skipped outright (logged to skipped_rows.csv):
  - CAR row 236, CAR row 237   (no Equipment description at all - can't classify)
  - R-III row 242              (two equipment names merged into one cell/cost -
                                 "PLOTTER (3IN1) / HP Designjet T908")
Everything else in the two review workbooks (CAR_Migration_Review_Flags.xlsx,
Inventory_Review_Flags_AllRegions.xlsx) is a flag for LATER correction, not a
migration blocker, and is handled here with a documented, conservative default:

  - Strikethrough cells (9 CAR rows): the struck field is never used to assign
    CurrentOwnerID. Where the OTHER (non-struck) holder field on the same row
    is usable, that value is used instead - this is not a guess about which
    name is "current", it only avoids the field that was flagged unreliable.
    Where BOTH holder fields on a row are struck (CAR rows 37 and 43),
    CurrentOwnerID is left NULL and both raw values are kept in Remarks.
  - DARCO's composite Plate/Body/Chassis field (26 rows): stored as-is, unsplit,
    in fleetvehicles.PlateNumber. No segment is guessed to be the "real" plate.
  - DARCO's informal Remarks nicknames (14 rows): stored as-is in Remarks.
  - Bundled accessories (printer/UPS/monitor/scanner/keyboard/mouse/AVR named in
    Remarks, ~280 rows nationwide): each becomes its own `assets` row linked via
    BundledWithAssetTag, per your earlier decision. The accessory's owner is
    always inherited from the parent asset (never reassigned to a "C/O <name>"
    mentioned in the remark) - the named person is preserved in the accessory's
    Remarks instead of being used to override CurrentOwnerID.
  - Duplicate PROPERTY NUMBER values: migrated as-is. PropertyNumber is a plain
    reference column, never a unique key, so duplicates cannot violate anything.
  - New equipment models (LAPTOP MSI KATANA, GNSS RTK, HP Designjet T908 [only
    where it's the sole item on its row], HP PRINTER, SERVER, etc.): mapped to
    catalog entries below using the same "Unspecified manufacturer" convention
    already used for CAR's ambiguous models.
  - Province/Office casing (e.g. "KALINGA" vs "Kalinga"): normalized to Title
    Case automatically - deterministic, not a judgment call.
  - PAR/PTR/ICS No. anomalies: Excel-auto-converted dates are rendered back to
    plain date strings; multi-document cells (e.g. "PTR:...\nPAR:...") are
    stored in full, newline replaced with " / ", in assetassignments.DocumentNo.

Personnel: nobody in the source file has an EmployeeID (personnel's primary
key), per the decision already made for CAR. A synthetic ID is generated for
every distinct cleaned name across ALL 19 tabs (format PERS-00001, ...),
deduplicated case-insensitively. A cell naming two people (e.g. "Engrs. Molines
Ewis/Antonio Martin") gets its first name as the row's primary
custodian/personnel link; the remaining name(s) are preserved in the asset's
Remarks as "Also associated: ...".

USAGE
-----
Dry run (default) - validates everything and writes a reviewable .sql file,
touches no database:
    python3 migrate_inventory.py --excel "Inventory_ICT_PPE.xlsx"

Execute against a real MySQL server (requires `pip install pymysql`):
    python3 migrate_inventory.py --excel "Inventory_ICT_PPE.xlsx" --execute \
        --host localhost --user root --password *** --database dar_inventory

Either mode also writes:
    migration_log.txt          - per-table row counts, warnings
    skipped_rows.csv            - the 3 rows excluded, and why
    accessories_created.csv     - every bundled accessory row generated, for spot-checking
    migration_flags_report.csv  - rows given a sentinel owner (PERS-00000, no resolvable
                                   owner in source) or a disambiguated duplicate SerialNumber/
                                   PlateNumber, for later manual review/reassignment
"""
import argparse
import csv
import datetime
import re
import sys
from collections import OrderedDict, defaultdict

try:
    import openpyxl
except ImportError:
    sys.exit("openpyxl is required: pip install openpyxl")

# ======================================================================
# Constants
# ======================================================================

REGION_ORDER = ['DARCO', 'CAR', 'R-I', 'R-II', 'R-III', 'R-IVA', 'R-IVB', 'R-V', 'R-VI', 'NIR',
                'R-VII', 'R-VIII', 'R-IX', 'R-X', 'R-XI', 'R-XII', 'CARAGA', 'DENR', 'LRA']

EXCLUDE_ROWS = {
    ('CAR', 236): "No Equipment description, Region, or Province at all - cannot classify. See Inventory_Review_Flags_AllRegions.xlsx > Unclassified_Rows.",
    ('CAR', 237): "No Equipment description, Region, or Province at all - cannot classify. See Inventory_Review_Flags_AllRegions.xlsx > Unclassified_Rows.",
    ('R-III', 242): "Two equipment names merged into one cell/cost ('PLOTTER (3IN1) / HP Designjet T908') - needs manual split into two line items. See report Section 3.6.",
}

# canonical column order (1-indexed, columns A..N in every sheet)
COL_REGION, COL_PROVINCE, COL_EQUIPMENT, COL_UNITCOST, COL_DEPR, COL_PROPNUM, COL_SERIAL, \
    COL_ISSUEDTO, COL_DESIGNATION, COL_PARPTR, COL_ENDUSER, COL_OFFICE, COL_STATUS, COL_REMARKS = range(1, 15)

# Equipment classification + catalog mapping.
# key: normalized (whitespace-collapsed, upper-cased) equipment text -> (target, Category, Manufacturer, ModelName)
FLEET, SURVEY, GENERAL = 'FLEET', 'SURVEY', 'GENERAL'

EQUIPMENT_MAP = {
    # Category aligned to the pre-existing equipmentcatalog seed row ('Desktop', not
    # 'Desktop Computer') so this reuses that CatalogID instead of forking a duplicate.
    'DESKTOP COMPUTER – (LENOVO M70T)': (GENERAL, 'Desktop', 'Lenovo', 'M70T'),
    'MOTORCYCLE – (HONDA XRL 150)': (FLEET, 'Motorcycle', 'Honda', 'XRL 150'),
    'DESKTOP COMPUTER – (MSI PRO DP180)': (GENERAL, 'Desktop Computer', 'MSI', 'PRO DP180'),
    'DESKTOP COMPUTER – (HP PRO SFF 280G9)': (GENERAL, 'Desktop', 'HP', 'Pro SFF 280G9'),
    # ModelName aligned to the pre-existing catalog row ('Extensa 15 (EX215-55)').
    'LAPTOP – (ACER EXTENSA EX215-55G I7)': (GENERAL, 'Laptop', 'Acer', 'Extensa 15 (EX215-55)'),
    'ACER TRAVELMATE': (GENERAL, 'Laptop', 'Acer', 'TravelMate'),
    'DOUBLE CABIN PICK-UP VEHICLES – MITSUBISHI STRADA': (FLEET, 'Double Cabin Pick-up', 'Mitsubishi', 'Strada'),
    'HANDHELD GPS – MODEL P6': (SURVEY, 'Handheld GPS', 'Unspecified', 'Model P6'),
    'A3 PRINTER (BROTHER)': (GENERAL, 'Printer', 'Brother', 'A3 Printer'),
    'PHOTOCOPIER MACHINE – KYOCERA': (GENERAL, 'Photocopier', 'Kyocera', 'Photocopier Machine'),
    'LAPTOP MSI KATANA': (GENERAL, 'Laptop', 'MSI', 'Katana'),
    'REAL-TIME KINEMATIC – GEOMAX ZENITH60': (SURVEY, 'RTK', 'GeoMax', 'Zenith60'),
    'TOTAL STATION – GEOMAX TOTAL STATION ZOOM 50': (SURVEY, 'Total Station', 'GeoMax', 'Zoom 50'),
    'PLOTTER (3IN1)': (GENERAL, 'Plotter', 'Unspecified', 'Plotter (3-in-1)'),
    'GLOBAL NAVIGATION SATELLITE SYSTEM REAL TIME KINEMATIC': (SURVEY, 'RTK', 'Unspecified', 'GNSS-RTK (Global Navigation Satellite System Real Time Kinematic)'),
    'HP PRINTER': (GENERAL, 'Printer', 'HP', 'Unspecified'),
    'SERVER': (GENERAL, 'Server', 'Unspecified', 'Unspecified'),
}
# Hilux Tamaraw appears with a trailing batch suffix ("-24", "-25", ...). The
# pre-existing fleetvehiclecatalog seed row keeps this suffix as part of the
# ModelName ('...MT-UVR-24'), so it is preserved (not stripped) below to reuse
# that CatalogID for -24 rows and to keep any other suffix as its own entry.
HILUX_RE = re.compile(r'^HILUX TAMARAW 2\.4 UTILITY VAN DSL MT-UVR(\s*-?\d*)$')

ACCESSORY_CATALOG = {
    'PRINTER': ('Printer', 'Unspecified', 'Bundled Printer (accessory - see assets.BundledWithAssetTag)'),
    'UPS': ('UPS', 'Unspecified', 'Bundled UPS (accessory - see assets.BundledWithAssetTag)'),
    'MONITOR': ('Monitor', 'Unspecified', 'Bundled Monitor (accessory - see assets.BundledWithAssetTag)'),
    'SCANNER': ('Scanner', 'Unspecified', 'Bundled Scanner (accessory - see assets.BundledWithAssetTag)'),
    'KEYBOARD': ('Keyboard', 'Unspecified', 'Bundled Keyboard (accessory - see assets.BundledWithAssetTag)'),
    'MOUSE': ('Mouse', 'Unspecified', 'Bundled Mouse (accessory - see assets.BundledWithAssetTag)'),
    'AVR': ('AVR', 'Unspecified', 'Bundled AVR (accessory - see assets.BundledWithAssetTag)'),
}
# NOTE: "CPU" is deliberately excluded from accessory creation - in this file it
# almost always refers to the desktop's own tower/case (e.g. "CPU (Hang)"), not
# a separate bundled item. CPU-mentioning remarks stay as plain text on the
# parent row.
ACCESSORY_KEYWORDS = list(ACCESSORY_CATALOG.keys())
DEFECT_WORDS = ['UNSERVICEABLE', 'DEFECTIVE', 'FOR REPAIR', 'HANG', 'NOT WORKING', 'BROKEN']
PROPNUM_RE = re.compile(r'\b\d{4}-\d{2}-\d{2}-[A-Z0-9\-]+\b')
CAREOF_RE = re.compile(r'(?:C/O|c/o)\s*([A-Za-z .,\'\-]+)|(?:User[:\s]+)\n?\s*([A-Za-z .,\'\-]+)', re.IGNORECASE)

PLACEHOLDER_SERIAL_RE = re.compile(r'no\s*plate\s*available|^n/?a$|^none$|^not\s*available$|^no\s*serial', re.IGNORECASE)

# Sentinel personnel record used when a row has a document number (PAR/PTR) but
# no resolvable owner, so assetassignments.EmployeeID (NOT NULL) is never left
# unset. Rows using it are marked with a [MIGRATION FLAG: ...] note and logged
# to migration_flags_report.csv for later manual review/reassignment.
SENTINEL_EMPLOYEE_ID = 'PERS-00000'
FLAG_UNRESOLVED_OWNER = '[MIGRATION FLAG: UNRESOLVED OWNER]'
FLAG_DUPLICATE_IDENTIFIER = '[MIGRATION FLAG: DUPLICATE IDENTIFIER]'


# ======================================================================
# Helpers
# ======================================================================

def norm(s):
    if s is None:
        return None
    return re.sub(r'\s+', ' ', str(s)).strip()


def norm_upper(s):
    n = norm(s)
    return n.upper() if n else n


def parse_cost(v):
    if v is None or v == '':
        return None
    if isinstance(v, (int, float)):
        return float(v)
    s = str(v).replace('₱', '').replace(',', '').strip()
    if s in ('-', ''):
        return 0.0
    try:
        return float(s)
    except ValueError:
        return None


def parse_par_ptr(v):
    """Return a clean string for assetassignments.DocumentNo, or None."""
    if v is None or v == '':
        return None
    if isinstance(v, datetime.datetime):
        return v.strftime('%Y-%m-%d')
    s = norm(str(v))
    s = s.replace('\n', ' / ')
    return s[:100] if s else None


def title_case_place(s):
    """Deterministic canonicalization for PROVINCE/OFFICE casing variants."""
    if not s:
        return s
    s = norm(s)
    # keep common all-caps admin abbreviations as-is (DARRO, DARPO, STOD, etc.)
    if re.fullmatch(r'[A-Z0-9\'\.\-/ ]+', s) and any(tok.isupper() and len(tok) >= 3 for tok in s.split()):
        small_words = {'of', 'the', 'and', 'de', 'del', 'la'}
        words = s.split(' ')
        out = []
        for w in words:
            wl = w.lower()
            if wl in small_words:
                out.append(wl)
            elif re.fullmatch(r"(mc|dar|os|lto|lgu|denr|lra|dar[a-z]*)", wl, re.IGNORECASE) and len(w) <= 6:
                out.append(w.upper())
            else:
                out.append(w[:1].upper() + w[1:].lower() if w else w)
        return ' '.join(out)
    return s


def clean_serial(v):
    if v is None or v == '':
        return None
    s = norm(str(v))
    if not s or PLACEHOLDER_SERIAL_RE.search(s):
        return None
    return s


def split_names(raw):
    """Split a cell that may hold multiple people separated by / or ' and '."""
    if not raw:
        return []
    s = norm(raw)
    parts = re.split(r'\s*/\s*|\n', s)
    out = []
    for p in parts:
        p = p.strip(' ()')
        if p:
            out.append(p)
    return out if out else [s]


def sql_str(v):
    if v is None:
        return 'NULL'
    s = str(v).replace('\\', '\\\\').replace("'", "\\'")
    return f"'{s}'"


def sql_num(v):
    return 'NULL' if v is None else repr(v)


def classify_equipment(raw):
    if not raw:
        return None
    key = norm_upper(raw)
    key = key.replace('\n', ' ')
    key = re.sub(r'\s+', ' ', key).strip()
    if key in EQUIPMENT_MAP:
        return EQUIPMENT_MAP[key]
    hilux_match = HILUX_RE.match(key)
    if hilux_match:
        digits = re.sub(r'\D', '', hilux_match.group(1) or '')
        model = 'Hilux Tamaraw 2.4 UTILITY VAN DSL MT-UVR' + (f'-{digits}' if digits else '')
        return (FLEET, 'Utility Van', 'Toyota', model)
    return None  # unrecognized model - handled by caller (logged, defaults to GENERAL/Other)


# ======================================================================
# Extraction
# ======================================================================

def sheet_last_data_row(ws_v, max_row):
    """Same footnote-aware row-bound detection used in the analysis phase."""
    last_data_row = 2
    blank_streak = 0
    footnote_rows = set()
    for r in range(3, max_row + 1):
        rowvals = [ws_v.cell(row=r, column=c).value for c in range(1, 15)]
        all_blank = all(v in (None, '') for v in rowvals)
        if all_blank:
            blank_streak += 1
            if blank_streak >= 3:
                break
            continue
        blank_streak = 0
        nonblank_cols = [i + 1 for i, v in enumerate(rowvals) if v not in (None, '')]
        a_val = rowvals[0]
        if nonblank_cols == [1] and isinstance(a_val, str) and len(a_val) > 15:
            footnote_rows.add(r)
            continue
        last_data_row = r
    return last_data_row, footnote_rows


def extract_rows(wb_v, wb_f):
    """Yield dicts for every real data row across all 19 sheets, in REGION_ORDER."""
    for sn in REGION_ORDER:
        ws_v = wb_v[sn]
        ws_f = wb_f[sn]
        last_row, footnotes = sheet_last_data_row(ws_v, ws_v.max_row)
        for r in range(3, last_row + 1):
            if r in footnotes:
                continue
            rowvals = [ws_v.cell(row=r, column=c).value for c in range(1, 15)]
            if all(v in (None, '') for v in rowvals):
                continue
            struck_fields = set()
            for c in range(1, 15):
                cell = ws_f.cell(row=r, column=c)
                if cell.font and cell.font.strike:
                    struck_fields.add(c)
            yield {
                'region_sheet': sn,
                'row': r,
                'region': rowvals[COL_REGION - 1],
                'province': rowvals[COL_PROVINCE - 1],
                'equipment': rowvals[COL_EQUIPMENT - 1],
                'unit_cost': rowvals[COL_UNITCOST - 1],
                'property_number': rowvals[COL_PROPNUM - 1],
                'serial_or_plate': rowvals[COL_SERIAL - 1],
                'issued_to': rowvals[COL_ISSUEDTO - 1],
                'designation': rowvals[COL_DESIGNATION - 1],
                'par_ptr': rowvals[COL_PARPTR - 1],
                'end_user': rowvals[COL_ENDUSER - 1],
                'office': rowvals[COL_OFFICE - 1],
                'status': rowvals[COL_STATUS - 1],
                'remarks': rowvals[COL_REMARKS - 1],
                'struck': struck_fields,  # column indices with strikethrough
            }


# ======================================================================
# Main migration builder
# ======================================================================

class Migration:
    def __init__(self):
        self.personnel = OrderedDict()       # normalized name -> {EmployeeID, FirstName, LastName, JobTitle, Department}
        self._personnel_seq = 0
        self.locations = OrderedDict()        # (region, province) -> LocationID
        self._location_seq = 0
        self.equipmentcatalog = OrderedDict()  # (cat,mfr,model) -> CatalogID
        self.fleetvehiclecatalog = OrderedDict()
        self.surveyequipmentcatalog = OrderedDict()
        self._catalog_seq = {'equipmentcatalog': 0, 'fleetvehiclecatalog': 0, 'surveyequipmentcatalog': 0}

        self.assets = []            # list of dicts, one INSERT row each
        self.fleetvehicles = []
        self.surveyassets = []
        self.assetassignments = []
        self.accessories_log = []
        self.skipped = []
        self.warnings = []
        self.flags = []            # rows that needed a sentinel owner or a duplicate-identifier fixup

        self._asset_tag_seq = 0
        self._vehicle_id_seq = 0
        self._survey_id_seq = 0

        # per-field in-batch dedup trackers for columns that are UNIQUE in the target schema
        self._dup_seen = {'assets_serial': set(), 'fleet_plate': set(), 'survey_serial': set()}
        self._dup_counter = {'assets_serial': 0, 'fleet_plate': 0, 'survey_serial': 0}

    # ---------------- personnel ----------------
    def get_or_create_person(self, raw_name, designation=None, office=None):
        if not raw_name:
            return None
        key = norm_upper(raw_name)
        if key in self.personnel:
            p = self.personnel[key]
            if designation and not p['JobTitle']:
                p['JobTitle'] = norm(designation)
            if office and not p['Department']:
                p['Department'] = title_case_place(office)
            return p['EmployeeID']
        self._personnel_seq += 1
        emp_id = f"PERS-{self._personnel_seq:05d}"
        name = norm(raw_name)
        tokens = name.split(' ')
        if len(tokens) > 1:
            last = tokens[-1]
            first = ' '.join(tokens[:-1])
        else:
            last = name
            first = name
        self.personnel[key] = {
            'EmployeeID': emp_id, 'FirstName': first[:50], 'LastName': last[:50],
            'JobTitle': norm(designation)[:100] if designation else None,
            'Department': title_case_place(office)[:100] if office else None,
            'RawName': name,
        }
        return emp_id

    # ---------------- sentinel owner for unresolvable assetassignments ----------------
    def get_sentinel_employee(self):
        key = '__MIGRATION_SENTINEL__'
        if key not in self.personnel:
            self.personnel[key] = {
                'EmployeeID': SENTINEL_EMPLOYEE_ID, 'FirstName': 'UNDEFINED', 'LastName': 'DURING MIGRATION',
                'JobTitle': None, 'Department': None, 'RawName': 'UNDEFINED DURING MIGRATION',
            }
        return SENTINEL_EMPLOYEE_ID

    def resolve_assignment_employee(self, emp_id, region, row_num, reference_type, reference_id):
        """Returns (employee_id, flag_note_or_None). Substitutes the sentinel
        personnel record instead of ever leaving EmployeeID unset (it's NOT NULL)."""
        if emp_id:
            return emp_id, None
        sentinel = self.get_sentinel_employee()
        self.flags.append({
            'category': 'UNRESOLVED_OWNER', 'region': region, 'row': row_num,
            'reference_type': reference_type, 'reference_id': reference_id,
            'field': 'EmployeeID', 'original_value': None, 'applied_value': sentinel,
            'note': 'No resolvable owner in source; assigned to migration sentinel personnel record for later reassignment.',
        })
        return sentinel, FLAG_UNRESOLVED_OWNER

    # ---------------- in-batch dedup for UNIQUE columns ----------------
    def dedupe_value(self, field_key, value, max_len, region, row_num, reference_type, reference_id, field_name):
        """Returns value unchanged the first time it's seen for this field; every
        later occurrence is disambiguated so it can't violate the column's UNIQUE
        constraint, and logged to self.flags for the migration_flags_report.csv."""
        if value is None:
            return None
        norm_val = value.strip().upper()
        seen = self._dup_seen[field_key]
        if norm_val in seen:
            self._dup_counter[field_key] += 1
            applied = f"MIGRATION DUPLICATE - {self._dup_counter[field_key]:03d} - {value}"[:max_len]
            self.flags.append({
                'category': 'DUPLICATE_IDENTIFIER', 'region': region, 'row': row_num,
                'reference_type': reference_type, 'reference_id': reference_id,
                'field': field_name, 'original_value': value, 'applied_value': applied,
                'note': f"{field_name} duplicated in source data; disambiguated to remain unique.",
            })
            return applied
        seen.add(norm_val)
        return value

    # ---------------- locations ----------------
    def get_or_create_location(self, region, province, office):
        prov = title_case_place(province) or 'Unknown'
        key = (norm_upper(region) or 'UNKNOWN', norm_upper(prov))
        if key in self.locations:
            return self.locations[key]['LocationID']
        self._location_seq += 1
        loc_id = self._location_seq
        self.locations[key] = {
            'LocationID': loc_id,
            'Area': (norm(region) or 'Unknown')[:50],
            'Province': prov[:50],
            'OfficeAddress': (title_case_place(office) or prov or 'N/A')[:255],
        }
        return loc_id

    # ---------------- catalogs ----------------
    def get_or_create_catalog(self, table, category, manufacturer, model):
        d = getattr(self, table)
        key = (category.strip().upper(), manufacturer.strip().upper(), model.strip().upper())
        if key in d:
            return d[key]['CatalogID']
        self._catalog_seq[table] += 1
        cid = self._catalog_seq[table]
        d[key] = {'CatalogID': cid, 'Category': category[:100], 'Manufacturer': manufacturer[:100], 'ModelName': model[:100]}
        return cid

    # ---------------- status mapping ----------------
    @staticmethod
    def map_status(raw):
        if not raw:
            return 'Unverified'
        s = str(raw).strip().upper()
        if 'UNSERVICE' in s:
            return 'Unserviceable'
        if 'SERVICE' in s:
            return 'Serviceable'
        return 'Unverified'

    # ---------------- owner resolution respecting strikethrough ----------------
    def resolve_owner(self, row):
        """
        Returns (employee_id_or_None, remarks_addendum_list)
        Never uses a struck field's OWN value to set the owner; falls back to the
        other holder field when only one side is struck; leaves owner None (with
        both raw values preserved in Remarks) when both are struck.
        """
        addenda = []
        issued_struck = COL_ISSUEDTO in row['struck']
        enduser_struck = COL_ENDUSER in row['struck']
        issued_val = row['issued_to']
        enduser_val = row['end_user']

        if issued_struck and enduser_struck:
            addenda.append(f"NEEDS MANUAL REVIEW (strikethrough, both fields flagged) - ISSUED TO: {norm(issued_val)!r}; NAME OF END USER: {norm(enduser_val)!r}. See Strikethrough_Review tab.")
            return None, addenda
        if issued_struck and not enduser_struck:
            addenda.append(f"NEEDS MANUAL REVIEW (strikethrough) - ISSUED TO on file: {norm(issued_val)!r}. Owner set from NAME OF END USER instead.")
            names = split_names(enduser_val)
        elif enduser_struck and not issued_struck:
            addenda.append(f"NEEDS MANUAL REVIEW (strikethrough) - NAME OF END USER on file: {norm(enduser_val)!r}. Owner set from ISSUED TO instead.")
            names = split_names(issued_val)
        else:
            # neither struck: prefer NAME OF END USER (actual user) then ISSUED TO
            names = split_names(enduser_val) or split_names(issued_val)

        if not names:
            return None, addenda
        primary = names[0]
        emp_id = self.get_or_create_person(primary, row['designation'], row['office'])
        if len(names) > 1:
            addenda.append(f"Also associated: {', '.join(names[1:])}")
        return emp_id, addenda

    # ---------------- accessory extraction ----------------
    def extract_accessories(self, remarks):
        """Return list of dicts: {keyword, status, property_number, careof}"""
        if not remarks:
            return []
        text = str(remarks)
        segments = re.split(r'\n', text)
        found = []
        seen_kw = set()
        for seg in segments:
            seg_up = seg.upper()
            # locate every accessory keyword's position in this segment, in order,
            # so a property number or "care of" name is attributed to the NEAREST
            # keyword rather than to every keyword mentioned anywhere in the segment
            # (e.g. "PRINTER-UNSERVICEABLE UPS-2023-05-03-10201SP528-01" must not
            # let the UPS's property number leak onto the PRINTER accessory).
            positions = []
            for kw in ACCESSORY_KEYWORDS:
                if kw in seg_up and kw not in seen_kw:
                    positions.append((seg_up.index(kw), kw))
            positions.sort()
            for idx, (pos, kw) in enumerate(positions):
                seen_kw.add(kw)
                end = positions[idx + 1][0] if idx + 1 < len(positions) else len(seg)
                span = seg[pos:end]
                span_up = span.upper()
                status = 'Unserviceable' if any(dw in span_up for dw in DEFECT_WORDS) else 'Unverified'
                pn_match = PROPNUM_RE.search(span)
                careof_match = CAREOF_RE.search(span)
                careof = None
                if careof_match:
                    careof = (careof_match.group(1) or careof_match.group(2) or '').strip()
                found.append({
                    'keyword': kw, 'status': status,
                    'property_number': pn_match.group(0) if pn_match else None,
                    'careof': careof or None,
                    'raw_segment': seg.strip(),
                })
        return found

    # ---------------- row processing ----------------
    def process_row(self, row):
        sn, r = row['region_sheet'], row['row']
        if (sn, r) in EXCLUDE_ROWS:
            self.skipped.append((sn, r, EXCLUDE_ROWS[(sn, r)]))
            return

        equip_raw = row['equipment']
        classification = classify_equipment(equip_raw)
        if classification is None:
            if equip_raw:
                self.warnings.append(f"{sn} row {r}: unrecognized equipment {equip_raw!r} - migrated to `assets` as Category='Other'.")
                classification = (GENERAL, 'Other', 'Unspecified', norm(equip_raw)[:100])
            else:
                self.skipped.append((sn, r, "Blank Equipment with no cost/property-number safety net caught earlier - skipped defensively."))
                return

        target, category, manufacturer, model = classification
        cost = parse_cost(row['unit_cost'])
        propnum = norm(row['property_number'])
        location_id = self.get_or_create_location(row['region'] or sn, row['province'], row['office'])
        emp_id, addenda = self.resolve_owner(row)
        status_word = self.map_status(row['status'])

        remarks_parts = []
        if row['remarks']:
            remarks_parts.append(norm(str(row['remarks'])))
        remarks_parts.extend(addenda)
        remarks_final = ' | '.join(remarks_parts) if remarks_parts else None

        doc_no = parse_par_ptr(row['par_ptr'])

        if target == FLEET:
            self._vehicle_id_seq += 1
            vid = self._vehicle_id_seq
            cat_id = self.get_or_create_catalog('fleetvehiclecatalog', category, manufacturer, model)
            plate_raw = norm(row['serial_or_plate'])
            plate_clean = None if (plate_raw and PLACEHOLDER_SERIAL_RE.search(plate_raw)) else plate_raw
            plate = self.dedupe_value('fleet_plate', plate_clean, 255, sn, r, 'FLEETVEHICLE', str(vid), 'PlateNumber')
            fleet_remarks = remarks_final
            if plate != plate_clean:
                fleet_remarks = (f"{fleet_remarks} | " if fleet_remarks else "") + FLAG_DUPLICATE_IDENTIFIER
            self.fleetvehicles.append({
                'VehicleID': vid, 'PropertyNumber': propnum, 'CatalogID': cat_id,
                'PlateNumber': plate, 'Cost': cost, 'AssignedDriverID': emp_id,
                'Remarks': fleet_remarks, 'OperationalStatus': status_word, 'MaintenanceStatus': status_word,
                '_region': sn, '_row': r, '_ref': ('FLEETVEHICLE', str(vid)),
            })
            if doc_no or emp_id:
                assign_emp_id, flag_note = self.resolve_assignment_employee(emp_id, sn, r, 'FLEETVEHICLE', str(vid))
                condition_notes = f"Migrated from {sn} row {r}."
                if flag_note:
                    condition_notes = f"{flag_note} {condition_notes}"
                self.assetassignments.append({
                    'ReferenceType': 'FLEETVEHICLE', 'AssetTag': str(vid), 'EmployeeID': assign_emp_id,
                    'ActionType': 'INITIAL MIGRATION RECORD', 'DocumentNo': doc_no,
                    'ConditionNotes': condition_notes,
                })

        elif target == SURVEY:
            self._survey_id_seq += 1
            sid = self._survey_id_seq
            asset_tag = f"SRV-{sid:05d}"
            cat_id = self.get_or_create_catalog('surveyequipmentcatalog', category, manufacturer, model)
            serial_raw = clean_serial(row['serial_or_plate'])
            serial = self.dedupe_value('survey_serial', serial_raw, 255, sn, r, 'SURVEYASSET', asset_tag, 'SerialNumber')
            survey_remarks = remarks_final
            if serial != serial_raw:
                survey_remarks = (f"{survey_remarks} | " if survey_remarks else "") + FLAG_DUPLICATE_IDENTIFIER
            self.surveyassets.append({
                'SurveyAssetID': sid, 'PropertyNumber': propnum, 'AssetTag': asset_tag,
                'CatalogID': cat_id, 'SerialNumber': serial, 'Cost': cost,
                'AssignedCustodianID': emp_id, 'Remarks': survey_remarks,
                'OperationalStatus': status_word, 'ConditionStatus': status_word,
                '_region': sn, '_row': r, '_ref': ('SURVEYASSET', str(sid)),
            })
            if doc_no or emp_id:
                assign_emp_id, flag_note = self.resolve_assignment_employee(emp_id, sn, r, 'SURVEYASSET', asset_tag)
                condition_notes = f"Migrated from {sn} row {r}."
                if flag_note:
                    condition_notes = f"{flag_note} {condition_notes}"
                self.assetassignments.append({
                    'ReferenceType': 'SURVEYASSET', 'AssetTag': str(sid), 'EmployeeID': assign_emp_id,
                    'ActionType': 'INITIAL MIGRATION RECORD', 'DocumentNo': doc_no,
                    'ConditionNotes': condition_notes,
                })

        else:  # GENERAL -> assets
            self._asset_tag_seq += 1
            asset_tag = f"AST-{self._asset_tag_seq:06d}"
            cat_id = self.get_or_create_catalog('equipmentcatalog', category, manufacturer, model)
            serial_raw = clean_serial(row['serial_or_plate'])
            serial = self.dedupe_value('assets_serial', serial_raw, 100, sn, r, 'ASSET', asset_tag, 'SerialNumber')
            deployment = 'Deployed' if emp_id else 'Unassigned'
            asset_remarks = remarks_final
            if serial != serial_raw:
                asset_remarks = (f"{asset_remarks} | " if asset_remarks else "") + FLAG_DUPLICATE_IDENTIFIER
            self.assets.append({
                'AssetTag': asset_tag, 'PropertyNumber': propnum, 'BundledWithAssetTag': None,
                'CatalogID': cat_id, 'SerialNumber': serial, 'PurchasePrice': cost,
                'CurrentOwnerID': emp_id, 'Remarks': asset_remarks,
                'DeploymentStatus': deployment, 'MaintenanceHealthStatus': status_word, 'LifecycleStatus': 'Active',
                '_region': sn, '_row': r, '_ref': ('ASSET', asset_tag),
            })
            if doc_no or emp_id:
                assign_emp_id, flag_note = self.resolve_assignment_employee(emp_id, sn, r, 'ASSET', asset_tag)
                condition_notes = f"Migrated from {sn} row {r}."
                if flag_note:
                    condition_notes = f"{flag_note} {condition_notes}"
                self.assetassignments.append({
                    'ReferenceType': 'ASSET', 'AssetTag': asset_tag, 'EmployeeID': assign_emp_id,
                    'ActionType': 'INITIAL MIGRATION RECORD', 'DocumentNo': doc_no,
                    'ConditionNotes': condition_notes,
                })

            # bundled accessories -> new linked `assets` rows (parent must be an `assets` row)
            for acc in self.extract_accessories(row['remarks']):
                acat, amfr, amodel = ACCESSORY_CATALOG[acc['keyword']]
                acc_cat_id = self.get_or_create_catalog('equipmentcatalog', acat, amfr, amodel)
                self._asset_tag_seq += 1
                acc_tag = f"AST-{self._asset_tag_seq:06d}"
                acc_remarks_parts = [f"Bundled with {asset_tag} ({model})."]
                if acc['raw_segment']:
                    acc_remarks_parts.append(f"Source remark: {acc['raw_segment']}")
                if acc['careof']:
                    acc_remarks_parts.append(f"Remark named a specific user ({acc['careof']}) - owner NOT reassigned; kept on parent's owner per migration policy.")
                self.assets.append({
                    'AssetTag': acc_tag, 'PropertyNumber': acc['property_number'], 'BundledWithAssetTag': asset_tag,
                    'CatalogID': acc_cat_id, 'SerialNumber': None, 'PurchasePrice': None,
                    'CurrentOwnerID': emp_id, 'Remarks': ' | '.join(acc_remarks_parts),
                    'DeploymentStatus': deployment, 'MaintenanceHealthStatus': acc['status'], 'LifecycleStatus': 'Active',
                    '_region': sn, '_row': r, '_ref': ('ASSET', acc_tag),
                })
                self.accessories_log.append({
                    'region': sn, 'row': r, 'parent_asset_tag': asset_tag, 'accessory_asset_tag': acc_tag,
                    'keyword': acc['keyword'], 'status': acc['status'], 'property_number': acc['property_number'],
                    'careof_named': acc['careof'], 'raw_segment': acc['raw_segment'],
                })

    def run(self, wb_v, wb_f):
        for row in extract_rows(wb_v, wb_f):
            self.process_row(row)


# ======================================================================
# SQL emission
# ======================================================================

def build_sql(m: Migration):
    out = []
    out.append("-- Auto-generated by migrate_inventory.py. Review before running against production.")
    out.append("-- Assumes dar_inventory_schema_updates.sql has already been applied.")
    out.append(f"-- Generated: {datetime.datetime.now().isoformat()}")
    out.append("START TRANSACTION;")
    out.append("")

    out.append("-- ===================== locations =====================")
    for loc in m.locations.values():
        out.append(
            f"INSERT INTO `locations` (`Area`,`Province`,`OfficeAddress`) VALUES "
            f"({sql_str(loc['Area'])},{sql_str(loc['Province'])},{sql_str(loc['OfficeAddress'])});"
        )
    out.append("")

    out.append("-- ===================== personnel =====================")
    for p in m.personnel.values():
        out.append(
            f"INSERT INTO `personnel` (`EmployeeID`,`FirstName`,`LastName`,`JobTitle`,`Department`) VALUES "
            f"({sql_str(p['EmployeeID'])},{sql_str(p['FirstName'])},{sql_str(p['LastName'])},"
            f"{sql_str(p['JobTitle'])},{sql_str(p['Department'])}) "
            f"ON DUPLICATE KEY UPDATE `EmployeeID`=`EmployeeID`;"
        )
    out.append("")

    for table in ['equipmentcatalog', 'fleetvehiclecatalog', 'surveyequipmentcatalog']:
        out.append(f"-- ===================== {table} (new entries) =====================")
        for c in getattr(m, table).values():
            out.append(
                f"INSERT INTO `{table}` (`Category`,`Manufacturer`,`ModelName`) VALUES "
                f"({sql_str(c['Category'])},{sql_str(c['Manufacturer'])},{sql_str(c['ModelName'])}) "
                f"ON DUPLICATE KEY UPDATE `ModelName`=`ModelName`;"
            )
        out.append("")

    out.append("-- ===================== assets =====================")
    out.append("-- CatalogID below is looked up by (Category,Manufacturer,ModelName) since the")
    out.append("-- real AUTO_INCREMENT id on your server depends on what already existed there.")
    for a in m.assets:
        out.append(
            "INSERT INTO `assets` (`AssetTag`,`PropertyNumber`,`BundledWithAssetTag`,`CatalogID`,`SerialNumber`,"
            "`PurchaseDate`,`PurchasePrice`,`CurrentOwnerID`,`Remarks`,`DeploymentStatus`,`MaintenanceHealthStatus`,`LifecycleStatus`) VALUES ("
            f"{sql_str(a['AssetTag'])},{sql_str(a['PropertyNumber'])},{sql_str(a['BundledWithAssetTag'])},"
            f"(SELECT CatalogID FROM equipmentcatalog WHERE Category={sql_str(_cat_lookup(m,'equipmentcatalog',a['CatalogID'])[0])} "
            f"AND Manufacturer={sql_str(_cat_lookup(m,'equipmentcatalog',a['CatalogID'])[1])} "
            f"AND ModelName={sql_str(_cat_lookup(m,'equipmentcatalog',a['CatalogID'])[2])} LIMIT 1),"
            f"{sql_str(a['SerialNumber'])},NULL,{sql_num(a['PurchasePrice'])},{sql_str(a['CurrentOwnerID'])},"
            f"{sql_str(a['Remarks'])},{sql_str(a['DeploymentStatus'])},{sql_str(a['MaintenanceHealthStatus'])},{sql_str(a['LifecycleStatus'])});"
        )
    out.append("")

    out.append("-- ===================== fleetvehicles =====================")
    for v in m.fleetvehicles:
        out.append(
            "INSERT INTO `fleetvehicles` (`PropertyNumber`,`CatalogID`,`PlateNumber`,`Cost`,`AssignedDriverID`,"
            "`Remarks`,`OperationalStatus`,`MaintenanceStatus`) VALUES ("
            f"{sql_str(v['PropertyNumber'])},"
            f"(SELECT CatalogID FROM fleetvehiclecatalog WHERE Category={sql_str(_cat_lookup(m,'fleetvehiclecatalog',v['CatalogID'])[0])} "
            f"AND Manufacturer={sql_str(_cat_lookup(m,'fleetvehiclecatalog',v['CatalogID'])[1])} "
            f"AND ModelName={sql_str(_cat_lookup(m,'fleetvehiclecatalog',v['CatalogID'])[2])} LIMIT 1),"
            f"{sql_str(v['PlateNumber'])},{sql_num(v['Cost'])},{sql_str(v['AssignedDriverID'])},"
            f"{sql_str(v['Remarks'])},{sql_str(v['OperationalStatus'])},{sql_str(v['MaintenanceStatus'])});"
        )
    out.append("")

    out.append("-- ===================== surveyassets =====================")
    for s in m.surveyassets:
        out.append(
            "INSERT INTO `surveyassets` (`PropertyNumber`,`AssetTag`,`CatalogID`,`SerialNumber`,`Cost`,"
            "`AssignedCustodianID`,`Remarks`,`OperationalStatus`,`ConditionStatus`) VALUES ("
            f"{sql_str(s['PropertyNumber'])},{sql_str(s['AssetTag'])},"
            f"(SELECT CatalogID FROM surveyequipmentcatalog WHERE Category={sql_str(_cat_lookup(m,'surveyequipmentcatalog',s['CatalogID'])[0])} "
            f"AND Manufacturer={sql_str(_cat_lookup(m,'surveyequipmentcatalog',s['CatalogID'])[1])} "
            f"AND ModelName={sql_str(_cat_lookup(m,'surveyequipmentcatalog',s['CatalogID'])[2])} LIMIT 1),"
            f"{sql_str(s['SerialNumber'])},{sql_num(s['Cost'])},{sql_str(s['AssignedCustodianID'])},"
            f"{sql_str(s['Remarks'])},{sql_str(s['OperationalStatus'])},{sql_str(s['ConditionStatus'])});"
        )
    out.append("")

    out.append("-- ===================== assetassignments (initial migration record per row) =====================")
    out.append("-- EmployeeID is never NULL here: rows with no resolvable owner are pointed at the")
    out.append(f"-- sentinel personnel record ({SENTINEL_EMPLOYEE_ID}) and flagged - see migration_flags_report.csv.")
    for a in m.assetassignments:
        out.append(
            "INSERT INTO `assetassignments` (`ReferenceType`,`AssetTag`,`EmployeeID`,`ActionType`,`DocumentNo`,`ConditionNotes`) VALUES ("
            f"{sql_str(a['ReferenceType'])},{sql_str(a['AssetTag'])},{sql_str(a['EmployeeID'])},"
            f"{sql_str(a['ActionType'])},{sql_str(a['DocumentNo'])},{sql_str(a['ConditionNotes'])});"
        )
    out.append("")
    out.append("COMMIT;")
    return '\n'.join(out)


def _cat_lookup(m, table, catalog_id):
    d = getattr(m, table)
    for key, v in d.items():
        if v['CatalogID'] == catalog_id:
            return v['Category'], v['Manufacturer'], v['ModelName']
    return ('Unspecified', 'Unspecified', 'Unspecified')


# ======================================================================
# Execute mode (optional, requires pymysql + a real server)
# ======================================================================

def execute_sql(sql_text, host, user, password, database, port=3306):
    try:
        import pymysql
    except ImportError:
        sys.exit("--execute requires pymysql: pip install pymysql")
    conn = pymysql.connect(host=host, user=user, password=password, database=database, port=port, autocommit=False)
    try:
        with conn.cursor() as cur:
            for stmt in sql_text.split(';\n'):
                stmt = stmt.strip()
                if not stmt or stmt.startswith('--'):
                    continue
                cur.execute(stmt)
        conn.commit()
        print("Migration committed successfully.")
    except Exception:
        conn.rollback()
        print("Error during execution - rolled back. Re-raising.")
        raise
    finally:
        conn.close()


# ======================================================================
# CLI
# ======================================================================

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--excel', required=True, help='Path to Inventory ICT_PPE CY 2022-2024 duplicate.xlsx')
    ap.add_argument('--execute', action='store_true', help='Actually run against a MySQL server (default: dry-run, writes .sql file only)')
    ap.add_argument('--host', default='localhost')
    ap.add_argument('--port', type=int, default=3306)
    ap.add_argument('--user', default='root')
    ap.add_argument('--password', default='')
    ap.add_argument('--database', default='dar_inventory')
    ap.add_argument('--out-sql', default='migration_output.sql')
    ap.add_argument('--out-log', default='migration_log.txt')
    ap.add_argument('--out-skipped', default='skipped_rows.csv')
    ap.add_argument('--out-accessories', default='accessories_created.csv')
    ap.add_argument('--out-flags', default='migration_flags_report.csv',
                     help='Rows given a sentinel owner or a disambiguated duplicate identifier, for later manual review')
    args = ap.parse_args()

    print(f"Loading {args.excel} ...")
    wb_v = openpyxl.load_workbook(args.excel, data_only=True)
    wb_f = openpyxl.load_workbook(args.excel, data_only=False)

    missing = [s for s in REGION_ORDER if s not in wb_v.sheetnames]
    if missing:
        sys.exit(f"Expected sheets missing from workbook: {missing}")

    m = Migration()
    m.run(wb_v, wb_f)

    # ---- logs ----
    with open(args.out_skipped, 'w', newline='', encoding='utf-8') as f:
        w = csv.writer(f)
        w.writerow(['Region', 'Row', 'Reason'])
        for sn, r, reason in m.skipped:
            w.writerow([sn, r, reason])

    with open(args.out_accessories, 'w', newline='', encoding='utf-8') as f:
        cols = ['region', 'row', 'parent_asset_tag', 'accessory_asset_tag', 'keyword', 'status', 'property_number', 'careof_named', 'raw_segment']
        dw = csv.DictWriter(f, fieldnames=cols)
        dw.writeheader()
        for rec in m.accessories_log:
            dw.writerow(rec)

    with open(args.out_flags, 'w', newline='', encoding='utf-8') as f:
        cols = ['category', 'region', 'row', 'reference_type', 'reference_id', 'field', 'original_value', 'applied_value', 'note']
        dw = csv.DictWriter(f, fieldnames=cols)
        dw.writeheader()
        for rec in m.flags:
            dw.writerow(rec)

    sql_text = build_sql(m)
    with open(args.out_sql, 'w', encoding='utf-8') as f:
        f.write(sql_text)

    with open(args.out_log, 'w', encoding='utf-8') as f:
        f.write("MIGRATION SUMMARY\n==================\n")
        f.write(f"Generated: {datetime.datetime.now().isoformat()}\n\n")
        f.write(f"assets rows (incl. bundled accessories): {len(m.assets)}\n")
        f.write(f"fleetvehicles rows: {len(m.fleetvehicles)}\n")
        f.write(f"surveyassets rows: {len(m.surveyassets)}\n")
        f.write(f"assetassignments rows: {len(m.assetassignments)}\n")
        f.write(f"personnel rows (synthetic EmployeeIDs generated): {len(m.personnel)}\n")
        f.write(f"locations rows: {len(m.locations)}\n")
        f.write(f"equipmentcatalog new entries: {len(m.equipmentcatalog)}\n")
        f.write(f"fleetvehiclecatalog new entries: {len(m.fleetvehiclecatalog)}\n")
        f.write(f"surveyequipmentcatalog new entries: {len(m.surveyequipmentcatalog)}\n")
        f.write(f"bundled accessory rows created: {len(m.accessories_log)}\n")
        f.write(f"rows skipped (manual review required): {len(m.skipped)}\n")
        unresolved_owner_flags = sum(1 for fl in m.flags if fl['category'] == 'UNRESOLVED_OWNER')
        duplicate_id_flags = sum(1 for fl in m.flags if fl['category'] == 'DUPLICATE_IDENTIFIER')
        f.write(f"flagged - sentinel owner assigned (see {args.out_flags}): {unresolved_owner_flags}\n")
        f.write(f"flagged - duplicate identifier disambiguated (see {args.out_flags}): {duplicate_id_flags}\n\n")
        f.write("SKIPPED ROWS\n------------\n")
        for sn, r, reason in m.skipped:
            f.write(f"  {sn} row {r}: {reason}\n")
        f.write(f"\nWARNINGS ({len(m.warnings)})\n------------\n")
        for w_ in m.warnings:
            f.write(f"  {w_}\n")

    print(f"\nWrote {args.out_sql}, {args.out_log}, {args.out_skipped}, {args.out_accessories}, {args.out_flags}")
    print(f"assets={len(m.assets)} fleetvehicles={len(m.fleetvehicles)} surveyassets={len(m.surveyassets)} "
          f"assetassignments={len(m.assetassignments)} personnel={len(m.personnel)} locations={len(m.locations)} "
          f"skipped={len(m.skipped)} warnings={len(m.warnings)} flags={len(m.flags)}")

    if args.execute:
        print("\n--execute set: connecting to MySQL and running the migration...")
        execute_sql(sql_text, args.host, args.user, args.password, args.database, args.port)
    else:
        print("\nDry run only (default). Review migration_output.sql, then re-run with --execute to apply it.")


if __name__ == '__main__':
    main()
