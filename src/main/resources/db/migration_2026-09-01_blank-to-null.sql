-- ============================================================================
-- MISD Master Asset Registry - corrective migration
-- Date:     2026-09-01
-- Database: misd_inventory
-- Pairs with the code changes for F-01..F-05, F-08, F-09, F-10 and F-32.
--
-- Sections
--   1  Preflight (read-only)
--   2  Empty strings to NULL            F-01, F-02, F-03
--   3  Drop the duplicate index         F-32
--   4  Verification (read-only)
--   5  Move audit rows onto stable keys F-10
--   6  Normalize status vocabulary      F-08, F-09
--   7  Final verification (read-only)
--
-- WHY
--   Spring bound an empty form field to '' rather than NULL. Several optional
--   columns are UNIQUE, and MySQL permits many NULLs in a unique index but only
--   one ''. So the *second* record saved with such a field left blank failed on
--   a duplicate-key error. Assets.CurrentOwnerID had the same problem against
--   its foreign key to Personnel, which has no employee with an empty ID.
--
--   The application no longer writes '' (see WebBindingAdvice and the
--   normalizeBlankOptionalFields / serializeFormToPayload changes). This script
--   cleans the rows written before that fix and removes a duplicate index.
--
-- SAFETY
--   - Idempotent: re-running it changes nothing further.
--   - No DROP TABLE, no row deletions, no schema changes to column types.
--   - Section 1 is read-only. Run it first and read the output.
--   - Section 2 is wrapped in a transaction.
--   - Section 3 is DDL (MySQL commits DDL implicitly) and is guarded so it is
--     a no-op if the duplicate index is already gone.
--
-- HOW TO RUN
--   mysql -uroot -p misd_inventory < migration_2026-09-01_blank-to-null.sql
--
--   Take a backup first:
--   mysqldump -uroot -p misd_inventory > misd_inventory_backup_2026-09-01.sql
-- ============================================================================


-- ----------------------------------------------------------------------------
-- SECTION 1 - Preflight. Read-only. Shows exactly what section 2 will change.
--             Expected on the reviewed database: assets.SerialNumber = 1,
--             assets.Remarks = 18, everything else 0.
-- ----------------------------------------------------------------------------

SELECT 'PREFLIGHT - rows holding an empty string' AS report;

SELECT 'assets.SerialNumber'                        AS column_name, COUNT(*) AS blank_rows FROM assets                WHERE SerialNumber          = ''
UNION ALL SELECT 'assets.CurrentOwnerID',                           COUNT(*) FROM assets                              WHERE CurrentOwnerID        = ''
UNION ALL SELECT 'assets.Remarks',                                  COUNT(*) FROM assets                              WHERE Remarks               = ''
UNION ALL SELECT 'fleetvehicles.PlateNumber',                       COUNT(*) FROM fleetvehicles                       WHERE PlateNumber           = ''
UNION ALL SELECT 'fleetvehicles.BodyNumber',                        COUNT(*) FROM fleetvehicles                       WHERE BodyNumber            = ''
UNION ALL SELECT 'fleetvehicles.EngineNumber',                      COUNT(*) FROM fleetvehicles                       WHERE EngineNumber          = ''
UNION ALL SELECT 'fleetvehicles.ChassisNumberVIN',                  COUNT(*) FROM fleetvehicles                       WHERE ChassisNumberVIN      = ''
UNION ALL SELECT 'fleetvehicles.AssignedDriverID',                  COUNT(*) FROM fleetvehicles                       WHERE AssignedDriverID      = ''
UNION ALL SELECT 'fleetvehicles.Remarks',                           COUNT(*) FROM fleetvehicles                       WHERE Remarks               = ''
UNION ALL SELECT 'realestateproperties.TitleNumber',                COUNT(*) FROM realestateproperties                WHERE TitleNumber           = ''
UNION ALL SELECT 'realestateproperties.TaxDeclarationNumber',       COUNT(*) FROM realestateproperties                WHERE TaxDeclarationNumber  = ''
UNION ALL SELECT 'realestateproperties.SurveyPlanNumber',           COUNT(*) FROM realestateproperties                WHERE SurveyPlanNumber      = ''
UNION ALL SELECT 'realestateproperties.CustodianID',                COUNT(*) FROM realestateproperties                WHERE CustodianID           = ''
UNION ALL SELECT 'realestateproperties.Remarks',                    COUNT(*) FROM realestateproperties                WHERE Remarks               = '';

-- The rows about to be touched, for the record.
SELECT 'PREFLIGHT - assets with a blank serial number' AS report;
SELECT AssetTag, CatalogID, DeploymentStatus FROM assets WHERE SerialNumber = '';


-- ----------------------------------------------------------------------------
-- SECTION 2 - Normalize empty strings to NULL.
--
--   Unique-constrained columns are the ones that actually broke. The remaining
--   columns are included so the data is consistent: a cleared Remarks or
--   CustodianID now reads as NULL everywhere rather than splitting between ''
--   and NULL depending on which release wrote it.
-- ----------------------------------------------------------------------------

START TRANSACTION;

-- IT assets --------------------------------------------------------------
-- SerialNumber is UNIQUE. This is the row that would have blocked the next
-- single-unit receipt with the serial number left blank.
UPDATE assets SET SerialNumber   = NULL WHERE SerialNumber   = '';
-- CurrentOwnerID is a foreign key to personnel.EmployeeID.
UPDATE assets SET CurrentOwnerID = NULL WHERE CurrentOwnerID = '';
UPDATE assets SET Remarks        = NULL WHERE Remarks        = '';

-- Fleet vehicles ---------------------------------------------------------
-- PlateNumber, BodyNumber, EngineNumber and ChassisNumberVIN are all UNIQUE.
UPDATE fleetvehicles SET PlateNumber      = NULL WHERE PlateNumber      = '';
UPDATE fleetvehicles SET BodyNumber       = NULL WHERE BodyNumber       = '';
UPDATE fleetvehicles SET EngineNumber     = NULL WHERE EngineNumber     = '';
UPDATE fleetvehicles SET ChassisNumberVIN = NULL WHERE ChassisNumberVIN = '';
-- AssignedDriverID is a foreign key to personnel.EmployeeID.
UPDATE fleetvehicles SET AssignedDriverID = NULL WHERE AssignedDriverID = '';
UPDATE fleetvehicles SET Remarks          = NULL WHERE Remarks          = '';

-- Real estate ------------------------------------------------------------
-- TitleNumber and TaxDeclarationNumber are both UNIQUE.
UPDATE realestateproperties SET TitleNumber          = NULL WHERE TitleNumber          = '';
UPDATE realestateproperties SET TaxDeclarationNumber = NULL WHERE TaxDeclarationNumber = '';
UPDATE realestateproperties SET SurveyPlanNumber     = NULL WHERE SurveyPlanNumber     = '';
UPDATE realestateproperties SET CustodianID          = NULL WHERE CustodianID          = '';
UPDATE realestateproperties SET Remarks              = NULL WHERE Remarks              = '';

COMMIT;


-- ----------------------------------------------------------------------------
-- SECTION 3 - Drop the duplicate unique index on FleetVehicles.BodyNumber.
--
--   The table carries the same constraint twice:
--       UNIQUE KEY `body_number` (`BodyNumber`)
--       UNIQUE KEY `BodyNumber`  (`BodyNumber`)
--   Two identical indexes are maintained on every insert and update for no
--   benefit. `BodyNumber` is kept because it matches the naming of the table's
--   other unique keys (PlateNumber, EngineNumber, ChassisNumberVIN).
--
--   MySQL has no DROP INDEX IF EXISTS, so this is guarded and re-runnable.
-- ----------------------------------------------------------------------------

SET @duplicate_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'fleetvehicles'
      AND INDEX_NAME   = 'body_number'
);

SET @drop_duplicate_index := IF(
    @duplicate_index_exists > 0,
    'ALTER TABLE fleetvehicles DROP INDEX body_number',
    'SELECT ''SKIPPED - duplicate index body_number is already absent'' AS note'
);

PREPARE stmt FROM @drop_duplicate_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ----------------------------------------------------------------------------
-- SECTION 4 - Verification. All counts must be 0 and the index list must show
--             BodyNumber once.
-- ----------------------------------------------------------------------------

SELECT 'VERIFY - remaining empty strings (all must be 0)' AS report;

SELECT 'assets.SerialNumber'                        AS column_name, COUNT(*) AS blank_rows FROM assets                WHERE SerialNumber          = ''
UNION ALL SELECT 'assets.CurrentOwnerID',                           COUNT(*) FROM assets                              WHERE CurrentOwnerID        = ''
UNION ALL SELECT 'fleetvehicles.PlateNumber',                       COUNT(*) FROM fleetvehicles                       WHERE PlateNumber           = ''
UNION ALL SELECT 'fleetvehicles.BodyNumber',                        COUNT(*) FROM fleetvehicles                       WHERE BodyNumber            = ''
UNION ALL SELECT 'fleetvehicles.EngineNumber',                      COUNT(*) FROM fleetvehicles                       WHERE EngineNumber          = ''
UNION ALL SELECT 'fleetvehicles.ChassisNumberVIN',                  COUNT(*) FROM fleetvehicles                       WHERE ChassisNumberVIN      = ''
UNION ALL SELECT 'realestateproperties.TitleNumber',                COUNT(*) FROM realestateproperties                WHERE TitleNumber           = ''
UNION ALL SELECT 'realestateproperties.TaxDeclarationNumber',       COUNT(*) FROM realestateproperties                WHERE TaxDeclarationNumber  = '';

SELECT 'VERIFY - fleetvehicles unique indexes (BodyNumber must appear once)' AS report;
SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'fleetvehicles'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;


-- ----------------------------------------------------------------------------
-- SECTION 5 - Move audit rows onto stable keys.                          (F-10)
--
--   History was filed under the plate number for vehicles and the title number
--   for properties. Both can change: a blank plate can be filled in later, a
--   vehicle can be re-plated, and a property's title can be recorded after
--   registration. Every entry written under the old value then became
--   unreachable from the detail panel. The application now always writes
--   "VEHICLE-{id}" and "PROP-{id}"; this moves the existing rows to match.
--
--   The NOT IN guard keeps IT asset rows untouched. AssetAssignments.AssetTag
--   and LifecycleAuditLog.ReferenceID hold references for all three domains, so
--   a plate or title that happened to equal a real asset tag must not be
--   rewritten. On the reviewed database no such collision exists.
-- ----------------------------------------------------------------------------

START TRANSACTION;

UPDATE assetassignments assignment
    JOIN fleetvehicles vehicle ON assignment.AssetTag = vehicle.PlateNumber
SET assignment.AssetTag = CONCAT('VEHICLE-', vehicle.VehicleID)
WHERE assignment.AssetTag NOT IN (SELECT AssetTag FROM assets);

UPDATE lifecycleauditlog auditlog
    JOIN fleetvehicles vehicle ON auditlog.ReferenceID = vehicle.PlateNumber
SET auditlog.ReferenceID = CONCAT('VEHICLE-', vehicle.VehicleID)
WHERE auditlog.ReferenceID NOT IN (SELECT AssetTag FROM assets);

UPDATE assetassignments assignment
    JOIN realestateproperties property ON assignment.AssetTag = property.TitleNumber
SET assignment.AssetTag = CONCAT('PROP-', property.PropertyID)
WHERE assignment.AssetTag NOT IN (SELECT AssetTag FROM assets);

UPDATE lifecycleauditlog auditlog
    JOIN realestateproperties property ON auditlog.ReferenceID = property.TitleNumber
SET auditlog.ReferenceID = CONCAT('PROP-', property.PropertyID)
WHERE auditlog.ReferenceID NOT IN (SELECT AssetTag FROM assets);

COMMIT;


-- ----------------------------------------------------------------------------
-- SECTION 6 - Normalize status vocabulary.                         (F-08, F-09)
--
--   F-08: FleetService writes "Missing" and "Stolen" as separate values and the
--         dropdown offers them separately, but the dashboard's problem queries
--         only ever looked for the combined "Missing/Stolen". The queries now
--         accept all three; this retires the combined value so one state is not
--         spelled two ways. (No row currently holds it - this is a safeguard.)
--
--   F-09: The Add Property form offered "Paid 2026" while the detail panel
--         offered "Paid (Current Year)", so the column holds both spellings of
--         the same state - and "Paid 2026" is wrong from 1 January. Both forms
--         now share one list; this moves the existing rows onto it.
-- ----------------------------------------------------------------------------

START TRANSACTION;

UPDATE fleetvehicles SET OperationalStatus = 'Missing' WHERE OperationalStatus = 'Missing/Stolen';

UPDATE realestateproperties
SET PropertyTaxStatus = 'Paid (Current Year)'
WHERE PropertyTaxStatus LIKE 'Paid %'
  AND PropertyTaxStatus <> 'Paid (Current Year)';

COMMIT;


-- ----------------------------------------------------------------------------
-- SECTION 7 - Final verification. Read-only.
-- ----------------------------------------------------------------------------

SELECT 'VERIFY - audit references still keyed on a business value (must be 0)' AS report;

SELECT 'assignments on a plate' AS reference_kind, COUNT(*) AS rows_remaining
FROM assetassignments assignment JOIN fleetvehicles vehicle ON assignment.AssetTag = vehicle.PlateNumber
UNION ALL SELECT 'lifecycle on a plate', COUNT(*)
FROM lifecycleauditlog auditlog JOIN fleetvehicles vehicle ON auditlog.ReferenceID = vehicle.PlateNumber
UNION ALL SELECT 'assignments on a title', COUNT(*)
FROM assetassignments assignment JOIN realestateproperties property ON assignment.AssetTag = property.TitleNumber
UNION ALL SELECT 'lifecycle on a title', COUNT(*)
FROM lifecycleauditlog auditlog JOIN realestateproperties property ON auditlog.ReferenceID = property.TitleNumber;

SELECT 'VERIFY - retired status values (must be 0)' AS report;

SELECT 'fleetvehicles Missing/Stolen' AS status_value, COUNT(*) AS rows_remaining
FROM fleetvehicles WHERE OperationalStatus = 'Missing/Stolen'
UNION ALL SELECT 'realestateproperties Paid <year>', COUNT(*)
FROM realestateproperties WHERE PropertyTaxStatus LIKE 'Paid %' AND PropertyTaxStatus <> 'Paid (Current Year)';

SELECT 'VERIFY - distinct status values now in use' AS report;
SELECT DISTINCT OperationalStatus FROM fleetvehicles ORDER BY OperationalStatus;
SELECT DISTINCT PropertyTaxStatus FROM realestateproperties ORDER BY PropertyTaxStatus;


-- ============================================================================
-- OPTIONAL - not applied, needs a decision from MISD.
--
-- Vehicle 107 carries the literal plate number 'NA'. That looks like a manual
-- workaround for the bug section 2 fixes: the field was optional, but leaving it
-- blank was unsafe, so a placeholder went in instead. With blanks now stored as
-- NULL, the placeholder can become a real absence:
--
--     UPDATE fleetvehicles SET PlateNumber = NULL WHERE VehicleID = 107 AND PlateNumber = 'NA';
--
-- Run this only if that vehicle genuinely has no plate. Its history needs no
-- further action either way - section 5 has already moved audit rows onto
-- VEHICLE-107, which does not depend on the plate.
-- ============================================================================
