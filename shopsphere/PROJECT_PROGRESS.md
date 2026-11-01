# ShopSphere - Project Progress Tracker

Tracking the end-to-end development phases of **ShopSphere**, a full-stack production-grade e-commerce application.

---

## Architectural Refinements Incorporated
- [x] **Java 21 LTS Target**: Standardized on Java 21 across Maven configuration, compiler flags, and container runtimes.
- [x] **Inventory Authoritative Source**: Product catalog delegates stock availability and transactional reservations strictly to the `Inventory` entity and service to prevent overselling.
- [x] **Order Address Snapshots**: `Order` stores immutable address snapshots (recipient name, phone, full address, city, state, postal code, country) rather than mutable references so past orders are never corrupted if a customer alters their address book.
- [x] **Consistent Role Design**: Role model using `Role` entity backed by `ERole` enum (`ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER`) with join tables and method-level security.
- [x] **JWT Security**: Access tokens (24h) + cryptographic refresh token rotation (7 days) with database-backed revocation.
- [x] **Zero Hardcoded Secrets**: Secrets configurable via `.env` / environment variables with safe defaults for local development.
- [x] **Separated Application Profiles**: `dev` for local development, `prod` for Docker / cloud deployments, and `test` using in-memory H2.
- [x] **Explicitly Pinned Dependencies**: Compatible Spring Boot 3.3.4, Spring Security 6, JJWT 0.12.6, Springdoc OpenAPI 2.6.0, React 18.3, Vite 5.4, and Tailwind CSS 3.4.

---

## Phase Execution Checklist

### [COMPLETED] Phase 1: Project Initialization & Build Setup
- [x] Define root project structure (`shopsphere/backend`, `shopsphere/frontend`).
- [x] Initialize backend Maven project (`pom.xml`, Java 21, Spring Boot 3.3.4, Web, Data JPA, Security, Validation, Actuator, Mail, JJWT, OpenAPI, Razorpay SDK, Lombok).
- [x] Setup Spring Boot multi-profile configuration (`application.yml`, `application-dev.yml`, `application-prod.yml`, `application-test.yml`).
- [x] Create core backend entrypoint (`ShopSphereApplication.java`) and `/api/v1/health` endpoint.
- [x] Initialize frontend Vite + React 18 project (`package.json`, Tailwind CSS, Lucide React, Axios, TanStack Query, Zustand, Recharts, Zod, React Hook Form).
- [x] Configure Vite dev server with `/api` proxy and Tailwind styling.
- [x] Implement initial frontend entrypoint and health status dashboard.
- [x] Run backend compilation (`mvn clean compile`) -> **BUILD SUCCESS** (0 errors).
- [x] Run backend test suite (`mvn test`) -> **2/2 TESTS PASSED** (`HealthControllerTest`, `ShopSphereApplicationTests`).
- [x] Run frontend dependency install (`npm install`) -> **335 packages audited** (exit code 0).
- [x] Run frontend production build (`npm run build`) -> **BUILT IN 6.65s** (dist artifacts generated).
- [x] Run frontend test suite (`npm run test`) -> **1/1 TESTS PASSED** with Vitest and testing-library.

---

### [COMPLETED] Phase 2: Database and Core Architecture
- [x] Created all 20 domain JPA entities with JPA auditing (`BaseEntity`), proper relationships, indexes, unique constraints, and validation.
- [x] Created all 20 Spring Data JPA repositories with `JpaSpecificationExecutor` and optimized queries.
- [x] Implemented centralized exception handling using `@RestControllerAdvice` (`GlobalExceptionHandler`, `ResourceNotFoundException`, `BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `InsufficientStockException`, `PaymentException`, `ErrorResponse`).
- [x] Implemented decoupled request and response DTOs for every module (strictly no entities exposed).
- [x] Verified database relationships and inventory reservation logic with automated JUnit 5 tests.

---

### [COMPLETED] Phase 3: Authentication, Security and JWT
- [x] Implemented Spring Security 6 stateless filter chain (`SecurityConfig.java`) with custom entry point and access denied handlers.
- [x] Configured `CustomUserDetailsService` loading user credentials, account status, and role-based authorities.
- [x] Implemented `BCryptPasswordEncoder` bean and wired into `AuthenticationManager`.
- [x] Implemented `JwtTokenProvider` using JJWT 0.12.6 with HS256 signature verification, claims extraction, expiration checks, and cryptographically secure random token generator.
- [x] Implemented `JwtAuthenticationFilter` with `Bearer ` token extraction and SecurityContext population.
- [x] Implemented `AuthService` and `AuthServiceImpl` covering:
  - Registration with server-enforced `ROLE_CUSTOMER` (client input rejected) and automated Cart/Wishlist provisioning.
  - Login via `AuthenticationManager` returning access token + rotated refresh token.
  - **Refresh token rotation & reuse detection**: Detects compromised tokens, triggers whole-family revocation, and invalidates attacker sessions.
  - Logout with token revocation and cookie clearing.
  - Secure forgot-password and reset-password with expiry validation and forced session revocation.
  - Email verification workflow.
- [x] Created `AuthController` exposing all 8 authentication endpoints with HttpOnly cookie support for refresh tokens.
- [x] Implemented `DataInitializer` bootstrapping initial roles (`ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER`) and super admin credentials from environment variables (`app.admin.*`).
- [x] Created `AdminTestController` with `@PreAuthorize` method-level authorization.
- [x] Verified security and auth test suites:
  - `AuthControllerIntegrationTest`: 7 tests covering registration, duplicate email, invalid password, login, wrong password, password reset, and email verification -> **7/7 PASSED**.
  - `JwtAndAuthorizationSecurityTest`: 9 tests covering missing token (401), invalid signature (401), expired token (401), token refresh, token rotation, revoked token, token reuse detection, customer accessing admin endpoint (403), admin accessing admin endpoint (200), and `/me` data isolation -> **9/9 PASSED**.
  - Full backend test suite: **23/23 TESTS PASSED (0 failures, 0 errors)**.

---

### [NEXT] Phase 4: Product and Category Modules
- [ ] Category hierarchy CRUD with safety checks and cycle prevention.
- [ ] Product CRUD with Spring Data JPA Specification dynamic filtering (multi-criteria).
- [ ] Dynamic search by name, description, brand, category, price range, and in-stock status.
- [ ] Sorting options (`price_asc`, `price_desc`, `rating_desc`, `newest`).
- [ ] Product image gallery management and primary image selection.
- [ ] Product variant management (SKU, price adjustment, attribute json).
- [ ] Unit and MockMvc integration tests for catalog endpoints.
