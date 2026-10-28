# Vendored front-end libraries

Served from `/vendor/**` and included through `templates/fragments/vendor.html`.
Files are the unmodified npm `dist` builds, fetched from cdn.jsdelivr.net/npm. The only change is that trailing `sourceMappingURL` comments were removed, because the maps are not vendored.

| Library | Version | Files |
| --- | --- | --- |
| bootstrap | 5.3.8 | `bootstrap/bootstrap.min.css`, `bootstrap/bootstrap.bundle.min.js` |
| bootstrap-icons | 1.13.1 | `bootstrap-icons/` (css + fonts) |
| jquery | 3.7.1 | `jquery/jquery.min.js` |
| datatables.net (+ -bs5) | 2.3.8 | `datatables/dataTables*.js/css` |
| datatables.net-buttons (+ -bs5) | 3.2.6 | `datatables/*buttons*` |
| jszip | 3.10.1 | `jszip/jszip.min.js` (Excel export) |
| tom-select | 2.6.2 | `tom-select/` (replaced Select2 4.1.0-rc.0) |

To upgrade, replace the files, update this table, and run the Playwright suite in `e2e/`.
DataTables 3.x and jQuery 4.x exist but were not adopted in this step. Treat each as its own upgrade.
