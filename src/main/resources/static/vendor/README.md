# Vendored front-end libraries

Served from `/vendor/**` and included through `templates/fragments/vendor.html`.
Files are the unmodified npm `dist` builds, fetched from cdn.jsdelivr.net/npm. The only change is that trailing `sourceMappingURL` comments were removed, because the maps are not vendored.

| Library | Version | Files |
| --- | --- | --- |
| bootstrap | 5.3.8 | `bootstrap/bootstrap.min.css`, `bootstrap/bootstrap.bundle.min.js` |
| bootstrap-icons | 1.13.1 | `bootstrap-icons/` (css + fonts) |
| jquery | 4.0.0 | `jquery/jquery.min.js` |
| datatables.net (+ -bs5) | 3.1.4 | `datatables/dataTables*.js/css` |
| datatables.net-buttons (+ -bs5) | 4.1.2 | `datatables/*buttons*` (CSV/Excel export is built into the core file since Buttons 4) |
| jszip | 3.10.1 | `jszip/jszip.min.js` (Excel export) |
| tom-select | 2.6.2 | `tom-select/` (replaced Select2 4.1.0-rc.0) |
| htmx.org | 2.0.11 | `htmx/htmx.min.js` (server-rendered partial updates; first used by the location pickers) |

To upgrade, replace the files, update this table, and run the Playwright suite in `e2e/`.
DataTables 3 no longer requires jQuery; it is still loaded because the page scripts use it.
