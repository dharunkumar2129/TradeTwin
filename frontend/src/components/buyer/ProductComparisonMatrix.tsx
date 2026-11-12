import React, { useState } from 'react';
import type { SearchResultItem, BuyerIntent } from '../../types';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { formatCurrency } from '../../lib/utils';
import {
  ShieldCheck,
  Truck,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  Store,
  Layers,
  Zap,
  Info
} from 'lucide-react';

interface ProductComparisonMatrixProps {
  candidates: SearchResultItem[];
  intent: BuyerIntent;
  onSelectForNegotiation: (item: SearchResultItem) => void;
  isStartingNegotiation?: boolean;
}

export const ProductComparisonMatrix: React.FC<ProductComparisonMatrixProps> = ({
  candidates,
  intent,
  onSelectForNegotiation,
  isStartingNegotiation = false,
}) => {
  const [showTableView, setShowTableView] = useState(false);
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);

  if (!candidates || candidates.length === 0) {
    return (
      <Card className="p-8 text-center bg-white border border-charcoal-200 space-y-3">
        <Info className="w-10 h-10 text-charcoal-400 mx-auto" />
        <h4 className="font-bold text-charcoal-800 text-base">No Matching Merchant Listings Found</h4>
        <p className="text-xs text-charcoal-500 max-w-md mx-auto">
          We couldn't find active listings matching all constraints. Try adjusting your maximum budget, delivery timeframe, or brand preferences.
        </p>
      </Card>
    );
  }

  const handleNegotiateClick = (item: SearchResultItem) => {
    setSelectedItemId(item.product.id);
    onSelectForNegotiation(item);
  };

  return (
    <div className="space-y-6 pt-2">
      {/* Header and View Toggle */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-charcoal-200">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h3 className="text-xl font-extrabold text-charcoal-900 tracking-tight">
              Best Eligible Merchant Listings
            </h3>
            <Badge variant="primary" size="sm">
              {candidates.length} Ranked Options
            </Badge>
            <Badge variant="warning" size="sm">DEMO DATA</Badge>
          </div>
          <p className="text-xs text-charcoal-500 mt-1">
            Deterministic MongoDB retrieval verified against real merchant stock, prices, and warranties.
          </p>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setShowTableView(!showTableView)}
          leftIcon={<Layers className="w-3.5 h-3.5" />}
          className="self-start sm:self-center text-xs"
        >
          {showTableView ? 'Show Card View' : 'Side-by-Side Spec Table'}
        </Button>
      </div>

      {/* Card Comparison View */}
      {!showTableView ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {candidates.map((item, index) => {
            const isTopRanked = index === 0;
            const savingsVsBudget =
              intent.maxBudget && item.estimatedTotalPrice < intent.maxBudget
                ? intent.maxBudget - item.estimatedTotalPrice
                : 0;

            return (
              <Card
                key={item.product.id}
                className={`flex flex-col justify-between overflow-hidden transition-all duration-200 ${
                  isTopRanked
                    ? 'border-2 border-primary-500 shadow-md ring-2 ring-primary-500/10'
                    : 'border border-charcoal-200/90 shadow-subtle hover:border-charcoal-300'
                }`}
              >
                <div>
                  {/* Top Badge Strip */}
                  <div className="p-4 pb-0 flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      {isTopRanked ? (
                        <Badge variant="success" size="sm" className="font-bold">
                          ★ #1 Best Match
                        </Badge>
                      ) : (
                        <Badge variant="default" size="sm">
                          Option #{index + 1}
                        </Badge>
                      )}
                      <span className="text-xs font-bold text-primary-700 bg-primary-50 px-2 py-0.5 rounded-full border border-primary-200">
                        {item.matchScore}% Match
                      </span>
                    </div>

                    <Badge variant="outline" size="sm">
                      {item.condition || 'Brand New'}
                    </Badge>
                  </div>

                  {/* Product Image */}
                  <div className="p-4 pt-3">
                    <div className="aspect-[16/10] rounded-xl overflow-hidden bg-charcoal-100 relative group">
                      {item.product.images?.[0] ? (
                        <img
                          src={item.product.images[0]}
                          alt={item.product.title}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-charcoal-400">
                          <Store className="w-8 h-8" />
                        </div>
                      )}
                      <div className="absolute top-2.5 right-2.5">
                        <span className="bg-white/95 backdrop-blur-sm text-[11px] font-bold text-charcoal-800 px-2 py-0.5 rounded shadow-xs flex items-center gap-1">
                          <Zap className="w-3 h-3 text-primary-600 fill-primary-600" /> Auto-Negotiate
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Title & Seller Store */}
                  <div className="px-4 space-y-2">
                    <h4 className="font-bold text-charcoal-900 text-sm line-clamp-2 leading-snug" title={item.product.title}>
                      {item.product.title}
                    </h4>

                    <div className="flex items-center justify-between text-xs text-charcoal-500 pt-1">
                      <span className="flex items-center gap-1 font-medium text-charcoal-700">
                        <Store className="w-3.5 h-3.5 text-charcoal-400" />
                        {item.sellerStoreName}
                      </span>
                      <span className="font-semibold text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded text-[11px]">
                        ★ {item.sellerReputation.toFixed(1)}% Rep
                      </span>
                    </div>
                  </div>

                  {/* Price & Logistics Breakdown */}
                  <div className="mx-4 mt-3 p-3 rounded-xl bg-charcoal-50 border border-charcoal-100 space-y-2 text-xs">
                    <div className="flex justify-between items-baseline">
                      <span className="text-charcoal-500">Unit Asking Price:</span>
                      <span className="font-semibold text-charcoal-800">{formatCurrency(item.unitPrice)}</span>
                    </div>
                    <div className="flex justify-between items-baseline">
                      <span className="text-charcoal-500">Shipping:</span>
                      <span className="font-semibold text-charcoal-800">
                        {item.shippingCost > 0 ? formatCurrency(item.shippingCost) : 'FREE'}
                      </span>
                    </div>
                    <div className="flex justify-between items-baseline pt-1.5 border-t border-charcoal-200">
                      <span className="font-bold text-charcoal-900">Estimated Total:</span>
                      <span className="text-base font-extrabold text-charcoal-900">
                        {formatCurrency(item.estimatedTotalPrice)}
                      </span>
                    </div>

                    {savingsVsBudget > 0 && (
                      <div className="text-[11px] font-semibold text-emerald-700 bg-emerald-100/60 px-2 py-0.5 rounded text-center">
                        Saves {formatCurrency(savingsVsBudget)} vs your max budget!
                      </div>
                    )}
                  </div>

                  {/* Key Highlights */}
                  <div className="px-4 pt-3 space-y-1.5 text-xs">
                    <div className="flex items-center gap-1.5 text-charcoal-600">
                      <Truck className="w-3.5 h-3.5 text-primary-600 shrink-0" />
                      <span>
                        Delivery in <strong>{item.estimatedDeliveryDays} days</strong>{' '}
                        {intent.maxDeliveryDays && item.estimatedDeliveryDays <= intent.maxDeliveryDays && (
                          <span className="text-emerald-700 text-[11px] font-semibold">(within {intent.maxDeliveryDays}d limit)</span>
                        )}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 text-charcoal-600">
                      <ShieldCheck className="w-3.5 h-3.5 text-primary-600 shrink-0" />
                      <span>
                        <strong>{item.warrantyInfo}</strong>{' '}
                        {intent.minWarrantyMonths && item.warrantyMonths >= intent.minWarrantyMonths && (
                          <span className="text-emerald-700 text-[11px] font-semibold">(meets {intent.minWarrantyMonths}mo min)</span>
                        )}
                      </span>
                    </div>

                    {/* Match highlights pills */}
                    {item.matchHighlights && item.matchHighlights.length > 0 && (
                      <div className="pt-1.5 space-y-1">
                        {item.matchHighlights.slice(0, 2).map((h, i) => (
                          <div key={i} className="flex items-start gap-1 text-[11px] text-emerald-800">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600 shrink-0 mt-0.5" />
                            <span>{h}</span>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Trade-offs pills if any */}
                    {item.tradeOffs && item.tradeOffs.length > 0 && (
                      <div className="pt-1 space-y-1">
                        {item.tradeOffs.slice(0, 1).map((t, i) => (
                          <div key={i} className="flex items-start gap-1 text-[11px] text-amber-800">
                            <AlertTriangle className="w-3 h-3 text-amber-600 shrink-0 mt-0.5" />
                            <span>{t}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {/* Bottom Action CTA */}
                <div className="p-4 pt-4 border-t border-charcoal-100 mt-4">
                  <Button
                    type="button"
                    variant={isTopRanked ? 'primary' : 'outline'}
                    size="md"
                    isLoading={isStartingNegotiation && selectedItemId === item.product.id}
                    onClick={() => handleNegotiateClick(item)}
                    rightIcon={<ArrowRight className="w-4 h-4" />}
                    className="w-full rounded-xl font-semibold shadow-xs"
                  >
                    Deploy Agent to Negotiate
                  </Button>
                </div>
              </Card>
            );
          })}
        </div>
      ) : (
        /* Detailed Side-by-Side Comparison Matrix Table */
        <div className="overflow-x-auto rounded-2xl border border-charcoal-200 bg-white shadow-card">
          <table className="w-full text-xs text-left">
            <thead className="bg-charcoal-50 border-b border-charcoal-200 text-charcoal-700 uppercase tracking-wider text-[11px]">
              <tr>
                <th className="p-4 font-bold">Listing Attribute</th>
                {candidates.map((item, idx) => (
                  <th key={item.product.id} className="p-4 font-bold min-w-[200px]">
                    Option #{idx + 1}: {item.product.brand}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-charcoal-100">
              {/* Product Title */}
              <tr>
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Product</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4">
                    <span className="font-bold text-charcoal-900 block">{item.product.title}</span>
                    <span className="text-[11px] text-charcoal-500">{item.product.category}</span>
                  </td>
                ))}
              </tr>

              {/* Match Score */}
              <tr>
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Compatibility Score</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4">
                    <Badge variant="primary" size="sm" className="font-bold">
                      {item.matchScore}% Match
                    </Badge>
                  </td>
                ))}
              </tr>

              {/* Estimated Total Price */}
              <tr>
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Estimated Total</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4">
                    <span className="text-sm font-extrabold text-charcoal-900 block">
                      {formatCurrency(item.estimatedTotalPrice)}
                    </span>
                    <span className="text-[10px] text-charcoal-400">
                      ({formatCurrency(item.unitPrice)} + {item.shippingCost > 0 ? formatCurrency(item.shippingCost) : 'Free Shipping'})
                    </span>
                  </td>
                ))}
              </tr>

              {/* Seller */}
              <tr>
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Merchant Store</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4 text-charcoal-800 font-medium">
                    {item.sellerStoreName} (★ {item.sellerReputation.toFixed(1)}%)
                  </td>
                ))}
              </tr>

              {/* Delivery Window */}
              <tr>
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Delivery Window</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4">
                    <span className="font-bold text-charcoal-800">{item.estimatedDeliveryDays} Days</span>
                  </td>
                ))}
              </tr>

              {/* Warranty Coverage */}
              <tr>
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Warranty Coverage</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4 text-charcoal-800">
                    {item.warrantyInfo}
                  </td>
                ))}
              </tr>

              {/* Condition */}
              <tr>
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Condition</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4 text-charcoal-800 font-medium">
                    {item.condition}
                  </td>
                ))}
              </tr>

              {/* Available Inventory */}
              <tr>
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Stock Available</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4 text-charcoal-800">
                    {item.availableInventory} in stock
                  </td>
                ))}
              </tr>

              {/* Action row */}
              <tr className="bg-charcoal-50/30">
                <td className="p-4 font-semibold text-charcoal-700 bg-charcoal-50/50">Action</td>
                {candidates.map((item) => (
                  <td key={item.product.id} className="p-4">
                    <Button
                      type="button"
                      variant="primary"
                      size="sm"
                      isLoading={isStartingNegotiation && selectedItemId === item.product.id}
                      onClick={() => handleNegotiateClick(item)}
                      rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
                      className="rounded-lg text-xs"
                    >
                      Start Negotiation
                    </Button>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
