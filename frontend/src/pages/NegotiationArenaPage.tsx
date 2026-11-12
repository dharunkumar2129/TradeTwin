import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import type {
  Negotiation,
  Product,
  Turn,
  FairDealMetrics,
  AgreementSeal,
  NegotiationStatus,
  FairDealComparisonResult
} from '../types';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { formatCurrency, extractErrorMessage } from '../lib/utils';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Bot,
  Scale,
  Sparkles,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  Clock,
  ArrowLeft,
  ArrowRight,
  Gift,
  Truck,
  Store,
  Users,
  ChevronDown,
  ChevronUp,
  Check,
  Info,
  TrendingDown,
  Award,
  Star,
  ThumbsUp,
  Layers,
  RefreshCw
} from 'lucide-react';

export const NegotiationArenaPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [negotiation, setNegotiation] = useState<Negotiation | null>(null);
  const [product, setProduct] = useState<Product | null>(null);
  const [turns, setTurns] = useState<Turn[]>([]);
  const [fairDealMetrics, setFairDealMetrics] = useState<FairDealMetrics | null>(null);
  const [comparisonResult, setComparisonResult] = useState<FairDealComparisonResult | null>(null);
  const [thinkingAgent, setThinkingAgent] = useState<{ speaker: string; text: string } | null>(null);
  const [fallbackBanner, setFallbackBanner] = useState<string | null>(null);
  const [agreementSeal, setAgreementSeal] = useState<AgreementSeal | null>(null);
  const [isRejected, setIsRejected] = useState<boolean>(false);
  const [rejectionReason, setRejectionReason] = useState<string>('');
  const [timeLeftSec, setTimeLeftSec] = useState<number>(900);
  const [activeTab, setActiveTab] = useState<'arena' | 'comparison'>('arena');
  const [selectedOfferId, setSelectedOfferId] = useState<string | null>(null);
  const [isApprovingDeal, setIsApprovingDeal] = useState<boolean>(false);
  const [expandedExplanationOfferId, setExpandedExplanationOfferId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const turnsContainerRef = useRef<HTMLDivElement>(null);
  const terminalRef = useRef<boolean>(false);
  const navigate = useNavigate();

  const getStatusBadgeInfo = (status?: NegotiationStatus) => {
    switch (status) {
      case 'CREATED':
      case 'INITIATED':
        return { label: 'Session Initialized', variant: 'default' as const, pulse: false };
      case 'DISCOVERING':
        return { label: 'Discovering Sellers', variant: 'purple' as const, pulse: true };
      case 'NEGOTIATING':
      case 'IN_PROGRESS':
        return { label: 'Negotiation Active', variant: 'primary' as const, pulse: true };
      case 'OFFER_RECEIVED':
        return { label: 'Quotes Received', variant: 'primary' as const, pulse: false };
      case 'COUNTEROFFERED':
        return { label: 'Counter-Offer Active', variant: 'primary' as const, pulse: true };
      case 'ACCEPTED':
      case 'AGREED':
        return { label: 'Agreement Sealed', variant: 'success' as const, pulse: false };
      case 'COMPLETED':
        return { label: 'Completed & Settled', variant: 'success' as const, pulse: false };
      case 'REJECTED':
      case 'CANCELLED':
        return { label: 'Terminated (No Deal)', variant: 'danger' as const, pulse: false };
      case 'EXPIRED':
        return { label: 'Deal Expired', variant: 'warning' as const, pulse: false };
      default:
        return { label: status || 'Connecting', variant: 'default' as const, pulse: false };
    }
  };

  useEffect(() => {
    const container = turnsContainerRef.current;
    if (!container) return;
    const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight;
    if (distanceFromBottom < 140 || turns.length <= 1) {
      container.scrollTo({
        top: container.scrollHeight,
        behavior: 'smooth',
      });
    }
  }, [turns, thinkingAgent]);

  const refreshNegotiation = () => {
    if (!id) return;
    apiClient.get<{ data: Negotiation }>(`/negotiations/${id}`)
      .then((res) => {
        const neg = res.data.data;
        setNegotiation(neg);
        if (neg.turns && neg.turns.length > 0) setTurns(neg.turns);
        if (neg.fairDealMetrics) setFairDealMetrics(neg.fairDealMetrics);
        if (neg.agreementSeal) setAgreementSeal(neg.agreementSeal);
        if (neg.status === 'REJECTED' || neg.status === 'CANCELLED' || neg.status === 'EXPIRED') {
          terminalRef.current = true;
          setIsRejected(neg.status === 'REJECTED' || neg.status === 'CANCELLED');
          setThinkingAgent(null);
        } else if (neg.status === 'ACCEPTED' || neg.status === 'AGREED' || neg.status === 'COMPLETED') {
          terminalRef.current = true;
          setThinkingAgent(null);
        }
        if (neg.productId) {
          apiClient.get<{ data: Product }>(`/catalog/${neg.productId}`)
            .then((prodRes) => setProduct(prodRes.data.data))
            .catch(() => {});
        }
      })
      .catch(console.error);

    apiClient.get<{ data: FairDealComparisonResult }>(`/negotiations/${id}/compare-offers`)
      .then((res) => setComparisonResult(res.data.data))
      .catch(() => {});
  };

  useEffect(() => {
    if (!id) return;
    terminalRef.current = false;
    setIsLoading(true);
    setLoadError(null);

    // Load initial negotiation details and comparison
    apiClient.get<{ data: Negotiation }>(`/negotiations/${id}`)
      .then((res) => {
        const neg = res.data.data;
        setNegotiation(neg);
        setTurns(neg.turns || []);
        if (neg.fairDealMetrics) setFairDealMetrics(neg.fairDealMetrics);
        if (neg.agreementSeal) setAgreementSeal(neg.agreementSeal);
        if (neg.status === 'REJECTED' || neg.status === 'CANCELLED') setIsRejected(true);

        return apiClient.get<{ data: Product }>(`/catalog/${neg.productId}`);
      })
      .then((res) => setProduct(res.data.data))
      .catch((err: unknown) => {
        setLoadError(extractErrorMessage(err, 'Unable to load negotiation session details.'));
      })
      .finally(() => setIsLoading(false));

    apiClient.get<{ data: FairDealComparisonResult }>(`/negotiations/${id}/compare-offers`)
      .then((res) => setComparisonResult(res.data.data))
      .catch(() => {});

    // SSE Stream
    const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
    const streamUrl = `${apiBase}/negotiations/${id}/stream`;
    const eventSource = new EventSource(streamUrl);

    // Event listeners
    eventSource.addEventListener('AGENT_THINKING', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        setThinkingAgent({ speaker: data.speaker, text: data.payload });
      } catch (err) {
        console.error('Error parsing AGENT_THINKING', err);
      }
    });

    eventSource.addEventListener('SELLERS_DISCOVERED', () => {
      refreshNegotiation();
    });

    eventSource.addEventListener('SELLER_AGENT_STARTED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        if (data?.payload?.storeName) {
          setThinkingAgent({
            speaker: 'SELLER_AGENT',
            text: `${data.payload.storeName} is reviewing stock and preparing an opening offer...`,
          });
        }
        refreshNegotiation();
      } catch {
        refreshNegotiation();
      }
    });

    eventSource.addEventListener('SELLER_FALLBACK_TRIGGERED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        if (data?.payload?.storeName) {
          setFallbackBanner(
            `${data.payload.storeName} did not finalize (${data.payload.reason || 'terms unmatched'}). Checking next eligible merchant...`
          );
        }
        refreshNegotiation();
      } catch {
        refreshNegotiation();
      }
    });

    eventSource.addEventListener('NEXT_SELLER_STARTED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        if (data?.payload?.message) {
          setFallbackBanner(data.payload.message);
        }
        refreshNegotiation();
      } catch {
        refreshNegotiation();
      }
    });

    eventSource.addEventListener('SELLER_TRANSITION', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        if (data?.payload?.message) {
          setFallbackBanner(data.payload.message);
        }
        refreshNegotiation();
      } catch {
        refreshNegotiation();
      }
    });

    eventSource.addEventListener('NO_DEAL_EXHAUSTED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        setThinkingAgent(null);
        setIsRejected(true);
        setRejectionReason(data?.payload?.summary || 'All eligible sellers exhausted without agreement.');
        refreshNegotiation();
      } catch {
        setIsRejected(true);
        refreshNegotiation();
      }
    });

    eventSource.addEventListener('SELLER_INITIAL_OFFER', () => {
      setThinkingAgent(null);
      refreshNegotiation();
    });

    eventSource.addEventListener('BUYER_COUNTEROFFER', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        const newTurn = data.payload as Turn;
        setThinkingAgent(null);
        setTurns((prev) => {
          if (prev.some((t) => t.turnNumber === newTurn.turnNumber)) return prev;
          return [...prev, newTurn];
        });
        refreshNegotiation();
      } catch (err) {
        console.error('Error parsing BUYER_COUNTEROFFER', err);
      }
    });

    eventSource.addEventListener('SELLER_COUNTEROFFER', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        const newTurn = data.payload as Turn;
        setThinkingAgent(null);
        setTurns((prev) => {
          if (prev.some((t) => t.turnNumber === newTurn.turnNumber)) return prev;
          return [...prev, newTurn];
        });
        refreshNegotiation();
      } catch (err) {
        console.error('Error parsing SELLER_COUNTEROFFER', err);
      }
    });

    eventSource.addEventListener('OFFER_PROPOSED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        const newTurn = data.payload as Turn;
        setThinkingAgent(null);
        setTurns((prev) => {
          if (prev.some((t) => t.turnNumber === newTurn.turnNumber)) return prev;
          return [...prev, newTurn];
        });
        refreshNegotiation();
      } catch (err) {
        console.error('Error parsing OFFER_PROPOSED', err);
      }
    });

    eventSource.addEventListener('FAIRDEAL_METRIC', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        setFairDealMetrics(data.payload as FairDealMetrics);
      } catch (err) {
        console.error('Error parsing FAIRDEAL_METRIC', err);
      }
    });

    eventSource.addEventListener('DEAL_SELECTED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        setThinkingAgent(null);
        setAgreementSeal(data.payload as AgreementSeal);
        refreshNegotiation();
      } catch (err) {
        console.error('Error parsing DEAL_SELECTED', err);
      }
    });

    eventSource.addEventListener('AGREED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        setThinkingAgent(null);
        setAgreementSeal(data.payload as AgreementSeal);
        refreshNegotiation();
      } catch (err) {
        console.error('Error parsing AGREED', err);
      }
    });

    eventSource.addEventListener('SELLER_REJECTED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        setThinkingAgent(null);
        setIsRejected(true);
        setRejectionReason(typeof data.payload === 'string' ? data.payload : 'Negotiation terminated without consensus');
        refreshNegotiation();
      } catch (err) {
        console.error('Error parsing SELLER_REJECTED', err);
      }
    });

    eventSource.addEventListener('REJECTED', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        setThinkingAgent(null);
        setIsRejected(true);
        setRejectionReason(typeof data.payload === 'string' ? data.payload : 'Negotiation terminated without consensus');
        refreshNegotiation();
      } catch (err) {
        console.error('Error parsing REJECTED', err);
      }
    });

    // Lightweight polling safety net while session is actively deliberating
    const pollInterval = setInterval(() => {
      if (terminalRef.current) {
        clearInterval(pollInterval);
        return;
      }
      refreshNegotiation();
    }, 3000);

    return () => {
      clearInterval(pollInterval);
      eventSource.close();
    };
  }, [id]);

  useEffect(() => {
    if (!agreementSeal) return;
    const interval = setInterval(() => {
      const exp = new Date(agreementSeal.expiresAt).getTime();
      const now = Date.now();
      const diffSec = Math.max(0, Math.floor((exp - now) / 1000));
      setTimeLeftSec(diffSec);
    }, 1000);
    return () => clearInterval(interval);
  }, [agreementSeal]);

  const handleApproveDeal = async (offerId: string) => {
    if (!id) return;
    setIsApprovingDeal(true);
    try {
      const res = await apiClient.post<{ data: Negotiation }>(`/negotiations/${id}/select-deal`, {
        offerId,
      });
      const neg = res.data.data;
      setNegotiation(neg);
      if (neg.agreementSeal) setAgreementSeal(neg.agreementSeal);
      setSelectedOfferId(offerId);
      setActiveTab('arena');
    } catch (err) {
      console.error('Error approving deal', err);
    } finally {
      setIsApprovingDeal(false);
    }
  };

  const formatTimer = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const statusInfo = getStatusBadgeInfo(negotiation?.status);
  const sellerQueueCount = negotiation?.sellerQueue?.length || negotiation?.participatingSellerIds?.length || 1;
  const activeSellerIndex = negotiation?.currentSellerIndex || 0;
  const remainingQueueCount = Math.max(0, sellerQueueCount - activeSellerIndex - 1);
  const activeStoreName =
    negotiation?.currentSellerStoreName ||
    product?.brand ||
    'Verified Merchant';

  // Price Trajectory
  const priceHistory = [
    { label: 'List Price', price: product?.listPrice || 0, badge: 'Catalog' },
    ...turns.map((t) => ({
      label: `Round ${t.turnNumber} (${t.speaker === 'BUYER_AGENT' ? 'Buyer Counter' : 'Seller Quote'})`,
      price: t.proposal.price,
      badge: t.action,
    })),
    ...(agreementSeal ? [{ label: 'Final Agreement', price: agreementSeal.finalPrice, badge: 'SEALED' }] : []),
  ];

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto py-20 text-center space-y-4">
        <div className="w-12 h-12 rounded-2xl bg-primary-50 border border-primary-200 flex items-center justify-center mx-auto">
          <RefreshCw className="w-6 h-6 text-primary-600 animate-spin" />
        </div>
        <h2 className="text-lg font-bold text-charcoal-900">Synchronizing Live Negotiation Arena</h2>
        <p className="text-xs text-charcoal-500">Connecting to agent event stream and retrieving FairDeal comparison matrix...</p>
      </div>
    );
  }

  if (loadError || !negotiation) {
    return (
      <div className="max-w-2xl mx-auto py-16 space-y-5">
        <Alert type="error" title="Negotiation Unavailable">
          {loadError || 'This negotiation session could not be located.'}
        </Alert>
        <Button
          variant="primary"
          size="md"
          leftIcon={<ArrowLeft className="w-4 h-4" />}
          onClick={() => navigate('/catalog')}
        >
          Return to Marketplace
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12">
      {/* Top Banner / Matchup */}
      <div className="bg-white rounded-2xl border border-charcoal-200/90 shadow-card p-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-charcoal-100">
          <div className="flex items-start gap-4">
            {(product?.images?.[0] || negotiation?.productImage) && (
              <div className="w-16 h-16 rounded-xl bg-charcoal-100 border border-charcoal-200 overflow-hidden shrink-0 hidden sm:block">
                <img
                  src={product?.images?.[0] || negotiation?.productImage}
                  alt={product?.title || negotiation?.productTitle || 'Product'}
                  className="w-full h-full object-cover"
                />
              </div>
            )}
            <div>
              <div className="flex items-center gap-2 mb-1.5 flex-wrap">
                <span
                  className={`w-2.5 h-2.5 rounded-full ${
                    statusInfo.pulse
                      ? 'bg-amber-400 animate-pulse'
                      : negotiation?.status === 'ACCEPTED' || negotiation?.status === 'AGREED' || negotiation?.status === 'COMPLETED'
                      ? 'bg-emerald-500'
                      : 'bg-primary-500'
                  }`}
                />
                <span className="text-xs font-bold uppercase tracking-wider text-charcoal-500">
                  FairDeal Live Negotiation Arena
                </span>
                <Badge variant={statusInfo.variant} size="sm">
                  {statusInfo.label}
                </Badge>
                <Badge variant="warning" size="sm">DEMO DATA</Badge>
                {sellerQueueCount > 1 && (
                  <Badge variant="purple" size="sm">
                    <Users className="w-3 h-3 inline mr-1" />
                    Seller {Math.min(activeSellerIndex + 1, sellerQueueCount)} of {sellerQueueCount}
                    {remainingQueueCount > 0 ? ` (${remainingQueueCount} backup)` : ''}
                  </Badge>
                )}
              </div>
              <h1 className="text-2xl font-extrabold text-charcoal-900 tracking-tight">
                {product?.title || negotiation?.productTitle || 'Negotiation Session'}
              </h1>
              {negotiation?.buyerPrompt && (
                <p className="text-xs text-charcoal-500 mt-1">
                  Request: <span className="font-medium text-charcoal-700">{negotiation.buyerPrompt}</span>
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-4 bg-charcoal-50 px-4 py-2.5 rounded-xl border border-charcoal-200/80 shrink-0">
            <div>
              <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Catalog List</span>
              <span className="text-sm font-semibold text-charcoal-700 line-through">
                {product ? formatCurrency(product.listPrice) : '--'}
              </span>
            </div>
            <div className="w-px h-8 bg-charcoal-200" />
            <div>
              <span className="text-[10px] text-primary-600 block uppercase font-bold">Current Total</span>
              <span className="text-lg font-extrabold text-primary-600">
                {turns.length > 0
                  ? formatCurrency(turns[turns.length - 1].proposal.totalPayable ?? turns[turns.length - 1].proposal.price)
                  : '--'}
              </span>
            </div>
          </div>
        </div>

        {/* Distinct Visual Identities Strip */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-4 text-xs">
          {/* Buyer Agent Identity */}
          <div className="p-3.5 rounded-xl bg-gradient-to-br from-primary-50 to-indigo-50/70 border border-primary-200/80 shadow-subtle flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary-600 text-white flex items-center justify-center font-bold shadow-sm shrink-0">
              <Bot className="w-5 h-5" />
            </div>
            <div className="space-y-1">
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-primary-950 text-sm">Buyer Advocate AI</span>
                <span className="w-2 h-2 rounded-full bg-emerald-500" />
              </div>
              <p className="text-[11px] text-primary-800">
                Target: <span className="font-bold">{negotiation?.buyerPreferences?.targetPrice != null ? formatCurrency(negotiation.buyerPreferences.targetPrice) : '--'}</span>
                {' '}| Budget Ceiling: <span className="font-bold">{negotiation?.buyerPreferences?.maxCeilingPrice != null ? formatCurrency(negotiation.buyerPreferences.maxCeilingPrice) : 'Protected (Private)'}</span>
              </p>
              <span className="text-[10px] text-primary-600 block">Status: Optimizing Landed Value & Warranty</span>
            </div>
          </div>

          {/* Active Seller Agent Identity */}
          <div className="p-3.5 rounded-xl bg-gradient-to-br from-amber-50 to-orange-50/70 border border-amber-200/80 shadow-subtle flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-600 text-white flex items-center justify-center font-bold shadow-sm shrink-0">
              <Store className="w-5 h-5" />
            </div>
            <div className="space-y-1">
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-amber-950 text-sm">{activeStoreName}</span>
                <span className="w-2 h-2 rounded-full bg-amber-500" />
              </div>
              <p className="text-[11px] text-amber-800">
                In Stock: <span className="font-bold">{product?.availableInventory ?? 10} units</span> | Est: {product?.estimatedDeliveryDays || 3}d delivery
              </p>
              <span className="text-[10px] text-amber-700 block">
                Active Queue Slot: #{activeSellerIndex + 1} of {sellerQueueCount}
              </span>
            </div>
          </div>

          {/* Market Competition Monitor */}
          <div className="p-3.5 rounded-xl bg-gradient-to-br from-purple-50 to-pink-50/70 border border-purple-200/80 shadow-subtle flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-600 text-white flex items-center justify-center font-bold shadow-sm shrink-0">
              <Scale className="w-5 h-5" />
            </div>
            <div className="space-y-1">
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-purple-950 text-sm">FairDeal Engine</span>
                <Badge variant="purple" size="sm">MAUT Scoring</Badge>
              </div>
              <p className="text-[11px] text-purple-800">
                Active Round: <span className="font-bold">Round {turns.length} of {negotiation?.maxRounds || 3}</span>
              </p>
              <span className="text-[10px] text-purple-700 block">
                {comparisonResult ? `${comparisonResult.evaluations.length} merchant offers analyzed` : 'Calibrating multi-attribute scores...'}
              </span>
            </div>
          </div>
        </div>

        {/* Animated Automatic Fallback Banner */}
        <AnimatePresence>
          {(fallbackBanner || negotiation?.fallbackNotice) && !agreementSeal && (
            <motion.div
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              className="mt-4 p-3 rounded-xl bg-amber-50 border border-amber-200 flex items-start gap-2.5 text-xs text-amber-900"
            >
              <RefreshCw className="w-4 h-4 text-amber-600 shrink-0 mt-0.5 animate-spin" />
              <div>
                <span className="font-bold block">Automatic Seller Fallback Active</span>
                <span>{fallbackBanner || negotiation?.fallbackNotice}</span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Cross-Seller Prior Attempts Trail */}
        {negotiation?.failedSellerAttempts && negotiation.failedSellerAttempts.length > 0 && (
          <div className="mt-3 flex flex-wrap items-center gap-2 text-[11px]">
            <span className="text-charcoal-400 font-semibold uppercase tracking-wider">Previous Attempts:</span>
            {negotiation.failedSellerAttempts.map((att, i) => (
              <span
                key={`${att.sellerId}-${i}`}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-rose-50 text-rose-800 border border-rose-200/80"
                title={att.reason}
              >
                <XCircle className="w-3 h-3 text-rose-500 shrink-0" />
                <span className="font-semibold">{att.storeName}</span>
                <span className="text-rose-600">({att.reason})</span>
              </span>
            ))}
          </div>
        )}

        {/* Price & Term History Trajectory Bar */}
        {priceHistory.length > 1 && (
          <div className="mt-4 pt-3 border-t border-charcoal-100 flex items-center gap-2 overflow-x-auto text-xs py-1">
            <span className="text-[11px] font-bold uppercase tracking-wider text-charcoal-400 shrink-0 flex items-center gap-1">
              <TrendingDown className="w-3.5 h-3.5 text-emerald-600" /> Trajectory:
            </span>
            <div className="flex items-center gap-2 shrink-0">
              {priceHistory.map((step, idx) => (
                <React.Fragment key={idx}>
                  <div className="flex items-center gap-1.5 bg-charcoal-50 px-2.5 py-1 rounded-lg border border-charcoal-200/80 text-[11px]">
                    <span className="text-charcoal-500 font-medium">{step.label}:</span>
                    <span className="font-extrabold text-charcoal-900">{formatCurrency(step.price)}</span>
                    <Badge variant={step.badge === 'SEALED' ? 'success' : 'outline'} size="sm">
                      {step.badge}
                    </Badge>
                  </div>
                  {idx < priceHistory.length - 1 && <ArrowRight className="w-3 h-3 text-charcoal-300 shrink-0" />}
                </React.Fragment>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Navigation Tabs: Arena Chat vs Comparison Matrix */}
      <div className="flex items-center gap-3 border-b border-charcoal-200/80 pb-2">
        <button
          onClick={() => setActiveTab('arena')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'arena'
              ? 'bg-primary-600 text-white shadow-sm'
              : 'text-charcoal-600 hover:bg-charcoal-100'
          }`}
        >
          <Bot className="w-4 h-4" />
          Live Arena & Stream
        </button>

        <button
          onClick={() => setActiveTab('comparison')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'comparison'
              ? 'bg-primary-600 text-white shadow-sm'
              : 'text-charcoal-600 hover:bg-charcoal-100'
          }`}
        >
          <Layers className="w-4 h-4" />
          FairDeal Offer Comparison & Deal Approval
          {comparisonResult && (
            <span className="w-5 h-5 rounded-full bg-white/20 text-[10px] flex items-center justify-center font-extrabold ml-1">
              {comparisonResult.evaluations.length}
            </span>
          )}
        </button>
      </div>

      {/* TAB 1: Live Arena Chat & Stream */}
      {activeTab === 'arena' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-8 space-y-4">
            <Card className="p-6 h-[540px] flex flex-col justify-between overflow-hidden shadow-subtle border-charcoal-200/90">
              <div ref={turnsContainerRef} className="overflow-y-auto space-y-4 pr-2 flex-1">
                {turns.length === 0 && !thinkingAgent && (
                  <div className="h-full flex flex-col items-center justify-center text-center text-charcoal-400">
                    <Bot className="w-10 h-10 mb-2 animate-bounce text-primary-500" />
                    <p className="text-sm font-medium text-charcoal-600">Initializing FairDeal Multi-Agent Protocol...</p>
                    <p className="text-xs text-charcoal-400 mt-1">Discovering merchant agents and streaming telemetry</p>
                  </div>
                )}

                {turns.map((turn) => {
                  const isBuyer = turn.speaker === 'BUYER_AGENT';
                  const unitPrice = turn.proposal.unitPrice ?? turn.proposal.price;
                  const shipCost = turn.proposal.shippingCost ?? 0;
                  const totalPayable = turn.proposal.totalPayable ?? turn.proposal.price;
                  const offerQty = turn.proposal.quantity ?? 1;
                  const offerStatus = turn.proposal.offerStatus || turn.action;
                  return (
                    <motion.div
                      key={turn.turnNumber}
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.2 }}
                      className={`flex flex-col ${isBuyer ? 'items-start' : 'items-end'}`}
                    >
                      <div className="flex items-center gap-1.5 mb-1 px-1 flex-wrap">
                        <span className="text-[11px] font-semibold text-charcoal-600">
                          {isBuyer ? 'Buyer Advocate' : (turn.sellerStoreName || activeStoreName)}
                        </span>
                        <span className="text-[10px] text-charcoal-400">
                          • Round {turn.roundNumber || turn.proposal.roundNumber || turn.turnNumber}
                        </span>
                        <Badge
                          variant={
                            turn.action === 'ACCEPT'
                              ? 'success'
                              : turn.action === 'REJECT'
                              ? 'danger'
                              : isBuyer
                              ? 'primary'
                              : 'default'
                          }
                          size="sm"
                        >
                          {offerStatus}
                        </Badge>
                      </div>

                      <div
                        className={`max-w-lg p-4 rounded-2xl text-xs leading-relaxed shadow-subtle space-y-3 ${
                          isBuyer
                            ? 'bg-primary-50/90 text-charcoal-900 border border-primary-200/80 rounded-tl-sm'
                            : 'bg-white text-charcoal-900 border border-charcoal-200/90 rounded-tr-sm'
                        }`}
                      >
                        <p className="text-charcoal-800 text-[13px] leading-relaxed">{turn.agentReasoning}</p>

                        {/* Structured Offer Card */}
                        <div className="rounded-xl bg-white/90 border border-charcoal-200/80 p-3 space-y-2">
                          <div className="grid grid-cols-3 gap-2 text-[11px]">
                            <div>
                              <span className="text-charcoal-400 block">Unit Price</span>
                              <span className="font-bold text-charcoal-900">{formatCurrency(unitPrice)}</span>
                            </div>
                            <div>
                              <span className="text-charcoal-400 block">Shipping</span>
                              <span className={`font-bold ${shipCost <= 0 ? 'text-emerald-600' : 'text-charcoal-900'}`}>
                                {shipCost <= 0 ? 'FREE' : formatCurrency(shipCost)}
                              </span>
                            </div>
                            <div className="text-right">
                              <span className="text-charcoal-400 block">Total ({offerQty}x)</span>
                              <span className="font-extrabold text-primary-700">{formatCurrency(totalPayable)}</span>
                            </div>
                          </div>

                          <div className="flex flex-wrap items-center gap-1.5 pt-1.5 border-t border-charcoal-100 text-[11px] text-charcoal-600">
                            <span className="inline-flex items-center gap-1 bg-charcoal-50 px-2 py-0.5 rounded border border-charcoal-200/60">
                              <Truck className="w-3 h-3 text-emerald-600" /> {turn.proposal.deliveryDays}d delivery
                            </span>
                            <span className="inline-flex items-center gap-1 bg-charcoal-50 px-2 py-0.5 rounded border border-charcoal-200/60">
                              <ShieldCheck className="w-3 h-3 text-purple-600" /> {turn.proposal.warrantyMonths}m warranty
                            </span>
                            {turn.proposal.bonusPerks?.map((perk, idx) => (
                              <span key={idx} className="inline-flex items-center gap-1 bg-charcoal-50 px-1.5 py-0.5 rounded border border-charcoal-200/50">
                                <Gift className="w-3 h-3 text-amber-500" /> {perk}
                              </span>
                            ))}
                          </div>
                        </div>
                      </div>
                    </motion.div>
                  );
                })}

                {thinkingAgent && (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.96 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className={`flex flex-col ${thinkingAgent.speaker === 'BUYER_AGENT' ? 'items-start' : 'items-end'}`}
                  >
                    <div className="flex items-center gap-1.5 mb-1 px-1">
                      <span className="text-[11px] font-semibold text-charcoal-400">
                        {thinkingAgent.speaker === 'BUYER_AGENT' ? 'Buyer Advocate' : thinkingAgent.speaker === 'MARKETPLACE' ? 'Marketplace' : 'Merchant Agent'} is deliberating...
                      </span>
                    </div>
                    <div className="p-3 rounded-xl bg-charcoal-100 text-charcoal-600 text-xs italic flex items-center gap-2">
                      <Sparkles className="w-3.5 h-3.5 text-primary-500 animate-spin" />
                      {thinkingAgent.text}
                    </div>
                  </motion.div>
                )}
              </div>

              <div className="pt-3 border-t border-charcoal-100 flex items-center justify-between text-xs text-charcoal-400">
                <span className="flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-charcoal-400" />
                  Round {turns.length} of {negotiation?.maxRounds || 3}
                </span>
                <span>Telemetry Connected • Historical Events Persisted</span>
              </div>
            </Card>
          </div>

          {/* Right: Transparent FairDeal Evaluation & Settlement Panel */}
          <div className="lg:col-span-4 space-y-6">
            <Card className="p-6 border-charcoal-200/90 shadow-subtle">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Scale className="w-4 h-4 text-primary-600" />
                  <h3 className="font-bold text-sm text-charcoal-900">FairDeal Scoring Engine</h3>
                </div>
                <Badge variant="purple" size="sm">Transparent MAUT</Badge>
              </div>

              {fairDealMetrics ? (
                <div className="space-y-4">
                  {/* Gauge */}
                  <div className="flex items-center justify-center py-1">
                    <div className="w-24 h-24 rounded-full bg-gradient-to-tr from-primary-600 via-indigo-600 to-purple-600 flex flex-col items-center justify-center text-white shadow-card">
                      <span className="text-2xl font-extrabold">{fairDealMetrics.overallDealScore || fairDealMetrics.fairDealScore}</span>
                      <span className="text-[10px] uppercase font-bold tracking-wider opacity-85">Deal Score</span>
                    </div>
                  </div>

                  {/* 4 Core Dimensions */}
                  <div className="grid grid-cols-2 gap-2 text-xs">
                    <div className="p-2.5 rounded-lg bg-charcoal-50 border border-charcoal-100">
                      <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Buyer Satisfaction</span>
                      <span className="text-sm font-bold text-primary-700">{fairDealMetrics.buyerSatisfactionScore || 85.0}/100</span>
                    </div>
                    <div className="p-2.5 rounded-lg bg-charcoal-50 border border-charcoal-100">
                      <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Offer Quality</span>
                      <span className="text-sm font-bold text-emerald-700">{fairDealMetrics.offerQualityScore || 88.0}/100</span>
                    </div>
                    <div className="p-2.5 rounded-lg bg-charcoal-50 border border-charcoal-100">
                      <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Seller Compliance</span>
                      <span className="text-sm font-bold text-amber-700">{fairDealMetrics.sellerPolicyCompliance || 82.0}/100</span>
                    </div>
                    <div className="p-2.5 rounded-lg bg-charcoal-50 border border-charcoal-100">
                      <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Price Equitability</span>
                      <span className="text-sm font-bold text-purple-700">{Math.round((fairDealMetrics.priceEquitability || 0.9) * 100)}%</span>
                    </div>
                  </div>

                  {/* Surplus Split Bars */}
                  <div className="space-y-2 text-xs pt-1">
                    <div className="flex justify-between text-charcoal-600">
                      <span>Buyer Surplus Share</span>
                      <span className="font-bold text-charcoal-900">{fairDealMetrics.surplusSplitPctBuyer}%</span>
                    </div>
                    <div className="w-full bg-charcoal-100 rounded-full h-1.5 overflow-hidden">
                      <div
                        className="bg-primary-600 h-full rounded-full transition-all duration-300"
                        style={{ width: `${fairDealMetrics.surplusSplitPctBuyer}%` }}
                      />
                    </div>

                    <div className="flex justify-between text-charcoal-600 pt-1">
                      <span>Seller Margin Retained</span>
                      <span className="font-bold text-charcoal-900">{fairDealMetrics.surplusSplitPctSeller}%</span>
                    </div>
                    <div className="w-full bg-charcoal-100 rounded-full h-1.5 overflow-hidden">
                      <div
                        className="bg-charcoal-700 h-full rounded-full transition-all duration-300"
                        style={{ width: `${fairDealMetrics.surplusSplitPctSeller}%` }}
                      />
                    </div>
                  </div>

                  <p className="text-[11px] text-charcoal-500 leading-relaxed bg-charcoal-50 p-2.5 rounded-lg border border-charcoal-100">
                    {fairDealMetrics.summaryExplanation}
                  </p>
                </div>
              ) : (
                <div className="text-center py-8 text-xs text-charcoal-400">
                  Awaiting agent proposals to compute multi-attribute scoring...
                </div>
              )}
            </Card>

            {/* Agreement Settlement Box */}
            <AnimatePresence>
              {agreementSeal && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="rounded-2xl border-2 border-emerald-500/80 bg-white p-6 shadow-card space-y-4"
                >
                  <div className="flex items-center gap-2 text-emerald-600 font-bold text-sm">
                    <CheckCircle2 className="w-5 h-5 text-emerald-600" />
                    <span>Agreement Sealed & Approved!</span>
                  </div>

                  <div>
                    <div className="flex items-center justify-between text-xs text-charcoal-500 mb-1">
                      <span>Seller: <strong className="text-charcoal-800">{activeStoreName}</strong></span>
                      {agreementSeal.agreementId && (
                        <span className="font-mono text-[10px] bg-charcoal-100 px-1.5 py-0.5 rounded">{agreementSeal.agreementId}</span>
                      )}
                    </div>
                    <span className="text-xs text-charcoal-400 block font-medium uppercase">Locked Total Payable</span>
                    <div className="text-3xl font-extrabold text-charcoal-900 mt-0.5">
                      {formatCurrency(agreementSeal.finalPrice)}
                    </div>
                    <div className="text-[11px] text-charcoal-500 mt-1 flex items-center justify-between">
                      <span>Qty: {agreementSeal.quantity || 1}</span>
                      <span>
                        Shipping: {(agreementSeal.shippingCost || 0) <= 0 ? 'FREE' : formatCurrency(agreementSeal.shippingCost || 0)}
                      </span>
                    </div>
                    <span className="text-xs text-emerald-600 font-medium block mt-1">
                      {product && product.listPrice > agreementSeal.finalPrice
                        ? `You saved ${formatCurrency(product.listPrice - agreementSeal.finalPrice)} below catalog list price!`
                        : 'Verified sealed agreement ready for checkout.'}
                    </span>
                    <div className="mt-2.5 p-2.5 rounded-xl bg-charcoal-50 border border-charcoal-200/80 text-[11px] space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-charcoal-500">Execution Mode:</span>
                        <span className="font-bold text-primary-700">
                          {agreementSeal.paymentExecutionMode === 'DELEGATED_AUTONOMOUS_PAYMENT'
                            ? 'Delegated Vault Adapter'
                            : agreementSeal.paymentExecutionMode === 'MANDATE_VIOLATION_BLOCKED'
                            ? 'Mandate Violation Blocked'
                            : 'Supervised PayPal Checkout'}
                        </span>
                      </div>
                      {agreementSeal.preparedPaypalOrderId && (
                        <div className="flex items-center justify-between text-emerald-700">
                          <span>PayPal Order Auto-Prepared:</span>
                          <span className="font-mono font-semibold">{agreementSeal.preparedPaypalOrderId}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="p-3 bg-emerald-50/70 rounded-xl border border-emerald-200/60 text-xs text-emerald-900 flex items-center justify-between">
                    <span className="flex items-center gap-1.5">
                      <Clock className="w-4 h-4 text-emerald-600" /> Reservation & Lock TTL:
                    </span>
                    <span className="font-mono font-bold text-sm text-emerald-700">
                      {formatTimer(timeLeftSec)}
                    </span>
                  </div>

                  {negotiation?.status === 'COMPLETED' ? (
                    <div className="p-3 bg-charcoal-50 rounded-xl border border-charcoal-200 text-center text-xs text-charcoal-700 font-semibold">
                      Payment Captured & Settled via PayPal
                    </div>
                  ) : (
                    <Button
                      variant="primary"
                      size="lg"
                      className="w-full font-bold shadow-card rounded-xl py-3"
                      onClick={() => navigate(`/checkout/${id}`)}
                      rightIcon={<ArrowRight className="w-4 h-4" />}
                    >
                      Approve deal & continue
                    </Button>
                  )}
                </motion.div>
              )}

              {isRejected && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="rounded-2xl border border-rose-300 bg-rose-50/50 p-6 space-y-3"
                >
                  <div className="flex items-center gap-2 text-rose-700 font-bold text-sm">
                    <XCircle className="w-5 h-5 text-rose-600" />
                    <span>No Deal Reached Across Eligible Sellers</span>
                  </div>
                  <p className="text-xs text-rose-800 leading-relaxed">
                    {negotiation?.noDealSummary || rejectionReason || 'All eligible merchants were checked and none matched within your budget limit.'}
                  </p>
                  {negotiation?.alternatives && negotiation.alternatives.length > 0 && (
                    <div className="space-y-1.5 pt-1">
                      <span className="text-[11px] font-bold text-charcoal-700 block">Closest Alternative Options:</span>
                      {negotiation.alternatives.map((alt) => (
                        <button
                          key={alt.productId}
                          type="button"
                          onClick={() => navigate(`/catalog/${alt.productId}`)}
                          className="w-full text-left p-2 rounded-lg bg-white border border-charcoal-200 hover:border-primary-400 text-[11px] flex items-center justify-between transition-colors"
                        >
                          <span className="font-semibold text-charcoal-800 truncate pr-2">{alt.storeName}: {alt.productTitle}</span>
                          <span className="font-bold text-primary-600 shrink-0">{formatCurrency(alt.listPrice)}</span>
                        </button>
                      ))}
                    </div>
                  )}
                  <Button variant="outline" size="sm" onClick={() => navigate('/catalog')} className="w-full">
                    Explore Alternative Products
                  </Button>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      )}

      {/* TAB 2: FairDeal Offer Comparison & Explicit Deal Selection */}
      {activeTab === 'comparison' && (
        <div className="space-y-6">
          <Card className="p-6 border-charcoal-200/90 shadow-card space-y-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-charcoal-100">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <Award className="w-5 h-5 text-primary-600" />
                  <h2 className="text-xl font-bold text-charcoal-900">Multi-Merchant Offer Comparison Matrix</h2>
                </div>
                <p className="text-xs text-charcoal-500">
                  Transparent multi-attribute evaluation comparing Total Landed Cost, Specifications, Delivery, Warranty, and Compliance.
                </p>
              </div>

              {comparisonResult?.bestOffer && (
                <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-xl text-xs text-emerald-800 font-bold">
                  <Star className="w-4 h-4 text-emerald-600 fill-emerald-600" />
                  <span>Recommended: Seller {comparisonResult.bestOffer.sellerId.slice(0, 8)}...</span>
                </div>
              )}
            </div>

            {comparisonResult?.summaryExplanation && (
              <div className="p-3 bg-charcoal-50 border border-charcoal-200/80 rounded-xl text-xs text-charcoal-700 leading-relaxed flex items-start gap-2">
                <Info className="w-4 h-4 text-primary-600 shrink-0 mt-0.5" />
                <span>{comparisonResult.summaryExplanation}</span>
              </div>
            )}

            {/* Offer Cards Comparison Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-2">
              {comparisonResult?.evaluations && comparisonResult.evaluations.length > 0 ? (
                comparisonResult.evaluations.map((evalItem, idx) => {
                  const isTopPick = comparisonResult.bestOffer?.productId === evalItem.productId;
                  const isSealedDeal = agreementSeal && negotiation?.productId === evalItem.productId;
                  const isExpanded = expandedExplanationOfferId === evalItem.productId;

                  return (
                    <Card
                      key={idx}
                      className={`p-5 flex flex-col justify-between space-y-4 transition-all ${
                        isTopPick
                          ? 'border-2 border-primary-500 shadow-md ring-2 ring-primary-500/20'
                          : 'border-charcoal-200 shadow-subtle'
                      } ${!evalItem.passedHardFilters ? 'opacity-65 bg-charcoal-50/50' : 'bg-white'}`}
                    >
                      <div className="space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="font-mono text-xs font-bold text-charcoal-500">
                            Merchant: {evalItem.sellerId.slice(0, 8)}...
                          </span>
                          <div className="flex items-center gap-1">
                            {isTopPick && (
                              <Badge variant="primary" size="sm">Top Match</Badge>
                            )}
                            <Badge variant={evalItem.passedHardFilters ? 'success' : 'danger'} size="sm">
                              {evalItem.passedHardFilters ? 'Passed Filters' : 'Failed Filter'}
                            </Badge>
                          </div>
                        </div>

                        <div>
                          <h3 className="text-sm font-extrabold text-charcoal-900 line-clamp-1">
                            {evalItem.productTitle}
                          </h3>
                          <span className="text-[11px] text-charcoal-400 block">{evalItem.brand || 'Merchant Special'}</span>
                        </div>

                        {/* Pricing / Landed Cost Box */}
                        <div className="p-3 bg-charcoal-50 rounded-xl border border-charcoal-100 space-y-1">
                          <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Total Landed Cost</span>
                          <div className="text-2xl font-extrabold text-charcoal-900">
                            {formatCurrency(evalItem.totalLandedCost)}
                          </div>
                          <div className="text-[11px] text-charcoal-500 flex justify-between">
                            <span>Unit: {formatCurrency(evalItem.unitPrice)}</span>
                            <span>Shipping: {formatCurrency(evalItem.shippingCost)}</span>
                          </div>
                        </div>

                        {/* Terms Pill */}
                        <div className="flex items-center justify-between text-xs text-charcoal-600 bg-white p-2 rounded-lg border border-charcoal-100">
                          <span className="flex items-center gap-1">
                            <Truck className="w-3.5 h-3.5 text-emerald-600" /> {evalItem.deliveryEstimateDays}d delivery
                          </span>
                          <span className="flex items-center gap-1">
                            <ShieldCheck className="w-3.5 h-3.5 text-purple-600" /> {evalItem.warrantyMonths}m warranty
                          </span>
                        </div>

                        {/* Perks */}
                        {evalItem.perks && evalItem.perks.length > 0 && (
                          <div className="flex flex-wrap gap-1">
                            {evalItem.perks.map((p, pIdx) => (
                              <span key={pIdx} className="text-[10px] bg-amber-50 text-amber-800 px-2 py-0.5 rounded-full border border-amber-200">
                                + {p}
                              </span>
                            ))}
                          </div>
                        )}

                        {/* 4 FairDeal Scores */}
                        {evalItem.passedHardFilters ? (
                          <div className="space-y-1.5 pt-2 border-t border-charcoal-100 text-xs">
                            <div className="flex justify-between items-center font-bold">
                              <span className="text-charcoal-900">Overall Deal Score:</span>
                              <span className="text-primary-600 text-sm font-extrabold">{evalItem.overallDealScore}/100</span>
                            </div>
                            <div className="flex justify-between text-[11px] text-charcoal-500">
                              <span>Buyer Satisfaction:</span>
                              <span className="font-semibold text-charcoal-800">{evalItem.buyerSatisfactionScore}/100</span>
                            </div>
                            <div className="flex justify-between text-[11px] text-charcoal-500">
                              <span>Offer Quality:</span>
                              <span className="font-semibold text-charcoal-800">{evalItem.offerQualityScore}/100</span>
                            </div>
                            <div className="flex justify-between text-[11px] text-charcoal-500">
                              <span>Seller Compliance:</span>
                              <span className="font-semibold text-charcoal-800">{evalItem.sellerPolicyCompliance}/100</span>
                            </div>
                          </div>
                        ) : (
                          <div className="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800 space-y-1">
                            <span className="font-bold block">Hard Constraint Violation:</span>
                            <p className="text-[11px] leading-relaxed">{evalItem.filterFailureReason}</p>
                          </div>
                        )}

                        {/* Toggleable Calculation Explanation */}
                        {evalItem.calculationExplanation && (
                          <div className="pt-1">
                            <button
                              onClick={() => setExpandedExplanationOfferId(isExpanded ? null : evalItem.productId)}
                              className="text-[11px] text-primary-600 hover:text-primary-800 font-bold flex items-center gap-1"
                            >
                              <span>{isExpanded ? 'Hide formula explanation' : 'View scoring breakdown'}</span>
                              {isExpanded ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
                            </button>
                            {isExpanded && (
                              <p className="mt-2 text-[10px] text-charcoal-600 leading-relaxed bg-charcoal-50 p-2.5 rounded-lg border border-charcoal-200">
                                {evalItem.calculationExplanation}
                              </p>
                            )}
                          </div>
                        )}
                      </div>

                      {/* Approval Action */}
                      <div className="pt-3 border-t border-charcoal-100">
                        {isSealedDeal ? (
                          <div className="w-full py-2.5 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-xl text-center text-xs font-bold flex items-center justify-center gap-1.5">
                            <Check className="w-4 h-4 text-emerald-600" />
                            <span>Approved Deal Active</span>
                          </div>
                        ) : evalItem.passedHardFilters ? (
                          <Button
                            variant={isTopPick ? 'primary' : 'outline'}
                            size="sm"
                            className="w-full font-bold shadow-sm"
                            disabled={isApprovingDeal}
                            onClick={() => handleApproveDeal(evalItem.offerId || evalItem.productId)}
                            rightIcon={<ThumbsUp className="w-3.5 h-3.5" />}
                          >
                            {isApprovingDeal && selectedOfferId === (evalItem.offerId || evalItem.productId)
                              ? 'Locking Deal...'
                              : 'Select & Approve Deal'}
                          </Button>
                        ) : (
                          <Button variant="outline" size="sm" className="w-full opacity-50 cursor-not-allowed" disabled>
                            Disqualified
                          </Button>
                        )}
                      </div>
                    </Card>
                  );
                })
              ) : (
                <div className="col-span-3 text-center py-12 text-charcoal-400 text-xs">
                  Awaiting multi-merchant opening quotes to render comparison matrix...
                </div>
              )}
            </div>
          </Card>
        </div>
      )}
    </div>
  );
};
