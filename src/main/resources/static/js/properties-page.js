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

    function statusBadgeHtml(value, variant = 'secondary') {
        const normalized = value == null ? '' : String(value).trim();
        if (!normalized) {
            return '<span class="badge bg-secondary">N/A</span>';
        }

        if (variant === 'operational') {
            const css = normalized === 'Active/In Use' ? 'badge bg-primary' : 'badge bg-secondary';
            return `<span class="${css}">${normalized}</span>`;
        }

        if (variant === 'neutral') {
            return `<span class="badge bg-light text-dark border">${normalized}</span>`;
        }

        return `<span class="badge bg-secondary">${normalized}</span>`;
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

    const hasLandRegistry = $('#landPropertiesTable').length > 0;
    const hasBuildingRegistry = $('#buildingFacilitiesTable').length > 0;
    const hasPropertiesRegistry = hasLandRegistry || hasBuildingRegistry;

    if (hasPropertiesRegistry) {
        MISDCommon.initPageUI({
            themeToggleId: 'themeToggleBtn',
            successToastId: 'successToast',
            initializeSelect2Modals: true
        });
    }

    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
        bootstrap.Tooltip.getOrCreateInstance(element);
    });

    setPropertyEditMode(false);

    function initPropertiesDataTable(selector, clearButtonId) {
        if (!$(selector).length) {
            return null;
        }

        const table = $(selector).DataTable(MISDCommon.buildStandardDataTableConfig({
            pageLength: 10,
            lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
            order: [[1, 'asc']]
        }));

        MISDCommon.attachDataTableClearButton({
            filterContainerSelector: `${selector}_filter`,
            buttonId: clearButtonId,
            ariaLabel: 'Clear properties table search',
            onClear: function () {
                MISDCommon.clearDataTableFilters(table, { stateKey: `${selector}State` });
            }
        });

        return table;
    }

    if (hasPropertiesRegistry) {
        initPropertiesDataTable('#landPropertiesTable', 'clearLandPropertyFiltersBtn');
        initPropertiesDataTable('#buildingFacilitiesTable', 'clearBuildingPropertyFiltersBtn');
    }

    if ($('#propertyHistoryTable').length > 0) {
        const propertyHistoryTable = $('#propertyHistoryTable').DataTable({
            data: [],
            columns: [
                {
                    data: 'transactionDate',
                    render: function (value, type) {
                        if (type === 'sort' || type === 'type') return value || '';
                        return value ? new Intl.DateTimeFormat('en-PH', {
                            dateStyle: 'medium',
                            timeStyle: 'medium'
                        }).format(new Date(value)) : 'N/A';
                    }
                },
                { data: 'logType', render: $.fn.dataTable.render.text() },
                { data: 'actionType', render: $.fn.dataTable.render.text() },
                { data: 'recordedBy', render: $.fn.dataTable.render.text() },
                { data: 'notes', defaultContent: '', render: $.fn.dataTable.render.text() }
            ],
            order: [[0, 'desc']],
            pageLength: 10,
            lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
            buttons: [{
                extend: 'excel',
                className: 'd-none',
                title: function () {
                    return `Property History - ${$('#propertyHistoryPropertyName').text()}`;
                },
                filename: function () {
                    return `Property_History_${$('#propertyHistoryPropertyId').text()}`;
                }
            }],
            dom: "<'d-none'B>" +
                "<'row mb-3'<'col-sm-12 col-md-6'l><'col-sm-12 col-md-6'f>>" +
                "<'row'<'col-sm-12'tr>>" +
                "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            language: {
                emptyTable: 'No history has been recorded for this property.'
            }
        });

        function loadPropertyHistory(propertyId, propertyName) {
            $('#propertyHistoryPropertyId').text(propertyId);
            $('#propertyHistoryPropertyName').text(propertyName || 'N/A');
            $('#propertyHistoryError').addClass('d-none').text('');
            propertyHistoryTable.clear().draw();

            $.get(`/properties/${encodeURIComponent(propertyId)}/history`, function (history) {
                propertyHistoryTable.rows.add(history).draw();
                propertyHistoryTable.columns.adjust();
            }).fail(function () {
                $('#propertyHistoryError').removeClass('d-none').text('Unable to load property history.');
            });
        }

        function printPropertyHistory() {
            const propertyId = $('#propertyHistoryPropertyId').text();
            const propertyName = $('#propertyHistoryPropertyName').text();
            const rows = propertyHistoryTable.rows({ search: 'applied' }).data().toArray();
            const rowMarkup = rows.map(function (entry) {
                const transactionDate = entry.transactionDate
                    ? new Intl.DateTimeFormat('en-PH', { dateStyle: 'medium', timeStyle: 'medium' })
                        .format(new Date(entry.transactionDate))
                    : 'N/A';
                return `<tr><td>${MISDCommon.escapeHtml(transactionDate)}</td>` +
                    `<td>${MISDCommon.escapeHtml(entry.logType || '')}</td>` +
                    `<td>${MISDCommon.escapeHtml(entry.actionType || '')}</td>` +
                    `<td>${MISDCommon.escapeHtml(entry.recordedBy || '')}</td>` +
                    `<td>${MISDCommon.escapeHtml(entry.notes || '')}</td></tr>`;
            }).join('');
            const printWindow = window.open('', '_blank', 'width=1100,height=700');
            if (!printWindow) {
                alert('Allow popups to print property history.');
                return;
            }
            printWindow.document.write('<!doctype html><html><head>' +
                `<title>Property History - ${MISDCommon.escapeHtml(propertyName)}</title>` +
                '<style>body{font-family:Arial,sans-serif;margin:24px}table{border-collapse:collapse;width:100%}' +
                'th,td{border:1px solid #bbb;padding:8px;text-align:left;vertical-align:top}th{background:#eee}</style>' +
                `</head><body><h1>Property History</h1><p>Property ID: ${MISDCommon.escapeHtml(propertyId)} | ` +
                `Property Name: ${MISDCommon.escapeHtml(propertyName)}</p>` +
                '<table><thead><tr><th>Date and Time</th><th>Log Type</th><th>Action</th>' +
                `<th>Recorded By / Employee</th><th>Notes</th></tr></thead><tbody>${rowMarkup}</tbody></table>` +
                '</body></html>');
            printWindow.document.close();
            printWindow.focus();
            printWindow.print();
        }

        MISDCommon.bindClick('.property-history-action', function (link) {
            loadPropertyHistory(link.data('property-id'), link.data('name'));
        });

        $('#propertyHistoryModal').on('shown.bs.modal', function () {
            propertyHistoryTable.columns.adjust();
        });

        $('#printPropertyHistoryBtn').on('click', printPropertyHistory);

        $('#exportPropertyHistoryBtn').on('click', function () {
            propertyHistoryTable.button(0).trigger();
        });
    }

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
            $('#propertyDetailAcquisitionDate').text(MISDCommon.formatDate(data.acquisitionDate));
            $('#propertyDetailLotArea').text(formatDecimal(data.lotAreaSqm));
            $('#propertyDetailFloorArea').text(formatDecimal(data.floorAreaSqm));
            $('#propertyDetailPropertyDetails').text(data.propertyDetails || 'N/A');
            $('#propertyDetailAssessedValue').text(MISDCommon.formatPesoCurrency(data.assessedValue));
            $('#propertyDetailTaxStatus').text(data.propertyTaxStatus || 'N/A');
            $('#propertyDetailLegalTitlingStatus').html(statusBadgeHtml(data.legalTitlingStatus, 'neutral'));
            $('#propertyDetailOperationalStatus').html(statusBadgeHtml(data.operationalStatus, 'operational'));
            $('#propertyDetailConditionStatus').html(statusBadgeHtml(data.conditionStatus, 'neutral'));
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
                MISDCommon.showInlineSuccessToast('property', 'Property updated successfully.');
                setTimeout(function () {
                    location.reload();
                }, 900);
            },
            error: function () {
                alert('Failed to save property changes.');
            }
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
