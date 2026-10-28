-- ============================================================================
-- MISD Master Asset Registry - schema migration
-- Date:     2026-10-28
-- Database: misd_inventory
--
-- Separates the person who USES an asset from the person ACCOUNTABLE for it.
-- In government property accountability (PAR/ICS), non-regular personnel
-- (Contract of Service, Job Order, and others not configured as eligible)
-- may use equipment and drive vehicles but cannot sign for them; the
-- accountability rests with a regular employee, usually their manager.
--
-- Sections
--   1  Preflight (read-only)
--   2  Personnel.EmploymentStatus               (new, defaults to 'Regular')
--   3  Assets.AssignedUserID                    (new; CurrentOwnerID stays the
--                                                accountable owner)
--   4  FleetVehicles.AccountableOwnerID         (new; AssignedDriverID stays
--                                                the person who drives it)
--   5  Backfill: existing holders become both user and accountable owner
--   6  Verification (read-only)
--
-- AFTER RUNNING
--   Every existing employee is marked 'Regular'. Update the non-regular ones,
--   for example:
--     UPDATE personnel SET EmploymentStatus = 'Job Order' WHERE EmployeeID IN (...);
--   Accountability for assets they already hold is NOT recalculated by this
--   script; reassign those assets in the application (or update the rows) so
--   a regular employee becomes accountable.
--
-- SAFETY
--   - Idempotent: every DDL statement is guarded and re-running changes nothing.
--   - No row deletions; the backfill only fills NULLs.
--   - Take a backup first:
--       mysqldump -u<user> -p misd_inventory > misd_inventory_backup_2026-10-28.sql
--
-- HOW TO RUN
--   mysql -u<user> -p misd_inventory < migration_2026-10-28_assigned-vs-accountable.sql
-- ============================================================================


-- ----------------------------------------------------------------------------
-- SECTION 1 - Preflight (read-only): which of the new columns already exist.
-- ----------------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'personnel'     AND COLUMN_NAME = 'EmploymentStatus')
    OR (TABLE_NAME = 'assets'        AND COLUMN_NAME = 'AssignedUserID')
    OR (TABLE_NAME = 'fleetvehicles' AND COLUMN_NAME = 'AccountableOwnerID'));


-- ----------------------------------------------------------------------------
-- SECTION 2 - Personnel.EmploymentStatus
-- ----------------------------------------------------------------------------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'personnel' AND COLUMN_NAME = 'EmploymentStatus') = 0,
    'ALTER TABLE personnel ADD COLUMN EmploymentStatus varchar(40) NOT NULL DEFAULT ''Regular'' AFTER JobTitle',
    'SELECT ''SKIPPED - personnel.EmploymentStatus exists'' AS note');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ----------------------------------------------------------------------------
-- SECTION 3 - Assets.AssignedUserID (+ foreign key to personnel)
-- ----------------------------------------------------------------------------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assets' AND COLUMN_NAME = 'AssignedUserID') = 0,
    'ALTER TABLE assets ADD COLUMN AssignedUserID varchar(20) DEFAULT NULL AFTER CurrentOwnerID',
    'SELECT ''SKIPPED - assets.AssignedUserID exists'' AS note');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assets' AND CONSTRAINT_NAME = 'assets_assigned_user_fk') = 0,
    'ALTER TABLE assets ADD CONSTRAINT assets_assigned_user_fk FOREIGN KEY (AssignedUserID) REFERENCES personnel (EmployeeID) ON DELETE SET NULL',
    'SELECT ''SKIPPED - assets_assigned_user_fk exists'' AS note');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ----------------------------------------------------------------------------
-- SECTION 4 - FleetVehicles.AccountableOwnerID (+ foreign key to personnel)
-- ----------------------------------------------------------------------------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fleetvehicles' AND COLUMN_NAME = 'AccountableOwnerID') = 0,
    'ALTER TABLE fleetvehicles ADD COLUMN AccountableOwnerID varchar(20) DEFAULT NULL AFTER AssignedDriverID',
    'SELECT ''SKIPPED - fleetvehicles.AccountableOwnerID exists'' AS note');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fleetvehicles' AND CONSTRAINT_NAME = 'fleetvehicles_accountable_owner_fk') = 0,
    'ALTER TABLE fleetvehicles ADD CONSTRAINT fleetvehicles_accountable_owner_fk FOREIGN KEY (AccountableOwnerID) REFERENCES personnel (EmployeeID) ON DELETE SET NULL',
    'SELECT ''SKIPPED - fleetvehicles_accountable_owner_fk exists'' AS note');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ----------------------------------------------------------------------------
-- SECTION 5 - Backfill. Until now one column meant both "uses" and "is
-- accountable". The warranty pseudo-owner (SUPPLIER) holds custody, not use.
-- ----------------------------------------------------------------------------
START TRANSACTION;

UPDATE assets
SET AssignedUserID = CurrentOwnerID
WHERE AssignedUserID IS NULL
  AND CurrentOwnerID IS NOT NULL
  AND CurrentOwnerID <> 'SUPPLIER'
  AND DeploymentStatus IN ('Deployed', 'With MISD Technician');

UPDATE fleetvehicles
SET AccountableOwnerID = AssignedDriverID
WHERE AccountableOwnerID IS NULL
  AND AssignedDriverID IS NOT NULL;

COMMIT;


-- ----------------------------------------------------------------------------
-- SECTION 6 - Verification (read-only).
-- ----------------------------------------------------------------------------
SELECT 'VERIFY - personnel by employment status' AS report;
SELECT EmploymentStatus, COUNT(*) AS employees FROM personnel GROUP BY EmploymentStatus;

SELECT 'VERIFY - assets deployed without an assigned user (should be 0)' AS report;
SELECT COUNT(*) AS rows_to_review FROM assets
WHERE DeploymentStatus = 'Deployed' AND AssignedUserID IS NULL;

SELECT 'VERIFY - vehicles with a driver but no accountable owner (should be 0)' AS report;
SELECT COUNT(*) AS rows_to_review FROM fleetvehicles
WHERE AssignedDriverID IS NOT NULL AND AccountableOwnerID IS NULL;
