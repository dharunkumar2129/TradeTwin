# TradeTwin REST & SSE API Documentation

Base URL (Local Default): `http://localhost:8080/api/v1`  
*(Controllers also mount unversioned aliases under `/api/*` where shown in `@RequestMapping`.)*

All JSON responses follow the canonical `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "Descriptive status message",
  "data": {}
}
```

Errors return `ErrorResponse` with HTTP status `400`, `401`, `403`, `404`, `409`, `422` (`BusinessRuleException`), or `500`, and may include a sanitized `details` array (`List<String>`) for actionable diagnostics (such as PayPal Orders v2 failure stage, issue code, description, and `debug_id`):

```json
{
  "success": false,
  "message": "PayPal rejected order capture (UNPROCESSABLE_ENTITY)",
  "error": "PayPal rejected order capture (UNPROCESSABLE_ENTITY)",
  "details": [
    "Stage: CAPTURE_ORDER",
    "HTTP Status: 422 UNPROCESSABLE_ENTITY",
    "Issue: INSTRUMENT_DECLINED",
    "Debug ID: abc123"
  ],
  "timestamp": "2026-11-12T12:00:00Z",
  "path": "/api/v1/payments/paypal/capture-order"
}
```

---

## 1. Health & System

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/health` | Public | Returns system status, timestamp, and configured Gemini model ID (`geminiModel`). |

---

## 2. Authentication (`auth`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Registers a `ROLE_BUYER`, `ROLE_SELLER`, or `ROLE_ADMIN` account with BCrypt password hashing. Returns `201 Created` + JWT `AuthResponse`. |
| `POST` | `/api/v1/auth/login` | Public | Validates credentials and returns JWT Bearer token + user summary. |
| `GET` | `/api/v1/auth/me` | Authenticated | Returns the signed-in user's full `UserProfileDto`. |

---

## 3. User Account & Preferences (`user`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/users/profile` | Authenticated | Fetches authenticated user's profile, shipping address, and shopping preferences. |
| `PUT` | `/api/v1/users/profile` | Authenticated | Updates full name, default currency, shipping address, and shopping preferences. |
| `GET` | `/api/v1/users/{id}` | Admin or Owner | Fetches sanitized user record (`passwordHash` nulled). |

---

## 4. Seller Onboarding & Dashboard (`seller`)

Requires `ROLE_SELLER` or `ROLE_ADMIN`.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/sellers/onboard` | Creates or completes merchant `SellerProfile` (store name, description, logo, contact info, shipping regions, shipping terms, negotiation strategy). Returns `201 Created`. |
| `GET` | `/api/v1/sellers/me` | Returns authenticated merchant's `SellerProfile` (creates a default draft profile if missing). |
| `GET` | `/api/v1/sellers/profile` | Alias returning authenticated merchant's `SellerProfile`. |
| `PUT` | `/api/v1/sellers/profile` | Updates store metadata, contact details, and default strategy. |
| `GET` | `/api/v1/sellers/dashboard-summary` | Computes live inventory counts, active negotiations, accepted deals, order counts, sales revenue, and activity feed. |

---

## 5. Catalog & Seller Product Management (`catalog`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/catalog` | Public | Lists active public products (`ProductPublicDto`), optionally filtered via `?search=`. **Private negotiation floors are stripped.** |
| `GET` | `/api/v1/catalog/{id}` | Public | Fetches a single public product by ID (`ProductPublicDto`). Alias: `/api/v1/catalog/products/{id}`. |
| `POST` | `/api/v1/catalog/search` | Public | Executes deterministic multi-attribute catalog search against stock, budget, brand, warranty, and delivery preferences. |
| `POST` | `/api/v1/catalog` | `SELLER`, `ADMIN` | Creates a product listing including private `NegotiationPolicy` (`201 Created`). |
| `PUT` | `/api/v1/catalog/{id}` | `SELLER`, `ADMIN` | Updates product details and pricing/policy (enforces seller ownership). |
| `DELETE` | `/api/v1/catalog/{id}` | `SELLER`, `ADMIN` | Deletes a seller's product listing. |
| `PATCH` | `/api/v1/catalog/{id}/status?active=true` | `SELLER`, `ADMIN` | Enables or pauses a product listing. |
| `PATCH` | `/api/v1/catalog/{id}/stock?count=10` | `SELLER`, `ADMIN` | Sets available inventory count directly. |
| `GET` | `/api/v1/catalog/seller/my-products` | `SELLER`, `ADMIN` | Returns full `Product` records (including private policies) owned by the authenticated seller. |
| `GET` | `/api/v1/catalog/seller/products/{id}` | `SELLER`, `ADMIN` | Fetches a single owned product with full policy details. |
| `PUT` | `/api/v1/catalog/seller/products/{id}/negotiation-policy` | `SELLER`, `ADMIN` | Updates only the product's private AI negotiation policy. |

---

## 6. Buyer Natural-Language Intent (`intent`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/intents/parse` | Public / Optional JWT | Parses natural-language shopping query (`{ "query": "..." }`) via Gemini API (with deterministic fallback), saves draft `BuyerIntent`, and returns ranked matching candidates. |
| `POST` | `/api/v1/intents` | Public / Optional JWT | Confirms and persists validated `BuyerIntent` (`201 Created`). |
| `GET` | `/api/v1/intents` | Public / Optional JWT | Returns confirmed/draft intents for the signed-in buyer (or `[]` if unauthenticated). |
| `GET` | `/api/v1/intents/{id}` | Public / Owner | Retrieves `BuyerIntent`; masks private `maxBudget` and `targetBudget` unless requested by the owning buyer. |

---

## 7. Multi-Agent Negotiation & FairDeal SSE (`negotiation` / `deal` / `event`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/negotiations/start` | Authenticated | Initiates multi-agent negotiation with ordered `candidateProductIds`, optional `buyerQuery`, `intentId`, `quantity`, `minWarrantyMonths`, `requiredBrand`, and `preferredBrand`, launching asynchronous negotiation + automatic seller fallback (`201 Created`). |
| `GET` | `/api/v1/negotiations/{id}` | Public / Owner | Returns `Negotiation` session (including `sellerQueue`, `failedSellerAttempts`, `alternatives`, `noDealSummary`, and structured `Turn.proposal`); masks `buyerPreferences.maxCeilingPrice` unless caller is the owning buyer or `ROLE_ADMIN`. |
| `GET` | `/api/v1/negotiations/{id}/stream` | Public (`text/event-stream`) | Opens Server-Sent Events subscription streaming real-time agent updates without leaking buyer/seller reserve thresholds. |
| `GET` | `/api/v1/negotiations/{id}/events` | Public | Returns persisted `NegotiationEventDocument` timeline from MongoDB. |
| `GET` | `/api/v1/negotiations/{id}/compare-offers` | Public | Runs `FairDealEngine.compareOffers(...)` across participating seller quotes and returns transparent hard-filter + MAUT breakdown results. |
| `POST` | `/api/v1/negotiations/{id}/select-deal` | Authenticated (Buyer) | Buyer explicitly selects a competing offer ID/product ID and seals the 10-minute `AgreementSeal` token & stock lock. |
| `GET` | `/api/v1/negotiations/buyer/my-negotiations` | Authenticated | Lists all negotiations initiated by the signed-in buyer. |

### SSE Stream Event Types

- `NEGOTIATION_STARTED`: Sanitized session initialization metadata (`negotiationId`, `productId`, `status`, `maxRounds`)
- `AGENT_THINKING`: Status update indicator during agent/FairDeal computation
- `SELLERS_DISCOVERED`: Array of public merchant listings (`ProductPublicDto`)
- `SELLER_AGENT_STARTED`: Competing seller agent activation notice
- `SELLER_INITIAL_OFFER`: Round 1 merchant quote (`NegotiationOffer`)
- `BUYER_COUNTEROFFER`: Buyer agent counteroffer turn
- `SELLER_COUNTEROFFER`: Merchant agent counteroffer turn
- `OFFER_PROPOSED`: Standardized turn update broadcast
- `FAIRDEAL_METRIC`: Updated transparent `FairDealMetrics` breakdown
- `SELLER_ACCEPTED` / `SELLER_REJECTED` / `REJECTED`
- `SELLER_FALLBACK_TRIGGERED`: Emitted when the active seller fails to reach agreement or loses stock before switching candidates
- `NEXT_SELLER_STARTED` / `SELLER_TRANSITION`: Emitted when orchestrator transitions to the next eligible seller in `sellerQueue`
- `NO_DEAL_EXHAUSTED`: Emitted when all eligible candidate sellers have been exhausted without reserving stock (`alternatives` populated)
- `DEAL_SELECTED` / `AGREED`: Sealed agreement (`agreementId`, `dealToken`) + 10-minute stock reservation TTL (`reservationExpiresAt`)
- `NEGOTIATION_COMPLETED` / `ORDER_STATUS_UPDATED`: Post-settlement order events

---

## 8. PayPal Orders v2 Checkout (`payment`)

> **Single Sandbox Merchant MVP Scope**: PayPal Orders v2 checkout settles payment through the configured TradeTwin Sandbox Merchant application (`PAYPAL_CLIENT_ID` / `PAYPAL_CLIENT_SECRET`). Multi-seller marketplace split payouts via PayPal Commerce Platform are deferred to a future production phase.

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/payments/paypal/create-order` | Authenticated | Validates 10-minute `AgreementSeal`, `dealToken`, inventory (re-reserving stock cleanly on active agreement retry), 2-decimal `BigDecimal` `item_total + shipping` breakdown, and ceiling guardrail; creates PayPal order via OAuth2 Bearer + `PayPal-Request-Id` idempotency, or returns sanitized PayPal 422 diagnostics on error (never masked with simulated success when live keys are active). |
| `POST` | `/api/v1/payments/paypal/capture-order` | Authenticated | Inspects PayPal order status first (`CREATED`/`PAYER_ACTION_REQUIRED` -> diagnostic `ORDER_NOT_APPROVED`, `VOIDED` rejected, already `COMPLETED` recovered idempotently), captures approved order, handles `ORDER_ALREADY_CAPTURED` / 422 declines with `PaymentRecord` diagnostics + stock release, verifies captured currency/amount, deducts stock, and issues `Order`. |
| `POST` | `/api/v1/payments/paypal/cancel-order` | Authenticated | Records `CANCELLED` `PaymentRecord` audit state and releases the agreement's reserved stock without marking the order paid. |
| `POST` | `/api/v1/payments/paypal/fail-order` | Authenticated | Records `DECLINED` or `FAILED` `PaymentRecord` diagnostics (`failureStage`, `paypalIssue`, `debugId`) and releases reserved stock so buyer can safely retry before TTL expiry. |
| `GET` | `/api/v1/payments/paypal/orders/{paypalOrderId}` | Authenticated | Queries live PayPal order details via OAuth2 Bearer header (or returns stored record in sandbox simulation). |
| `GET` | `/api/v1/payments/execution-status/{negotiationId}` | Authenticated (Buyer) | Returns deterministic `MandateValidationResult`, `paymentExecutionMode`, `preparedPaypalOrderId`, and `DelegatedCapabilityStatus` (`PayPalVaultDelegatedPaymentAdapter`). |
| `POST` | `/api/v1/payments/delegated/execute` | Authenticated (Buyer) | Executes `DELEGATED_AUTONOMOUS_PAYMENT` exclusively when `tradetwin.paypal.delegated-vault-enabled=true`, valid non-OAuth Vault `payment_token` is supplied, and Spend Mandate constraints pass; refuses simulation and reports `DELEGATED_ACCESS_UNAVAILABLE` otherwise. |
| `GET` | `/api/v1/payments/audit/my-logs` | Authenticated (Buyer) | Returns buyer `PaymentAuditEntry` records containing mandate snapshot, negotiated amount, seller, budget compliance, auth method, PayPal Order ID, Capture ID, and final status. |
| `GET` | `/api/v1/payments/{id}` | Authenticated | Fetches `PaymentRecord` by record ID, `paypalOrderId`, or `negotiationId`. |
| `GET` | `/api/v1/payments/negotiation/{negotiationId}` | Authenticated | Retrieves payment record associated with a negotiation. |
| `POST` | `/api/v1/payments/paypal/webhook` | Public (Signature Verified) | Verifies PayPal webhook signature headers and acknowledges asynchronous events. |

---

## 8b. Spend Mandates (`mandate`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/mandates/me` | Authenticated (Buyer) | Lists buyer Spend Mandates with live daily spend (`spentToday`), remaining daily cap, masked delegated Vault token, and automatic expiry status updates. |
| `POST` | `/api/v1/mandates` | Authenticated (Buyer) | Creates and activates a `SpendMandate` (per-tx max, daily limit, currency, categories, brands, max qty, approved sellers, expiry, `SUPERVISED_PAYPAL_CHECKOUT` vs `DELEGATED_AUTONOMOUS_PAYMENT`). Rejects ordinary OAuth `A21AA...`/`Bearer` tokens in delegated Vault fields. (`201 Created`). |
| `PUT` | `/api/v1/mandates/{id}` | Authenticated (Owner) | Updates an existing buyer Spend Mandate with Bean Validation (`@Valid`) and `BigDecimal` cap checks. |
| `POST` | `/api/v1/mandates/{id}/revoke` | Authenticated (Owner) | Marks the specified Spend Mandate as `REVOKED`. |

---

## 9. Commerce Order Management & Fulfilment (`order`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/orders/{id}` | Authenticated (Owner/Seller/Admin) | Returns full order details, PayPal payment audit metadata, shipping address, and status history. |
| `GET` | `/api/v1/orders/my-orders` | Authenticated | Lists all orders placed by the signed-in buyer. |
| `GET` | `/api/v1/orders/buyer/summary` | Authenticated | Returns aggregated `BuyerDashboardSummaryDto` (`totalOrders`, `totalSpent`, active negotiations, fulfilment breakdown). |
| `GET` | `/api/v1/orders/seller/my-orders` | `SELLER`, `ADMIN` | Lists incoming orders belonging to the signed-in merchant store. |
| `PUT` | `/api/v1/orders/{id}/status` | `SELLER`, `ADMIN` | Advances order state through `OrderStateMachine` (`PROCESSING`, `SHIPPED` with carrier/tracking, `DELIVERED`, `CANCELLED`, `REFUNDED`). |
