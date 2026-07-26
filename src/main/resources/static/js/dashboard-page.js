$(document).ready(function () {
    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn',
        successToastId: 'successToast',
        errorToastId: 'errorToast',
        errorToastDelay: 4000
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

    initDashboardTable('#agingTable', [[5, 'desc']], 'No aging equipment found.');
    initDashboardTable('#fleetActionRequiredTable', [[0, 'asc']], 'No aging vehicles found in the fleet.');
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

    function removeSelectedFile(input, fileIndex) {
        const files = Array.from(input.files || []);
        const updatedFiles = files.filter((_, index) => index !== fileIndex);
        MISDCommon.setInputFiles(input, updatedFiles);
        renderDocumentPreview(input);
    }

    function validateFileInputBeforeSubmit(input) {
        return MISDCommon.validateFileInputBySize(input);
    }

    function renderDocumentPreview(input) {
        const previewTarget = input.dataset.documentPreviewTarget;
        const previewList = previewTarget ? document.getElementById(previewTarget) : null;
        const categoryTemplateTarget = input.dataset.documentCategoryTemplateTarget;
        const categoryTemplate = categoryTemplateTarget ? document.getElementById(categoryTemplateTarget) : null;

        if (!previewList) {
            return;
        }

        previewList.innerHTML = '';

        const validationResults = MISDCommon.getFileValidationResults(input);
        const files = validationResults.map(result => result.file);
        const { maxSizeMb } = MISDCommon.getUploadConstraints(input);
        const tooLarge = validationResults.filter(result => !result.sizeAllowed).map(result => result.file.name);

        if (tooLarge.length) {
            MISDCommon.showUploadError(input, `File size exceeds ${maxSizeMb}MB: ${tooLarge.join(', ')}`);
        } else {
            MISDCommon.clearUploadError(input);
        }

        if (files.length === 0) {
            const emptyItem = document.createElement('li');
            emptyItem.className = 'list-group-item text-muted';
            emptyItem.textContent = 'No files selected.';
            previewList.appendChild(emptyItem);
            return;
        }

        validationResults.forEach((result, index) => {
            const file = result.file;
            const item = document.createElement('li');
            item.className = 'list-group-item d-flex flex-column gap-2';

            const fileInfo = document.createElement('div');
            fileInfo.className = 'd-flex justify-content-between align-items-start gap-3';

            const fileName = document.createElement('div');
            fileName.className = 'fw-semibold';
            fileName.textContent = file.name;

            const fileMeta = document.createElement('div');
            fileMeta.className = 'text-muted small';
            fileMeta.textContent = MISDCommon.formatBytes(file.size);

            const fileLabelWrapper = document.createElement('div');
            fileLabelWrapper.className = 'd-flex flex-column';
            fileLabelWrapper.appendChild(fileName);
            fileLabelWrapper.appendChild(fileMeta);

            const removeButton = document.createElement('button');
            removeButton.type = 'button';
            removeButton.className = 'btn btn-sm btn-outline-danger';
            removeButton.textContent = 'X';
            removeButton.setAttribute('aria-label', `Remove ${file.name}`);
            removeButton.addEventListener('click', function () {
                removeSelectedFile(input, index);
            });

            fileInfo.appendChild(fileLabelWrapper);
            fileInfo.appendChild(removeButton);

            const badge = document.createElement('span');
            const typeAllowed = result.typeAllowed;
            const sizeAllowed = result.sizeAllowed;

            if (typeAllowed && sizeAllowed) {
                badge.className = 'badge bg-success align-self-center';
                badge.textContent = 'Ready';
            } else {
                badge.className = 'badge bg-danger align-self-center';
                badge.textContent = !typeAllowed ? 'Invalid type' : 'Too large';
            }

            item.appendChild(fileInfo);

            const footer = document.createElement('div');
            footer.className = 'd-flex flex-column gap-2';

            if (categoryTemplate) {
                const categoryLabel = document.createElement('label');
                categoryLabel.className = 'form-label fw-semibold mb-0 small';
                categoryLabel.innerHTML = 'Document category <span class="required-indicator" aria-hidden="true">*</span>';

                const categorySelect = document.createElement('select');
                categorySelect.className = 'form-select form-select-sm';
                categorySelect.name = 'documentCategories';
                categorySelect.required = true;
                categorySelect.innerHTML = categoryTemplate.innerHTML;

                footer.appendChild(categoryLabel);
                footer.appendChild(categorySelect);
            }

            const metaRow = document.createElement('div');
            metaRow.className = 'd-flex justify-content-between align-items-center';
            metaRow.appendChild(document.createElement('span'));
            metaRow.appendChild(badge);

            footer.appendChild(metaRow);
            item.appendChild(footer);
            previewList.appendChild(item);
        });
    }

    $('.js-document-upload-input').each(function () {
        renderDocumentPreview(this);
        $(this).on('change', function () {
            renderDocumentPreview(this);
        });
    });

    $('#receiveAssetForm, #addVehicleOffcanvas form, #addPropertyOffcanvas form').on('submit', function (event) {
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

        $(this).find('.js-document-upload-input').each(function () {
            renderDocumentPreview(this);
        });
    });

    $('#receiveAssetOffcanvas').on('hidden.bs.offcanvas', function () {
        $('#receiveAssetForm')[0].reset();
        $('#receiveAssetTag').prop('disabled', false).attr('placeholder', 'Leave blank to auto-generate');
        $('#receiveSerialNumber').prop('disabled', false).attr('placeholder', '');
    });

    const today = new Date().toISOString().split('T')[0];
    $('input[name="purchaseDate"]').val(today);
});
