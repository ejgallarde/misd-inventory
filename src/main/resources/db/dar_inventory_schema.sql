-- DAR Inventory schema
--
-- Fresh schema for the DAR (Department of Agrarian Reform) deployment of this
-- application. Separate from, and not derived by migration from, the original
-- MISD `misd_inventory` database documented by the `misd_inventory_*.sql`
-- reference dumps in this same folder. This DAR deployment tracks only three
-- asset domains -- IT equipment, fleet vehicles, and survey equipment -- so the
-- real-estate (`realestateproperties`) and PSGC province/city/barangay tables
-- from the original schema are intentionally NOT included here.
--
-- Column definitions for `personnel`, `locations`, `equipmentcatalog`,
-- `assets`, `fleetvehicles`, `assetdocument`, `lifecycleauditlog`, and
-- `assetassignments` are carried over verbatim from the MISD reference dumps
-- so the Java entities in this codebase map onto either database unchanged.
-- `surveyassets` is new, added for the DAR-specific Survey Assets module.
--
-- Usage:
--   mysql -u<user> -p -e "CREATE DATABASE IF NOT EXISTS dar_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
--   mysql -u<user> -p dar_inventory < src/main/resources/db/dar_inventory_schema.sql

CREATE DATABASE IF NOT EXISTS `dar_inventory` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `dar_inventory`;

-- ---------------------------------------------------------------------------
-- Shared reference/lookup tables
-- ---------------------------------------------------------------------------

CREATE TABLE `locations` (
  `LocationID` int NOT NULL AUTO_INCREMENT,
  `Area` varchar(50) NOT NULL,
  `Province` varchar(50) NOT NULL,
  `OfficeAddress` varchar(255) NOT NULL,
  PRIMARY KEY (`LocationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `personnel` (
  `EmployeeID` varchar(20) NOT NULL,
  `FirstName` varchar(50) NOT NULL,
  `LastName` varchar(50) NOT NULL,
  `JobTitle` varchar(100) DEFAULT NULL,
  `Department` varchar(100) DEFAULT NULL,
  `Division` varchar(100) DEFAULT NULL,
  `ManagerID` varchar(20) DEFAULT NULL,
  `BaseLocationID` int DEFAULT NULL,
  PRIMARY KEY (`EmployeeID`),
  KEY `ManagerID` (`ManagerID`),
  KEY `BaseLocationID` (`BaseLocationID`),
  CONSTRAINT `personnel_ibfk_1` FOREIGN KEY (`ManagerID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL,
  CONSTRAINT `personnel_ibfk_2` FOREIGN KEY (`BaseLocationID`) REFERENCES `locations` (`LocationID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `equipmentcatalog` (
  `CatalogID` int NOT NULL AUTO_INCREMENT,
  `Category` varchar(50) NOT NULL,
  `Manufacturer` varchar(50) NOT NULL,
  `ModelName` varchar(100) NOT NULL,
  `Specifications` json DEFAULT NULL,
  PRIMARY KEY (`CatalogID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `assetdocument` (
  `DocumentId` int NOT NULL AUTO_INCREMENT,
  `ReferenceType` varchar(50) NOT NULL,
  `ReferenceId` varchar(255) NOT NULL,
  `DocumentCategory` varchar(100) NOT NULL,
  `FileName` varchar(255) NOT NULL,
  `MinioObjectKey` varchar(500) NOT NULL,
  `ContentType` varchar(100) DEFAULT NULL,
  `FileSize` bigint DEFAULT NULL,
  `UploadedBy` varchar(255) DEFAULT NULL,
  `UploadDate` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`DocumentId`),
  KEY `idx_asset_reference` (`ReferenceType`,`ReferenceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `lifecycleauditlog` (
  `LogID` int NOT NULL AUTO_INCREMENT,
  `ReferenceID` varchar(100) NOT NULL,
  `ActionType` varchar(100) NOT NULL,
  `PerformedBy` varchar(100) NOT NULL,
  `TransactionDate` datetime NOT NULL,
  `Notes` text,
  PRIMARY KEY (`LogID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- IT Assets
-- ---------------------------------------------------------------------------

CREATE TABLE `assets` (
  `AssetTag` varchar(50) NOT NULL,
  `CatalogID` int NOT NULL,
  `SerialNumber` varchar(100) DEFAULT NULL,
  `PurchaseDate` date DEFAULT NULL,
  `PurchasePrice` decimal(10,2) DEFAULT NULL,
  `CurrentOwnerID` varchar(20) DEFAULT NULL,
  `Remarks` text,
  `DeploymentStatus` varchar(255) NOT NULL,
  `MaintenanceHealthStatus` varchar(255) NOT NULL,
  `LifecycleStatus` varchar(255) NOT NULL,
  PRIMARY KEY (`AssetTag`),
  UNIQUE KEY `SerialNumber` (`SerialNumber`),
  KEY `CatalogID` (`CatalogID`),
  KEY `CurrentOwnerID` (`CurrentOwnerID`),
  CONSTRAINT `assets_ibfk_1` FOREIGN KEY (`CatalogID`) REFERENCES `equipmentcatalog` (`CatalogID`) ON DELETE RESTRICT,
  CONSTRAINT `assets_ibfk_2` FOREIGN KEY (`CurrentOwnerID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `assetassignments` (
  `TransactionID` int NOT NULL AUTO_INCREMENT,
  `AssetTag` varchar(50) NOT NULL,
  `EmployeeID` varchar(20) NOT NULL,
  `ActionType` varchar(100) DEFAULT NULL,
  `TransactionDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `ConditionNotes` text,
  PRIMARY KEY (`TransactionID`),
  KEY `AssetTag` (`AssetTag`),
  KEY `assetassignments_ibfk_2` (`EmployeeID`),
  CONSTRAINT `assetassignments_ibfk_2` FOREIGN KEY (`EmployeeID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Fleet Management
--
-- Mirrors the IT/Survey Assets catalog/asset split: `fleetvehiclecatalog`
-- defines a reusable VehicleType+Make+Model (+ optional specs) once, and each
-- `fleetvehicles` row is one physical vehicle pointing at a catalog entry via
-- CatalogID. Unlike IT/Survey, there is no batch/quantity receiving for
-- vehicles -- PlateNumber, EngineNumber, ChassisNumberVIN and BodyNumber are
-- all real-world unique identifiers issued externally, not internal tags that
-- can be auto-generated, so vehicles are still registered one at a time.
--
-- Anyone with an existing local dev DB from before this change needs to
-- either drop and re-run this whole script, or apply by hand:
--   CREATE TABLE `fleetvehiclecatalog` ( ... as below ... );
--   ALTER TABLE `fleetvehicles`
--     ADD COLUMN `CatalogID` int NOT NULL AFTER `VehicleID`,
--     DROP COLUMN `VehicleType`,
--     DROP COLUMN `Make`,
--     DROP COLUMN `Model`,
--     ADD KEY `CatalogID` (`CatalogID`),
--     ADD CONSTRAINT `fleetvehicles_ibfk_2` FOREIGN KEY (`CatalogID`)
--       REFERENCES `fleetvehiclecatalog` (`CatalogID`) ON DELETE RESTRICT;
-- (Only safe on a table with no rows yet, or after backfilling CatalogID --
-- ADD COLUMN ... NOT NULL with no default fails on a populated table.)
-- ---------------------------------------------------------------------------

CREATE TABLE `fleetvehiclecatalog` (
  `CatalogID` int NOT NULL AUTO_INCREMENT,
  `Category` varchar(100) NOT NULL,
  `Manufacturer` varchar(100) NOT NULL,
  `ModelName` varchar(100) NOT NULL,
  `Specifications` json DEFAULT NULL,
  PRIMARY KEY (`CatalogID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `fleetvehicles` (
  `VehicleID` int NOT NULL AUTO_INCREMENT,
  `CatalogID` int NOT NULL,
  `PlateNumber` varchar(255) DEFAULT NULL,
  `ManufactureYear` int DEFAULT NULL,
  `EngineNumber` varchar(255) DEFAULT NULL,
  `ChassisNumberVIN` varchar(255) DEFAULT NULL,
  `FuelType` varchar(255) DEFAULT NULL,
  `RegistrationExpiry` date DEFAULT NULL,
  `InsuranceExpiry` date DEFAULT NULL,
  `AssignedDriverID` varchar(20) DEFAULT NULL,
  `BodyNumber` varchar(255) DEFAULT NULL,
  `Cost` decimal(15,2) DEFAULT NULL,
  `AcquisitionYear` int DEFAULT NULL,
  `Remarks` text,
  `AdminLegaltionalStatus` varchar(255) DEFAULT NULL,
  `OperationalStatus` varchar(255) DEFAULT NULL,
  `MaintenanceStatus` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`VehicleID`),
  UNIQUE KEY `PlateNumber` (`PlateNumber`),
  UNIQUE KEY `EngineNumber` (`EngineNumber`),
  UNIQUE KEY `ChassisNumberVIN` (`ChassisNumberVIN`),
  UNIQUE KEY `BodyNumber` (`BodyNumber`),
  KEY `CatalogID` (`CatalogID`),
  KEY `AssignedDriverID` (`AssignedDriverID`),
  CONSTRAINT `fleetvehicles_ibfk_1` FOREIGN KEY (`AssignedDriverID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL,
  CONSTRAINT `fleetvehicles_ibfk_2` FOREIGN KEY (`CatalogID`) REFERENCES `fleetvehiclecatalog` (`CatalogID`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Survey Assets (new for DAR)
--
-- Mirrors the IT Assets catalog/asset split above: `surveyequipmentcatalog`
-- defines a reusable Type+Manufacturer+Model (+ optional specs) once, and each
-- `surveyassets` row is one physical unit pointing at a catalog entry via
-- CatalogID, the same way `assets.CatalogID` points at `equipmentcatalog`.
--
-- Anyone with an existing local dev DB from before this change (no production
-- data expected yet) needs to either drop and re-run this whole script, or
-- apply by hand:
--   CREATE TABLE `surveyequipmentcatalog` ( ... as below ... );
--   ALTER TABLE `surveyassets`
--     ADD COLUMN `CatalogID` int NOT NULL AFTER `SurveyAssetID`,
--     DROP COLUMN `SurveyAssetType`,
--     DROP COLUMN `Manufacturer`,
--     DROP COLUMN `ModelName`,
--     ADD KEY `CatalogID` (`CatalogID`),
--     ADD CONSTRAINT `surveyassets_ibfk_2` FOREIGN KEY (`CatalogID`)
--       REFERENCES `surveyequipmentcatalog` (`CatalogID`) ON DELETE RESTRICT;
-- (Only safe on a table with no rows yet, or after backfilling CatalogID --
-- ADD COLUMN ... NOT NULL with no default fails on a populated table.)
-- ---------------------------------------------------------------------------

CREATE TABLE `surveyequipmentcatalog` (
  `CatalogID` int NOT NULL AUTO_INCREMENT,
  `Category` varchar(100) NOT NULL,
  `Manufacturer` varchar(100) NOT NULL,
  `ModelName` varchar(100) NOT NULL,
  `Specifications` json DEFAULT NULL,
  PRIMARY KEY (`CatalogID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `surveyassets` (
  `SurveyAssetID` int NOT NULL AUTO_INCREMENT,
  `CatalogID` int NOT NULL,
  `AssetTag` varchar(50) DEFAULT NULL,
  `SerialNumber` varchar(255) DEFAULT NULL,
  `AcquisitionDate` date DEFAULT NULL,
  `Cost` decimal(15,2) DEFAULT NULL,
  `CalibrationDueDate` date DEFAULT NULL,
  `LastCalibrationDate` date DEFAULT NULL,
  `AssignedCustodianID` varchar(20) DEFAULT NULL,
  `AdminLegalStatus` varchar(255) DEFAULT NULL,
  `OperationalStatus` varchar(255) DEFAULT NULL,
  `ConditionStatus` varchar(255) DEFAULT NULL,
  `Remarks` text,
  PRIMARY KEY (`SurveyAssetID`),
  UNIQUE KEY `AssetTag` (`AssetTag`),
  UNIQUE KEY `SerialNumber` (`SerialNumber`),
  KEY `CatalogID` (`CatalogID`),
  KEY `AssignedCustodianID` (`AssignedCustodianID`),
  CONSTRAINT `surveyassets_ibfk_1` FOREIGN KEY (`AssignedCustodianID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL,
  CONSTRAINT `surveyassets_ibfk_2` FOREIGN KEY (`CatalogID`) REFERENCES `surveyequipmentcatalog` (`CatalogID`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
