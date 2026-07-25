$(document).ready(function () {
    let currentPropertyReferenceId = null;

    const propertyDocumentConfig = {
        refType: 'PROPERTY',
        bodySelector: '#propertyDocumentsTableBody',
        emptySelector: '#propertyDocumentsEmpty',
        printButtonClass: 'property-doc-print',
        deleteButtonClass: 'property-doc-delete',
        emptyText: 'No documents attached yet.',
        loadErrorText: 'Unable to load documents.',
        fileInputSelector: '#propertyDetailDocumentFiles',
        previewListSelector: '#propertyDetailDocumentPreview',
        previewTemplateSelector: '#propertyDetailDocumentCategoryTemplate'
    };

    function loadPropertyDocuments(refId) {
        MISDCommon.loadDocumentsForReference({
            refType: propertyDocumentConfig.refType,
            refId: refId,
            bodySelector: propertyDocumentConfig.bodySelector,
            emptySelector: propertyDocumentConfig.emptySelector,
            printButtonClass: propertyDocumentConfig.printButtonClass,
            deleteButtonClass: propertyDocumentConfig.deleteButtonClass,
            emptyText: propertyDocumentConfig.emptyText,
            loadErrorText: propertyDocumentConfig.loadErrorText
        });
    }

    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn',
        successToastId: 'successToast',
        initializeSelect2Modals: true
    });

    const propertiesTable = $('#propertiesTable').DataTable(MISDCommon.buildStandardDataTableConfig({
        pageLength: 25,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
        order: [[1, 'asc']]
    }));

    MISDCommon.attachDataTableClearButton({
        filterContainerSelector: '#propertiesTable_filter',
        buttonId: 'clearPropertyFiltersBtn',
        ariaLabel: 'Clear properties table search',
        onClear: function () {
            MISDCommon.clearDataTableFilters(propertiesTable, { stateKey: 'propertiesTableState' });
        }
    });

    MISDCommon.bindClick('.action-btn', function (button) {
        const pId = button.data('id');
        const pName = button.data('name');

        $('#custodianPropID').val(pId);
        $('#custodianPropDisplay').val(pName);

        $('#taxPropID').val(pId);
        $('#taxPropDisplay').text(pName);
    });

    MISDCommon.bindClick('.property-detail-link', function (link, event) {
        event.preventDefault();
        const propertyId = link.data('property-id');
        if (!propertyId) {
            return;
        }

        $.get('/properties/' + propertyId, function (data) {
            $('#propertyDetailName').text(data.propertyName || 'N/A');
            $('#propertyDetailType').text(data.propertyType || 'N/A');
            $('#propertyDetailTitle').text(data.titleNumber || 'N/A');
            $('#propertyDetailRegion').text(data.region || 'N/A');
            $('#propertyDetailTaxStatus').text(data.propertyTaxStatus || 'N/A');
            $('#propertyDetailStatus').text(data.currentStatus || 'N/A');

            currentPropertyReferenceId = data.propertyID != null ? String(data.propertyID) : null;
            loadPropertyDocuments(currentPropertyReferenceId);

            bootstrap.Modal.getOrCreateInstance(document.getElementById('propertyDetailModal')).show();
        }).fail(function () {
            alert('Unable to load property details.');
        });
    });

    $('#propertyDetailDocumentFiles').on('change', function () {
        MISDCommon.renderDocumentPreviewBySelectors(
            propertyDocumentConfig.fileInputSelector,
            propertyDocumentConfig.previewListSelector,
            propertyDocumentConfig.previewTemplateSelector
        );
    });

    $('#uploadPropertyDocumentsBtn').on('click', function () {
        if (!currentPropertyReferenceId) {
            alert('No property selected.');
            return;
        }

        const uploadSelection = MISDCommon.getDocumentUploadSelection({
            fileInputSelector: propertyDocumentConfig.fileInputSelector,
            previewSelector: propertyDocumentConfig.previewListSelector
        });

        if (!uploadSelection.isValid) {
            if (uploadSelection.message) {
                alert(uploadSelection.message);
            }
            return;
        }

        const formData = MISDCommon.buildDocumentUploadFormData({
            refType: propertyDocumentConfig.refType,
            refId: currentPropertyReferenceId,
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
                    bodySelector: propertyDocumentConfig.bodySelector,
                    emptySelector: propertyDocumentConfig.emptySelector,
                    emptyText: propertyDocumentConfig.emptyText,
                    fileInputSelector: propertyDocumentConfig.fileInputSelector,
                    previewInputSelector: propertyDocumentConfig.fileInputSelector,
                    previewListSelector: propertyDocumentConfig.previewListSelector,
                    previewTemplateSelector: propertyDocumentConfig.previewTemplateSelector
                });
                loadPropertyDocuments(currentPropertyReferenceId);
            },
            error: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to upload document(s).';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.property-doc-delete', function (button) {
        const docId = button.data('doc-id');
        if (!docId || !confirm('Remove this document?')) {
            return;
        }

        $.ajax({
            url: `/documents/${docId}`,
            type: 'DELETE',
            success: function () {
                loadPropertyDocuments(currentPropertyReferenceId);
            },
            error: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to remove document.';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.property-doc-print', function (button) {
        const docId = button.data('doc-id');
        if (docId) {
            MISDCommon.printDocument(`/documents/${docId}/view`);
        }
    });

    $('#propertyDetailModal').on('hidden.bs.modal', function () {
        currentPropertyReferenceId = null;
        MISDCommon.resetDocumentDetailUI({
            bodySelector: propertyDocumentConfig.bodySelector,
            emptySelector: propertyDocumentConfig.emptySelector,
            emptyText: propertyDocumentConfig.emptyText,
            fileInputSelector: propertyDocumentConfig.fileInputSelector,
            previewInputSelector: propertyDocumentConfig.fileInputSelector,
            previewListSelector: propertyDocumentConfig.previewListSelector,
            previewTemplateSelector: propertyDocumentConfig.previewTemplateSelector
        });
    });
});
