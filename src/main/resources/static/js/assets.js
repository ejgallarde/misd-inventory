$(document).ready(function () {
    const clearAssetFiltersBtn = document.getElementById('clearAssetFiltersBtn');
    const tableStateKey = 'assetsTableState';

    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn',
        successToastId: 'successToast',
        initializeSelect2Modals: true
    });

    if (clearAssetFiltersBtn) {
        clearAssetFiltersBtn.addEventListener('click', function () {
            MISDCommon.clearDataTableFilters(assetsTable, { stateKey: tableStateKey });
        });
    }

    const assetDocumentConfig = {
        refType: 'IT_EQUIPMENT',
        bodySelector: '#assetDocumentsTableBody',
        emptySelector: '#assetDocumentsEmpty',
        printButtonClass: 'asset-doc-print',
        deleteButtonClass: 'asset-doc-delete',
        emptyText: 'No documents attached yet.',
        loadErrorText: 'Unable to load documents.',
        fileInputSelector: '#assetDetailDocumentFiles',
        previewListSelector: '#assetDetailDocumentPreview',
        previewTemplateSelector: '#assetDetailDocumentCategoryTemplate'
    };

    // DataTables
    const assetsTable = $('#assetsTable').DataTable(MISDCommon.buildStandardDataTableConfig({
        pageLength: 25,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
        order: [[0, 'asc']]
    }));

    assetsTable.on('search.dt draw.dt order.dt page.dt', function () {
        MISDCommon.syncDataTableStateToSessionAndUrl(assetsTable, {
            stateKey: tableStateKey,
            includeOrder: true
        });
    });

    MISDCommon.restoreDataTableStateFromSessionAndUrl(assetsTable, {
        stateKey: tableStateKey,
        restoreOrder: true,
        warningMessage: 'Unable to restore saved asset table state.'
    });

    // --- SLIDEOUT & EDIT LOGIC ---

    let currentActiveAssetTag = ''; // Tracks the currently open asset
    let isSerialEditableForCurrentAsset = false;

    function setAssetEditMode(active) {
        $('#itDetailOffcanvas').toggleClass('asset-edit-mode-active', active);
    }

    function loadAssetDocuments(assetTag) {
        MISDCommon.loadDocumentsForReference({
            refType: assetDocumentConfig.refType,
            refId: assetTag,
            bodySelector: assetDocumentConfig.bodySelector,
            emptySelector: assetDocumentConfig.emptySelector,
            printButtonClass: assetDocumentConfig.printButtonClass,
            deleteButtonClass: assetDocumentConfig.deleteButtonClass,
            emptyText: assetDocumentConfig.emptyText,
            loadErrorText: assetDocumentConfig.loadErrorText
        });
    }

    // Helper function to lock the form back to view-only mode
    function lockForm() {
        $('.it-field').prop('disabled', true);
        setAssetEditMode(false);
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
        // Unlock all fields except Asset Tag.
        // Serial Number is only editable in edit mode when initially blank.
        $('.it-field').not('#editAssetTag').prop('disabled', false);
        if (!isSerialEditableForCurrentAsset) {
            $('#editSerialNumber').prop('disabled', true);
        }

        // Keep 'Assigned To' locked unless Status is already 'Deployed'
        if ($('#editCurrentStatus').val() !== 'Deployed') {
            $('#editCurrentOwnerID').prop('disabled', true);
        }

        setAssetEditMode(true);
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

        MISDCommon.syncDataTableStateToSessionAndUrl(assetsTable, {
            stateKey: tableStateKey,
            includeOrder: true
        });

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
            const serialNumberValue = data.serialNumber == null ? '' : String(data.serialNumber);

            $('#editAssetTag').val(data.assetTag);
            $('#editCatalogID').val(data.catalogID);
            $('#editSerialNumber').val(serialNumberValue);

            isSerialEditableForCurrentAsset = serialNumberValue.trim() === '';

            let pDate = data.purchaseDate ? data.purchaseDate.split('T')[0] : '';
            $('#editPurchaseDate').val(pDate);

            $('#editPurchasePrice').val(data.purchasePrice);
            $('#editCurrentOwnerID').val(data.currentOwnerID);

            // Because we changed this to a <select>, this will automatically 
            // select the correct option if it matches exactly.
            $('#editCurrentStatus').val(data.currentStatus);
            $('#editRemarks').val(data.remarks);

            lockForm(); // Ensure form starts locked
            loadAssetDocuments(data.assetTag);

            // Show the slideout
            // Show the slideout (prevents backdrop stacking bug)
            const detailPanel = bootstrap.Offcanvas.getOrCreateInstance(document.getElementById('itDetailOffcanvas'));
            detailPanel.show();
        }).fail(function () {
            console.error("Failed to fetch asset details.");
            alert("Could not load asset details.");
        });
    }

    $('#assetDetailDocumentFiles').on('change', function () {
        MISDCommon.prepareMultiFileSelection(this);
        MISDCommon.renderDocumentPreviewBySelectors(
            assetDocumentConfig.fileInputSelector,
            assetDocumentConfig.previewListSelector,
            assetDocumentConfig.previewTemplateSelector
        );
    });

    $('#uploadAssetDocumentsBtn').on('click', function () {
        if (!currentActiveAssetTag) {
            alert('No asset selected.');
            return;
        }

        const uploadSelection = MISDCommon.getDocumentUploadSelection({
            fileInputSelector: assetDocumentConfig.fileInputSelector,
            previewSelector: assetDocumentConfig.previewListSelector
        });

        if (!uploadSelection.isValid) {
            if (uploadSelection.message) {
                alert(uploadSelection.message);
            }
            return;
        }

        const formData = MISDCommon.buildDocumentUploadFormData({
            refType: assetDocumentConfig.refType,
            refId: currentActiveAssetTag,
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
                    bodySelector: assetDocumentConfig.bodySelector,
                    emptySelector: assetDocumentConfig.emptySelector,
                    emptyText: assetDocumentConfig.emptyText,
                    fileInputSelector: assetDocumentConfig.fileInputSelector,
                    previewInputSelector: assetDocumentConfig.fileInputSelector,
                    previewListSelector: assetDocumentConfig.previewListSelector,
                    previewTemplateSelector: assetDocumentConfig.previewTemplateSelector
                });
                loadAssetDocuments(currentActiveAssetTag);
            },
            error: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to upload document(s).';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.asset-doc-delete', function (button) {
        const docId = button.data('doc-id');
        MISDCommon.deleteDocumentById(docId, {
            onSuccess: function () {
                loadAssetDocuments(currentActiveAssetTag);
            },
            onError: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to remove document.';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.asset-doc-print', function (button) {
        const docId = button.data('doc-id');
        if (docId) {
            MISDCommon.printDocument(`/documents/${docId}/view`);
        }
    });

    $('#itDetailOffcanvas').on('hidden.bs.offcanvas', function () {
        currentActiveAssetTag = '';
        MISDCommon.resetDocumentDetailUI({
            bodySelector: assetDocumentConfig.bodySelector,
            emptySelector: assetDocumentConfig.emptySelector,
            emptyText: assetDocumentConfig.emptyText,
            fileInputSelector: assetDocumentConfig.fileInputSelector,
            previewInputSelector: assetDocumentConfig.fileInputSelector,
            previewListSelector: assetDocumentConfig.previewListSelector,
            previewTemplateSelector: assetDocumentConfig.previewTemplateSelector
        });
    });

    // Persist the current table view before action forms redirect back to the list
    $('form').on('submit', function (event) {
        const state = MISDCommon.getDataTableState(assetsTable, true);
        MISDCommon.appendDataTableStateToFormAction(this, state);
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