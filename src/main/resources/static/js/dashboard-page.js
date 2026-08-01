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
    initDashboardTable('#propertiesActionRequiredTable', [[0, 'asc']], 'All property dues and taxes are up to date.');

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
        const propertyTitleNotAvailableWrap = document.getElementById('propertyTitleNotAvailableWrap');
        const propertyTaxDeclarationFieldWrap = document.getElementById('propertyTaxDeclarationFieldWrap');
        const titleInput = document.getElementById('propertyTitleNumberInput');
        const taxDeclarationInput = document.getElementById('propertyTaxDeclarationNumberInput');
        const titleNotAvailableCheckbox = document.getElementById('propertyTitleNotAvailableCheckbox');

        if (!propertyTypeInput || !propertyIdentifierSection || !propertyTitleFieldWrap
            || !propertyTitleNotAvailableWrap || !propertyTaxDeclarationFieldWrap
            || !titleInput || !taxDeclarationInput || !titleNotAvailableCheckbox) {
            return;
        }

        const propertyTypeRaw = propertyTypeInput.value || '';
        const propertyType = propertyTypeRaw.toLowerCase();
        const hasTypeSelection = propertyTypeRaw.trim() !== '';
        const isLot = propertyType === 'lot';
        const titleNotAvailable = titleNotAvailableCheckbox.checked;

        propertyIdentifierSection.classList.toggle('d-none', !hasTypeSelection);

        if (!hasTypeSelection) {
            titleInput.required = false;
            titleInput.disabled = false;
            titleInput.value = '';
            taxDeclarationInput.required = false;
            taxDeclarationInput.disabled = false;
            taxDeclarationInput.value = '';
            titleNotAvailableCheckbox.checked = false;
            propertyTitleFieldWrap.classList.add('d-none');
            propertyTitleNotAvailableWrap.classList.add('d-none');
            propertyTaxDeclarationFieldWrap.classList.add('d-none');
            return;
        }

        if (isLot) {
            propertyTitleNotAvailableWrap.classList.remove('d-none');
            propertyTitleFieldWrap.classList.toggle('d-none', titleNotAvailable);
            propertyTaxDeclarationFieldWrap.classList.toggle('d-none', !titleNotAvailable);

            titleInput.disabled = titleNotAvailable;
            titleInput.required = !titleNotAvailable;
            taxDeclarationInput.disabled = !titleNotAvailable;
            taxDeclarationInput.required = titleNotAvailable;

            if (titleNotAvailable) {
                titleInput.value = '';
            } else {
                taxDeclarationInput.value = '';
            }
            return;
        }

        titleNotAvailableCheckbox.checked = false;
        propertyTitleNotAvailableWrap.classList.add('d-none');
        propertyTitleFieldWrap.classList.add('d-none');
        propertyTaxDeclarationFieldWrap.classList.remove('d-none');

        titleInput.required = false;
        titleInput.disabled = true;
        titleInput.value = '';

        taxDeclarationInput.disabled = false;
        taxDeclarationInput.required = true;
    }

    const propertyTypeInput = document.getElementById('propertyTypeInput');
    const propertyTitleNotAvailableCheckbox = document.getElementById('propertyTitleNotAvailableCheckbox');
    if (propertyTypeInput) {
        propertyTypeInput.addEventListener('change', updatePropertyIdentifierRules);
    }
    if (propertyTitleNotAvailableCheckbox) {
        propertyTitleNotAvailableCheckbox.addEventListener('change', updatePropertyIdentifierRules);
    }
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

    $('.js-document-upload-input').each(function () {
        renderDocumentPreview(this, { mergeSelection: false, enableRemove: true });
        $(this).on('change', function () {
            const input = this;
            const receiveChange = isReceiveAssetUploadInput(input);

            if (receiveChange) {
                MISDCommon.clearSelectedFiles(input, { preserveNativeSelection: true });
            }

            window.requestAnimationFrame(function () {
                renderDocumentPreview(input, {
                    mergeSelection: !receiveChange,
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

        const files = Array.from(documentInput.files || []);
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

    $('#receiveAssetOffcanvas, #addVehicleOffcanvas, #addPropertyOffcanvas').on('hidden.bs.offcanvas', function () {
        const form = this.querySelector('form');
        if (form) {
            form.reset();
        }

        if (this.id === 'addPropertyOffcanvas') {
            updatePropertyIdentifierRules();
        }

        $(this).find('.js-document-upload-input').each(function () {
            MISDCommon.clearSelectedFiles(this);
            renderDocumentPreview(this, { mergeSelection: false, enableRemove: true });
        });
    });

    $('#receiveAssetOffcanvas').on('shown.bs.offcanvas', function () {
        const receiveInput = document.getElementById('receiveDocumentFilesInput');
        if (!receiveInput) {
            return;
        }

        MISDCommon.clearSelectedFiles(receiveInput);
        renderDocumentPreview(receiveInput, { mergeSelection: false, enableRemove: true });
    });

    $('#receiveAssetOffcanvas').on('hidden.bs.offcanvas', function () {
        $('#receiveAssetForm')[0].reset();
        $('#receiveAssetTag').prop('disabled', false).attr('placeholder', 'Leave blank to auto-generate');
        $('#receiveSerialNumber').prop('disabled', false).attr('placeholder', '');
    });

    const today = new Date().toISOString().split('T')[0];
    $('input[name="purchaseDate"]').val(today);
});
