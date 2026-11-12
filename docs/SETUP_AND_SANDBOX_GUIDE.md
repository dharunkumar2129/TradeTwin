# TradeTwin Local Setup & Sandbox Configuration Guide

This guide walks through setting up a clean local TradeTwin environment, configuring MongoDB, Google Gemini AI, and PayPal Sandbox credentials, and signing in with pre-seeded demo accounts.

---

## 1. Prerequisites

- **Java 21+** (Maven Wrapper `mvnw` / `mvnw.cmd` is included under `backend/`)
- **Node.js 18+** and **npm 9+**
- **MongoDB 7.0** (either via Docker Compose or native local `mongod` / MongoDB Atlas)

---

## 2. Quick Start (Fresh Local Setup)

### Step 1: Start MongoDB
From the repository root:

```bash
docker compose up -d
```

Or run a native local `mongod` instance listening on `mongodb://localhost:27017/tradetwin`.

### Step 2: Configure Environment Variables
Copy the root environment template:

```bash
cp .env.example .env
```

Review and populate keys in `.env` (or pass as shell environment variables before starting the backend):

| Variable | Default / Required | Description |
|---|---|---|
| `PORT` | `8080` | Spring Boot backend HTTP port |
| `FRONTEND_URL` | `http://localhost:5173` | Allowed CORS origin for Vite SPA |
| `MONGODB_URI` | `mongodb://localhost:27017/tradetwin` | MongoDB connection string |
| `JWT_SECRET` | Dev fallback configured | Base64 / UTF-8 HS256 secret (use >= 32 chars in production) |
| `GEMINI_API_KEY` | Optional in dev | Google AI Studio API key; omits live LLM calls and activates deterministic fallback if blank or placeholder |
| `GEMINI_MODEL` | `gemini-3.5-flash-lite` | Fast JSON-capable Gemini model (with timeout & automatic cooldown fallback) |
| `PAYPAL_CLIENT_ID` | Optional in dev | PayPal Sandbox REST App Client ID |
| `PAYPAL_CLIENT_SECRET` | Optional in dev | PayPal Sandbox REST App Client Secret |
| `PAYPAL_BASE_URL` | `https://api-m.sandbox.paypal.com` | PayPal API base URL |
| `PAYPAL_CURRENCY` | `USD` | Settlement ISO currency code |
| `PAYPAL_DELEGATED_VAULT_ENABLED` | `false` | Enables official PayPal Vault v3 / Reference Transactions adapter only when merchant account has Vault enabled; when `false`, delegated requests strictly return `DELEGATED_ACCESS_UNAVAILABLE` without simulation |

Optional frontend overrides in `frontend/.env.local`:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_PAYPAL_CLIENT_ID=your_paypal_sandbox_client_id
```

> If `VITE_PAYPAL_CLIENT_ID` and `PAYPAL_CLIENT_ID` are omitted or left as placeholders, `CheckoutPage` cleanly transitions to **Verified Sandbox Simulation Mode**, executing the exact same backend order validation, server-side pricing recalculation, inventory deduction, and `PaymentRecord` persistence pipeline.

### Step 3: Start the Spring Boot Backend
On Windows PowerShell:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:
```bash
cd backend
./mvnw spring-boot:run
```

On startup against an empty database, [`DataInitializer`](../backend/src/main/java/com/tradetwin/common/config/DataInitializer.java) seeds users, seller profiles, multi-seller catalog listings, a sample completed negotiation (`neg-seed-01`), and a confirmed order (`ORD-2026-9812`).

### Step 4: Start the React + Vite Frontend
```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

---

## 3. Setting Up Google Gemini API (Runtime AI)

1. Visit [Google AI Studio](https://aistudio.google.com/) and create an API key.
2. Export `GEMINI_API_KEY=<your-key>` and `GEMINI_MODEL=gemini-3.8-flash`.
3. Restart the backend and verify `GET http://localhost:8080/api/v1/health` returns `"geminiModel": "gemini-3.8-flash"`.

---

## 4. Setting Up PayPal Sandbox (Orders API v2 + JS SDK)

1. Log in to the [PayPal Developer Dashboard](https://developer.paypal.com/dashboard/).
2. Navigate to **Apps & Credentials -> Sandbox -> Create App**.
3. Copy the generated **Client ID** and **Secret Key**:
   - Set backend `PAYPAL_CLIENT_ID` and `PAYPAL_CLIENT_SECRET`.
   - Set frontend `VITE_PAYPAL_CLIENT_ID` to the same Sandbox Client ID.
4. Locate your auto-generated **Sandbox Personal (Buyer)** account credentials under **Testing Tools -> Sandbox Accounts** so you can sign into the live PayPal popup during checkout.
5. (Optional Webhook Verification) Register a Sandbox webhook pointing to `/api/v1/payments/paypal/webhook` and set `TRADETWIN_PAYPAL_WEBHOOK_ID` (Spring property `tradetwin.paypal.webhook-id`).

> **Single Sandbox Merchant MVP Scope**: Live PayPal checkout sends orders to your configured Sandbox Merchant REST App (`PAYPAL_CLIENT_ID` / `PAYPAL_CLIENT_SECRET`), matching standard PayPal Sandbox testing. Multi-seller split disbursements via PayPal Commerce Platform are documented as a future phase.
>
> Only when both `PAYPAL_CLIENT_ID` and `PAYPAL_CLIENT_SECRET` are omitted or left as placeholders does TradeTwin run in local **Verified Sandbox Simulation Mode**. Whenever live PayPal Sandbox keys are configured, errors from PayPal (including `HTTP 422 UNPROCESSABLE_ENTITY`) are parsed and surfaced cleanly rather than hidden behind simulated success.

### Troubleshooting PayPal `HTTP 422 UNPROCESSABLE_ENTITY`

TradeTwin's `PayPalService` extracts sanitized failure fields (`failureStage`, `paypalIssue`, `paypalDetailDescription`, `paypalDetailField`, `debugId`) onto `PaymentRecord` and returns them in `ErrorResponse.details[]`:

| Stage / Issue | Typical Cause | Resolution / Built-In Guard |
|---|---|---|
| `CAPTURE_ORDER` + `ORDER_NOT_APPROVED` | Capture called while PayPal order is still `CREATED` or `PAYER_ACTION_REQUIRED` | Complete approval in PayPal popup before capturing; `PayPalService` inspects `GET /v2/checkout/orders/{id}` first and releases reserved stock so buyer can retry. |
| `CAPTURE_ORDER` + `ORDER_ALREADY_CAPTURED` | Duplicate capture submission after success | TradeTwin inspects PayPal order status and idempotently returns the completed order if `COMPLETED`. |
| `CAPTURE_ORDER` + `INSTRUMENT_DECLINED` | Sandbox buyer funding source declined | Releases agreement stock reservation (`stockReserved=false`) and lets buyer click **Retry PayPal Checkout** within the 10-minute agreement window. |
| `CREATE_ORDER` breakdown / currency mismatch | Non-2-decimal totals or `item_total + shipping != total` | Enforced server-side with `BigDecimal` (`RoundingMode.HALF_UP`, scale 2), ISO uppercase currency, and `<= 127`-char descriptions. |

---

## 5. Pre-Seeded Local Development Accounts & Catalog (DEMO DATA)

All demo accounts share password: `password123`

| Role | Email | Password | Store / Persona Details |
|---|---|---|---|
| **Buyer** | `buyer@tradetwin.com` | `password123` | Alice Walker (Default USD, Oregon shipping address, Reputation 98.5) |
| **Seller 1** | `seller@tradetwin.com` | `password123` | **QuantumTech Direct Official** (Workstations, Sony Audio `$98` / `$89.99`, Smartwatches, Keyboards, Monitors) |
| **Seller 2** | `seller2@tradetwin.com` | `password123` | **Apex Audio Collective** (Competing Sony WH-CH720N `$95` 24-Mo Warranty Bundle & Sony WF-C700N `$86` Express Pack) |
| **Demo Seller A** | `novatech@tradetwin.com` | `password123` | **NovaTech Electronics** (Sony WH-CH720N & electronics listings) |
| **Demo Seller B** | `apexdigital@tradetwin.com` | `password123` | **Apex Digital Store** (Sony WH-CH720N & peripherals) |
| **Demo Seller C** | `quantum@tradetwin.com` | `password123` | **Quantum Gadgets** (Sony WH-CH720N & multi-category catalog) |
| **Demo Seller D** | `orbit@tradetwin.com` | `password123` | **Orbit Electronics** (High-floor Sony WH-CH720N `$112` ask / `$104` floor + `$7` shipping for fallback demo) |
| **Demo Seller E** | `primeaudio@tradetwin.com` | `password123` | **Prime Audio Market** (Cooperative Sony WH-CH720N listing & audio gear) |
| **Demo Seller F** | `vertex@tradetwin.com` | `password123` | **Vertex Technology** (Competitive `$79.50` floor Sony WH-CH720N & accessories) |
| **Admin** | `admin@tradetwin.com` | `password123` | System Administrator |

---

## 6. Running Automated Verification

### Backend Unit, Security, PayPal & Full Workflow Tests
```powershell
cd backend
.\mvnw.cmd test
```

### Frontend Lint & Production TypeScript Bundle
```powershell
cd frontend
npm run lint
npm run build
```
