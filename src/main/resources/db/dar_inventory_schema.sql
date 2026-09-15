-- dar_inventory_schema.sql
--
-- Authoritative DDL for the DAR deployment's `dar_inventory` database
-- (temp-dar-split-branding branch). No such file existed before this;
-- the schema previously only lived as CREATE TABLE statements baked into
-- src/main/resources/migrated-db/*.sql (mysqldump output). Those baked-in
-- definitions were the source for this file, with the following changes:
--
--   - personnel: ManagerID column/FK/index removed (unused org-hierarchy
--     concept, always NULL in this dataset - replaced by EndUserID below).
--   - assets, fleetvehicles, surveyassets: added EndUserID (FK -> personnel,
--     mirrors CurrentOwnerID/AssignedDriverID/AssignedCustodianID exactly)
--     plus CurrentValue, DepreciationAmount, ValuationAsOfDate for the
--     persisted straight-line depreciation calculation.
--   - assetassignments: added EndUserID (FK -> personnel) so one audit row
--     can record both who was accountable and who was using the asset at
--     that point in time. ReferenceType/DocumentNo already existed in the
--     baked-in dump schema; kept as-is here.
--
-- Run this against an empty `dar_inventory` database:
--   mysql -u<user> -p dar_inventory < dar_inventory_schema.sql

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ===================== locations =====================
CREATE TABLE `locations` (
  `LocationID` int NOT NULL AUTO_INCREMENT,
  `Area` varchar(50) NOT NULL,
  `Province` varchar(50) NOT NULL,
  `OfficeAddress` varchar(255) NOT NULL,
  PRIMARY KEY (`LocationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== personnel =====================
-- ManagerID column intentionally removed - see header note.
CREATE TABLE `personnel` (
  `EmployeeID` varchar(20) NOT NULL,
  `FirstName` varchar(50) NOT NULL,
  `LastName` varchar(50) NOT NULL,
  `JobTitle` varchar(100) DEFAULT NULL,
  `Department` varchar(100) DEFAULT NULL,
  `Division` varchar(100) DEFAULT NULL,
  `BaseLocationID` int DEFAULT NULL,
  PRIMARY KEY (`EmployeeID`),
  KEY `BaseLocationID` (`BaseLocationID`),
  CONSTRAINT `personnel_ibfk_2` FOREIGN KEY (`BaseLocationID`) REFERENCES `locations` (`LocationID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== equipmentcatalog =====================
CREATE TABLE `equipmentcatalog` (
  `CatalogID` int NOT NULL AUTO_INCREMENT,
  `Category` varchar(50) NOT NULL,
  `Manufacturer` varchar(50) NOT NULL,
  `ModelName` varchar(100) NOT NULL,
  `Specifications` json DEFAULT NULL,
  PRIMARY KEY (`CatalogID`),
  UNIQUE KEY `uq_equipmentcatalog_model` (`Category`,`Manufacturer`,`ModelName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== fleetvehiclecatalog =====================
CREATE TABLE `fleetvehiclecatalog` (
  `CatalogID` int NOT NULL AUTO_INCREMENT,
  `Category` varchar(100) NOT NULL,
  `Manufacturer` varchar(100) NOT NULL,
  `ModelName` varchar(100) NOT NULL,
  `YearModel` int NOT NULL,
  `FuelType` varchar(50) NOT NULL,
  PRIMARY KEY (`CatalogID`),
  UNIQUE KEY `uq_fleetvehiclecatalog_model` (`Category`,`Manufacturer`,`ModelName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== surveyequipmentcatalog =====================
CREATE TABLE `surveyequipmentcatalog` (
  `CatalogID` int NOT NULL AUTO_INCREMENT,
  `Category` varchar(100) NOT NULL,
  `Manufacturer` varchar(100) NOT NULL,
  `ModelName` varchar(100) NOT NULL,
  `Specifications` json DEFAULT NULL,
  PRIMARY KEY (`CatalogID`),
  UNIQUE KEY `uq_surveyequipmentcatalog_model` (`Category`,`Manufacturer`,`ModelName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== assets =====================
-- Added: EndUserID, CurrentValue, DepreciationAmount, ValuationAsOfDate.
CREATE TABLE `assets` (
  `AssetTag` varchar(50) NOT NULL,
  `PropertyNumber` varchar(100) DEFAULT NULL,
  `BundledWithAssetTag` varchar(50) DEFAULT NULL,
  `CatalogID` int NOT NULL,
  `SerialNumber` varchar(100) DEFAULT NULL,
  `PurchaseDate` date DEFAULT NULL,
  `PurchasePrice` decimal(10,2) DEFAULT NULL,
  `CurrentOwnerID` varchar(20) DEFAULT NULL,
  `EndUserID` varchar(20) DEFAULT NULL,
  `CurrentValue` decimal(15,2) DEFAULT NULL,
  `DepreciationAmount` decimal(15,2) DEFAULT NULL,
  `ValuationAsOfDate` date DEFAULT NULL,
  `Remarks` text,
  `DeploymentStatus` varchar(255) NOT NULL,
  `MaintenanceHealthStatus` varchar(255) NOT NULL,
  `LifecycleStatus` varchar(255) NOT NULL,
  PRIMARY KEY (`AssetTag`),
  UNIQUE KEY `SerialNumber` (`SerialNumber`),
  KEY `CatalogID` (`CatalogID`),
  KEY `CurrentOwnerID` (`CurrentOwnerID`),
  KEY `EndUserID` (`EndUserID`),
  KEY `idx_assets_propertynumber` (`PropertyNumber`),
  KEY `idx_assets_bundledwith` (`BundledWithAssetTag`),
  CONSTRAINT `assets_ibfk_1` FOREIGN KEY (`CatalogID`) REFERENCES `equipmentcatalog` (`CatalogID`) ON DELETE RESTRICT,
  CONSTRAINT `assets_ibfk_2` FOREIGN KEY (`CurrentOwnerID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL,
  CONSTRAINT `assets_ibfk_3` FOREIGN KEY (`BundledWithAssetTag`) REFERENCES `assets` (`AssetTag`) ON DELETE SET NULL,
  CONSTRAINT `assets_ibfk_4` FOREIGN KEY (`EndUserID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== fleetvehicles =====================
-- Added: EndUserID, CurrentValue, DepreciationAmount, ValuationAsOfDate.
CREATE TABLE `fleetvehicles` (
  `VehicleID` int NOT NULL AUTO_INCREMENT,
  `PropertyNumber` varchar(100) DEFAULT NULL,
  `CatalogID` int NOT NULL,
  `PlateNumber` varchar(255) DEFAULT NULL,
  `EngineNumber` varchar(255) DEFAULT NULL,
  `ChassisNumberVIN` varchar(255) DEFAULT NULL,
  `RegistrationExpiry` date DEFAULT NULL,
  `InsuranceExpiry` date DEFAULT NULL,
  `AssignedDriverID` varchar(20) DEFAULT NULL,
  `EndUserID` varchar(20) DEFAULT NULL,
  `BodyNumber` varchar(255) DEFAULT NULL,
  `Cost` decimal(15,2) DEFAULT NULL,
  `AcquisitionYear` int DEFAULT NULL,
  `CurrentValue` decimal(15,2) DEFAULT NULL,
  `DepreciationAmount` decimal(15,2) DEFAULT NULL,
  `ValuationAsOfDate` date DEFAULT NULL,
  `Remarks` text,
  `AdminLegaltionalStatus` varchar(255) DEFAULT NULL,
  `OperationalStatus` varchar(255) DEFAULT NULL,
  `MaintenanceStatus` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`VehicleID`),
  UNIQUE KEY `PlateNumber` (`PlateNumber`),
  UNIQUE KEY `EngineNumber` (`EngineNumber`),
  UNIQUE KEY `ChassisNumberVIN` (`ChassisNumberVIN`),
  UNIQUE KEY `BodyNumber` (`BodyNumber`),
  KEY `AssignedDriverID` (`AssignedDriverID`),
  KEY `EndUserID` (`EndUserID`),
  KEY `CatalogID` (`CatalogID`),
  KEY `idx_fleetvehicles_propertynumber` (`PropertyNumber`),
  CONSTRAINT `fleetvehicles_ibfk_1` FOREIGN KEY (`AssignedDriverID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL,
  CONSTRAINT `fleetvehicles_ibfk_2` FOREIGN KEY (`CatalogID`) REFERENCES `fleetvehiclecatalog` (`CatalogID`) ON DELETE RESTRICT,
  CONSTRAINT `fleetvehicles_ibfk_3` FOREIGN KEY (`EndUserID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== surveyassets =====================
-- Added: EndUserID, CurrentValue, DepreciationAmount, ValuationAsOfDate.
CREATE TABLE `surveyassets` (
  `SurveyAssetID` int NOT NULL AUTO_INCREMENT,
  `PropertyNumber` varchar(100) DEFAULT NULL,
  `CatalogID` int NOT NULL,
  `AssetTag` varchar(50) DEFAULT NULL,
  `SerialNumber` varchar(255) DEFAULT NULL,
  `AcquisitionDate` date DEFAULT NULL,
  `Cost` decimal(15,2) DEFAULT NULL,
  `CalibrationDueDate` date DEFAULT NULL,
  `LastCalibrationDate` date DEFAULT NULL,
  `AssignedCustodianID` varchar(20) DEFAULT NULL,
  `EndUserID` varchar(20) DEFAULT NULL,
  `CurrentValue` decimal(15,2) DEFAULT NULL,
  `DepreciationAmount` decimal(15,2) DEFAULT NULL,
  `ValuationAsOfDate` date DEFAULT NULL,
  `AdminLegalStatus` varchar(255) DEFAULT NULL,
  `OperationalStatus` varchar(255) DEFAULT NULL,
  `ConditionStatus` varchar(255) DEFAULT NULL,
  `Remarks` text,
  PRIMARY KEY (`SurveyAssetID`),
  UNIQUE KEY `AssetTag` (`AssetTag`),
  UNIQUE KEY `SerialNumber` (`SerialNumber`),
  KEY `AssignedCustodianID` (`AssignedCustodianID`),
  KEY `EndUserID` (`EndUserID`),
  KEY `CatalogID` (`CatalogID`),
  KEY `idx_surveyassets_propertynumber` (`PropertyNumber`),
  CONSTRAINT `surveyassets_ibfk_1` FOREIGN KEY (`AssignedCustodianID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL,
  CONSTRAINT `surveyassets_ibfk_2` FOREIGN KEY (`CatalogID`) REFERENCES `surveyequipmentcatalog` (`CatalogID`) ON DELETE RESTRICT,
  CONSTRAINT `surveyassets_ibfk_3` FOREIGN KEY (`EndUserID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== assetassignments =====================
-- ReferenceType/DocumentNo already existed in the baked-in dump schema but
-- were unmapped by the JPA entity (fixed in AssetAssignmentLog.java, not
-- here). Added: EndUserID, so one row can capture both roles at once.
CREATE TABLE `assetassignments` (
  `TransactionID` int NOT NULL AUTO_INCREMENT,
  `ReferenceType` varchar(20) NOT NULL DEFAULT 'ASSET',
  `AssetTag` varchar(50) NOT NULL,
  `EmployeeID` varchar(20) NOT NULL,
  `EndUserID` varchar(20) DEFAULT NULL,
  `ActionType` varchar(100) DEFAULT NULL,
  `DocumentNo` varchar(100) DEFAULT NULL,
  `TransactionDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `ConditionNotes` text,
  PRIMARY KEY (`TransactionID`),
  KEY `AssetTag` (`AssetTag`),
  KEY `assetassignments_ibfk_2` (`EmployeeID`),
  KEY `EndUserID` (`EndUserID`),
  KEY `idx_assetassignments_referencetype` (`ReferenceType`,`AssetTag`),
  CONSTRAINT `assetassignments_ibfk_2` FOREIGN KEY (`EmployeeID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE RESTRICT,
  CONSTRAINT `assetassignments_ibfk_3` FOREIGN KEY (`EndUserID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===================== assetdocument =====================
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

-- ===================== lifecycleauditlog =====================
CREATE TABLE `lifecycleauditlog` (
  `LogID` int NOT NULL AUTO_INCREMENT,
  `ReferenceID` varchar(100) NOT NULL,
  `ActionType` varchar(100) NOT NULL,
  `PerformedBy` varchar(100) NOT NULL,
  `TransactionDate` datetime NOT NULL,
  `Notes` text,
  PRIMARY KEY (`LogID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
