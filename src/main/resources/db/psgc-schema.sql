-- PSGC master tables for property address cascading dropdowns
-- Execute manually because spring.jpa.hibernate.ddl-auto=none.

CREATE TABLE IF NOT EXISTS PsgcProvinces (
    ProvinceCode VARCHAR(20) NOT NULL,
    ProvinceName VARCHAR(150) NOT NULL,
    PRIMARY KEY (ProvinceCode)
);

CREATE TABLE IF NOT EXISTS PsgcCitiesMunicipalities (
    CityMunicipalityCode VARCHAR(20) NOT NULL,
    ProvinceCode VARCHAR(20) NOT NULL,
    CityMunicipalityName VARCHAR(150) NOT NULL,
    PRIMARY KEY (CityMunicipalityCode),
    INDEX IX_PsgcCities_ProvinceCode (ProvinceCode),
    CONSTRAINT FK_PsgcCities_Province
        FOREIGN KEY (ProvinceCode) REFERENCES PsgcProvinces (ProvinceCode)
);

CREATE TABLE IF NOT EXISTS PsgcBarangays (
    BarangayCode VARCHAR(20) NOT NULL,
    CityMunicipalityCode VARCHAR(20) NOT NULL,
    BarangayName VARCHAR(150) NOT NULL,
    ZipCode VARCHAR(10) NULL,
    PRIMARY KEY (BarangayCode),
    INDEX IX_PsgcBarangays_CityCode (CityMunicipalityCode),
    CONSTRAINT FK_PsgcBarangays_City
        FOREIGN KEY (CityMunicipalityCode) REFERENCES PsgcCitiesMunicipalities (CityMunicipalityCode)
);

-- Load authoritative PSGC records into these tables from your official source dataset.

-- Optional API importer (after app starts):
-- POST /api/locations/import/csv (multipart/form-data)
-- parts:
--   provincesFile: CSV columns ProvinceCode,ProvinceName
--   citiesMunicipalitiesFile: CSV columns CityMunicipalityCode,ProvinceCode,CityMunicipalityName
--   barangaysFile: CSV columns BarangayCode,CityMunicipalityCode,BarangayName,ZipCode
-- Notes:
--   1) First row is treated as header and skipped.
--   2) Import replaces existing PSGC rows transactionally.

-- Single-file PSGC importer (recommended for official PSGC extract with geographic level):
-- POST /api/locations/import/psgc-single (multipart/form-data)
-- parts:
--   psgcFile: CSV columns including 10-digit PSGC, Name, Geographic Level
-- Hierarchy behavior:
--   1) Province rows are taken from Geographic Level = Prov.
--   2) City/Municipality parent ProvinceCode is derived from 10-digit code: first 5 digits + 00000.
--   3) Barangay parent CityMunicipalityCode is derived from 10-digit code: first 7 digits + 000.
