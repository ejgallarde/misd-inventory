-- Run manually on the MISD inventory database before deploying this change.
-- Adds TaxDeclarationNumber to support lot/facility identification rules.

ALTER TABLE RealEstateProperties
    ADD COLUMN TaxDeclarationNumber VARCHAR(255) NULL;

ALTER TABLE RealEstateProperties
    ADD COLUMN PropertyDetails TEXT NULL;

ALTER TABLE RealEstateProperties
    ADD COLUMN LegalTitlingStatus VARCHAR(255) NULL;

ALTER TABLE RealEstateProperties
    ADD COLUMN OperationalStatus VARCHAR(255) NULL;

ALTER TABLE RealEstateProperties
    ADD COLUMN ConditionStatus VARCHAR(255) NULL;

ALTER TABLE RealEstateProperties
    ADD UNIQUE INDEX UK_RealEstateProperties_TaxDeclarationNumber (TaxDeclarationNumber);