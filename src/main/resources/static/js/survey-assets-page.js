$(document).ready(function () {
    let currentSurveyAssetReferenceId = null;
    let currentSurveyAssetData = null;

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

    const surveyAssetDocumentConfig = {
        refType: 'SURVEY_ASSET',
        bodySelector: '#surveyAssetDocumentsTableBody',
        emptySelector: '#surveyAssetDocumentsEmpty',
        printButtonClass: 'survey-asset-doc-print',
        deleteButtonClass: 'survey-asset-doc-delete',
        emptyText: 'No documents attached yet.',
        loadErrorText: 'Unable to load documents.',
        fileInputSelector: '#surveyAssetDetailDocumentFiles',
        previewListSelector: '#surveyAssetDetailDocumentPreview',
        previewTemplateSelector: '#surveyAssetDetailDocumentCategoryTemplate'
    };

    function loadSurveyAssetDocuments(refId) {
        MISDCommon.loadDocumentsForReference({
            refType: surveyAssetDocumentConfig.refType,
            refId: refId,
            bodySelector: surveyAssetDocumentConfig.bodySelector,
            emptySelector: surveyAssetDocumentConfig.emptySelector,
            printButtonClass: surveyAssetDocumentConfig.printButtonClass,
            deleteButtonClass: surveyAssetDocumentConfig.deleteButtonClass,
            emptyText: surveyAssetDocumentConfig.emptyText,
            loadErrorText: surveyAssetDocumentConfig.loadErrorText
        });
    }

    function setSurveyAssetEditMode(enabled) {
        $('#surveyAssetDetailOffcanvas').toggleClass('survey-asset-edit-mode-active', enabled);
        $('.survey-asset-detail-view-only').toggleClass('d-none', enabled);
        $('.survey-asset-detail-edit-only').toggleClass('d-none', !enabled);
        $('#enableSurveyAssetEditBtn').toggleClass('d-none', enabled);
        $('#saveSurveyAssetEditBtn, #cancelSurveyAssetEditBtn').toggleClass('d-none', !enabled);
        $('.survey-asset-field').prop('disabled', !enabled);
        applyLockonceVisibility(enabled, currentSurveyAssetData);
    }

    const LOCKONCE_FIELDS = [
        'propertyNumber', 'assetTag', 'serialNumber', 'acquisitionDate', 'cost'
    ];

    function escapeValue(value) {
        return MISDCommon.escapeHtml(value == null || value === '' ? 'N/A' : String(value));
    }

    function renderSurveyCatalogSummary(data) {
        return `<div class="d-grid gap-2">
            <div><span class="text-muted fw-semibold">Category:</span> ${escapeValue(data.catalogCategory)}</div>
            <div><span class="text-muted fw-semibold">Manufacturer:</span> ${escapeValue(data.catalogManufacturer)}</div>
            <div><span class="text-muted fw-semibold">Model Name:</span> ${escapeValue(data.catalogModelName)}</div>
            <div><span class="text-muted fw-semibold">Specifications:</span>${MISDCommon.renderCatalogSpecifications(data.catalogSpecifications)}</div>
        </div>`;
    }

    function applyLockonceVisibility(editMode, data) {
        if (!editMode || !data) {
            $('[data-lockonce-view]').removeClass('d-none');
            $('[data-lockonce-input]').addClass('d-none').prop('disabled', true);
            return;
        }
        LOCKONCE_FIELDS.forEach(function (field) {
            const val = data[field];
            const isBlank = val === null || val === undefined || String(val).trim() === '';
            $('[data-lockonce-view="' + field + '"]').toggleClass('d-none', isBlank);
            const $input = $('[data-lockonce-input="' + field + '"]');
            $input.toggleClass('d-none', !isBlank).prop('disabled', !isBlank);
        });
    }

    function fillSurveyAssetEditFields(data) {
        $('#editSurveyAssetID').val(data.surveyAssetID || '');
        // Lock-once inputs — pre-populated; visibility controlled by applyLockonceVisibility
        $('#editSurveyAssetPropertyNumber').val(data.propertyNumber || '');
        $('#editSurveyAssetTag').val(data.assetTag || '');
        $('#editSurveyAssetSerialNumber').val(data.serialNumber || '');
        $('#editSurveyAssetAcquisitionDate').val(formatDateInput(data.acquisitionDate));
        $('#editSurveyAssetCost').val(data.cost || '');
        // Always-editable fields
        $('#editSurveyAssetLastCalibrationDate').val(formatDateInput(data.lastCalibrationDate));
        $('#editSurveyAssetCalibrationDueDate').val(formatDateInput(data.calibrationDueDate));
        $('#editSurveyAssetAdminLegalStatus').val(data.adminLegalStatus || '');
        $('#editSurveyAssetOperationalStatus').val(data.operationalStatus || '');
        $('#editSurveyAssetConditionStatus').val(data.conditionStatus || '');
        $('#editSurveyAssetRemarks').val(data.remarks || '');
    }

    function buildSurveyAssetUpdatePayload() {
        const surveyAssetID = Number($('#editSurveyAssetID').val());
        return {
            surveyAssetID: Number.isNaN(surveyAssetID) ? null : surveyAssetID,
            // Lock-once fields (backend ignores if already set in DB)
            propertyNumber: $('#editSurveyAssetPropertyNumber').val() || null,
            assetTag: $('#editSurveyAssetTag').val() || null,
            serialNumber: $('#editSurveyAssetSerialNumber').val() || null,
            acquisitionDate: $('#editSurveyAssetAcquisitionDate').val() || null,
            cost: MISDCommon.normalizeDecimalInput($('#editSurveyAssetCost').val()),
            // Always-editable fields
            lastCalibrationDate: $('#editSurveyAssetLastCalibrationDate').val() || null,
            calibrationDueDate: $('#editSurveyAssetCalibrationDueDate').val() || null,
            adminLegalStatus: $('#editSurveyAssetAdminLegalStatus').val() || null,
            operationalStatus: $('#editSurveyAssetOperationalStatus').val() || null,
            conditionStatus: $('#editSurveyAssetConditionStatus').val() || null,
            remarks: $('#editSurveyAssetRemarks').val() || null
        };
    }

    const hasSurveyAssetRegistry = $('#surveyAssetTable').length > 0;

    if (hasSurveyAssetRegistry) {
        MISDCommon.initPageUI({
            themeToggleId: 'themeToggleBtn',
            successToastId: 'successToast',
            initializeSelect2Modals: true
        });
    }

    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
        bootstrap.Tooltip.getOrCreateInstance(element);
    });

    // SurveyAssets.Cost is a DECIMAL column on a server running in strict mode,
    // so grouped input like "350,000.00" has to be flattened before it is sent.
    MISDCommon.bindDecimalInputNormalizer('#editSurveyAssetCost, input[name="cost"]');

    setSurveyAssetEditMode(false);

    if (hasSurveyAssetRegistry) {
        let activeSurveyAssetFilter = $('#surveyAssetPageConfig').data('filter') || '';
        const terminalStatuses = ['Sold', 'Disposed', 'Decommissioned'];
        const operationalIssues = ['Missing', 'Stolen', 'Slated for Disposal'];
        const conditionIssues = ['Needs Calibration', 'Under Repair', 'Beyond Economic Repair (BER)'];
        const adminIssues = ['Under Investigation'];

        function calibrationDueSoon(value) {
            if (!value) return false;
            const dueDate = new Date(value + 'T00:00:00');
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const cutoff = new Date(today);
            cutoff.setDate(cutoff.getDate() + 30);
            return dueDate >= today && dueDate <= cutoff;
        }

        $.fn.dataTable.ext.search.push(function (settings, _data, dataIndex) {
            if (settings.nTable.id !== 'surveyAssetTable' || !activeSurveyAssetFilter) return true;
            const row = settings.aoData[dataIndex].nTr;
            const acquisitionYear = Number(row.dataset.surveyAssetAcquisitionYear);
            const adminStatus = row.dataset.adminStatus || '';
            const operationalStatus = row.dataset.operationalStatus || '';
            const conditionStatus = row.dataset.conditionStatus || '';
            const isTerminal = terminalStatuses.includes(adminStatus);
            const hasAdminIssue = adminIssues.includes(adminStatus);
            const hasOperationalConditionIssue = !isTerminal && (
                operationalIssues.includes(operationalStatus) || conditionIssues.includes(conditionStatus)
            );
            const dueSoon = calibrationDueSoon(row.dataset.calibrationDueDate);
            const isOldAsset = acquisitionYear > 0 && new Date().getFullYear() - acquisitionYear >= 10;

            switch (activeSurveyAssetFilter) {
                case 'current-inventory':
                    return !isTerminal;
                case 'available-idle':
                    return operationalStatus === 'Available/Idle';
                case 'deployed':
                    return operationalStatus === 'Deployed/Field Use';
                case 'under-calibration':
                    return conditionStatus === 'Needs Calibration';
                case 'under-repair':
                    return conditionStatus === 'Under Repair';
                case 'slated-for-disposal':
                    return operationalStatus === 'Slated for Disposal';
                case 'decommissioned':
                    return isTerminal;
                case 'problematic':
                    return !isTerminal && (isOldAsset || dueSoon || hasAdminIssue || hasOperationalConditionIssue);
                case 'calibration-due':
                    return dueSoon;
                default:
                    return true;
            }
        });

        const surveyAssetTable = $('#surveyAssetTable').DataTable(MISDCommon.buildStandardDataTableConfig({
            pageLength: 10,
            lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
            order: [[0, 'asc']]
        }));

        const surveyAssetHistoryTable = $('#surveyAssetHistoryTable').DataTable({
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
                    return `Survey Asset History - ${$('#surveyAssetHistoryTag').text()}`;
                },
                filename: function () {
                    return `Survey_Asset_History_${$('#surveyAssetHistoryId').text()}`;
                }
            }],
            dom: "<'d-none'B>" +
                "<'row mb-3'<'col-sm-12 col-md-6'l><'col-sm-12 col-md-6'f>>" +
                "<'row'<'col-sm-12'tr>>" +
                "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            language: {
                emptyTable: 'No history has been recorded for this survey asset.'
            }
        });

        function loadSurveyAssetHistory(surveyAssetId, tag) {
            $('#surveyAssetHistoryId').text(surveyAssetId);
            $('#surveyAssetHistoryTag').text(tag || 'N/A');
            $('#surveyAssetHistoryError').addClass('d-none').text('');
            surveyAssetHistoryTable.clear().draw();

            $.get(`/survey-assets/${encodeURIComponent(surveyAssetId)}/history`, function (history) {
                surveyAssetHistoryTable.rows.add(history).draw();
                surveyAssetHistoryTable.columns.adjust();
            }).fail(function () {
                $('#surveyAssetHistoryError').removeClass('d-none').text('Unable to load survey asset history.');
            });
        }

        function printSurveyAssetHistory() {
            const surveyAssetId = $('#surveyAssetHistoryId').text();
            const tag = $('#surveyAssetHistoryTag').text();
            const rows = surveyAssetHistoryTable.rows({ search: 'applied' }).data().toArray();
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
                alert('Allow popups to print survey asset history.');
                return;
            }
            printWindow.document.write('<!doctype html><html><head>' +
                `<title>Survey Asset History - ${MISDCommon.escapeHtml(tag)}</title>` +
                '<style>body{font-family:Arial,sans-serif;margin:24px}table{border-collapse:collapse;width:100%}' +
                'th,td{border:1px solid #bbb;padding:8px;text-align:left;vertical-align:top}th{background:#eee}</style>' +
                `</head><body><h1>Survey Asset History</h1><p>Survey Asset ID: ${MISDCommon.escapeHtml(surveyAssetId)} | ` +
                `Asset Tag / Serial: ${MISDCommon.escapeHtml(tag)}</p>` +
                '<table><thead><tr><th>Date and Time</th><th>Log Type</th><th>Action</th>' +
                `<th>Recorded By / Employee</th><th>Notes</th></tr></thead><tbody>${rowMarkup}</tbody></table>` +
                '</body></html>');
            printWindow.document.close();
            printWindow.focus();
            printWindow.print();
        }

        MISDCommon.bindClick('.survey-asset-history-action', function (link) {
            loadSurveyAssetHistory(link.data('survey-asset-id'), link.data('tag'));
        });

        $('#surveyAssetHistoryModal').on('shown.bs.modal', function () {
            surveyAssetHistoryTable.columns.adjust();
        });

        $('#printSurveyAssetHistoryBtn').on('click', printSurveyAssetHistory);

        $('#exportSurveyAssetHistoryBtn').on('click', function () {
            surveyAssetHistoryTable.button(0).trigger();
        });

        MISDCommon.attachDataTableClearButton({
            filterContainerSelector: '#surveyAssetTable_filter',
            buttonId: 'clearSurveyAssetFiltersBtn',
            ariaLabel: 'Clear survey asset table search',
            onClear: function () {
                activeSurveyAssetFilter = '';
                MISDCommon.clearDataTableFilters(surveyAssetTable, { stateKey: 'surveyAssetTableState' });
            }
        });

        MISDCommon.bindClick('.action-btn', function (button) {
            const surveyAssetId = button.data('id');
            const tag = button.data('tag');
            const displayLabel = tag || `Survey Asset ID ${surveyAssetId}`;

            $('#surveyAssetAssignID').val(surveyAssetId);
            $('#surveyAssetAssignDisplay').val(displayLabel);

            $('#surveyAssetReturnID').val(surveyAssetId);
            $('#surveyAssetReturnDisplay').text(displayLabel);

            $('#surveyAssetRetireID').val(surveyAssetId);
            $('#surveyAssetRetireDisplay').text(displayLabel);
        });
    }

    MISDCommon.bindClick('.survey-asset-detail-link', function (link, event) {
        event.preventDefault();
        loadSurveyAssetDetails(link.data('survey-asset-id'));
    });

    function loadSurveyAssetDetails(surveyAssetId) {
        $.get('/survey-assets/' + surveyAssetId, function (data) {
            currentSurveyAssetData = data;
            $('#surveyAssetCatalogSummary').html(renderSurveyCatalogSummary(data));
            $('#surveyAssetDetailPropertyNumber').text(data.propertyNumber || 'N/A');
            $('#surveyAssetDetailAssetTag').text(data.assetTag || 'N/A');
            $('#surveyAssetDetailSerialNumber').text(data.serialNumber || 'N/A');
            $('#surveyAssetDetailAcquisitionDate').text(MISDCommon.formatDate(data.acquisitionDate));
            $('#surveyAssetDetailCustodian').text(data.assignedCustodianName || 'Unassigned');
            $('#surveyAssetDetailCustodianManager').text(data.assignedCustodianManagerName || 'N/A');
            $('#surveyAssetDetailLastCalibrationDate').text(MISDCommon.formatDate(data.lastCalibrationDate));
            $('#surveyAssetDetailCalibrationDueDate').text(MISDCommon.formatDate(data.calibrationDueDate));
            $('#surveyAssetDetailCalibrationOverdueNote').toggleClass('d-none', !data.isCalibrationOverdue);
            $('#surveyAssetDetailCost').text(MISDCommon.formatPesoCurrency(data.cost));

            const valuation = MISDCommon.computeStraightLineValuation(data.cost,
                data.acquisitionDate ? new Date(data.acquisitionDate).getFullYear() : null);
            const $valuationElement = $('#surveyAssetDetailCurrentValuation');
            if (valuation !== null) {
                const formatted = MISDCommon.formatPesoCurrency(valuation);
                const note = data.isFullyDepreciated ? ' (fully depreciated — residual value only)' : '';
                $valuationElement.text(formatted + note);
                if (data.isFullyDepreciated) {
                    $valuationElement.removeClass('text-primary').addClass('text-danger fw-bold');
                } else {
                    $valuationElement.removeClass('text-danger fw-bold').addClass('text-primary');
                }
            } else {
                $valuationElement.text('N/A — original cost and acquisition date required');
                $valuationElement.removeClass('text-danger fw-bold').addClass('text-primary');
            }

            $('#surveyAssetDetailAdminLegalStatus').text(data.adminLegalStatus || 'N/A');
            $('#surveyAssetDetailOperationalStatus').text(data.operationalStatus || 'N/A');
            $('#surveyAssetDetailConditionStatus').text(data.conditionStatus || 'N/A');
            $('#surveyAssetDetailRemarks').text(data.remarks || 'N/A');

            fillSurveyAssetEditFields(data);
            setSurveyAssetEditMode(false);

            currentSurveyAssetReferenceId = data.surveyAssetID != null ? String(data.surveyAssetID) : null;
            loadSurveyAssetDocuments(currentSurveyAssetReferenceId);
            bootstrap.Offcanvas.getOrCreateInstance(document.getElementById('surveyAssetDetailOffcanvas')).show();
        }).fail(function () {
            alert('Unable to load survey asset details.');
        });
    }

    $('#enableSurveyAssetEditBtn').on('click', function () {
        setSurveyAssetEditMode(true);
    });

    $('#cancelSurveyAssetEditBtn').on('click', function () {
        if (!currentSurveyAssetData) {
            setSurveyAssetEditMode(false);
            return;
        }

        fillSurveyAssetEditFields(currentSurveyAssetData);
        setSurveyAssetEditMode(false);
    });

    $('#saveSurveyAssetEditBtn').on('click', function () {
        const payload = buildSurveyAssetUpdatePayload();
        if (!payload.surveyAssetID) {
            alert('No survey asset selected.');
            return;
        }

        $.ajax({
            url: '/survey-assets/update',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function () {
                MISDCommon.showInlineSuccessToast('survey-asset', 'Survey asset updated successfully.');
                setTimeout(function () {
                    location.reload();
                }, 900);
            },
            error: function (xhr) {
                alert(xhr?.responseJSON?.error || 'Failed to save survey asset changes.');
            }
        });
    });

    $('#surveyAssetDetailDocumentFiles').on('change', function () {
        MISDCommon.renderDocumentPreviewBySelectors(
            surveyAssetDocumentConfig.fileInputSelector,
            surveyAssetDocumentConfig.previewListSelector,
            surveyAssetDocumentConfig.previewTemplateSelector
        );
    });

    $('#uploadSurveyAssetDocumentsBtn').on('click', function () {
        if (!currentSurveyAssetReferenceId) {
            alert('No survey asset selected.');
            return;
        }

        const uploadSelection = MISDCommon.getDocumentUploadSelection({
            fileInputSelector: surveyAssetDocumentConfig.fileInputSelector,
            previewSelector: surveyAssetDocumentConfig.previewListSelector
        });

        if (!uploadSelection.isValid) {
            if (uploadSelection.message) {
                alert(uploadSelection.message);
            }
            return;
        }

        const formData = MISDCommon.buildDocumentUploadFormData({
            refType: surveyAssetDocumentConfig.refType,
            refId: currentSurveyAssetReferenceId,
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
                    bodySelector: surveyAssetDocumentConfig.bodySelector,
                    emptySelector: surveyAssetDocumentConfig.emptySelector,
                    emptyText: surveyAssetDocumentConfig.emptyText,
                    fileInputSelector: surveyAssetDocumentConfig.fileInputSelector,
                    previewInputSelector: surveyAssetDocumentConfig.fileInputSelector,
                    previewListSelector: surveyAssetDocumentConfig.previewListSelector,
                    previewTemplateSelector: surveyAssetDocumentConfig.previewTemplateSelector
                });
                loadSurveyAssetDocuments(currentSurveyAssetReferenceId);
            },
            error: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to upload document(s).';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.survey-asset-doc-delete', function (button) {
        const docId = button.data('doc-id');
        MISDCommon.deleteDocumentById(docId, {
            onSuccess: function () {
                loadSurveyAssetDocuments(currentSurveyAssetReferenceId);
            },
            onError: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to remove document.';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.survey-asset-doc-print', function (button) {
        const docId = button.data('doc-id');
        if (docId) {
            MISDCommon.printDocument(`/documents/${docId}/view`);
        }
    });

    $('#surveyAssetDetailOffcanvas').on('hidden.bs.offcanvas', function () {
        currentSurveyAssetReferenceId = null;
        currentSurveyAssetData = null;
        setSurveyAssetEditMode(false);
        MISDCommon.resetDocumentDetailUI({
            bodySelector: surveyAssetDocumentConfig.bodySelector,
            emptySelector: surveyAssetDocumentConfig.emptySelector,
            emptyText: surveyAssetDocumentConfig.emptyText,
            fileInputSelector: surveyAssetDocumentConfig.fileInputSelector,
            previewInputSelector: surveyAssetDocumentConfig.fileInputSelector,
            previewListSelector: surveyAssetDocumentConfig.previewListSelector,
            previewTemplateSelector: surveyAssetDocumentConfig.previewTemplateSelector
        });
    });
});
