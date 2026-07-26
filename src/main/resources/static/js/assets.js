$(document).ready(function () {
    const clearAssetFiltersBtn = document.getElementById('clearAssetFiltersBtn');
    const tableStateKey = 'assetsTableState';

    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn',
        successToastId: 'successToast',
        initializeSelect2Modals: true
    });

    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
        bootstrap.Tooltip.getOrCreateInstance(element);
    });

    if (clearAssetFiltersBtn) {
        clearAssetFiltersBtn.addEventListener('click', function () {
            MISDCommon.clearDataTableFilters(assetsTable, { stateKey: tableStateKey });
        });
    }

    const assetDocumentConfig = {
        refType: 'IT_EQUIPMENT',
        tableSelector: '#assetDocumentsTable',
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
        pageLength: 10,
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

    function resetDetailPanelScroll() {
        const offcanvasBody = document.querySelector('#itDetailOffcanvas .offcanvas-body');
        if (offcanvasBody) {
            offcanvasBody.scrollTop = 0;
        }
    }

    function escapeValue(value) {
        return MISDCommon.escapeHtml(value == null || value === '' ? 'N/A' : String(value));
    }

    function formatSpecificationValue(value) {
        if (value == null || value === '') {
            return 'N/A';
        }

        if (Array.isArray(value)) {
            return value.map(item => item == null ? '' : String(item)).filter(Boolean).join(', ') || 'N/A';
        }

        if (typeof value === 'object') {
            return JSON.stringify(value);
        }

        return String(value);
    }

    function renderSpecifications(specifications) {
        if (!specifications) {
            return '<div class="text-muted">N/A</div>';
        }

        let parsed = specifications;
        if (typeof specifications === 'string') {
            try {
                parsed = JSON.parse(specifications);
            } catch (error) {
                return `<div class="mt-2 small">${MISDCommon.escapeHtml(specifications)}</div>`;
            }
        }

        if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
            return `<div class="mt-2 small">${MISDCommon.escapeHtml(formatSpecificationValue(parsed))}</div>`;
        }

        const entries = Object.entries(parsed);
        if (!entries.length) {
            return '<div class="text-muted">N/A</div>';
        }

        const rows = entries.map(([key, value]) => {
            return `<li class="d-flex justify-content-between gap-2 py-1 border-bottom">
                        <span class="fw-semibold">${MISDCommon.escapeHtml(key)}</span>
                        <span class="text-end">${MISDCommon.escapeHtml(formatSpecificationValue(value))}</span>
                    </li>`;
        }).join('');

        return `<ul class="list-unstyled mb-0 mt-2">${rows}</ul>`;
    }

    function renderCatalogSummary(data) {
        const specHtml = renderSpecifications(data.catalogSpecifications);

        return `
            <div class="d-grid gap-2">
                <div><span class="text-muted fw-semibold">Category:</span> ${escapeValue(data.catalogCategory)}</div>
                <div><span class="text-muted fw-semibold">Manufacturer:</span> ${escapeValue(data.catalogManufacturer)}</div>
                <div><span class="text-muted fw-semibold">Model Name:</span> ${escapeValue(data.catalogModelName)}</div>
                <div><span class="text-muted fw-semibold">Specifications:</span>${specHtml}</div>
            </div>
        `;
    }

    function renderAssigneeSummary(data) {
        const managerName = data.assigneeManagerFullName || 'N/A';

        return `
            <div class="d-grid gap-2">
                <div><span class="text-muted fw-semibold">Employee ID:</span> ${escapeValue(data.assigneeEmployeeID)}</div>
                <div><span class="text-muted fw-semibold">Full Name:</span> ${escapeValue(data.assigneeFullName)}</div>
                <div><span class="text-muted fw-semibold">Department:</span> ${escapeValue(data.assigneeDepartment)}</div>
                <div><span class="text-muted fw-semibold">Division:</span> ${escapeValue(data.assigneeDivision)}</div>
                <div><span class="text-muted fw-semibold">Manager's Full Name:</span> ${escapeValue(managerName)}</div>
            </div>
        `;
    }

    function loadAssetDocuments(assetTag) {
        MISDCommon.loadDocumentsForReference({
            refType: assetDocumentConfig.refType,
            refId: assetTag,
            tableSelector: assetDocumentConfig.tableSelector,
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

    $('#itDetailOffcanvas').on('shown.bs.offcanvas', function () {
        resetDetailPanelScroll();
    });

    // Enable Edit Mode
    $('#enableEditBtn').on('click', function () {
        // Unlock all fields except Asset Tag.
        // Serial Number is only editable in edit mode when initially blank.
        $('.it-field').not('#editAssetTag').prop('disabled', false);
        if (!isSerialEditableForCurrentAsset) {
            $('#editSerialNumber').prop('disabled', true);
        }

        // Keep 'Assigned To' locked unless deployment status indicates active assignment.
        if ($('#editDeploymentStatus').val() !== 'Deployed' && $('#editDeploymentStatus').val() !== 'Deployed / Assigned' && $('#editDeploymentStatus').val() !== 'On Loan') {
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
    $('#editDeploymentStatus').on('change', function () {
        if ($(this).val() === 'Deployed' || $(this).val() === 'Deployed / Assigned' || $(this).val() === 'On Loan') {
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
            $('#assetCatalogSummary').html(renderCatalogSummary(data));
            $('#assetAssigneeSummary').html(renderAssigneeSummary(data));

            isSerialEditableForCurrentAsset = serialNumberValue.trim() === '';

            let pDate = data.purchaseDate ? data.purchaseDate.split('T')[0] : '';
            $('#editPurchaseDate').val(pDate);

            $('#editPurchasePrice').val(data.purchasePrice);
            $('#editCurrentOwnerID').val(data.currentOwnerID);

            $('#editDeploymentStatus').val(data.deploymentStatus);
            $('#editMaintenanceHealthStatus').val(data.maintenanceHealthStatus);
            $('#editLifecycleStatus').val(data.lifecycleStatus);
            $('#editRemarks').val(data.remarks);

            lockForm(); // Ensure form starts locked
            loadAssetDocuments(data.assetTag);
            resetDetailPanelScroll();

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
                    tableSelector: assetDocumentConfig.tableSelector,
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
            useModalConfirm: true,
            confirmTitle: 'Delete Attachment',
            confirmMessage: 'Remove this attachment from the selected asset?',
            confirmButtonText: 'Delete',
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
            tableSelector: assetDocumentConfig.tableSelector,
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