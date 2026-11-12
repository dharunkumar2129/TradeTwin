export type UserRole = 'ROLE_BUYER' | 'ROLE_SELLER' | 'ROLE_ADMIN';

export interface ShippingAddress {
  fullName?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

export interface ShoppingPreferences {
  defaultCurrency?: string;
  categoriesOfInterest?: string[];
  priceSensitivity?: 'BALANCED' | 'DISCOUNT_SEEKER' | 'PREMIUM';
  preferredWarrantyMonths?: number;
  autoNegotiateDefault?: boolean;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  reputationScore: number;
  defaultCurrency?: string;
  shippingAddress?: ShippingAddress;
  shoppingPreferences?: ShoppingPreferences;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponse {
  token: string;
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  reputationScore: number;
  defaultCurrency?: string;
}

export interface BusinessContact {
  phone?: string;
  supportEmail?: string;
  supportHours?: string;
}

export interface ShippingTerms {
  handlingTimeDays?: number;
  freeShippingThreshold?: number;
  allowExpressShipping?: boolean;
  returnPolicyDays?: number;
}

export type NegotiationStrategy = 'BALANCED' | 'CONSERVATIVE' | 'COOPERATIVE';

export interface SellerProfile {
  id?: string;
  userId: string;
  storeName: string;
  description: string;
  logoUrl?: string;
  businessContact?: BusinessContact;
  shippingRegions?: string[];
  defaultShippingTerms?: ShippingTerms;
  defaultStrategy?: NegotiationStrategy;
  autoNegotiateEnabled?: boolean;
  minimumReputationRequired?: number;
  onboardingCompleted?: boolean;
  onboardingStep?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateProfileRequest {
  fullName?: string;
  defaultCurrency?: string;
  shippingAddress?: ShippingAddress;
  shoppingPreferences?: ShoppingPreferences;
}

export interface SellerOnboardingRequest {
  storeName: string;
  description: string;
  logoUrl?: string;
  businessContact?: BusinessContact;
  shippingRegions?: string[];
  defaultShippingTerms?: ShippingTerms;
  defaultStrategy?: NegotiationStrategy;
  autoNegotiateEnabled?: boolean;
  minimumReputationRequired?: number;
  onboardingStep?: number;
  onboardingCompleted?: boolean;
}

export interface QuantityDiscountRule {
  minQuantity: number;
  discountPct: number;
}

export interface NegotiationPolicy {
  autoNegotiate: boolean;
  floorPrice: number;
  targetPrice: number;
  maxDiscountPct: number;
  autoAcceptPriceThreshold?: number;
  strategy: NegotiationStrategy;
  maxRounds: number;
  quantityDiscounts?: QuantityDiscountRule[];
  allowWarrantySweeteners: boolean;
  maxWarrantyMonthsSweetener?: number;
  allowFreeShipping: boolean;
  maxDeliveryDaysCompromise?: number;
  minimumBuyerReputation: number;
}

export interface Product {
  id: string;
  sellerId: string;
  title: string;
  brand?: string;
  sku?: string;
  description: string;
  category: string;
  condition?: string;
  listPrice: number;
  currency: string;
  availableInventory: number;
  inventoryCount?: number;
  reservedCount?: number;
  minOrderQuantity?: number;
  maxOrderQuantity?: number;
  shippingCost?: number;
  estimatedDeliveryDays?: number;
  warrantyInfo?: string;
  returnPolicy?: string;
  specifications?: Record<string, string>;
  autoNegotiate: boolean;
  maxNegotiationRounds: number;
  allowWarrantySweeteners: boolean;
  allowFreeShipping: boolean;
  images: string[];
  tags: string[];
  active?: boolean;
  createdAt: string;
  updatedAt?: string;
  negotiationPolicy?: NegotiationPolicy; // Included only in authenticated seller management view
}

export interface SellerActivityItem {
  id: string;
  type: string;
  title: string;
  description: string;
  amount?: number;
  currency?: string;
  timestamp: string;
}

export interface SellerDashboardSummary {
  totalProducts: number;
  activeListings: number;
  totalInventory: number;
  reservedInventory: number;
  lowStockCount: number;
  negotiationsReceived: number;
  acceptedDeals: number;
  totalOrders: number;
  pendingFulfilmentOrders?: number;
  shippedOrders?: number;
  deliveredOrders?: number;
  totalSalesVolume: number;
  currency: string;
  recentActivity: SellerActivityItem[];
}

export interface BuyerPreferences {
  targetPrice: number;
  maxCeilingPrice: number;
  urgency: 'LOW' | 'MEDIUM' | 'HIGH';
  desiredDeliveryDays: number;
  requireExtendedWarranty: boolean;
  quantity?: number;
  minWarrantyMonths?: number;
  requiredBrand?: string;
  preferredBrand?: string;
  buyerPrompt?: string;
}

export interface OfferProposal {
  price: number;
  unitPrice?: number;
  shippingCost?: number;
  totalPayable?: number;
  quantity?: number;
  currency: string;
  deliveryDays: number;
  warrantyMonths: number;
  offerStatus?: 'OFFERED' | 'COUNTERED' | 'ACCEPTED' | 'REJECTED';
  roundNumber?: number;
  bonusPerks: string[];
}

export interface Turn {
  turnNumber: number;
  roundNumber?: number;
  speaker: 'BUYER_AGENT' | 'SELLER_AGENT';
  action: 'PROPOSE' | 'OFFER' | 'COUNTER' | 'ACCEPT' | 'REJECT';
  sellerId?: string;
  sellerStoreName?: string;
  productId?: string;
  productTitle?: string;
  proposal: OfferProposal;
  agentReasoning: string;
  timestamp: string;
}

export interface ScoreBreakdown {
  landedCostScore: number;
  brandMatchScore: number;
  specMatchScore: number;
  deliveryScore: number;
  warrantyScore: number;
  quantityScore: number;
  returnConditionsScore: number;
  constraintComplianceScore: number;
}

export interface FairDealOfferEvaluation {
  offerId?: string;
  sellerId: string;
  sellerStoreName?: string;
  productId: string;
  productTitle: string;
  brand?: string;
  unitPrice: number;
  quantity: number;
  shippingCost: number;
  totalLandedCost: number;
  deliveryEstimateDays: number;
  warrantyMonths: number;
  perks?: string[];
  passedHardFilters: boolean;
  filterFailureReason?: string;
  buyerSatisfactionScore: number;
  offerQualityScore: number;
  sellerPolicyCompliance: number;
  overallDealScore: number;
  breakdown?: ScoreBreakdown;
  calculationExplanation?: string;
}

export interface FairDealComparisonResult {
  hasViableDeal: boolean;
  summaryExplanation: string;
  evaluations: FairDealOfferEvaluation[];
  bestOffer?: FairDealOfferEvaluation;
}

export interface FairDealMetrics {
  fairDealScore: number;
  priceEquitability: number;
  utilityBuyer: number;
  utilitySeller: number;
  surplusSplitPctBuyer: number;
  surplusSplitPctSeller: number;
  marketFairPriceEstimate: number;
  summaryExplanation: string;
  buyerSatisfactionScore?: number;
  offerQualityScore?: number;
  sellerPolicyCompliance?: number;
  overallDealScore?: number;
  totalLandedCost?: number;
  scoreBreakdown?: ScoreBreakdown;
  passedHardFilters?: boolean;
  filterFailureReason?: string;
  calculationExplanation?: string;
}

export interface NegotiationStreamEvent {
  eventType: string;
  negotiationId: string;
  turnNumber?: number;
  speaker?: string;
  payload: unknown;
  timestamp: string;
}

export interface AgreementSeal {
  agreementId?: string;
  dealToken: string;
  sealedAt: string;
  expiresAt: string;
  reservationExpiresAt?: string;
  finalPrice: number;
  unitPrice?: number;
  shippingCost?: number;
  quantity?: number;
  currency: string;
  status: 'AWAITING_PAYMENT' | 'PAID' | 'EXPIRED';
  stockReserved?: boolean;
  preparedPaypalOrderId?: string;
  paymentExecutionMode?: 'SUPERVISED_PAYPAL_CHECKOUT' | 'DELEGATED_AUTONOMOUS_PAYMENT' | string;
  mandateId?: string;
}

export interface SellerAttemptRecord {
  sellerId: string;
  storeName: string;
  productId: string;
  productTitle: string;
  reason: string;
  roundsCompleted: number;
  lastOfferPrice?: number;
  timestamp: string;
}

export interface SellerCandidateSummary {
  sellerId: string;
  storeName: string;
  productId: string;
  productTitle: string;
  brand?: string;
  listPrice: number;
  shippingCost?: number;
  estimatedDeliveryDays: number;
  warrantyMonths?: number;
  availableInventory: number;
  status: 'QUEUED' | 'ACTIVE' | 'AGREED' | 'FAILED' | 'SKIPPED' | 'ALTERNATIVE';
  matchScore: number;
}

export interface NegotiationOffer {
  id?: string;
  negotiationId: string;
  sellerId: string;
  productId: string;
  roundNumber: number;
  unitPrice: number;
  quantity: number;
  shippingCost: number;
  deliveryEstimateDays: number;
  warrantyMonths: number;
  offerStatus: 'PROPOSED' | 'COUNTERED' | 'ACCEPTED' | 'REJECTED';
  publicExplanation: string;
  timestamp: string;
  perks?: string[];
}

export type NegotiationStatus =
  | 'CREATED'
  | 'DISCOVERING'
  | 'NEGOTIATING'
  | 'OFFER_RECEIVED'
  | 'COUNTEROFFERED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'EXPIRED'
  | 'COMPLETED'
  | 'INITIATED'
  | 'IN_PROGRESS'
  | 'AGREED'
  | 'CANCELLED';

export interface Negotiation {
  id: string;
  productId: string;
  buyerId: string;
  sellerId: string;
  status: NegotiationStatus;
  roundCount: number;
  maxRounds: number;
  buyerPreferences: BuyerPreferences;
  currentOffer?: OfferProposal;
  turns: Turn[];
  offers?: NegotiationOffer[];
  participatingSellerIds?: string[];
  participatingProductIds?: string[];
  fairDealMetrics?: FairDealMetrics;
  agreementSeal?: AgreementSeal;
  buyerPrompt?: string;
  productTitle?: string;
  productImage?: string;
  currentSellerStoreName?: string;
  currentSellerIndex?: number;
  sellerQueue?: SellerCandidateSummary[];
  failedSellerAttempts?: SellerAttemptRecord[];
  fallbackNotice?: string;
  noDealSummary?: string;
  alternatives?: SellerCandidateSummary[];
  createdAt: string;
  updatedAt: string;
}

export type OrderStatus =
  | 'PAYMENT_PENDING'
  | 'PAID'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED';

export interface PaymentRecord {
  id?: string;
  negotiationId: string;
  buyerId?: string;
  sellerId?: string;
  productId?: string;
  provider: string;
  paypalOrderId: string;
  captureId?: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'APPROVED' | 'COMPLETED' | 'DECLINED' | 'FAILED' | 'CANCELLED' | 'REFUNDED';
  failureReason?: string;
  agreementId?: string;
  failureStage?: string;
  paypalHttpStatus?: number;
  paypalErrorName?: string;
  paypalErrorMessage?: string;
  paypalIssue?: string;
  paypalDetailDescription?: string;
  paypalDetailField?: string;
  debugId?: string;
  paidAt?: string;
  createdAt?: string;
}

export interface OrderStatusChange {
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  updatedBy: string;
  notes?: string;
  timestamp: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  negotiationId: string;
  buyerId: string;
  sellerId: string;
  productId: string;
  productTitle?: string;
  productImage?: string;
  sellerStoreName?: string;
  quantity?: number;
  unitPrice?: number;
  finalAmount: number;
  currency: string;
  dealToken: string;
  payment?: PaymentRecord;
  shippingDetails?: Record<string, string>;
  orderStatus: OrderStatus;
  carrier?: string;
  trackingNumber?: string;
  fulfilmentNotes?: string;
  shippedAt?: string;
  deliveredAt?: string;
  statusHistory?: OrderStatusChange[];
  createdAt: string;
  updatedAt?: string;
}

export interface BuyerDashboardSummary {
  totalOrders: number;
  totalSpent: number;
  activeNegotiations: number;
  agreedDeals: number;
  pendingFulfilmentOrders: number;
  shippedOrDeliveredOrders: number;
  currency: string;
  recentOrders: Order[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface BuyerIntent {
  id?: string;
  buyerId?: string;
  rawQuery?: string;
  productType?: string;
  brandPreferences?: string[];
  requiredSpecifications?: Record<string, string>;
  optionalPreferences?: string[];
  maxBudget?: number;
  targetBudget?: number;
  quantity?: number;
  minWarrantyMonths?: number;
  maxDeliveryDays?: number;
  preferredConditions?: string[];
  shippingLocation?: string;
  currency?: string;
  status?: string;
  ambiguous?: boolean;
  followUpQuestion?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SearchResultItem {
  product: Product;
  sellerStoreName: string;
  sellerReputation: number;
  unitPrice: number;
  shippingCost: number;
  estimatedTotalPrice: number;
  estimatedDeliveryDays: number;
  warrantyInfo: string;
  warrantyMonths: number;
  condition?: string;
  availableInventory: number;
  matchScore: number;
  matchHighlights: string[];
  tradeOffs: string[];
  eligibleForNegotiation: boolean;
}

export interface CatalogSearchResponse {
  results: SearchResultItem[];
  totalMatched: number;
  searchSummary: string;
}

export interface CatalogSearchRequest {
  query?: string;
  productType?: string;
  brandPreferences?: string[];
  requiredSpecifications?: Record<string, string>;
  optionalPreferences?: string[];
  maxBudget?: number;
  quantity?: number;
  minWarrantyMonths?: number;
  maxDeliveryDays?: number;
  preferredConditions?: string[];
  currency?: string;
}

export interface ParseIntentResponse {
  intent: BuyerIntent;
  candidates: SearchResultItem[];
  totalMatched: number;
  searchSummary: string;
  followUpQuestion?: string;
}

export type PaymentApprovalPolicy = 'SUPERVISED_PAYPAL_CHECKOUT' | 'DELEGATED_AUTONOMOUS_PAYMENT';
export type MandateStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED';

export interface SpendMandate {
  id: string;
  buyerId: string;
  name: string;
  maxAmountPerTransaction: number;
  dailySpendingLimit: number;
  spentToday: number;
  remainingDailyBudget: number;
  currency: string;
  allowedCategories: string[];
  preferredBrands: string[];
  maxQuantity: number;
  approvedSellerIds: string[];
  expiresAt: string;
  paymentApprovalPolicy: PaymentApprovalPolicy;
  hasDelegatedVaultToken: boolean;
  delegatedVaultTokenMasked?: string;
  delegatedMerchantId?: string;
  delegatedMaxCurrency?: string;
  status: MandateStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface SpendMandateRequest {
  name: string;
  maxAmountPerTransaction: number;
  dailySpendingLimit: number;
  currency: string;
  allowedCategories: string[];
  preferredBrands: string[];
  maxQuantity: number;
  approvedSellerIds: string[];
  expiresAt: string;
  paymentApprovalPolicy: PaymentApprovalPolicy;
  delegatedVaultTokenId?: string;
  delegatedMerchantId?: string;
  delegatedMaxCurrency?: string;
}

export interface MandateValidationResult {
  compliant: boolean;
  hasActiveMandate: boolean;
  mandateId?: string;
  mandateName?: string;
  paymentApprovalPolicy: PaymentApprovalPolicy;
  maxAmountPerTransaction?: number;
  dailySpendingLimit?: number;
  spentToday?: number;
  projectedDailyTotal?: number;
  mandateExpiresAt?: string;
  violations: string[];
}

export interface DelegatedCapabilityStatus {
  adapterSupported: boolean;
  accountAccessEnabled: boolean;
  buyerCredentialLinked: boolean;
  eligible: boolean;
  adapterName: string;
  reasonCode: string;
  message: string;
}

export interface PaymentAuditEntry {
  id: string;
  negotiationId: string;
  agreementId?: string;
  buyerId: string;
  sellerId: string;
  sellerStoreName?: string;
  productId: string;
  productTitle?: string;
  mandateId?: string;
  mandateSnapshot?: Record<string, unknown>;
  negotiatedAmount: number;
  currency: string;
  budgetCompliant: boolean;
  complianceSummary: string;
  complianceViolations: string[];
  authorizationMethod: string;
  paypalOrderId?: string;
  paypalCaptureId?: string;
  finalStatus: string;
  diagnosticDetails?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface PaymentExecutionStatus {
  negotiationId: string;
  paymentExecutionMode: PaymentApprovalPolicy;
  preparedPaypalOrderId?: string;
  mandateValidation: MandateValidationResult;
  delegatedCapability: DelegatedCapabilityStatus;
  latestAudit?: PaymentAuditEntry;
}
