$(document).ready(function () {
    const clearAssetFiltersBtn = document.getElementById('clearAssetFiltersBtn');
    const tableStateKey = 'assetsTableState';

    function clearAssetFilters(table) {
        table.search('');
        table.columns().search('');
        table.page(0).draw();
        $('.dataTables_filter input').val('').trigger('keyup');
        sessionStorage.removeItem(tableStateKey);
        const params = new URLSearchParams(window.location.search);
        params.delete('search');
        params.delete('page');
        const nextUrl = `${window.location.pathname}${params.toString() ? `?${params.toString()}` : ''}`;
        window.history.replaceState({}, '', nextUrl);
    }

    MISDCommon.setupThemeToggle('themeToggleBtn');

    if (clearAssetFiltersBtn) {
        clearAssetFiltersBtn.addEventListener('click', function () {
            clearAssetFilters(assetsTable);
        });
    }

    function getAssetsTableState(table) {
        return {
            search: table.search(),
            page: table.page.info().page,
            order: table.order()
        };
    }

    function syncAssetsTableStateToUrl(table) {
        const state = getAssetsTableState(table);
        const params = new URLSearchParams(window.location.search);

        if (state.search) {
            params.set('search', state.search);
        } else {
            params.delete('search');
        }

        params.set('page', String(state.page));

        const nextUrl = `${window.location.pathname}${params.toString() ? `?${params.toString()}` : ''}`;
        window.history.replaceState({}, '', nextUrl);

        sessionStorage.setItem(tableStateKey, JSON.stringify(state));
    }

    function restoreAssetsTableState(table) {
        const savedState = sessionStorage.getItem(tableStateKey);
        const params = new URLSearchParams(window.location.search);
        const urlSearch = params.get('search') || '';
        const urlPage = Number(params.get('page')) || 0;

        try {
            const state = savedState ? JSON.parse(savedState) : null;
            const searchValue = urlSearch || (state && state.search ? state.search : '');
            const pageValue = Number.isFinite(urlPage) && urlPage >= 0 ? urlPage : (state && typeof state.page === 'number' ? state.page : 0);

            if (searchValue) {
                table.search(searchValue);
            } else {
                table.search('');
            }

            if (state && state.order) {
                table.order(state.order);
            }

            table.page(pageValue).draw(false);
        } catch (error) {
            console.warn('Unable to restore saved asset table state.', error);
        }
    }

    MISDCommon.showToast('successToast');

    // DataTables
    const assetsTable = $('#assetsTable').DataTable({
        pageLength: 25,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, "All"]],
        order: [[0, "asc"]],
        buttons: [
            { extend: 'csv', className: 'btn btn-primary btn-sm me-2 text-white', text: 'Export to CSV' },
            { extend: 'excel', className: 'btn btn-success btn-sm text-white', text: 'Export to Excel' }
        ],
        dom: "<'row mb-3'<'col-sm-12 col-md-4'l><'col-sm-12 col-md-4 text-center'B><'col-sm-12 col-md-4'f>>" +
            "<'row'<'col-sm-12'tr>>" +
            "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>"
    });

    assetsTable.on('search.dt draw.dt order.dt page.dt', function () {
        syncAssetsTableStateToUrl(assetsTable);
    });

    restoreAssetsTableState(assetsTable);

    MISDCommon.initSelect2Modals();

    // --- SLIDEOUT & EDIT LOGIC ---

    let currentActiveAssetTag = ''; // Tracks the currently open asset

    // Helper function to lock the form back to view-only mode
    function lockForm() {
        $('.it-field').prop('disabled', true);
        $('#enableEditBtn').removeClass('d-none');
        $('#saveEditBtn, #cancelEditBtn').addClass('d-none');
    }

    // Trigger slideout on Asset Tag click
    MISDCommon.bindClick('.asset-detail-link', function (link) {
        currentActiveAssetTag = link.data('assettag');
        loadAssetDetails(currentActiveAssetTag);
    });

    // Enable Edit Mode
    $('#enableEditBtn').on('click', function () {
        // Unlock all fields EXCEPT Asset Tag and Serial Number
        $('.it-field').not('#editAssetTag, #editSerialNumber').prop('disabled', false);

        // Keep 'Assigned To' locked unless Status is already 'Deployed'
        if ($('#editCurrentStatus').val() !== 'Deployed') {
            $('#editCurrentOwnerID').prop('disabled', true);
        }

        $(this).addClass('d-none');
        $('#saveEditBtn, #cancelEditBtn').removeClass('d-none');
    });

    // Cancel Edit Mode
    $('#cancelEditBtn').on('click', function () {
        // Re-fetch original data to reset the form
        loadAssetDetails(currentActiveAssetTag);
    });

    // Listen for Status Changes
    $('#editCurrentStatus').on('change', function () {
        if ($(this).val() === 'Deployed') {
            $('#editCurrentOwnerID').prop('disabled', false);
        } else {
            $('#editCurrentOwnerID').prop('disabled', true).val(''); // Clear owner if not deployed
        }
    });

    // Save the updated data
    $('#saveEditBtn').on('click', function () {
        // Temporarily unlock ALL fields. jQuery's .serializeArray() ignores disabled inputs,
        // so we must enable them right before saving to ensure AssetTag and SerialNumber are sent to the server.
        $('.it-field').prop('disabled', false);

        const formData = $('#itEditForm').serializeArray();
        const jsonPayload = {};
        formData.forEach(field => jsonPayload[field.name] = field.value);

        lockForm(); // Re-lock immediately to prevent UI flashes

        syncAssetsTableStateToUrl(assetsTable);

        $.ajax({
            url: '/assets/update',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(jsonPayload),
            success: function () {
                location.reload(); // Refresh the table to reflect changes
            },
            error: function () {
                alert("Failed to save changes. Please check server logs.");
                $('#cancelEditBtn').trigger('click'); // Reset UI on failure
            }
        });
    });

    // Function to fetch and load asset data into the slideout
    function loadAssetDetails(id) {
        let url = `/assets/${id}`;

        $.get(url, function (data) {
            $('#editAssetTag').val(data.assetTag);
            $('#editCatalogID').val(data.catalogID);
            $('#editSerialNumber').val(data.serialNumber);

            let pDate = data.purchaseDate ? data.purchaseDate.split('T')[0] : '';
            $('#editPurchaseDate').val(pDate);

            $('#editPurchasePrice').val(data.purchasePrice);
            $('#editCurrentOwnerID').val(data.currentOwnerID);

            // Because we changed this to a <select>, this will automatically 
            // select the correct option if it matches exactly.
            $('#editCurrentStatus').val(data.currentStatus);
            $('#editRemarks').val(data.remarks);

            lockForm(); // Ensure form starts locked

            // Show the slideout
            // Show the slideout (prevents backdrop stacking bug)
            const detailPanel = bootstrap.Offcanvas.getOrCreateInstance(document.getElementById('itDetailOffcanvas'));
            detailPanel.show();
        }).fail(function () {
            console.error("Failed to fetch asset details.");
            alert("Could not load asset details.");
        });
    }

    // Persist the current table view before action forms redirect back to the list
    $('form').on('submit', function (event) {
        const form = $(this);
        const state = getAssetsTableState(assetsTable);
        const params = new URLSearchParams(window.location.search);

        if (state.search) {
            params.set('search', state.search);
        } else {
            params.delete('search');
        }

        params.set('page', String(state.page));
        const actionUrl = new URL(form.attr('action') || window.location.pathname, window.location.origin);
        actionUrl.search = params.toString();
        form.attr('action', `${actionUrl.pathname}${actionUrl.search}`);
    });

    // Automatically pass the Asset Tag to the Action Modals
    MISDCommon.bindModalShow('.modal', function (modal, triggerElement) {
        const assetTag = triggerElement.data('assettag');

        if (!assetTag) {
            return;
        }

        MISDCommon.populateModalFields(modal, {
            'input[name="assetTag"]': assetTag
        });

        if (modal.attr('id') === 'assignModal') {
            MISDCommon.populateModalFields(modal, {
                '#assignAssetTagDisplay': assetTag
            });
        } else if (modal.attr('id') === 'returnModal') {
            MISDCommon.populateModalFields(modal, {
                '#returnAssetTagDisplay': assetTag
            });
        } else if (modal.attr('id') === 'warrantyModal') {
            MISDCommon.populateModalFields(modal, {
                '#warrantyAssetTagDisplay': assetTag
            });
        } else if (modal.attr('id') === 'unserviceableModal') {
            MISDCommon.populateModalFields(modal, {
                '#unserviceableAssetTagDisplay': assetTag
            });
        } else if (modal.attr('id') === 'retireModal') {
            MISDCommon.populateModalFields(modal, {
                '#retireAssetTagDisplay': assetTag
            });
        }
    });

});