$(document).ready(function () {
    let currentFleetReferenceId = null;

    const fleetDocumentConfig = {
        refType: 'VEHICLE',
        bodySelector: '#fleetDocumentsTableBody',
        emptySelector: '#fleetDocumentsEmpty',
        printButtonClass: 'fleet-doc-print',
        deleteButtonClass: 'fleet-doc-delete',
        emptyText: 'No documents attached yet.',
        loadErrorText: 'Unable to load documents.',
        fileInputSelector: '#fleetDetailDocumentFiles',
        previewListSelector: '#fleetDetailDocumentPreview',
        previewTemplateSelector: '#fleetDetailDocumentCategoryTemplate'
    };

    function loadFleetDocuments(refId) {
        MISDCommon.loadDocumentsForReference({
            refType: fleetDocumentConfig.refType,
            refId: refId,
            bodySelector: fleetDocumentConfig.bodySelector,
            emptySelector: fleetDocumentConfig.emptySelector,
            printButtonClass: fleetDocumentConfig.printButtonClass,
            deleteButtonClass: fleetDocumentConfig.deleteButtonClass,
            emptyText: fleetDocumentConfig.emptyText,
            loadErrorText: fleetDocumentConfig.loadErrorText
        });
    }

    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn',
        successToastId: 'successToast',
        initializeSelect2Modals: true
    });

    const fleetTable = $('#fleetTable').DataTable(MISDCommon.buildStandardDataTableConfig({
        pageLength: 25,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
        order: [[0, 'asc']]
    }));

    MISDCommon.attachDataTableClearButton({
        filterContainerSelector: '#fleetTable_filter',
        buttonId: 'clearFleetFiltersBtn',
        ariaLabel: 'Clear fleet table search',
        onClear: function () {
            MISDCommon.clearDataTableFilters(fleetTable, { stateKey: 'fleetTableState' });
        }
    });

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
        MISDCommon.renderDocumentPreviewBySelectors(
            fleetDocumentConfig.fileInputSelector,
            fleetDocumentConfig.previewListSelector,
            fleetDocumentConfig.previewTemplateSelector
        );
    });

    $('#uploadFleetDocumentsBtn').on('click', function () {
        if (!currentFleetReferenceId) {
            alert('No vehicle selected.');
            return;
        }

        const uploadSelection = MISDCommon.getDocumentUploadSelection({
            fileInputSelector: fleetDocumentConfig.fileInputSelector,
            previewSelector: fleetDocumentConfig.previewListSelector
        });

        if (!uploadSelection.isValid) {
            if (uploadSelection.message) {
                alert(uploadSelection.message);
            }
            return;
        }

        const formData = MISDCommon.buildDocumentUploadFormData({
            refType: fleetDocumentConfig.refType,
            refId: currentFleetReferenceId,
            files: uploadSelection.files,
            categorySelects: uploadSelection.categorySelects
        });

        $.ajax({
            url: '/documents/add',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function () {
                MISDCommon.resetDocumentDetailUI({
                    bodySelector: fleetDocumentConfig.bodySelector,
                    emptySelector: fleetDocumentConfig.emptySelector,
                    emptyText: fleetDocumentConfig.emptyText,
                    fileInputSelector: fleetDocumentConfig.fileInputSelector,
                    previewInputSelector: fleetDocumentConfig.fileInputSelector,
                    previewListSelector: fleetDocumentConfig.previewListSelector,
                    previewTemplateSelector: fleetDocumentConfig.previewTemplateSelector
                });
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
        MISDCommon.resetDocumentDetailUI({
            bodySelector: fleetDocumentConfig.bodySelector,
            emptySelector: fleetDocumentConfig.emptySelector,
            emptyText: fleetDocumentConfig.emptyText,
            fileInputSelector: fleetDocumentConfig.fileInputSelector,
            previewInputSelector: fleetDocumentConfig.fileInputSelector,
            previewListSelector: fleetDocumentConfig.previewListSelector,
            previewTemplateSelector: fleetDocumentConfig.previewTemplateSelector
        });
    });
});
