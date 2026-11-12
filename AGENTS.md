# TradeTwin Development & Agent Instructions

This document defines the persistent engineering standards, architectural rules, and operational guidelines for all AI agents and developers contributing to the TradeTwin codebase.

---

## 1. Core Engineering Principles
1. **Follow the Approved Architecture**:
   - Always adhere to the modular monolith architecture established in `docs/ARCHITECTURE.md` and the approved system plan.
   - Do not introduce microservices or arbitrary asynchronous message brokers without an explicit architectural change review.

2. **Preserve Working Features**:
   - Never overwrite working features unnecessarily.
   - Inspect existing files before editing to understand context, dependencies, and side effects.
   - Refactor with backwards compatibility in mind; preserve existing unit and integration tests.

3. **Modular Domain Separation**:
   - Separate components and services into logical, decoupled business modules:
     - `auth`, `user`, `seller`, `catalog`, `intent`, `negotiation`, `deal`, `payment`, `order`, `event`, and `common`.
   - Avoid circular package dependencies. Use event publishing (`ApplicationEventPublisher` or domain events) for cross-module side effects.

4. **Type Safety & Spring Boot Conventions**:
   - Use strongly typed TypeScript on the frontend (strict null checks, no `any` types).
   - Follow standard Spring Boot idioms: constructor injection (`@RequiredArgsConstructor`), immutable records for DTOs, `@Service`, `@Repository`, and proper HTTP status codes.

5. **Financial Precision (Non-Negotiable)**:
   - **Always use `java.math.BigDecimal`** for all prices, offers, discounts, utilities, and financial calculations on the backend.
   - Never use `double` or `float` for currency amounts to avoid IEEE 754 floating-point rounding errors.
   - Set explicit scale and rounding modes (e.g., `RoundingMode.HALF_UP`).

---

## 2. Security & Guardrails
1. **Never Expose Secrets or API Keys**:
   - Never commit or log API keys, JWT secrets, or payment credentials.
   - Read all sensitive configuration strictly via environment variables or `.env` files.

2. **Protect Private Financial Thresholds**:
   - Never expose private buyer reserve limits (maximum willingness to pay) or seller floor prices (minimum acceptable margin) to external APIs, public SSE streams, or client payloads.
   - Keep agent private constraints strictly on the server-side orchestrator.

3. **Backend Validation**:
   - Validate every important operation on the backend using Spring Validation (`@Valid`, `@NotNull`, `@DecimalMin`, `@Size`).
   - Never trust client-supplied input or unvalidated LLM output.

4. **Human-in-the-Loop & Payment Safety**:
   - **Never allow an LLM or autonomous agent to execute a payment directly.**
   - Autonomous agents negotiate terms to an `AGREED` state; final checkout authorization and capture must be explicitly initiated by the human buyer via PayPal Orders API v2.

---

## 3. Implementation Quality & Testing
1. **Real Implementations Over Decorative Mockery**:
   - Implement authentic domain logic, database operations, state transitions, and API contracts.
   - When external cloud dependencies (e.g. PayPal sandbox or live Gemini API) are optional in dev, provide robust, realistic simulation fallbacks rather than non-functional stubs.

2. **Automated Testing**:
   - Write comprehensive unit tests for critical business logic (e.g., Seller guardrails, negotiation state machine, FairDeal utility calculation, JWT filtering).
   - Run the appropriate test suite after completing each module before advancing.

3. **API Contracts**:
   - Maintain strict, synchronized API contracts between the frontend typed clients (`src/api/*`, `src/types/*`) and backend Spring controllers (`src/main/java/com/tradetwin/*/controller/*`).
