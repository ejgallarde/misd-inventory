# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

MISD Master Asset Registry — a Spring Boot 4.1 / Java 17 monolith (Thymeleaf + Bootstrap 5 + jQuery/DataTables/Select2) for PHLPost's MISD division to track three asset domains: **IT equipment**, **fleet vehicles**, and **real estate properties** (land + buildings/facilities), plus a document-attachment system shared across all three.

## Commands

Maven wrapper is present (`mvnw` / `mvnw.cmd`) — prefer it over a globally installed `mvn` unless one is already configured.

```
# Build
./mvnw clean package
mvnw.cmd clean package          # Windows

# Run the app locally (needs MySQL at localhost:3306/misd_inventory — see Local setup below)
./mvnw spring-boot:run

# Run the full test suite
./mvnw test

# Run a single test class
./mvnw test -Dtest=FleetControllerTest

# Run a single test method
./mvnw test -Dtest=FleetControllerTest#getVehicleDetailsReturnsNotFoundForUnknownId
```

There is no separate lint/format command configured in the pom.

### Local setup

- **Credentials are not in git.** `application.properties` references `${MISD_DB_USERNAME}`, `${MISD_DB_PASSWORD}`, `${MISD_MINIO_ACCESS_KEY}`, `${MISD_MINIO_SECRET_KEY}`, `${MISD_STORAGE_ROOT}` and `${MISD_DEMO_USER_PASSWORD}` with **no defaults** — the app deliberately refuses to start rather than fall back to a shared development password. Supply them either as environment variables or by copying `src/main/resources/application-local.properties.example` to `application-local.properties` (gitignored, imported via `spring.config.import`).
- `spring.datasource.url` points at `jdbc:mysql://localhost:3306/misd_inventory` with `spring.jpa.hibernate.ddl-auto=none` — schema must already exist. `src/main/resources/db/README.md` explains what is in that folder; `misd_inventory_*.sql` are reference dumps and `migration_*.sql` are dated corrective scripts. **`MisdInventoryDashboardApplicationTests` (`@SpringBootTest`) boots the full context and will fail without a reachable MySQL instance matching this config** — every other test avoids Spring entirely (see Testing below).
- Document storage defaults to the local filesystem (`storage.mode=filesystem`, root at `${MISD_STORAGE_ROOT}`). Set `storage.mode=minio` to switch to the MinIO/S3 client wired in `config/MinioConfig` (only activates via `@ConditionalOnProperty` when that mode is selected).
- `app.security.demo-mode=true` by default: `SecurityConfig` provisions a single in-memory user (`app.security.demo-user-email`, password from `${MISD_DEMO_USER_PASSWORD}`) instead of real auth. Set `app.security.oauth2.enabled=true` to layer on Entra/OAuth2 login instead.

## Architecture

### Package layout
`ph.gov.phlpost.inventory.misddashboard` — flat by responsibility, not by domain: `controller/`, `service/`, `repository/`, `model/`, `config/`, `util/`. Each of the three asset domains (IT/Fleet/Properties) gets one controller and (for IT and Fleet) one service; there is **no `PropertiesService`** — `PropertiesController` talks directly to `RealEstatePropertyRepository` and `AuditLogService`, which is an intentional-but-inconsistent asymmetry versus the other two modules (flagged, not yet fixed).

### Blank input, error handling, and audit keys
`config/WebBindingAdvice` registers a global `StringTrimmerEditor(true)`, so every **form-bound** blank field arrives as `null`, not `""`. This matters because several optional columns are `UNIQUE` (`Assets.SerialNumber`, `FleetVehicles.PlateNumber`/`BodyNumber`/`EngineNumber`/`ChassisNumberVIN`, `RealEstateProperties.TitleNumber`/`TaxDeclarationNumber`) and MySQL allows many `NULL`s in a unique index but only one `''`. **JSON request bodies bypass this** — Jackson never touches a `WebDataBinder` — so `/assets/update` normalizes blanks itself in `normalizeBlankOptionalFields`, and the browser side does the same via `MISDCommon.serializeFormToPayload`. Keep both halves in mind when adding an endpoint that takes `@RequestBody`.

`config/GlobalExceptionHandler` turns constraint violations, binding failures and oversized uploads into a flash message (form posts) or a JSON `{"error": ...}` body (AJAX and `ResponseEntity` handlers), falling back to `templates/error.html`. Controllers still catch `IllegalArgumentException` themselves for expected validation failures.

Audit rows are keyed on **immutable primary keys**: `FleetService.auditReferenceId` returns `VEHICLE-{id}` and `PropertiesController.resolveReferenceId` returns `PROP-{id}`. They previously used plate and title numbers, which can be filled in or changed later and orphaned earlier history. IT assets key on the asset tag, which is the real primary key.

### Reference-data fan-out
`MainDashboardController` (renders the tabbed `dashboard.html` hub) and each of `ITAssetController`/`FleetController`/`PropertiesController` (the standalone per-module pages) independently populate an overlapping set of model attributes: `employeeMap`, `catalogMap`, `departmentMap`, `divisionMap`, `personnelLocationMap`, `managerNameMap`, and the document-upload config (max size, allowed extensions, per-module category lists sourced from `document.upload.categories.*` in `application.properties`). All of these ultimately come from `RegistryService`, whose lookup methods are `@Cacheable` (employee/catalog/department/division/location/manager maps) — treat that cache as effectively static per app run; there's no eviction wired for personnel changes.

### Thymeleaf helper beans
`AssetWorkflowHelper` (`@assetHelper`) and `FleetWorkflowHelper` (`@fleetHelper`) are `@Component`s invoked directly from templates (`${@assetHelper.deploymentBadgeClass(...)}`) to keep status-badge CSS/tooltip logic and lifecycle-action-visibility predicates out of the HTML. When adding a new status value or lifecycle action, the corresponding badge/tooltip/predicate method usually needs updating in these classes, not in the template.

### Document storage
`DocumentService` validates uploads (size/extension/category-per-reference-type, configured via `document.upload.*` properties) and delegates actual byte storage to `DocumentStorageService`, which switches between filesystem and MinIO based on `storage.mode`. Reference IDs are resolved per domain (`resolveVehicleStorageId`/`resolvePropertyStorageId` prefer plate number / title number over the numeric PK) before building the storage path.

### PSGC location cascade
`LocationImportService` parses PSGC (Philippine Standard Geographic Code) CSVs into `PsgcProvince`/`PsgcCityMunicipality`/`PsgcBarangay`, and `LocationLookupService` + `LocationController`/`LocationAdminController` expose cascading province→city→barangay dropdowns, mirrored on the frontend by `static/js/location-cascade.js`. `db/psgc_province_city_fixes_3_ascii.sql` is a one-off data-correction script for mis-imported names — not part of normal migrations.

### Frontend structure
- `static/js/ui-common.js` exposes a single `window.MISDCommon` object (IIFE) — shared DataTable config, document-upload wiring, currency/date formatters, toast helpers, straight-line depreciation calculator, etc. Check here before adding a new per-page helper; several formatters were consolidated into this file to remove cross-page duplication.
- Each module has its own page script (`assets.js`, `fleet-page.js`, `properties-page.js`) plus a Thymeleaf template (`assets.html`, `fleet.html`, `properties.html`) that is loaded both standalone *and* embedded as a tab inside `dashboard.html`. Fleet and Properties share one JS file for both contexts (gated by presence checks like `$('#fleetTable').length > 0`); IT Assets splits differently: `asset-detail.js` owns the detail slideout for **both** the standalone page and the dashboard tab, and `assets.js` owns only the table, filters, history modal and action modals. `assets.html` loads `asset-detail.js` first so `window.MISDAssetDetail` exists before `assets.js` sets `onBeforeSave` and calls `open()` for the `openAsset` deep link. (These were duplicate implementations until 2026-09-01; they had drifted so that only one filled in the Current Valuation field.)
- `fragments/` holds reusable Thymeleaf fragments (`asset-table`, `fleet-detail`, `properties-history`, etc.). When embedding a fragment that defines its own root attributes via `th:fragment`, the calling `<div th:replace="~{fragments/x :: y}">` must be left **empty** — Thymeleaf replaces the whole host element including any children, so anything placed inside it never renders (this bug existed in `assets.html`/`fleet.html` and was fixed; `properties.html` was already correct).
- Per `.github/copilot-instructions.md`: no inline `<script>` logic in templates — everything JS lives under `static/js/`; use the Bootstrap 5 JS API (`bootstrap.Offcanvas.getOrCreateInstance(...)`) rather than jQuery show/hide for modals/offcanvases to avoid backdrop-stacking bugs; use event delegation for anything inside a DataTable; destroy Select2 instances on `hidden.bs.modal`.

### Key naming/schema quirks (do not "fix" opportunistically)
- `FleetVehicle.adminLegaltionalStatus` (and its DB column `AdminLegaltionalStatus`) has a persistent typo baked into the schema and every native query in `FleetVehicleRepository` — renaming requires a coordinated DB migration.
- Dropdown option lists in `application-dropdowns.properties` must match the literal values in native `@Query` strings. Two mismatches were corrected on 2026-09-01: the split `property-tax-status-add`/`-update` lists are now one `dropdown.property-tax-statuses`, and the fleet problem queries now accept `'Missing'` and `'Stolen'` (the values actually written) alongside the legacy `'Missing/Stolen'`. `RealEstatePropertyRepository` still checks for a `'Unpaid'` tax status that no dropdown offers — verify against the dropdown before adding another such query.
- `FleetVehicles.Cost` is `DECIMAL(15,2)` and the entity field is `BigDecimal`. It was a Java `String` until 2026-09-01; MySQL runs in `STRICT_TRANS_TABLES`, so grouped input like `1,500,000.00` is a hard error. `MISDCommon.normalizeDecimalInput` flattens it client-side.
- Entity ID strategy is mixed: `Asset`/`Personnel`/PSGC entities use natural/business-key `String` `@Id`s; `FleetVehicle`/`RealEstateProperty`/`Document`/audit-log entities use `IDENTITY` `Integer`. Follow whichever convention the entity you're touching already uses.
- `AssetAssignmentLog` and `LifecycleAuditLog` are near-duplicate audit tables (both written via `AuditLogService`) rather than one generic audit entity — this is deliberate legacy structure, not an oversight to merge casually.

### Endpoint reference
`docs/endpoint-ownership-matrix.md` tracks every controller route, which template/JS calls it, and whether it has any internal caller at all. **Check it before removing or renaming an endpoint** — it also records what's already been cleaned up (e.g. the old `/documents/upload` and `/properties/properties/*` alias routes) so that work isn't redone or reversed.

## Testing conventions

- Controller and service tests use plain `@ExtendWith(MockitoExtension.class)` with `@Mock`/`@InjectMocks` and call methods directly — there is no `@WebMvcTest`/`MockMvc` usage in this codebase; don't introduce it without checking whether it fits the existing pattern.
- `LoginControllerTest` shows the simplest pattern: construct the controller directly with constructor args, no mocking framework needed for simple cases.
- Only `MisdInventoryDashboardApplicationTests` touches the real Spring context (see Local setup above for its DB requirement).
