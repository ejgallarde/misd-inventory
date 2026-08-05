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

- `spring.datasource.url` in `application.properties` points at `jdbc:mysql://localhost:3306/misd_inventory` with `spring.jpa.hibernate.ddl-auto=none` — schema must already exist (see `src/main/resources/db/*.sql` for PSGC schema/fix scripts and manual DDL additions). **`MisdInventoryDashboardApplicationTests` (`@SpringBootTest`) boots the full context and will fail without a reachable MySQL instance matching this config** — every other test avoids Spring entirely (see Testing below).
- Document storage defaults to the local filesystem (`storage.mode=filesystem`, root at `storage.filesystem.root-path`). Set `storage.mode=minio` to switch to the MinIO/S3 client wired in `config/MinioConfig` (only activates via `@ConditionalOnProperty` when that mode is selected).
- `app.security.demo-mode=true` by default: `SecurityConfig` provisions a single in-memory user (`app.security.demo-user-email`, password `change-me`) instead of real auth. Set `app.security.oauth2.enabled=true` to layer on Entra/OAuth2 login instead.

## Architecture

### Package layout
`ph.gov.phlpost.inventory.misddashboard` — flat by responsibility, not by domain: `controller/`, `service/`, `repository/`, `model/`, `config/`, `util/`. Each of the three asset domains (IT/Fleet/Properties) gets one controller and (for IT and Fleet) one service; there is **no `PropertiesService`** — `PropertiesController` talks directly to `RealEstatePropertyRepository` and `AuditLogService`, which is an intentional-but-inconsistent asymmetry versus the other two modules (flagged, not yet fixed).

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
- Each module has its own page script (`assets.js`, `fleet-page.js`, `properties-page.js`) plus a Thymeleaf template (`assets.html`, `fleet.html`, `properties.html`) that is loaded both standalone *and* embedded as a tab inside `dashboard.html`. Fleet and Properties share one JS file for both contexts (gated by presence checks like `$('#fleetTable').length > 0`); IT Assets instead uses two separate files (`assets.js` for the standalone page, `asset-detail.js` for the dashboard tab) that independently reimplement the same detail-slideout workflow — a known duplication, not yet unified.
- `fragments/` holds reusable Thymeleaf fragments (`asset-table`, `fleet-detail`, `properties-history`, etc.). When embedding a fragment that defines its own root attributes via `th:fragment`, the calling `<div th:replace="~{fragments/x :: y}">` must be left **empty** — Thymeleaf replaces the whole host element including any children, so anything placed inside it never renders (this bug existed in `assets.html`/`fleet.html` and was fixed; `properties.html` was already correct).
- Per `.github/copilot-instructions.md`: no inline `<script>` logic in templates — everything JS lives under `static/js/`; use the Bootstrap 5 JS API (`bootstrap.Offcanvas.getOrCreateInstance(...)`) rather than jQuery show/hide for modals/offcanvases to avoid backdrop-stacking bugs; use event delegation for anything inside a DataTable; destroy Select2 instances on `hidden.bs.modal`.

### Key naming/schema quirks (do not "fix" opportunistically)
- `FleetVehicle.adminLegaltionalStatus` (and its DB column `AdminLegaltionalStatus`) has a persistent typo baked into the schema and every native query in `FleetVehicleRepository` — renaming requires a coordinated DB migration.
- Dropdown option lists in `application-dropdowns.properties` don't always match the literal values checked in native `@Query` strings (e.g. `property-tax-status-add`/`-update` list "Paid 2026"/"Paid (Current Year)" but `RealEstatePropertyRepository` checks for `'Unpaid'`; `fleet-operational-statuses` lists "Missing"/"Stolen" separately but queries check the combined `'Missing/Stolen'`). These are pre-existing bugs, not something to silently correct as part of unrelated changes.
- Entity ID strategy is mixed: `Asset`/`Personnel`/PSGC entities use natural/business-key `String` `@Id`s; `FleetVehicle`/`RealEstateProperty`/`Document`/audit-log entities use `IDENTITY` `Integer`. Follow whichever convention the entity you're touching already uses.
- `AssetAssignmentLog` and `LifecycleAuditLog` are near-duplicate audit tables (both written via `AuditLogService`) rather than one generic audit entity — this is deliberate legacy structure, not an oversight to merge casually.

### Endpoint reference
`docs/endpoint-ownership-matrix.md` tracks every controller route, which template/JS calls it, and whether it has any internal caller at all. **Check it before removing or renaming an endpoint** — it also records what's already been cleaned up (e.g. the old `/documents/upload` and `/properties/properties/*` alias routes) so that work isn't redone or reversed.

## Testing conventions

- Controller and service tests use plain `@ExtendWith(MockitoExtension.class)` with `@Mock`/`@InjectMocks` and call methods directly — there is no `@WebMvcTest`/`MockMvc` usage in this codebase; don't introduce it without checking whether it fits the existing pattern.
- `LoginControllerTest` shows the simplest pattern: construct the controller directly with constructor args, no mocking framework needed for simple cases.
- Only `MisdInventoryDashboardApplicationTests` touches the real Spring context (see Local setup above for its DB requirement).
