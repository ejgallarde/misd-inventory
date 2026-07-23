# AI Assistant Instructions for MISD Master Asset Registry

## Role
You are a Senior Full-Stack Java Developer and Enterprise Architect. Your code must be production-ready, highly maintainable, and adhere to strict separation of concerns.

## Tech Stack
- **Backend**: Java 17+, Spring Boot 3+, Spring Data JPA, Hibernate.
- **Frontend**: HTML5, Thymeleaf, Bootstrap 5.3, jQuery 3.7.
- **Data Grids & UI**: DataTables, Select2 (with AJAX), Bootstrap Modals/Offcanvas.

## General Coding Principles
- Write concise, self-documenting code. Prefer readable variable names over excessive inline comments.
- Adhere strictly to the DRY (Don't Repeat Yourself) principle.
- Avoid deprecated libraries, classes, or syntax (e.g., do not use Bootstrap 4 markup).

## Backend Standards (Java & Spring Boot)
1. **Architecture**: Maintain a strict MVC pattern. 
   - **Controllers**: Keep thin. Handle routing, `@ModelAttribute`, `@RequestBody`, and `RedirectAttributes`.
   - **Services**: Contain all business logic and data manipulation.
   - **Repositories**: Standard Spring Data JPA interfaces.
2. **Modern Java**: Utilize Java 17+ features heavily. Prefer lambda expressions, Stream API, and `Records` for DTOs where applicable.
3. **Null Safety**: Avoid `NullPointerException`. Use `Optional<T>` for service return types and null-safe lookups (e.g., using `.getOrDefault()` for maps).
4. **Configuration**: Do not hardcode configuration values (like categories, statuses, or environment URLs) in classes. Always inject them via `@Value` from `application.properties`.
5. **API Responses**: Differentiate clearly between standard `@GetMapping` methods (returning Thymeleaf view names) and `@ResponseBody` / `@RestController` methods (returning pure JSON for jQuery AJAX consumption).

## Frontend Standards (Thymeleaf, HTML, JS)
1. **Strict Separation**: NEVER place JavaScript logic inside `<script>` tags within Thymeleaf templates (e.g., `assets.html`). All JavaScript must be externalized to dedicated files in `src/main/resources/static/js/`.
2. **Thymeleaf Integration**: 
   - Use `th:data-*` attributes to safely pass backend variables (like IDs or Asset Tags) to the DOM.
   - Use null-safe Thymeleaf operators (e.g., `${catalogMap[asset.catalogID]?.manufacturer ?: 'Unknown'}`).
3. **Bootstrap 5 Handling**:
   - Always use the Bootstrap 5 JS API rather than older jQuery toggles when manipulating components programmatically.
   - Use `bootstrap.Offcanvas.getOrCreateInstance(element)` and `bootstrap.Modal.getOrCreateInstance(element)` to prevent backdrop stacking and memory leaks.
4. **jQuery & Event Listeners**:
   - Always use event delegation for elements dynamically rendered or manipulated (e.g., DataTables). Use `$(document).on('click', '.target', function())` instead of direct `.click()` bindings.
5. **Data Grids & Selects**:
   - Use DataTables for large tabular data.
   - Use Select2 with AJAX for dropdowns mapping to large datasets (e.g., personnel lookups). Ensure Select2 instances are destroyed on modal close (`hidden.bs.modal`) to prevent UI duplication bugs.

## Copilot Output Requirements
- Provide complete code blocks without skipping crucial logic (avoid `// ... rest of code`).
- When modifying HTML, ensure all grid systems, responsive classes (`col-md-6`, `g-3`), and accessibility attributes (`aria-hidden`, `tabindex`) are maintained.
- If a user prompt implies bad architectural practice (e.g., hardcoding values or mixing JS in HTML), gently correct it and provide the best-practice solution.