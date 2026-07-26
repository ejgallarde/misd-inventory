-- Run manually on the MISD inventory database before deploying fleet status changes.
-- Target dialect: MySQL 8+

-- 1) Add new fleet status columns used by the updated fleet UI and backend.
ALTER TABLE FleetVehicles
    ADD COLUMN IF NOT EXISTS AdminLegaltionalStatus VARCHAR(255) NULL;

ALTER TABLE FleetVehicles
    ADD COLUMN IF NOT EXISTS OperationalStatus VARCHAR(255) NULL;

ALTER TABLE FleetVehicles
    ADD COLUMN IF NOT EXISTS MaintenanceStatus VARCHAR(255) NULL;

-- Keep legacy compatibility for code paths/reports still expecting CurrentStatus.
ALTER TABLE FleetVehicles
    ADD COLUMN IF NOT EXISTS CurrentStatus VARCHAR(255) NULL;

-- 2) Align nullable rules for plate and body numbers (plate can be missing).
ALTER TABLE FleetVehicles
    MODIFY COLUMN PlateNumber VARCHAR(255) NULL;

ALTER TABLE FleetVehicles
    MODIFY COLUMN BodyNumber VARCHAR(255) NULL;

-- 3) Backfill values for existing rows.
UPDATE FleetVehicles
SET
    AdminLegaltionalStatus = COALESCE(NULLIF(TRIM(AdminLegaltionalStatus), ''), 'Active / Registered'),
    OperationalStatus = COALESCE(NULLIF(TRIM(OperationalStatus), ''), NULLIF(TRIM(CurrentStatus), ''), 'Available/Idle'),
    MaintenanceStatus = COALESCE(NULLIF(TRIM(MaintenanceStatus), ''), 'Roadworthy');

-- 4) Synchronize legacy status from operational status for backward compatibility.
UPDATE FleetVehicles
SET CurrentStatus = OperationalStatus
WHERE NULLIF(TRIM(OperationalStatus), '') IS NOT NULL;

-- 5) Enforce status columns as required after backfill.
ALTER TABLE FleetVehicles
    MODIFY COLUMN AdminLegaltionalStatus VARCHAR(255) NOT NULL;

ALTER TABLE FleetVehicles
    MODIFY COLUMN OperationalStatus VARCHAR(255) NOT NULL;

ALTER TABLE FleetVehicles
    MODIFY COLUMN MaintenanceStatus VARCHAR(255) NOT NULL;
