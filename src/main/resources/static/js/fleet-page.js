$(document).ready(function () {
    const clearFleetFiltersBtn = document.getElementById('clearFleetFiltersBtn');

    function clearFleetFilters(table) {
        table.search('');
        table.columns().search('');
        table.page(0).draw();
        $('.dataTables_filter input').val('').trigger('keyup');
        sessionStorage.removeItem('fleetTableState');
        const params = new URLSearchParams(window.location.search);
        params.delete('search');
        params.delete('page');
        const nextUrl = `${window.location.pathname}${params.toString() ? `?${params.toString()}` : ''}`;
        window.history.replaceState({}, '', nextUrl);
    }

    MISDCommon.setupThemeToggle('themeToggleBtn');

    if (clearFleetFiltersBtn) {
        clearFleetFiltersBtn.addEventListener('click', function () {
            clearFleetFilters(fleetTable);
        });
    }

    MISDCommon.showToast('successToast');

    const fleetTable = $('#fleetTable').DataTable({
        pageLength: 25,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
        order: [[0, 'asc']],
        buttons: [
            { extend: 'csv', className: 'btn btn-primary btn-sm me-2 text-white', text: 'Export to CSV' },
            { extend: 'excel', className: 'btn btn-success btn-sm text-white', text: 'Export to Excel' }
        ],
        dom: "<'row mb-3'<'col-sm-12 col-md-4'l><'col-sm-12 col-md-4 text-center'B><'col-sm-12 col-md-4'f>>" +
            "<'row'<'col-sm-12'tr>>" +
            "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>"
    });

    MISDCommon.initSelect2Modals();

    MISDCommon.bindClick('.action-btn', function (button) {
        const vId = button.data('id');
        const plate = button.data('plate');

        $('#assignVehicleID').val(vId);
        $('#assignPlateDisplay').val(plate);

        $('#returnVehicleID').val(vId);
        $('#returnPlateDisplay').text(plate);

        $('#retireVehicleID').val(vId);
        $('#retirePlateDisplay').text(plate);
    });

    MISDCommon.bindClick('.fleet-detail-link', function (link, event) {
        event.preventDefault();
        loadFleetDetails(link.data('vehicle-id'));
    });

    function loadFleetDetails(vehicleId) {
        $.get('/fleet/' + vehicleId, function (data) {
            $('#fleetDetailPlate').text(data.plateNumber || 'N/A');
            $('#fleetDetailType').text(data.vehicleType || 'N/A');
            $('#fleetDetailMakeModel').text(((data.make || '') + ' ' + (data.model || '')).trim() || 'N/A');
            $('#fleetDetailYear').text(data.manufactureYear || 'N/A');
            $('#fleetDetailDriver').text(data.assignedDriverID || 'Unassigned');
            $('#fleetDetailRegExpiry').text(data.registrationExpiry || 'N/A');
            $('#fleetDetailStatus').text(data.currentStatus || 'N/A');
            bootstrap.Modal.getOrCreateInstance(document.getElementById('fleetDetailModal')).show();
        }).fail(function () {
            alert('Unable to load fleet vehicle details.');
        });
    }
});
