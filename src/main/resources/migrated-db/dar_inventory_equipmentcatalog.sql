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
-- Table structure for table `equipmentcatalog`
--

DROP TABLE IF EXISTS `equipmentcatalog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipmentcatalog` (
  `CatalogID` int NOT NULL AUTO_INCREMENT,
  `Category` varchar(50) NOT NULL,
  `Manufacturer` varchar(50) NOT NULL,
  `ModelName` varchar(100) NOT NULL,
  `Specifications` json DEFAULT NULL,
  PRIMARY KEY (`CatalogID`),
  UNIQUE KEY `uq_equipmentcatalog_model` (`Category`,`Manufacturer`,`ModelName`)
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `equipmentcatalog`
--

LOCK TABLES `equipmentcatalog` WRITE;
/*!40000 ALTER TABLE `equipmentcatalog` DISABLE KEYS */;
INSERT INTO `equipmentcatalog` VALUES (1,'Desktop','Lenovo','M70T','{\"Ports\": \"Front: USB-C, USB-A ports, headphone/mic combo jack, optional card reader\", \"Memory (RAM)\": \"Up to 64GB DDR4 or DDR5 UDIMM\", \"Processor (CPU)\": \"Up to Intel Core i5/i7\", \"Storage (SSD/HDD)\": \"M.2 PCIe NVMe SSD + optional 3.5-inch SATA HDD\"}'),(2,'Desktop','HP','Pro SFF 280G9','{\"Memory (RAM)\": \"Up to DDR4-3200 MT/s, 2 DIMM slots\", \"Processor (CPU)\": \"Up to 14th Gen like i5-14500\", \"Dimensions/Weight\": \"9.5 x 30.3 x 27 cm, ~4.2 kg\", \"Storage (SSD/HDD)\": \"M.2 PCIe NVMe SSD (2242/2280) + one 3.5\\\" HDD bay\"}'),(3,'Laptop','Acer','Extensa 15 (EX215-55)','{\"Memory (RAM)\": \"8GB DDR4 (onboard + free slot, expandable up to 32GB / dual-channel support)\", \"Graphics (GPU)\": \"Intel UHD / Iris Xe Graphics (some configurations feature NVIDIA GeForce MX550 2GB GDDR6)\", \"Processor (CPU)\": \"Intel Core i7-1255U\", \"Storage (SSD/HDD)\": \"512GB or 1TB M.2 PCIe NVMe SSD\"}'),(4,'Desktop Computer','MSI','PRO DP180',NULL),(5,'Laptop','Acer','TravelMate',NULL),(6,'Printer','Brother','A3 Printer',NULL),(7,'Photocopier','Kyocera','Photocopier Machine',NULL),(8,'Plotter','Unspecified','Plotter (3-in-1)',NULL),(9,'Printer','Unspecified','Bundled Printer (accessory - see assets.BundledWithAssetTag)',NULL),(10,'UPS','Unspecified','Bundled UPS (accessory - see assets.BundledWithAssetTag)',NULL),(11,'Monitor','Unspecified','Bundled Monitor (accessory - see assets.BundledWithAssetTag)',NULL),(72,'Laptop','MSI','Katana',NULL),(76,'Printer','HP','Unspecified',NULL),(77,'Server','Unspecified','Unspecified',NULL);
/*!40000 ALTER TABLE `equipmentcatalog` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-14 23:32:16
