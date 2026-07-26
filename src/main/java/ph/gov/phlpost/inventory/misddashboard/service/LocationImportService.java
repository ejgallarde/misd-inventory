package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.PsgcBarangay;
import ph.gov.phlpost.inventory.misddashboard.model.PsgcCityMunicipality;
import ph.gov.phlpost.inventory.misddashboard.model.PsgcImportResult;
import ph.gov.phlpost.inventory.misddashboard.model.PsgcProvince;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcBarangayRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcCityMunicipalityRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcProvinceRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LocationImportService {

    private final PsgcProvinceRepository provinceRepository;
    private final PsgcCityMunicipalityRepository cityMunicipalityRepository;
    private final PsgcBarangayRepository barangayRepository;
    private final CacheManager cacheManager;

    public LocationImportService(PsgcProvinceRepository provinceRepository,
            PsgcCityMunicipalityRepository cityMunicipalityRepository,
            PsgcBarangayRepository barangayRepository,
            CacheManager cacheManager) {
        this.provinceRepository = provinceRepository;
        this.cityMunicipalityRepository = cityMunicipalityRepository;
        this.barangayRepository = barangayRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public PsgcImportResult importFromCsv(
            MultipartFile provincesFile,
            MultipartFile citiesFile,
            MultipartFile barangaysFile) throws IOException {
        List<PsgcProvince> provinces = parseProvinces(provincesFile);
        List<PsgcCityMunicipality> citiesMunicipalities = parseCitiesMunicipalities(citiesFile);
        List<PsgcBarangay> barangays = parseBarangays(barangaysFile);

        barangayRepository.deleteAllInBatch();
        cityMunicipalityRepository.deleteAllInBatch();
        provinceRepository.deleteAllInBatch();

        provinceRepository.saveAll(provinces);
        cityMunicipalityRepository.saveAll(citiesMunicipalities);
        barangayRepository.saveAll(barangays);

        clearLocationCaches();

        return new PsgcImportResult(provinces.size(), citiesMunicipalities.size(), barangays.size());
    }

    @Transactional
    public PsgcImportResult importFromSinglePsgcCsv(MultipartFile psgcFile) throws IOException {
        Map<String, PsgcProvince> provinceByCode = new LinkedHashMap<>();
        Map<String, PsgcCityMunicipality> cityByCode = new LinkedHashMap<>();
        Map<String, PsgcBarangay> barangayByCode = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(psgcFile.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line == null || line.isBlank()) {
                    continue;
                }

                String trimmed = line.trim();
                // Skip non-data lines including multiline header fragments.
                if (trimmed.length() < 12 || !Character.isDigit(trimmed.charAt(0))) {
                    continue;
                }

                List<String> columns = parseCsvLine(trimmed);
                if (columns.size() < 4) {
                    continue;
                }

                String tenDigitCode = clean(columns.get(0));
                String name = clean(columns.get(1));
                String geographicLevel = clean(columns.get(3));
                if (!isValidTenDigitCode(tenDigitCode) || name.isEmpty() || geographicLevel.isEmpty()) {
                    continue;
                }

                String normalizedLevel = geographicLevel.toUpperCase();
                if (normalizedLevel.startsWith("PROV") || normalizedLevel.startsWith("DIST")) {
                    PsgcProvince province = new PsgcProvince();
                    province.setProvinceCode(tenDigitCode);
                    province.setProvinceName(name);
                    provinceByCode.put(tenDigitCode, province);
                    continue;
                }

                if (normalizedLevel.contains("MUN") || normalizedLevel.contains("CITY")) {
                    String provinceCode = deriveProvinceCode(tenDigitCode);
                    if (provinceCode == null) {
                        continue;
                    }

                    // Some datasets expose district-level parent keys (for example NCR)
                    // instead of explicit province rows. Create a stable fallback parent
                    // to satisfy FK constraints while preserving code-based hierarchy.
                    provinceByCode.computeIfAbsent(provinceCode, code -> {
                        PsgcProvince fallbackProvince = new PsgcProvince();
                        fallbackProvince.setProvinceCode(code);
                        fallbackProvince.setProvinceName("Province " + code);
                        return fallbackProvince;
                    });

                    PsgcCityMunicipality cityMunicipality = new PsgcCityMunicipality();
                    cityMunicipality.setCityMunicipalityCode(tenDigitCode);
                    cityMunicipality.setProvinceCode(provinceCode);
                    cityMunicipality.setCityMunicipalityName(name);
                    cityByCode.put(tenDigitCode, cityMunicipality);
                    continue;
                }

                if (normalizedLevel.startsWith("BGY") || normalizedLevel.startsWith("BRGY")
                        || normalizedLevel.contains("BARANGAY")) {
                    String cityCode = deriveCityMunicipalityCode(tenDigitCode);
                    if (cityCode == null) {
                        continue;
                    }

                    PsgcBarangay barangay = new PsgcBarangay();
                    barangay.setBarangayCode(tenDigitCode);
                    barangay.setCityMunicipalityCode(cityCode);
                    barangay.setBarangayName(name);
                    barangayByCode.put(tenDigitCode, barangay);
                }
            }
        }

        List<PsgcProvince> provinces = new ArrayList<>(provinceByCode.values());
        List<PsgcCityMunicipality> citiesMunicipalities = cityByCode.values().stream()
                .filter(city -> provinceByCode.containsKey(city.getProvinceCode()))
                .toList();
        Set<String> savedCityCodes = new HashSet<>();
        citiesMunicipalities.forEach(city -> savedCityCodes.add(city.getCityMunicipalityCode()));
        List<PsgcBarangay> barangays = barangayByCode.values().stream()
                .filter(barangay -> savedCityCodes.contains(barangay.getCityMunicipalityCode()))
                .toList();

        barangayRepository.deleteAllInBatch();
        cityMunicipalityRepository.deleteAllInBatch();
        provinceRepository.deleteAllInBatch();

        provinceRepository.saveAll(provinces);
        cityMunicipalityRepository.saveAll(citiesMunicipalities);
        barangayRepository.saveAll(barangays);

        clearLocationCaches();

        return new PsgcImportResult(provinces.size(), citiesMunicipalities.size(), barangays.size());
    }

    private List<PsgcProvince> parseProvinces(MultipartFile file) throws IOException {
        List<PsgcProvince> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                List<String> columns = parseCsvLine(line);
                if (columns.size() < 2) {
                    continue;
                }
                String provinceCode = clean(columns.get(0));
                String provinceName = clean(columns.get(1));
                if (provinceCode.isEmpty() || provinceName.isEmpty()) {
                    continue;
                }

                PsgcProvince province = new PsgcProvince();
                province.setProvinceCode(provinceCode);
                province.setProvinceName(provinceName);
                rows.add(province);
            }
        }
        return rows;
    }

    private List<PsgcCityMunicipality> parseCitiesMunicipalities(MultipartFile file) throws IOException {
        List<PsgcCityMunicipality> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                List<String> columns = parseCsvLine(line);
                if (columns.size() < 3) {
                    continue;
                }
                String cityMunicipalityCode = clean(columns.get(0));
                String provinceCode = clean(columns.get(1));
                String cityMunicipalityName = clean(columns.get(2));
                if (cityMunicipalityCode.isEmpty() || provinceCode.isEmpty() || cityMunicipalityName.isEmpty()) {
                    continue;
                }

                PsgcCityMunicipality cityMunicipality = new PsgcCityMunicipality();
                cityMunicipality.setCityMunicipalityCode(cityMunicipalityCode);
                cityMunicipality.setProvinceCode(provinceCode);
                cityMunicipality.setCityMunicipalityName(cityMunicipalityName);
                rows.add(cityMunicipality);
            }
        }
        return rows;
    }

    private List<PsgcBarangay> parseBarangays(MultipartFile file) throws IOException {
        List<PsgcBarangay> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                List<String> columns = parseCsvLine(line);
                if (columns.size() < 3) {
                    continue;
                }
                String barangayCode = clean(columns.get(0));
                String cityMunicipalityCode = clean(columns.get(1));
                String barangayName = clean(columns.get(2));
                String zipCode = columns.size() > 3 ? clean(columns.get(3)) : "";
                if (barangayCode.isEmpty() || cityMunicipalityCode.isEmpty() || barangayName.isEmpty()) {
                    continue;
                }

                PsgcBarangay barangay = new PsgcBarangay();
                barangay.setBarangayCode(barangayCode);
                barangay.setCityMunicipalityCode(cityMunicipalityCode);
                barangay.setBarangayName(barangayName);
                barangay.setZipCode(zipCode.isEmpty() ? null : zipCode);
                rows.add(barangay);
            }
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return columns;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        columns.add(current.toString());
        return columns;
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private boolean isValidTenDigitCode(String value) {
        if (value == null || value.length() != 10) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // PSGC city/municipality code shares first 7 digits with barangays and ends
    // with 000.
    private String deriveCityMunicipalityCode(String barangayCode) {
        if (!isValidTenDigitCode(barangayCode)) {
            return null;
        }
        return barangayCode.substring(0, 7) + "000";
    }

    // PSGC province code shares first 5 digits with city/municipality and ends with
    // 00000.
    private String deriveProvinceCode(String cityMunicipalityCode) {
        if (!isValidTenDigitCode(cityMunicipalityCode)) {
            return null;
        }
        return cityMunicipalityCode.substring(0, 5) + "00000";
    }

    private void clearLocationCaches() {
        clearCache("psgcProvinces");
        clearCache("psgcCities");
        clearCache("psgcBarangays");
    }

    private void clearCache(String cacheName) {
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).clear();
        }
    }
}
