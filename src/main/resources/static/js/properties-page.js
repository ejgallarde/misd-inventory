$(document).ready(function () {
    let currentPropertyReferenceId = null;
    let currentPropertyData = null;

    function formatDecimal(value) {
        if (value === null || value === undefined || value === '') {
            return 'N/A';
        }
        const numberValue = Number(value);
        if (Number.isNaN(numberValue)) {
            return value;
        }
        return numberValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

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

    function setPropertyEditMode(enabled) {
        $('#propertyDetailOffcanvas').toggleClass('property-edit-mode-active', enabled);
        $('.property-detail-view-only').toggleClass('d-none', enabled);
        $('.property-detail-edit-only').toggleClass('d-none', !enabled);
        $('#enablePropertyEditBtn').toggleClass('d-none', enabled);
        $('#savePropertyEditBtn, #cancelPropertyEditBtn').toggleClass('d-none', !enabled);
        $('.property-field').prop('disabled', !enabled);
    }

    function resetDetailPanelScroll() {
        const offcanvasBody = document.querySelector('#propertyDetailOffcanvas .offcanvas-body');
        if (offcanvasBody) {
            offcanvasBody.scrollTop = 0;
        }
    }

    function fillPropertyEditableFields(data) {
        $('#editPropertyID').val(data.propertyID || '');
        $('#editPropertyAssessedValue').val(data.assessedValue || '');
        $('#editPropertyTaxStatus').val(data.propertyTaxStatus || '');
        $('#editPropertyLegalTitlingStatus').val(data.legalTitlingStatus || '');
        $('#editPropertyOperationalStatus').val(data.operationalStatus || '');
        $('#editPropertyConditionStatus').val(data.conditionStatus || '');
        $('#editPropertyZipCode').val(data.zipCode || '');
        $('#editPropertyLotArea').val(data.lotAreaSqm || '');
        $('#editPropertyFloorArea').val(data.floorAreaSqm || '');
        $('#editPropertyDetails').val(data.propertyDetails || '');
    }

    function buildPropertyUpdatePayload() {
        return {
            propertyID: Number($('#editPropertyID').val()),
            assessedValue: $('#editPropertyAssessedValue').val() || null,
            propertyTaxStatus: $('#editPropertyTaxStatus').val(),
            legalTitlingStatus: $('#editPropertyLegalTitlingStatus').val(),
            operationalStatus: $('#editPropertyOperationalStatus').val(),
            conditionStatus: $('#editPropertyConditionStatus').val(),
            zipCode: $('#editPropertyZipCode').val(),
            lotAreaSqm: $('#editPropertyLotArea').val() || null,
            floorAreaSqm: $('#editPropertyFloorArea').val() || null,
            propertyDetails: $('#editPropertyDetails').val()
        };
    }

    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn',
        successToastId: 'successToast',
        initializeSelect2Modals: true
    });

    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
        bootstrap.Tooltip.getOrCreateInstance(element);
    });

    const propertiesTable = $('#propertiesTable').DataTable(MISDCommon.buildStandardDataTableConfig({
        pageLength: 10,
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
            currentPropertyData = data;
            $('#propertyDetailName').text(data.propertyName || 'N/A');
            $('#propertyDetailType').text(data.propertyType || 'N/A');
            $('#propertyDetailTitle').text(data.titleNumber || 'N/A');
            $('#propertyDetailTaxDeclaration').text(data.taxDeclarationNumber || 'N/A');
            $('#propertyDetailAddressLine1').text(data.addressLine1 || 'N/A');
            $('#propertyDetailAddressLine2').text(data.addressLine2 || 'N/A');
            $('#propertyDetailProvince').text(data.province || 'N/A');
            $('#propertyDetailCity').text(data.city || 'N/A');
            $('#propertyDetailBarangay').text(data.barangay || 'N/A');
            $('#propertyDetailZipCode').text(data.zipCode || 'N/A');
            $('#propertyDetailAcquisitionDate').text(formatDate(data.acquisitionDate));
            $('#propertyDetailLotArea').text(formatDecimal(data.lotAreaSqm));
            $('#propertyDetailFloorArea').text(formatDecimal(data.floorAreaSqm));
            $('#propertyDetailPropertyDetails').text(data.propertyDetails || 'N/A');
            $('#propertyDetailAssessedValue').text(formatDecimal(data.assessedValue));
            $('#propertyDetailTaxStatus').text(data.propertyTaxStatus || 'N/A');
            $('#propertyDetailLegalTitlingStatus').text(data.legalTitlingStatus || 'N/A');
            $('#propertyDetailOperationalStatus').text(data.operationalStatus || 'N/A');
            $('#propertyDetailConditionStatus').text(data.conditionStatus || 'N/A');
            $('#propertyDetailCustodian').text(data.custodianName || 'Unassigned');
            $('#propertyDetailRemarks').text(data.remarks || 'N/A');

            fillPropertyEditableFields(data);
            setPropertyEditMode(false);

            currentPropertyReferenceId = data.propertyID != null ? String(data.propertyID) : null;
            loadPropertyDocuments(currentPropertyReferenceId);
            resetDetailPanelScroll();

            bootstrap.Offcanvas.getOrCreateInstance(document.getElementById('propertyDetailOffcanvas')).show();
        }).fail(function () {
            alert('Unable to load property details.');
        });
    });

    $('#propertyDetailOffcanvas').on('shown.bs.offcanvas', function () {
        resetDetailPanelScroll();
    });

    $('#enablePropertyEditBtn').on('click', function () {
        setPropertyEditMode(true);
    });

    $('#cancelPropertyEditBtn').on('click', function () {
        if (!currentPropertyData) {
            setPropertyEditMode(false);
            return;
        }
        fillPropertyEditableFields(currentPropertyData);
        setPropertyEditMode(false);
    });

    $('#savePropertyEditBtn').on('click', function () {
        const payload = buildPropertyUpdatePayload();
        if (!payload.propertyID) {
            alert('No property selected.');
            return;
        }

        $.ajax({
            url: '/properties/update',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function () {
                location.reload();
            },
            error: function () {
                alert('Failed to save property changes.');
            }
        });
    });

    $('#propertyDetailDocumentFiles').on('change', function () {
        MISDCommon.prepareMultiFileSelection(this);
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
        MISDCommon.deleteDocumentById(docId, {
            useModalConfirm: true,
            confirmTitle: 'Delete Attachment',
            confirmMessage: 'Remove this attachment from the selected property?',
            confirmButtonText: 'Delete',
            onSuccess: function () {
                loadPropertyDocuments(currentPropertyReferenceId);
            },
            onError: function (xhr) {
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

    $('#propertyDetailOffcanvas').on('hidden.bs.offcanvas', function () {
        currentPropertyReferenceId = null;
        currentPropertyData = null;
        setPropertyEditMode(false);
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
