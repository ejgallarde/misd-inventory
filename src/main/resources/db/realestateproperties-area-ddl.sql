-- RealEstateProperties schema update for area-based land/facility intake.
-- Run manually because spring.jpa.hibernate.ddl-auto=none.

ALTER TABLE RealEstateProperties
    ADD COLUMN IF NOT EXISTS Area VARCHAR(50) NULL AFTER PropertyName,
    ADD COLUMN IF NOT EXISTS SurveyPlanNumber VARCHAR(150) NULL AFTER TaxDeclarationNumber;
