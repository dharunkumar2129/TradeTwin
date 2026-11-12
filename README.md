# TradeTwin — Agentic AI Commerce Marketplace

TradeTwin is a full-stack autonomous B2C/B2B negotiation marketplace powered by **React + TypeScript + Vite**, **Java 21 + Spring Boot 3**, **MongoDB**, **Google Gemini API**, **Server-Sent Events (SSE)**, and **PayPal Orders API v2**.

Instead of static "Add to Cart" fixed pricing or manual catalog filters, buyers enter natural-language requests. A dedicated **Buyer Agent** negotiates simultaneously against competing merchants' **Seller Agents**—each enforcing strict, server-side private reserve thresholds (`maxCeilingPrice` vs. `floorPrice`)—while the transparent **FairDeal MAUT Engine** scores landed cost, delivery speed, warranty, and policy compliance before handing final checkout authorization exclusively to the human buyer via **PayPal Checkout**.

---

## Core Capabilities & Architecture

- **Natural-Language Shopping (`IntentParsingService`)**: Parses free-text prompts into structured `BuyerIntent` models via Gemini API (`gemini-3.5-flash-lite`, with automatic cooldown fallback + deterministic recovery) and MongoDB catalog matching.
- **Multi-Agent Negotiation & Automatic Seller Fallback (`BuyerNegotiationAgent`, `SellerNegotiationAgent`, `NegotiationOrchestrator`)**: Progressive multi-round bargaining (`3–5` rounds) with conversational dialogue + structured Offer Cards, strict `java.math.BigDecimal` server-side guardrails (`maxCeilingPrice` vs. `floorPrice`), mandatory brand preservation, and automatic cross-seller fallback across ranked candidate listings.
- **Transparent FairDeal Evaluation (`FairDealEngine`)**: Mandatory hard filters (budget ceiling, available stock, MOQ) followed by weighted Multi-Attribute Utility scoring with human-readable breakdowns.
- **Live SSE Streaming (`SseStreamingHub`)**: Real-time `EventSource` updates (`NEGOTIATION_STARTED`, `SELLERS_DISCOVERED`, `SELLER_AGENT_STARTED`, `SELLER_INITIAL_OFFER`, `BUYER_COUNTEROFFER`, `SELLER_COUNTEROFFER`, `SELLER_FALLBACK_TRIGGERED`, `NEXT_SELLER_STARTED`, `SELLER_TRANSITION`, `NO_DEAL_EXHAUSTED`, `DEAL_SELECTED`, `AGREED`) persisted in MongoDB.
- **Spend Mandate & Payment Execution Engine (`SpendMandateService`, `PaymentExecutionEngine`, `PayPalVaultDelegatedPaymentAdapter`, `PaymentAuditService`)**: Deterministic server-side spending guardrails completely isolated from LLM agent reasoning (`maxAmountPerTransaction`, `dailySpendingLimit`, allowed categories, preferred brands, max quantity, approved sellers, expiry, and `SUPERVISED_PAYPAL_CHECKOUT` vs `DELEGATED_AUTONOMOUS_PAYMENT` policy). Supervised agreements auto-prepare the PayPal Sandbox order immediately upon sealing and require explicit buyer approval before backend capture; delegated execution strictly validates Vault v3 merchant access (`PAYPAL_DELEGATED_VAULT_ENABLED`) + buyer Vault `payment_token` and transparently reports `DELEGATED_ACCESS_UNAVAILABLE` when unavailable (never fakes payment or substitutes OAuth client tokens).
- **PayPal Orders v2 Checkout (`PayPalService`)**: Server-side OAuth2 authentication, 2-decimal breakdown validation (`item_total + shipping == total`), `PayPal-Request-Id` idempotency, pre-capture order status inspection, deal-lock expiration (`10-minute TTL`), stock reservation release/re-reserve on fail/cancel/retry, webhook signature verification, and sanitized PayPal HTTP 422 error diagnostics (never hidden as fake success when live Sandbox credentials are configured).
  - *Single Sandbox Merchant MVP Scope*: Live PayPal Sandbox checkout routes settlement through the configured TradeTwin Sandbox Merchant application (`PAYPAL_CLIENT_ID` / `PAYPAL_CLIENT_SECRET`); future production phases can extend `PayPalService` to PayPal Commerce Platform multi-seller onboarding and split payouts.
- **Controlled Order State Machine (`OrderStateMachine`, `OrderService`)**: `PAYMENT_PENDING` -> `PAID` -> `CONFIRMED` -> `PROCESSING` -> `SHIPPED` -> `DELIVERED` (with `CANCELLED`/`REFUNDED` automatic inventory restock).
- **Responsive UI**: Sticky role-aware navigation with mobile drawer, Buyer Studio, Seller Portal (listings, private policies, incoming order fulfilment), Negotiation Arena, and PayPal Checkout.

---

## Repository Structure

```text
tradetwin/
├── backend/                  # Java 21 + Spring Boot 3.3.5 modular monolith (93 source files, Maven wrapper)
│   └── src/main/java/com/tradetwin/
│       ├── auth/             # JWT authentication, BCrypt, Spring Security config
│       ├── user/             # User profiles & shipping/shopping preferences
│       ├── seller/           # Merchant onboarding & dashboard summaries
│       ├── catalog/          # Product CRUD, stock, private NegotiationPolicy, search ranking
│       ├── intent/           # NL intent extraction & buyer intent storage
│       ├── negotiation/      # Orchestrator, BuyerNegotiationAgent, SellerNegotiationAgent
│       ├── deal/             # FairDeal scoring engine & AgreementSeal
│       ├── event/            # SseStreamingHub & persistent NegotiationEventDocument
│       ├── mandate/          # SpendMandate config, deterministic limits & policy rules
│       ├── payment/          # PayPal Orders v2, Vault adapter, Execution Engine & audit logs
│       ├── order/            # OrderStateMachine & buyer/seller fulfilment APIs
│       └── common/           # Exceptions, CORS, health, DataInitializer
├── frontend/                 # React 19 + TypeScript + Vite 8 + Tailwind CSS SPA
├── docs/                     # Architecture diagrams, API spec, sandbox guide, demo script
├── docker-compose.yml        # MongoDB 7.0 local development container
├── .env.example              # Backend & integration environment variable template
└── AGENTS.md                 # Persistent engineering & financial safety standards
```

---

## Quick Start

1. **Start MongoDB 7.0**:
   ```bash
   docker compose up -d
   ```
2. **Configure environment variables**:
   ```bash
   cp .env.example .env
   ```
   *(Optional in dev: Gemini AI and PayPal Checkout include deterministic / verified Sandbox simulation fallbacks when live API keys are unset.)*
3. **Run the Spring Boot Backend** (`http://localhost:8080`):
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
4. **Run the React Frontend** (`http://localhost:5173`):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

---

## Seeded Local Development Accounts (DEMO DATA)

All demo users use password: `password123`

| Role | Email | Password | Seeded Demo Store / Persona Highlights |
|---|---|---|---|
| **Buyer** | `buyer@tradetwin.com` | `password123` | Alice Walker — Profile, Oregon address, Order `ORD-2026-9812` |
| **Seller 1** | `seller@tradetwin.com` | `password123` | **QuantumTech Direct Official** — 8 listings incl. Sony WH-CH720N (`$98`) & WF-C700N (`$89.99`) |
| **Seller 2** | `seller2@tradetwin.com` | `password123` | **Apex Audio Collective** — Competing Sony ANC listings (`$95` & `$86`) |
| **Demo Seller A** | `novatech@tradetwin.com` | `password123` | **NovaTech Electronics** — Sony WH-CH720N + electronics catalog (`AGGRESSIVE` / balanced options) |
| **Demo Seller B** | `apexdigital@tradetwin.com` | `password123` | **Apex Digital Store** — Sony WH-CH720N + monitors, smartwatches, keyboards |
| **Demo Seller C** | `quantum@tradetwin.com` | `password123` | **Quantum Gadgets** — Sony WH-CH720N + multi-category consumer tech |
| **Demo Seller D** | `orbit@tradetwin.com` | `password123` | **Orbit Electronics** — Sony WH-CH720N high-floor listing (`$104` floor + `$7` shipping) for fallback testing |
| **Demo Seller E** | `primeaudio@tradetwin.com` | `password123` | **Prime Audio Market** — Sony WH-CH720N cooperative bundle + audio accessories |
| **Demo Seller F** | `vertex@tradetwin.com` | `password123` | **Vertex Technology** — Sony WH-CH720N lowest-floor catalog option (`$79.50` floor) + accessories |
| **Admin** | `admin@tradetwin.com` | `password123` | System Administrator |

---

## Verification Commands

- **Backend Automated Tests & Package Build** (Unit, Security/RBAC, Guardrails, PayPal Cancel/Capture/Idempotency, Full Workflow Integration):
  ```powershell
  cd backend
  .\mvnw.cmd clean test package -DskipTests=false
  ```
- **Frontend Lint & Production TypeScript Build**:
  ```powershell
  cd frontend
  npm run lint
  npm run build
  ```

---

## Detailed Documentation

- [System Architecture & Mermaid Diagrams](docs/ARCHITECTURE.md)
- [REST & SSE API Documentation](docs/API_DOCUMENTATION.md)
- [Local Setup, Gemini & PayPal Sandbox Guide](docs/SETUP_AND_SANDBOX_GUIDE.md)
- [3-Minute Live Demo Script](docs/DEMO_SCRIPT.md)
