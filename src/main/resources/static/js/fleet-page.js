$(document).ready(function () {
    let currentFleetReferenceId = null;

    function renderFleetDocuments(documents) {
        const body = $('#fleetDocumentsTableBody');
        const empty = $('#fleetDocumentsEmpty');
        body.empty();

        if (!documents || !documents.length) {
            empty.removeClass('d-none');
            return;
        }

        empty.addClass('d-none');
        documents.forEach(doc => {
            const viewUrl = `/documents/${doc.documentId}/view`;
            const downloadUrl = `/documents/${doc.documentId}/download`;
            body.append(`
                <tr>
                    <td>${MISDCommon.escapeHtml(doc.documentCategory || 'N/A')}</td>
                    <td>
                        <div class="fw-semibold">${MISDCommon.escapeHtml(doc.fileName || 'Unnamed file')}</div>
                        <div class="small text-muted">${MISDCommon.formatBytes(doc.fileSize || 0)}</div>
                    </td>
                    <td>${MISDCommon.escapeHtml(MISDCommon.formatUploadDate(doc.uploadDate))}</td>
                    <td class="text-center">
                        <div class="btn-group btn-group-sm" role="group">
                            <a class="btn btn-outline-primary" href="${viewUrl}" target="_blank">View</a>
                            <a class="btn btn-outline-success" href="${downloadUrl}">Download</a>
                            <button type="button" class="btn btn-outline-secondary fleet-doc-print" data-doc-id="${doc.documentId}">Print</button>
                            <button type="button" class="btn btn-outline-danger fleet-doc-delete" data-doc-id="${doc.documentId}">Remove</button>
                        </div>
                    </td>
                </tr>
            `);
        });
    }

    function loadFleetDocuments(refId) {
        if (!refId) {
            renderFleetDocuments([]);
            return;
        }

        $.get('/documents/list', { refType: 'VEHICLE', refId: refId })
            .done(function (documents) {
                renderFleetDocuments(documents || []);
            })
            .fail(function () {
                $('#fleetDocumentsEmpty').removeClass('d-none').text('Unable to load documents.');
                $('#fleetDocumentsTableBody').empty();
            });
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

    MISDCommon.setupThemeToggle('themeToggleBtn');

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

    const fleetFilterContainer = $('#fleetTable_filter');
    const fleetSearchInput = fleetFilterContainer.find('input[type="search"]');

    fleetSearchInput.addClass('me-2');
    if (!document.getElementById('clearFleetFiltersBtn')) {
        fleetFilterContainer.append(
            '<button class="btn btn-outline-secondary btn-sm" type="button" id="clearFleetFiltersBtn" aria-label="Clear fleet table search">Clear</button>'
        );
    }

    $('#clearFleetFiltersBtn').on('click', function () {
        clearFleetFilters(fleetTable);
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
            currentFleetReferenceId = data.vehicleID != null ? String(data.vehicleID) : null;
            loadFleetDocuments(currentFleetReferenceId);
            bootstrap.Modal.getOrCreateInstance(document.getElementById('fleetDetailModal')).show();
        }).fail(function () {
            alert('Unable to load fleet vehicle details.');
        });
    }

    $('#fleetDetailDocumentFiles').on('change', function () {
        MISDCommon.renderDocumentPreviewBySelectors('#fleetDetailDocumentFiles', '#fleetDetailDocumentPreview', '#fleetDetailDocumentCategoryTemplate');
    });

    $('#uploadFleetDocumentsBtn').on('click', function () {
        if (!currentFleetReferenceId) {
            alert('No vehicle selected.');
            return;
        }

        const fileInput = document.getElementById('fleetDetailDocumentFiles');
        if (!MISDCommon.validateFileInputBySize(fileInput)) {
            return;
        }

        const files = Array.from(fileInput.files || []);
        if (!files.length) {
            alert('Please select file(s) to upload.');
            return;
        }

        const categorySelects = Array.from(document.querySelectorAll('#fleetDetailDocumentPreview select[name="documentCategories"]'));
        const missingCategory = categorySelects.some(select => !select.value);
        if (missingCategory || categorySelects.length !== files.length) {
            alert('Select one document category for each file.');
            return;
        }

        const formData = new FormData();
        formData.append('refType', 'VEHICLE');
        formData.append('refId', currentFleetReferenceId);
        files.forEach(file => formData.append('documentFiles', file));
        categorySelects.forEach(select => formData.append('documentCategories', select.value));

        $.ajax({
            url: '/documents/add',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function () {
                fileInput.value = '';
                MISDCommon.renderDocumentPreviewBySelectors('#fleetDetailDocumentFiles', '#fleetDetailDocumentPreview', '#fleetDetailDocumentCategoryTemplate');
                loadFleetDocuments(currentFleetReferenceId);
            },
            error: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to upload document(s).';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.fleet-doc-delete', function (button) {
        const docId = button.data('doc-id');
        if (!docId || !confirm('Remove this document?')) {
            return;
        }

        $.ajax({
            url: `/documents/${docId}`,
            type: 'DELETE',
            success: function () {
                loadFleetDocuments(currentFleetReferenceId);
            },
            error: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to remove document.';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.fleet-doc-print', function (button) {
        const docId = button.data('doc-id');
        if (docId) {
            MISDCommon.printDocument(`/documents/${docId}/view`);
        }
    });

    $('#fleetDetailModal').on('hidden.bs.modal', function () {
        currentFleetReferenceId = null;
        $('#fleetDocumentsTableBody').empty();
        $('#fleetDocumentsEmpty').removeClass('d-none').text('No documents attached yet.');
        const fileInput = document.getElementById('fleetDetailDocumentFiles');
        if (fileInput) {
            fileInput.value = '';
        }
        MISDCommon.renderDocumentPreviewBySelectors('#fleetDetailDocumentFiles', '#fleetDetailDocumentPreview', '#fleetDetailDocumentCategoryTemplate');
    });
});
