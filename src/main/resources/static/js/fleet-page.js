$(document).ready(function () {
    let currentFleetReferenceId = null;
    let currentFleetData = null;

    function formatDate(value) {
        if (!value) {
            return 'N/A';
        }
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) {
            return value;
        }
        return parsed.toLocaleDateString();
    }

    function formatDateInput(value) {
        if (!value) {
            return '';
        }
        if (typeof value === 'string') {
            return value.split('T')[0];
        }
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) {
            return '';
        }
        return parsed.toISOString().split('T')[0];
    }

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

    function setFleetEditMode(enabled) {
        $('#fleetDetailOffcanvas').toggleClass('fleet-edit-mode-active', enabled);
        $('.fleet-detail-view-only').toggleClass('d-none', enabled);
        $('.fleet-detail-edit-only').toggleClass('d-none', !enabled);
        $('#enableFleetEditBtn').toggleClass('d-none', enabled);
        $('#saveFleetEditBtn, #cancelFleetEditBtn').toggleClass('d-none', !enabled);
        $('.fleet-field').prop('disabled', !enabled);
    }

    function fillFleetEditFields(data) {
        $('#editFleetVehicleID').val(data.vehicleID || '');
        $('#editFleetRegistrationExpiry').val(formatDateInput(data.registrationExpiry));
        $('#editFleetInsuranceExpiry').val(formatDateInput(data.insuranceExpiry));
    }

    function buildFleetUpdatePayload() {
        const vehicleID = Number($('#editFleetVehicleID').val());
        return {
            vehicleID: Number.isNaN(vehicleID) ? null : vehicleID,
            registrationExpiry: $('#editFleetRegistrationExpiry').val() || null,
            insuranceExpiry: $('#editFleetInsuranceExpiry').val() || null
        };
    }

    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn',
        successToastId: 'successToast',
        initializeSelect2Modals: true
    });

    const fleetTable = $('#fleetTable').DataTable(MISDCommon.buildStandardDataTableConfig({
        pageLength: 10,
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
            currentFleetData = data;
            $('#fleetDetailPlate').text(data.plateNumber || 'N/A');
            $('#fleetDetailType').text(data.vehicleType || 'N/A');
            $('#fleetDetailMake').text(data.make || 'N/A');
            $('#fleetDetailModel').text(data.model || 'N/A');
            $('#fleetDetailYear').text(data.manufactureYear || 'N/A');
            $('#fleetDetailBodyNumber').text(data.bodyNumber || 'N/A');
            $('#fleetDetailFuelType').text(data.fuelType || 'N/A');
            $('#fleetDetailEngineNumber').text(data.engineNumber || 'N/A');
            $('#fleetDetailChassisVin').text(data.chassisNumberVIN || 'N/A');
            $('#fleetDetailDriver').text(data.assignedDriverName || 'Unassigned');
            $('#fleetDetailRegExpiry').text(formatDate(data.registrationExpiry));
            $('#fleetDetailInsuranceExpiry').text(formatDate(data.insuranceExpiry));
            $('#fleetDetailCost').text(data.cost || 'N/A');
            $('#fleetDetailStatus').text(data.currentStatus || 'N/A');
            $('#fleetDetailRemarks').text(data.remarks || 'N/A');

            fillFleetEditFields(data);
            setFleetEditMode(false);

            currentFleetReferenceId = data.vehicleID != null ? String(data.vehicleID) : null;
            loadFleetDocuments(currentFleetReferenceId);
            bootstrap.Offcanvas.getOrCreateInstance(document.getElementById('fleetDetailOffcanvas')).show();
        }).fail(function () {
            alert('Unable to load fleet vehicle details.');
        });
    }

    $('#enableFleetEditBtn').on('click', function () {
        setFleetEditMode(true);
    });

    $('#cancelFleetEditBtn').on('click', function () {
        if (!currentFleetData) {
            setFleetEditMode(false);
            return;
        }

        fillFleetEditFields(currentFleetData);
        setFleetEditMode(false);
    });

    $('#saveFleetEditBtn').on('click', function () {
        const payload = buildFleetUpdatePayload();
        if (!payload.vehicleID) {
            alert('No vehicle selected.');
            return;
        }

        $.ajax({
            url: '/fleet/update',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function () {
                location.reload();
            },
            error: function () {
                alert('Failed to save vehicle changes.');
            }
        });
    });

    $('#fleetDetailDocumentFiles').on('change', function () {
        MISDCommon.prepareMultiFileSelection(this);
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
        MISDCommon.deleteDocumentById(docId, {
            onSuccess: function () {
                loadFleetDocuments(currentFleetReferenceId);
            },
            onError: function (xhr) {
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

    $('#fleetDetailOffcanvas').on('hidden.bs.offcanvas', function () {
        currentFleetReferenceId = null;
        currentFleetData = null;
        setFleetEditMode(false);
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
