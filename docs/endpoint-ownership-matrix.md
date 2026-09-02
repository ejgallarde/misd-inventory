# Endpoint Ownership Matrix

Last updated: 2026-09-01
Purpose: Track controller endpoint ownership and internal callers (template forms, links, and JS AJAX) before any future cleanup.

## Scope
- Included: Endpoints declared in controllers under src/main/java/ph/gov/phlpost/inventory/misddashboard/controller.
- Included: Internal callers from Thymeleaf templates and static JS.
- Excluded: Framework-managed routes (for example Spring Security's POST /login and /logout, and /error), unless noted.

48 mappings across 8 controllers. 46 have a live internal caller.

## Main dashboard

| Method | Route | Owning Controller | Internal Caller Type | Internal Callers | Status |
|---|---|---|---|---|---|
| GET | / | MainDashboardController | Link / redirect target | templates/assets.html, fleet.html, properties.html, location-admin.html, error.html ("Back to Dashboard"); redirect target of every registration form | Active |
| GET | /api/personnel/search | MainDashboardController | JS AJAX | static/js/ui-common.js (Select2 AJAX source) | Active |

## IT assets

| Method | Route | Owning Controller | Internal Caller Type | Internal Callers | Status |
|---|---|---|---|---|---|
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
| GET | /assets/{id} | ITAssetController | JS AJAX | static/js/asset-detail.js (shared detail offcanvas) | Active |
| GET | /api/assets/{assetTag}/history | ITAssetController | JS AJAX | static/js/assets.js (View Asset History modal) | Active |
| POST | /assets/update | ITAssetController | JS AJAX | static/js/asset-detail.js (save edit) | Active |

## Fleet

| Method | Route | Owning Controller | Internal Caller Type | Internal Callers | Status |
|---|---|---|---|---|---|
| GET | /fleet | FleetController | Link | templates/dashboard.html (Fleet card href) | Active |
| POST | /fleet/add | FleetController | Form submit | templates/dashboard.html (Register Vehicle form) | Active |
| POST | /fleet/assign | FleetController | Form submit | fragments/fleet-table.html → templates/fleet.html (Assign modal) | Active |
| POST | /fleet/return | FleetController | Form submit | fragments/fleet-table.html → templates/fleet.html (Return modal) | Active |
| POST | /fleet/retire | FleetController | Form submit | fragments/fleet-table.html → templates/fleet.html (Retire modal) | Active |
| POST | /fleet/{vehicleID}/under-maintenance | FleetController | Form submit | fragments/fleet-table.html (Actions dropdown) | Active |
| POST | /fleet/{vehicleID}/impound | FleetController | Form submit | fragments/fleet-table.html (Actions dropdown) | Active |
| POST | /fleet/{vehicleID}/ber | FleetController | Form submit | fragments/fleet-table.html (Actions dropdown) | Active |
| POST | /fleet/{vehicleID}/stolen | FleetController | Form submit | fragments/fleet-table.html (Actions dropdown) | Active |
| POST | /fleet/{vehicleID}/missing | FleetController | Form submit | fragments/fleet-table.html (Actions dropdown) | Active |
| GET | /fleet/{id} | FleetController | JS AJAX | static/js/fleet-page.js (fleet.html and the dashboard tab). Read-only. | Active |
| GET | /fleet/{id}/history | FleetController | JS AJAX | static/js/fleet-page.js (View Vehicle History modal) | Active |
| POST | /fleet/update | FleetController | JS AJAX | static/js/fleet-page.js (detail panel save) | Active |

## Properties

| Method | Route | Owning Controller | Internal Caller Type | Internal Callers | Status |
|---|---|---|---|---|---|
| GET | /properties | PropertiesController | Link | templates/dashboard.html (Land and Buildings card hrefs) | Active |
| POST | /properties/add | PropertiesController | Form submit | templates/dashboard.html (Add Property form, both registration contexts) | Active |
| POST | /properties/assign-custodian | PropertiesController | Form submit | templates/properties.html (Custodian modal) | Active |
| POST | /properties/update-tax | PropertiesController | Form submit | templates/properties.html (Tax modal) | Active |
| GET | /properties/{id} | PropertiesController | JS AJAX | static/js/properties-page.js (property detail load) | Active |
| GET | /properties/{id}/history | PropertiesController | JS AJAX | static/js/properties-page.js (View Property History modal) | Active |
| POST | /properties/update | PropertiesController | JS AJAX | static/js/properties-page.js (detail panel save) | Active |

## Documents

| Method | Route | Owning Controller | Internal Caller Type | Internal Callers | Status |
|---|---|---|---|---|---|
| GET | /documents/list | DocumentController | JS AJAX | static/js/ui-common.js (loadDocumentsForReference) | Active |
| POST | /documents/add | DocumentController | JS AJAX | static/js/asset-detail.js, fleet-page.js, properties-page.js | Active |
| DELETE | /documents/{id} | DocumentController | JS AJAX | static/js/ui-common.js (deleteDocumentById) | Active |
| GET | /documents/{id}/view | DocumentController | JS action | static/js/ui-common.js (print), asset-detail.js, fleet-page.js, properties-page.js | Active |
| GET | /documents/{id}/download | DocumentController | JS-rendered link | static/js/ui-common.js (documents table download link) | Active |

## Locations and authentication

| Method | Route | Owning Controller | Internal Caller Type | Internal Callers | Status |
|---|---|---|---|---|---|
| GET | /login | LoginController | Browser route | Spring Security login redirect; templates/login.html | Active |
| GET | /admin/locations | LocationAdminController | Link | fragments/components.html (user dropdown → Manage PSGC Data) | Active |
| POST | /admin/locations/import-psgc | LocationAdminController | Form submit | templates/location-admin.html (PSGC upload form) | Active |
| GET | /api/locations/provinces | LocationController | JS AJAX | static/js/location-cascade.js | Active |
| GET | /api/locations/cities | LocationController | JS AJAX | static/js/location-cascade.js | Active |
| GET | /api/locations/barangays | LocationController | JS AJAX | static/js/location-cascade.js | Active |
| POST | /api/locations/import/csv | LocationController | None | No template or JS reference | Keep (see below) |
| POST | /api/locations/import/psgc-single | LocationController | None | No template or JS reference | Keep (see below) |

## Endpoints With No Current Internal Callers

1. `POST /api/locations/import/csv` — three-file PSGC import.
2. `POST /api/locations/import/psgc-single` — single-file PSGC import, duplicated by the `POST /admin/locations/import-psgc` form route the admin page actually uses.

Both are REST variants of an import the UI performs through `LocationAdminController`. They are referenced in the comments of `db/psgc-schema.sql` as the documented import API, so they may have external callers. Not removed.

## Changed in the 2026-09-01 review

1. `POST /fleet/{vehicleID}/return-to-motorpool` — **removed**. It duplicated `POST /fleet/return` without capturing condition notes. The Actions dropdown now opens the Return modal, which was previously defined in `fleet.html` with no trigger anywhere.
2. `GET /fleet/{id}` — **behaviour change, same route**. It called `reviewAndUpdateVehicleStatus`, which wrote to the database on a read. It is now strictly read-only and returns `isFullyDepreciated` and `isRegistrationExpired` as derived display flags.
3. `POST /fleet/update` and `POST /properties/update` — now take the authenticated user and write a lifecycle audit entry, matching the modal-driven actions.
4. A `Dispose / Retire` trigger was added to the fleet Actions dropdown for the existing `POST /fleet/retire`; the modal existed but nothing opened it, so retiring a vehicle was not possible from the interface.
5. The IT detail slideout is now served by `static/js/asset-detail.js` on both `/assets` and the dashboard tab; the duplicate implementation in `assets.js` was removed.

## Already Removed in Prior Cleanup
1. Legacy dashboard JS block that called /api/assets/fleet/* with no matching controller route.
2. Legacy document endpoint /documents/upload (superseded by /documents/add).
3. Redundant properties route aliases /properties/properties/{id} and /properties/properties/update.

## Maintenance Rule
Before deleting any endpoint:
1. Confirm no template action, link, or JS caller.
2. Confirm no integration consumer (scripts, API tests, third-party callers).
3. If uncertain, deprecate first and log usage before removal.

Regenerate the route list with:

```
grep -rn "@\(Get\|Post\|Put\|Delete\|Patch\)Mapping" src/main/java/ph/gov/phlpost/inventory/misddashboard/controller/
```
