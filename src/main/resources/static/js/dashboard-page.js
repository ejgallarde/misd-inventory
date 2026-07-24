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

    $('#receiveAssetOffcanvas').on('hidden.bs.offcanvas', function () {
        $('#receiveAssetForm')[0].reset();
        $('#receiveAssetTag').prop('disabled', false).attr('placeholder', 'Leave blank to auto-generate');
        $('#receiveSerialNumber').prop('disabled', false).attr('placeholder', '');
    });

    const today = new Date().toISOString().split('T')[0];
    $('input[name="purchaseDate"]').val(today);
});
