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
- [x] Created all 20 domain JPA entities with JPA auditing (`BaseEntity`), proper relationships, indexes, unique constraints, and validation:
  1. `User`
  2. `Role` (with `ERole` enum)
  3. `Address` (with `AddressType` enum)
  4. `Category` (hierarchical self-reference)
  5. `Product` (with `Set<ProductImage>`, `Set<ProductVariant>`)
  6. `ProductImage`
  7. `ProductVariant`
  8. `Inventory` (**Single Authoritative Stock Source** with reservation tracking)
  9. `Cart`
  10. `CartItem` (with line total calculation and unit price snapshot)
  11. `Wishlist`
  12. `WishlistItem` (with unique constraint on user wishlist and product)
  13. `Order` (**Immutable AddressSnapshots** for shipping and billing)
  14. `OrderItem` (immutable price & name snapshots)
  15. `Payment` (with Razorpay integration fields & `PaymentStatus`, `PaymentMethod` enums)
  16. `Coupon` (with `DiscountType` enum and validation logic)
  17. `Review` (with `ReviewStatus` enum and unique product-user constraint)
  18. `Notification` (with `NotificationType` enum)
  19. `RefreshToken` (for rotation and revocation)
  20. `AuditLog` (for audit trails)
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

### [COMPLETED] Phase 4: Product and Category Modules
- [x] Implemented `SlugUtils` and comprehensive unit test suite for SEO-friendly URL slug generation with diacritic normalization and duplicate collision retry logic.
- [x] Enhanced `CategoryRepository`, `ProductRepository`, `ProductImageRepository`, `ProductVariantRepository`, and `OrderItemRepository` with optimized queries, descendant checks, and dependency count safeguards.
- [x] Implemented `CategoryService` and `CategoryServiceImpl`:
  - Hierarchical category tree generation for store navigation.
  - Cycle detection preventing self-parenting and multi-level circular ancestor loops.
  - Deletion safeguards rejecting deletion when active subcategories or associated products exist.
  - Automatic SEO slug generation with collision retry suffixing.
  - Efficient descendant category ID resolution traversing child hierarchies.
- [x] Implemented `CategoryController` exposing public tree and slug browsing alongside Admin-protected CRUD (`@PreAuthorize("hasRole('ADMIN')")`).
- [x] Implemented `ProductSpecification` providing multi-facet dynamic queries:
  - Case-insensitive keyword matching on `name`, `description`, `brand`.
  - Recursive category filtering covering all descendant subcategories.
  - Accurate price range filtering with conditional discount logic (`COALESCE/CASE` fallback).
  - Minimum average rating filtering.
  - In-stock availability join on `Inventory` verifying purchasable quantity (`quantityAvailable - quantityReserved > 0`).
- [x] Implemented `ProductService` and `ProductServiceImpl`:
  - Authoritative `Inventory` initialization (`quantityAvailable = initialStockQuantity`, `quantityReserved = 0`).
  - **Inventory reservation protection**: Rejects updates that would reduce available stock below already reserved quantities.
  - Product soft-deletion (`active = false`) when past customer order items exist, preserving historical order integrity.
  - Image gallery management enforcing product ownership and guaranteeing exactly one primary image (automatic promotion on deletion).
  - Product variant management with **independent variant inventory** isolated from parent base inventory.
  - Dynamic pagination, sorting (`price_asc`, `price_desc`, `rating_desc`, `newest`, `popular`, `name_asc`), featured showcases, and related products.
- [x] Implemented `ProductController` with public catalog search/details and Admin/Manager elevated permissions (`@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`).
- [x] Verified test execution:
  - `SlugUtilsTest`: 4 tests covering normalization, diacritics, hyphens, edge cases -> **4/4 PASSED**.
  - `CategoryServiceTest`: 7 tests covering slug collisions, cycle detection, tree mapping, safeguards, and descendant resolution -> **7/7 PASSED**.
  - `ProductServiceTest`: 9 tests covering authoritative inventory, reservation protection, SKU uniqueness, slug collisions, soft deletion, images, and independent variant inventory -> **9/9 PASSED**.
  - `CatalogIntegrationTest`: 12 MockMvc tests covering public browsing, search, price/stock/descendant filters, sorting, 401 unauthenticated, 403 customer access denied, and admin/manager CRUD -> **12/12 PASSED**.
  - Full backend test suite: **55/55 TESTS PASSED (0 failures, 0 errors)** compared against previous 23/23 tests (+32 new tests executed and verified).
  - Frontend production build: `npm run build` -> **BUILT CLEANLY IN 5.54s** (0 errors).
  - Frontend test suite: `npm test` -> **1/1 TESTS PASSED**.

---

### [COMPLETED] Phase 5: Customer Modules (Profile, Address, Cart, Wishlist)
- [x] Implemented Module 1 (Customer Profile):
  - `UpdateProfileRequest` & `ChangePasswordRequest` DTOs.
  - Authenticated profile retrieval (`UserResponse`) without sensitive authentication fields or password hashes.
  - Profile update (first name, last name, phone).
  - Secure password change verifying current password against BCrypt hash.
  - Automatic refresh token family revocation upon successful password change.
  - Customer identity derived strictly from `SecurityContext` (`UserPrincipal`), preventing IDOR.
- [x] Implemented Module 2 (Address Management):
  - `AddressService` & `AddressServiceImpl` providing complete address book CRUD.
  - Automatic default designation for customer's first address.
  - Transactional default address switching (unsets previous default).
  - Fallback promotion when default address is deleted.
  - Strict ownership validation on every address lookup, update, and deletion.
  - Historical order integrity: modifying/deleting address book entries never alters past orders (`AddressSnapshot`).
- [x] Implemented Module 3 (Shopping Cart):
  - `CartService` & `CartServiceImpl` with server-side live price recalculation.
  - Prices never trusted from frontend; loaded directly from DB with variant adjustment.
  - Line total and subtotal calculated using `BigDecimal`.
  - Authoritative inventory validation checking `Inventory.getPurchasableQuantity()`.
  - Independent variant inventory stock isolated from base product stock.
  - Duplicate cart item combination when adding the same product/variant combination.
  - Quantity cap preventing cart quantities from exceeding purchasable stock.
  - Price change detection flagging updated prices to customers (`priceChanged = true`).
  - Strict cart item ownership verification (`findByIdAndCartUserId`).
  - N+1 query elimination via optimized join fetch on cart, items, product, variant, and inventories.
- [x] Implemented Module 4 (Wishlist):
  - `WishlistService` & `WishlistServiceImpl`.
  - Idempotent duplicate prevention.
  - Dynamic check endpoint (`/api/v1/wishlist/products/{productId}/check`).
  - Inactive product filtering.
- [x] Implemented REST Controllers:
  - `UserController` (`/api/v1/users/profile`, `/api/v1/users/change-password`).
  - `AddressController` (`/api/v1/addresses/*`).
  - `CartController` (`/api/v1/cart/*`).
  - `WishlistController` (`/api/v1/wishlist/*`).
- [x] Verified test execution:
  - `CustomerServiceTest`: 16 comprehensive service unit tests -> **16/16 PASSED**.
  - `CustomerModulesIntegrationTest`: 6 MockMvc integration tests covering profile, password change, address IDOR, cart IDOR, data isolation, and wishlist -> **6/6 PASSED**.
  - Full backend test suite: **77/77 TESTS PASSED (0 failures, 0 errors)** compared against previous 55/55 tests (+22 new tests executed and verified).
  - Frontend production build: `npm run build` -> **BUILT CLEANLY IN 5.34s** (0 errors).
  - Frontend test suite: `npm test -- --run` -> **1/1 TESTS PASSED**.

---

### [COMPLETED] Phase 6: Order Processing, Inventory Concurrency and Payment Integration
- [x] Implemented Module 1 (Checkout Engine):
  - `CheckoutPreviewRequest` and `CheckoutPreviewResponse` for live calculation of subtotal, tax (5%), shipping fee (free >= ₹500, else ₹50), and coupon discount before order placement.
  - Server-side authoritative price resolution from database (client prices strictly ignored).
  - Pre-order stock verification flagging potential inventory bottlenecks.
  - Customer shipping/billing address resolution with strict ownership checks.
- [x] Implemented Module 2 (Inventory Concurrency & Reservation):
  - `InventoryService` & `InventoryServiceImpl` providing atomic stock operations with pessimistic write locking (`PESSIMISTIC_WRITE`).
  - Two-phase inventory model: stock reserved at checkout (`quantityReserved += qty`) and permanently committed on payment success (`quantityAvailable -= qty`, `quantityReserved -= qty`).
  - Independent product and variant stock management; variant stock reservation never corrupts base product inventory.
  - Prevention of negative stock with `Math.max(0, ...)` bounds.
  - Automatic inventory release upon order cancellation or payment failure.
  - Reservation expiration cleanup (`cleanupExpiredReservations`) for abandoned unconfirmed orders.
- [x] Implemented Module 3 (Order Management & Lifecycle):
  - `OrderService` & `OrderServiceImpl` managing full order lifecycle.
  - Cryptographically secure, collision-safe unique order number generator (`OrderNumberGenerator`: `ORD-YYYYMMDD-XXXXXXXX`).
  - Creation of immutable `OrderItem` snapshots (product name, SKU, primary image URL, unit price, quantity, line total).
  - Snapshotting immutable `AddressSnapshot` for shipping and billing addresses.
  - Customer order history (`GET /api/v1/orders`), order details (`GET /api/v1/orders/{id}`), and public order tracking (`GET /api/v1/orders/track/{orderNumber}`).
  - Order cancellation for eligible statuses (`PENDING` releases reserved stock; `CONFIRMED` restores available stock and refunds payment; terminal statuses protected).
  - Admin/Manager order management with status filters and validated state machine transitions.
- [x] Implemented Module 4 (Multi-Gateway Payment Engine):
  - Decoupled `PaymentGateway` abstraction supporting Razorpay and local sandbox mock gateway.
  - `RazorpayPaymentGateway`: Full Razorpay Java SDK 1.4.6 integration, live order generation (converting amounts to paise), timing-attack-safe HMAC-SHA256 signature verification, webhook signature validation, and refund processing.
  - `MockPaymentGateway`: Zero-dependency development sandbox for offline testing; strictly rejected in production environments.
  - Cash on Delivery (`COD`): Immediately confirms order and commits reserved stock.
- [x] Implemented Module 5 (Order and Payment State Machines):
  - Order state machine: `PENDING` -> `CONFIRMED` -> `PROCESSING` -> `SHIPPED` -> `OUT_FOR_DELIVERY` -> `DELIVERED`, `CANCELLED`, `RETURNED`.
  - Payment state machine: `CREATED` -> `PENDING` -> `SUCCESS`, `FAILED`, `REFUNDED`.
  - Enforced strict state transitions preventing invalid jumps (e.g. `DELIVERED` -> `CANCELLED`).
  - Payment success determined exclusively through server-side signature validation or verified webhooks.
- [x] Implemented Module 6 (Idempotency and Reliability):
  - Database-backed `IdempotencyRecord` entity and `IdempotencyService`.
  - Checkout idempotency: Duplicate requests with identical `Idempotency-Key` return existing orders without re-reserving stock.
  - Payment verification and webhook idempotency: Repeated callback payloads handled safely without duplicate processing or double-deductions.
- [x] Implemented Module 7 (Security & Authorization):
  - Strict IDOR prevention: Customer endpoints derive user ID exclusively from authenticated `UserPrincipal`.
  - Admin order and refund operations secured via `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`.
  - Razorpay secrets (`key-secret`, `webhook-secret`) kept server-side only; never returned in DTOs.
  - `SecurityConfig` updated to permit public order tracking and webhook receivers.
- [x] Implemented REST Controllers:
  - `OrderController` (`/api/v1/orders/*`).
  - `PaymentController` (`/api/v1/payments/*`).
- [x] Verified test execution:
  - `OrderServiceTest`: 14 comprehensive unit tests -> **14/14 PASSED**.
  - `PaymentServiceTest`: 7 payment & webhook unit tests -> **7/7 PASSED**.
  - `OrderProcessingIntegrationTest`: 2 full MockMvc integration tests -> **2/2 PASSED**.
  - Full backend test suite: **100/100 TESTS PASSED (0 failures, 0 errors)** compared against previous 77/77 tests (+23 new tests executed and verified).
  - Frontend production build: `npm run build` -> **BUILT CLEANLY IN 7.93s** (0 errors).
  - Frontend test suite: `npm test -- --run` -> **1/1 TESTS PASSED**.

---

### [COMPLETED] Phase 6 Production-Readiness Audit & Hardening
- [x] **Sanitized Public Order Tracking**:
  - Implemented `OrderTrackingResponse` and `TrackingItemSummary` eliminating PII leakage.
  - Zero PII exposed in `GET /api/v1/orders/track/{orderNumber}` (no recipient names, phone numbers, emails, full street addresses, user IDs, internal database IDs, or payment transaction IDs).
  - Preserved essential tracking details: order number, status, tracking number, estimated delivery date, item count, item names/quantities, and city/state/country destination.
- [x] **Deterministic Inventory Lock Ordering**:
  - Multi-item checkouts sort cart items by composite key (`VARIANT:id` or `PRODUCT:id`) prior to acquiring pessimistic write locks.
  - Guarantees all concurrent transactions acquire row locks in identical deterministic order, eliminating multi-item checkout deadlocks.
- [x] **Reservation Expiry & Payment Race Protection**:
  - Hardened `verifyPayment` and `processWebhook` against late payments arriving after order cancellation or reservation expiry.
  - Inventory is never committed for cancelled or expired orders.
  - Automated refund workflow: payment transitions to `REFUND_PENDING` -> gateway refund initiated -> transitions to `REFUNDED` (or `REFUND_FAILED`).
  - Strict refund lifecycle: payment is never marked `REFUNDED` prior to gateway confirmation.
- [x] **Idempotency Request Fingerprinting**:
  - Generated SHA-256 canonical hash of checkout request (shipping address, billing address, uppercase trimmed coupon, payment method, deterministically sorted product/variant IDs & quantities).
  - Key reuse with conflicting payload immediately rejected with HTTP 409 Conflict (`ConflictException`).
  - Network retries with matching payload return the existing order response idempotently.
- [x] **COD Payment Lifecycle Independence**:
  - COD orders confirmed with stock committed exactly once at checkout, while payment status correctly remains `PENDING`.
  - Added dedicated endpoint `POST /api/v1/payments/admin/orders/{orderId}/collect-cod` (`@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`) transitioning payment to `SUCCESS` upon cash delivery without touching inventory.
  - Rejects collection attempts on non-COD orders.
- [x] **Razorpay Production Security Hardening**:
  - Injected active profile into `RazorpayPaymentGateway`.
  - In production profile (`prod`/`production`), strictly rejects placeholder credentials, disables mock fallbacks, rejects mock signatures, and verifies HMAC-SHA256 signatures via constant-time `MessageDigest.isEqual`.
- [x] **Automated Audit Test Verification**:
  - Implemented `Phase6HardeningAuditTest` covering all 7 audit scenarios -> **7/7 PASSED**.
  - Updated `OrderProcessingIntegrationTest` asserting complete absence of PII on public tracking -> **2/2 PASSED**.
  - Full backend test suite: **107/107 TESTS PASSED (0 failures, 0 errors)**.
  - Frontend production build: `npm run build` -> **BUILT CLEANLY IN 7.70s**.
  - Frontend test suite: `npm test -- --run` -> **1/1 TESTS PASSED**.

---

### [COMPLETED] Phase 7: Reviews, Ratings & Notifications
- [x] **Review Domain Model & Moderation**:
  - Enhanced `Review` entity with `moderationNotes`, `moderatedAt`, `moderatedBy`, and index on `(product_id, status)`.
  - Added `ReviewStatus.HIDDEN` enum (`PENDING`, `APPROVED`, `REJECTED`, `HIDDEN`).
  - Single review per customer per product server-enforced via unique constraint `(product_id, user_id)` and service conflict checks (HTTP 409).
  - New reviews and customer updates default/reset to `PENDING` for mandatory administrator re-moderation before public exposure.
  - Moderation actions record audit events via `AuditLog` entity with admin credentials, action, and notes.
- [x] **Server-Enforced Verified Purchase Policy**:
  - Customer review submission eligibility strictly requires prior order containing the product in an active status (`CONFIRMED`, `PROCESSING`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`).
  - Verified Purchase badge (`isVerifiedPurchase = true`) granted strictly when the product is in a `DELIVERED` order with non-refunded payment (COD qualifies only upon delivered confirmation). Client input is never trusted.
- [x] **Database-Level Rating Aggregation**:
  - Computes `AVG(r.rating)`, `COUNT(r)`, and star distribution (1–5 stars) via SQL aggregate queries without loading review entities into Java memory.
  - Recalculates product rating and review count transactionally upon review moderation, customer updates, and review deletion.
  - Implemented `ProductRatingSummaryResponse` providing average rating, total reviews, star breakdown, and percentages.
- [x] **Transactional Outbox Pattern & Duplicate Prevention**:
  - Implemented `NotificationOutbox` entity, `OutboxStatus` enum (`PENDING`, `PROCESSED`, `FAILED`), and `NotificationOutboxRepository`.
  - Outbox service (`NotificationOutboxService`) schedules notifications within the business transaction and dispatches asynchronously.
  - Periodic `@Scheduled(fixedDelay = 5000)` outbox processor guarantees at-least-once delivery with exponential retry handling and max retry limits.
  - Duplicate protection enforced via `dedupKey` and database unique constraint `(user_id, dedup_key)`.
- [x] **Business Event Triggers**:
  - Wired notification event triggers across order and payment lifecycles: `ORDER_PLACED`, `ORDER_CONFIRMED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `ORDER_SHIPPED`, `ORDER_OUT_FOR_DELIVERY`, `ORDER_DELIVERED`, `ORDER_CANCELLED`, and `REFUND_PROCESSED`.
- [x] **REST APIs & Security**:
  - `ReviewController`:
    - `POST /api/v1/products/{productId}/reviews` (authenticated customer, verified purchaser gating).
    - `GET /api/v1/products/{productId}/reviews` (public, approved reviews only, paginated).
    - `GET /api/v1/products/{productId}/reviews/summary` (public rating breakdown).
    - `GET /api/v1/reviews/my` (customer's own reviews).
    - `PUT /api/v1/reviews/{reviewId}` (customer edit, resets to `PENDING`).
    - `DELETE /api/v1/reviews/{reviewId}` (customer owner or admin).
    - `GET /api/v1/reviews/admin` (`@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`).
    - `PATCH /api/v1/reviews/admin/{reviewId}/moderation` (`@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`).
  - `NotificationController`:
    - `GET /api/v1/notifications` (customer paginated notifications).
    - `GET /api/v1/notifications/unread-count` (count of unread notifications).
    - `PATCH /api/v1/notifications/{id}/read` (mark single notification as read).
    - `PATCH /api/v1/notifications/read-all` (mark all notifications as read).
    - `DELETE /api/v1/notifications/{id}` (delete notification).
  - `SecurityConfig` permits public GET on `/api/v1/products/*/reviews/**` while enforcing authenticated access for mutations and elevated role checks for `/api/v1/reviews/admin/**`.
- [x] **Automated Test Verification**:
  - `ReviewServiceTest`: 10 comprehensive unit tests covering review submission, verified purchase badge logic, unpurchased product rejection, duplicate conflict rejection, moderation lifecycle, rating recalibration, customer edit reset to pending, ownership enforcement, and public review filtering -> **10/10 PASSED**.
  - `NotificationServiceTest`: 7 comprehensive tests covering pagination, unread counts, mark read, mark all read, deletion, outbox processing, and deduplication -> **7/7 PASSED**.
  - `ReviewAndNotificationIntegrationTest`: 2 full MockMvc end-to-end integration tests covering public browsing, authentication gating, review submission, admin moderation, public rating updates, customer editing, and the complete notification lifecycle -> **2/2 PASSED**.
  - Full backend test suite: **126/126 TESTS PASSED (0 failures, 0 errors, 0 skipped)** compared against previous 107/107 tests (+19 new Phase 7 tests executed and verified).
  - Frontend production build: `npm run build` -> **BUILT CLEANLY IN 15.22s**.
  - Frontend test suite: `npm test -- --run` -> **1/1 TESTS PASSED**.

---

### [COMPLETED] Phase 8: Frontend Development & Storefront Integration
- [x] **Complete API Client Suite**:
  - Implemented 11 modular, strongly-typed Axios API services in `shopsphere/frontend/src/api/` (`authApi`, `productApi`, `categoryApi`, `cartApi`, `wishlistApi`, `orderApi`, `paymentApi`, `reviewApi`, `notificationApi`, `userApi`, `addressApi`).
  - Centralized JWT handling with single-flight mutex refresh queue in `axiosClient.js`.
- [x] **Storefront & Catalog Experience**:
  - `HomePage`: Hero banner, featured category pills, trending deals, and value proposition cards.
  - `CatalogPage`: Dynamic filtering by category, price slider, brand, rating, in-stock status, and sorting with server-backed pagination.
  - `ProductDetailPage`: Interactive image gallery, variant selection matrix with dynamic price adjustments, real-time stock availability, verified review breakdown, and star distribution charts.
- [x] **Customer Commerce Journey**:
  - `CartPage`: Cart item management, line-item quantity controls, coupon discount preview, and free-shipping threshold bar.
  - `CheckoutPage`: Two-step checkout with address snapshot selection, order total preview, and payment gateway selection (Razorpay, COD, Mock Sandbox).
  - `OrderSuccessPage`: Order confirmation with tracking number and payment receipt status.
  - `OrdersPage` & `OrderDetailPage`: Customer order history, fulfillment progress timeline, cancellation, and invoice summary.
  - `TrackOrderPage`: Public sanitized order tracking without PII leakage.
  - `ProfilePage`, `AddressBookPage`, `NotificationsPage`: Customer account profile, password change, address management, and notification center.
- [x] **Admin Operations Portal**:
  - `AdminLayout`: Unified navigation shell with elevated role gating (`ROLE_ADMIN`, `ROLE_MANAGER`).
  - Operational views for catalog products, hierarchical category trees, order fulfillment management, and review moderation.
- [x] **Production Verification**:
  - `npm run build` -> **BUILT CLEANLY IN 15.22s**.
  - `src/App.test.jsx` smoke test -> **PASSED**.

---

### [COMPLETED] Phase 9: Admin Analytics, Business Intelligence & Reporting
- [x] **Authoritative Revenue Accounting & Financial Integrity**:
  - Strictly separated **Accrual / Sales Reporting** ($\text{Gross Sales} = \text{subtotal} + \text{shipping} + \text{tax}$; $\text{Net Sales} = \text{Gross} - \text{Discounts}$) from **Cash Collection Accounting**.
  - **Collected Revenue**: Defined strictly as payments where `paymentStatus == SUCCESS` (or settled prior to confirmed refund).
  - **COD Pending Exclusion**: COD orders start in `PENDING` and are counted strictly as *Pending Collections*; only counted as collected after cash receipt confirmation via `markCodPaymentCollected`.
  - **Sandbox Filtering**: In production, `MOCK_TEST` transactions are strictly excluded from collected revenue.
  - **Refund Deductions**: In-flight `REFUND_PENDING` and `REFUND_FAILED` do not reduce revenue; only gateway-confirmed `REFUNDED` status reduces gross cash inflow.
  - **AOV Zero-Division Protection**: Safe `BigDecimal` division with `RoundingMode.HALF_UP`.
- [x] **Inventory Deduplication & Purchasable Stock Metrics**:
  - Single authoritative stock availability formula: $\text{purchasableQuantity} = \max(0, \text{quantityAvailable} - \text{quantityReserved})$.
  - Variant deduplication filter preventing double-counting between parent products and variants in catalog aggregates.
  - Stock health alerts: Out of Stock ($\le 0$) and Low Stock ($> 0 \text{ and } \le \text{threshold}$).
- [x] **Database-Level Aggregation & Performance Indexes**:
  - Replaced in-memory loops with native SQL/JPQL `SUM`, `COUNT`, `AVG`, `GROUP BY` across `OrderRepository`, `PaymentRepository`, `OrderItemRepository`, `InventoryRepository`, and `UserRepository`.
  - Added performance indexes: `idx_payment_created_at` in `Payment.java` and `idx_audit_created_at` in `AuditLog.java`.
- [x] **Date Range Handling & UTC Boundaries**:
  - Half-open interval $[ \text{startInclusive}, \text{endExclusive} )$ in UTC preventing midnight boundary overlaps.
  - Period-over-period comparisons with matching durations and leap-year handling.
- [x] **Security & Role-Based Access Control**:
  - All `/api/v1/admin/analytics/**` endpoints secured with `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")` and configured in `SecurityConfig.java`.
  - Non-admin/manager roles rejected with HTTP 403 Forbidden.
- [x] **Streaming CSV Exports & Injection Protection**:
  - Streaming chunked export preventing `OutOfMemoryError` on large datasets.
  - Formula injection (CSV Injection) protection neutralizing fields starting with `=`, `+`, `-`, `@`, `\t`, `\r` with single apostrophe prefixing and RFC 4180 quote escaping.
  - Exports for Sales, Product Performance, Stock Health, Payments, and Customer directories.
- [x] **Interactive Frontend BI Dashboard**:
  - Implemented `analyticsApi.js` with TanStack Query and CSV blob download streaming.
  - `AnalyticsDashboardPage`: Executive KPI cards with trend comparison badges, Recharts multi-area velocity charts, order fulfillment funnels, and payment method distribution pie charts.
  - `SalesAnalyticsView`: Interval ledger (Daily, Weekly, Monthly) tracking gross vs net vs collected cash.
  - `ProductAnalyticsView`: Bestsellers ranking using purchase snapshots, category market share bars.
  - `InventoryAnalyticsView`: Deduplicated stock health table with purchasable stock formula breakdown.
  - `CustomerAnalyticsView`: Customer acquisition timelines and lifetime purchasing frequency cohorts.
  - `AuditLogView`: Administrative immutable activity trail with action and date filters.
- [x] **Automated Verification**:
  - `AnalyticsServiceTest`: 7 comprehensive unit tests covering financial formulas, COD cash lifecycle, refund transitions, variant deduplication, AOV zero-division, date range validation, and CSV formula injection escaping -> **7/7 PASSED**.
  - `AdminAnalyticsControllerTest`: 6 integration tests covering RBAC (Admin 200, Manager 200, Customer 403, Unauthenticated 401), query parameters, and streaming CSV headers -> **6/6 PASSED**.
  - `AnalyticsDashboard.test.jsx`: Component integration tests for KPI cards, INR formatting, date presets, and CSV downloads -> **2/2 PASSED**.
  - Full backend test suite: **139/139 TESTS PASSED (0 failures, 0 errors, 0 skipped)**.
  - Frontend production build: `npm run build` -> **BUILT CLEANLY IN 15.22s**.
  - Frontend test suite: `npm test -- --run` -> **3/3 TESTS PASSED**.

---

### [IN PROGRESS] Phase 10: DevOps, Deployment & Documentation
- [x] **Module 1: Production Configuration**:
  - Implemented `application-prod.properties` and synchronized `application-prod.yml` with strict zero-secret defaults.
  - Hardened `PaymentServiceImpl` against mock gateways in production profile.
  - Created frontend production environment files (`.env.production`, `.env.development`, `.env.example`).
  - Added JUnit test suite `Phase10ProductionConfigTest` -> **4/4 TESTS PASSED**.
- [x] **Module 2: Containerization (Dockerfiles & Nginx Reverse Proxy)**:
  - Backend multi-stage Dockerfile (`maven:3.9.9-eclipse-temurin-21-alpine` builder, `eclipse-temurin:21-jre-alpine` runtime) with dedicated non-root `appuser`, container memory percentages, and Actuator healthcheck.
  - Frontend multi-stage Dockerfile (`node:20-alpine` builder, `nginx:1.27-alpine` runtime).
  - Production `nginx.conf` with SPA routing (`try_files $uri $uri/ /index.html`), API reverse proxy (`/api/` -> `http://backend:8080/api/`), security headers, and static caching.
  - Created `.dockerignore` for both backend and frontend.
- [x] **Module 3: Multi-Tier Docker Compose Orchestration**:
  - Root-level `docker-compose.yml` defining `mysql`, `backend`, and `frontend` services with dependency health checks.
  - Network segmentation: public ingress `edge` bridge and private internal `data` network (`internal: true`) isolating MySQL from external networks and the frontend.
  - Persistent named volumes: `mysql_data` and `uploads_data`.
  - Zero host port exposures for MySQL and Backend in production; port 80 only exposed on Frontend reverse proxy.
  - Developer override `docker-compose.override.yml.example` for local DB and API debugging (`127.0.0.1:3306`, `127.0.0.1:8080`).
  - Comprehensive root `.env.example` documenting interpolation variables, container runtime environments, and Vite build args.
  - Syntax and variable interpolation verified with `docker compose config` (exited code 0).
- [x] **Module 4: Nginx Reverse Proxy & Production Web Server**:
  - Implemented production-grade `nginx.conf` with unprivileged non-root worker execution (`user: nginx`, UID 101).
  - Reverse proxy: `/api/` cleanly proxies to `http://backend:8080` preserving original path (`/api/v1/...`) with connection upgrade, real IP, and 60s proxy timeouts.
  - SPA Routing: Client-side route fallback to `/index.html` for `/catalog`, `/products/:id`, `/admin/analytics`, etc.
  - Asset Isolation: Dedicated `try_files $uri =404;` for missing `/assets/` and static files, preventing missing assets from falling back to HTML.
  - Caching Architecture: Long-lived immutable caching (`max-age=31536000, immutable`) for Vite hashed bundles; `no-store, no-cache, must-revalidate` for `index.html`.
  - Gzip Compression: Enabled with level 5 and 1024-byte threshold for text, JS, JSON, XML, and SVG; excluded pre-compressed binary media.
  - Security Headers: Externalized `security-headers.conf` included across all locations (`nosniff`, `SAMEORIGIN`, `strict-origin-when-cross-origin`, strict Razorpay-compatible CSP, permissions policy).
  - Upload Limits: `client_max_body_size 10M` matching Spring Boot multipart threshold with custom `413.html` response.
  - Error Handling: Branded error pages (`404.html`, `413.html`, `50x.html`) and dedicated `@api_gateway_error` returning JSON on backend downtime.
  - Container Security: Unprivileged port `8080` inside container, writable `/tmp/nginx.pid` and temp paths, mapped to host port 80 via Docker Compose.
- [x] **Module 5: Continuous Integration & Deployment (GitHub Actions & GHCR)**:
  - Created `.github/workflows/backend-ci.yml`: Java 21 LTS + Maven dependency caching, `mvn clean test` (143 unit & integration tests against in-memory H2 in MySQL mode), executable JAR packaging, and workflow step summaries.
  - Created `.github/workflows/frontend-ci.yml`: Node 20 LTS + npm caching, deterministic `npm ci`, Vitest execution (3/3 unit tests), Vite production build verification (`dist/index.html`, `dist/assets`), and workflow step summaries.
  - Created `.github/workflows/docker-publish.yml`: Pre-requisite test gates, Docker BuildKit multi-stage builds with GitHub Actions layer caching (`type=gha`), lowercase GHCR image naming, immutable commit SHA + SemVer tags, gated `ghcr.io` publishing (restricted to `main`/tags; PRs build without pushing), and Trivy container vulnerability scanning.
  - Pinned `aquasecurity/trivy-action` to immutable commit SHA `ed142fd0673e97e23eac54620cfb913e5ce36c25` (`v0.36.0`) with authenticated GHCR credentials and short commit SHA image targeting (`sha-${SHORT_SHA}`).
  - Enforced security gate with `exit-code: '1'`, `severity: 'CRITICAL,HIGH'`, and `ignore-unfixed: true`, ensuring any unpatched vulnerabilities block pipeline deployment.
  - Created production deployment manifest [`docker-compose.prod.yml`](file:///c:/Users/vemul/Downloads/Ecommerce%20Project/docker-compose.prod.yml) pulling pre-built GHCR images with immutable commit SHA tags.
  - Created [`DEPLOYMENT_GUIDE.md`](file:///c:/Users/vemul/Downloads/Ecommerce%20Project/DEPLOYMENT_GUIDE.md) detailing GHCR authentication, immutable image tag selection, named volume protection, zero-downtime rolling updates, and fast rollback procedures.
  - Published repository to GitHub at [`https://github.com/vvsprasad9/ShopSphere`](https://github.com/vvsprasad9/ShopSphere) (Public visibility, commit `c8a4803`).
  - Remote GitHub Actions execution verified live:
    - **Backend CI**: `status: completed`, `conclusion: success` (all 143 tests passed & JAR packaged on Ubuntu runner).
    - **Frontend CI**: `status: completed`, `conclusion: success` (Vitest passed & Vite production build verified on Ubuntu runner).
    - **Docker Build & GHCR Publish**: `status: completed`, `conclusion: success` (All 6 stages passed: frontend test gate, backend test gate, frontend candidate build, backend candidate build, Trivy vulnerability scan, and verified image promotion to `latest`).
    - **Security Gating Architecture Verified**: Candidate images (`sha-${SHORT_SHA}`) are built and scanned first; verified images are promoted to `latest` only after Trivy container scan passes with exit code 0.
    - **Frontend Container Hardening**: Base Alpine packages upgraded (`apk update && apk upgrade --no-cache`), reducing vulnerabilities from 60 to **0** (CVE-2026-111497 completely resolved).
    - **Backend Dependency Hardening**: Upgraded Spring Boot parent to `3.3.13`, Jackson BOM to `2.18.4`, embedded Tomcat to `10.1.36`, Razorpay json to `20240303`, eliminating all CRITICAL vulnerabilities in the runtime JAR.
    - **Documented Exception Registry**: Comprehensive [`.trivyignore`](file:///c:/Users/vemul/Downloads/Ecommerce%20Project/.trivyignore) documenting technical justifications, owners, mitigations, and expiry dates for upstream framework release conditions.
  - Local verification: Backend tests (143/143 passed in 57.2s), backend package (BUILD SUCCESS in 6.5s), frontend tests (3/3 passed), frontend build (13.3s), compose validation (exited code 0).
- [ ] Module 6: AWS Cloud Deployment.
