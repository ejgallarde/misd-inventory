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


    // DataTables
    const assetsTable = $('#assetsTable').DataTable(MISDCommon.buildStandardDataTableConfig({
        pageLength: 10,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
        order: [[0, 'asc']]
    }));

    const assetHistoryTable = $('#assetHistoryTable').DataTable({
        data: [],
        columns: [
            {
                data: 'transactionDate',
                render: function (value, type) {
                    if (type === 'sort' || type === 'type') {
                        return value || '';
                    }
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
                return `Asset History - ${$('#assetHistoryAssetTag').text()}`;
            },
            filename: function () {
                return `Asset_History_${$('#assetHistoryAssetTag').text()}`;
            }
        }],
        dom: "<'d-none'B>" +
            "<'row mb-3'<'col-sm-12 col-md-6'l><'col-sm-12 col-md-6'f>>" +
            "<'row'<'col-sm-12'tr>>" +
            "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
        language: {
            emptyTable: 'No history has been recorded for this asset.'
        }
    });

    function loadAssetHistory(assetTag) {
        $('#assetHistoryAssetTag').text(assetTag);
        $('#assetHistoryError').addClass('d-none').text('');
        assetHistoryTable.clear().draw();

        $.get(`/api/assets/${encodeURIComponent(assetTag)}/history`, function (history) {
            assetHistoryTable.rows.add(history).draw();
            assetHistoryTable.columns.adjust();
        }).fail(function () {
            $('#assetHistoryError').removeClass('d-none').text('Unable to load asset history.');
        });
    }

    function printAssetHistory() {
        const assetTag = $('#assetHistoryAssetTag').text();
        const rows = assetHistoryTable.rows({ search: 'applied' }).data().toArray();
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
            alert('Allow popups to print asset history.');
            return;
        }
        printWindow.document.write(`<!doctype html><html><head><title>Asset History - ${MISDCommon.escapeHtml(assetTag)}</title>` +
            '<style>body{font-family:Arial,sans-serif;margin:24px}table{border-collapse:collapse;width:100%}' +
            'th,td{border:1px solid #bbb;padding:8px;text-align:left;vertical-align:top}th{background:#eee}</style>' +
            `</head><body><h1>Asset History</h1><p>Asset Tag: ${MISDCommon.escapeHtml(assetTag)}</p>` +
            '<table><thead><tr><th>Date and Time</th><th>Log Type</th><th>Action</th>' +
            `<th>Recorded By / Employee</th><th>Notes</th></tr></thead><tbody>${rowMarkup}</tbody></table>` +
            '</body></html>');
        printWindow.document.close();
        printWindow.focus();
        printWindow.print();
    }

    assetsTable.on('search.dt draw.dt order.dt page.dt', function () {
        MISDCommon.syncDataTableStateToSessionAndUrl(assetsTable, {
            stateKey: tableStateKey,
            includeOrder: true
        });
    });

    // Pre-set filter from URL param (e.g. /assets?filter=deployed).
    // When a preset filter is active, session state is NOT restored to avoid overriding it.
    function applyPresetFilter(filter) {
        const STATUS_COL = 7;
        const FILTERS = {
            'deployed': ['Deployment: Deployed', false],
            'current-inventory': ['^(?!.*Lifecycle: Decommissioned / Retired).+$', true],
            'deployment-issues': ['Deployment: Missing / Unaccounted', false],
            'under-maintenance': [
                'Deployment: (With Service Center|With MISD Technician).*Health: Under Repair',
                true
            ],
            'maintenance-issues': [
                'Deployment: (With Service Center|With MISD Technician).*Health: Under Repair',
                true
            ],
            'decommissioned-retired': ['Lifecycle: Decommissioned / Retired', false],
            'problematic': [
                '^(?!.*Lifecycle: Decommissioned / Retired).*Health: Beyond Economic Repair \(BER\).*$',
                true
            ]
        };
        const config = FILTERS[filter];
        if (config) {
            assetsTable.column(STATUS_COL).search(config[0], config[1], false).draw();
        }
    }

    const pageConfig = document.getElementById('assetPageConfig');
    const urlFilter = pageConfig ? (pageConfig.dataset.filter || '') : '';
    const urlOpenAsset = pageConfig ? (pageConfig.dataset.openAsset || '') : '';

    if (urlFilter) {
        applyPresetFilter(urlFilter);
    } else {
        MISDCommon.restoreDataTableStateFromSessionAndUrl(assetsTable, {
            stateKey: tableStateKey,
            restoreOrder: true,
            warningMessage: 'Unable to restore saved asset table state.'
        });
    }

    // The detail slideout lives in asset-detail.js, shared with the dashboard tab.
    // This page only tells it which asset to open and asks it to preserve the
    // table view across the reload that follows a save.
    window.MISDAssetDetail = window.MISDAssetDetail || {};
    window.MISDAssetDetail.onBeforeSave = function () {
        MISDCommon.syncDataTableStateToSessionAndUrl(assetsTable, {
            stateKey: tableStateKey,
            includeOrder: true
        });
    };

    if (urlOpenAsset && typeof window.MISDAssetDetail.open === 'function') {
        window.MISDAssetDetail.open(urlOpenAsset);
    }

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

        if (modal.attr('id') === 'assetHistoryModal') {
            loadAssetHistory(assetTag);
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
        } else if (modal.attr('id') === 'misdMaintenanceModal') {
            MISDCommon.populateModalFields(modal, {
                '#misdMaintenanceAssetTagDisplay': assetTag
            });
        } else if (modal.attr('id') === 'repairedModal') {
            MISDCommon.populateModalFields(modal, {
                '#repairedAssetTagDisplay': assetTag
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

    $('#assetHistoryModal').on('shown.bs.modal', function () {
        assetHistoryTable.columns.adjust();
    });

    $('#printAssetHistoryBtn').on('click', printAssetHistory);

    $('#exportAssetHistoryBtn').on('click', function () {
        assetHistoryTable.button(0).trigger();
    });

});
