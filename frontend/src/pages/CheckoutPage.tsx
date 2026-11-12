import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { apiClient, extractErrorDetails } from '../api/client';
import { mandateApi } from '../api/mandateApi';
import type { Negotiation, Product, Order, PaymentExecutionStatus } from '../types';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { Badge } from '../components/ui/Badge';
import { formatCurrency, extractErrorMessage } from '../lib/utils';
import {
  ShieldCheck,
  Lock,
  Clock,
  ArrowLeft,
  CheckCircle2,
  AlertTriangle,
  ShoppingBag,
  Truck,
  Shield,
  CreditCard,
  Building,
} from 'lucide-react';

declare global {
  interface Window {
    paypal?: {
      Buttons?: (options: {
        style?: {
          layout?: 'vertical' | 'horizontal';
          color?: 'gold' | 'blue' | 'silver' | 'white' | 'black';
          shape?: 'rect' | 'pill';
          label?: 'paypal' | 'checkout' | 'buynow' | 'pay';
          height?: number;
        };
        createOrder: () => Promise<string>;
        onApprove: (data: { orderID: string; payerID?: string }) => Promise<void>;
        onCancel?: (data: { orderID: string }) => void;
        onError?: (err: unknown) => void;
      }) => {
        render: (container: string | HTMLElement) => Promise<void>;
      };
    };
  }
}

export const CheckoutPage: React.FC = () => {
  const { negotiationId } = useParams<{ negotiationId: string }>();
  const [negotiation, setNegotiation] = useState<Negotiation | null>(null);
  const [product, setProduct] = useState<Product | null>(null);
  const [executionStatus, setExecutionStatus] = useState<PaymentExecutionStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingStage, setProcessingStage] = useState<string>('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [errorDetails, setErrorDetails] = useState<string[]>([]);
  const [cancelMsg, setCancelMsg] = useState<string | null>(null);

  // Shipping Form State
  const [fullName, setFullName] = useState('Alice Walker');
  const [addressLine1, setAddressLine1] = useState('100 Innovation Way, Suite 400');
  const [city, setCity] = useState('San Francisco');
  const [state, setState] = useState('CA');
  const [postalCode, setPostalCode] = useState('94105');
  const [country, setCountry] = useState('United States');

  // PayPal SDK State
  const [sdkStatus, setSdkStatus] = useState<'IDLE' | 'LOADING' | 'READY' | 'FAILED'>('IDLE');
  const [checkoutMode, setCheckoutMode] = useState<'PAYPAL_SDK' | 'SANDBOX_SIMULATION'>('PAYPAL_SDK');
  const paypalContainerRef = useRef<HTMLDivElement>(null);
  const buttonsRenderedRef = useRef(false);
  const activePaypalOrderIdRef = useRef<string | null>(null);
  const shippingRef = useRef({ fullName, addressLine1, city, state, postalCode, country });

  useEffect(() => {
    shippingRef.current = { fullName, addressLine1, city, state, postalCode, country };
  }, [fullName, addressLine1, city, state, postalCode, country]);

  // 15-Minute Deal Expiration Countdown
  const [secondsRemaining, setSecondsRemaining] = useState<number>(900);
  const isExpired = secondsRemaining <= 0;

  const navigate = useNavigate();

  // Load negotiation and product
  useEffect(() => {
    if (negotiationId) {
      apiClient
        .get<{ data: Negotiation }>(`/negotiations/${negotiationId}`)
        .then((res) => {
          const neg = res.data.data;
          setNegotiation(neg);

          // Calculate initial remaining seconds based on agreement seal expiresAt
          if (neg.agreementSeal?.expiresAt) {
            const expireTime = new Date(neg.agreementSeal.expiresAt).getTime();
            const now = Date.now();
            const diffSec = Math.max(0, Math.floor((expireTime - now) / 1000));
            setSecondsRemaining(diffSec);
          }

          mandateApi
            .getExecutionStatus(neg.id)
            .then((statusData) => setExecutionStatus(statusData))
            .catch(() => {});

          return apiClient.get<{ data: Product }>(`/catalog/${neg.productId}`);
        })
        .then((res) => setProduct(res.data.data))
        .catch((err) => setErrorMsg(err.message || 'Failed to load checkout details'))
        .finally(() => setIsLoading(false));
    }
  }, [negotiationId]);

  // Expiration countdown ticker
  useEffect(() => {
    if (secondsRemaining <= 0) return;
    const interval = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [secondsRemaining]);

  // Load PayPal JavaScript SDK script dynamically
  useEffect(() => {
    if (!negotiation || !negotiation.agreementSeal || isExpired) return;

    // If SDK already loaded on window
    if (window.paypal?.Buttons) {
      setSdkStatus('READY');
      return;
    }

    setSdkStatus('LOADING');
    const clientId = import.meta.env.VITE_PAYPAL_CLIENT_ID || 'sb';
    const dealCurrency = negotiation.agreementSeal.currency || 'USD';
    const script = document.createElement('script');
    script.src = `https://www.paypal.com/sdk/js?client-id=${encodeURIComponent(
      clientId
    )}&currency=${encodeURIComponent(dealCurrency)}&components=buttons&intent=capture`;
    script.async = true;

    script.onload = () => {
      setSdkStatus('READY');
    };

    script.onerror = () => {
      console.warn('PayPal JS SDK failed to load from CDN. Switching to verified Sandbox Simulation fallback.');
      setSdkStatus('FAILED');
      setCheckoutMode('SANDBOX_SIMULATION');
    };

    document.head.appendChild(script);

    return () => {
      // Clean up script tag if needed
      if (script.parentNode) {
        script.parentNode.removeChild(script);
      }
    };
  }, [negotiation, isExpired]);

  // Mount PayPal Buttons when SDK is READY and mode is PAYPAL_SDK
  useEffect(() => {
    if (checkoutMode !== 'PAYPAL_SDK') {
      buttonsRenderedRef.current = false;
      return;
    }

    const container = paypalContainerRef.current;
    if (
      sdkStatus !== 'READY' ||
      !window.paypal?.Buttons ||
      !container ||
      !negotiation ||
      !negotiation.agreementSeal ||
      isExpired ||
      buttonsRenderedRef.current
    ) {
      return;
    }

    try {
      container.innerHTML = '';
      buttonsRenderedRef.current = true;

      window.paypal
        .Buttons({
          style: {
            layout: 'vertical',
            color: 'gold',
            shape: 'rect',
            label: 'paypal',
            height: 48,
          },
          createOrder: async () => {
            setErrorMsg(null);
            setErrorDetails([]);
            setCancelMsg(null);
            setIsProcessing(true);
            setProcessingStage('Preparing your PayPal order securely...');

            try {
              const createRes = await apiClient.post<{ data: { paypalOrderId: string; simulated?: boolean } }>(
                '/payments/paypal/create-order',
                {
                  negotiationId: negotiation.id,
                  dealToken: negotiation.agreementSeal!.dealToken,
                }
              );
              if (createRes.data.data.simulated) {
                setIsProcessing(false);
                setProcessingStage('');
                setCheckoutMode('SANDBOX_SIMULATION');
                const simMsg =
                  'Backend is running in Sandbox Simulation mode (server PAYPAL_CLIENT_SECRET is not configured). Click "Continue securely with PayPal" below to run the simulated backend flow.';
                setErrorMsg(simMsg);
                setErrorDetails(['stage: CREATE_ORDER', 'mode: DEV_SANDBOX_FALLBACK']);
                throw new Error(simMsg);
              }
              activePaypalOrderIdRef.current = createRes.data.data.paypalOrderId;
              setProcessingStage('Complete approval inside the PayPal Sandbox window...');
              return createRes.data.data.paypalOrderId;
            } catch (err: unknown) {
              setIsProcessing(false);
              const msg = extractErrorMessage(err, 'Failed to initialize PayPal order');
              const details = extractErrorDetails(err);
              setErrorMsg(msg);
              setErrorDetails(details);
              throw new Error(msg);
            }
          },
          onApprove: async (data: { orderID: string; payerID?: string }) => {
            setErrorMsg(null);
            setErrorDetails([]);
            setProcessingStage('Verifying capture & locking merchant inventory...');
            try {
              const captureRes = await apiClient.post<{ data: Order }>(
                '/payments/paypal/capture-order',
                {
                  paypalOrderId: data.orderID,
                  negotiationId: negotiation.id,
                  dealToken: negotiation.agreementSeal!.dealToken,
                  shippingDetails: shippingRef.current,
                }
              );

              const order = captureRes.data.data;
              navigate(`/order-success/${order.id}`);
            } catch (err: unknown) {
              setErrorMsg(extractErrorMessage(err, 'Payment capture failed. Please inspect details below and retry.'));
              setErrorDetails(extractErrorDetails(err));
            } finally {
              setIsProcessing(false);
              setProcessingStage('');
            }
          },
          onCancel: async (data) => {
            setIsProcessing(false);
            setProcessingStage('');
            const cancelledOrderId = data.orderID || activePaypalOrderIdRef.current || '';
            try {
              if (cancelledOrderId || negotiation.id) {
                await apiClient.post('/payments/paypal/cancel-order', {
                  paypalOrderId: cancelledOrderId,
                  negotiationId: negotiation.id,
                  reason: 'Buyer cancelled transaction inside PayPal window',
                });
              }
            } catch {
              // Non-blocking audit log update
            }
            setCancelMsg(
              `PayPal checkout was cancelled (Order: ${cancelledOrderId || 'pending'}). Held inventory was released safely, and you can retry before the deal lock expires.`
            );
          },
          onError: async (err: unknown) => {
            setIsProcessing(false);
            setProcessingStage('');
            const reason = extractErrorMessage(err, 'Transaction declined or gateway error.');
            try {
              await apiClient.post('/payments/paypal/fail-order', {
                paypalOrderId: activePaypalOrderIdRef.current || '',
                negotiationId: negotiation.id,
                reason,
              });
            } catch {
              // Non-blocking audit log update
            }
            setErrorMsg('PayPal could not complete authorization: ' + reason);
            setErrorDetails(extractErrorDetails(err));
          },
        })
        .render(container)
        .catch((renderErr) => {
          console.warn('Failed to render PayPal Buttons:', renderErr);
          buttonsRenderedRef.current = false;
          setSdkStatus('FAILED');
          setCheckoutMode('SANDBOX_SIMULATION');
        });
    } catch (e: unknown) {
      console.warn('PayPal button initialization error:', e);
      buttonsRenderedRef.current = false;
      setSdkStatus('FAILED');
      setCheckoutMode('SANDBOX_SIMULATION');
    }

    return () => {
      buttonsRenderedRef.current = false;
    };
  }, [sdkStatus, checkoutMode, negotiation, isExpired, navigate]);

  // Direct Sandbox Simulation (Executes exact same backend create + capture flow)
  const handleSimulatedPayment = async () => {
    if (!negotiation || !negotiation.agreementSeal) return;
    if (!fullName.trim() || !addressLine1.trim() || !city.trim() || !postalCode.trim()) {
      setErrorMsg('Please complete all shipping address fields before checking out.');
      return;
    }

    setIsProcessing(true);
    setErrorMsg(null);
    setErrorDetails([]);
    setCancelMsg(null);
    setProcessingStage('Initiating verified PayPal sandbox order...');

    try {
      // Step 1: Create Order on backend
      const createRes = await apiClient.post<{ data: { paypalOrderId: string } }>(
        '/payments/paypal/create-order',
        {
          negotiationId: negotiation.id,
          dealToken: negotiation.agreementSeal.dealToken,
        }
      );

      const paypalOrderId = createRes.data.data.paypalOrderId;
      activePaypalOrderIdRef.current = paypalOrderId;
      setProcessingStage('Simulating PayPal authorization & capturing funds...');

      // Step 2: Capture Order on backend
      const captureRes = await apiClient.post<{ data: Order }>(
        '/payments/paypal/capture-order',
        {
          paypalOrderId,
          negotiationId: negotiation.id,
          dealToken: negotiation.agreementSeal.dealToken,
          shippingDetails: {
            fullName,
            addressLine1,
            city,
            state,
            postalCode,
            country,
          },
        }
      );

      const order = captureRes.data.data;
      navigate(`/order-success/${order.id}`);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Payment processing failed'));
      setErrorDetails(extractErrorDetails(err));
    } finally {
      setIsProcessing(false);
      setProcessingStage('');
    }
  };

  // Authorized Delegated Autonomous Payment (PayPal Vault v3 Adapter only; never simulated)
  const handleDelegatedPayment = async () => {
    if (!negotiation || !negotiation.agreementSeal) return;
    if (!fullName.trim() || !addressLine1.trim() || !city.trim() || !postalCode.trim()) {
      setErrorMsg('Please complete all shipping address fields before executing payment.');
      return;
    }

    setIsProcessing(true);
    setErrorMsg(null);
    setErrorDetails([]);
    setCancelMsg(null);
    setProcessingStage('Executing verified PayPal Vault v3 delegated settlement...');

    try {
      const order = await mandateApi.executeDelegatedPayment({
        negotiationId: negotiation.id,
        dealToken: negotiation.agreementSeal.dealToken,
        shippingDetails: {
          fullName,
          addressLine1,
          city,
          state,
          postalCode,
          country,
        },
      });
      navigate(`/order-success/${order.id}`);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Delegated autonomous payment could not be executed.'));
      setErrorDetails(extractErrorDetails(err));
      mandateApi.getExecutionStatus(negotiation.id).then(setExecutionStatus).catch(() => {});
    } finally {
      setIsProcessing(false);
      setProcessingStage('');
    }
  };

  // Format countdown time MM:SS
  const formatCountdown = (totalSec: number) => {
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center space-y-3">
        <div className="w-10 h-10 rounded-full border-3 border-primary-600 border-t-transparent animate-spin" />
        <p className="text-xs text-charcoal-500 font-medium">Loading sealed agreement terms...</p>
      </div>
    );
  }

  if (!negotiation || !negotiation.agreementSeal) {
    return (
      <div className="text-center py-16 max-w-md mx-auto space-y-4">
        <div className="w-12 h-12 rounded-full bg-charcoal-100 text-charcoal-500 mx-auto flex items-center justify-center">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-bold text-charcoal-900">No sealed agreement found to settle</h2>
        <p className="text-xs text-charcoal-500 leading-relaxed">
          Negotiation terms must be agreed upon by buyer and seller agents before proceeding to settlement.
        </p>
        <Button variant="primary" size="md" onClick={() => navigate('/catalog')} className="mt-2">
          Browse Marketplace Catalog
        </Button>
      </div>
    );
  }

  const seal = negotiation.agreementSeal;
  const qty = seal.quantity || 1;
  const shippingCost = seal.shippingCost || 0;
  const originalListPrice = (product?.listPrice || seal.finalPrice) * qty;
  const savings = Math.max(0, originalListPrice - (seal.finalPrice - shippingCost));
  const savingsPct = originalListPrice > 0 ? Math.round((savings / originalListPrice) * 100) : 0;
  const sellerStore =
    negotiation.currentSellerStoreName ||
    product?.brand ||
    `Verified Merchant #${negotiation.sellerId.slice(-6)}`;

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Top Header & Breadcrumb */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-charcoal-100">
        <div>
          <div className="flex items-center gap-2 text-xs text-charcoal-500 mb-1">
            <Link
              to={`/arena/${negotiation.id}`}
              className="inline-flex items-center gap-1 hover:text-charcoal-800 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              Return to Negotiation Arena
            </Link>
            <span>•</span>
            <span className="text-charcoal-800 font-medium">Checkout & Settlement</span>
          </div>
          <h1 className="text-2xl font-black text-charcoal-900 tracking-tight">Settlement & PayPal Checkout</h1>
          <p className="text-xs text-charcoal-500 mt-0.5">
            Authorize capture for your agent-negotiated deal via official PayPal Orders API v2.
          </p>
        </div>

        {/* 15-Minute Countdown Lock Badge */}
        <div
          className={`flex items-center gap-3 px-4 py-2.5 rounded-xl border text-xs font-medium shadow-subtle transition-all ${
            isExpired
              ? 'bg-rose-50 border-rose-200 text-rose-700'
              : secondsRemaining < 120
              ? 'bg-amber-50 border-amber-200 text-amber-800 animate-pulse'
              : 'bg-emerald-50 border-emerald-200 text-emerald-800'
          }`}
        >
          <Clock className="w-4 h-4 shrink-0" />
          <div>
            <div className="text-[10px] uppercase font-bold tracking-wider opacity-80">
              {isExpired ? 'Price Lock Expired' : 'Deal Terms Locked'}
            </div>
            <div className="font-mono text-sm font-black">
              {isExpired ? 'EXPIRED' : `${formatCountdown(secondsRemaining)} remaining`}
            </div>
          </div>
        </div>
      </div>

      {/* Notifications / Alerts */}
      {isExpired && (
        <Alert type="error" title="Agreement Seal Expired">
          The 15-minute price lock window for this agreement has expired. Return to the Negotiation Arena to request an
          updated offer.
        </Alert>
      )}

      {errorMsg && (
        <Alert type="error" title="PayPal Checkout Diagnostic">
          <div className="space-y-2">
            <p className="font-medium">{errorMsg}</p>
            {errorDetails.length > 0 && (
              <div className="p-2.5 rounded-lg bg-white/80 border border-rose-200 font-mono text-[11px] space-y-1 text-rose-900">
                {errorDetails.map((line, i) => (
                  <div key={i}>{line}</div>
                ))}
              </div>
            )}
            <div className="pt-1 flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => {
                  setErrorMsg(null);
                  setErrorDetails([]);
                  if (checkoutMode === 'SANDBOX_SIMULATION' && sdkStatus === 'READY') {
                    setCheckoutMode('PAYPAL_SDK');
                  }
                }}
              >
                Retry PayPal Checkout
              </Button>
            </div>
          </div>
        </Alert>
      )}

      {cancelMsg && (
        <Alert type="warning" title="Authorization Cancelled">
          {cancelMsg}
        </Alert>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Shipping & Security Safeguards */}
        <div className="lg:col-span-7 space-y-6">
          {/* Shipping Form Card */}
          <Card className="p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-charcoal-100">
              <div className="flex items-center gap-2">
                <Truck className="w-4 h-4 text-primary-600" />
                <h3 className="font-bold text-sm text-charcoal-900">Shipping Destination</h3>
              </div>
              <Badge variant="primary">Fast Express Delivery</Badge>
            </div>

            <div className="space-y-3.5">
              <Input
                label="Recipient Full Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="First and last name"
                disabled={isProcessing || isExpired}
              />
              <Input
                label="Street Address"
                value={addressLine1}
                onChange={(e) => setAddressLine1(e.target.value)}
                placeholder="Street address, apartment, or suite"
                disabled={isProcessing || isExpired}
              />
              <div className="grid grid-cols-3 gap-3">
                <Input
                  label="City"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  disabled={isProcessing || isExpired}
                />
                <Input
                  label="State / Province"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  disabled={isProcessing || isExpired}
                />
                <Input
                  label="Postal Code"
                  value={postalCode}
                  onChange={(e) => setPostalCode(e.target.value)}
                  disabled={isProcessing || isExpired}
                />
              </div>
              <Input
                label="Country / Region"
                value={country}
                onChange={(e) => setCountry(e.target.value)}
                disabled={isProcessing || isExpired}
              />
            </div>
          </Card>

          {/* Cryptographic Deal Seal & Security Safeguards */}
          <Card className="p-6 space-y-4 bg-charcoal-50/60 border-charcoal-200/70">
            <div className="flex items-center gap-2 text-charcoal-900 font-bold text-sm">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>TradeTwin Settlement Safeguards</span>
            </div>

            <div className="space-y-3 text-xs text-charcoal-600">
              <div className="flex items-start gap-2.5">
                <Lock className="w-3.5 h-3.5 text-primary-600 shrink-0 mt-0.5" />
                <div>
                  <strong className="text-charcoal-900">Cryptographically Sealed Deal Token:</strong>
                  <div className="font-mono text-[11px] text-charcoal-600 bg-white px-2 py-1 rounded border border-charcoal-200 mt-1 break-all">
                    {seal.dealToken}
                  </div>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <Shield className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                <p>
                  <strong className="text-charcoal-900">Server-Side Price Authority:</strong> Final payable amount is
                  recalculated strictly on the backend against the sealed MongoDB agreement and merchant inventory.
                  Client-side tampering is impossible.
                </p>
              </div>

              <div className="flex items-start gap-2.5">
                <CreditCard className="w-3.5 h-3.5 text-blue-600 shrink-0 mt-0.5" />
                <p>
                  <strong className="text-charcoal-900">Human-in-the-Loop Payment:</strong> AI agents negotiate terms,
                  but payment authorization and capture is exclusively executed by your explicit action via PayPal
                  Orders v2.
                </p>
              </div>
            </div>
          </Card>
        </div>

        {/* Right Column: Order Summary & PayPal Checkout Container */}
        <div className="lg:col-span-5 space-y-6">
          <Card className="p-6 space-y-6 shadow-card">
            <div>
              <div className="flex items-center justify-between pb-3 border-b border-charcoal-100">
                <div className="flex items-center gap-2">
                  <ShoppingBag className="w-4 h-4 text-primary-600" />
                  <h3 className="font-bold text-sm text-charcoal-900">Deal Summary</h3>
                </div>
                <Badge variant="success">Deal Sealed</Badge>
              </div>

              {/* Product Info Thumbnail */}
              <div className="flex items-center gap-3.5 py-4 border-b border-charcoal-100">
                <div className="w-16 h-16 rounded-xl bg-charcoal-100 border border-charcoal-200 overflow-hidden shrink-0">
                  {product?.images?.[0] ? (
                    <img src={product.images[0]} alt={product.title} className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-charcoal-400">
                      <ShoppingBag className="w-6 h-6" />
                    </div>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <h4 className="font-bold text-xs text-charcoal-900 truncate">{product?.title || 'Negotiated Item'}</h4>
                  <div className="flex items-center gap-1.5 text-[11px] text-charcoal-500 mt-0.5">
                    <Building className="w-3 h-3 text-charcoal-400" />
                    <span className="truncate">{sellerStore}</span>
                  </div>
                  {product?.condition && (
                    <span className="inline-block text-[10px] text-charcoal-500 bg-charcoal-100 px-1.5 py-0.5 rounded mt-1">
                      {product.condition}
                    </span>
                  )}
                </div>
              </div>

              {/* Pricing Breakdown */}
              <div className="space-y-2.5 text-xs py-4 border-b border-charcoal-100">
                <div className="flex justify-between text-charcoal-600">
                  <span>Catalog List Price</span>
                  <span className="line-through text-charcoal-400">{formatCurrency(originalListPrice)}</span>
                </div>

                {savings > 0 && (
                  <div className="flex justify-between text-emerald-600 font-semibold">
                    <span>Agent Negotiated Savings ({savingsPct}%)</span>
                    <span>-{formatCurrency(savings)}</span>
                  </div>
                )}

                <div className="flex justify-between text-charcoal-600">
                  <span>Quantity</span>
                  <span className="font-medium text-charcoal-800">{qty} unit{qty > 1 ? 's' : ''}</span>
                </div>

                {seal.agreementId && (
                  <div className="flex justify-between text-charcoal-600">
                    <span>Agreement ID</span>
                    <span className="font-mono text-[11px] text-charcoal-800">{seal.agreementId}</span>
                  </div>
                )}

                <div className="flex justify-between text-charcoal-600">
                  <span>Shipping & Delivery</span>
                  <span className={`font-semibold ${shippingCost <= 0 ? 'text-emerald-600' : 'text-charcoal-800'}`}>
                    {shippingCost <= 0 ? 'FREE Express' : formatCurrency(shippingCost)}
                  </span>
                </div>

                <div className="flex justify-between text-charcoal-600">
                  <span>Warranty Included</span>
                  <span className="font-medium text-charcoal-800">{product?.warrantyInfo || 'Extended Coverage'}</span>
                </div>
              </div>

              {/* Total Due */}
              <div className="flex items-center justify-between pt-4">
                <div>
                  <span className="text-xs text-charcoal-500 block">Total Payable</span>
                  <span className="text-[10px] text-charcoal-400">Guaranteed by Agreement Seal</span>
                </div>
                <div className="text-right">
                  <span className="text-2xl font-black text-primary-600">{formatCurrency(seal.finalPrice)}</span>
                  <span className="text-[10px] text-charcoal-500 block uppercase font-mono">{seal.currency}</span>
                </div>
              </div>
            </div>

            {/* Spend Mandate & Payment Execution Mode Distinction */}
            {executionStatus && (
              <div className="space-y-2.5 pt-3 border-t border-charcoal-100">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-charcoal-800 flex items-center gap-1.5">
                    <ShieldCheck className="w-4 h-4 text-primary-600" />
                    Payment Execution Policy
                  </span>
                  <Badge
                    variant={
                      executionStatus.paymentExecutionMode === 'DELEGATED_AUTONOMOUS_PAYMENT' ? 'purple' : 'primary'
                    }
                    size="sm"
                  >
                    {executionStatus.paymentExecutionMode === 'DELEGATED_AUTONOMOUS_PAYMENT'
                      ? 'Delegated Autonomous Adapter'
                      : 'Supervised PayPal Checkout'}
                  </Badge>
                </div>

                {/* Mandate Compliance Summary */}
                <div
                  className={`p-3 rounded-xl border text-[11px] space-y-1 ${
                    executionStatus.mandateValidation.compliant
                      ? 'bg-emerald-50/70 border-emerald-200 text-emerald-900'
                      : 'bg-rose-50 border-rose-200 text-rose-900'
                  }`}
                >
                  <div className="flex items-center justify-between font-bold">
                    <span>
                      Mandate: {executionStatus.mandateValidation.mandateName || 'Standard Supervised Checkout'}
                    </span>
                    <span>
                      {executionStatus.mandateValidation.compliant ? 'COMPLIANT' : 'BLOCKED'}
                    </span>
                  </div>
                  {executionStatus.mandateValidation.maxAmountPerTransaction !== undefined &&
                    executionStatus.mandateValidation.maxAmountPerTransaction !== null && (
                      <div className="text-[10px] opacity-85">
                        Max/Tx: {formatCurrency(executionStatus.mandateValidation.maxAmountPerTransaction)} • Daily:{' '}
                        {formatCurrency(executionStatus.mandateValidation.projectedDailyTotal || 0)} /{' '}
                        {formatCurrency(executionStatus.mandateValidation.dailySpendingLimit || 0)}
                      </div>
                    )}
                  {!executionStatus.mandateValidation.compliant &&
                    executionStatus.mandateValidation.violations.map((v, idx) => (
                      <div key={idx} className="font-medium text-rose-800">
                        • {v}
                      </div>
                    ))}
                </div>

                {executionStatus.paymentExecutionMode === 'SUPERVISED_PAYPAL_CHECKOUT' &&
                  executionStatus.preparedPaypalOrderId && (
                    <div className="px-3 py-2 rounded-lg bg-blue-50/70 border border-blue-200 text-[11px] text-blue-900 flex items-center justify-between">
                      <span>Auto-Prepared PayPal Order:</span>
                      <span className="font-mono font-bold">{executionStatus.preparedPaypalOrderId}</span>
                    </div>
                  )}

                {executionStatus.paymentExecutionMode === 'DELEGATED_AUTONOMOUS_PAYMENT' && (
                  <div
                    className={`p-3 rounded-xl border text-[11px] space-y-2 ${
                      executionStatus.delegatedCapability.eligible
                        ? 'bg-purple-50 border-purple-200 text-purple-900'
                        : 'bg-amber-50 border-amber-200 text-amber-900'
                    }`}
                  >
                    <div className="flex items-center justify-between font-bold">
                      <span>{executionStatus.delegatedCapability.adapterName}</span>
                      <span className="font-mono text-[10px]">{executionStatus.delegatedCapability.reasonCode}</span>
                    </div>
                    <p>{executionStatus.delegatedCapability.message}</p>
                    {executionStatus.delegatedCapability.eligible ? (
                      <Button
                        type="button"
                        variant="primary"
                        size="md"
                        className="w-full"
                        onClick={handleDelegatedPayment}
                        isLoading={isProcessing}
                        disabled={isExpired || isProcessing || !executionStatus.mandateValidation.compliant}
                      >
                        Execute Authorized Delegated Vault Payment
                      </Button>
                    ) : (
                      <p className="text-[10px] opacity-85 font-medium">
                        Delegated execution is strictly blocked when account Vault v3 access or valid buyer Vault credentials are unavailable. Complete authorization below via Supervised PayPal Checkout instead.
                      </p>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Unified PayPal Checkout Flow */}
            <div className="space-y-4 pt-3 border-t border-charcoal-100">
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold text-charcoal-800">Continue securely with PayPal</span>
                <Badge variant="primary" size="sm">Supervised Orders v2</Badge>
              </div>

              {/* Processing Loader */}
              {isProcessing && (
                <div className="p-4 rounded-xl bg-blue-50 border border-blue-200 text-center space-y-2">
                  <div className="w-6 h-6 rounded-full border-2 border-primary-600 border-t-transparent animate-spin mx-auto" />
                  <p className="text-xs font-semibold text-primary-800">{processingStage || 'Processing payment...'}</p>
                  <p className="text-[10px] text-primary-600">Verifying agreement seal & merchant stock lock</p>
                </div>
              )}

              {/* Primary PayPal JS SDK Buttons */}
              {checkoutMode === 'PAYPAL_SDK' && (
                <div className="space-y-3">
                  {sdkStatus === 'LOADING' && (
                    <div className="py-6 text-center text-xs text-charcoal-400">
                      <div className="w-5 h-5 rounded-full border-2 border-primary-600 border-t-transparent animate-spin mx-auto mb-2" />
                      Preparing PayPal Sandbox Checkout...
                    </div>
                  )}

                  {sdkStatus === 'FAILED' && (
                    <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-800 space-y-2">
                      <p className="font-medium">
                        PayPal JavaScript SDK CDN could not load in this browser context.
                      </p>
                    </div>
                  )}

                  <div
                    ref={paypalContainerRef}
                    id="paypal-button-container"
                    className={`min-h-[48px] ${isProcessing || isExpired ? 'opacity-50 pointer-events-none' : ''}`}
                  />
                </div>
              )}

              {/* Fallback CTA when SDK CDN fails or server runs in dev simulation without live secret */}
              {(checkoutMode === 'SANDBOX_SIMULATION' || sdkStatus === 'FAILED') && (
                <div className="space-y-2.5">
                  <Button
                    variant="primary"
                    size="lg"
                    className="w-full bg-[#0070BA] hover:bg-[#005ea6] text-white font-bold py-3.5 rounded-xl shadow-card"
                    onClick={handleSimulatedPayment}
                    isLoading={isProcessing}
                    disabled={isExpired || isProcessing}
                    leftIcon={<ShieldCheck className="w-5 h-5" />}
                  >
                    Continue securely with PayPal
                  </Button>
                  <p className="text-[11px] text-center text-charcoal-500">
                    Dev sandbox fallback: executes backend PayPal order creation, verification, and capture rules.
                  </p>
                </div>
              )}

              <div className="pt-2 text-center text-[10px] text-charcoal-400 flex items-center justify-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                <span>Single Sandbox Merchant Checkout • Inventory Held Only on Approval</span>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};