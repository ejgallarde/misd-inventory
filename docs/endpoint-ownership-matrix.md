# Endpoint Ownership Matrix

Last updated: 2026-07-26
Purpose: Track controller endpoint ownership and internal callers (template forms, links, and JS AJAX) before any future cleanup.

## Scope
- Included: Endpoints declared in controllers under src/main/java/ph/gov/phlpost/inventory/misddashboard/controller.
- Included: Internal callers from Thymeleaf templates and static JS.
- Excluded: Framework-managed routes (for example Spring Security internals), unless noted.

## Matrix

| Method | Route | Owning Controller | Internal Caller Type | Internal Callers | Status |
|---|---|---|---|---|---|
| GET | / | MainDashboardController | Link / redirect target | templates/assets.html (Back to Dashboard), templates/fleet.html, templates/properties.html | Active |
| GET | /login | LoginController | Browser route | Security/login navigation | Active |
| GET | /api/personnel/search | MainDashboardController | JS AJAX | static/js/ui-common.js (Select2 AJAX source) | Active |
| GET | /assets | ITAssetController | Link | templates/dashboard.html (IT card href) | Active |
| POST | /catalog/add | ITAssetController | Form submit | templates/dashboard.html (Add to Catalog form) | Active |
| POST | /assets/receive | ITAssetController | Form submit | templates/dashboard.html (Receive Asset form) | Active |
| POST | /assets/assign | ITAssetController | Form submit | templates/assets.html (Assign/Re-assign modal) | Active |
| POST | /assets/return | ITAssetController | Form submit | templates/assets.html (Return modal) | Active |
| POST | /assets/unserviceable | ITAssetController | Form submit | templates/assets.html (Unserviceable modal) | Active |
| POST | /assets/warranty | ITAssetController | Form submit | templates/assets.html (Warranty modal) | Active |
| POST | /assets/misd-maintenance | ITAssetController | Form submit | templates/assets.html (MISD Maintenance modal) | Active |
| POST | /assets/repaired | ITAssetController | Form submit | templates/assets.html (Mark Asset as Repaired modal) | Active |
| POST | /assets/retire | ITAssetController | Form submit | templates/assets.html (Retire modal) | Active |
| GET | /assets/{id} | ITAssetController | JS AJAX | static/js/assets.js (loadAssetDetails) | Active |
| GET | /api/assets/{assetTag}/history | ITAssetController | JS AJAX | static/js/assets.js (View Asset History modal) | Active |
| POST | /assets/update | ITAssetController | JS AJAX | static/js/assets.js (save edit) | Active |
| GET | /fleet | FleetController | Link | templates/dashboard.html (Fleet card href) | Active |
| POST | /fleet/add | FleetController | Form submit | templates/dashboard.html (Register Vehicle form) | Active |
| POST | /fleet/assign | FleetController | Form submit | templates/fleet.html (Assign/Re-assign modal) | Active |
| POST | /fleet/return | FleetController | Form submit | templates/fleet.html (Return modal) | Active |
| POST | /fleet/retire | FleetController | Form submit | templates/fleet.html (Retire modal) | Active |
| GET | /fleet/{id} | FleetController | JS AJAX | static/js/fleet-page.js (loadFleetDetails) | Active |
| POST | /fleet/update | FleetController | API endpoint (no current internal caller) | No current template/JS reference | Keep (possible external/manual API use) |
| GET | /properties | PropertiesController | Link | templates/dashboard.html (Properties card href) | Active |
| POST | /properties/add | PropertiesController | Form submit | templates/dashboard.html (Add Property form) | Active |
| POST | /properties/assign-custodian | PropertiesController | Form submit | templates/properties.html (Custodian modal) | Active |
| POST | /properties/update-tax | PropertiesController | Form submit | templates/properties.html (Tax modal) | Active |
| GET | /properties/{id} | PropertiesController | JS AJAX | static/js/properties-page.js (property detail load) | Active |
| POST | /properties/update | PropertiesController | API endpoint (no current internal caller) | No current template/JS reference | Keep (possible external/manual API use) |
| GET | /documents/list | DocumentController | JS AJAX | static/js/ui-common.js (loadDocumentsForReference) | Active |
| POST | /documents/add | DocumentController | JS AJAX | static/js/assets.js, static/js/fleet-page.js, static/js/properties-page.js | Active |
| DELETE | /documents/{id} | DocumentController | JS AJAX | static/js/ui-common.js (deleteDocumentById) | Active |
| GET | /documents/{id}/view | DocumentController | JS action/view | static/js/assets.js, static/js/fleet-page.js, static/js/properties-page.js | Active |
| GET | /documents/{id}/download | DocumentController | JS-rendered link | static/js/ui-common.js (documents table download link) | Active |

## Endpoints With No Current Internal Callers
These are not removed because they may be used by external tools, manual calls, or future UI integrations.

1. POST /fleet/update
- Declared in FleetController.
- No current template form or JS route reference.

2. POST /properties/update
- Declared in PropertiesController.
- No current template form or JS route reference.

## Already Removed in Prior Cleanup
1. Legacy dashboard JS block that called /api/assets/fleet/* with no matching controller route.
2. Legacy document endpoint /documents/upload (superseded by /documents/add).
3. Redundant properties route aliases /properties/properties/{id} and /properties/properties/update.

## Maintenance Rule
Before deleting any endpoint:
1. Confirm no template action, link, or JS caller.
2. Confirm no integration consumer (scripts, API tests, third-party callers).
3. If uncertain, deprecate first and log usage before removal.
