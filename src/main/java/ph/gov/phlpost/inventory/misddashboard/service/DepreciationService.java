package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.model.SurveyAsset;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/**
 * Straight-line depreciation, computed the same way as the client-side estimate in
 * ui-common.js (computeStraightLineValuation) so the two never drift, but persisted
 * server-side and using useful-life years specific to each asset domain instead of a
 * single generic default.
 * <p>
 * Useful life per COA Circular 2003-007, Annex A ("Table of Estimated Useful Life of
 * Property, Plant and Equipment"): IT Equipment - Hardware = 5 years, Motor Vehicles = 7
 * years, Technical and Scientific Equipment = 10 years. Residual/salvage value (10% of
 * cost) is a separate, longstanding COA/GAM for NGAs assumption, not part of Annex A.
 */
@Service
public class DepreciationService {

    public static final int IT_USEFUL_LIFE_YEARS = 5;
    public static final int FLEET_USEFUL_LIFE_YEARS = 7;
    public static final int SURVEY_USEFUL_LIFE_YEARS = 10;
    public static final BigDecimal RESIDUAL_RATE = new BigDecimal("0.10");

    private final AssetRepository assetRepo;
    private final FleetVehicleRepository fleetRepo;
    private final SurveyAssetRepository surveyAssetRepo;

    public DepreciationService(AssetRepository assetRepo, FleetVehicleRepository fleetRepo,
            SurveyAssetRepository surveyAssetRepo) {
        this.assetRepo = assetRepo;
        this.fleetRepo = fleetRepo;
        this.surveyAssetRepo = surveyAssetRepo;
    }

    public record Valuation(BigDecimal currentValue, BigDecimal depreciationAmount) {
    }

    /**
     * @param cost           acquisition cost; null/non-positive yields no valuation
     * @param acquisitionYear calendar year the asset was acquired; null yields no valuation
     * @param usefulLifeYears domain-specific useful life (IT/Fleet/Survey constants above)
     */
    public static Valuation compute(BigDecimal cost, Integer acquisitionYear, int usefulLifeYears) {
        if (cost == null || cost.signum() <= 0 || acquisitionYear == null || acquisitionYear <= 0) {
            return null;
        }
        int yearsUsed = Math.max(0, Year.now().getValue() - acquisitionYear);
        BigDecimal depreciableBase = cost.multiply(BigDecimal.ONE.subtract(RESIDUAL_RATE));
        BigDecimal annualDepreciation = depreciableBase.divide(BigDecimal.valueOf(usefulLifeYears), 10,
                RoundingMode.HALF_UP);
        BigDecimal maxDepreciation = depreciableBase; // floor at residual value, never below it
        BigDecimal depreciationAmount = annualDepreciation.multiply(BigDecimal.valueOf(yearsUsed));
        if (depreciationAmount.compareTo(maxDepreciation) > 0) {
            depreciationAmount = maxDepreciation;
        }
        BigDecimal currentValue = cost.subtract(depreciationAmount);
        return new Valuation(
                currentValue.setScale(2, RoundingMode.HALF_UP),
                depreciationAmount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Recomputes and persists CurrentValue/DepreciationAmount/ValuationAsOfDate for every
     * asset, fleet vehicle, and survey asset that has both a cost and a known acquisition
     * year. Manual/on-demand only - no scheduling is wired up.
     */
    @Transactional
    public RecomputeSummary recomputeAll() {
        LocalDate today = LocalDate.now();
        int assetsUpdated = recomputeAssets(today);
        int vehiclesUpdated = recomputeFleetVehicles(today);
        int surveyAssetsUpdated = recomputeSurveyAssets(today);
        return new RecomputeSummary(assetsUpdated, vehiclesUpdated, surveyAssetsUpdated);
    }

    public record RecomputeSummary(int assetsUpdated, int fleetVehiclesUpdated, int surveyAssetsUpdated) {
    }

    private int recomputeAssets(LocalDate today) {
        List<Asset> assets = assetRepo.findAll();
        int updated = 0;
        for (Asset asset : assets) {
            Integer acquisitionYear = asset.getPurchaseDate() != null ? asset.getPurchaseDate().getYear() : null;
            Valuation valuation = compute(asset.getPurchasePrice(), acquisitionYear, IT_USEFUL_LIFE_YEARS);
            if (valuation == null) {
                continue;
            }
            asset.setCurrentValue(valuation.currentValue());
            asset.setDepreciationAmount(valuation.depreciationAmount());
            asset.setValuationAsOfDate(today);
            updated++;
        }
        assetRepo.saveAll(assets);
        return updated;
    }

    private int recomputeFleetVehicles(LocalDate today) {
        List<FleetVehicle> vehicles = fleetRepo.findAll();
        int updated = 0;
        for (FleetVehicle vehicle : vehicles) {
            Valuation valuation = compute(vehicle.getCost(), vehicle.getAcquisitionYear(), FLEET_USEFUL_LIFE_YEARS);
            if (valuation == null) {
                continue;
            }
            vehicle.setCurrentValue(valuation.currentValue());
            vehicle.setDepreciationAmount(valuation.depreciationAmount());
            vehicle.setValuationAsOfDate(today);
            updated++;
        }
        fleetRepo.saveAll(vehicles);
        return updated;
    }

    private int recomputeSurveyAssets(LocalDate today) {
        List<SurveyAsset> surveyAssets = surveyAssetRepo.findAll();
        int updated = 0;
        for (SurveyAsset asset : surveyAssets) {
            Integer acquisitionYear = asset.getAcquisitionDate() != null ? asset.getAcquisitionDate().getYear()
                    : null;
            Valuation valuation = compute(asset.getCost(), acquisitionYear, SURVEY_USEFUL_LIFE_YEARS);
            if (valuation == null) {
                continue;
            }
            asset.setCurrentValue(valuation.currentValue());
            asset.setDepreciationAmount(valuation.depreciationAmount());
            asset.setValuationAsOfDate(today);
            updated++;
        }
        surveyAssetRepo.saveAll(surveyAssets);
        return updated;
    }
}
