$(document).ready(function () {
    let currentPropertyReferenceId = null;

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

    function renderPropertyDocuments(documents) {
        const body = $('#propertyDocumentsTableBody');
        const empty = $('#propertyDocumentsEmpty');
        body.empty();

        if (!documents || !documents.length) {
            empty.removeClass('d-none');
            return;
        }

        empty.addClass('d-none');
        documents.forEach(doc => {
            const viewUrl = `/documents/${doc.documentId}/view`;
            const downloadUrl = `/documents/${doc.documentId}/download`;
            body.append(`
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
                            <button type="button" class="btn btn-outline-secondary property-doc-print" data-doc-id="${doc.documentId}">Print</button>
                            <button type="button" class="btn btn-outline-danger property-doc-delete" data-doc-id="${doc.documentId}">Remove</button>
                        </div>
                    </td>
                </tr>
            `);
        });
    }

    function loadPropertyDocuments(refId) {
        if (!refId) {
            renderPropertyDocuments([]);
            return;
        }

        $.get('/documents/list', { refType: 'PROPERTY', refId: refId })
            .done(function (documents) {
                renderPropertyDocuments(documents || []);
            })
            .fail(function () {
                $('#propertyDocumentsEmpty').removeClass('d-none').text('Unable to load documents.');
                $('#propertyDocumentsTableBody').empty();
            });
    }

    function clearPropertyFilters(table) {
        table.search('');
        table.columns().search('');
        table.page(0).draw();
        $('.dataTables_filter input').val('').trigger('keyup');
        sessionStorage.removeItem('propertiesTableState');
        const params = new URLSearchParams(window.location.search);
        params.delete('search');
        params.delete('page');
        const nextUrl = `${window.location.pathname}${params.toString() ? `?${params.toString()}` : ''}`;
        window.history.replaceState({}, '', nextUrl);
    }

    MISDCommon.setupThemeToggle('themeToggleBtn');

    MISDCommon.showToast('successToast');

    const propertiesTable = $('#propertiesTable').DataTable({
        pageLength: 25,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'All']],
        order: [[1, 'asc']],
        buttons: [
            { extend: 'csv', className: 'btn btn-primary btn-sm me-2 text-white', text: 'Export to CSV' },
            { extend: 'excel', className: 'btn btn-success btn-sm text-white', text: 'Export to Excel' }
        ],
        dom: "<'row mb-3'<'col-sm-12 col-md-4'l><'col-sm-12 col-md-4 text-center'B><'col-sm-12 col-md-4'f>>" +
            "<'row'<'col-sm-12'tr>>" +
            "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>"
    });

    const propertiesFilterContainer = $('#propertiesTable_filter');
    const propertiesSearchInput = propertiesFilterContainer.find('input[type="search"]');

    propertiesSearchInput.addClass('me-2');
    if (!document.getElementById('clearPropertyFiltersBtn')) {
        propertiesFilterContainer.append(
            '<button class="btn btn-outline-secondary btn-sm" type="button" id="clearPropertyFiltersBtn" aria-label="Clear properties table search">Clear</button>'
        );
    }

    $('#clearPropertyFiltersBtn').on('click', function () {
        clearPropertyFilters(propertiesTable);
    });

    MISDCommon.initSelect2Modals();

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
        renderDetailDocumentPreview('#propertyDetailDocumentFiles', '#propertyDetailDocumentPreview', '#propertyDetailDocumentCategoryTemplate');
    });

    $('#uploadPropertyDocumentsBtn').on('click', function () {
        if (!currentPropertyReferenceId) {
            alert('No property selected.');
            return;
        }

        const fileInput = document.getElementById('propertyDetailDocumentFiles');
        const files = Array.from(fileInput.files || []);
        if (!files.length) {
            alert('Please select file(s) to upload.');
            return;
        }

        const categorySelects = Array.from(document.querySelectorAll('#propertyDetailDocumentPreview select[name="documentCategories"]'));
        const missingCategory = categorySelects.some(select => !select.value);
        if (missingCategory || categorySelects.length !== files.length) {
            alert('Select one document category for each file.');
            return;
        }

        const formData = new FormData();
        formData.append('refType', 'PROPERTY');
        formData.append('refId', currentPropertyReferenceId);
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
                renderDetailDocumentPreview('#propertyDetailDocumentFiles', '#propertyDetailDocumentPreview', '#propertyDetailDocumentCategoryTemplate');
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
            printDocument(`/documents/${docId}/view`);
        }
    });

    $('#propertyDetailModal').on('hidden.bs.modal', function () {
        currentPropertyReferenceId = null;
        $('#propertyDocumentsTableBody').empty();
        $('#propertyDocumentsEmpty').removeClass('d-none').text('No documents attached yet.');
        const fileInput = document.getElementById('propertyDetailDocumentFiles');
        if (fileInput) {
            fileInput.value = '';
        }
        renderDetailDocumentPreview('#propertyDetailDocumentFiles', '#propertyDetailDocumentPreview', '#propertyDetailDocumentCategoryTemplate');
    });
});
