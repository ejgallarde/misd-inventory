$(document).ready(function () {
    const clearAssetFiltersBtn = document.getElementById('clearAssetFiltersBtn');
    const tableStateKey = 'assetsTableState';

    function clearAssetFilters(table) {
        table.search('');
        table.columns().search('');
        table.page(0).draw();
        $('.dataTables_filter input').val('').trigger('keyup');
        sessionStorage.removeItem(tableStateKey);
        const params = new URLSearchParams(window.location.search);
        params.delete('search');
        params.delete('page');
        const nextUrl = `${window.location.pathname}${params.toString() ? `?${params.toString()}` : ''}`;
        window.history.replaceState({}, '', nextUrl);
    }

    MISDCommon.setupThemeToggle('themeToggleBtn');

    if (clearAssetFiltersBtn) {
        clearAssetFiltersBtn.addEventListener('click', function () {
            clearAssetFilters(assetsTable);
        });
    }

    function getAssetsTableState(table) {
        return {
            search: table.search(),
            page: table.page.info().page,
            order: table.order()
        };
    }

    function syncAssetsTableStateToUrl(table) {
        const state = getAssetsTableState(table);
        const params = new URLSearchParams(window.location.search);

        if (state.search) {
            params.set('search', state.search);
        } else {
            params.delete('search');
        }

        params.set('page', String(state.page));

        const nextUrl = `${window.location.pathname}${params.toString() ? `?${params.toString()}` : ''}`;
        window.history.replaceState({}, '', nextUrl);

        sessionStorage.setItem(tableStateKey, JSON.stringify(state));
    }

    function restoreAssetsTableState(table) {
        const savedState = sessionStorage.getItem(tableStateKey);
        const params = new URLSearchParams(window.location.search);
        const urlSearch = params.get('search') || '';
        const urlPage = Number(params.get('page')) || 0;

        try {
            const state = savedState ? JSON.parse(savedState) : null;
            const searchValue = urlSearch || (state && state.search ? state.search : '');
            const pageValue = Number.isFinite(urlPage) && urlPage >= 0 ? urlPage : (state && typeof state.page === 'number' ? state.page : 0);

            if (searchValue) {
                table.search(searchValue);
            } else {
                table.search('');
            }

            if (state && state.order) {
                table.order(state.order);
            }

            table.page(pageValue).draw(false);
        } catch (error) {
            console.warn('Unable to restore saved asset table state.', error);
        }
    }

    MISDCommon.showToast('successToast');

    // DataTables
    const assetsTable = $('#assetsTable').DataTable({
        pageLength: 25,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, "All"]],
        order: [[0, "asc"]],
        buttons: [
            { extend: 'csv', className: 'btn btn-primary btn-sm me-2 text-white', text: 'Export to CSV' },
            { extend: 'excel', className: 'btn btn-success btn-sm text-white', text: 'Export to Excel' }
        ],
        dom: "<'row mb-3'<'col-sm-12 col-md-4'l><'col-sm-12 col-md-4 text-center'B><'col-sm-12 col-md-4'f>>" +
            "<'row'<'col-sm-12'tr>>" +
            "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>"
    });

    assetsTable.on('search.dt draw.dt order.dt page.dt', function () {
        syncAssetsTableStateToUrl(assetsTable);
    });

    restoreAssetsTableState(assetsTable);

    MISDCommon.initSelect2Modals();

    // --- SLIDEOUT & EDIT LOGIC ---

    let currentActiveAssetTag = ''; // Tracks the currently open asset

    function formatBytes(bytes) {
        if (!Number.isFinite(bytes) || bytes <= 0) {
            return '0 B';
        }

        const units = ['B', 'KB', 'MB', 'GB'];
        const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
        const value = bytes / Math.pow(1024, index);
        return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
    }

    function formatUploadDate(value) {
        if (!value) {
            return 'N/A';
        }

        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) {
            return value;
        }
        return parsed.toLocaleString();
    }

    function escapeHtml(value) {
        return $('<div>').text(value || '').html();
    }

    function printDocument(url) {
        const printWindow = window.open('', '_blank');
        if (!printWindow) {
            alert('Unable to open print window. Please allow pop-ups and try again.');
            return;
        }

        printWindow.document.write(`
            <html><head><title>Print Document</title></head>
            <body style="margin:0">
                <iframe src="${url}" style="border:0;width:100%;height:100vh;"></iframe>
            </body></html>
        `);
        printWindow.document.close();
        printWindow.onload = function () {
            printWindow.focus();
            printWindow.print();
        };
    }

    function renderDetailDocumentPreview(inputSelector, previewSelector, templateSelector) {
        const input = document.querySelector(inputSelector);
        const previewList = document.querySelector(previewSelector);
        const categoryTemplate = document.querySelector(templateSelector);

        if (!input || !previewList) {
            return;
        }

        previewList.innerHTML = '';

        const allowedExtensions = (input.dataset.documentAllowedExtensions || '')
            .split(',')
            .map(value => value.trim().toLowerCase())
            .filter(Boolean);
        const maxSizeMb = Number.parseInt(input.dataset.documentMaxSizeMb || '10', 10);
        const maxSizeBytes = Number.isFinite(maxSizeMb) && maxSizeMb > 0 ? maxSizeMb * 1024 * 1024 : 10 * 1024 * 1024;
        const files = Array.from(input.files || []);

        if (!files.length) {
            const emptyItem = document.createElement('li');
            emptyItem.className = 'list-group-item text-muted';
            emptyItem.textContent = 'No files selected.';
            previewList.appendChild(emptyItem);
            return;
        }

        files.forEach(file => {
            const extension = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : '';
            const typeAllowed = !allowedExtensions.length || allowedExtensions.includes(extension);
            const sizeAllowed = file.size <= maxSizeBytes;

            const item = document.createElement('li');
            item.className = 'list-group-item d-flex flex-column gap-2';

            item.innerHTML = `
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="fw-semibold">${escapeHtml(file.name)}</div>
                        <div class="small text-muted">${formatBytes(file.size)}</div>
                    </div>
                    <span class="badge ${typeAllowed && sizeAllowed ? 'bg-success' : 'bg-danger'}">
                        ${typeAllowed && sizeAllowed ? 'Ready' : (!typeAllowed ? 'Invalid type' : 'Too large')}
                    </span>
                </div>
            `;

            if (categoryTemplate) {
                const label = document.createElement('label');
                label.className = 'form-label fw-semibold mb-0 small';
                label.textContent = 'Document category';

                const select = document.createElement('select');
                select.className = 'form-select form-select-sm';
                select.name = 'documentCategories';
                select.required = true;
                select.innerHTML = categoryTemplate.innerHTML;

                item.appendChild(label);
                item.appendChild(select);
            }

            previewList.appendChild(item);
        });
    }

    function renderAssetDocuments(documents) {
        const body = $('#assetDocumentsTableBody');
        const empty = $('#assetDocumentsEmpty');

        body.empty();

        if (!documents || !documents.length) {
            empty.removeClass('d-none');
            return;
        }

        empty.addClass('d-none');

        documents.forEach(doc => {
            const viewUrl = `/documents/${doc.documentId}/view`;
            const downloadUrl = `/documents/${doc.documentId}/download`;

            const row = `
                <tr>
                    <td>${escapeHtml(doc.documentCategory || 'N/A')}</td>
                    <td>
                        <div class="fw-semibold">${escapeHtml(doc.fileName || 'Unnamed file')}</div>
                        <div class="small text-muted">${formatBytes(doc.fileSize || 0)}</div>
                    </td>
                    <td>${escapeHtml(formatUploadDate(doc.uploadDate))}</td>
                    <td class="text-center">
                        <div class="btn-group btn-group-sm" role="group">
                            <a class="btn btn-outline-primary" href="${viewUrl}" target="_blank">View</a>
                            <a class="btn btn-outline-success" href="${downloadUrl}">Download</a>
                            <button type="button" class="btn btn-outline-secondary asset-doc-print" data-doc-id="${doc.documentId}">Print</button>
                            <button type="button" class="btn btn-outline-danger asset-doc-delete" data-doc-id="${doc.documentId}">Remove</button>
                        </div>
                    </td>
                </tr>
            `;

            body.append(row);
        });
    }

    function loadAssetDocuments(assetTag) {
        if (!assetTag) {
            renderAssetDocuments([]);
            return;
        }

        $.get('/documents/list', { refType: 'IT_EQUIPMENT', refId: assetTag })
            .done(function (documents) {
                renderAssetDocuments(documents || []);
            })
            .fail(function () {
                $('#assetDocumentsEmpty').removeClass('d-none').text('Unable to load documents.');
                $('#assetDocumentsTableBody').empty();
            });
    }

    // Helper function to lock the form back to view-only mode
    function lockForm() {
        $('.it-field').prop('disabled', true);
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
        // Unlock all fields EXCEPT Asset Tag and Serial Number
        $('.it-field').not('#editAssetTag, #editSerialNumber').prop('disabled', false);

        // Keep 'Assigned To' locked unless Status is already 'Deployed'
        if ($('#editCurrentStatus').val() !== 'Deployed') {
            $('#editCurrentOwnerID').prop('disabled', true);
        }

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

        syncAssetsTableStateToUrl(assetsTable);

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
            $('#editAssetTag').val(data.assetTag);
            $('#editCatalogID').val(data.catalogID);
            $('#editSerialNumber').val(data.serialNumber);

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
        renderDetailDocumentPreview('#assetDetailDocumentFiles', '#assetDetailDocumentPreview', '#assetDetailDocumentCategoryTemplate');
    });

    $('#uploadAssetDocumentsBtn').on('click', function () {
        if (!currentActiveAssetTag) {
            alert('No asset selected.');
            return;
        }

        const fileInput = document.getElementById('assetDetailDocumentFiles');
        const files = Array.from(fileInput.files || []);
        if (!files.length) {
            alert('Please select file(s) to upload.');
            return;
        }

        const categorySelects = Array.from(document.querySelectorAll('#assetDetailDocumentPreview select[name="documentCategories"]'));
        const missingCategory = categorySelects.some(select => !select.value);
        if (missingCategory || categorySelects.length !== files.length) {
            alert('Select one document category for each file.');
            return;
        }

        const formData = new FormData();
        formData.append('refType', 'IT_EQUIPMENT');
        formData.append('refId', currentActiveAssetTag);

        files.forEach(file => formData.append('documentFiles', file));
        categorySelects.forEach(select => formData.append('documentCategories', select.value));

        $.ajax({
            url: '/documents/add',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function () {
                fileInput.value = '';
                renderDetailDocumentPreview('#assetDetailDocumentFiles', '#assetDetailDocumentPreview', '#assetDetailDocumentCategoryTemplate');
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
        if (!docId) {
            return;
        }

        if (!confirm('Remove this document?')) {
            return;
        }

        $.ajax({
            url: `/documents/${docId}`,
            type: 'DELETE',
            success: function () {
                loadAssetDocuments(currentActiveAssetTag);
            },
            error: function (xhr) {
                const message = xhr?.responseJSON?.error || 'Failed to remove document.';
                alert(message);
            }
        });
    });

    MISDCommon.bindClick('.asset-doc-print', function (button) {
        const docId = button.data('doc-id');
        if (docId) {
            printDocument(`/documents/${docId}/view`);
        }
    });

    $('#itDetailOffcanvas').on('hidden.bs.offcanvas', function () {
        currentActiveAssetTag = '';
        $('#assetDocumentsTableBody').empty();
        $('#assetDocumentsEmpty').removeClass('d-none').text('No documents attached yet.');
        const fileInput = document.getElementById('assetDetailDocumentFiles');
        if (fileInput) {
            fileInput.value = '';
        }
        renderDetailDocumentPreview('#assetDetailDocumentFiles', '#assetDetailDocumentPreview', '#assetDetailDocumentCategoryTemplate');
    });

    // Persist the current table view before action forms redirect back to the list
    $('form').on('submit', function (event) {
        const form = $(this);
        const state = getAssetsTableState(assetsTable);
        const params = new URLSearchParams(window.location.search);

        if (state.search) {
            params.set('search', state.search);
        } else {
            params.delete('search');
        }

        params.set('page', String(state.page));
        const actionUrl = new URL(form.attr('action') || window.location.pathname, window.location.origin);
        actionUrl.search = params.toString();
        form.attr('action', `${actionUrl.pathname}${actionUrl.search}`);
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