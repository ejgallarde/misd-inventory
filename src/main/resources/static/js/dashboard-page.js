$(document).ready(function () {
    MISDCommon.setupThemeToggle('themeToggleBtn');

    $('button[data-bs-toggle="tab"]').on('shown.bs.tab', function (e) {
        $('button[data-bs-toggle="tab"]').removeClass('text-dark').addClass('text-secondary');
        $(e.target).removeClass('text-secondary').addClass('text-dark');
        const targetKey = $(e.target).attr('data-bs-target')?.replace('#', '');
        if (targetKey) {
            sessionStorage.setItem('dashboardActiveTab', targetKey);
        }
    });

    MISDCommon.showToast('successToast');

    const storedDashboardTab = sessionStorage.getItem('dashboardActiveTab');
    if (storedDashboardTab) {
        const triggerButton = document.querySelector(`#assetTabs button[data-bs-target="#${storedDashboardTab}"]`);
        if (triggerButton) {
            new bootstrap.Tab(triggerButton).show();
        }
    }
    MISDCommon.showToast('errorToast', 4000);

    const defaultTableConfig = {
        pageLength: 10,
        lengthMenu: [[5, 10, 25, -1], [5, 10, 25, 'All']],
        buttons: [
            { extend: 'csv', className: 'btn btn-secondary btn-sm me-1', text: 'Export to CSV' },
            { extend: 'excel', className: 'btn btn-secondary btn-sm', text: 'Export to Excel' }
        ],
        dom: "<'row mb-3'<'col-sm-12 col-md-4'l><'col-sm-12 col-md-4 text-center'B><'col-sm-12 col-md-4'f>>" +
            "<'row'<'col-sm-12'tr>>" +
            "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>"
    };

    function initDashboardTable(selector, order, emptyMessage) {
        return $(selector).DataTable($.extend(true, {}, defaultTableConfig, {
            order: order,
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

    function loadVehicleDetails(id) {
        $.get('/api/assets/fleet/' + id, function (data) {
            $('#editVehicleId').val(data.vehicleId);
            $('#editPlateNumber').val(data.plateNumber);
            $('#editVehicleType').val(data.vehicleType);
            $('#fleetModal').modal('show');
        });
    }

    $('#enableEditBtn').on('click', function () {
        $('.asset-field').prop('disabled', false);
        $(this).addClass('d-none');
        $('#saveEditBtn').removeClass('d-none');
    });

    $('#saveEditBtn').on('click', function () {
        const formData = $('#fleetEditForm').serializeArray();
        const json = {};
        formData.forEach(field => json[field.name] = field.value);

        $.ajax({
            url: '/api/assets/fleet/update',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(json),
            success: function () {
                location.reload();
            }
        });
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

    function formatBytes(bytes) {
        if (!Number.isFinite(bytes) || bytes <= 0) {
            return '0 B';
        }

        const units = ['B', 'KB', 'MB', 'GB'];
        const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
        const value = bytes / Math.pow(1024, index);
        return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
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

        const allowedExtensions = (input.dataset.documentAllowedExtensions || '')
            .split(',')
            .map(value => value.trim().toLowerCase())
            .filter(Boolean);
        const maxSizeMb = Number.parseInt(input.dataset.documentMaxSizeMb || '10', 10);
        const maxSizeBytes = Number.isFinite(maxSizeMb) && maxSizeMb > 0 ? maxSizeMb * 1024 * 1024 : 10 * 1024 * 1024;
        const files = Array.from(input.files || []);

        if (files.length === 0) {
            const emptyItem = document.createElement('li');
            emptyItem.className = 'list-group-item text-muted';
            emptyItem.textContent = 'No files selected.';
            previewList.appendChild(emptyItem);
            return;
        }

        files.forEach(file => {
            const item = document.createElement('li');
            item.className = 'list-group-item d-flex flex-column gap-2';

            const fileInfo = document.createElement('div');
            fileInfo.className = 'd-flex justify-content-between align-items-start gap-3';

            const fileName = document.createElement('div');
            fileName.className = 'fw-semibold';
            fileName.textContent = file.name;

            const fileMeta = document.createElement('div');
            fileMeta.className = 'text-muted small';
            fileMeta.textContent = formatBytes(file.size);

            fileInfo.appendChild(fileName);
            fileInfo.appendChild(fileMeta);

            const badge = document.createElement('span');
            const extension = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : '';
            const typeAllowed = !allowedExtensions.length || allowedExtensions.includes(extension);
            const sizeAllowed = file.size <= maxSizeBytes;

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
                categoryLabel.textContent = 'Document category';

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
