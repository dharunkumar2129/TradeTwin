# TradeTwin System Architecture

TradeTwin is designed as a **Modular Monolith** balancing domain isolation, cryptographic/financial safety, and operational simplicity for local and cloud deployment.

---

## 1. High-Level System Architecture

```mermaid
flowchart TD
    UI["React + TypeScript + Vite SPA"]
    SEC["Spring Security + Stateless JWT Filter"]

    subgraph MONOLITH["TradeTwin Spring Boot 3 Modular Monolith"]
        AUTH["auth / user / seller Modules"]
        CAT["catalog & intent Modules"]
        NEG["negotiation Orchestrator & Agents"]
        FAIR["deal FairDeal MAUT Engine"]
        SSE["event SseStreamingHub"]
        PAY["payment PayPal Orders v2 Service"]
        ORD["order Fulfilment State Machine"]
    end

    MONGO[("MongoDB 7.0")]
    GEMINI["Google Gemini API (gemini-3.8-flash)"]
    PAYPAL["PayPal Orders v2 Sandbox / Live API"]

    UI -->|REST HTTPS JSON| SEC
    UI -->|EventSource SSE| SSE
    SEC --> AUTH
    SEC --> CAT
    SEC --> NEG
    SEC --> PAY
    SEC --> ORD

    CAT -->|Structured Extraction| GEMINI
    NEG -->|Buyer & Seller Decisions| GEMINI
    NEG --> FAIR
    NEG -->|Broadcast Events| SSE
    PAY -->|OAuth2 + Create/Capture + Webhooks| PAYPAL
    PAY -->|Verified Settlement| ORD

    AUTH --> MONGO
    CAT --> MONGO
    NEG --> MONGO
    SSE --> MONGO
    PAY --> MONGO
    ORD --> MONGO
```

---

## 2. Multi-Agent Negotiation & FairDeal Checkout Sequence

```mermaid
sequenceDiagram
    actor Buyer as Human Buyer
    participant UI as TradeTwin Frontend
    participant Intent as IntentParsingService
    participant Orch as NegotiationOrchestrator
    participant SAgent as SellerNegotiationAgent (xN)
    participant BAgent as BuyerNegotiationAgent
    participant Fair as FairDealEngine
    participant Pay as PayPalService + Orders v2

    Buyer->>UI: Enter natural-language request
    UI->>Intent: POST /api/v1/intents/parse
    Intent-->>UI: Structured BuyerIntent + ranked catalog matches
    Buyer->>UI: Deploy AI Agents (/negotiations/start)
    UI->>Orch: POST /api/v1/negotiations/start
    UI->>Orch: Subscribe SSE GET /api/v1/negotiations/{id}/stream

    Orch->>SAgent: Evaluate Seller Policy (Floor hidden from Buyer & LLM leaks scrubbed)
    SAgent-->>Orch: Opening Quotes (Offer 1..N)
    Orch->>Fair: Hard filters + Multi-attribute scoring
    Orch->>BAgent: Evaluate Quotes against Private Ceiling (Ceiling hidden from Sellers)
    BAgent-->>Orch: Counteroffer / Accept Decision
    Orch->>SAgent: Evaluate Counteroffer within Floor Guardrails
    Orch-->>UI: SSE Events (OFFER_PROPOSED, FAIRDEAL_METRIC, DEAL_SELECTED)

    Note over Buyer,Pay: Human-in-the-Loop Checkout (Agents NEVER execute payments)
    Buyer->>UI: Click PayPal Checkout
    UI->>Pay: POST /api/v1/payments/paypal/create-order
    Pay-->>UI: Server-calculated paypalOrderId
    Buyer->>Pay: Approve in PayPal UI -> POST /api/v1/payments/paypal/capture-order
    Pay-->>UI: Confirmed Order + PayPal Capture ID
```

---

## 3. Controlled Order Fulfilment State Machine

```mermaid
stateDiagram-v2
    [*] --> PAYMENT_PENDING: PayPal order initiated
    PAYMENT_PENDING --> PAID: Server capture verified
    PAYMENT_PENDING --> CANCELLED: Buyer cancelled / expired
    PAID --> CONFIRMED: AgreementSeal verified & order issued
    CONFIRMED --> PROCESSING: Seller prepares shipment
    PROCESSING --> SHIPPED: Tracking number & carrier assigned
    SHIPPED --> DELIVERED: Delivery confirmed
    CONFIRMED --> CANCELLED: Order cancelled (restocks inventory)
    PROCESSING --> CANCELLED: Order cancelled (restocks inventory)
    CONFIRMED --> REFUNDED: Refund issued (restocks inventory)
    SHIPPED --> REFUNDED: Return refund issued (restocks inventory)
    DELIVERED --> REFUNDED: Post-delivery refund issued (restocks inventory)
```

---

## 4. Modular Package Responsibilities

| Module | Package (`com.tradetwin.*`) | Core Responsibilities |
|---|---|---|
| Authentication | `auth` | BCrypt registration/login, stateless HS256 JWT filter, RBAC (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`) |
| Users | `user` | Buyer profile, default currency, shipping address, shopping preferences |
| Sellers | `seller` | Seller onboarding store profile, shipping regions, default strategy, seller KPI dashboard summary |
| Catalog | `catalog` | Product CRUD, image galleries, stock management, private `NegotiationPolicy`, deterministic search ranking |
| Intent | `intent` | Gemini-assisted NL query parsing with heuristic fallback and follow-up ambiguity prompts |
| Negotiation | `negotiation` | Autonomous `BuyerNegotiationAgent`, `SellerNegotiationAgent`, and `NegotiationOrchestrator` loop |
| FairDeal | `deal` | Mandatory hard filters (budget, inventory, MOQ) + weighted transparent multi-attribute scoring + `AgreementSeal` |
| Real-Time Events | `event` | `SseStreamingHub` managing live browser `SseEmitter` streams + MongoDB audit event log |
| Payments | `payment` | PayPal OAuth2 token cache, Orders v2 create/capture/lookup, cancel/decline logging, idempotency, webhook verification |
| Orders | `order` | Controlled `OrderStateMachine`, inventory deduction/restock, shipment tracking, buyer dashboard summaries |
| Common | `common` | Global exception mapping (`GlobalExceptionHandler`), CORS config, health actuator, demo `DataInitializer` |

---

## 5. Non-Negotiable Guardrails

1. **Private Threshold Isolation**:
   - Seller `floorPrice` and `autoAcceptPriceThreshold` are omitted from `ProductPublicDto`, search responses, SSE events, and buyer payloads.
   - Buyer `maxCeilingPrice` is never passed to `SellerNegotiationAgent`, omitted from public `NEGOTIATION_STARTED` SSE broadcasts and `Turn.agentReasoning`, and masked from unauthenticated or non-owner callers on `GET /api/v1/negotiations/{id}` and `GET /api/v1/intents/{id}`.
2. **Zero-Trust Validation of LLM Decisions**:
   - Every AI counteroffer or acceptance is clamped/overruled server-side with `java.math.BigDecimal` (`RoundingMode.HALF_UP`) against private floor/ceiling bounds.
   - Explanations are scrubbed before persistence or SSE emission so threshold values do not leak in natural language.
3. **Human-in-the-Loop Financial Settlement**:
   - Negotiating agents can only advance to `ACCEPTED` / `AGREED` with a 15-minute `AgreementSeal`.
   - Final payment creation and capture require explicit human buyer action and server-side PayPal amount recalculation.
