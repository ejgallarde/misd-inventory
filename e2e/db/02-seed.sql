-- Deterministic seed data for the Playwright suite. Every status value below is
-- taken from application-dropdowns.properties so the UI sees what it would see
-- in production. Records prefixed E2E- are owned by the tests.

SET NAMES utf8mb4;
USE misd_inventory;

INSERT INTO locations (LocationID, Area, Province, OfficeAddress) VALUES
  (1, 'Central Office', 'Metro Manila', 'Liwasang Bonifacio, Manila');

INSERT INTO Personnel (EmployeeID, FirstName, LastName, JobTitle, Department, Division, ManagerID, BaseLocationID) VALUES
  ('E2E-MGR',  'Maria', 'Reyes',  'Division Chief',                       'MISD',      'Systems',     NULL,      1),
  ('E2E-001',  'Juan',  'Santos', 'Information Systems Analyst',          'MISD',      'Systems',     'E2E-MGR', 1),
  ('E2E-002',  'Ana',   'Cruz',   'Driver',                               'Motorpool', 'Logistics',   'E2E-MGR', 1),
  ('E2E-TECH', 'Leo',   'Garcia', 'Computer Maintenance Technologist II', 'MISD',      'Tech Support','E2E-MGR', 1);

INSERT INTO EquipmentCatalog (CatalogID, Category, Manufacturer, ModelName, Specifications) VALUES
  (1, 'Laptop',  'Dell',  'Latitude 5440', '{"cpu":"i5-1345U","ram":"16GB","storage":"512GB SSD"}'),
  (2, 'Desktop', 'HP',    'ProDesk 400 G9', '{"cpu":"i5-12500","ram":"8GB"}'),
  (3, 'Printer', 'Epson', 'L3250', NULL);

INSERT INTO Assets (AssetTag, CatalogID, SerialNumber, PurchaseDate, PurchasePrice, CurrentOwnerID, Remarks,
                    DeploymentStatus, MaintenanceHealthStatus, LifecycleStatus) VALUES
  ('E2E-ASSET-STORAGE',  1, 'SN-E2E-0001', '2025-01-15', 65000.00, NULL,      NULL,
   'In Storage', 'Operational', 'Procured / Pre-Deployment'),
  ('E2E-ASSET-DEPLOYED', 1, 'SN-E2E-0002', '2024-03-10', 65000.00, 'E2E-001', 'Seeded deployed laptop',
   'Deployed', 'Operational', 'Active'),
  ('E2E-ASSET-BER',      2, 'SN-E2E-0003', '2018-06-01', 42000.00, NULL,      NULL,
   'In Storage', 'Beyond Economic Repair (BER)', 'End of Life (EOL)'),
  ('E2E-ASSET-MISSING',  3, NULL,          '2023-02-20', 12000.00, NULL,      NULL,
   'Missing / Unaccounted', 'Operational', 'Active'),
  ('E2E-ASSET-ASSIGN',   2, 'SN-E2E-0005', '2025-05-05', 42000.00, NULL,      NULL,
   'In Storage', 'Operational', 'Procured / Pre-Deployment');

INSERT INTO FleetVehicles (VehicleID, PlateNumber, VehicleType, Make, Model, ManufactureYear, EngineNumber, ChassisNumberVIN,
                           FuelType, RegistrationExpiry, InsuranceExpiry, AssignedDriverID, BodyNumber, Cost, AcquisitionYear,
                           Remarks, AdminLegaltionalStatus, OperationalStatus, MaintenanceStatus) VALUES
  (1, 'E2E-1001', 'Delivery Van', 'Toyota', 'Hiace', 2022, 'ENG-E2E-1001', 'VIN-E2E-1001', 'Diesel',
   '2027-06-30', '2027-06-30', NULL, 'BODY-1001', 1800000.00, 2022, NULL, 'Registered', 'Available/Idle', 'Roadworthy'),
  -- Registration already lapsed, status still "Registered": nothing auto-updates it any more.
  (2, 'E2E-1002', 'Motorcycle', 'Honda', 'XRM 125', 2021, 'ENG-E2E-1002', 'VIN-E2E-1002', 'Gas',
   '2026-01-15', '2026-01-15', NULL, 'BODY-1002', 85000.00, 2021, NULL, 'Registered', 'Available/Idle', 'Roadworthy'),
  -- Past its ten-year useful life.
  (3, 'E2E-1003', 'Executive Car', 'Toyota', 'Camry', 2010, 'ENG-E2E-1003', 'VIN-E2E-1003', 'Gas',
   '2027-03-31', '2027-03-31', NULL, 'BODY-1003', 1500000.00, 2011, NULL, 'Registered', 'Available/Idle', 'Roadworthy'),
  (4, 'E2E-1004', 'Van', 'Nissan', 'Urvan', 2023, 'ENG-E2E-1004', 'VIN-E2E-1004', 'Diesel',
   '2027-09-30', '2027-09-30', NULL, 'BODY-1004', 1700000.00, 2023, NULL, 'Registered', 'Available/Idle', 'Roadworthy'),
  -- Reported stolen: every dispatch must be refused, including a direct POST.
  (5, 'E2E-1005', 'Pickup Truck', 'Ford', 'Ranger', 2020, 'ENG-E2E-1005', 'VIN-E2E-1005', 'Diesel',
   '2027-05-31', '2027-05-31', NULL, 'BODY-1005', 1400000.00, 2020, NULL, 'Under Investigation', 'Stolen',
   'Not Applicable');

INSERT INTO RealEstateProperties (PropertyID, PropertyType, PropertyName, Area, TitleNumber, AddressLine1, Province, City,
                                  Barangay, ZipCode, LotAreaSqm, FloorAreaSqm, AcquisitionDate, AssessedValue,
                                  PropertyTaxStatus, CustodianID, TaxDeclarationNumber, SurveyPlanNumber,
                                  LegalTitlingStatus, OperationalStatus, ConditionStatus) VALUES
  (1, 'Lot', 'E2E Clean Lot', 'Central Office', 'TCT-E2E-0001', 'Liwasang Bonifacio', 'Metro Manila', 'Manila',
   'Barangay 659', '1000', 1200.00, NULL, '1995-01-01', 25000000.00,
   'Paid (Current Year)', 'E2E-001', 'TD-E2E-0001', 'PSD-E2E-0001', 'Titled', 'Active/In Use', 'Usable - No Known Issues'),
  -- Every status is a "problem" value from the dropdown, but none appears in the repository query.
  (2, 'Lot', 'E2E Disputed Lot', 'Area 3', 'TCT-E2E-0002', 'Sample Street', 'Pampanga', 'San Fernando',
   'Dolores', '2000', 800.00, NULL, '2001-05-05', 4000000.00,
   'Paid (Current Year)', NULL, 'TD-E2E-0002', 'PSD-E2E-0002', 'Under Dispute', 'Active/In Use',
   'Not Usable - Under Legal Dispute'),
  (3, 'Facility - Building', 'E2E Overdue Building', 'Central Office', NULL, 'Liwasang Bonifacio', 'Metro Manila', 'Manila',
   'Barangay 659', '1000', NULL, 3500.00, '1926-01-01', 90000000.00,
   'Overdue', NULL, 'TD-E2E-0003', NULL, 'Titled', 'Active/In Use', 'Usable - Minor Repair Needed');

INSERT INTO PsgcProvinces (ProvinceCode, ProvinceName) VALUES
  ('1339', 'Metro Manila'),
  ('0354', 'Pampanga');
INSERT INTO PsgcCitiesMunicipalities (CityMunicipalityCode, ProvinceCode, CityMunicipalityName) VALUES
  ('133901', '1339', 'Manila'),
  ('035416', '0354', 'San Fernando');
INSERT INTO PsgcBarangays (BarangayCode, CityMunicipalityCode, BarangayName, ZipCode) VALUES
  ('133901659', '133901', 'Barangay 659', '1000'),
  ('035416010', '035416', 'Dolores', '2000');

INSERT INTO LifecycleAuditLog (ReferenceID, ActionType, PerformedBy, TransactionDate, Notes) VALUES
  ('E2E-ASSET-DEPLOYED', 'Asset Received', 'seed', '2024-03-10 09:00:00', 'Seeded receipt'),
  ('VEHICLE-1', 'Vehicle Details Updated', 'seed', '2025-01-01 09:00:00', 'Seeded history'),
  ('PROP-1', 'Property Details Updated', 'seed', '2025-01-01 09:00:00', 'Seeded history');
INSERT INTO AssetAssignments (AssetTag, EmployeeID, ActionType, TransactionDate, ConditionNotes) VALUES
  ('E2E-ASSET-DEPLOYED', 'E2E-001', 'Checkout', '2024-03-11 09:00:00', 'Seeded checkout');
