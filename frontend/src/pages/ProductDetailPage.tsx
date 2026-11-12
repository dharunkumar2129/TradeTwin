import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { Product, Negotiation } from '../types';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { Alert } from '../components/ui/Alert';
import { formatCurrency, extractErrorMessage } from '../lib/utils';
import { useAuth } from '../context/AuthContext';
import { Bot, Zap, Shield, Truck, Sparkles, ArrowRight, ArrowLeft } from 'lucide-react';

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isStartingNegotiation, setIsStartingNegotiation] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Buyer Agent Parameters
  const [targetPrice, setTargetPrice] = useState<number>(0);
  const [maxCeilingPrice, setMaxCeilingPrice] = useState<number>(0);
  const [urgency, setUrgency] = useState<'LOW' | 'MEDIUM' | 'HIGH'>('MEDIUM');
  const [desiredDeliveryDays, setDesiredDeliveryDays] = useState<number>(3);
  const [requireExtendedWarranty, setRequireExtendedWarranty] = useState<boolean>(true);

  const { isAuthenticated, quickLoginAsBuyer } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (id) {
      apiClient.get<{ data: Product }>(`/catalog/${id}`)
        .then((res) => {
          const prod = res.data.data;
          setProduct(prod);
          const target = Math.round(prod.listPrice * 0.85);
          const ceiling = Math.round(prod.listPrice * 0.95);
          setTargetPrice(target);
          setMaxCeilingPrice(ceiling);
        })
        .catch((err) => setErrorMsg(err.message))
        .finally(() => setIsLoading(false));
    }
  }, [id]);

  const handleStartNegotiation = async () => {
    if (!isAuthenticated) {
      await quickLoginAsBuyer();
    }

    if (targetPrice <= 0 || maxCeilingPrice <= 0) {
      setErrorMsg('Please specify valid target and ceiling budgets');
      return;
    }
    if (targetPrice > maxCeilingPrice) {
      setErrorMsg('Target budget cannot be higher than maximum ceiling budget');
      return;
    }

    setIsStartingNegotiation(true);
    setErrorMsg(null);
    try {
      const res = await apiClient.post<{ data: Negotiation }>('/negotiations/start', {
        productId: product?.id,
        preferences: {
          targetPrice,
          maxCeilingPrice,
          urgency,
          desiredDeliveryDays,
          requireExtendedWarranty,
        },
      });

      const negotiation = res.data.data;
      navigate(`/arena/${negotiation.id}`);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Failed to initiate negotiation'));
    } finally {
      setIsStartingNegotiation(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <div className="w-8 h-8 rounded-full border-2 border-primary-600 border-t-transparent animate-spin" />
      </div>
    );
  }

  if (!product) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-bold text-charcoal-900">Product not found</h2>
        <Button variant="outline" size="sm" onClick={() => navigate('/catalog')} className="mt-4">
          Back to Catalog
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-5xl mx-auto">
      <button
        onClick={() => navigate('/catalog')}
        className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-800 transition-colors"
      >
        <ArrowLeft className="w-3.5 h-3.5" /> Back to Catalog
      </button>

      {errorMsg && <Alert type="error" title="Action Error">{errorMsg}</Alert>}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Image & Details */}
        <div className="lg:col-span-7 space-y-6">
          <div className="aspect-[4/3] rounded-2xl overflow-hidden bg-charcoal-100 border border-charcoal-200/80 shadow-subtle">
            {product.images[0] ? (
              <img src={product.images[0]} alt={product.title} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-charcoal-400">
                <Bot className="w-16 h-16" />
              </div>
            )}
          </div>

          <div>
            <div className="flex flex-wrap items-center gap-2 mb-2">
              <Badge variant="default">{product.category}</Badge>
              {product.brand && <Badge variant="purple">{product.brand}</Badge>}
              <Badge variant="outline">{product.condition || 'Brand New'}</Badge>
              {product.sku && <span className="text-xs text-charcoal-400 font-mono">SKU: {product.sku}</span>}
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-charcoal-900 tracking-tight">
              {product.title}
            </h1>
            <p className="mt-3 text-sm text-charcoal-600 leading-relaxed">
              {product.description}
            </p>

            {/* Logistics & Protection Strip */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mt-4 p-3.5 bg-charcoal-50 rounded-xl border border-charcoal-200/80 text-xs">
              <div>
                <span className="text-[10px] text-charcoal-400 font-bold block uppercase">Shipping</span>
                <span className="font-semibold text-charcoal-900">
                  {product.shippingCost && product.shippingCost > 0 ? formatCurrency(product.shippingCost) : 'Free Shipping'}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-charcoal-400 font-bold block uppercase">Delivery</span>
                <span className="font-semibold text-charcoal-900">
                  {product.estimatedDeliveryDays ? `${product.estimatedDeliveryDays} Business Days` : '3-5 Days'}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-charcoal-400 font-bold block uppercase">Warranty</span>
                <span className="font-semibold text-charcoal-900">
                  {product.warrantyInfo || '12 Months Limited'}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-charcoal-400 font-bold block uppercase">Returns</span>
                <span className="font-semibold text-charcoal-900">
                  {product.returnPolicy || '30-Day Return'}
                </span>
              </div>
            </div>

            {/* Technical Specifications */}
            {product.specifications && Object.keys(product.specifications).length > 0 && (
              <div className="mt-6 border-t border-charcoal-200/70 pt-5">
                <h3 className="text-xs font-bold uppercase tracking-wider text-charcoal-400 mb-3">
                  Technical Specifications
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                  {Object.entries(product.specifications).map(([key, val]) => (
                    <div key={key} className="flex justify-between py-1.5 px-3 rounded-lg bg-charcoal-50/70 border border-charcoal-100">
                      <span className="text-charcoal-500 font-medium">{key}</span>
                      <span className="font-semibold text-charcoal-900 text-right">{val}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Seller Agent Features */}
          <div className="grid grid-cols-3 gap-3 pt-4 border-t border-charcoal-100">
            <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/80 text-center">
              <Zap className="w-4 h-4 text-primary-600 mx-auto mb-1.5" />
              <span className="text-[11px] font-semibold text-charcoal-900 block">Auto-Bargaining</span>
              <span className="text-[10px] text-charcoal-500">Max {product.maxNegotiationRounds} rounds</span>
            </div>
            <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/80 text-center">
              <Truck className="w-4 h-4 text-emerald-600 mx-auto mb-1.5" />
              <span className="text-[11px] font-semibold text-charcoal-900 block">Shipping Perks</span>
              <span className="text-[10px] text-charcoal-500">{product.allowFreeShipping ? 'Free Express eligible' : 'Standard'}</span>
            </div>
            <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/80 text-center">
              <Shield className="w-4 h-4 text-purple-600 mx-auto mb-1.5" />
              <span className="text-[11px] font-semibold text-charcoal-900 block">Care Sweeteners</span>
              <span className="text-[10px] text-charcoal-500">{product.allowWarrantySweeteners ? 'Extended care available' : 'Standard'}</span>
            </div>
          </div>
        </div>

        {/* Right Column: Negotiation Card */}
        <div className="lg:col-span-5 space-y-6">
          <Card className="p-6 sticky top-24 border-charcoal-200/90 shadow-card">
            <div className="flex items-center justify-between pb-4 border-b border-charcoal-100">
              <div>
                <span className="text-xs text-charcoal-400 uppercase font-medium">Catalog List Price</span>
                <div className="text-3xl font-extrabold text-charcoal-900 mt-0.5">
                  {formatCurrency(product.listPrice)}
                </div>
              </div>
              <Badge variant="success">
                In Stock ({product.availableInventory})
              </Badge>
            </div>

            <div className="py-5 space-y-4">
              <div className="flex items-start gap-3 p-3.5 rounded-xl bg-primary-50/70 border border-primary-200/60 text-xs text-primary-900">
                <Bot className="w-5 h-5 text-primary-600 shrink-0 mt-0.5" />
                <p className="leading-relaxed">
                  Don't pay full list price. Set your target budget and let your <strong>Buyer Agent</strong> negotiate price concessions, expedited shipping, and extended warranties directly with the merchant's AI.
                </p>
              </div>

              <div className="space-y-3 pt-2">
                <Button
                  variant="primary"
                  size="lg"
                  className="w-full text-base font-semibold shadow-card rounded-xl py-3"
                  onClick={() => setIsModalOpen(true)}
                  leftIcon={<Sparkles className="w-5 h-5" />}
                >
                  Configure & Start Negotiation
                </Button>

                <p className="text-[11px] text-center text-charcoal-400">
                  Negotiation takes ~10 seconds. You are never obligated to buy.
                </p>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Negotiation Configuration Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Configure Buyer Agent Strategy"
        description="Your agent will negotiate aggressively within these bounds."
      >
        <div className="space-y-4 pt-2">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Target Price ($)"
              type="number"
              value={targetPrice}
              onChange={(e) => setTargetPrice(Number(e.target.value))}
              hint="Your ideal price goal"
            />
            <Input
              label="Maximum Ceiling ($)"
              type="number"
              value={maxCeilingPrice}
              onChange={(e) => setMaxCeilingPrice(Number(e.target.value))}
              hint="Hard cutoff: will never exceed"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                Urgency
              </label>
              <select
                value={urgency}
                onChange={(e) => setUrgency(e.target.value as 'LOW' | 'MEDIUM' | 'HIGH')}
                className="w-full rounded-lg border border-charcoal-200 bg-white px-3 py-2 text-sm text-charcoal-900 focus:outline-none focus:ring-2 focus:ring-primary-500/20"
              >
                <option value="LOW">Low (Patient concessions)</option>
                <option value="MEDIUM">Medium (Balanced)</option>
                <option value="HIGH">High (Fast close)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                Target Delivery Days
              </label>
              <select
                value={desiredDeliveryDays}
                onChange={(e) => setDesiredDeliveryDays(Number(e.target.value))}
                className="w-full rounded-lg border border-charcoal-200 bg-white px-3 py-2 text-sm text-charcoal-900 focus:outline-none focus:ring-2 focus:ring-primary-500/20"
              >
                <option value="1">1 Day (Next Day Express)</option>
                <option value="2">2 Days (Express)</option>
                <option value="3">3 Days (Standard)</option>
                <option value="5">5 Days (Economy)</option>
              </select>
            </div>
          </div>

          <div className="pt-2">
            <label className="flex items-center gap-2.5 text-xs text-charcoal-700 font-medium cursor-pointer">
              <input
                type="checkbox"
                checked={requireExtendedWarranty}
                onChange={(e) => setRequireExtendedWarranty(e.target.checked)}
                className="rounded border-charcoal-300 text-primary-600 focus:ring-primary-500"
              />
              Require Extended Care / 2-Year Warranty in agreement
            </label>
          </div>

          <div className="pt-4 flex items-center justify-end gap-2 border-t border-charcoal-100">
            <Button variant="ghost" size="sm" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="md"
              onClick={handleStartNegotiation}
              isLoading={isStartingNegotiation}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Launch Live Negotiation
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
