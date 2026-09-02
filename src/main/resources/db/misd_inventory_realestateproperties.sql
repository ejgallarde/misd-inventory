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
-- Table structure for table `realestateproperties`
--

-- Reference only. The DROP TABLE statement mysqldump emits here has been
-- removed: running this file against a populated database would have
-- destroyed `realestateproperties` and everything cascading from it, with no prompt.
-- To recreate the table from scratch, drop it deliberately first.
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `realestateproperties` (
  `PropertyID` int NOT NULL AUTO_INCREMENT,
  `PropertyType` varchar(50) NOT NULL,
  `PropertyName` varchar(150) NOT NULL,
  `Area` varchar(50) DEFAULT NULL,
  `TitleNumber` varchar(100) DEFAULT NULL,
  `AddressLine1` varchar(255) DEFAULT NULL,
  `AddressLine2` varchar(255) DEFAULT NULL,
  `Province` varchar(120) DEFAULT NULL,
  `City` varchar(120) DEFAULT NULL,
  `Barangay` varchar(120) DEFAULT NULL,
  `ZipCode` varchar(10) DEFAULT NULL,
  `LotAreaSqm` decimal(10,2) DEFAULT NULL,
  `FloorAreaSqm` decimal(10,2) DEFAULT NULL,
  `AcquisitionDate` date DEFAULT NULL,
  `AssessedValue` decimal(15,2) DEFAULT NULL,
  `PropertyTaxStatus` varchar(50) DEFAULT NULL,
  `CustodianID` varchar(20) DEFAULT NULL,
  `Remarks` text,
  `TaxDeclarationNumber` varchar(255) DEFAULT NULL,
  `SurveyPlanNumber` varchar(150) DEFAULT NULL,
  `PropertyDetails` text,
  `LegalTitlingStatus` varchar(255) DEFAULT NULL,
  `OperationalStatus` varchar(255) DEFAULT NULL,
  `ConditionStatus` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PropertyID`),
  UNIQUE KEY `TitleNumber` (`TitleNumber`),
  UNIQUE KEY `UK_RealEstateProperties_TaxDeclarationNumber` (`TaxDeclarationNumber`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-01  3:18:05
