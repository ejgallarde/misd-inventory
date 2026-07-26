document.addEventListener('DOMContentLoaded', function () {
    // New login/session should start with a clean client-side UI state.
    sessionStorage.clear();
    localStorage.removeItem('misd-theme');

    document.querySelectorAll('form').forEach(form => {
        form.reset();
    });

    if (window.MISDCommon) {
        window.MISDCommon.initPageUI({ themeToggleId: 'themeToggleBtn' });
    }
});