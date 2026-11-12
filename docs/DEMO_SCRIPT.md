# TradeTwin Live Product Demo Script (< 3 Minutes)

**Target Duration**: ~2 minutes 45 seconds  
**Prerequisites**: MongoDB running, Spring Boot backend on `:8080`, React frontend on `:5173`.

---

## [0:00 – 0:25] 1. Natural-Language Buyer Shopping (`HomePage`)
1. Open `http://localhost:5173`. Notice the clean storefront with the prominent **AI Natural-Language Search** box and the login status in the navbar.
2. Click **Sign In -> Demo Buyer (`buyer@tradetwin.com`)** (or let the AI deploy step auto-authenticate).
3. Click the first sample prompt (or paste):
   > `"Find me Sony wireless noise-cancelling headphones. My maximum total budget is $100. I prefer at least one year warranty and delivery within four days."`
4. Point out: **No dropdown filters needed.** The backend extracts structured fields (`Sony`, budget `$100`, `12` months warranty, `4` days delivery) into `BuyerIntent` and runs deterministic MongoDB retrieval.

## [0:25 – 0:50] 2. Intent Review & Multi-Seller Discovery
1. Show the `IntentReviewCard`:
   - Highlight the **Private Reserve Budget** lock badge (`$100` stays strictly server-side; never exposed to merchant agents).
   - Click **Confirm Intent Parameters**.
2. Scroll to `ProductComparisonMatrix` (badged **DEMO DATA**) and switch briefly between **Card View** and **Side-by-Side Spec Table**:
   - Point out overlapping `Sony WH-CH720N` listings across **6+ demo merchants**: including **Orbit Electronics** (`$112` ask / `$104` private floor, ideal for triggering automatic fallback when budget is `$100`) alongside **NovaTech Electronics**, **Apex Digital Store**, **Quantum Gadgets**, **Prime Audio Market**, and **Vertex Technology**.
3. Click **Deploy AI Negotiator** (passing `buyerQuery`, `candidateProductIds`, `quantity`, and `requiredBrand: Sony`) to enter the Arena.

## [0:50 – 1:40] 3. Live Multi-Agent Negotiation Arena, Fallback & FairDeal (`NegotiationArenaPage`)
1. Watch the **Server-Sent Events (`SSE`)** live feed stream rounds in real time:
   - Point out the **Seller Queue Progress** bar (`Seller 1 of N`) and conversational agent turn bubbles containing embedded **Offer Cards** (`Unit Price`, `Shipping`, `Total`, `Delivery`, `Warranty`, `Status`).
   - If the first candidate (`Orbit Electronics`) cannot settle within the `$100` buyer reserve ceiling, highlight the animated **Fallback Transition** (`SELLER_FALLBACK_TRIGGERED` -> `NEXT_SELLER_STARTED` -> `SELLER_TRANSITION`) automatically engaging the next eligible `Sony` merchant while preserving full conversation history.
   - Switch briefly to the **FairDeal Comparison Matrix** tab to show transparent hard filters and weighted sub-scores.
2. Note the sealed 10-minute `AgreementSeal` banner (`agreementId` `AGR-*`, `DEAL_SELECTED` / `AGREED`, with **10-minute stock reservation**).
3. Emphasize the core safety rule: **Autonomous agents stop at `AGREED` — they can never execute a payment.**
4. Click **Approve deal & continue**.

## [1:40 – 2:15] 4. Human-in-the-Loop PayPal Sandbox Checkout & Receipt (`CheckoutPage` -> `OrderSuccessPage`)
1. Show the agreement ID (`AGR-*`), itemized quantity/shipping breakdown, 10-minute reservation lock countdown, and **Server-Verified Amount** with the unified **Continue securely with PayPal** experience (single Sandbox Merchant MVP settlement scope).
2. Approve payment using the PayPal Sandbox button (or in local verification without live credentials, Verified Sandbox Mode; if a live PayPal 422 decline occurs, show that TradeTwin renders exact sanitized PayPal diagnostics and allows **Retry PayPal Checkout** after releasing/re-reserving stock safely).
3. Explain what happened behind the scenes:
   - `POST /api/v1/payments/paypal/create-order` recalculated final price from MongoDB `AgreementSeal` (ignoring any tampered frontend numbers).
   - `POST /api/v1/payments/paypal/capture-order` captured payment, stored `PaymentRecord` + `captureId`, deducted stock, and issued confirmed `Order` `TT-*`.
4. Show the `OrderSuccessPage` digital receipt with PayPal Order ID, Capture ID, and Deal Seal token.

## [2:15 – 2:45] 5. Commerce Hub & Controlled Order State Machine (`BuyerDashboardPage` & `SellerDashboardPage`)
1. Open **Buyer Studio** (`/buyer/dashboard`): show live KPIs (`Total Spent`, active deals) and the PayPal Audit Modal on the completed order.
2. Sign out and click **Demo Seller (`seller@tradetwin.com`)** -> open **Seller Portal** (`/seller/dashboard`):
   - Briefly show **Negotiation Policies** tab (where seller's private `floorPrice` lives privately).
   - Open **Orders & Fulfilment** tab -> advance an incoming confirmed order through the controlled state machine (`CONFIRMED` -> `PROCESSING` -> `SHIPPED` with tracking number), showing live audit history updates.
