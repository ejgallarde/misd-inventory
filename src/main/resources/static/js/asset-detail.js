(function ($) {
    'use strict';

    const panelSelector = '#itDetailOffcanvas';
    const documentConfig = {
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

    let currentAssetTag = '';
    let serialNumberEditable = false;

    function escapeValue(value) {
        return MISDCommon.escapeHtml(value == null || value === '' ? 'N/A' : String(value));
    }

    function renderCatalogSummary(data) {
        return `<div class="d-grid gap-2">
            <div><span class="text-muted fw-semibold">Category:</span> ${escapeValue(data.catalogCategory)}</div>
            <div><span class="text-muted fw-semibold">Manufacturer:</span> ${escapeValue(data.catalogManufacturer)}</div>
            <div><span class="text-muted fw-semibold">Model Name:</span> ${escapeValue(data.catalogModelName)}</div>
            <div><span class="text-muted fw-semibold">Specifications:</span>${MISDCommon.renderCatalogSpecifications(data.catalogSpecifications)}</div>
        </div>`;
    }

    function renderAssigneeSummary(data) {
        return `<div class="d-grid gap-2">
            <div><span class="text-muted fw-semibold">Employee ID:</span> ${escapeValue(data.assigneeEmployeeID)}</div>
            <div><span class="text-muted fw-semibold">Full Name:</span> ${escapeValue(data.assigneeFullName)}</div>
            <div><span class="text-muted fw-semibold">Department:</span> ${escapeValue(data.assigneeDepartment)}</div>
            <div><span class="text-muted fw-semibold">Division:</span> ${escapeValue(data.assigneeDivision)}</div>
            <div><span class="text-muted fw-semibold">Manager's Full Name:</span> ${escapeValue(data.assigneeManagerFullName)}</div>
        </div>`;
    }

    function loadDocuments(assetTag) {
        MISDCommon.loadDocumentsForReference({
            refType: documentConfig.refType,
            refId: assetTag,
            tableSelector: documentConfig.tableSelector,
            bodySelector: documentConfig.bodySelector,
            emptySelector: documentConfig.emptySelector,
            printButtonClass: documentConfig.printButtonClass,
            deleteButtonClass: documentConfig.deleteButtonClass,
            emptyText: documentConfig.emptyText,
            loadErrorText: documentConfig.loadErrorText
        });
    }

    function lockForm() {
        $('.it-field').prop('disabled', true);
        $(panelSelector).removeClass('asset-edit-mode-active');
        $('#enableEditBtn').removeClass('d-none');
        $('#saveEditBtn, #cancelEditBtn').addClass('d-none');
    }

    function load(assetTag) {
        $.get(`/assets/${encodeURIComponent(assetTag)}`, function (data) {
            const serialNumber = data.serialNumber == null ? '' : String(data.serialNumber);
            currentAssetTag = data.assetTag;
            serialNumberEditable = serialNumber.trim() === '';

            $('#editAssetTag').val(data.assetTag);
            $('#editCatalogID').val(data.catalogID);
            $('#editSerialNumber').val(serialNumber);
            $('#editPurchaseDate').val(data.purchaseDate ? data.purchaseDate.split('T')[0] : '');
            $('#editPurchasePrice').val(data.purchasePrice);
            $('#editCurrentOwnerID').val(data.currentOwnerID);
            $('#editDeploymentStatus').val(data.deploymentStatus);
            $('#editMaintenanceHealthStatus').val(data.maintenanceHealthStatus);
            $('#editLifecycleStatus').val(data.lifecycleStatus);
            $('#editRemarks').val(data.remarks);
            $('#assetCatalogSummary').html(renderCatalogSummary(data));
            $('#assetAssigneeSummary').html(renderAssigneeSummary(data));

            lockForm();
            loadDocuments(data.assetTag);
            const panel = document.querySelector(panelSelector);
            panel.querySelector('.offcanvas-body').scrollTop = 0;
            bootstrap.Offcanvas.getOrCreateInstance(panel).show();
        }).fail(function () {
            alert('Could not load asset details.');
        });
    }

    function uploadDocuments(uploadSelection) {
        if (!uploadSelection.files || !uploadSelection.files.length) {
            location.reload();
            return;
        }

        const formData = MISDCommon.buildDocumentUploadFormData({
            refType: documentConfig.refType,
            refId: currentAssetTag,
            files: uploadSelection.files,
            categorySelects: uploadSelection.categorySelects
        });
        $.ajax({
            url: '/documents/add',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function () { location.reload(); },
            error: function (xhr) {
                alert(xhr?.responseJSON?.error || 'Asset saved, but document upload failed.');
                location.reload();
            }
        });
    }

    function save() {
        const uploadSelection = MISDCommon.getDocumentUploadSelection({
            fileInputSelector: documentConfig.fileInputSelector,
            previewSelector: documentConfig.previewListSelector
        });
        if (uploadSelection.files?.length && !uploadSelection.isValid) {
            if (uploadSelection.message) {
                alert(uploadSelection.message);
            }
            return;
        }

        $('.it-field').prop('disabled', false);
        const payload = {};
        $('#itEditForm').serializeArray().forEach(field => payload[field.name] = field.value);
        lockForm();

        $.ajax({
            url: '/assets/update',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function () { uploadDocuments(uploadSelection); },
            error: function () {
                alert('Failed to save changes. Please check server logs.');
                load(currentAssetTag);
            }
        });
    }

    function init() {
        if (!document.querySelector(panelSelector)) {
            return;
        }

        MISDCommon.bindClick('.asset-detail-link', function (link, event) {
            event.preventDefault();
            const assetTag = link.data('assettag');
            if (assetTag) {
                load(assetTag);
            }
        });

        $('#enableEditBtn').on('click', function () {
            $('.it-field').not('#editAssetTag').prop('disabled', false);
            if (!serialNumberEditable) {
                $('#editSerialNumber').prop('disabled', true);
            }
            $(panelSelector).addClass('asset-edit-mode-active');
            $(this).addClass('d-none');
            $('#saveEditBtn, #cancelEditBtn').removeClass('d-none');
        });

        $('#cancelEditBtn').on('click', function () { load(currentAssetTag); });
        $('#saveEditBtn').on('click', save);
        $('#editDeploymentStatus').on('change', function () {
            const assigned = ['Deployed', 'Deployed / Assigned'].includes($(this).val());
            $('#editCurrentOwnerID').prop('disabled', !assigned);
            if (!assigned) {
                $('#editCurrentOwnerID').val('');
            }
        });

        $('#assetDetailDocumentFiles').on('change', function () {
            MISDCommon.renderDocumentPreviewBySelectors(
                documentConfig.fileInputSelector,
                documentConfig.previewListSelector,
                documentConfig.previewTemplateSelector
            );
        });

        MISDCommon.bindClick('.asset-doc-delete', function (button) {
            MISDCommon.deleteDocumentById(button.data('doc-id'), {
                useModalConfirm: true,
                confirmTitle: 'Delete Attachment',
                confirmMessage: 'Remove this attachment from the selected asset?',
                confirmButtonText: 'Delete',
                onSuccess: function () { loadDocuments(currentAssetTag); },
                onError: function (xhr) { alert(xhr?.responseJSON?.error || 'Failed to remove document.'); }
            });
        });

        MISDCommon.bindClick('.asset-doc-print', function (button) {
            if (button.data('doc-id')) {
                MISDCommon.printDocument(`/documents/${button.data('doc-id')}/view`);
            }
        });

        $(panelSelector).on('hidden.bs.offcanvas', function () {
            currentAssetTag = '';
            MISDCommon.resetDocumentDetailUI({
                tableSelector: documentConfig.tableSelector,
                bodySelector: documentConfig.bodySelector,
                emptySelector: documentConfig.emptySelector,
                emptyText: documentConfig.emptyText,
                fileInputSelector: documentConfig.fileInputSelector,
                previewInputSelector: documentConfig.fileInputSelector,
                previewListSelector: documentConfig.previewListSelector,
                previewTemplateSelector: documentConfig.previewTemplateSelector
            });
        });
    }

    $(init);
})(window.jQuery);