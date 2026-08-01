window.MISDCommon = (function (jqueryGlobal) {
    const $ = jqueryGlobal || null;
    const selectedFilesByInput = new WeakMap();

    function applyTheme(theme, toggleButton, storageKey = 'misd-theme') {
        document.body.setAttribute('data-theme', theme);
        document.documentElement.setAttribute('data-bs-theme', theme);

        if (toggleButton) {
            toggleButton.textContent = theme === 'dark' ? '🌙' : '☀️';
            toggleButton.setAttribute('title', theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
            toggleButton.setAttribute('aria-label', theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
        }

        sessionStorage.setItem(storageKey, theme);
    }

    function setupThemeToggle(buttonId, storageKey = 'misd-theme', defaultTheme = 'light') {
        const toggleButton = document.getElementById(buttonId);
        const savedTheme = sessionStorage.getItem(storageKey) || defaultTheme;

        applyTheme(savedTheme, toggleButton, storageKey);

        if (toggleButton) {
            toggleButton.addEventListener('click', function () {
                const nextTheme = document.body.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                applyTheme(nextTheme, toggleButton, storageKey);
            });
        }
    }

    function showToast(toastId, delay = 3000) {
        const toastEl = document.getElementById(toastId);
        if (!toastEl || typeof bootstrap === 'undefined') {
            return null;
        }

        const toast = new bootstrap.Toast(toastEl, { delay: delay });
        toast.show();
        return toast;
    }

    function initPageUI({
        themeToggleId = 'themeToggleBtn',
        setupTheme = true,
        successToastId = null,
        errorToastId = null,
        errorToastDelay = 4000,
        initializeSelect2Modals = false,
        select2ModalSelector = '.modal',
        select2DropdownSelector = '.select2-dropdown'
    } = {}) {
        if (setupTheme && themeToggleId) {
            setupThemeToggle(themeToggleId);
        }

        if (successToastId) {
            showToast(successToastId);
        }

        if (errorToastId) {
            showToast(errorToastId, errorToastDelay);
        }

        if (initializeSelect2Modals) {
            initSelect2Modals(select2ModalSelector, select2DropdownSelector);
        }
    }

    function initSelect2Modals(modalSelector = '.modal', dropdownSelector = '.select2-dropdown') {
        if (!$) {
            return;
        }

        $(document).on('shown.bs.modal', modalSelector, function () {
            const $modal = $(this);
            const $dropdowns = $modal.find(dropdownSelector);

            if (!$dropdowns.length) {
                return;
            }

            $dropdowns.select2({
                theme: 'bootstrap-5',
                dropdownParent: $modal,
                placeholder: 'Type a name to search...',
                minimumInputLength: 2,
                ajax: {
                    url: '/api/personnel/search',
                    dataType: 'json',
                    delay: 250,
                    data: function (params) {
                        return {
                            q: params.term || '',
                            page: (params.page || 1) - 1
                        };
                    },
                    processResults: function (data) {
                        return {
                            results: data.results,
                            pagination: { more: data.pagination.more }
                        };
                    },
                    cache: true
                }
            });
        });

        $(document).on('hidden.bs.modal', modalSelector, function () {
            $(this).find(dropdownSelector).each(function () {
                const $dropdown = $(this);
                if ($dropdown.hasClass('select2-hidden-accessible')) {
                    $dropdown.select2('destroy');
                }
            });
        });
    }

    function bindClick(selector, handler) {
        if (!$) {
            return;
        }

        $(document).on('click', selector, function (event) {
            handler($(this), event);
        });
    }

    function bindModalShow(modalSelector, handler) {
        if (!$) {
            return;
        }

        $(document).on('show.bs.modal', modalSelector, function (event) {
            handler($(this), $(event.relatedTarget), event);
        });
    }

    function populateModalFields(modal, values) {
        Object.entries(values).forEach(([selector, value]) => {
            const target = modal.find(selector);
            if (!target.length) {
                return;
            }

            if (target.is('input, textarea, select')) {
                target.val(value);
            } else {
                target.text(value);
            }
        });
    }

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
        if ($) {
            return $('<div>').text(value || '').html();
        }

        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/\"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function printDocument(url, blockedPopupMessage = 'Unable to open print window. Please allow pop-ups and try again.') {
        const printWindow = window.open('', '_blank');
        if (!printWindow) {
            alert(blockedPopupMessage);
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

    function getUploadConstraints(input) {
        const allowedExtensions = (input?.dataset?.documentAllowedExtensions || '')
            .split(',')
            .map(value => value.trim().toLowerCase())
            .filter(Boolean);
        const maxSizeMb = Number.parseInt(input?.dataset?.documentMaxSizeMb || '15', 10);
        const maxFiles = Number.parseInt(input?.dataset?.documentMaxFiles || '25', 10);
        const maxSizeBytes = Number.isFinite(maxSizeMb) && maxSizeMb > 0 ? maxSizeMb * 1024 * 1024 : 15 * 1024 * 1024;

        return {
            allowedExtensions,
            maxSizeMb,
            maxFiles,
            maxSizeBytes
        };
    }

    function getFileValidationResults(input) {
        const files = getSelectedFiles(input);
        const { allowedExtensions, maxSizeBytes } = getUploadConstraints(input);

        return files.map(file => {
            const extension = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : '';
            const typeAllowed = !allowedExtensions.length || allowedExtensions.includes(extension);
            const sizeAllowed = file.size <= maxSizeBytes;

            return {
                file,
                typeAllowed,
                sizeAllowed,
                isValid: typeAllowed && sizeAllowed
            };
        });
    }

    function setInputFiles(input, files) {
        try {
            const dataTransfer = new DataTransfer();
            files.forEach(file => dataTransfer.items.add(file));
            input.files = dataTransfer.files;
        } catch (error) {
            // Some browser/OS combinations can fail when rebuilding very large file lists.
            console.warn('Unable to update input file list via DataTransfer.', error);
        }
    }

    function getFileSignature(file) {
        return [file.name, file.size, file.lastModified, file.type].join('::');
    }

    function dedupeFiles(files) {
        const seen = new Set();
        const uniqueFiles = [];

        files.forEach(file => {
            const signature = getFileSignature(file);
            if (seen.has(signature)) {
                return;
            }

            seen.add(signature);
            uniqueFiles.push(file);
        });

        return uniqueFiles;
    }

    function areFileArraysEquivalent(left, right) {
        if (left.length !== right.length) {
            return false;
        }

        for (let i = 0; i < left.length; i += 1) {
            if (getFileSignature(left[i]) !== getFileSignature(right[i])) {
                return false;
            }
        }

        return true;
    }

    function getSelectedFiles(input) {
        if (!input) {
            return [];
        }

        const stored = selectedFilesByInput.get(input);
        if (stored && stored.length) {
            return stored.slice();
        }

        return Array.from(input.files || []);
    }

    function setSelectedFiles(input, files) {
        if (!input) {
            return;
        }

        const normalizedFiles = dedupeFiles(Array.from(files || []));
        const existingFiles = selectedFilesByInput.get(input) || [];

        if (areFileArraysEquivalent(existingFiles, normalizedFiles)) {
            return;
        }

        selectedFilesByInput.set(input, normalizedFiles);
        setInputFiles(input, normalizedFiles);
    }

    function prepareMultiFileSelection(input) {
        if (!input) {
            return [];
        }

        const existing = selectedFilesByInput.get(input) || [];
        const incoming = Array.from(input.files || []);

        if (!incoming.length) {
            return existing.slice();
        }

        const merged = dedupeFiles(existing.concat(incoming));
        setSelectedFiles(input, merged);
        return merged.slice();
    }

    function clearSelectedFiles(input, options = {}) {
        const { preserveNativeSelection = false } = options;
        if (!input) {
            return;
        }

        selectedFilesByInput.delete(input);
        if (!preserveNativeSelection) {
            input.value = '';
        }
    }

    function getOrCreateUploadErrorContainer(input) {
        let errorEl = input.parentElement.querySelector('.js-upload-error');
        if (!errorEl) {
            errorEl = document.createElement('div');
            errorEl.className = 'form-text text-danger js-upload-error d-none';
            input.insertAdjacentElement('afterend', errorEl);
        }
        return errorEl;
    }

    function showUploadError(input, message) {
        const errorEl = getOrCreateUploadErrorContainer(input);
        errorEl.textContent = message;
        errorEl.classList.remove('d-none');
    }

    function clearUploadError(input) {
        const errorEl = input.parentElement.querySelector('.js-upload-error');
        if (!errorEl) {
            return;
        }

        errorEl.textContent = '';
        errorEl.classList.add('d-none');
    }

    function validateFileInputBySize(input) {
        const { maxSizeMb, maxFiles } = getUploadConstraints(input);
        const selectedCount = getSelectedFiles(input).length;
        if (selectedCount > maxFiles) {
            showUploadError(input, `You selected ${selectedCount} files. Maximum allowed is ${maxFiles}.`);
            return false;
        }

        const validationResults = getFileValidationResults(input);
        const tooLarge = validationResults.filter(result => !result.sizeAllowed).map(result => result.file.name);

        if (tooLarge.length) {
            showUploadError(input, `File size exceeds ${maxSizeMb}MB: ${tooLarge.join(', ')}`);
            return false;
        }

        clearUploadError(input);
        return true;
    }

    function renderDocumentPreviewBySelectors(inputSelector, previewSelector, templateSelector, options = {}) {
        const { mergeSelection = true, enableRemove = true } = options;
        const input = document.querySelector(inputSelector);
        const previewList = document.querySelector(previewSelector);
        const categoryTemplate = document.querySelector(templateSelector);

        if (!input || !previewList) {
            return;
        }

        previewList.innerHTML = '';

        // Preserve already chosen files when users pick additional files in follow-up selections.
        if (mergeSelection) {
            prepareMultiFileSelection(input);
        }

        const { maxFiles } = getUploadConstraints(input);
        const selectedFiles = getSelectedFiles(input);
        const exceedsMaxFiles = selectedFiles.length > maxFiles;
        if (exceedsMaxFiles) {
            showUploadError(input,
                `You selected ${selectedFiles.length} files. Remove files until no more than ${maxFiles} remain.`);
        }

        const validationResults = getFileValidationResults(input);
        const files = validationResults.map(result => result.file);
        const { maxSizeMb } = getUploadConstraints(input);
        const tooLarge = validationResults.filter(result => !result.sizeAllowed).map(result => result.file.name);

        if (tooLarge.length) {
            showUploadError(input, `File size exceeds ${maxSizeMb}MB: ${tooLarge.join(', ')}`);
        } else if (!exceedsMaxFiles) {
            clearUploadError(input);
        }

        if (!files.length) {
            const emptyItem = document.createElement('li');
            emptyItem.className = 'list-group-item text-muted';
            emptyItem.textContent = 'No files selected.';
            previewList.appendChild(emptyItem);
            return;
        }

        validationResults.forEach((result, index) => {
            const file = result.file;
            const item = document.createElement('li');
            item.className = 'list-group-item d-flex flex-column gap-2';

            item.innerHTML = `
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div class="flex-grow-1">
                        <div class="fw-semibold">${escapeHtml(file.name)}</div>
                        <div class="small text-muted">${formatBytes(file.size)}</div>
                    </div>
                    <div class="d-flex flex-column align-items-end gap-2">
                        ${enableRemove ? `<button type="button" class="btn btn-sm btn-outline-danger" title="Remove file" aria-label="Remove ${escapeHtml(file.name)}">&times;</button>` : ''}
                        <span class="badge ${result.typeAllowed && result.sizeAllowed ? 'bg-success' : 'bg-danger'}">
                            ${result.typeAllowed && result.sizeAllowed ? 'Ready' : (!result.typeAllowed ? 'Invalid type' : 'Too large')}
                        </span>
                    </div>
                </div>
            `;

            if (enableRemove) {
                const removeButton = item.querySelector('button');
                removeButton.addEventListener('click', function () {
                    const updatedFiles = getSelectedFiles(input).filter((_, fileIndex) => fileIndex !== index);
                    setSelectedFiles(input, updatedFiles);
                    renderDocumentPreviewBySelectors(inputSelector, previewSelector, templateSelector,
                        { mergeSelection: false, enableRemove: true });
                });
            }

            if (categoryTemplate) {
                const label = document.createElement('label');
                label.className = 'form-label fw-semibold mb-0 small';
                label.innerHTML = 'Document category <span class="required-indicator" aria-hidden="true">*</span>';

                const select = document.createElement('select');
                select.id = `${input.id || 'documentFiles'}Category${index}`;
                select.className = 'form-select form-select-sm';
                select.name = 'documentCategories';
                select.required = true;
                select.innerHTML = categoryTemplate.innerHTML;
                label.htmlFor = select.id;

                item.appendChild(label);
                item.appendChild(select);
            }

            previewList.appendChild(item);
        });
    }

    function updateUrlSearchParams(mutateParams) {
        const params = new URLSearchParams(window.location.search);
        mutateParams(params);
        const nextUrl = `${window.location.pathname}${params.toString() ? `?${params.toString()}` : ''}`;
        window.history.replaceState({}, '', nextUrl);
    }

    function buildDataTableExportButtons({
        csvClassName = 'btn btn-primary btn-sm me-2 text-white',
        excelClassName = 'btn btn-success btn-sm text-white',
        csvText = 'Export to CSV',
        excelText = 'Export to Excel'
    } = {}) {
        return [
            { extend: 'csv', className: csvClassName, text: csvText },
            { extend: 'excel', className: excelClassName, text: excelText }
        ];
    }

    function buildStandardDataTableConfig({
        pageLength = 25,
        lengthMenu = [[10, 25, 50, -1], [10, 25, 50, 'All']],
        order = [[0, 'asc']],
        dom = "<'row mb-3'<'col-sm-12 col-md-4'l><'col-sm-12 col-md-4 text-center'B><'col-sm-12 col-md-4'f>>" +
        "<'row'<'col-sm-12'tr>>" +
        "<'row pt-2'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
        buttons = null,
        exportButtonOptions = {},
        language = null
    } = {}) {
        const config = {
            pageLength,
            lengthMenu,
            order,
            buttons: buttons || buildDataTableExportButtons(exportButtonOptions),
            dom
        };

        if (language) {
            config.language = language;
        }

        return config;
    }

    function clearDataTableFilters(table, {
        stateKey = null,
        searchParam = 'search',
        pageParam = 'page',
        searchInputSelector = '.dataTables_filter input'
    } = {}) {
        table.search('');
        table.columns().search('');
        table.page(0).draw();

        document.querySelectorAll(searchInputSelector).forEach(input => {
            input.value = '';
            input.dispatchEvent(new Event('keyup', { bubbles: true }));
        });

        if (stateKey) {
            sessionStorage.removeItem(stateKey);
        }

        updateUrlSearchParams(params => {
            params.delete(searchParam);
            params.delete(pageParam);
        });
    }

    function attachDataTableClearButton({
        filterContainerSelector,
        buttonId,
        onClear,
        buttonLabel = 'Clear',
        buttonClass = 'btn btn-outline-secondary btn-sm',
        ariaLabel = 'Clear table search',
        searchInputClass = 'me-2'
    }) {
        const filterContainer = document.querySelector(filterContainerSelector);
        if (!filterContainer) {
            return null;
        }

        const searchInput = filterContainer.querySelector('input[type="search"]');
        if (searchInput && searchInputClass) {
            searchInputClass.split(' ').filter(Boolean).forEach(className => {
                searchInput.classList.add(className);
            });
        }

        let button = document.getElementById(buttonId);
        if (!button) {
            button = document.createElement('button');
            button.type = 'button';
            button.id = buttonId;
            button.className = buttonClass;
            button.setAttribute('aria-label', ariaLabel);
            button.textContent = buttonLabel;
            filterContainer.appendChild(button);
        }

        if (typeof onClear === 'function' && button.dataset.misdBound !== 'true') {
            button.addEventListener('click', onClear);
            button.dataset.misdBound = 'true';
        }

        return button;
    }

    function getDataTableState(table, includeOrder = true) {
        const state = {
            search: table.search(),
            page: table.page.info().page
        };

        if (includeOrder) {
            state.order = table.order();
        }

        return state;
    }

    function syncDataTableStateToSessionAndUrl(table, {
        stateKey,
        searchParam = 'search',
        pageParam = 'page',
        includeOrder = true
    }) {
        const state = getDataTableState(table, includeOrder);

        updateUrlSearchParams(params => {
            if (state.search) {
                params.set(searchParam, state.search);
            } else {
                params.delete(searchParam);
            }

            params.set(pageParam, String(state.page));
        });

        if (stateKey) {
            sessionStorage.setItem(stateKey, JSON.stringify(state));
        }
    }

    function restoreDataTableStateFromSessionAndUrl(table, {
        stateKey,
        searchParam = 'search',
        pageParam = 'page',
        defaultPage = 0,
        restoreOrder = true,
        warningMessage = 'Unable to restore saved table state.'
    }) {
        const savedState = stateKey ? sessionStorage.getItem(stateKey) : null;
        const params = new URLSearchParams(window.location.search);
        const urlSearch = params.get(searchParam) || '';
        const urlPage = Number(params.get(pageParam));

        try {
            const state = savedState ? JSON.parse(savedState) : null;
            const searchValue = urlSearch || (state && state.search ? state.search : '');
            const pageValue = Number.isFinite(urlPage) && urlPage >= 0
                ? urlPage
                : (state && typeof state.page === 'number' ? state.page : defaultPage);

            table.search(searchValue || '');

            if (restoreOrder && state && state.order) {
                table.order(state.order);
            }

            table.page(pageValue).draw(false);
        } catch (error) {
            console.warn(warningMessage, error);
        }
    }

    function renderDocumentsTable({
        documents,
        bodySelector,
        emptySelector,
        tableSelector = null,
        printButtonClass,
        deleteButtonClass,
        emptyText = null
    }) {
        if (!$) {
            return;
        }

        const body = $(bodySelector);
        const empty = $(emptySelector);
        const table = tableSelector ? $(tableSelector) : null;
        body.empty();

        if (!documents || !documents.length) {
            empty.removeClass('d-none');
            if (table && table.length) {
                table.addClass('d-none');
            }
            if (emptyText) {
                empty.text(emptyText);
            }
            return;
        }

        empty.addClass('d-none');
        if (table && table.length) {
            table.removeClass('d-none');
        }
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
                        <div class="doc-action-list" role="group" aria-label="Document actions">
                            <a class="btn btn-outline-primary doc-action-btn doc-action-btn--view" href="${viewUrl}" target="_blank" title="View" aria-label="View document">
                                <img src="/icons/view.svg" alt="" class="doc-action-icon" aria-hidden="true">
                            </a>
                            <a class="btn btn-outline-success doc-action-btn doc-action-btn--download" href="${downloadUrl}" title="Download" aria-label="Download document">
                                <img src="/icons/download.svg" alt="" class="doc-action-icon" aria-hidden="true">
                            </a>
                            <button type="button" class="btn doc-action-btn doc-action-btn--print js-doc-print ${printButtonClass}" data-doc-id="${doc.documentId}" title="Print" aria-label="Print document">
                                <img src="/icons/print.svg" alt="" class="doc-action-icon" aria-hidden="true">
                            </button>
                            <button type="button" class="btn btn-outline-danger doc-action-btn doc-action-btn--delete js-doc-delete ${deleteButtonClass}" data-doc-id="${doc.documentId}" title="Remove" aria-label="Remove document">
                                <img src="/icons/delete.svg" alt="" class="doc-action-icon" aria-hidden="true">
                            </button>
                        </div>
                    </td>
                </tr>
            `);
        });
    }

    function loadDocumentsForReference({
        refType,
        refId,
        bodySelector,
        emptySelector,
        tableSelector = null,
        printButtonClass,
        deleteButtonClass,
        emptyText = null,
        loadErrorText = 'Unable to load documents.'
    }) {
        if (!$) {
            return;
        }

        if (!refId) {
            renderDocumentsTable({
                documents: [],
                bodySelector,
                emptySelector,
                tableSelector,
                printButtonClass,
                deleteButtonClass,
                emptyText
            });
            return;
        }

        $.get('/documents/list', { refType: refType, refId: refId })
            .done(function (documents) {
                renderDocumentsTable({
                    documents: documents || [],
                    bodySelector,
                    emptySelector,
                    tableSelector,
                    printButtonClass,
                    deleteButtonClass,
                    emptyText
                });
            })
            .fail(function () {
                $(emptySelector).removeClass('d-none').text(loadErrorText);
                $(bodySelector).empty();
                if (tableSelector) {
                    $(tableSelector).addClass('d-none');
                }
            });
    }

    function showActionConfirmationModal({
        title = 'Confirm Action',
        message = 'Continue?',
        confirmButtonText = 'Confirm',
        confirmButtonClass = 'btn-danger',
        cancelButtonText = 'Cancel',
        onConfirm
    } = {}) {
        if (!$ || !window.bootstrap) {
            const accepted = window.confirm(message);
            if (accepted && typeof onConfirm === 'function') {
                onConfirm();
            }
            return;
        }

        const modalId = 'misdActionConfirmModal';
        let modalElement = document.getElementById(modalId);

        if (!modalElement) {
            modalElement = document.createElement('div');
            modalElement.id = modalId;
            modalElement.className = 'modal fade';
            modalElement.setAttribute('tabindex', '-1');
            modalElement.setAttribute('aria-hidden', 'true');
            modalElement.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title"></h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body"></div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-role="cancel" data-bs-dismiss="modal"></button>
                            <button type="button" class="btn" data-role="confirm"></button>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(modalElement);
        }

        const titleElement = modalElement.querySelector('.modal-title');
        const bodyElement = modalElement.querySelector('.modal-body');
        const cancelButton = modalElement.querySelector('[data-role="cancel"]');
        const confirmButton = modalElement.querySelector('[data-role="confirm"]');

        titleElement.textContent = title;
        bodyElement.textContent = message;
        cancelButton.textContent = cancelButtonText;
        confirmButton.textContent = confirmButtonText;
        confirmButton.className = `btn ${confirmButtonClass}`;

        const modalInstance = bootstrap.Modal.getOrCreateInstance(modalElement);
        confirmButton.onclick = function () {
            modalInstance.hide();
            if (typeof onConfirm === 'function') {
                onConfirm();
            }
        };

        modalInstance.show();
    }

    function deleteDocumentById(documentId, {
        confirmMessage = 'Remove this document?',
        confirmTitle = 'Remove Document',
        confirmButtonText = 'Remove',
        confirmButtonClass = 'btn-danger',
        cancelButtonText = 'Cancel',
        useModalConfirm = false,
        defaultErrorMessage = 'Failed to remove document.',
        onSuccess = null,
        onError = null
    } = {}) {
        if (!$ || !documentId) {
            return false;
        }

        const runDeleteRequest = function () {
            $.ajax({
                url: `/documents/${documentId}`,
                type: 'DELETE',
                success: function (response) {
                    if (typeof onSuccess === 'function') {
                        onSuccess(response);
                    }
                },
                error: function (xhr) {
                    if (typeof onError === 'function') {
                        onError(xhr);
                        return;
                    }

                    const message = xhr?.responseJSON?.error || defaultErrorMessage;
                    alert(message);
                }
            });
        };

        if (!confirmMessage) {
            runDeleteRequest();
            return true;
        }

        if (useModalConfirm) {
            showActionConfirmationModal({
                title: confirmTitle,
                message: confirmMessage,
                confirmButtonText,
                confirmButtonClass,
                cancelButtonText,
                onConfirm: runDeleteRequest
            });
            return true;
        }

        if (!window.confirm(confirmMessage)) {
            return false;
        }

        runDeleteRequest();

        return true;
    }

    function getDocumentUploadSelection({
        fileInputSelector,
        previewSelector,
        requireFilesMessage = 'Please select file(s) to upload.',
        requireCategoryMessage = 'Select one document category for each file.'
    }) {
        const fileInput = document.querySelector(fileInputSelector);
        if (!fileInput) {
            return {
                isValid: false,
                message: 'File input not found.',
                files: [],
                categorySelects: [],
                fileInput: null
            };
        }

        if (!validateFileInputBySize(fileInput)) {
            return {
                isValid: false,
                message: null,
                files: [],
                categorySelects: [],
                fileInput
            };
        }

        const files = Array.from(fileInput.files || []);
        if (!files.length) {
            return {
                isValid: false,
                message: requireFilesMessage,
                files,
                categorySelects: [],
                fileInput
            };
        }

        const categorySelects = Array.from(document.querySelectorAll(`${previewSelector} select[name="documentCategories"]`));
        categorySelects.forEach(select => {
            select.classList.remove('is-invalid');
            if (typeof select.setCustomValidity === 'function') {
                select.setCustomValidity('');
            }
        });

        const missingCategory = categorySelects.some(select => !select.value);

        if (missingCategory || categorySelects.length !== files.length) {
            categorySelects.forEach(select => {
                if (!select.value) {
                    select.classList.add('is-invalid');
                    if (typeof select.setCustomValidity === 'function') {
                        select.setCustomValidity(requireCategoryMessage);
                        if (typeof select.reportValidity === 'function') {
                            select.reportValidity();
                        }
                    }
                }
            });

            return {
                isValid: false,
                message: requireCategoryMessage,
                files,
                categorySelects,
                fileInput
            };
        }

        return {
            isValid: true,
            message: null,
            files,
            categorySelects,
            fileInput
        };
    }

    function buildDocumentUploadFormData({ refType, refId, files, categorySelects }) {
        const formData = new FormData();
        formData.append('refType', refType);
        formData.append('refId', refId);

        files.forEach(file => formData.append('documentFiles', file));
        categorySelects.forEach(select => formData.append('documentCategories', select.value));

        return formData;
    }

    function resetDocumentDetailUI({
        bodySelector,
        emptySelector,
        emptyText = 'No documents attached yet.',
        tableSelector = null,
        fileInputSelector,
        previewInputSelector,
        previewListSelector,
        previewTemplateSelector
    }) {
        if ($) {
            $(bodySelector).empty();
            $(emptySelector).removeClass('d-none').text(emptyText);
            if (tableSelector) {
                $(tableSelector).addClass('d-none');
            }
        }

        const fileInput = document.querySelector(fileInputSelector);
        if (fileInput) {
            clearSelectedFiles(fileInput);
        }

        renderDocumentPreviewBySelectors(previewInputSelector, previewListSelector, previewTemplateSelector);
    }

    function appendDataTableStateToFormAction(form, state, {
        searchParam = 'search',
        pageParam = 'page'
    } = {}) {
        const formElement = form instanceof HTMLElement ? form : (form?.[0] || null);
        if (!formElement) {
            return;
        }

        const params = new URLSearchParams(window.location.search);

        if (state.search) {
            params.set(searchParam, state.search);
        } else {
            params.delete(searchParam);
        }

        params.set(pageParam, String(state.page));

        const actionValue = formElement.getAttribute('action') || window.location.pathname;
        const actionUrl = new URL(actionValue, window.location.origin);
        actionUrl.search = params.toString();
        formElement.setAttribute('action', `${actionUrl.pathname}${actionUrl.search}`);
    }

    return {
        setupThemeToggle: setupThemeToggle,
        showToast: showToast,
        initPageUI: initPageUI,
        initSelect2Modals: initSelect2Modals,
        bindClick: bindClick,
        bindModalShow: bindModalShow,
        populateModalFields: populateModalFields,
        formatBytes: formatBytes,
        formatUploadDate: formatUploadDate,
        escapeHtml: escapeHtml,
        printDocument: printDocument,
        getUploadConstraints: getUploadConstraints,
        getFileValidationResults: getFileValidationResults,
        setInputFiles: setInputFiles,
        getSelectedFiles: getSelectedFiles,
        prepareMultiFileSelection: prepareMultiFileSelection,
        clearSelectedFiles: clearSelectedFiles,
        showUploadError: showUploadError,
        clearUploadError: clearUploadError,
        validateFileInputBySize: validateFileInputBySize,
        renderDocumentPreviewBySelectors: renderDocumentPreviewBySelectors,
        buildDataTableExportButtons: buildDataTableExportButtons,
        buildStandardDataTableConfig: buildStandardDataTableConfig,
        clearDataTableFilters: clearDataTableFilters,
        attachDataTableClearButton: attachDataTableClearButton,
        getDataTableState: getDataTableState,
        syncDataTableStateToSessionAndUrl: syncDataTableStateToSessionAndUrl,
        restoreDataTableStateFromSessionAndUrl: restoreDataTableStateFromSessionAndUrl,
        renderDocumentsTable: renderDocumentsTable,
        loadDocumentsForReference: loadDocumentsForReference,
        deleteDocumentById: deleteDocumentById,
        getDocumentUploadSelection: getDocumentUploadSelection,
        buildDocumentUploadFormData: buildDocumentUploadFormData,
        resetDocumentDetailUI: resetDocumentDetailUI,
        appendDataTableStateToFormAction: appendDataTableStateToFormAction
    };
})(window.jQuery);