$(document).ready(function () {
    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn',
        successToastId: 'successToast',
        errorToastId: 'errorToast',
        errorToastDelay: 4000
    });

    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
        bootstrap.Tooltip.getOrCreateInstance(element);
    });

    $('button[data-bs-toggle="tab"]').on('shown.bs.tab', function (e) {
        $('button[data-bs-toggle="tab"]').removeClass('text-dark').addClass('text-secondary');
        $(e.target).removeClass('text-secondary').addClass('text-dark');
        const targetKey = $(e.target).attr('data-bs-target')?.replace('#', '');
        if (targetKey) {
            sessionStorage.setItem('dashboardActiveTab', targetKey);
        }
    });

    const storedDashboardTab = sessionStorage.getItem('dashboardActiveTab');
    if (storedDashboardTab) {
        const triggerButton = document.querySelector(`#assetTabs button[data-bs-target="#${storedDashboardTab}"]`);
        if (triggerButton) {
            new bootstrap.Tab(triggerButton).show();
        }
    }

    function initDashboardTable(selector, order, emptyMessage) {
        if (!$(selector).length) {
            return null;
        }
        return $(selector).DataTable(MISDCommon.buildStandardDataTableConfig({
            pageLength: 10,
            lengthMenu: [[5, 10, 25, -1], [5, 10, 25, 'All']],
            order: order,
            exportButtonOptions: {
                csvClassName: 'btn btn-secondary btn-sm me-1',
                excelClassName: 'btn btn-secondary btn-sm'
            },
            language: {
                emptyTable: emptyMessage
            }
        }));
    }

    const titleNotAvailableCheckbox = document.getElementById('propertyTitleNotAvailable');
    if (titleNotAvailableCheckbox) {
        titleNotAvailableCheckbox.addEventListener('change', applyTitleNotAvailableState);
    }

    initDashboardTable('#agingTable', [[7, 'desc']], 'No problematic IT assets found.');
    initDashboardTable('#fleetActionRequiredTable', [[0, 'asc']], 'No problematic vehicles found in the fleet.');
    initDashboardTable('#landAssetsActionRequiredTable', [[2, 'asc']], 'No land assets currently require attention.');
    initDashboardTable('#buildingsFacilitiesActionRequiredTable', [[1, 'asc']],
        'No buildings or facilities currently require attention.');

    const PROPERTY_FORM_CONTEXT = {
        LAND: 'LAND',
        BUILDING_FACILITY: 'BUILDING_FACILITY'
    };

    function getNormalizedPropertyFormContext(context) {
        return context === PROPERTY_FORM_CONTEXT.BUILDING_FACILITY
            ? PROPERTY_FORM_CONTEXT.BUILDING_FACILITY
            : PROPERTY_FORM_CONTEXT.LAND;
    }

    function ensurePropertyAreaOptions() {
        const propertyAreaInput = document.getElementById('propertyAreaInput');
        if (!propertyAreaInput) {
            return;
        }

        if (propertyAreaInput.options.length > 1) {
            return;
        }

        const defaultAreas = [
            'Area 1', 'Area 2', 'Area 3', 'Area 4', 'Area 5',
            'Area 6', 'Area 7', 'Area 8', 'Area 9', 'Central Office'
        ];

        defaultAreas.forEach(areaValue => {
            const option = document.createElement('option');
            option.value = areaValue;
            option.textContent = areaValue;
            propertyAreaInput.appendChild(option);
        });
    }

    /**
     * Keeps the Title Number field in step with the "no title on record"
     * checkbox. The server clears the submitted title when the box is ticked, so
     * the field is emptied and locked here to match.
     */
    function applyTitleNotAvailableState() {
        const checkbox = document.getElementById('propertyTitleNotAvailable');
        const titleInput = document.getElementById('propertyTitleNumberInput');
        if (!checkbox || !titleInput) {
            return;
        }

        const unavailable = checkbox.checked && !checkbox.disabled;
        titleInput.disabled = unavailable;
        if (unavailable) {
            titleInput.value = '';
        }
    }

    function applyPropertyFormContext(context) {
        const normalizedContext = getNormalizedPropertyFormContext(context);
        const propertyTypeInput = document.getElementById('propertyTypeInput');
        const propertyTypeGroup = document.getElementById('propertyTypeGroup');
        const propertyAreaInput = document.getElementById('propertyAreaInput');
        const propertyLotAreaGroup = document.getElementById('propertyLotAreaGroup');
        const propertyLotAreaInput = document.getElementById('propertyLotAreaInput');
        const propertyFloorAreaGroup = document.getElementById('propertyFloorAreaGroup');
        const propertyFloorAreaInput = document.getElementById('propertyFloorAreaInput');
        const propertyLegalTitlingStatusGroup = document.getElementById('propertyLegalTitlingStatusGroup');
        const propertyLegalTitlingStatusInput = document.getElementById('propertyLegalTitlingStatusInput');
        const propertyConditionStatusInput = document.getElementById('propertyConditionStatusInput');
        const contextInput = document.getElementById('propertyRegistrationContextInput');
        const titleEl = document.getElementById('addPropertyOffcanvasTitle');
        const submitButton = document.getElementById('addPropertySubmitBtn');

        ensurePropertyAreaOptions();

        if (contextInput) {
            contextInput.value = normalizedContext;
        }

        if (titleEl) {
            titleEl.textContent = normalizedContext === PROPERTY_FORM_CONTEXT.BUILDING_FACILITY
                ? 'Add Building or Facility'
                : 'Add Land Asset';
        }

        if (submitButton) {
            submitButton.textContent = normalizedContext === PROPERTY_FORM_CONTEXT.BUILDING_FACILITY
                ? 'Add Building or Facility'
                : 'Add Land Asset';
        }

        if (propertyTypeGroup) {
            propertyTypeGroup.classList.toggle('d-none', normalizedContext === PROPERTY_FORM_CONTEXT.LAND);
        }

        if (propertyAreaInput) {
            propertyAreaInput.required = true;
        }

        const isBuildingContext = normalizedContext === PROPERTY_FORM_CONTEXT.BUILDING_FACILITY;

        if (propertyLotAreaGroup) {
            propertyLotAreaGroup.classList.toggle('d-none', isBuildingContext);
            if (propertyLotAreaInput) {
                propertyLotAreaInput.disabled = isBuildingContext;
                if (isBuildingContext) {
                    propertyLotAreaInput.value = '';
                }
            }
        }

        if (propertyFloorAreaGroup) {
            propertyFloorAreaGroup.classList.toggle('d-none', !isBuildingContext);
            if (propertyFloorAreaInput) {
                propertyFloorAreaInput.disabled = !isBuildingContext;
                if (!isBuildingContext) {
                    propertyFloorAreaInput.value = '';
                }
            }
        }

        if (propertyLegalTitlingStatusGroup) {
            propertyLegalTitlingStatusGroup.classList.toggle('d-none', isBuildingContext);
        }

        // "No title on record" applies to buildings and facilities only; a land
        // asset always requires a Title Number / TCT.
        const titleNotAvailableWrap = document.getElementById('propertyTitleNotAvailableWrap');
        const titleNotAvailableInput = document.getElementById('propertyTitleNotAvailable');
        if (titleNotAvailableWrap) {
            titleNotAvailableWrap.classList.toggle('d-none', !isBuildingContext);
        }
        if (titleNotAvailableInput) {
            titleNotAvailableInput.disabled = !isBuildingContext;
            if (!isBuildingContext) {
                titleNotAvailableInput.checked = false;
            }
            applyTitleNotAvailableState();
        }

        if (propertyLegalTitlingStatusInput) {
            propertyLegalTitlingStatusInput.required = !isBuildingContext;
            propertyLegalTitlingStatusInput.disabled = isBuildingContext;
            if (isBuildingContext) {
                propertyLegalTitlingStatusInput.value = '';
            }
        }

        if (propertyConditionStatusInput) {
            const options = Array.from(propertyConditionStatusInput.options);
            options.forEach(option => {
                if (!option.value) {
                    return;
                }

                const isLandOnly = String(option.dataset.landOnly).toLowerCase() === 'true';
                const shouldHide = isBuildingContext && isLandOnly;
                option.hidden = shouldHide;
                option.disabled = shouldHide;
            });

            const selectedOption = propertyConditionStatusInput.options[propertyConditionStatusInput.selectedIndex];
            if (selectedOption && selectedOption.disabled) {
                propertyConditionStatusInput.value = '';
            }
        }

        if (!propertyTypeInput) {
            return;
        }

        propertyTypeInput.required = normalizedContext === PROPERTY_FORM_CONTEXT.BUILDING_FACILITY;

        const options = Array.from(propertyTypeInput.options);
        const placeholderOption = options.find(option => !option.value || option.value.trim() === '');
        options.forEach(option => {
            if (!option.value || option.value.trim() === '') {
                return;
            }

            const isLotType = option.value.trim().toLowerCase() === 'lot';
            const isAllowed = normalizedContext === PROPERTY_FORM_CONTEXT.BUILDING_FACILITY ? !isLotType : isLotType;
            option.hidden = !isAllowed;
            option.disabled = !isAllowed;
        });

        if (placeholderOption) {
            placeholderOption.textContent = normalizedContext === PROPERTY_FORM_CONTEXT.BUILDING_FACILITY
                ? 'Select building/facility type'
                : 'Select land type';
        }

        const selectedOption = propertyTypeInput.options[propertyTypeInput.selectedIndex];
        if (!selectedOption || selectedOption.disabled) {
            propertyTypeInput.value = '';
        }

        if (normalizedContext === PROPERTY_FORM_CONTEXT.LAND) {
            propertyTypeInput.value = 'Lot';
        }

        updatePropertyIdentifierRules();
    }

    document.querySelectorAll('[data-property-form-context]').forEach(button => {
        button.addEventListener('click', function () {
            applyPropertyFormContext(this.dataset.propertyFormContext);
        });
    });

    const specOptions = [
        'Processor (CPU)', 'Memory (RAM)', 'Storage (SSD/HDD)',
        'Graphics (GPU)', 'Display/Resolution', 'Network/Wi-Fi',
        'Ports', 'Battery', 'OS', 'Dimensions/Weight'
    ];

    document.getElementById('addSpecBtn').addEventListener('click', function () {
        const container = document.getElementById('spec-rows-container');
        const row = document.createElement('div');
        row.className = 'input-group input-group-sm mb-2 spec-row';

        let optionsHtml = '<option value="" selected disabled>Select...</option>';
        specOptions.forEach(opt => {
            optionsHtml += `<option value="${opt}">${opt}</option>`;
        });

        row.innerHTML = `
            <select class="form-select spec-key" style="max-width: 40%;" required>
                ${optionsHtml}
            </select>
            <input type="text" class="form-control spec-value" placeholder="Value" required>
            <button class="btn btn-outline-danger remove-spec-btn" type="button" title="Remove row">X</button>
        `;
        container.appendChild(row);

        row.querySelector('.remove-spec-btn').addEventListener('click', function () {
            row.remove();
        });
    });

    document.getElementById('catalogForm').addEventListener('submit', function (e) {
        if (!this.checkValidity()) {
            e.preventDefault();
            this.classList.add('was-validated');
            return;
        }

        const specRows = document.querySelectorAll('.spec-row');
        const specObject = {};

        specRows.forEach(row => {
            const key = row.querySelector('.spec-key').value;
            const value = row.querySelector('.spec-value').value;
            if (key && value) {
                specObject[key] = value;
            }
        });

        document.getElementById('specifications').value = JSON.stringify(specObject);
    });

    $('#receiveQuantity').on('input', function () {
        const qty = parseInt($(this).val()) || 1;

        if (qty > 1) {
            $('#receiveAssetTag').prop('disabled', true).val('').attr('placeholder', 'Auto-generated for bulk entry');
            $('#receiveSerialNumber').prop('disabled', true).val('').attr('placeholder', 'Disabled for bulk entry');
        } else {
            $('#receiveAssetTag').prop('disabled', false).attr('placeholder', 'Leave blank to auto-generate');
            $('#receiveSerialNumber').prop('disabled', false).attr('placeholder', '');
        }
    });

    function updatePropertyIdentifierRules() {
        const propertyTypeInput = document.getElementById('propertyTypeInput');
        const propertyIdentifierSection = document.getElementById('propertyIdentifierSection');
        const propertyTitleFieldWrap = document.getElementById('propertyTitleFieldWrap');
        const propertySurveyPlanFieldWrap = document.getElementById('propertySurveyPlanFieldWrap');
        const propertyTaxDeclarationFieldWrap = document.getElementById('propertyTaxDeclarationFieldWrap');
        const titleInput = document.getElementById('propertyTitleNumberInput');
        const taxDeclarationInput = document.getElementById('propertyTaxDeclarationNumberInput');
        const surveyPlanInput = document.getElementById('propertySurveyPlanNumberInput');
        const contextInput = document.getElementById('propertyRegistrationContextInput');

        if (!propertyTypeInput || !propertyIdentifierSection || !propertyTitleFieldWrap
            || !propertySurveyPlanFieldWrap || !propertyTaxDeclarationFieldWrap
            || !titleInput || !taxDeclarationInput || !surveyPlanInput || !contextInput) {
            return;
        }

        const isLandContext = getNormalizedPropertyFormContext(contextInput.value) === PROPERTY_FORM_CONTEXT.LAND;
        propertyIdentifierSection.classList.remove('d-none');

        propertyTitleFieldWrap.classList.toggle('d-none', !isLandContext);
        propertySurveyPlanFieldWrap.classList.toggle('d-none', !isLandContext);
        propertyTaxDeclarationFieldWrap.classList.remove('d-none');

        if (isLandContext) {
            propertyTypeInput.value = 'Lot';
            propertyTypeInput.disabled = true;
            titleInput.required = true;
            titleInput.disabled = false;
            taxDeclarationInput.required = true;
            taxDeclarationInput.disabled = false;
            surveyPlanInput.required = true;
            surveyPlanInput.disabled = false;
            taxDeclarationInput.placeholder = 'Required for land assets';
            return;
        }

        propertyTypeInput.disabled = false;
        titleInput.required = false;
        titleInput.disabled = true;
        titleInput.value = '';
        taxDeclarationInput.disabled = false;
        taxDeclarationInput.required = true;
        taxDeclarationInput.placeholder = 'Required for building/facility assets';
        surveyPlanInput.required = false;
        surveyPlanInput.disabled = true;
        surveyPlanInput.value = '';
    }

    const propertyTypeInput = document.getElementById('propertyTypeInput');
    if (propertyTypeInput) {
        propertyTypeInput.addEventListener('change', updatePropertyIdentifierRules);
    }
    applyPropertyFormContext(PROPERTY_FORM_CONTEXT.LAND);
    updatePropertyIdentifierRules();

    function validateFileInputBeforeSubmit(input) {
        return MISDCommon.validateFileInputBySize(input);
    }

    function renderDocumentPreview(input, options = {}) {
        const { mergeSelection = true, enableRemove = true } = options;
        const previewTarget = input.dataset.documentPreviewTarget;
        const categoryTemplateTarget = input.dataset.documentCategoryTemplateTarget;
        const inputId = input.id;

        if (!previewTarget || !categoryTemplateTarget || !inputId) {
            return;
        }

        MISDCommon.renderDocumentPreviewBySelectors(
            `#${inputId}`,
            `#${previewTarget}`,
            `#${categoryTemplateTarget}`,
            { mergeSelection, enableRemove }
        );
    }

    function isReceiveAssetUploadInput(input) {
        return input && input.id === 'receiveDocumentFilesInput';
    }

    function isPropertyUploadInput(input) {
        return input && input.id === 'propertyDocumentFilesInput';
    }

    $('.js-document-upload-input').each(function () {
        renderDocumentPreview(this, { mergeSelection: false, enableRemove: true });
        $(this).on('change', function () {
            const input = this;
            const receiveChange = isReceiveAssetUploadInput(input);
            const propertyChange = isPropertyUploadInput(input);
            const enforceNonMergingSelection = receiveChange || propertyChange;

            if (enforceNonMergingSelection) {
                MISDCommon.clearSelectedFiles(input, { preserveNativeSelection: true });
            }

            window.requestAnimationFrame(function () {
                renderDocumentPreview(input, {
                    mergeSelection: !enforceNonMergingSelection,
                    enableRemove: true
                });
            });
        });
    });

    $('#receiveAssetForm, #addVehicleOffcanvas form, #addPropertyOffcanvas form').on('submit', function (event) {
        if (this.id !== 'receiveAssetForm' && this.closest('#addPropertyOffcanvas')) {
            updatePropertyIdentifierRules();
        }

        if (!this.checkValidity()) {
            event.preventDefault();
            this.classList.add('was-validated');
            return;
        }

        const documentInput = this.querySelector('.js-document-upload-input');
        if (!documentInput) {
            return;
        }

        if (!validateFileInputBeforeSubmit(documentInput)) {
            event.preventDefault();
            return;
        }

        const files = MISDCommon.getSelectedFiles(documentInput);
        if (!files.length) {
            return;
        }

        const categorySelects = Array.from(this.querySelectorAll('select[name="documentCategories"]'));
        const missingCategory = categorySelects.some(select => !select.value);
        if (missingCategory || categorySelects.length !== files.length) {
            event.preventDefault();
            alert('Select one document category for each file.');
        }
    });

    // Closing an add-asset offcanvas with unsaved changes prompts for confirmation before clearing it.
    const ADD_ASSET_FORM_IDS = {
        receiveAssetOffcanvas: 'receiveAssetForm',
        addVehicleOffcanvas: 'addVehicleForm',
        addPropertyOffcanvas: 'addPropertyForm'
    };
    const formPristineSnapshots = new WeakMap();
    let pendingExitOffcanvasEl = null;

    $('#receiveAssetOffcanvas, #addVehicleOffcanvas, #addPropertyOffcanvas').on('hidden.bs.offcanvas', function () {
        this.dataset.forceClose = 'false';
        const form = document.getElementById(ADD_ASSET_FORM_IDS[this.id]);
        if (form) {
            form.reset();
            formPristineSnapshots.delete(form);
        }
        $(this).find('.js-document-upload-input').each(function () {
            MISDCommon.clearSelectedFiles(this);
            renderDocumentPreview(this, { mergeSelection: false, enableRemove: true });
        });
        if (this.id === 'addPropertyOffcanvas') {
            applyPropertyFormContext(PROPERTY_FORM_CONTEXT.LAND);
            updatePropertyIdentifierRules();
        }
        if (this.id === 'receiveAssetOffcanvas') {
            $('#receiveAssetTag').prop('disabled', false).attr('placeholder', 'Leave blank to auto-generate');
            $('#receiveSerialNumber').prop('disabled', false).attr('placeholder', '');
        }
    });

    $('#receiveAssetOffcanvas').on('shown.bs.offcanvas', function () {
        const receiveInput = document.getElementById('receiveDocumentFilesInput');
        if (!receiveInput) {
            return;
        }

        MISDCommon.clearSelectedFiles(receiveInput);
        renderDocumentPreview(receiveInput, { mergeSelection: false, enableRemove: true });
    });

    $('#addPropertyOffcanvas').on('shown.bs.offcanvas', function () {
        const propertyInput = document.getElementById('propertyDocumentFilesInput');
        if (!propertyInput) {
            return;
        }

        MISDCommon.clearSelectedFiles(propertyInput);
        renderDocumentPreview(propertyInput, { mergeSelection: false, enableRemove: true });
    });

    const today = new Date().toISOString().split('T')[0];
    $('input[name="purchaseDate"]').val(today);

    // --- Confirm-before-exit for add-asset offcanvases (Receive Asset, Register Vehicle, Add Property) ---
    function serializeFormState(form) {
        const state = {};
        Array.from(form.elements).forEach(function (element) {
            if (!element.name || ['submit', 'button', 'reset'].includes(element.type)
                || /csrf/i.test(element.name)) {
                return;
            }
            if (element.type === 'checkbox' || element.type === 'radio') {
                state[element.name] = element.checked;
            } else if (element.type === 'file') {
                state[element.name] = MISDCommon.getSelectedFiles(element)
                    .map(function (file) { return file.name + ':' + file.size; })
                    .join('|');
            } else {
                state[element.name] = element.value;
            }
        });
        return JSON.stringify(state);
    }

    function captureFormSnapshot(offcanvasEl) {
        const form = document.getElementById(ADD_ASSET_FORM_IDS[offcanvasEl.id]);
        if (form) {
            formPristineSnapshots.set(form, serializeFormState(form));
        }
    }

    function isFormDirty(offcanvasEl) {
        const form = document.getElementById(ADD_ASSET_FORM_IDS[offcanvasEl.id]);
        const baseline = form && formPristineSnapshots.get(form);
        return baseline !== undefined && serializeFormState(form) !== baseline;
    }

    $('#receiveAssetOffcanvas, #addVehicleOffcanvas, #addPropertyOffcanvas').on('shown.bs.offcanvas', function () {
        captureFormSnapshot(this);
    });

    $('#receiveAssetOffcanvas, #addVehicleOffcanvas, #addPropertyOffcanvas').on('hide.bs.offcanvas', function (event) {
        if (this.dataset.forceClose === 'true') {
            return;
        }
        if (isFormDirty(this)) {
            event.preventDefault();
            pendingExitOffcanvasEl = this;
            bootstrap.Modal.getOrCreateInstance(document.getElementById('unsavedChangesModal')).show();
        }
    });

    document.getElementById('unsavedChangesContinueBtn').addEventListener('click', function () {
        bootstrap.Modal.getOrCreateInstance(document.getElementById('unsavedChangesModal')).hide();
        pendingExitOffcanvasEl = null;
    });

    document.getElementById('unsavedChangesExitBtn').addEventListener('click', function () {
        bootstrap.Modal.getOrCreateInstance(document.getElementById('unsavedChangesModal')).hide();
        if (pendingExitOffcanvasEl) {
            pendingExitOffcanvasEl.dataset.forceClose = 'true';
            bootstrap.Offcanvas.getOrCreateInstance(pendingExitOffcanvasEl).hide();
            pendingExitOffcanvasEl = null;
        }
    });
});
