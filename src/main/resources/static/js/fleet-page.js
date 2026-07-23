$(document).ready(function () {
    const themeToggleBtn = document.getElementById('themeToggleBtn');
    const clearFleetFiltersBtn = document.getElementById('clearFleetFiltersBtn');

    function applyTheme(theme) {
        document.body.setAttribute('data-theme', theme);
        document.documentElement.setAttribute('data-bs-theme', theme);
        if (themeToggleBtn) {
            themeToggleBtn.textContent = theme === 'dark' ? '☀️ Light' : '🌙 Dark';
            themeToggleBtn.setAttribute('title', theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
            themeToggleBtn.setAttribute('aria-label', theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
        }
        localStorage.setItem('misd-theme', theme);
    }

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

    const savedTheme = localStorage.getItem('misd-theme') || 'light';
    applyTheme(savedTheme);
    if (themeToggleBtn) {
        themeToggleBtn.addEventListener('click', function () {
            const nextTheme = document.body.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
            applyTheme(nextTheme);
        });
    }

    if (clearFleetFiltersBtn) {
        clearFleetFiltersBtn.addEventListener('click', function () {
            clearFleetFilters(fleetTable);
        });
    }

    const toastEl = document.getElementById('successToast');
    if (toastEl) {
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
    }

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

    $('.modal').on('shown.bs.modal', function () {
        $(this).find('.select2-dropdown').select2({
            theme: 'bootstrap-5',
            dropdownParent: $(this),
            placeholder: 'Type a name to search...',
            minimumInputLength: 2,
            ajax: {
                url: '/api/personnel/search',
                dataType: 'json',
                delay: 250,
                data: function (params) {
                    return {
                        q: params.term || '',
                        page: (params.page || 1) - 1
                    };
                },
                processResults: function (data) {
                    return {
                        results: data.results,
                        pagination: { more: data.pagination.more }
                    };
                },
                cache: true
            }
        });
    });

    $('.modal').on('hidden.bs.modal', function () {
        $(this).find('.select2-dropdown').select2('destroy');
    });

    $('.action-btn').on('click', function () {
        const vId = $(this).data('id');
        const plate = $(this).data('plate');

        $('#assignVehicleID').val(vId);
        $('#assignPlateDisplay').val(plate);

        $('#returnVehicleID').val(vId);
        $('#returnPlateDisplay').text(plate);

        $('#retireVehicleID').val(vId);
        $('#retirePlateDisplay').text(plate);
    });

    window.loadFleetDetails = function (vehicleId) {
        $.get('/fleet/' + vehicleId, function (data) {
            $('#fleetDetailPlate').text(data.plateNumber || 'N/A');
            $('#fleetDetailType').text(data.vehicleType || 'N/A');
            $('#fleetDetailMakeModel').text(((data.make || '') + ' ' + (data.model || '')).trim() || 'N/A');
            $('#fleetDetailYear').text(data.manufactureYear || 'N/A');
            $('#fleetDetailDriver').text(data.assignedDriverID || 'Unassigned');
            $('#fleetDetailRegExpiry').text(data.registrationExpiry || 'N/A');
            $('#fleetDetailStatus').text(data.currentStatus || 'N/A');
            $('#fleetDetailModal').modal('show');
        }).fail(function () {
            alert('Unable to load fleet vehicle details.');
        });
    };
});
