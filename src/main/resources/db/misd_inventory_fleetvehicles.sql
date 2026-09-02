-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: misd_inventory
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `fleetvehicles`
--

-- Reference only. The DROP TABLE statement mysqldump emits here has been
-- removed: running this file against a populated database would have
-- destroyed `fleetvehicles` and everything cascading from it, with no prompt.
-- To recreate the table from scratch, drop it deliberately first.
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fleetvehicles` (
  `VehicleID` int NOT NULL AUTO_INCREMENT,
  `PlateNumber` varchar(255) DEFAULT NULL,
  `VehicleType` varchar(255) DEFAULT NULL,
  `Make` varchar(255) DEFAULT NULL,
  `Model` varchar(255) DEFAULT NULL,
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
  UNIQUE KEY `body_number` (`BodyNumber`),
  UNIQUE KEY `BodyNumber` (`BodyNumber`),
  KEY `AssignedDriverID` (`AssignedDriverID`),
  CONSTRAINT `fleetvehicles_ibfk_1` FOREIGN KEY (`AssignedDriverID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-01  3:18:04
