import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../api/client';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { extractErrorMessage } from '../lib/utils';
import type { SellerProfile, NegotiationStrategy } from '../types';
import {
  Store,
  Phone,
  Truck,
  Bot,
  CheckCircle2,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  Mail,
  Building,
} from 'lucide-react';

const REGION_OPTIONS = [
  'United States',
  'Canada',
  'United Kingdom',
  'European Union',
  'Australia',
  'Worldwide',
];

export const SellerOnboardingPage: React.FC = () => {
  const { user, refreshCurrentUser } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState<number>(1);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form fields
  // Step 1: Store Basics
  const [storeName, setStoreName] = useState('');
  const [description, setDescription] = useState('');

  // Step 2: Branding & Contact
  const [logoUrl, setLogoUrl] = useState('');
  const [phone, setPhone] = useState('');
  const [supportEmail, setSupportEmail] = useState('');
  const [supportHours, setSupportHours] = useState('Mon-Fri 09:00 - 18:00 EST');

  // Step 3: Logistics & Terms
  const [shippingRegions, setShippingRegions] = useState<string[]>(['United States', 'Canada']);
  const [handlingTimeDays, setHandlingTimeDays] = useState<number>(2);
  const [freeShippingThreshold, setFreeShippingThreshold] = useState<number>(75);
  const [allowExpressShipping, setAllowExpressShipping] = useState<boolean>(true);
  const [returnPolicyDays, setReturnPolicyDays] = useState<number>(30);

  // Step 4: AI Negotiation Strategy & Guardrails
  const [defaultStrategy, setDefaultStrategy] = useState<NegotiationStrategy>('BALANCED');
  const [autoNegotiateEnabled, setAutoNegotiateEnabled] = useState<boolean>(true);
  const [minimumReputationRequired, setMinimumReputationRequired] = useState<number>(70);

  // Load existing profile if any
  useEffect(() => {
    const fetchExistingProfile = async () => {
      try {
        const res = await apiClient.get<{ data: SellerProfile }>('/sellers/me');
        if (res.data?.data) {
          const p = res.data.data;
          if (p.storeName) setStoreName(p.storeName);
          if (p.description) setDescription(p.description);
          if (p.logoUrl) setLogoUrl(p.logoUrl);
          if (p.businessContact?.phone) setPhone(p.businessContact.phone);
          if (p.businessContact?.supportEmail) setSupportEmail(p.businessContact.supportEmail);
          if (p.businessContact?.supportHours) setSupportHours(p.businessContact.supportHours);
          if (p.shippingRegions && p.shippingRegions.length > 0) setShippingRegions(p.shippingRegions);
          if (p.defaultShippingTerms?.handlingTimeDays) setHandlingTimeDays(p.defaultShippingTerms.handlingTimeDays);
          if (p.defaultShippingTerms?.freeShippingThreshold != null) setFreeShippingThreshold(p.defaultShippingTerms.freeShippingThreshold);
          if (p.defaultShippingTerms?.allowExpressShipping != null) setAllowExpressShipping(p.defaultShippingTerms.allowExpressShipping);
          if (p.defaultShippingTerms?.returnPolicyDays != null) setReturnPolicyDays(p.defaultShippingTerms.returnPolicyDays);
          if (p.defaultStrategy) setDefaultStrategy(p.defaultStrategy);
          if (p.autoNegotiateEnabled != null) setAutoNegotiateEnabled(p.autoNegotiateEnabled);
          if (p.minimumReputationRequired != null) setMinimumReputationRequired(p.minimumReputationRequired);
          if (p.onboardingStep && p.onboardingStep > 1 && !p.onboardingCompleted) {
            setStep(p.onboardingStep);
          }
        }
      } catch {
        // If profile doesn't exist yet, default with user's info
        if (user) {
          setStoreName(user.fullName ? `${user.fullName}'s Official Store` : 'My Merchant Store');
          setSupportEmail(user.email || '');
        }
      } finally {
        setIsLoading(false);
      }
    };

    fetchExistingProfile();
  }, [user]);

  const toggleRegion = (region: string) => {
    if (shippingRegions.includes(region)) {
      if (shippingRegions.length > 1) {
        setShippingRegions(shippingRegions.filter((r) => r !== region));
      }
    } else {
      setShippingRegions([...shippingRegions, region]);
    }
  };

  const validateStep = (currentStep: number): boolean => {
    setErrorMessage(null);
    if (currentStep === 1) {
      if (!storeName.trim() || storeName.trim().length < 3) {
        setErrorMessage('Store name must be at least 3 characters.');
        return false;
      }
      if (!description.trim() || description.trim().length < 10) {
        setErrorMessage('Store description must be at least 10 characters.');
        return false;
      }
    } else if (currentStep === 2) {
      if (supportEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(supportEmail)) {
        setErrorMessage('Please enter a valid support email address.');
        return false;
      }
    } else if (currentStep === 3) {
      if (shippingRegions.length === 0) {
        setErrorMessage('Please select at least one shipping region.');
        return false;
      }
      if (freeShippingThreshold < 0) {
        setErrorMessage('Free shipping threshold cannot be negative.');
        return false;
      }
    }
    return true;
  };

  const handleNext = () => {
    if (validateStep(step)) {
      setStep((prev) => Math.min(4, prev + 1));
    }
  };

  const handleBack = () => {
    setErrorMessage(null);
    setStep((prev) => Math.max(1, prev - 1));
  };

  const handleSubmit = async () => {
    if (!validateStep(step)) return;
    setIsSubmitting(true);
    setErrorMessage(null);

    const payload = {
      storeName: storeName.trim(),
      description: description.trim(),
      logoUrl: logoUrl.trim() || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=400&q=80',
      businessContact: {
        phone: phone.trim() || '+1 (555) 019-2834',
        supportEmail: supportEmail.trim() || user?.email || 'support@merchant.com',
        supportHours: supportHours.trim() || 'Mon-Fri 09:00 - 18:00 EST',
      },
      shippingRegions,
      defaultShippingTerms: {
        handlingTimeDays,
        freeShippingThreshold,
        allowExpressShipping,
        returnPolicyDays,
      },
      defaultStrategy,
      autoNegotiateEnabled,
      minimumReputationRequired,
      onboardingCompleted: true,
      onboardingStep: 4,
    };

    try {
      await apiClient.post('/sellers/onboard', payload);
      await refreshCurrentUser();
      setSuccessMessage('Merchant store onboarded and verified successfully! Redirecting...');
      setTimeout(() => {
        navigate('/seller/dashboard');
      }, 1500);
    } catch (err: unknown) {
      setErrorMessage(extractErrorMessage(err, 'Failed to complete seller onboarding. Please try again.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const stepsMeta = [
    { num: 1, label: 'Store Identity', icon: Store },
    { num: 2, label: 'Branding & Contact', icon: Phone },
    { num: 3, label: 'Shipping Terms', icon: Truck },
    { num: 4, label: 'AI Agent Governance', icon: Bot },
  ];

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto py-16 text-center">
        <div className="w-10 h-10 border-4 border-purple-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
        <p className="text-xs text-charcoal-500 font-semibold">Loading merchant onboarding portal...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto py-8 px-4 sm:px-6 space-y-8">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-purple-600 to-purple-800 text-white flex items-center justify-center mx-auto shadow-subtle">
          <Store className="w-6 h-6" />
        </div>
        <h1 className="text-2xl font-black text-charcoal-900 tracking-tight">Merchant Onboarding Portal</h1>
        <p className="text-xs text-charcoal-500 max-w-md mx-auto">
          Establish your store profile, verified logistics standards, and autonomous AI negotiation rules.
        </p>
      </div>

      {/* Multi-Step Wizard Progress Bar */}
      <div className="bg-white p-4 rounded-2xl border border-charcoal-200/80 shadow-subtle">
        <div className="grid grid-cols-4 gap-2">
          {stepsMeta.map((s) => {
            const Icon = s.icon;
            const isDone = step > s.num;
            const isCurrent = step === s.num;
            return (
              <div
                key={s.num}
                onClick={() => {
                  if (s.num < step) setStep(s.num);
                }}
                className={`flex flex-col items-center text-center p-2 rounded-xl transition-all ${
                  isCurrent
                    ? 'bg-purple-50 text-purple-900 font-bold'
                    : isDone
                    ? 'text-purple-700 cursor-pointer hover:bg-charcoal-50'
                    : 'text-charcoal-400'
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold mb-1.5 transition-colors ${
                    isCurrent
                      ? 'bg-purple-600 text-white shadow-subtle'
                      : isDone
                      ? 'bg-purple-200 text-purple-900'
                      : 'bg-charcoal-100 text-charcoal-500'
                  }`}
                >
                  {isDone ? <CheckCircle2 className="w-4 h-4" /> : <Icon className="w-4 h-4" />}
                </div>
                <span className="text-[11px] leading-tight hidden sm:block">{s.label}</span>
                <span className="text-[10px] text-charcoal-400 mt-0.5 sm:hidden">Step {s.num}</span>
              </div>
            );
          })}
        </div>

        {/* Linear progress fill */}
        <div className="w-full bg-charcoal-100 h-1.5 rounded-full mt-3 overflow-hidden">
          <div
            className="bg-purple-600 h-full transition-all duration-300 rounded-full"
            style={{ width: `${(step / 4) * 100}%` }}
          />
        </div>
      </div>

      {errorMessage && (
        <Alert type="error" title="Onboarding Validation Error">
          {errorMessage}
        </Alert>
      )}

      {successMessage && (
        <Alert type="success" title="Success!">
          {successMessage}
        </Alert>
      )}

      {/* Step 1: Store Basics */}
      {step === 1 && (
        <Card className="p-6 space-y-5">
          <div className="border-b border-charcoal-100 pb-3">
            <h2 className="text-base font-bold text-charcoal-900 flex items-center gap-2">
              <Building className="w-4 h-4 text-purple-600" />
              Step 1: Store Identity & Description
            </h2>
            <p className="text-xs text-charcoal-500 mt-0.5">
              Buyers and buyer AI agents will evaluate your store reputation based on this profile.
            </p>
          </div>

          <div className="space-y-4">
            <Input
              label="Store / Merchant Name"
              value={storeName}
              onChange={(e) => setStoreName(e.target.value)}
              placeholder="e.g. QuantumTech Direct"
              required
            />

            <div>
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                Store Description & Value Proposition
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                className="w-full px-3.5 py-2.5 rounded-xl border border-charcoal-200 text-charcoal-900 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-600"
                placeholder="Describe your catalog, warranty standards, customer focus, and what sets your products apart..."
                required
              />
              <span className="text-[10px] text-charcoal-400 mt-1 block">
                Minimum 10 characters. This bio is parsed by buyer agents to assess seller credibility.
              </span>
            </div>
          </div>
        </Card>
      )}

      {/* Step 2: Branding & Contact */}
      {step === 2 && (
        <Card className="p-6 space-y-5">
          <div className="border-b border-charcoal-100 pb-3">
            <h2 className="text-base font-bold text-charcoal-900 flex items-center gap-2">
              <Mail className="w-4 h-4 text-purple-600" />
              Step 2: Brand Assets & Customer Support Contact
            </h2>
            <p className="text-xs text-charcoal-500 mt-0.5">
              Direct point of contact for customer resolution and PayPal order compliance.
            </p>
          </div>

          <div className="space-y-4">
            <div className="flex items-center gap-4 p-3 bg-charcoal-50 rounded-xl border border-charcoal-200">
              <div className="w-14 h-14 rounded-xl bg-charcoal-200 flex items-center justify-center overflow-hidden border border-charcoal-300">
                {logoUrl ? (
                  <img
                    src={logoUrl}
                    alt="Store Logo"
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      e.currentTarget.src =
                        'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=400&q=80';
                    }}
                  />
                ) : (
                  <Store className="w-6 h-6 text-charcoal-400" />
                )}
              </div>
              <div className="flex-1">
                <Input
                  label="Store Logo Image URL"
                  value={logoUrl}
                  onChange={(e) => setLogoUrl(e.target.value)}
                  placeholder="https://example.com/logo.png"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Customer Support Email"
                type="email"
                value={supportEmail}
                onChange={(e) => setSupportEmail(e.target.value)}
                placeholder="support@yourstore.com"
                required
              />
              <Input
                label="Business Phone Number"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 (555) 234-5678"
              />
            </div>

            <Input
              label="Operating & Support Hours"
              value={supportHours}
              onChange={(e) => setSupportHours(e.target.value)}
              placeholder="Mon-Fri 09:00 - 18:00 EST"
            />
          </div>
        </Card>
      )}

      {/* Step 3: Shipping & Logistics */}
      {step === 3 && (
        <Card className="p-6 space-y-5">
          <div className="border-b border-charcoal-100 pb-3">
            <h2 className="text-base font-bold text-charcoal-900 flex items-center gap-2">
              <Truck className="w-4 h-4 text-purple-600" />
              Step 3: Shipping Regions & Default Logistics Terms
            </h2>
            <p className="text-xs text-charcoal-500 mt-0.5">
              These terms form the baseline for autonomous deal concessions during negotiation rounds.
            </p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-2">
                Eligible Shipping Regions
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {REGION_OPTIONS.map((region) => {
                  const selected = shippingRegions.includes(region);
                  return (
                    <button
                      key={region}
                      type="button"
                      onClick={() => toggleRegion(region)}
                      className={`p-2.5 rounded-xl border text-xs font-semibold transition-all ${
                        selected
                          ? 'border-purple-600 bg-purple-50 text-purple-900 ring-2 ring-purple-500/20'
                          : 'border-charcoal-200 bg-white text-charcoal-600 hover:bg-charcoal-50'
                      }`}
                    >
                      {region} {selected && '✓'}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
              <div>
                <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                  Handling Time
                </label>
                <select
                  value={handlingTimeDays}
                  onChange={(e) => setHandlingTimeDays(parseInt(e.target.value, 10))}
                  className="w-full px-3.5 py-2 rounded-xl border border-charcoal-200 bg-white text-charcoal-800 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-600"
                >
                  <option value="1">1 Business Day (Same day/Next day)</option>
                  <option value="2">2 Business Days (Standard)</option>
                  <option value="3">3 Business Days</option>
                  <option value="5">5 Business Days (Made to order)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                  Free Shipping Minimum ($)
                </label>
                <input
                  type="number"
                  min="0"
                  step="5"
                  value={freeShippingThreshold}
                  onChange={(e) => setFreeShippingThreshold(parseFloat(e.target.value) || 0)}
                  className="w-full px-3.5 py-2 rounded-xl border border-charcoal-200 bg-white text-charcoal-800 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-600"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                  Return Window
                </label>
                <select
                  value={returnPolicyDays}
                  onChange={(e) => setReturnPolicyDays(parseInt(e.target.value, 10))}
                  className="w-full px-3.5 py-2 rounded-xl border border-charcoal-200 bg-white text-charcoal-800 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-600"
                >
                  <option value="14">14 Days</option>
                  <option value="30">30 Days (Recommended)</option>
                  <option value="60">60 Days Risk Free</option>
                </select>
              </div>
            </div>

            <div className="flex items-center justify-between p-3.5 rounded-xl border border-charcoal-200 bg-charcoal-50/60 mt-2">
              <div>
                <span className="block text-xs font-bold text-charcoal-900">
                  Allow Express Expedited Concessions
                </span>
                <span className="block text-[11px] text-charcoal-500 mt-0.5">
                  Let your seller twin offer free express shipping during close negotiations as a deal sweetener.
                </span>
              </div>
              <input
                type="checkbox"
                checked={allowExpressShipping}
                onChange={(e) => setAllowExpressShipping(e.target.checked)}
                className="w-4 h-4 text-purple-600 rounded border-charcoal-300 focus:ring-purple-500 cursor-pointer"
              />
            </div>
          </div>
        </Card>
      )}

      {/* Step 4: AI Agent Rules & Activation */}
      {step === 4 && (
        <Card className="p-6 space-y-6">
          <div className="border-b border-charcoal-100 pb-3">
            <h2 className="text-base font-bold text-charcoal-900 flex items-center gap-2">
              <Bot className="w-4 h-4 text-purple-600" />
              Step 4: AI Negotiation Engine Governance & Review
            </h2>
            <p className="text-xs text-charcoal-500 mt-0.5">
              Control the algorithmic strategy TradeTwin applies when countering buyer bids on your products.
            </p>
          </div>

          {/* Strategy selection */}
          <div>
            <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-2">
              Default Autonomous Counter-Offer Strategy
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {[
                {
                  id: 'CONSERVATIVE',
                  label: 'Conservative / Margin First',
                  desc: 'Protects floor price tightly; yields minimal discounts and favors bonus perks.',
                },
                {
                  id: 'BALANCED',
                  label: 'Balanced (Recommended)',
                  desc: 'Optimizes FairDeal composite utility; trades modest price concessions for volume.',
                },
                {
                  id: 'COOPERATIVE',
                  label: 'Cooperative / Rapid Liquidation',
                  desc: 'High concession pace; swiftly converges to agreement if buyer reputation is high.',
                },
              ].map((strat) => (
                <button
                  key={strat.id}
                  type="button"
                  onClick={() => setDefaultStrategy(strat.id as NegotiationStrategy)}
                  className={`p-3.5 rounded-xl border text-left transition-all ${
                    defaultStrategy === strat.id
                      ? 'border-purple-600 bg-purple-50 ring-2 ring-purple-500/20'
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

          {/* Guardrails */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                Minimum Buyer Reputation Required: {minimumReputationRequired} / 100
              </label>
              <input
                type="range"
                min="50"
                max="95"
                step="5"
                value={minimumReputationRequired}
                onChange={(e) => setMinimumReputationRequired(parseFloat(e.target.value))}
                className="w-full accent-purple-600 cursor-pointer"
              />
              <span className="text-[10px] text-charcoal-400 mt-1 block">
                Buyers with reputation below {minimumReputationRequired} cannot initiate automated agent negotiations with your store.
              </span>
            </div>

            <div className="flex items-center justify-between p-3.5 rounded-xl border border-charcoal-200 bg-charcoal-50/60">
              <div>
                <span className="block text-xs font-bold text-charcoal-900">
                  Autonomous Auto-Negotiate
                </span>
                <span className="block text-[11px] text-charcoal-500 mt-0.5">
                  Allows seller agent to respond to buyer turns in real time without manual approval per turn.
                </span>
              </div>
              <input
                type="checkbox"
                checked={autoNegotiateEnabled}
                onChange={(e) => setAutoNegotiateEnabled(e.target.checked)}
                className="w-4 h-4 text-purple-600 rounded border-charcoal-300 focus:ring-purple-500 cursor-pointer"
              />
            </div>
          </div>

          {/* Review Summary */}
          <div className="p-4 rounded-xl bg-purple-50/50 border border-purple-200 space-y-2">
            <h3 className="text-xs font-bold uppercase tracking-wider text-purple-900">
              Onboarding Summary
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
              <div>
                <span className="text-charcoal-400 block text-[10px]">Store Name:</span>
                <span className="font-semibold text-charcoal-900">{storeName}</span>
              </div>
              <div>
                <span className="text-charcoal-400 block text-[10px]">Support:</span>
                <span className="font-semibold text-charcoal-900">{supportEmail}</span>
              </div>
              <div>
                <span className="text-charcoal-400 block text-[10px]">Regions:</span>
                <span className="font-semibold text-charcoal-900">{shippingRegions.join(', ')}</span>
              </div>
              <div>
                <span className="text-charcoal-400 block text-[10px]">Strategy:</span>
                <span className="font-semibold text-purple-700">{defaultStrategy}</span>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Navigation Buttons */}
      <div className="flex items-center justify-between pt-2">
        {step > 1 ? (
          <Button
            type="button"
            variant="outline"
            size="md"
            onClick={handleBack}
            leftIcon={<ArrowLeft className="w-4 h-4" />}
            className="rounded-xl"
          >
            Previous
          </Button>
        ) : (
          <div />
        )}

        {step < 4 ? (
          <Button
            type="button"
            variant="primary"
            size="md"
            onClick={handleNext}
            rightIcon={<ArrowRight className="w-4 h-4" />}
            className="rounded-xl font-bold bg-purple-600 hover:bg-purple-700 border-purple-600 px-6"
          >
            Continue to Step {step + 1}
          </Button>
        ) : (
          <Button
            type="button"
            variant="primary"
            size="md"
            onClick={handleSubmit}
            isLoading={isSubmitting}
            leftIcon={<Sparkles className="w-4 h-4" />}
            className="rounded-xl font-bold bg-purple-600 hover:bg-purple-700 border-purple-600 px-8"
          >
            Complete Onboarding & Launch Store
          </Button>
        )}
      </div>
    </div>
  );
};
