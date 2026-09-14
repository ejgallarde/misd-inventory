-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: dar_inventory
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
-- Table structure for table `surveyassets`
--

DROP TABLE IF EXISTS `surveyassets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `AdminLegalStatus` varchar(255) DEFAULT NULL,
  `OperationalStatus` varchar(255) DEFAULT NULL,
  `ConditionStatus` varchar(255) DEFAULT NULL,
  `Remarks` text,
  PRIMARY KEY (`SurveyAssetID`),
  UNIQUE KEY `AssetTag` (`AssetTag`),
  UNIQUE KEY `SerialNumber` (`SerialNumber`),
  KEY `AssignedCustodianID` (`AssignedCustodianID`),
  KEY `CatalogID` (`CatalogID`),
  KEY `idx_surveyassets_propertynumber` (`PropertyNumber`),
  CONSTRAINT `surveyassets_ibfk_1` FOREIGN KEY (`AssignedCustodianID`) REFERENCES `personnel` (`EmployeeID`) ON DELETE SET NULL,
  CONSTRAINT `surveyassets_ibfk_2` FOREIGN KEY (`CatalogID`) REFERENCES `surveyequipmentcatalog` (`CatalogID`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `surveyassets`
--

LOCK TABLES `surveyassets` WRITE;
/*!40000 ALTER TABLE `surveyassets` DISABLE KEYS */;
/*!40000 ALTER TABLE `surveyassets` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-14 13:21:35
