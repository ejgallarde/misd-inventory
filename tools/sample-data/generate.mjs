// Builds out/sample-data.sql: a demo dataset for all three modules.
//
//   cd tools/sample-data && npm install && npm run generate
//
// People, IT assets and vehicles are defined below. Land records are sampled
// (with a fixed seed, so re-running gives the same picks) from the
// "ALL-INVENTORY working" tab of docs/LAND INVENTORY.xlsx. Only values present
// in the workbook are used; no title or tax-declaration numbers are invented.
// The workbook and the generated SQL are gitignored: they hold PHLPost data.
import ExcelJS from 'exceljs';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const workbookPath = process.env.LAND_INVENTORY ?? resolve(here, '../../docs/LAND INVENTORY.xlsx');
const SHEET = 'ALL-INVENTORY working';
const LOT_COUNT = 10;
const BUILDING_COUNT = 3;

// ---------------------------------------------------------------------------
// People. The four named staff are Regular. Two fictional extras show the
// accountability rule: a Job Order driver and a Contract of Service IT staffer.
// ---------------------------------------------------------------------------
const PEOPLE = [
  { id: 'SMP-0001', first: 'Marigold C.', last: 'Libuit-Arroyo', title: 'APMG for MSS', status: 'Regular',
    dept: 'Management Support Services', division: 'Office of the APMG', manager: null },
  { id: 'SMP-0002', first: 'Earl John C.', last: 'Gallarde', title: 'IT Officer II', status: 'Regular',
    dept: 'MISD', division: 'Systems and Infrastructure', manager: 'SMP-0001' },
  { id: 'SMP-0003', first: 'Joshua', last: 'de Jesus', title: 'Computer Programmer III', status: 'Regular',
    dept: 'MISD', division: 'Systems and Infrastructure', manager: 'SMP-0002' },
  { id: 'SMP-0004', first: 'Don', last: 'Melgarejo', title: 'Admin / Manager, LPMD', status: 'Regular',
    dept: 'LPMD', division: 'Property and Motorpool', manager: 'SMP-0001' },
  { id: 'SMP-0005', first: 'Ramon', last: 'Sample-Driver', title: 'Driver', status: 'Job Order',
    dept: 'LPMD', division: 'Property and Motorpool', manager: 'SMP-0004' },
  { id: 'SMP-0006', first: 'Liza', last: 'Sample-Support', title: 'IT Support Staff', status: 'Contract of Service',
    dept: 'MISD', division: 'Systems and Infrastructure', manager: 'SMP-0002' },
];

// Demo sign-ins for these people (paste into app.security.demo-users).
const DEMO_USERS = [
  'earl.gallarde@phlpost.gov.ph:ADMIN',
  'joshua.dejesus@phlpost.gov.ph:CLERK',
  'don.melgarejo@phlpost.gov.ph:CLERK',
  'marigold.libuit-arroyo@phlpost.gov.ph:FINANCE',
];

const CATALOG = [
  { id: 101, category: 'Laptop', make: 'Dell', model: 'Latitude 7450',
    specs: { cpu: 'Core Ultra 7 165U', ram: '16GB', storage: '512GB SSD', display: '14in FHD+' } },
  { id: 102, category: 'Desktop', make: 'HP', model: 'ProDesk 400 G9 SFF',
    specs: { cpu: 'Core i5-12500', ram: '16GB', storage: '512GB SSD' } },
  { id: 103, category: 'Firewall Equipment', make: 'Fortinet', model: 'FortiGate 100F',
    specs: { throughput: '20 Gbps firewall', ports: '22x GE', ha: 'active-passive' } },
  { id: 104, category: 'Barcode Scanner', make: 'Zebra', model: 'DS2208',
    specs: { type: '1D/2D imager', interface: 'USB' } },
  { id: 105, category: 'Printer', make: 'HP', model: 'LaserJet Enterprise M611dn',
    specs: { duty: 'Heavy duty, up to 300,000 pages/month', speed: '65 ppm', duplex: 'yes' } },
];

// [tag, catalogId, serial, purchaseDate, price, userId, accountableId, deployment, health, lifecycle, remarks]
const ASSETS = [
  ['SAMPLE-LAP-0001', 101, 'SMP-DL7450-001', '2025-03-14', 89500, 'SMP-0003', 'SMP-0003', 'Deployed', 'Operational', 'Active', 'Development laptop'],
  ['SAMPLE-LAP-0002', 101, 'SMP-DL7450-002', '2025-03-14', 89500, 'SMP-0006', 'SMP-0002', 'Deployed', 'Operational', 'Active',
    'User is Contract of Service; accountability rests with the IT Officer'],
  ['SAMPLE-DSK-0001', 102, 'SMP-HP400-001', '2024-08-02', 52000, 'SMP-0002', 'SMP-0002', 'Deployed', 'Operational', 'Active', null],
  ['SAMPLE-DSK-0002', 102, 'SMP-HP400-002', '2024-08-02', 52000, null, null, 'In Storage', 'Operational', 'Procured / Pre-Deployment', 'Spare unit'],
  ['SAMPLE-FW-0001', 103, 'SMP-FG100F-001', '2023-11-20', 685000, 'SMP-0002', 'SMP-0002', 'Deployed', 'Operational', 'Active',
    'Perimeter firewall, Central Office data center'],
  ['SAMPLE-BCS-0001', 104, 'SMP-DS2208-001', '2025-06-05', 9800, 'SMP-0005', 'SMP-0004', 'Deployed', 'Operational', 'Active',
    'Used by a Job Order driver for dispatch scanning; LPMD manager accountable'],
  ['SAMPLE-PRN-0001', 105, 'SMP-M611-001', '2024-02-12', 168000, 'SMP-0001', 'SMP-0001', 'Deployed', 'Degraded', 'Active',
    'Heavy-duty office printer; fuser due for replacement'],
  ['SAMPLE-PRN-0002', 105, 'SMP-M611-002', '2025-09-30', 172000, null, null, 'In Storage', 'Operational', 'Procured / Pre-Deployment', null],
];

// [id, plate, type, make, model, year, fuel, regExpiry, cost, acqYear, driver, accountable, legal, operational, maintenance, remarks]
const VEHICLES = [
  [201, 'SMP 1001', 'Delivery Van', 'BYD', 'T3', 2024, 'EV', '2027-04-30', 1890000, 2024, 'SMP-0005', 'SMP-0004',
    'Registered', 'Dispatched', 'Roadworthy', 'Electric delivery van; driver is Job Order, LPMD manager accountable'],
  [202, 'SMP 1002', 'Service Vehicle', 'BYD', 'Atto 3', 2025, 'EV', '2027-08-31', 1798000, 2025, null, null,
    'Registered', 'Available/Idle', 'Roadworthy', null],
  [203, 'SMP 1003', 'Van', 'Toyota', 'Hiace Commuter Deluxe', 2022, 'Diesel', '2026-03-31', 2105000, 2022, null, null,
    'Registered', 'Available/Idle', 'Roadworthy', 'Registration has lapsed'],
  [204, 'SMP 1004', 'Pickup Truck', 'Nissan', 'Navara VE 4x2', 2019, 'Diesel', '2027-01-31', 1450000, 2019, null, null,
    'Registered', 'Grounded', 'Under Repair', 'Clutch replacement at motorpool'],
  [205, 'SMP 1005', 'Executive Car', 'BMW', '320i Sport', 2023, 'Gas', '2027-10-31', 3290000, 2023, 'SMP-0005', 'SMP-0001',
    'Registered', 'Dispatched', 'Roadworthy', 'Executive car; accountable owner chosen at assignment'],
];

// ---------------------------------------------------------------------------
const q = (value) => (value === null || value === undefined || value === '')
  ? 'NULL'
  : typeof value === 'number' ? String(value) : `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`;

function mulberry32(seed) {
  return function () {
    seed |= 0; seed = (seed + 0x6D2B79F5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const cellValue = (v) => (v && typeof v === 'object')
  ? (v.result ?? v.text ?? (v.richText ? v.richText.map((t) => t.text).join('') : null))
  : v;
const clean = (v) => (v === null || v === undefined) ? '' : String(v).replace(/\s+/g, ' ').trim();

async function readLandRows() {
  const reader = new ExcelJS.stream.xlsx.WorkbookReader(workbookPath,
    { sharedStrings: 'cache', hyperlinks: 'ignore', styles: 'ignore', worksheets: 'emit' });
  const rows = [];
  for await (const sheet of reader) {
    if (sheet.name !== SHEET) continue;
    for await (const row of sheet) {
      const c = row.values.slice(1).map(cellValue);
      // Data rows are numbered in column A and name a post office in column D.
      if (typeof c[0] === 'number' && clean(c[3]) && clean(c[2]) && typeof c[5] === 'number' && c[5] > 0) {
        rows.push({
          sheetRow: row.number, area: clean(c[1]), province: clean(c[2]), name: clean(c[3]),
          location: clean(c[4]), lotAreaSqm: c[5], acquisition: clean(c[7]),
          withBuilding: clean(c[11]) !== '', totalValue: typeof c[15] === 'number' ? c[15] : null,
        });
      }
    }
  }
  if (!rows.length) throw new Error(`No data rows found in "${SHEET}" of ${workbookPath}`);
  return rows;
}

function normalizeArea(area) {
  if (/^co$/i.test(area)) return 'Central Office';
  const match = /area\s*(\d)/i.exec(area);
  return match ? `Area ${match[1]}` : null;
}

function legalStatus(acquisition) {
  const a = acquisition.toLowerCase();
  if (a.includes('titled')) return 'Titled';
  if (a.includes('lease') || a.includes('usufruct')) return 'Leased';
  if (a.includes('no legal')) return 'In Process';
  return 'Tax Declared Only';
}

const rows = await readLandRows();
const random = mulberry32(20261028);
const picks = [];
const pool = rows.filter((r) => normalizeArea(r.area));
while (picks.length < LOT_COUNT && pool.length) {
  picks.push(...pool.splice(Math.floor(random() * pool.length), 1));
}
const taxStatuses = ['Paid (Current Year)', 'Paid (Current Year)', 'Pending', 'Overdue'];

const sql = [];
sql.push('-- Generated by tools/sample-data/generate.mjs on ' + new Date().toISOString());
sql.push(`-- Land records sampled from "${SHEET}" (${rows.length} rows; ${picks.length} picked, seed 20261028).`);
sql.push('-- Load into an EMPTY schema that has the dated migrations applied. Not for production.');
sql.push('SET NAMES utf8mb4;');
sql.push('START TRANSACTION;');

sql.push(`INSERT INTO locations (LocationID, Area, Province, OfficeAddress) VALUES
  (1, 'Central Office', 'Metro Manila', 'Liwasang Bonifacio, Ermita, Manila');`);

sql.push('INSERT INTO Personnel (EmployeeID, FirstName, LastName, JobTitle, EmploymentStatus, Department, Division, ManagerID, BaseLocationID) VALUES\n'
  + PEOPLE.map((p) => `  (${[p.id, p.first, p.last, p.title, p.status, p.dept, p.division, p.manager].map(q).join(', ')}, 1)`).join(',\n') + ';');

sql.push('INSERT INTO EquipmentCatalog (CatalogID, Category, Manufacturer, ModelName, Specifications) VALUES\n'
  + CATALOG.map((c) => `  (${c.id}, ${q(c.category)}, ${q(c.make)}, ${q(c.model)}, ${q(JSON.stringify(c.specs))})`).join(',\n') + ';');

sql.push('INSERT INTO Assets (AssetTag, CatalogID, SerialNumber, PurchaseDate, PurchasePrice, AssignedUserID, CurrentOwnerID, DeploymentStatus, MaintenanceHealthStatus, LifecycleStatus, Remarks) VALUES\n'
  + ASSETS.map((a) => `  (${a.map(q).join(', ')})`).join(',\n') + ';');

sql.push('INSERT INTO FleetVehicles (VehicleID, PlateNumber, VehicleType, Make, Model, ManufactureYear, FuelType, RegistrationExpiry, InsuranceExpiry, Cost, AcquisitionYear, AssignedDriverID, AccountableOwnerID, AdminLegaltionalStatus, OperationalStatus, MaintenanceStatus, Remarks, EngineNumber, ChassisNumberVIN, BodyNumber) VALUES\n'
  + VEHICLES.map((v) => {
    const [id, plate, type, make, model, year, fuel, reg, cost, acq, driver, accountable, legal, op, maint, remarks] = v;
    return `  (${[id, plate, type, make, model, year, fuel, reg, reg, cost, acq, driver, accountable, legal, op, maint, remarks,
      `SMP-ENG-${id}`, `SMP-VIN-${id}`, `SMP-BODY-${id}`].map(q).join(', ')})`;
  }).join(',\n') + ';');

const properties = [];
let propertyId = 501;
picks.forEach((lot, index) => {
  const source = `Source: LAND INVENTORY.xlsx, ${SHEET} row ${lot.sheetRow}; acquisition: ${lot.acquisition || 'not stated'}`;
  properties.push({
    id: propertyId++, type: 'Lot', name: lot.name, area: normalizeArea(lot.area), address: lot.location || lot.name,
    // The workbook's location is one free-text field; it goes in Address Line 1 only,
    // so the address is not shown twice. City is left for the user to pick from PSGC.
    province: lot.province, city: null, lotArea: lot.lotAreaSqm, value: lot.totalValue,
    tax: taxStatuses[Math.floor(random() * taxStatuses.length)], legal: legalStatus(lot.acquisition),
    operational: lot.withBuilding ? 'Active/In Use' : 'Vacant/Idle', condition: 'Usable - No Known Issues',
    custodian: index % 3 === 2 ? null : 'SMP-0004', remarks: source,
  });
});
picks.filter((lot) => lot.withBuilding).slice(0, BUILDING_COUNT).forEach((lot, index) => {
  properties.push({
    id: propertyId++, type: 'Facility - Branch Office', name: `${lot.name} (PPC Building)`, area: normalizeArea(lot.area),
    address: lot.location || lot.name, province: lot.province, city: null, lotArea: null, value: null,
    tax: index === 0 ? 'Overdue' : 'Paid (Current Year)', legal: legalStatus(lot.acquisition),
    operational: 'Active/In Use', condition: index === 1 ? 'Usable - Minor Repair Needed' : 'Usable - No Known Issues',
    custodian: 'SMP-0004', remarks: `PPC building on the lot listed at ${SHEET} row ${lot.sheetRow}`,
  });
});
sql.push('INSERT INTO RealEstateProperties (PropertyID, PropertyType, PropertyName, Area, AddressLine1, Province, City, LotAreaSqm, AssessedValue, PropertyTaxStatus, LegalTitlingStatus, OperationalStatus, ConditionStatus, CustodianID, Remarks) VALUES\n'
  + properties.map((p) => `  (${[p.id, p.type, p.name, p.area, p.address, p.province, p.city, p.lotArea, p.value, p.tax, p.legal,
    p.operational, p.condition, p.custodian, p.remarks].map(q).join(', ')})`).join(',\n') + ';');

// A little history so the history views are not empty.
const lifecycle = [];
const assignments = [];
ASSETS.forEach(([tag, , , date, , user, accountable]) => {
  lifecycle.push([tag, 'Asset Received', 'earl.gallarde@phlpost.gov.ph', `${date} 09:00:00`, 'Received into storage']);
  if (user) {
    lifecycle.push([tag, 'Checkout', 'joshua.dejesus@phlpost.gov.ph', `${date} 14:00:00`,
      `User: ${user}; Accountable: ${accountable}${user !== accountable ? ' (manager; user is not eligible)' : ''}`]);
    assignments.push([tag, user, 'Checkout', `${date} 14:00:00`, `User: ${user}; Accountable: ${accountable}`]);
    if (user !== accountable) assignments.push([tag, accountable, 'Accountability Assigned', `${date} 14:00:00`, `User: ${user}`]);
  }
});
VEHICLES.filter((v) => v[10]).forEach(([id, , , , , , , , , acq, driver, accountable]) => {
  lifecycle.push([`VEHICLE-${id}`, 'Vehicle Checkout', 'don.melgarejo@phlpost.gov.ph', '2026-10-01 07:30:00',
    `Driver: ${driver}; Accountable: ${accountable}`]);
  assignments.push([`VEHICLE-${id}`, driver, 'Vehicle Checkout', '2026-10-01 07:30:00', `Driver: ${driver}; Accountable: ${accountable}`]);
});
properties.filter((p) => p.tax === 'Overdue').forEach((p) => {
  lifecycle.push([`PROP-${p.id}`, 'Tax Status Updated: Overdue', 'marigold.libuit-arroyo@phlpost.gov.ph',
    '2026-09-15 10:00:00', 'Real property tax for 2026 unpaid']);
});
sql.push('INSERT INTO LifecycleAuditLog (ReferenceID, ActionType, PerformedBy, TransactionDate, Notes) VALUES\n'
  + lifecycle.map((r) => `  (${r.map(q).join(', ')})`).join(',\n') + ';');
sql.push('INSERT INTO AssetAssignments (AssetTag, EmployeeID, ActionType, TransactionDate, ConditionNotes) VALUES\n'
  + assignments.map((r) => `  (${r.map(q).join(', ')})`).join(',\n') + ';');

sql.push('COMMIT;');
sql.push(`-- Demo sign-ins: app.security.demo-users=${DEMO_USERS.join(',')}`);

const outPath = resolve(here, 'out/sample-data.sql');
mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, sql.join('\n\n') + '\n');
console.log(`Wrote ${outPath}`);
console.log(`  ${PEOPLE.length} people, ${CATALOG.length} catalog items, ${ASSETS.length} assets, ${VEHICLES.length} vehicles, ${properties.length} properties`);
console.log('  Picked lots:', picks.map((p) => `${p.name} (${p.province})`).join('; '));
