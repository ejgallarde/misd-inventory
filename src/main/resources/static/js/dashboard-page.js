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

    initDashboardTable('#agingTable', [[7, 'desc']], 'No problematic IT assets found.');
    initDashboardTable('#fleetActionRequiredTable', [[0, 'asc']], 'No problematic vehicles found in the fleet.');
    initDashboardTable('#surveyAssetActionRequiredTable', [[0, 'asc']],
        'No problematic survey assets found.');

    function wireCatalogSpecBuilder(options) {
        const addBtn = document.getElementById(options.addBtnId);
        const form = document.getElementById(options.formId);
        if (!addBtn || !form) {
            return;
        }

        addBtn.addEventListener('click', function () {
            const container = document.getElementById(options.containerId);
            const row = document.createElement('div');
            row.className = 'input-group input-group-sm mb-2 spec-row';

            let optionsHtml = '<option value="" selected disabled>Select...</option>';
            options.specOptions.forEach(opt => {
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

        form.addEventListener('submit', function (e) {
            if (!this.checkValidity()) {
                e.preventDefault();
                this.classList.add('was-validated');
                return;
            }

            const specRows = document.querySelectorAll(`#${options.containerId} .spec-row`);
            const specObject = {};

            specRows.forEach(row => {
                const key = row.querySelector('.spec-key').value;
                const value = row.querySelector('.spec-value').value;
                if (key && value) {
                    specObject[key] = value;
                }
            });

            document.getElementById(options.hiddenInputId).value = JSON.stringify(specObject);
        });
    }

    wireCatalogSpecBuilder({
        addBtnId: 'addSpecBtn',
        containerId: 'spec-rows-container',
        formId: 'catalogForm',
        hiddenInputId: 'specifications',
        specOptions: [
            'Processor (CPU)', 'Memory (RAM)', 'Storage (SSD/HDD)',
            'Graphics (GPU)', 'Display/Resolution', 'Network/Wi-Fi',
            'Ports', 'Battery', 'OS', 'Dimensions/Weight'
        ]
    });

    wireCatalogSpecBuilder({
        addBtnId: 'addSurveySpecBtn',
        containerId: 'survey-spec-rows-container',
        formId: 'surveyCatalogForm',
        hiddenInputId: 'surveySpecifications',
        specOptions: [
            'Horizontal Accuracy (RTK)', 'Vertical Accuracy (RTK)', 'Angular Accuracy (Total Station)',
            'EDM Range (Reflectorless)', 'EDM Range (with Prism)', 'Number of Channels',
            'Satellite Constellations Supported', 'Update Rate (Hz)', 'IP Rating (Dust/Water Resistance)',
            'Battery Life', 'Operating Temperature Range', 'Onboard Memory/Storage',
            'Connectivity (Bluetooth/Wi-Fi/Radio)', 'Display/Screen', 'Warranty Period'
        ]
    });

    function wireBulkReceiveQuantityToggle(options) {
        $(`#${options.quantityInputId}`).on('input', function () {
            const qty = parseInt($(this).val()) || 1;
            const $tagInput = $(`#${options.tagInputId}`);
            const $serialInput = $(`#${options.serialInputId}`);

            if (qty > 1) {
                $tagInput.prop('disabled', true).val('').attr('placeholder', 'Auto-generated for bulk entry');
                $serialInput.prop('disabled', true).val('').attr('placeholder', 'Disabled for bulk entry');
            } else {
                $tagInput.prop('disabled', false).attr('placeholder', 'Leave blank to auto-generate');
                $serialInput.prop('disabled', false).attr('placeholder', '');
            }
        });
    }

    wireBulkReceiveQuantityToggle({
        quantityInputId: 'receiveQuantity',
        tagInputId: 'receiveAssetTag',
        serialInputId: 'receiveSerialNumber'
    });

    wireBulkReceiveQuantityToggle({
        quantityInputId: 'surveyReceiveQuantity',
        tagInputId: 'surveyReceiveAssetTag',
        serialInputId: 'surveyReceiveSerialNumber'
    });

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

    $('.js-document-upload-input').each(function () {
        renderDocumentPreview(this, { mergeSelection: false, enableRemove: true });
        $(this).on('change', function () {
            const input = this;
            const enforceNonMergingSelection = isReceiveAssetUploadInput(input);

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

    $('#receiveAssetForm, #addVehicleOffcanvas form, #addSurveyAssetOffcanvas form').on('submit', function (event) {
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
        addSurveyAssetOffcanvas: 'addSurveyAssetForm'
    };
    const formPristineSnapshots = new WeakMap();
    let pendingExitOffcanvasEl = null;

    $('#receiveAssetOffcanvas, #addVehicleOffcanvas, #addSurveyAssetOffcanvas').on('hidden.bs.offcanvas', function () {
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
        if (this.id === 'receiveAssetOffcanvas') {
            $('#receiveAssetTag').prop('disabled', false).attr('placeholder', 'Leave blank to auto-generate');
            $('#receiveSerialNumber').prop('disabled', false).attr('placeholder', '');
        }
        if (this.id === 'addSurveyAssetOffcanvas') {
            $('#surveyReceiveAssetTag').prop('disabled', false).attr('placeholder', 'Leave blank to auto-generate');
            $('#surveyReceiveSerialNumber').prop('disabled', false).attr('placeholder', '');
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

    $('#addSurveyAssetOffcanvas').on('shown.bs.offcanvas', function () {
        const surveyAssetInput = document.getElementById('surveyAssetDocumentFilesInput');
        if (!surveyAssetInput) {
            return;
        }

        MISDCommon.clearSelectedFiles(surveyAssetInput);
        renderDocumentPreview(surveyAssetInput, { mergeSelection: false, enableRemove: true });
    });

    const today = new Date().toISOString().split('T')[0];
    $('input[name="purchaseDate"]').val(today);

    // --- Confirm-before-exit for add-asset offcanvases (Receive Asset, Register Vehicle) ---
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

    $('#receiveAssetOffcanvas, #addVehicleOffcanvas, #addSurveyAssetOffcanvas').on('shown.bs.offcanvas', function () {
        captureFormSnapshot(this);
    });

    $('#receiveAssetOffcanvas, #addVehicleOffcanvas, #addSurveyAssetOffcanvas').on('hide.bs.offcanvas', function (event) {
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
