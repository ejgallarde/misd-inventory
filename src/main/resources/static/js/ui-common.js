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

    return {
        setupThemeToggle: setupThemeToggle,
        showToast: showToast,
        initSelect2Modals: initSelect2Modals,
        bindClick: bindClick,
        bindModalShow: bindModalShow,
        populateModalFields: populateModalFields
    };
})(window.jQuery);