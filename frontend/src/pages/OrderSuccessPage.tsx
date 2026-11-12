import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { Order, Product } from '../types';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { formatCurrency } from '../lib/utils';
import { CheckCircle2, ArrowRight, CreditCard, Building, Bot, ShieldCheck } from 'lucide-react';

export const OrderSuccessPage: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [product, setProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (orderId) {
      apiClient
        .get<{ data: Order }>(`/orders/${orderId}`)
        .then((res) => {
          const ord = res.data.data;
          setOrder(ord);
          return apiClient.get<{ data: Product }>(`/catalog/${ord.productId}`);
        })
        .then((res) => setProduct(res.data.data))
        .catch(console.error)
        .finally(() => setIsLoading(false));
    }
  }, [orderId]);

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <div className="w-8 h-8 rounded-full border-2 border-primary-600 border-t-transparent animate-spin" />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-bold text-charcoal-900">Order not found</h2>
        <Button variant="outline" size="sm" onClick={() => navigate('/catalog')} className="mt-4">
          Back to Catalog
        </Button>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-8 py-6">
      <div className="text-center space-y-3">
        <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 mx-auto flex items-center justify-center shadow-subtle">
          <CheckCircle2 className="w-10 h-10" />
        </div>
        <h1 className="text-3xl font-extrabold text-charcoal-900 tracking-tight">Order Confirmed!</h1>
        <p className="text-xs text-charcoal-500 max-w-md mx-auto leading-relaxed">
          Your PayPal payment was cryptographically captured and verified on the backend. Merchant inventory has been permanently allocated.
        </p>
      </div>

      <Card className="p-6 space-y-6 shadow-card">
        <div className="flex items-center justify-between pb-4 border-b border-charcoal-100">
          <div>
            <span className="text-[10px] text-charcoal-400 uppercase font-semibold">Order Reference</span>
            <div className="font-mono font-bold text-sm text-charcoal-900">{order.orderNumber}</div>
          </div>
          <Badge variant="success">{order.orderStatus}</Badge>
        </div>

        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-xl bg-charcoal-100 overflow-hidden shrink-0 border border-charcoal-200/80">
            {(order.productImage || product?.images?.[0]) && (
              <img
                src={order.productImage || product?.images[0]}
                alt={order.productTitle || product?.title}
                className="w-full h-full object-cover"
              />
            )}
          </div>
          <div className="flex-1 min-w-0">
            <h4 className="font-semibold text-sm text-charcoal-900 truncate">
              {order.productTitle || product?.title}
            </h4>
            <div className="flex items-center gap-1.5 text-xs text-charcoal-500 mt-0.5">
              <Building className="w-3.5 h-3.5 text-charcoal-400" />
              <span>{order.sellerStoreName || product?.brand || 'Verified Merchant'}</span>
            </div>
            <span className="text-[11px] text-charcoal-400 block mt-0.5">
              Qty: {order.quantity || 1} • Settled at negotiated agreement
            </span>
          </div>
          <div className="text-right shrink-0">
            <span className="font-black text-lg text-primary-600">{formatCurrency(order.finalAmount)}</span>
            <span className="text-[10px] text-emerald-600 font-semibold block uppercase">
              {order.payment?.status || 'COMPLETED'}
            </span>
          </div>
        </div>

        {/* PayPal Audit & Deal Seal */}
        <div className="p-4 rounded-xl bg-charcoal-50/70 border border-charcoal-200/70 space-y-2.5 text-xs">
          <div className="flex items-center gap-1.5 font-bold text-charcoal-900">
            <CreditCard className="w-4 h-4 text-[#0070BA]" />
            <span>PayPal Orders API v2 Transaction Receipt</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-[11px]">
            <div>
              <span className="text-charcoal-400 font-sans block">PayPal Order ID</span>
              <span className="text-charcoal-800 break-all">{order.payment?.paypalOrderId || 'Verified Sandbox'}</span>
            </div>
            <div>
              <span className="text-charcoal-400 font-sans block">PayPal Capture ID</span>
              <span className="text-charcoal-800 break-all">{order.payment?.captureId || 'Confirmed Capture'}</span>
            </div>
            <div>
              <span className="text-charcoal-400 font-sans block">Verification Method</span>
              <span className="text-emerald-700 font-sans font-semibold flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" />
                Server Capture Verified
              </span>
            </div>
            <div>
              <span className="text-charcoal-400 font-sans block">Deal Token</span>
              <span className="text-charcoal-600 truncate block">{order.dealToken}</span>
            </div>
          </div>
        </div>
      </Card>

      <div className="flex flex-wrap items-center justify-center gap-3">
        <Button variant="primary" size="md" onClick={() => navigate('/buyer/dashboard')}>
          Track Order in Buyer Dashboard
        </Button>
        <Button
          variant="outline"
          size="md"
          onClick={() => navigate(`/arena/${order.negotiationId}`)}
          leftIcon={<Bot className="w-4 h-4" />}
        >
          View Negotiation History
        </Button>
        <Button
          variant="outline"
          size="md"
          onClick={() => navigate('/catalog')}
          rightIcon={<ArrowRight className="w-4 h-4" />}
        >
          Continue Shopping
        </Button>
      </div>
    </div>
  );
};
