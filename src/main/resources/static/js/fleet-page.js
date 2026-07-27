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

    function formatCurrency(value) {
        if (!value || value === 'N/A') {
            return 'N/A';
        }
        try {
            const numValue = typeof value === 'string'
                ? parseFloat(value.replace(/[^0-9.]/g, ''))
                : parseFloat(value);
            if (Number.isNaN(numValue)) {
                return 'N/A';
            }
            return new Intl.NumberFormat('en-PH', {
                style: 'currency',
                currency: 'PHP',
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            }).format(numValue);
        } catch (e) {
            return value || 'N/A';
        }
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

    function computeCurrentValuation(costStr, acquisitionYear) {
        if (!costStr || !acquisitionYear) return null;
        const cost = parseFloat(String(costStr).replace(/[^0-9.]/g, ''));
        const acqYear = parseInt(acquisitionYear, 10);
        if (isNaN(cost) || cost <= 0 || isNaN(acqYear) || acqYear <= 0) return null;
        const currentYear = new Date().getFullYear();
        const yearsUsed = Math.max(0, currentYear - acqYear);
        // COA Circular 2003-007 / GAM: Transportation Equipment
        // Useful life: 10 yrs, Residual value: 10%, Method: Straight-line
        // Annual depreciation rate = (1 - 0.10) / 10 = 9%
        const depFactor = Math.min(0.09 * yearsUsed, 0.90);
        return cost * (1 - depFactor);
    }

    function showInlineSuccessToast(message) {
        let container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            container.style.zIndex = '1055';
            document.body.appendChild(container);
        }

        let toastEl = document.getElementById('fleetInlineSuccessToast');
        if (!toastEl) {
            toastEl = document.createElement('div');
            toastEl.id = 'fleetInlineSuccessToast';
            toastEl.className = 'toast align-items-center text-bg-success border-0';
            toastEl.setAttribute('role', 'alert');
            toastEl.setAttribute('aria-live', 'assertive');
            toastEl.setAttribute('aria-atomic', 'true');
            toastEl.innerHTML = `
                <div class="d-flex">
                    <div class="toast-body fw-bold"></div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            `;
            container.appendChild(toastEl);
        }

        const body = toastEl.querySelector('.toast-body');
        if (body) {
            body.textContent = message;
        }

        MISDCommon.showToast('fleetInlineSuccessToast', 1800);
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
            cost: $('#editFleetCost').val() || null,
            // Always-editable fields
            registrationExpiry: $('#editFleetRegistrationExpiry').val() || null,
            insuranceExpiry: $('#editFleetInsuranceExpiry').val() || null,
            adminLegaltionalStatus: $('#editFleetAdminLegalStatus').val() || null,
            operationalStatus: $('#editFleetOperationalStatus').val() || null,
            maintenanceStatus: $('#editFleetMaintenanceStatus').val() || null,
            remarks: $('#editFleetRemarks').val() || null
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

    setFleetEditMode(false);

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
        const displayLabel = plate || `Vehicle ID ${vId}`;

        $('#assignVehicleID').val(vId);
        $('#assignPlateDisplay').val(displayLabel);

        $('#returnVehicleID').val(vId);
        $('#returnPlateDisplay').text(displayLabel);

        $('#retireVehicleID').val(vId);
        $('#retirePlateDisplay').text(displayLabel);
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
            $('#fleetDetailAcquisitionYear').text(data.acquisitionYear || 'N/A');
            $('#fleetDetailBodyNumber').text(data.bodyNumber || 'N/A');
            $('#fleetDetailFuelType').text(data.fuelType || 'N/A');
            $('#fleetDetailEngineNumber').text(data.engineNumber || 'N/A');
            $('#fleetDetailChassisVin').text(data.chassisNumberVIN || 'N/A');
            $('#fleetDetailDriver').text(data.assignedDriverName || 'Unassigned');
            $('#fleetDetailDriverManager').text(data.assignedDriverManagerName || 'N/A');
            $('#fleetDetailRegExpiry').text(formatDate(data.registrationExpiry));
            $('#fleetDetailInsuranceExpiry').text(formatDate(data.insuranceExpiry));
            $('#fleetDetailCost').text(formatCurrency(data.cost));

            const valuation = computeCurrentValuation(data.cost, data.acquisitionYear);
            const $valuationElement = $('#fleetDetailCurrentValuation');
            if (valuation !== null) {
                const formatted = new Intl.NumberFormat('en-PH', {
                    style: 'currency', currency: 'PHP', minimumFractionDigits: 2
                }).format(valuation);
                const yearsUsed = Math.max(0, new Date().getFullYear() - parseInt(data.acquisitionYear, 10));
                const note = yearsUsed >= 10 ? ' (fully depreciated — residual value only)' : '';
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
                showInlineSuccessToast('Fleet vehicle updated successfully.');
                setTimeout(function () {
                    location.reload();
                }, 900);
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
