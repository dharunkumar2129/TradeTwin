import React, { useState } from 'react';
import type { BuyerIntent } from '../../types';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { formatCurrency } from '../../lib/utils';
import {
  Lock,
  Sparkles,
  CheckCircle2,
  Edit3,
  Bot,
  Truck,
  Shield,
  Send
} from 'lucide-react';

interface IntentReviewCardProps {
  intent: BuyerIntent;
  onConfirm: (confirmedIntent: BuyerIntent) => void;
  onRefineWithAnswer?: (answer: string) => void;
  isConfirming?: boolean;
}

export const IntentReviewCard: React.FC<IntentReviewCardProps> = ({
  intent,
  onConfirm,
  onRefineWithAnswer,
  isConfirming = false,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editedIntent, setEditedIntent] = useState<BuyerIntent>({ ...intent });
  const [followUpAnswer, setFollowUpAnswer] = useState('');

  // Sync state if external intent updates
  React.useEffect(() => {
    setEditedIntent({ ...intent });
  }, [intent]);

  const handleSaveEdit = () => {
    setIsEditing(false);
  };

  const handleConfirm = () => {
    onConfirm(editedIntent);
  };

  const handleSendFollowUp = (e: React.FormEvent) => {
    e.preventDefault();
    if (!followUpAnswer.trim() || !onRefineWithAnswer) return;
    onRefineWithAnswer(followUpAnswer.trim());
    setFollowUpAnswer('');
  };

  return (
    <Card className="border-primary-200 bg-gradient-to-b from-white to-primary-50/20 shadow-md p-6 space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-charcoal-200/80">
        <div className="flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-xl bg-primary-100/80 flex items-center justify-center text-primary-700 shadow-xs">
            <Sparkles className="w-5 h-5 text-primary-600" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-bold text-charcoal-900 text-base sm:text-lg">Structured Buyer Intent</h3>
              <Badge variant={intent.status === 'CONFIRMED' ? 'success' : 'primary'} size="sm">
                {intent.status === 'CONFIRMED' ? 'Confirmed & Verified' : 'AI Extracted Draft'}
              </Badge>
            </div>
            <p className="text-xs text-charcoal-500 mt-0.5">
              Review and confirm your commercial parameters before negotiations begin.
            </p>
          </div>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setIsEditing(!isEditing)}
          leftIcon={<Edit3 className="w-3.5 h-3.5" />}
          className="self-start sm:self-center text-xs"
        >
          {isEditing ? 'Done Editing' : 'Edit Parameters'}
        </Button>
      </div>

      {/* AI Ambiguity Follow-up Box */}
      {intent.followUpQuestion && (
        <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-4 space-y-3">
          <div className="flex items-start gap-2.5">
            <Bot className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <span className="text-xs font-bold text-amber-900 block">Agent Clarification Needed</span>
              <p className="text-xs text-amber-800 leading-relaxed">{intent.followUpQuestion}</p>
            </div>
          </div>

          {onRefineWithAnswer && (
            <form onSubmit={handleSendFollowUp} className="flex items-center gap-2 pt-1">
              <Input
                value={followUpAnswer}
                onChange={(e) => setFollowUpAnswer(e.target.value)}
                placeholder="Type your clarification (e.g. 'Over-ear headphones under $100')..."
                className="bg-white text-xs"
              />
              <Button type="submit" variant="primary" size="sm" rightIcon={<Send className="w-3.5 h-3.5" />}>
                Clarify
              </Button>
            </form>
          )}
        </div>
      )}

      {/* Parameters Grid */}
      {!isEditing ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {/* Product Type */}
          <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/90 shadow-2xs space-y-1">
            <span className="text-[11px] font-medium text-charcoal-400 block uppercase tracking-wider">Product Type</span>
            <span className="text-sm font-bold text-charcoal-900 block truncate" title={editedIntent.productType}>
              {editedIntent.productType || 'Any Electronics'}
            </span>
          </div>

          {/* Brand Preferences */}
          <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/90 shadow-2xs space-y-1">
            <span className="text-[11px] font-medium text-charcoal-400 block uppercase tracking-wider">Brand</span>
            <div className="flex flex-wrap gap-1">
              {editedIntent.brandPreferences && editedIntent.brandPreferences.length > 0 ? (
                editedIntent.brandPreferences.map((b, i) => (
                  <Badge key={i} variant="primary" size="sm">
                    {b}
                  </Badge>
                ))
              ) : (
                <span className="text-xs text-charcoal-500 font-medium">Any Verified Brand</span>
              )}
            </div>
          </div>

          {/* Maximum Budget (Private) */}
          <div className="p-3.5 rounded-xl bg-white border border-emerald-200 shadow-2xs space-y-1 relative">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-medium text-emerald-800 uppercase tracking-wider">Max Budget</span>
              <span className="inline-flex items-center gap-0.5 text-[10px] font-bold text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200">
                <Lock className="w-2.5 h-2.5" /> Confidential
              </span>
            </div>
            <span className="text-base font-extrabold text-charcoal-900 block">
              {editedIntent.maxBudget ? formatCurrency(editedIntent.maxBudget) : 'Flexible'}
            </span>
            <span className="text-[10px] text-charcoal-400 block truncate">
              Target: {editedIntent.targetBudget ? formatCurrency(editedIntent.targetBudget) : 'Optimized'}
            </span>
          </div>

          {/* Quantity */}
          <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/90 shadow-2xs space-y-1">
            <span className="text-[11px] font-medium text-charcoal-400 block uppercase tracking-wider">Quantity</span>
            <span className="text-sm font-bold text-charcoal-900 block">
              {editedIntent.quantity || 1} unit(s)
            </span>
          </div>

          {/* Warranty */}
          <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/90 shadow-2xs space-y-1">
            <div className="flex items-center gap-1 text-[11px] font-medium text-charcoal-400 uppercase tracking-wider">
              <Shield className="w-3 h-3 text-primary-600" />
              <span>Min Warranty</span>
            </div>
            <span className="text-sm font-bold text-charcoal-900 block">
              {editedIntent.minWarrantyMonths ? `${editedIntent.minWarrantyMonths} Months` : 'Standard'}
            </span>
          </div>

          {/* Delivery Window */}
          <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/90 shadow-2xs space-y-1">
            <div className="flex items-center gap-1 text-[11px] font-medium text-charcoal-400 uppercase tracking-wider">
              <Truck className="w-3 h-3 text-primary-600" />
              <span>Max Delivery</span>
            </div>
            <span className="text-sm font-bold text-charcoal-900 block">
              {editedIntent.maxDeliveryDays ? `${editedIntent.maxDeliveryDays} Days or less` : 'Standard'}
            </span>
          </div>

          {/* Preferred Condition */}
          <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/90 shadow-2xs space-y-1">
            <span className="text-[11px] font-medium text-charcoal-400 block uppercase tracking-wider">Condition</span>
            <span className="text-sm font-bold text-charcoal-900 block">
              {editedIntent.preferredConditions?.[0] || 'Brand New'}
            </span>
          </div>

          {/* Shipping Location */}
          <div className="p-3.5 rounded-xl bg-white border border-charcoal-200/90 shadow-2xs space-y-1">
            <span className="text-[11px] font-medium text-charcoal-400 block uppercase tracking-wider">Shipping</span>
            <span className="text-sm font-bold text-charcoal-900 block truncate">
              {editedIntent.shippingLocation || 'United States'}
            </span>
          </div>
        </div>
      ) : (
        /* In-Place Editing Form */
        <div className="bg-white rounded-xl border border-charcoal-200 p-4 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="text-xs font-semibold text-charcoal-700 block mb-1">Product Type</label>
              <Input
                value={editedIntent.productType || ''}
                onChange={(e) => setEditedIntent({ ...editedIntent, productType: e.target.value })}
                className="text-xs"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-charcoal-700 block mb-1">Max Ceiling Budget ($)</label>
              <Input
                type="number"
                value={editedIntent.maxBudget || ''}
                onChange={(e) => setEditedIntent({ ...editedIntent, maxBudget: Number(e.target.value) })}
                className="text-xs"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-charcoal-700 block mb-1">Quantity</label>
              <Input
                type="number"
                min={1}
                value={editedIntent.quantity || 1}
                onChange={(e) => setEditedIntent({ ...editedIntent, quantity: Number(e.target.value) })}
                className="text-xs"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-charcoal-700 block mb-1">Min Warranty (Months)</label>
              <Input
                type="number"
                value={editedIntent.minWarrantyMonths || 12}
                onChange={(e) => setEditedIntent({ ...editedIntent, minWarrantyMonths: Number(e.target.value) })}
                className="text-xs"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-charcoal-700 block mb-1">Max Delivery (Days)</label>
              <Input
                type="number"
                value={editedIntent.maxDeliveryDays || 4}
                onChange={(e) => setEditedIntent({ ...editedIntent, maxDeliveryDays: Number(e.target.value) })}
                className="text-xs"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-charcoal-700 block mb-1">Condition</label>
              <select
                value={editedIntent.preferredConditions?.[0] || 'Brand New'}
                onChange={(e) => setEditedIntent({ ...editedIntent, preferredConditions: [e.target.value] })}
                className="w-full text-xs rounded-lg border border-charcoal-200 px-3 py-2 bg-white text-charcoal-900"
              >
                <option value="Brand New">Brand New</option>
                <option value="Open Box">Open Box</option>
                <option value="Refurbished">Refurbished</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-charcoal-100">
            <Button type="button" variant="primary" size="sm" onClick={handleSaveEdit}>
              Apply Changes
            </Button>
          </div>
        </div>
      )}

      {/* Specifications & Required Attributes */}
      {editedIntent.requiredSpecifications && Object.keys(editedIntent.requiredSpecifications).length > 0 && (
        <div className="space-y-1.5 pt-1">
          <span className="text-xs font-semibold text-charcoal-700 block">Required Specifications:</span>
          <div className="flex flex-wrap gap-2">
            {Object.entries(editedIntent.requiredSpecifications).map(([k, v], idx) => (
              <span
                key={idx}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-white border border-charcoal-200 text-xs text-charcoal-700 shadow-2xs"
              >
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                <span className="font-semibold text-charcoal-900 uppercase text-[10px]">{k}:</span> {v}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Security Guardrail Callout */}
      <div className="rounded-lg bg-charcoal-900 text-white p-3.5 flex items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-lg bg-charcoal-800 flex items-center justify-center shrink-0">
            <Lock className="w-4 h-4 text-emerald-400" />
          </div>
          <p className="text-xs text-charcoal-200 leading-relaxed">
            <strong className="text-white">Strict Buyer Guardrail:</strong> Your maximum ceiling budget of{' '}
            <span className="font-bold text-emerald-300">
              {editedIntent.maxBudget ? formatCurrency(editedIntent.maxBudget) : '$100.00'}
            </span>{' '}
            is strictly encrypted server-side and never revealed to merchant seller agents.
          </p>
        </div>

        {editedIntent.status !== 'CONFIRMED' && (
          <Button
            type="button"
            variant="primary"
            size="sm"
            isLoading={isConfirming}
            onClick={handleConfirm}
            className="shrink-0 rounded-lg shadow-sm"
          >
            Confirm Intent
          </Button>
        )}
      </div>
    </Card>
  );
};
