$(document).ready(function () {
    let currentFleetReferenceId = null;
    let currentFleetData = null;

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
        applyLockonceVisibility(enabled, currentFleetData);
    }

    const LOCKONCE_FIELDS = [
        'plateNumber', 'make', 'model', 'manufactureYear', 'bodyNumber',
        'fuelType', 'engineNumber', 'chassisNumberVIN', 'cost', 'acquisitionYear'
    ];

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

    function fillFleetEditFields(data) {
        $('#editFleetVehicleID').val(data.vehicleID || '');
        // Lock-once inputs — pre-populated; visibility controlled by applyLockonceVisibility
        $('#editFleetPlateNumber').val(data.plateNumber || '');
        $('#editFleetMake').val(data.make || '');
        $('#editFleetModel').val(data.model || '');
        $('#editFleetManufactureYear').val(data.manufactureYear || '');
        $('#editFleetAcquisitionYear').val(data.acquisitionYear || '');
        $('#editFleetBodyNumber').val(data.bodyNumber || '');
        $('#editFleetFuelType').val(data.fuelType || '');
        $('#editFleetEngineNumber').val(data.engineNumber || '');
        $('#editFleetChassisVin').val(data.chassisNumberVIN || '');
        $('#editFleetCost').val(data.cost || '');
        // Always-editable fields
        $('#editFleetRegistrationExpiry').val(formatDateInput(data.registrationExpiry));
        $('#editFleetInsuranceExpiry').val(formatDateInput(data.insuranceExpiry));
        $('#editFleetAdminLegalStatus').val(data.adminLegaltionalStatus || '');
        $('#editFleetOperationalStatus').val(data.operationalStatus || '');
        $('#editFleetMaintenanceStatus').val(data.maintenanceStatus || '');
        $('#editFleetRemarks').val(data.remarks || '');
    }

    function buildFleetUpdatePayload() {
        const vehicleID = Number($('#editFleetVehicleID').val());
        const mfgYear = $('#editFleetManufactureYear').val();
        const acqYear = $('#editFleetAcquisitionYear').val();
        return {
            vehicleID: Number.isNaN(vehicleID) ? null : vehicleID,
            // Lock-once fields (backend ignores if already set in DB)
            plateNumber: $('#editFleetPlateNumber').val() || null,
            make: $('#editFleetMake').val() || null,
            model: $('#editFleetModel').val() || null,
            manufactureYear: mfgYear ? Number(mfgYear) : null,
            acquisitionYear: acqYear ? Number(acqYear) : null,
            bodyNumber: $('#editFleetBodyNumber').val() || null,
            fuelType: $('#editFleetFuelType').val() || null,
            engineNumber: $('#editFleetEngineNumber').val() || null,
            chassisNumberVIN: $('#editFleetChassisVin').val() || null,
            cost: MISDCommon.normalizeDecimalInput($('#editFleetCost').val()),
            // Always-editable fields
            registrationExpiry: $('#editFleetRegistrationExpiry').val() || null,
            insuranceExpiry: $('#editFleetInsuranceExpiry').val() || null,
            adminLegaltionalStatus: $('#editFleetAdminLegalStatus').val() || null,
            operationalStatus: $('#editFleetOperationalStatus').val() || null,
            maintenanceStatus: $('#editFleetMaintenanceStatus').val() || null,
            remarks: $('#editFleetRemarks').val() || null
        };
    }

    const hasFleetRegistry = $('#fleetTable').length > 0;

    if (hasFleetRegistry) {
        MISDCommon.initPageUI({
            themeToggleId: 'themeToggleBtn',
            successToastId: 'successToast',
            initializeSelect2Modals: true
        });
    }

    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
        bootstrap.Tooltip.getOrCreateInstance(element);
    });

    // FleetVehicles.Cost is a DECIMAL column on a server running in strict mode,
    // so grouped input like "1,500,000.00" has to be flattened before it is sent.
    // Covers the detail panel and the Register Vehicle form on the dashboard.
    MISDCommon.bindDecimalInputNormalizer('#editFleetCost, input[name="cost"]');

    setFleetEditMode(false);

    if (hasFleetRegistry) {
        let activeFleetFilter = $('#fleetPageConfig').data('filter') || '';
        const terminalStatuses = ['Sold', 'Disposed', 'Decommissioned'];
        const operationalIssues = ['Grounded', 'Missing', 'Missing/Stolen', 'Stolen', 'Slated for Disposal'];
        const maintenanceIssues = ['Under Repair', 'Beyond Economic Repair (BER)'];
        const adminIssues = ['Registration Expired', 'Impounded', 'Under Investigation'];

        function registrationExpiresSoon(value) {
            if (!value) return false;
            const expiry = new Date(value + 'T00:00:00');
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const cutoff = new Date(today);
            cutoff.setDate(cutoff.getDate() + 30);
            return expiry >= today && expiry <= cutoff;
        }

        $.fn.dataTable.ext.search.push(function (settings, _data, dataIndex) {
            if (settings.nTable.id !== 'fleetTable' || !activeFleetFilter) return true;
            const row = settings.aoData[dataIndex].nTr;
            const manufactureYear = Number(row.dataset.manufactureYear);
            const adminStatus = row.dataset.adminStatus || '';
            const operationalStatus = row.dataset.operationalStatus || '';
            const maintenanceStatus = row.dataset.maintenanceStatus || '';
            const isTerminal = terminalStatuses.includes(adminStatus);
            const hasAdminIssue = adminIssues.includes(adminStatus);
            const hasOperationalMaintenanceIssue = !isTerminal && (
                operationalIssues.includes(operationalStatus) || maintenanceIssues.includes(maintenanceStatus)
            );
            const expiresSoon = registrationExpiresSoon(row.dataset.registrationExpiry);
            const isOldVehicle = manufactureYear > 0 && new Date().getFullYear() - manufactureYear >= 10;

            switch (activeFleetFilter) {
                case 'current-inventory':
                    return !isTerminal;
                case 'available-idle':
                    return operationalStatus === 'Available/Idle';
                case 'dispatched':
                    return ['Dispatched', 'Dispatched/In Transit'].includes(operationalStatus);
                case 'under-maintenance':
                    return maintenanceStatus === 'Under Repair';
                case 'slated-for-disposal':
                    return operationalStatus === 'Slated for Disposal';
                case 'decommissioned':
                    return isTerminal;
                case 'problematic':
                    return !isTerminal && (isOldVehicle || expiresSoon || hasAdminIssue || hasOperationalMaintenanceIssue);
                case 'admin-legal-issues':
                    return hasAdminIssue;
                case 'maintenance-issues':
                    return hasOperationalMaintenanceIssue;
                case 'expiring-registrations':
                    return expiresSoon;
                case 'sold':
                    return adminStatus === 'Sold';
                default:
                    return true;
            }
        });

        const fleetTable = $('#fleetTable').DataTable(MISDCommon.buildStandardDataTableConfig({
            pageLength: 10,
            lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
            order: [[0, 'asc']]
        }));

        const fleetHistoryTable = $('#fleetHistoryTable').DataTable({
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
                    return `Vehicle History - ${$('#fleetHistoryPlateNumber').text()}`;
                },
                filename: function () {
                    return `Vehicle_History_${$('#fleetHistoryVehicleId').text()}`;
                }
            }],
            dom: "<'d-none'B>" +
                "<'row mb-3'<'col-sm-12 col-md-6'l><'col-sm-12 col-md-6'f>>" +
                "<'row'<'col-sm-12'tr>>" +
                "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            language: {
                emptyTable: 'No history has been recorded for this vehicle.'
            }
        });

        function loadFleetHistory(vehicleId, plateNumber) {
            $('#fleetHistoryVehicleId').text(vehicleId);
            $('#fleetHistoryPlateNumber').text(plateNumber || 'N/A');
            $('#fleetHistoryError').addClass('d-none').text('');
            fleetHistoryTable.clear().draw();

            $.get(`/fleet/${encodeURIComponent(vehicleId)}/history`, function (history) {
                fleetHistoryTable.rows.add(history).draw();
                fleetHistoryTable.columns.adjust();
            }).fail(function () {
                $('#fleetHistoryError').removeClass('d-none').text('Unable to load vehicle history.');
            });
        }

        function printFleetHistory() {
            const vehicleId = $('#fleetHistoryVehicleId').text();
            const plateNumber = $('#fleetHistoryPlateNumber').text();
            const rows = fleetHistoryTable.rows({ search: 'applied' }).data().toArray();
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
                alert('Allow popups to print vehicle history.');
                return;
            }
            printWindow.document.write('<!doctype html><html><head>' +
                `<title>Vehicle History - ${MISDCommon.escapeHtml(plateNumber)}</title>` +
                '<style>body{font-family:Arial,sans-serif;margin:24px}table{border-collapse:collapse;width:100%}' +
                'th,td{border:1px solid #bbb;padding:8px;text-align:left;vertical-align:top}th{background:#eee}</style>' +
                `</head><body><h1>Vehicle History</h1><p>Vehicle ID: ${MISDCommon.escapeHtml(vehicleId)} | ` +
                `Plate No.: ${MISDCommon.escapeHtml(plateNumber)}</p>` +
                '<table><thead><tr><th>Date and Time</th><th>Log Type</th><th>Action</th>' +
                `<th>Recorded By / Employee</th><th>Notes</th></tr></thead><tbody>${rowMarkup}</tbody></table>` +
                '</body></html>');
            printWindow.document.close();
            printWindow.focus();
            printWindow.print();
        }

        MISDCommon.bindClick('.fleet-history-action', function (link) {
            loadFleetHistory(link.data('vehicle-id'), link.data('plate'));
        });

        $('#fleetHistoryModal').on('shown.bs.modal', function () {
            fleetHistoryTable.columns.adjust();
        });

        $('#printFleetHistoryBtn').on('click', printFleetHistory);

        $('#exportFleetHistoryBtn').on('click', function () {
            fleetHistoryTable.button(0).trigger();
        });

        MISDCommon.attachDataTableClearButton({
            filterContainerSelector: '#fleetTable_filter',
            buttonId: 'clearFleetFiltersBtn',
            ariaLabel: 'Clear fleet table search',
            onClear: function () {
                activeFleetFilter = '';
                MISDCommon.clearDataTableFilters(fleetTable, { stateKey: 'fleetTableState' });
            }
        });

        MISDCommon.bindClick('.action-btn', function (button) {
            const vehicleId = button.data('id');
            const plate = button.data('plate');
            const displayLabel = plate || `Vehicle ID ${vehicleId}`;

            $('#assignVehicleID').val(vehicleId);
            $('#assignPlateDisplay').val(displayLabel);

            $('#returnVehicleID').val(vehicleId);
            $('#returnPlateDisplay').text(displayLabel);

            $('#retireVehicleID').val(vehicleId);
            $('#retirePlateDisplay').text(displayLabel);
        });
    }

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
            $('#fleetDetailAcquisitionYear').text(data.acquisitionYear || 'N/A');
            $('#fleetDetailBodyNumber').text(data.bodyNumber || 'N/A');
            $('#fleetDetailFuelType').text(data.fuelType || 'N/A');
            $('#fleetDetailEngineNumber').text(data.engineNumber || 'N/A');
            $('#fleetDetailChassisVin').text(data.chassisNumberVIN || 'N/A');
            $('#fleetDetailDriver').text(data.assignedDriverName || 'Unassigned');
            $('#fleetDetailDriverManager').text(data.assignedDriverManagerName || 'N/A');
            $('#fleetDetailRegExpiry').text(MISDCommon.formatDate(data.registrationExpiry));
            // The detail endpoint no longer rewrites the legal status when a
            // registration has lapsed; it reports the fact and the panel shows it.
            $('#fleetDetailRegExpiredNote').toggleClass('d-none', !data.isRegistrationExpired);
            $('#fleetDetailInsuranceExpiry').text(MISDCommon.formatDate(data.insuranceExpiry));
            $('#fleetDetailCost').text(MISDCommon.formatPesoCurrency(data.cost));

            const valuation = MISDCommon.computeStraightLineValuation(data.cost, data.acquisitionYear);
            const $valuationElement = $('#fleetDetailCurrentValuation');
            if (valuation !== null) {
                const formatted = MISDCommon.formatPesoCurrency(valuation);
                const note = data.isFullyDepreciated ? ' (fully depreciated — residual value only)' : '';
                $valuationElement.text(formatted + note);
                // Apply red styling if fully depreciated
                if (data.isFullyDepreciated) {
                    $valuationElement.removeClass('text-primary').addClass('text-danger fw-bold');
                } else {
                    $valuationElement.removeClass('text-danger fw-bold').addClass('text-primary');
                }
            } else {
                $valuationElement.text('N/A — original cost and acquisition year required');
                $valuationElement.removeClass('text-danger fw-bold').addClass('text-primary');
            }

            $('#fleetDetailAdminLegalStatus').text(data.adminLegaltionalStatus || 'N/A');
            $('#fleetDetailOperationalStatus').text(data.operationalStatus || data.currentStatus || 'N/A');
            $('#fleetDetailMaintenanceStatus').text(data.maintenanceStatus || 'N/A');
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
                MISDCommon.showInlineSuccessToast('fleet', 'Fleet vehicle updated successfully.');
                setTimeout(function () {
                    location.reload();
                }, 900);
            },
            error: function (xhr) {
                alert(xhr?.responseJSON?.error || 'Failed to save vehicle changes.');
            }
        });
    });

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
