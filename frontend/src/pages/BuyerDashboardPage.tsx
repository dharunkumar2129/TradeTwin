import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { Negotiation, Order, BuyerDashboardSummary, OrderStatus } from '../types';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { formatCurrency } from '../lib/utils';
import { useAuth } from '../context/AuthContext';
import { SpendMandatePanel } from '../components/mandate/SpendMandatePanel';
import {
  Bot,
  Sparkles,
  ArrowRight,
  Package,
  CreditCard,
  Truck,
  CheckCircle2,
  Clock,
  ShieldCheck,
  Building,
  DollarSign,
  ExternalLink,
  Copy,
  Check,
  Eye,
  ShoppingBag,
} from 'lucide-react';

export const BuyerDashboardPage: React.FC = () => {
  const { user, quickLoginAsBuyer } = useAuth();
  const navigate = useNavigate();

  const [summary, setSummary] = useState<BuyerDashboardSummary | null>(null);
  const [negotiations, setNegotiations] = useState<Negotiation[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'all' | 'orders' | 'negotiations' | 'mandates'>('all');

  // Selected Order for Detail Modal
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  useEffect(() => {
    fetchBuyerData();
  }, [user]);

  const fetchBuyerData = async () => {
    setIsLoading(true);
    try {
      const [sumRes, negRes, ordRes] = await Promise.all([
        apiClient.get<{ data: BuyerDashboardSummary }>('/orders/buyer/summary'),
        apiClient.get<{ data: Negotiation[] }>('/negotiations/buyer/my-negotiations'),
        apiClient.get<{ data: Order[] }>('/orders/my-orders'),
      ]);
      setSummary(sumRes.data.data);
      setNegotiations(negRes.data.data);
      setOrders(ordRes.data.data);
    } catch {
      // Gracefully handled if unauthenticated
    } finally {
      setIsLoading(false);
    }
  };

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const getOrderStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case 'CONFIRMED':
        return <Badge variant="success">CONFIRMED</Badge>;
      case 'PROCESSING':
        return <Badge variant="primary">PROCESSING</Badge>;
      case 'SHIPPED':
        return <Badge variant="purple">SHIPPED</Badge>;
      case 'DELIVERED':
        return <Badge variant="success">DELIVERED</Badge>;
      case 'PAID':
        return <Badge variant="success">PAID</Badge>;
      case 'PAYMENT_PENDING':
        return <Badge variant="warning">PAYMENT PENDING</Badge>;
      case 'CANCELLED':
        return <Badge variant="danger">CANCELLED</Badge>;
      case 'REFUNDED':
        return <Badge variant="warning">REFUNDED</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  const getStepperIndex = (status: OrderStatus) => {
    switch (status) {
      case 'PAYMENT_PENDING':
        return 0;
      case 'PAID':
      case 'CONFIRMED':
        return 1;
      case 'PROCESSING':
        return 2;
      case 'SHIPPED':
        return 3;
      case 'DELIVERED':
        return 4;
      default:
        return 1;
    }
  };

  if (isLoading && orders.length === 0 && negotiations.length === 0) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center space-y-3">
        <div className="w-8 h-8 rounded-full border-2 border-primary-600 border-t-transparent animate-spin" />
        <p className="text-xs text-charcoal-500 font-medium">Loading Buyer Commerce Hub...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-charcoal-200/80">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Badge variant="primary">Buyer Commerce Hub</Badge>
            <span className="text-xs text-charcoal-400">Autonomous Sessions & Orders</span>
          </div>
          <h1 className="text-2xl font-black text-charcoal-900 tracking-tight">Buyer Dashboard</h1>
          <p className="text-xs text-charcoal-500 mt-1">
            Track completed orders, inspect PayPal transaction audits, track shipments, and review agent negotiation histories.
          </p>
        </div>

        <div>
          {(!user || user.role !== 'ROLE_BUYER') && (
            <Button variant="secondary" size="sm" onClick={quickLoginAsBuyer}>
              Switch to Demo Buyer
            </Button>
          )}
        </div>
      </div>

      {/* KPI Stats Cards (Actual Stored MongoDB Data) */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card className="p-4 flex items-center gap-3.5 shadow-subtle">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
            <DollarSign className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-charcoal-500 font-medium block">Total Spend</span>
            <span className="text-lg font-black text-charcoal-900">
              {formatCurrency(summary?.totalSpent || 0)}
            </span>
          </div>
        </Card>

        <Card className="p-4 flex items-center gap-3.5 shadow-subtle">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
            <Package className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-charcoal-500 font-medium block">Settled Orders</span>
            <span className="text-lg font-black text-charcoal-900">{summary?.totalOrders || orders.length}</span>
          </div>
        </Card>

        <Card className="p-4 flex items-center gap-3.5 shadow-subtle">
          <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-600 flex items-center justify-center shrink-0">
            <Bot className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-charcoal-500 font-medium block">Active Sessions</span>
            <span className="text-lg font-black text-charcoal-900">{summary?.activeNegotiations || 0}</span>
          </div>
        </Card>

        <Card className="p-4 flex items-center gap-3.5 shadow-subtle">
          <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-charcoal-500 font-medium block">Agreed Deals</span>
            <span className="text-lg font-black text-charcoal-900">{summary?.agreedDeals || 0}</span>
          </div>
        </Card>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-charcoal-200/80 pb-2">
        <button
          type="button"
          onClick={() => setActiveTab('all')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
            activeTab === 'all'
              ? 'bg-primary-600 text-white'
              : 'text-charcoal-600 hover:text-charcoal-900 hover:bg-charcoal-100'
          }`}
        >
          All Overview
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('orders')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
            activeTab === 'orders'
              ? 'bg-primary-600 text-white'
              : 'text-charcoal-600 hover:text-charcoal-900 hover:bg-charcoal-100'
          }`}
        >
          <span>Confirmed Orders</span>
          <span className="px-1.5 py-0.2 bg-white/20 rounded-full text-[10px]">{orders.length}</span>
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('negotiations')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
            activeTab === 'negotiations'
              ? 'bg-primary-600 text-white'
              : 'text-charcoal-600 hover:text-charcoal-900 hover:bg-charcoal-100'
          }`}
        >
          <span>Agent Negotiations</span>
          <span className="px-1.5 py-0.2 bg-white/20 rounded-full text-[10px]">{negotiations.length}</span>
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('mandates')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
            activeTab === 'mandates'
              ? 'bg-primary-600 text-white'
              : 'text-charcoal-600 hover:text-charcoal-900 hover:bg-charcoal-100'
          }`}
        >
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>Spend Mandates & Audit</span>
        </button>
      </div>

      {activeTab === 'mandates' && <SpendMandatePanel />}

      {/* Section 1: Confirmed Orders & Shipments */}
      {(activeTab === 'all' || activeTab === 'orders') && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-charcoal-900 flex items-center gap-2">
              <Package className="w-4 h-4 text-emerald-600" />
              Settled Orders & Shipment Tracking
            </h2>
            <span className="text-xs text-charcoal-400">Captured via PayPal Orders v2</span>
          </div>

          {orders.length === 0 ? (
            <Card className="p-8 text-center text-xs text-charcoal-400">
              <ShoppingBag className="w-8 h-8 text-charcoal-300 mx-auto mb-2" />
              No settled orders found yet. Negotiate and complete checkout to see receipts and tracking here!
              <div className="mt-3">
                <Button variant="primary" size="sm" onClick={() => navigate('/catalog')}>
                  Browse Catalog
                </Button>
              </div>
            </Card>
          ) : (
            <div className="space-y-4">
              {orders.map((ord) => {
                const stepIdx = getStepperIndex(ord.orderStatus);
                const hasTracking = !!ord.trackingNumber;

                return (
                  <Card key={ord.id} className="p-5 space-y-4 shadow-subtle hover:border-charcoal-300 transition-all">
                    {/* Order Header */}
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-charcoal-100">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono font-bold text-sm text-charcoal-900">{ord.orderNumber}</span>
                          {getOrderStatusBadge(ord.orderStatus)}
                        </div>
                        <span className="text-[11px] text-charcoal-400">
                          Placed on {new Date(ord.createdAt).toLocaleDateString()} at{' '}
                          {new Date(ord.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setSelectedOrder(ord)}
                          leftIcon={<Eye className="w-3.5 h-3.5" />}
                        >
                          View Order Audit
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/arena/${ord.negotiationId}`)}
                          leftIcon={<Bot className="w-3.5 h-3.5" />}
                        >
                          Negotiation History
                        </Button>
                      </div>
                    </div>

                    {/* Product & Payment Row */}
                    <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-center">
                      <div className="md:col-span-6 flex items-center gap-3">
                        <div className="w-14 h-14 rounded-xl bg-charcoal-100 overflow-hidden shrink-0 border border-charcoal-200">
                          {ord.productImage ? (
                            <img src={ord.productImage} alt={ord.productTitle} className="w-full h-full object-cover" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-charcoal-400">
                              <ShoppingBag className="w-6 h-6" />
                            </div>
                          )}
                        </div>
                        <div className="min-w-0 flex-1">
                          <h4 className="font-bold text-xs text-charcoal-900 truncate">
                            {ord.productTitle || 'Negotiated Item'}
                          </h4>
                          <div className="flex items-center gap-1.5 text-[11px] text-charcoal-500 mt-0.5">
                            <Building className="w-3 h-3 text-charcoal-400" />
                            <span>{ord.sellerStoreName || `Merchant #${ord.sellerId.slice(-6)}`}</span>
                          </div>
                          <span className="text-[11px] text-charcoal-500">Qty: {ord.quantity || 1} unit</span>
                        </div>
                      </div>

                      {/* PayPal & Amount Info */}
                      <div className="md:col-span-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-charcoal-50/70 p-3 rounded-xl border border-charcoal-200/60 text-xs">
                        <div>
                          <div className="flex items-center gap-1.5 text-charcoal-700 font-semibold mb-0.5">
                            <CreditCard className="w-3.5 h-3.5 text-[#0070BA]" />
                            <span>PayPal Orders v2</span>
                          </div>
                          {ord.payment?.paypalOrderId && (
                            <div className="flex items-center gap-1 font-mono text-[10px] text-charcoal-500">
                              <span>Order ID: {ord.payment.paypalOrderId.slice(0, 14)}...</span>
                              <button
                                type="button"
                                onClick={() => copyToClipboard(ord.payment!.paypalOrderId, ord.id + '-pp')}
                                className="text-charcoal-400 hover:text-charcoal-700"
                              >
                                {copiedId === ord.id + '-pp' ? (
                                  <Check className="w-3 h-3 text-emerald-600" />
                                ) : (
                                  <Copy className="w-3 h-3" />
                                )}
                              </button>
                            </div>
                          )}
                          {ord.payment?.captureId && (
                            <div className="font-mono text-[10px] text-charcoal-400">
                              Capture ID: {ord.payment.captureId}
                            </div>
                          )}
                        </div>

                        <div className="text-right">
                          <span className="text-[10px] text-charcoal-400 block uppercase font-medium">Landed Price</span>
                          <span className="font-black text-base text-primary-600">{formatCurrency(ord.finalAmount)}</span>
                          <span className="text-[10px] text-emerald-600 font-semibold block">Paid in Full</span>
                        </div>
                      </div>
                    </div>

                    {/* Fulfilment Stepper */}
                    <div className="pt-2 border-t border-charcoal-100">
                      <div className="flex items-center justify-between text-[11px] font-semibold text-charcoal-500 mb-2">
                        <span>Fulfilment Tracking</span>
                        {hasTracking && (
                          <span className="text-primary-600 flex items-center gap-1 font-mono">
                            <Truck className="w-3 h-3" />
                            {ord.carrier}: {ord.trackingNumber}
                          </span>
                        )}
                      </div>

                      <div className="grid grid-cols-4 gap-2 text-center text-[10px]">
                        <div
                          className={`p-2 rounded-lg border font-medium ${
                            stepIdx >= 1
                              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                              : 'bg-charcoal-50 border-charcoal-200 text-charcoal-400'
                          }`}
                        >
                          <div className="font-bold">1. Confirmed</div>
                          <span className="text-[9px] opacity-80">Payment Verified</span>
                        </div>

                        <div
                          className={`p-2 rounded-lg border font-medium ${
                            stepIdx >= 2
                              ? 'bg-blue-50 border-blue-200 text-blue-800'
                              : 'bg-charcoal-50 border-charcoal-200 text-charcoal-400'
                          }`}
                        >
                          <div className="font-bold">2. Processing</div>
                          <span className="text-[9px] opacity-80">Merchant Packing</span>
                        </div>

                        <div
                          className={`p-2 rounded-lg border font-medium ${
                            stepIdx >= 3
                              ? 'bg-purple-50 border-purple-200 text-purple-800'
                              : 'bg-charcoal-50 border-charcoal-200 text-charcoal-400'
                          }`}
                        >
                          <div className="font-bold">3. Shipped</div>
                          <span className="text-[9px] opacity-80">{ord.carrier || 'In Transit'}</span>
                        </div>

                        <div
                          className={`p-2 rounded-lg border font-medium ${
                            stepIdx >= 4
                              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                              : 'bg-charcoal-50 border-charcoal-200 text-charcoal-400'
                          }`}
                        >
                          <div className="font-bold">4. Delivered</div>
                          <span className="text-[9px] opacity-80">Destination Reached</span>
                        </div>
                      </div>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Section 2: Agent Negotiations & Sealed Agreements */}
      {(activeTab === 'all' || activeTab === 'negotiations') && (
        <div className="space-y-4 pt-4 border-t border-charcoal-200/80">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-charcoal-900 flex items-center gap-2">
              <Bot className="w-4 h-4 text-primary-600" />
              Agent Negotiations & Sealed Agreements
            </h2>
            <span className="text-xs text-charcoal-400">Autonomous Sessions</span>
          </div>

          {negotiations.length === 0 ? (
            <Card className="p-8 text-center text-xs text-charcoal-400">
              <Sparkles className="w-8 h-8 text-charcoal-300 mx-auto mb-2" />
              No active negotiations found. Explore the marketplace catalog to deploy your Buyer Agent!
              <div className="mt-3">
                <Button variant="primary" size="sm" onClick={() => navigate('/catalog')}>
                  Browse Catalog
                </Button>
              </div>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {negotiations.map((neg) => (
                <Card key={neg.id} className="p-5 flex flex-col justify-between space-y-4">
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-[11px] font-mono text-charcoal-400">ID: {neg.id.slice(0, 8)}...</span>
                      <Badge
                        variant={
                          neg.status === 'AGREED' || neg.status === 'ACCEPTED'
                            ? 'success'
                            : neg.status === 'COMPLETED'
                            ? 'success'
                            : neg.status === 'REJECTED' || neg.status === 'CANCELLED'
                            ? 'danger'
                            : neg.status === 'EXPIRED'
                            ? 'warning'
                            : neg.status === 'DISCOVERING'
                            ? 'purple'
                            : neg.status === 'NEGOTIATING' || neg.status === 'COUNTEROFFERED' || neg.status === 'IN_PROGRESS'
                            ? 'primary'
                            : 'default'
                        }
                        size="sm"
                      >
                        {neg.status}
                      </Badge>
                    </div>

                    <div className="text-xs space-y-1.5 text-charcoal-600">
                      <div className="flex justify-between">
                        <span>Target Budget:</span>
                        <span className="font-semibold text-charcoal-900">
                          {formatCurrency(neg.buyerPreferences?.targetPrice || 0)}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span>Turns Completed:</span>
                        <span className="font-semibold text-charcoal-900">{neg.turns?.length || 0}</span>
                      </div>
                      {neg.fairDealMetrics && (
                        <div className="flex justify-between text-primary-600 font-semibold">
                          <span>FairDeal Score:</span>
                          <span>{neg.fairDealMetrics.fairDealScore}/100</span>
                        </div>
                      )}
                      {neg.agreementSeal && (
                        <div className="flex justify-between text-emerald-600 font-semibold pt-1 border-t border-charcoal-100">
                          <span>Sealed Price:</span>
                          <span>{formatCurrency(neg.agreementSeal.finalPrice)}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="pt-3 border-t border-charcoal-100 flex items-center justify-between">
                    {neg.status === 'AGREED' || neg.status === 'ACCEPTED' ? (
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={() => navigate(`/checkout/${neg.id}`)}
                        rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
                      >
                        Checkout ({formatCurrency(neg.agreementSeal?.finalPrice || 0)})
                      </Button>
                    ) : neg.status === 'COMPLETED' ? (
                      <div className="flex items-center justify-between w-full">
                        <span className="text-xs font-bold text-emerald-600 flex items-center gap-1">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Paid & Settled
                        </span>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/arena/${neg.id}`)}
                          rightIcon={<ExternalLink className="w-3 h-3" />}
                        >
                          View Transcript
                        </Button>
                      </div>
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/arena/${neg.id}`)}
                        rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
                      >
                        Enter Arena
                      </Button>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Order Detail Modal */}
      {selectedOrder && (
        <Modal
          isOpen={!!selectedOrder}
          onClose={() => setSelectedOrder(null)}
          title={`Order Audit Details — ${selectedOrder.orderNumber}`}
        >
          <div className="space-y-6 text-xs text-charcoal-700">
            {/* Status & Summary Header */}
            <div className="flex items-center justify-between pb-3 border-b border-charcoal-100">
              <div>
                <span className="text-[10px] text-charcoal-400 uppercase font-semibold">Current State</span>
                <div className="mt-0.5">{getOrderStatusBadge(selectedOrder.orderStatus)}</div>
              </div>
              <div className="text-right">
                <span className="text-[10px] text-charcoal-400 uppercase font-semibold">Total Amount</span>
                <div className="text-base font-black text-primary-600">{formatCurrency(selectedOrder.finalAmount)}</div>
              </div>
            </div>

            {/* Product & Seller Info */}
            <div className="space-y-2">
              <h4 className="font-bold text-charcoal-900">Purchased Item</h4>
              <div className="p-3 rounded-xl bg-charcoal-50 border border-charcoal-200/70 space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-charcoal-500">Title:</span>
                  <span className="font-medium text-charcoal-900">{selectedOrder.productTitle}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-charcoal-500">Merchant / Brand:</span>
                  <span className="font-medium text-charcoal-900">{selectedOrder.sellerStoreName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-charcoal-500">Quantity:</span>
                  <span className="font-medium text-charcoal-900">{selectedOrder.quantity || 1} unit</span>
                </div>
              </div>
            </div>

            {/* PayPal Payment Details */}
            <div className="space-y-2">
              <h4 className="font-bold text-charcoal-900 flex items-center gap-1.5">
                <CreditCard className="w-3.5 h-3.5 text-[#0070BA]" />
                PayPal Transaction Record
              </h4>
              <div className="p-3 rounded-xl bg-charcoal-50 border border-charcoal-200/70 space-y-1.5 font-mono text-[11px]">
                <div className="flex justify-between">
                  <span className="text-charcoal-500 font-sans">PayPal Order ID:</span>
                  <span className="text-charcoal-900">{selectedOrder.payment?.paypalOrderId || 'N/A'}</span>
                </div>
                {selectedOrder.payment?.captureId && (
                  <div className="flex justify-between">
                    <span className="text-charcoal-500 font-sans">PayPal Capture ID:</span>
                    <span className="text-charcoal-900">{selectedOrder.payment.captureId}</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-charcoal-500 font-sans">Payment Status:</span>
                  <span className="font-sans font-bold text-emerald-600">
                    {selectedOrder.payment?.status || 'COMPLETED'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-charcoal-500 font-sans">Deal Token:</span>
                  <span className="text-charcoal-600 truncate max-w-[200px]">{selectedOrder.dealToken}</span>
                </div>
              </div>
            </div>

            {/* Authorized Shipping Destination */}
            {selectedOrder.shippingDetails && (
              <div className="space-y-2">
                <h4 className="font-bold text-charcoal-900 flex items-center gap-1.5">
                  <Truck className="w-3.5 h-3.5 text-primary-600" />
                  Delivery Destination
                </h4>
                <div className="p-3 rounded-xl bg-charcoal-50 border border-charcoal-200/70 text-xs text-charcoal-700">
                  <p className="font-semibold text-charcoal-900">{selectedOrder.shippingDetails.fullName}</p>
                  <p>{selectedOrder.shippingDetails.addressLine1}</p>
                  <p>
                    {selectedOrder.shippingDetails.city}, {selectedOrder.shippingDetails.state}{' '}
                    {selectedOrder.shippingDetails.postalCode}
                  </p>
                  <p>{selectedOrder.shippingDetails.country || 'United States'}</p>
                </div>
              </div>
            )}

            {/* Fulfilment Carrier & Tracking */}
            {selectedOrder.trackingNumber && (
              <div className="space-y-2">
                <h4 className="font-bold text-charcoal-900 flex items-center gap-1.5">
                  <Truck className="w-3.5 h-3.5 text-purple-600" />
                  Carrier & Tracking
                </h4>
                <div className="p-3 rounded-xl bg-purple-50/70 border border-purple-200/70 space-y-1 text-xs">
                  <div className="flex justify-between font-mono">
                    <span className="text-purple-700 font-sans font-medium">Carrier:</span>
                    <span className="font-bold text-purple-900">{selectedOrder.carrier}</span>
                  </div>
                  <div className="flex justify-between font-mono">
                    <span className="text-purple-700 font-sans font-medium">Tracking Number:</span>
                    <span className="font-bold text-purple-900">{selectedOrder.trackingNumber}</span>
                  </div>
                  {selectedOrder.fulfilmentNotes && (
                    <div className="pt-1 text-[11px] text-purple-800 italic">
                      Notes: {selectedOrder.fulfilmentNotes}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Audit History (Controlled State Machine Transitions) */}
            {selectedOrder.statusHistory && selectedOrder.statusHistory.length > 0 && (
              <div className="space-y-2">
                <h4 className="font-bold text-charcoal-900 flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-charcoal-500" />
                  State Machine Audit Trail
                </h4>
                <div className="border border-charcoal-200 rounded-xl overflow-hidden divide-y divide-charcoal-100 text-[11px]">
                  {selectedOrder.statusHistory.map((hist, idx) => (
                    <div key={idx} className="p-2.5 flex items-start justify-between gap-2">
                      <div>
                        <div className="font-semibold text-charcoal-900 flex items-center gap-1.5">
                          <span>{hist.fromStatus ? `${hist.fromStatus} → ` : ''}</span>
                          <span className="text-primary-600">{hist.toStatus}</span>
                        </div>
                        {hist.notes && <p className="text-charcoal-500 text-[10px] mt-0.5">{hist.notes}</p>}
                      </div>
                      <span className="text-[10px] text-charcoal-400 font-mono shrink-0">
                        {new Date(hist.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="pt-2 flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setSelectedOrder(null)}>
                Close
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={() => {
                  navigate(`/arena/${selectedOrder.negotiationId}`);
                }}
              >
                Inspect Agent Negotiation
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};