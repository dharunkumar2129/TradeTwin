import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { Badge } from '../components/ui/Badge';
import { extractErrorMessage } from '../lib/utils';
import type { ShippingAddress, ShoppingPreferences } from '../types';
import {
  User as UserIcon,
  MapPin,
  Sliders,
  Save,
} from 'lucide-react';

const AVAILABLE_CURRENCIES = ['USD', 'EUR', 'GBP', 'CAD', 'AUD', 'JPY'];
const POPULAR_CATEGORIES = [
  'Electronics',
  'Audio & Acoustics',
  'Developer Tools',
  'Smart Devices',
  'Wearables',
  'Monitors & Displays',
];

export const ProfilePage: React.FC = () => {
  const { user, updateProfile, refreshCurrentUser } = useAuth();

  const [activeTab, setActiveTab] = useState<'general' | 'shipping' | 'preferences'>('general');
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [currency, setCurrency] = useState(user?.defaultCurrency || 'USD');

  // Shipping Address state
  const [shippingAddress, setShippingAddress] = useState<ShippingAddress>({
    fullName: user?.shippingAddress?.fullName || user?.fullName || '',
    addressLine1: user?.shippingAddress?.addressLine1 || '',
    addressLine2: user?.shippingAddress?.addressLine2 || '',
    city: user?.shippingAddress?.city || '',
    state: user?.shippingAddress?.state || '',
    postalCode: user?.shippingAddress?.postalCode || '',
    country: user?.shippingAddress?.country || 'United States',
  });

  // Shopping Preferences state
  const [shoppingPreferences, setShoppingPreferences] = useState<ShoppingPreferences>({
    defaultCurrency: user?.shoppingPreferences?.defaultCurrency || user?.defaultCurrency || 'USD',
    categoriesOfInterest: user?.shoppingPreferences?.categoriesOfInterest || ['Electronics', 'Audio & Acoustics'],
    priceSensitivity: user?.shoppingPreferences?.priceSensitivity || 'BALANCED',
    preferredWarrantyMonths: user?.shoppingPreferences?.preferredWarrantyMonths || 12,
    autoNegotiateDefault: user?.shoppingPreferences?.autoNegotiateDefault ?? true,
  });

  const [isSaving, setIsSaving] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Sync state when user object arrives/updates
  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setCurrency(user.defaultCurrency || 'USD');
      if (user.shippingAddress) {
        setShippingAddress({
          fullName: user.shippingAddress.fullName || user.fullName || '',
          addressLine1: user.shippingAddress.addressLine1 || '',
          addressLine2: user.shippingAddress.addressLine2 || '',
          city: user.shippingAddress.city || '',
          state: user.shippingAddress.state || '',
          postalCode: user.shippingAddress.postalCode || '',
          country: user.shippingAddress.country || 'United States',
        });
      }
      if (user.shoppingPreferences) {
        setShoppingPreferences({
          defaultCurrency: user.shoppingPreferences.defaultCurrency || user.defaultCurrency || 'USD',
          categoriesOfInterest: user.shoppingPreferences.categoriesOfInterest || ['Electronics'],
          priceSensitivity: user.shoppingPreferences.priceSensitivity || 'BALANCED',
          preferredWarrantyMonths: user.shoppingPreferences.preferredWarrantyMonths || 12,
          autoNegotiateDefault: user.shoppingPreferences.autoNegotiateDefault ?? true,
        });
      }
    }
  }, [user]);

  const toggleCategory = (cat: string) => {
    const current = shoppingPreferences.categoriesOfInterest || [];
    const exists = current.includes(cat);
    const updated = exists ? current.filter((c) => c !== cat) : [...current, cat];
    setShoppingPreferences({ ...shoppingPreferences, categoriesOfInterest: updated });
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setStatusMessage(null);

    try {
      await updateProfile({
        fullName,
        defaultCurrency: currency,
        shippingAddress,
        shoppingPreferences: {
          ...shoppingPreferences,
          defaultCurrency: currency,
        },
      });
      await refreshCurrentUser();
      setStatusMessage({
        type: 'success',
        text: 'Your profile, shipping address, and shopping preferences have been updated.',
      });
    } catch (err: unknown) {
      setStatusMessage({
        type: 'error',
        text: extractErrorMessage(err, 'Failed to update profile. Please try again.'),
      });
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 sm:px-6 space-y-6">
      {/* Profile Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-6 bg-white rounded-2xl border border-charcoal-200/80 shadow-subtle">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-600 to-primary-700 flex items-center justify-center text-white shadow-subtle font-bold text-xl">
            {fullName ? fullName.charAt(0).toUpperCase() : 'U'}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-charcoal-900">{fullName || 'Account User'}</h1>
              <Badge
                variant={
                  user?.role === 'ROLE_ADMIN'
                    ? 'primary'
                    : user?.role === 'ROLE_SELLER'
                    ? 'purple'
                    : 'outline'
                }
              >
                {user?.role === 'ROLE_ADMIN'
                  ? 'Administrator'
                  : user?.role === 'ROLE_SELLER'
                  ? 'Verified Merchant'
                  : 'Buyer Agent'}
              </Badge>
            </div>
            <p className="text-xs text-charcoal-500 mt-0.5">{user?.email}</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="px-3.5 py-1.5 rounded-xl bg-charcoal-50 border border-charcoal-200/70 text-right">
            <span className="text-[10px] uppercase font-semibold text-charcoal-400 block">Trust Rating</span>
            <span className="text-sm font-extrabold text-charcoal-900">
              ★ {user?.reputationScore ? user.reputationScore.toFixed(0) : '100'}/100
            </span>
          </div>
          <div className="px-3.5 py-1.5 rounded-xl bg-charcoal-50 border border-charcoal-200/70 text-right">
            <span className="text-[10px] uppercase font-semibold text-charcoal-400 block">Default Currency</span>
            <span className="text-sm font-extrabold text-primary-700">{currency}</span>
          </div>
        </div>
      </div>

      {statusMessage && (
        <Alert
          type={statusMessage.type}
          title={statusMessage.type === 'success' ? 'Profile Saved' : 'Error'}
        >
          {statusMessage.text}
        </Alert>
      )}

      {/* Tabs */}
      <div className="flex border-b border-charcoal-200 gap-2">
        <button
          type="button"
          onClick={() => setActiveTab('general')}
          className={`pb-3 px-3 text-xs font-semibold flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'general'
              ? 'border-primary-600 text-primary-700'
              : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          <UserIcon className="w-4 h-4" />
          General & Currency
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('shipping')}
          className={`pb-3 px-3 text-xs font-semibold flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'shipping'
              ? 'border-primary-600 text-primary-700'
              : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          <MapPin className="w-4 h-4" />
          Shipping Address
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('preferences')}
          className={`pb-3 px-3 text-xs font-semibold flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'preferences'
              ? 'border-primary-600 text-primary-700'
              : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          <Sliders className="w-4 h-4" />
          Shopping Preferences
        </button>
      </div>

      {/* Form Content */}
      <form onSubmit={handleSave} className="space-y-6">
        {/* Tab 1: General */}
        {activeTab === 'general' && (
          <Card className="p-6 space-y-5">
            <div>
              <h2 className="text-sm font-bold text-charcoal-900 flex items-center gap-2">
                <UserIcon className="w-4 h-4 text-primary-600" />
                Personal Profile
              </h2>
              <p className="text-xs text-charcoal-500 mt-0.5">
                Manage your name, primary email, and preferred base settlement currency.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Full Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Alice Walker"
                required
              />

              <div>
                <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                  Email Address
                </label>
                <input
                  type="email"
                  value={user?.email || ''}
                  disabled
                  className="w-full px-3.5 py-2 rounded-xl border border-charcoal-200 bg-charcoal-50 text-charcoal-500 text-sm cursor-not-allowed"
                />
                <span className="text-[10px] text-charcoal-400 mt-1 block">
                  Email cannot be changed directly for security and reputation preservation.
                </span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                Default Currency
              </label>
              <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
                {AVAILABLE_CURRENCIES.map((curr) => (
                  <button
                    key={curr}
                    type="button"
                    onClick={() => setCurrency(curr)}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all ${
                      currency === curr
                        ? 'border-primary-600 bg-primary-50 text-primary-800 ring-2 ring-primary-500/20'
                        : 'border-charcoal-200 bg-white text-charcoal-600 hover:bg-charcoal-50'
                    }`}
                  >
                    {curr}
                  </button>
                ))}
              </div>
              <span className="text-[10px] text-charcoal-400 mt-1 block">
                All autonomous agent negotiations and PayPal orders will be priced in this currency.
              </span>
            </div>
          </Card>
        )}

        {/* Tab 2: Shipping Address */}
        {activeTab === 'shipping' && (
          <Card className="p-6 space-y-5">
            <div>
              <h2 className="text-sm font-bold text-charcoal-900 flex items-center gap-2">
                <MapPin className="w-4 h-4 text-primary-600" />
                Default Delivery & Shipping Address
              </h2>
              <p className="text-xs text-charcoal-500 mt-0.5">
                Used to automatically dispatch orders won by your buyer agent upon checkout.
              </p>
            </div>

            <div className="space-y-4">
              <Input
                label="Recipient Full Name"
                value={shippingAddress.fullName || ''}
                onChange={(e) =>
                  setShippingAddress({ ...shippingAddress, fullName: e.target.value })
                }
                placeholder="Alice Walker"
                required
              />

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Street Address Line 1"
                  value={shippingAddress.addressLine1 || ''}
                  onChange={(e) =>
                    setShippingAddress({ ...shippingAddress, addressLine1: e.target.value })
                  }
                  placeholder="742 Evergreen Terrace"
                  required
                />
                <Input
                  label="Address Line 2 (Apt, Suite, Unit)"
                  value={shippingAddress.addressLine2 || ''}
                  onChange={(e) =>
                    setShippingAddress({ ...shippingAddress, addressLine2: e.target.value })
                  }
                  placeholder="Apt 4B"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <Input
                  label="City"
                  value={shippingAddress.city || ''}
                  onChange={(e) =>
                    setShippingAddress({ ...shippingAddress, city: e.target.value })
                  }
                  placeholder="Springfield"
                  required
                />
                <Input
                  label="State / Province / Region"
                  value={shippingAddress.state || ''}
                  onChange={(e) =>
                    setShippingAddress({ ...shippingAddress, state: e.target.value })
                  }
                  placeholder="OR"
                  required
                />
                <Input
                  label="Postal / ZIP Code"
                  value={shippingAddress.postalCode || ''}
                  onChange={(e) =>
                    setShippingAddress({ ...shippingAddress, postalCode: e.target.value })
                  }
                  placeholder="97477"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                  Country
                </label>
                <select
                  value={shippingAddress.country || 'United States'}
                  onChange={(e) =>
                    setShippingAddress({ ...shippingAddress, country: e.target.value })
                  }
                  className="w-full px-3.5 py-2 rounded-xl border border-charcoal-200 bg-white text-charcoal-800 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-600 transition-colors"
                >
                  <option value="United States">United States</option>
                  <option value="Canada">Canada</option>
                  <option value="United Kingdom">United Kingdom</option>
                  <option value="Germany">Germany</option>
                  <option value="Australia">Australia</option>
                  <option value="France">France</option>
                  <option value="Japan">Japan</option>
                </select>
              </div>
            </div>
          </Card>
        )}

        {/* Tab 3: Shopping Preferences */}
        {activeTab === 'preferences' && (
          <Card className="p-6 space-y-5">
            <div>
              <h2 className="text-sm font-bold text-charcoal-900 flex items-center gap-2">
                <Sliders className="w-4 h-4 text-primary-600" />
                Autonomous Shopping & Negotiation Guardrails
              </h2>
              <p className="text-xs text-charcoal-500 mt-0.5">
                Direct your autonomous buyer twin on how aggressively to negotiate and what deals to favor.
              </p>
            </div>

            {/* Price Sensitivity */}
            <div>
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-2">
                Agent Bargaining Sensitivity
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                {[
                  {
                    id: 'DISCOUNT_SEEKER',
                    label: 'Discount Seeker',
                    desc: 'Prioritizes steepest discounts, max rounds, and bonus perks.',
                  },
                  {
                    id: 'BALANCED',
                    label: 'Balanced Compromise',
                    desc: 'Aims for Pareto-optimal FairDeal agreement with high mutual utility.',
                  },
                  {
                    id: 'PREMIUM',
                    label: 'Fast Track / Premium',
                    desc: 'Favors quick settlement, express shipping, and longest warranty.',
                  },
                ].map((strat) => (
                  <button
                    key={strat.id}
                    type="button"
                    onClick={() =>
                      setShoppingPreferences({
                        ...shoppingPreferences,
                        priceSensitivity: strat.id as ShoppingPreferences['priceSensitivity'],
                      })
                    }
                    className={`p-3.5 rounded-xl border text-left transition-all ${
                      shoppingPreferences.priceSensitivity === strat.id
                        ? 'border-primary-600 bg-primary-50/70 ring-2 ring-primary-500/20'
                        : 'border-charcoal-200 bg-white hover:bg-charcoal-50'
                    }`}
                  >
                    <span className="block text-xs font-bold text-charcoal-900">{strat.label}</span>
                    <span className="block text-[11px] text-charcoal-500 mt-1 leading-snug">
                      {strat.desc}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            {/* Categories of Interest */}
            <div>
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-2">
                Preferred Product Categories
              </label>
              <div className="flex flex-wrap gap-2">
                {POPULAR_CATEGORIES.map((cat) => {
                  const isSelected = (shoppingPreferences.categoriesOfInterest || []).includes(cat);
                  return (
                    <button
                      key={cat}
                      type="button"
                      onClick={() => toggleCategory(cat)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                        isSelected
                          ? 'border-primary-600 bg-primary-600 text-white shadow-subtle'
                          : 'border-charcoal-200 bg-white text-charcoal-600 hover:bg-charcoal-100/70'
                      }`}
                    >
                      {cat} {isSelected && '✓'}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Preferred Warranty & Auto-Negotiate */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
              <div>
                <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                  Target Warranty Coverage
                </label>
                <select
                  value={shoppingPreferences.preferredWarrantyMonths || 12}
                  onChange={(e) =>
                    setShoppingPreferences({
                      ...shoppingPreferences,
                      preferredWarrantyMonths: parseInt(e.target.value, 10),
                    })
                  }
                  className="w-full px-3.5 py-2 rounded-xl border border-charcoal-200 bg-white text-charcoal-800 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-600 transition-colors"
                >
                  <option value="6">6 Months Basic</option>
                  <option value="12">12 Months Standard (Recommended)</option>
                  <option value="24">24 Months Extended Protection</option>
                  <option value="36">36 Months Full Hardware Cover</option>
                </select>
              </div>

              <div className="flex items-center justify-between p-3.5 rounded-xl border border-charcoal-200 bg-charcoal-50/60 mt-1">
                <div>
                  <span className="block text-xs font-bold text-charcoal-900">
                    Enable Autonomous Twin Negotiation
                  </span>
                  <span className="block text-[11px] text-charcoal-500 mt-0.5">
                    Allow buyer agent to negotiate within your ceiling price automatically.
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={shoppingPreferences.autoNegotiateDefault ?? true}
                  onChange={(e) =>
                    setShoppingPreferences({
                      ...shoppingPreferences,
                      autoNegotiateDefault: e.target.checked,
                    })
                  }
                  className="w-4 h-4 text-primary-600 rounded border-charcoal-300 focus:ring-primary-500 cursor-pointer"
                />
              </div>
            </div>
          </Card>
        )}

        {/* Submit Bar */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <Button
            type="submit"
            variant="primary"
            size="md"
            isLoading={isSaving}
            leftIcon={<Save className="w-4 h-4" />}
            className="rounded-xl font-bold px-6"
          >
            Save All Settings
          </Button>
        </div>
      </form>
    </div>
  );
};
