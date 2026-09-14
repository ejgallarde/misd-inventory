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
-- Table structure for table `locations`
--

DROP TABLE IF EXISTS `locations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `locations` (
  `LocationID` int NOT NULL AUTO_INCREMENT,
  `Area` varchar(50) NOT NULL,
  `Province` varchar(50) NOT NULL,
  `OfficeAddress` varchar(255) NOT NULL,
  PRIMARY KEY (`LocationID`)
) ENGINE=InnoDB AUTO_INCREMENT=588 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `locations`
--

LOCK TABLES `locations` WRITE;
/*!40000 ALTER TABLE `locations` DISABLE KEYS */;
INSERT INTO `locations` VALUES (1,'DAR Central Office','National Capital Region','Elliptical Road, Quezon City'),(470,'DARCO','Central Office','Osec'),(471,'CAR','Regional Office','DARRO'),(472,'CAR','Abra','DARRO'),(473,'CAR','Apayao','DARRO'),(474,'CAR','Benguet','DARRO'),(475,'CAR','Ifugao','DARPO'),(476,'CAR','Kalinga','DARPO'),(477,'CAR','Mt. Province','Mt. Province'),(478,'CAR','Mt Province','Mt Province'),(479,'Region I','Ilocos Sur','DARPO Ilocos Norte'),(480,'Region I','Pangasinan','DARPO Pangasinan'),(481,'Region I','La Union','DARPO La Union'),(482,'Region I','Ilocos Norte','DARPO Ilocos Sur'),(483,'Region I','Regional Office','DARPO Ilocos Norte'),(484,'Region I','Regional Office I','Regional Office I'),(485,'Region I','Unknown','Unknown'),(486,'Region II','Regional Office','Stod'),(487,'Region II','Batanes','Stod'),(488,'Region II','Cagayan','Split'),(489,'Region II','Nueva Vizcaya','Ltid'),(490,'Region II','Isabela','DARPO'),(491,'Region II','Quirino','Lts'),(492,'Region III','Isabela','DARMO Santiago'),(493,'Region III','Quirino','Gss'),(494,'Region II','Unknown','Unknown'),(495,'Region III','Regional Office','RDs Office'),(496,'Region III','Aurora','Lts'),(497,'Region III','Bataan','Ltsp'),(498,'Region III','Zambales','Stod'),(499,'Region III','Bulacan','Ltid'),(500,'Region III','Nueva Ecija','Ltid'),(501,'Region III','Pampanga','Stod'),(502,'Region III','Tarlac','DARPO - Tarlac Stod'),(503,'Region IV-A','Batangas','Ltid'),(504,'Region IV-A','Quezon I','DARPO Quezon I'),(505,'Region IV-A','Quezon Ii','Ltid'),(506,'Region IV-A','Rizal','Lts/survey Unit'),(507,'Region IV-A','Regional Office','Planning'),(508,'Region IV-A','Cavite','Stod'),(509,'Region IV-A','Laguna','Ltiu'),(510,'Region IV-A','Unknown','Unknown'),(511,'Region IV-B','Regional Office','Fod'),(512,'Region IV-B','Palawan','Ltspd'),(513,'Region IV-B','Romblon','Parpo\'s Office'),(514,'Region IV-B','Marinduque','Ltspd'),(515,'Region IV-B','Occidental Mindoro','Sto'),(516,'Region IV-B','Oriental Mindoro','Stod'),(517,'REGION V','DARROV','Ltid'),(518,'REGION V','Albay','DARPO'),(519,'REGION V','Camarines Norte','Sarpo'),(520,'REGION V','Cam. Sur 1','Arpo I/cs1 Pio'),(521,'REGION V','Camrines Sur Ii','Survey Office'),(522,'REGION V','Catanduanes','PARPO\'s Office'),(523,'REGION V','Masbate','Darpo-survey'),(524,'REGION V','Sorsogon','Ltid-survey'),(525,'NCR','DENR','DENR'),(526,'REGION V','Unknown','Unknown'),(527,'REGION V','Camarines Sur I','Camarines Sur I'),(528,'REGION V','Camarines Sur Ii','Camarines Sur Ii'),(529,'Region VI','Regional Office','LGU Passi'),(530,'Region VI','Iloilo','Ltid'),(531,'Region VI','Capiz','Parpo I'),(532,'Region VI','Antique','Darpo-stod'),(533,'Region VI','Aklan','Stod'),(534,'Region VI','Guimaras','Legal'),(535,'NIR','Negros Occidental II','Split Office'),(536,'NIR','Negros Occidental I','Satellite Office'),(537,'NIR','Negros Oriental','Paro I'),(538,'NIR','Siquijor','DARPO Siquijor'),(539,'Region VII','Regional Office','Regional Office'),(540,'Region VII','Bohol','Darpo-bohol'),(541,'Region VII','Cebu','Darpo- Cebu'),(542,'REGION VIII','Regional Office','Lts'),(543,'REGION VIII','Biliran','STO - Supply'),(544,'REGION VIII','Eastern Samar','Lts'),(545,'REGION VIII','Northern Samar','Ltsp'),(546,'REGION VIII','Western Samar','Ltsp'),(547,'REGION VIII','Leyte','Ltsp'),(548,'REGION VIII','Southern Leyte','Darpo-ltid'),(549,'REGION IX','Regional Office','RD\'s OFFICE'),(550,'REGION IX','Zamboanga Sibugay','Stod/supply Section'),(551,'REGION IX','Zamboanga del Norte','Darpo-stod'),(552,'REGION IX','Zamboanga del Sur','Darpo-stod'),(553,'Region X','Regional Office','DAR Regional Officex'),(554,'Region X','Misamis Oriental','DAR Regional Officex'),(555,'Region X','Lanao del Norte','DAR Regional Officex'),(556,'Region X','Misami9s Occidental','DAR Regional Officex'),(557,'Region X','Bukidnon','DAR Regional Officex'),(558,'Region X','Camiguin','Dar-mis. Or/cam'),(559,'Region X','Misamis Occidental','Darro-arbdsp'),(560,'R-X','Bukidnon','DAR Regional Officex'),(561,'Region X','Unknown','Unknown'),(562,'Region X','Bukinon','DARPO'),(563,'Region XI','Regional Office','Oardo'),(564,'Region XI','Davao City','Ltid'),(565,'Region XI','Davao de Oro','DARPO Davao de Oro'),(566,'Region XI','Davao Occidental','DARMO DON MARCELINO & JAS'),(567,'Region XI','Davao del Norte','Maro New Corella'),(568,'Region XI','Davao del Sur','DARPO Davao Sur'),(569,'Region XI','Davao Oriental','DARPO Davao Oriental'),(570,'Region XI','Unknown','Unknown'),(571,'REGION XII','Sarangani','Ltid'),(572,'REGION XII','Sultan Kudarat','Stod'),(573,'REGION XII','South Cotabato','Stod'),(574,'REGION XII','North Cotabato','Darpo-stod'),(575,'REGION XII','Cotabato','Darpo-ltid'),(576,'REGION XII','DARRO','Stod'),(577,'REGION XII','Unknown','Unknown'),(578,'CARAGA','Agusan del Norte','PPMO - Agusan del Norte/LTID'),(579,'CARAGA','Agusan del Sur','PPMO - Agusan del Sur/ GS'),(580,'CARAGA','Surigao del Norte','PPMO - Surigao del Norte/LTID'),(581,'CARAGA','Surigao del Sur','PPMO - Surigao del Sur'),(582,'CARAGA','Regional Office','RPMO-Caraga/LTID'),(583,'CARAGA','Unknown','Unknown'),(584,'DENR','Namria','DENR Namria'),(585,'DENR','Fmb','Denr-fmb'),(586,'DENR','Lmb','DENR Lmb'),(587,'LRA','Central Office','Carp');
/*!40000 ALTER TABLE `locations` ENABLE KEYS */;
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
