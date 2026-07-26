$(document).ready(function () {
    MISDCommon.initPageUI({
        themeToggleId: 'themeToggleBtn'
    });

    $('.needs-validation').on('submit', function (event) {
        if (!this.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
        }
        this.classList.add('was-validated');
    });

    const imported = String(document.body.dataset.psgcImported || '').toLowerCase() === 'true';
    if (imported) {
        if (window.MISDLocationCascade && typeof window.MISDLocationCascade.clearCache === 'function') {
            window.MISDLocationCascade.clearCache();
        } else if (window.sessionStorage) {
            Object.keys(window.sessionStorage).forEach(function (key) {
                if (key === 'misd.psgc.provinces'
                    || key.startsWith('misd.psgc.cities.')
                    || key.startsWith('misd.psgc.barangays.')) {
                    window.sessionStorage.removeItem(key);
                }
            });
        }
    }
});
