(function (window) {
    const CACHE_KEYS = {
        provinces: 'misd.psgc.provinces',
        citiesPrefix: 'misd.psgc.cities.',
        barangaysPrefix: 'misd.psgc.barangays.'
    };

    function getSessionStorage() {
        try {
            return window.sessionStorage;
        } catch (error) {
            return null;
        }
    }

    function readCached(key) {
        const storage = getSessionStorage();
        if (!storage) {
            return null;
        }

        const raw = storage.getItem(key);
        if (!raw) {
            return null;
        }

        try {
            return JSON.parse(raw);
        } catch (error) {
            storage.removeItem(key);
            return null;
        }
    }

    function writeCached(key, value) {
        const storage = getSessionStorage();
        if (!storage) {
            return;
        }

        try {
            storage.setItem(key, JSON.stringify(value));
        } catch (error) {
            // Ignore storage quota/access failures and continue with network calls.
        }
    }

    function clearCascadeCache() {
        const storage = getSessionStorage();
        if (!storage) {
            return;
        }

        Object.keys(storage).forEach(function (key) {
            if (key === CACHE_KEYS.provinces
                || key.startsWith(CACHE_KEYS.citiesPrefix)
                || key.startsWith(CACHE_KEYS.barangaysPrefix)) {
                storage.removeItem(key);
            }
        });
    }

    function fetchWithCache(cacheKey, url, params) {
        const cached = readCached(cacheKey);
        if (Array.isArray(cached)) {
            return Promise.resolve(cached);
        }

        return $.get(url, params || {}).then(function (result) {
            const normalized = Array.isArray(result) ? result : [];
            writeCached(cacheKey, normalized);
            return normalized;
        });
    }

    function buildOption(optionData) {
        const option = document.createElement('option');
        option.value = optionData.name || '';
        option.textContent = optionData.name || '';
        option.dataset.code = optionData.code || '';
        option.dataset.zipCode = optionData.zipCode || '';
        return option;
    }

    function resetSelect(selectElement, placeholder, disabled) {
        if (!selectElement) {
            return;
        }

        selectElement.innerHTML = '';
        const placeholderOption = document.createElement('option');
        placeholderOption.value = '';
        placeholderOption.textContent = placeholder;
        placeholderOption.disabled = true;
        placeholderOption.selected = true;
        selectElement.appendChild(placeholderOption);

        selectElement.disabled = !!disabled;
    }

    function getSelectedCode(selectElement) {
        if (!selectElement || selectElement.selectedIndex < 0) {
            return '';
        }
        const selectedOption = selectElement.options[selectElement.selectedIndex];
        return selectedOption ? (selectedOption.dataset.code || '') : '';
    }

    function populateSelect(selectElement, options, placeholder) {
        resetSelect(selectElement, placeholder, false);
        options.forEach(function (optionData) {
            selectElement.appendChild(buildOption(optionData));
        });
    }

    function selectOptionByValue(selectElement, value) {
        if (!selectElement || !value) {
            return false;
        }

        const expected = String(value).trim().toLowerCase();
        for (let i = 0; i < selectElement.options.length; i++) {
            const option = selectElement.options[i];
            if ((option.value || '').trim().toLowerCase() === expected) {
                selectElement.selectedIndex = i;
                return true;
            }
        }

        return false;
    }

    function createController(config) {
        const provinceSelect = document.querySelector(config.provinceSelector);
        const citySelect = document.querySelector(config.citySelector);
        const barangaySelect = document.querySelector(config.barangaySelector);
        const zipInput = document.querySelector(config.zipSelector);

        if (!provinceSelect || !citySelect || !barangaySelect || !zipInput) {
            return null;
        }

        const placeholders = {
            province: config.provincePlaceholder || 'Select province',
            city: config.cityPlaceholder || 'Select city/municipality',
            barangay: config.barangayPlaceholder || 'Select barangay'
        };

        function loadProvinces() {
            resetSelect(provinceSelect, 'Loading provinces...', true);
            return fetchWithCache(CACHE_KEYS.provinces, '/api/locations/provinces').then(function (provinces) {
                populateSelect(provinceSelect, provinces || [], placeholders.province);
                return provinces || [];
            }, function () {
                resetSelect(provinceSelect, 'Unable to load provinces', true);
                return [];
            });
        }

        function loadCities(provinceCode) {
            resetSelect(citySelect, 'Loading cities/municipalities...', true);
            resetSelect(barangaySelect, placeholders.barangay, true);

            return fetchWithCache(
                CACHE_KEYS.citiesPrefix + provinceCode,
                '/api/locations/cities',
                { provinceCode: provinceCode }
            ).then(function (cities) {
                populateSelect(citySelect, cities || [], placeholders.city);
                return cities || [];
            }, function () {
                resetSelect(citySelect, 'Unable to load cities/municipalities', true);
                return [];
            });
        }

        function loadBarangays(cityMunicipalityCode) {
            resetSelect(barangaySelect, 'Loading barangays...', true);

            return fetchWithCache(
                CACHE_KEYS.barangaysPrefix + cityMunicipalityCode,
                '/api/locations/barangays',
                { cityMunicipalityCode: cityMunicipalityCode }
            ).then(function (barangays) {
                populateSelect(barangaySelect, barangays || [], placeholders.barangay);
                return barangays || [];
            }, function () {
                resetSelect(barangaySelect, 'Unable to load barangays', true);
                return [];
            });
        }

        function resetDependents() {
            resetSelect(citySelect, placeholders.city, true);
            resetSelect(barangaySelect, placeholders.barangay, true);
        }

        function initializeEmpty() {
            resetDependents();
            zipInput.value = '';
            return loadProvinces();
        }

        function loadWithSelection(selection) {
            const state = selection || {};
            return loadProvinces().then(function () {
                const provinceSelected = selectOptionByValue(provinceSelect, state.province);
                if (!provinceSelected) {
                    resetDependents();
                    if (state.zipCode) {
                        zipInput.value = state.zipCode;
                    }
                    return;
                }

                const provinceCode = getSelectedCode(provinceSelect);
                if (!provinceCode) {
                    return;
                }

                return loadCities(provinceCode).then(function () {
                    const citySelected = selectOptionByValue(citySelect, state.city);
                    if (!citySelected) {
                        if (state.zipCode) {
                            zipInput.value = state.zipCode;
                        }
                        return;
                    }

                    const cityCode = getSelectedCode(citySelect);
                    if (!cityCode) {
                        return;
                    }

                    return loadBarangays(cityCode).then(function () {
                        selectOptionByValue(barangaySelect, state.barangay);
                        if (state.zipCode && !zipInput.value) {
                            zipInput.value = state.zipCode;
                        }
                    });
                });
            });
        }

        provinceSelect.addEventListener('change', function () {
            const provinceCode = getSelectedCode(provinceSelect);
            if (!provinceCode) {
                resetDependents();
                return;
            }
            loadCities(provinceCode);
        });

        citySelect.addEventListener('change', function () {
            const cityMunicipalityCode = getSelectedCode(citySelect);
            if (!cityMunicipalityCode) {
                resetSelect(barangaySelect, placeholders.barangay, true);
                return;
            }
            loadBarangays(cityMunicipalityCode);
        });

        return {
            initializeEmpty: initializeEmpty,
            loadWithSelection: loadWithSelection,
            resetDependents: resetDependents
        };
    }

    window.MISDLocationCascade = {
        createController: createController,
        clearCache: clearCascadeCache
    };

    $(document).ready(function () {
        const dashboardController = createController({
            provinceSelector: '#propertyProvince',
            citySelector: '#propertyCity',
            barangaySelector: '#propertyBarangay',
            zipSelector: '#propertyZipCode'
        });

        if (dashboardController) {
            dashboardController.initializeEmpty();

            const addPropertyOffcanvas = document.getElementById('addPropertyOffcanvas');
            if (addPropertyOffcanvas) {
                addPropertyOffcanvas.addEventListener('hidden.bs.offcanvas', function () {
                    dashboardController.initializeEmpty();
                });
            }
        }
    });
})(window);
