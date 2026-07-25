window.MISDCommon = (function (jqueryGlobal) {
    const $ = jqueryGlobal || null;

    function applyTheme(theme, toggleButton) {
        document.body.setAttribute('data-theme', theme);
        document.documentElement.setAttribute('data-bs-theme', theme);

        if (toggleButton) {
            toggleButton.textContent = theme === 'dark' ? '🌙' : '☀️';
            toggleButton.setAttribute('title', theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
            toggleButton.setAttribute('aria-label', theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
        }

        localStorage.setItem('misd-theme', theme);
    }

    function setupThemeToggle(buttonId, storageKey = 'misd-theme', defaultTheme = 'light') {
        const toggleButton = document.getElementById(buttonId);
        const savedTheme = localStorage.getItem(storageKey) || defaultTheme;

        applyTheme(savedTheme, toggleButton);

        if (toggleButton) {
            toggleButton.addEventListener('click', function () {
                const nextTheme = document.body.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                applyTheme(nextTheme, toggleButton);
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
        const maxSizeBytes = Number.isFinite(maxSizeMb) && maxSizeMb > 0 ? maxSizeMb * 1024 * 1024 : 15 * 1024 * 1024;

        return {
            allowedExtensions,
            maxSizeMb,
            maxSizeBytes
        };
    }

    function getFileValidationResults(input) {
        const { allowedExtensions, maxSizeBytes } = getUploadConstraints(input);

        return Array.from(input?.files || []).map(file => {
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
        const dataTransfer = new DataTransfer();
        files.forEach(file => dataTransfer.items.add(file));
        input.files = dataTransfer.files;
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
        const { maxSizeMb } = getUploadConstraints(input);
        const validationResults = getFileValidationResults(input);
        const tooLarge = validationResults.filter(result => !result.sizeAllowed).map(result => result.file.name);

        if (tooLarge.length) {
            showUploadError(input, `File size exceeds ${maxSizeMb}MB: ${tooLarge.join(', ')}`);
            return false;
        }

        clearUploadError(input);
        return true;
    }

    function renderDocumentPreviewBySelectors(inputSelector, previewSelector, templateSelector) {
        const input = document.querySelector(inputSelector);
        const previewList = document.querySelector(previewSelector);
        const categoryTemplate = document.querySelector(templateSelector);

        if (!input || !previewList) {
            return;
        }

        previewList.innerHTML = '';

        const validationResults = getFileValidationResults(input);
        const files = validationResults.map(result => result.file);
        const { maxSizeMb } = getUploadConstraints(input);
        const tooLarge = validationResults.filter(result => !result.sizeAllowed).map(result => result.file.name);

        if (tooLarge.length) {
            showUploadError(input, `File size exceeds ${maxSizeMb}MB: ${tooLarge.join(', ')}`);
        } else {
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
                        <button type="button" class="btn btn-sm btn-outline-danger" aria-label="Remove ${escapeHtml(file.name)}">X</button>
                        <span class="badge ${result.typeAllowed && result.sizeAllowed ? 'bg-success' : 'bg-danger'}">
                            ${result.typeAllowed && result.sizeAllowed ? 'Ready' : (!result.typeAllowed ? 'Invalid type' : 'Too large')}
                        </span>
                    </div>
                </div>
            `;

            const removeButton = item.querySelector('button');
            removeButton.addEventListener('click', function () {
                const updatedFiles = Array.from(input.files || []).filter((_, fileIndex) => fileIndex !== index);
                setInputFiles(input, updatedFiles);
                renderDocumentPreviewBySelectors(inputSelector, previewSelector, templateSelector);
            });

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

    return {
        setupThemeToggle: setupThemeToggle,
        showToast: showToast,
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
        showUploadError: showUploadError,
        clearUploadError: clearUploadError,
        validateFileInputBySize: validateFileInputBySize,
        renderDocumentPreviewBySelectors: renderDocumentPreviewBySelectors
    };
})(window.jQuery);