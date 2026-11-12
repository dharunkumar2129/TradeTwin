import React, { useState, useEffect } from 'react';
import { mandateApi } from '../../api/mandateApi';
import type {
  PaymentApprovalPolicy,
  PaymentAuditEntry,
  SpendMandate,
  SpendMandateRequest,
} from '../../types';
import { Card } from '../ui/Card';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { Input } from '../ui/Input';
import { Alert } from '../ui/Alert';
import { extractErrorMessage, formatCurrency } from '../../lib/utils';
import { extractErrorDetails } from '../../api/client';
import {
  ShieldCheck,
  Lock,
  Bot,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Clock,
  FileText,
  KeyRound,
  RefreshCw,
} from 'lucide-react';

export const SpendMandatePanel: React.FC = () => {
  const [mandates, setMandates] = useState<SpendMandate[]>([]);
  const [auditLogs, setAuditLogs] = useState<PaymentAuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string; details?: string[] } | null>(
    null
  );

  // Form states for all 8 Spend Mandate fields
  const [name, setName] = useState('Default Smart Buyer Mandate');
  const [maxAmountPerTransaction, setMaxAmountPerTransaction] = useState('500');
  const [dailySpendingLimit, setDailySpendingLimit] = useState('1500');
  const [currency, setCurrency] = useState('USD');
  const [maxQuantity, setMaxQuantity] = useState('3');
  const [allowedCategories, setAllowedCategories] = useState('');
  const [preferredBrands, setPreferredBrands] = useState('');
  const [approvedSellerIds, setApprovedSellerIds] = useState('');
  const [expiryDays, setExpiryDays] = useState('30');
  const [policy, setPolicy] = useState<PaymentApprovalPolicy>('SUPERVISED_PAYPAL_CHECKOUT');
  const [delegatedVaultTokenId, setDelegatedVaultTokenId] = useState('');
  const [delegatedMerchantId, setDelegatedMerchantId] = useState('');
  const [delegatedMaxCurrency, setDelegatedMaxCurrency] = useState('USD');

  const activeMandate = mandates.find((m) => m.status === 'ACTIVE') || null;

  const loadData = async () => {
    setLoading(true);
    try {
      const [mList, audits] = await Promise.all([
        mandateApi.getMyMandates(),
        mandateApi.getMyAuditLogs(),
      ]);
      setMandates(mList);
      setAuditLogs(audits);

      const current = mList.find((m) => m.status === 'ACTIVE') || mList[0];
      if (current) {
        setName(current.name);
        setMaxAmountPerTransaction(String(current.maxAmountPerTransaction));
        setDailySpendingLimit(String(current.dailySpendingLimit));
        setCurrency(current.currency || 'USD');
        setMaxQuantity(String(current.maxQuantity || 1));
        setAllowedCategories((current.allowedCategories || []).join(', '));
        setPreferredBrands((current.preferredBrands || []).join(', '));
        setApprovedSellerIds((current.approvedSellerIds || []).join(', '));
        setPolicy(current.paymentApprovalPolicy);
        setDelegatedMerchantId(current.delegatedMerchantId || '');
        setDelegatedMaxCurrency(current.delegatedMaxCurrency || current.currency || 'USD');
      }
    } catch {
      // Handled gracefully when logged out
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const parseCsv = (val: string): string[] =>
    val
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);

  const handleSaveMandate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setFeedback(null);

    try {
      const days = Math.max(1, parseInt(expiryDays, 10) || 30);
      const expiresAt = new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString();

      const payload: SpendMandateRequest = {
        name: name.trim() || 'Buyer Spend Mandate',
        maxAmountPerTransaction: Number(maxAmountPerTransaction) || 100,
        dailySpendingLimit: Number(dailySpendingLimit) || 500,
        currency: currency.trim().toUpperCase() || 'USD',
        maxQuantity: Math.max(1, parseInt(maxQuantity, 10) || 1),
        allowedCategories: parseCsv(allowedCategories),
        preferredBrands: parseCsv(preferredBrands),
        approvedSellerIds: parseCsv(approvedSellerIds),
        expiresAt,
        paymentApprovalPolicy: policy,
        delegatedVaultTokenId:
          policy === 'DELEGATED_AUTONOMOUS_PAYMENT' && delegatedVaultTokenId.trim()
            ? delegatedVaultTokenId.trim()
            : undefined,
        delegatedMerchantId:
          policy === 'DELEGATED_AUTONOMOUS_PAYMENT' && delegatedMerchantId.trim()
            ? delegatedMerchantId.trim()
            : undefined,
        delegatedMaxCurrency:
          policy === 'DELEGATED_AUTONOMOUS_PAYMENT' ? delegatedMaxCurrency.trim().toUpperCase() || 'USD' : undefined,
      };

      await mandateApi.createMandate(payload);
      setDelegatedVaultTokenId('');
      setFeedback({
        type: 'success',
        message: 'Spend Mandate saved and activated cleanly. Every negotiated agreement is deterministically verified against these bounds before payment execution.',
      });
      await loadData();
    } catch (err: unknown) {
      setFeedback({
        type: 'error',
        message: extractErrorMessage(err, 'Failed to save Spend Mandate'),
        details: extractErrorDetails(err),
      });
    } finally {
      setSaving(false);
    }
  };

  const handleRevoke = async (mandateId: string) => {
    setFeedback(null);
    try {
      await mandateApi.revokeMandate(mandateId);
      setFeedback({
        type: 'success',
        message: 'Spend Mandate revoked. Default Supervised PayPal Checkout safeguards remain active.',
      });
      await loadData();
    } catch (err: unknown) {
      setFeedback({
        type: 'error',
        message: extractErrorMessage(err, 'Failed to revoke mandate'),
      });
    }
  };

  const getAuditStatusBadge = (status: string) => {
    if (status === 'COMPLETED') return <Badge variant="success">COMPLETED</Badge>;
    if (status === 'PENDING_SUPERVISED_APPROVAL') return <Badge variant="primary">AWAITING PAYPAL APPROVAL</Badge>;
    if (status === 'DELEGATED_ACCESS_UNAVAILABLE') return <Badge variant="warning">DELEGATED ACCESS UNAVAILABLE</Badge>;
    if (status === 'BLOCKED_MANDATE_VIOLATION') return <Badge variant="danger">MANDATE BLOCKED</Badge>;
    if (status === 'CANCELLED') return <Badge variant="warning">CANCELLED</Badge>;
    return <Badge variant="danger">{status}</Badge>;
  };

  if (loading) {
    return (
      <div className="py-12 text-center text-xs text-charcoal-500">
        Loading Spend Mandates & Payment Audit History...
      </div>
    );
  }

  const dailyPct = activeMandate
    ? Math.min(100, Math.round((activeMandate.spentToday / Math.max(1, activeMandate.dailySpendingLimit)) * 100))
    : 0;

  return (
    <div className="space-y-8">
      {feedback && (
        <Alert
          type={feedback.type === 'success' ? 'success' : 'error'}
          title={feedback.type === 'success' ? 'Spend Mandate Updated' : 'Mandate Validation Error'}
        >
          <p>{feedback.message}</p>
          {feedback.details && feedback.details.length > 0 && (
            <ul className="list-disc pl-4 mt-1 text-[11px] font-mono">
              {feedback.details.map((d, idx) => (
                <li key={idx}>{d}</li>
              ))}
            </ul>
          )}
        </Alert>
      )}

      {/* Top Grid: Active Mandate Summary & Configuration Form */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Active Mandate Card */}
        <div className="lg:col-span-5 space-y-4">
          <Card className="p-5 space-y-4 shadow-card border-primary-100">
            <div className="flex items-center justify-between pb-3 border-b border-charcoal-100">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-primary-600" />
                <div>
                  <h3 className="font-bold text-sm text-charcoal-900">Active Spend Mandate</h3>
                  <p className="text-[11px] text-charcoal-400">Enforced strictly outside LLM agent reasoning</p>
                </div>
              </div>
              {activeMandate ? (
                <Badge variant="success">ACTIVE</Badge>
              ) : (
                <Badge variant="default">UNCONFIGURED</Badge>
              )}
            </div>

            {activeMandate ? (
              <div className="space-y-4 text-xs">
                <div>
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-charcoal-900">{activeMandate.name}</span>
                    <Badge
                      variant={
                        activeMandate.paymentApprovalPolicy === 'SUPERVISED_PAYPAL_CHECKOUT' ? 'primary' : 'purple'
                      }
                      size="sm"
                    >
                      {activeMandate.paymentApprovalPolicy === 'SUPERVISED_PAYPAL_CHECKOUT'
                        ? 'Supervised PayPal'
                        : 'Delegated Vault Adapter'}
                    </Badge>
                  </div>
                  <span className="text-[11px] text-charcoal-400 flex items-center gap-1 mt-1">
                    <Clock className="w-3 h-3" /> Expires {new Date(activeMandate.expiresAt).toLocaleDateString()}
                  </span>
                </div>

                {/* Daily Spend Bar */}
                <div className="p-3 rounded-xl bg-charcoal-50 border border-charcoal-100 space-y-1.5">
                  <div className="flex justify-between font-medium text-charcoal-700">
                    <span>Daily Spend Today</span>
                    <span className="font-mono font-bold text-charcoal-900">
                      {formatCurrency(activeMandate.spentToday)} / {formatCurrency(activeMandate.dailySpendingLimit)}
                    </span>
                  </div>
                  <div className="w-full h-2 rounded-full bg-charcoal-200 overflow-hidden">
                    <div
                      className={`h-full transition-all ${dailyPct >= 90 ? 'bg-rose-500' : 'bg-emerald-500'}`}
                      style={{ width: `${dailyPct}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-[11px] text-charcoal-500">
                    <span>Remaining today: {formatCurrency(activeMandate.remainingDailyBudget)}</span>
                    <span>Max / Tx: {formatCurrency(activeMandate.maxAmountPerTransaction)}</span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-[11px]">
                  <div className="p-2.5 rounded-lg border border-charcoal-100">
                    <span className="text-charcoal-400 block">Max Qty / Order</span>
                    <span className="font-bold text-charcoal-900">{activeMandate.maxQuantity} unit(s)</span>
                  </div>
                  <div className="p-2.5 rounded-lg border border-charcoal-100">
                    <span className="text-charcoal-400 block">Currency Lock</span>
                    <span className="font-bold text-charcoal-900">{activeMandate.currency}</span>
                  </div>
                  <div className="p-2.5 rounded-lg border border-charcoal-100 col-span-2">
                    <span className="text-charcoal-400 block">Allowed Categories</span>
                    <span className="font-semibold text-charcoal-800">
                      {activeMandate.allowedCategories.length > 0
                        ? activeMandate.allowedCategories.join(', ')
                        : 'Any Category'}
                    </span>
                  </div>
                  <div className="p-2.5 rounded-lg border border-charcoal-100">
                    <span className="text-charcoal-400 block">Preferred Brands</span>
                    <span className="font-semibold text-charcoal-800">
                      {activeMandate.preferredBrands.length > 0 ? activeMandate.preferredBrands.join(', ') : 'Any'}
                    </span>
                  </div>
                  <div className="p-2.5 rounded-lg border border-charcoal-100">
                    <span className="text-charcoal-400 block">Approved Sellers</span>
                    <span className="font-semibold text-charcoal-800">
                      {activeMandate.approvedSellerIds.length > 0
                        ? activeMandate.approvedSellerIds.join(', ')
                        : 'All Verified'}
                    </span>
                  </div>
                </div>

                {activeMandate.paymentApprovalPolicy === 'DELEGATED_AUTONOMOUS_PAYMENT' && (
                  <div className="p-3 rounded-xl bg-purple-50 border border-purple-200 text-[11px] text-purple-900 space-y-1">
                    <div className="font-bold flex items-center gap-1.5">
                      <KeyRound className="w-3.5 h-3.5" /> Delegated Vault Credential Status
                    </div>
                    <p>
                      Token:{' '}
                      <span className="font-mono font-semibold">
                        {activeMandate.hasDelegatedVaultToken
                          ? activeMandate.delegatedVaultTokenMasked
                          : 'None linked'}
                      </span>
                    </p>
                    <p className="text-[10px] text-purple-700">
                      Requires official PayPal Vault v3 / Reference Transactions merchant enablement (`PAYPAL_DELEGATED_VAULT_ENABLED=true`). Never substitutes OAuth tokens or simulates payment.
                    </p>
                  </div>
                )}

                <div className="pt-2 flex justify-end">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => handleRevoke(activeMandate.id)}
                  >
                    Revoke Mandate
                  </Button>
                </div>
              </div>
            ) : (
              <div className="py-6 text-center text-xs text-charcoal-500 space-y-2">
                <Lock className="w-7 h-7 text-charcoal-300 mx-auto" />
                <p>
                  No explicit Spend Mandate configured yet. Deals default to{' '}
                  <strong>Supervised PayPal Checkout</strong> with human confirmation on every order.
                </p>
              </div>
            )}
          </Card>
        </div>

        {/* Right: Mandate Configuration Form */}
        <div className="lg:col-span-7">
          <Card className="p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-charcoal-100">
              <div>
                <h3 className="font-bold text-sm text-charcoal-900">Configure Spend Mandate & Approval Policy</h3>
                <p className="text-[11px] text-charcoal-500">
                  Sets hard deterministic bounds validated on the backend before any PayPal order or capture executes.
                </p>
              </div>
            </div>

            <form onSubmit={handleSaveMandate} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <Input
                  label="Mandate Name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
                <div className="grid grid-cols-2 gap-2">
                  <Input
                    label="Currency"
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value)}
                    required
                  />
                  <Input
                    label="Expires in (Days)"
                    type="number"
                    min={1}
                    max={365}
                    value={expiryDays}
                    onChange={(e) => setExpiryDays(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <Input
                  label="Max Per Transaction ($)"
                  type="number"
                  step="0.01"
                  min="1"
                  value={maxAmountPerTransaction}
                  onChange={(e) => setMaxAmountPerTransaction(e.target.value)}
                  required
                />
                <Input
                  label="Daily Spending Limit ($)"
                  type="number"
                  step="0.01"
                  min="1"
                  value={dailySpendingLimit}
                  onChange={(e) => setDailySpendingLimit(e.target.value)}
                  required
                />
                <Input
                  label="Max Quantity / Order"
                  type="number"
                  min={1}
                  value={maxQuantity}
                  onChange={(e) => setMaxQuantity(e.target.value)}
                  required
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <Input
                  label="Allowed Categories (CSV)"
                  placeholder="Audio, Workstations (blank = all)"
                  value={allowedCategories}
                  onChange={(e) => setAllowedCategories(e.target.value)}
                />
                <Input
                  label="Preferred Brands (CSV)"
                  placeholder="Sony, Lenovo (blank = any)"
                  value={preferredBrands}
                  onChange={(e) => setPreferredBrands(e.target.value)}
                />
                <Input
                  label="Approved Seller IDs (CSV)"
                  placeholder="seller-id-1 (blank = any)"
                  value={approvedSellerIds}
                  onChange={(e) => setApprovedSellerIds(e.target.value)}
                />
              </div>

              {/* Payment Approval Policy Distinction */}
              <div className="space-y-2 pt-1">
                <label className="text-xs font-bold text-charcoal-800 block">
                  Payment Approval Policy
                </label>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setPolicy('SUPERVISED_PAYPAL_CHECKOUT')}
                    className={`text-left p-3.5 rounded-xl border-2 transition-all ${
                      policy === 'SUPERVISED_PAYPAL_CHECKOUT'
                        ? 'border-primary-600 bg-primary-50/60'
                        : 'border-charcoal-200 hover:border-charcoal-300 bg-white'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-charcoal-900 flex items-center gap-1.5">
                        <ShieldCheck className="w-4 h-4 text-primary-600" />
                        Supervised PayPal Checkout
                      </span>
                      <Badge variant="success" size="sm">Default</Badge>
                    </div>
                    <p className="text-[11px] text-charcoal-600 leading-relaxed">
                      Automatically prepares the PayPal Sandbox order once AI agents reach agreement, then opens the live PayPal approval experience for your explicit authorization before backend capture.
                    </p>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPolicy('DELEGATED_AUTONOMOUS_PAYMENT')}
                    className={`text-left p-3.5 rounded-xl border-2 transition-all ${
                      policy === 'DELEGATED_AUTONOMOUS_PAYMENT'
                        ? 'border-purple-600 bg-purple-50/60'
                        : 'border-charcoal-200 hover:border-charcoal-300 bg-white'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-charcoal-900 flex items-center gap-1.5">
                        <Bot className="w-4 h-4 text-purple-600" />
                        Delegated Autonomous Adapter
                      </span>
                      <Badge variant="purple" size="sm">Vault v3</Badge>
                    </div>
                    <p className="text-[11px] text-charcoal-600 leading-relaxed">
                      Executes only via official PayPal Vault v3 / Reference Transactions when authorized credentials are supplied. Never fakes approval or substitutes OAuth tokens.
                    </p>
                  </button>
                </div>
              </div>

              {policy === 'DELEGATED_AUTONOMOUS_PAYMENT' && (
                <div className="p-3.5 rounded-xl bg-purple-50/70 border border-purple-200 space-y-3">
                  <div className="flex items-start gap-2 text-[11px] text-purple-900">
                    <AlertTriangle className="w-4 h-4 text-purple-600 shrink-0 mt-0.5" />
                    <span>
                      Supply only an official PayPal Vault `payment_token` (`vault_id`). Ordinary PayPal REST OAuth access tokens (`A21AA...`) are rejected by backend security rules.
                    </span>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2.5">
                    <Input
                      label="Vault Payment Token (vault_id)"
                      placeholder="Optional Vault v3 token"
                      value={delegatedVaultTokenId}
                      onChange={(e) => setDelegatedVaultTokenId(e.target.value)}
                    />
                    <Input
                      label="Restricted Merchant ID"
                      placeholder="Optional sellerId lock"
                      value={delegatedMerchantId}
                      onChange={(e) => setDelegatedMerchantId(e.target.value)}
                    />
                    <Input
                      label="Restricted Currency"
                      placeholder="USD"
                      value={delegatedMaxCurrency}
                      onChange={(e) => setDelegatedMaxCurrency(e.target.value)}
                    />
                  </div>
                </div>
              )}

              <div className="flex justify-end pt-2">
                <Button type="submit" variant="primary" size="md" isLoading={saving}>
                  Save & Activate Spend Mandate
                </Button>
              </div>
            </form>
          </Card>
        </div>
      </div>

      {/* Payment & Mandate Audit History */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-charcoal-900 flex items-center gap-2">
              <FileText className="w-4 h-4 text-primary-600" />
              Spend Mandate & Payment Execution Audit History
            </h2>
            <p className="text-xs text-charcoal-500">
              Immutable trail of original mandate constraints, budget compliance checks, PayPal Order/Capture IDs, and settlement status.
            </p>
          </div>
          <Button variant="outline" size="sm" onClick={loadData} leftIcon={<RefreshCw className="w-3.5 h-3.5" />}>
            Refresh Audit
          </Button>
        </div>

        {auditLogs.length === 0 ? (
          <Card className="p-8 text-center text-xs text-charcoal-400">
            No payment execution audits recorded yet. Sealed agreements and checkout attempts appear here automatically.
          </Card>
        ) : (
          <div className="space-y-3">
            {auditLogs.map((entry) => (
              <Card key={entry.id} className="p-4 space-y-3 shadow-subtle">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-2 pb-2.5 border-b border-charcoal-100">
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-bold text-xs text-charcoal-900">
                        {entry.productTitle || `Negotiation ${entry.negotiationId}`}
                      </span>
                      {getAuditStatusBadge(entry.finalStatus)}
                      <Badge variant={entry.budgetCompliant ? 'success' : 'danger'} size="sm">
                        {entry.budgetCompliant ? 'Budget Compliant' : 'Mandate Violated'}
                      </Badge>
                    </div>
                    <div className="text-[11px] text-charcoal-500">
                      Seller: <strong>{entry.sellerStoreName || entry.sellerId}</strong> • Auth Method:{' '}
                      <span className="font-mono text-charcoal-700">{entry.authorizationMethod}</span>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-black text-charcoal-900">
                      {formatCurrency(entry.negotiatedAmount)} <span className="text-[10px] font-mono">{entry.currency}</span>
                    </div>
                    <div className="text-[10px] text-charcoal-400">
                      {new Date(entry.updatedAt || entry.createdAt).toLocaleString()}
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-[11px]">
                  <div className="p-2.5 rounded-lg bg-charcoal-50 border border-charcoal-100">
                    <span className="text-charcoal-400 block font-medium">Original Buyer Mandate</span>
                    <span className="font-semibold text-charcoal-800 block">
                      {String(entry.mandateSnapshot?.mandateName || 'Standard Supervised Checkout')}
                    </span>
                    {entry.mandateSnapshot?.maxAmountPerTransaction !== undefined &&
                      entry.mandateSnapshot?.maxAmountPerTransaction !== null && (
                        <span className="text-[10px] text-charcoal-500">
                          Cap: ${String(entry.mandateSnapshot.maxAmountPerTransaction)} / tx • Daily: $
                          {String(entry.mandateSnapshot.dailySpendingLimit ?? '-')}
                        </span>
                      )}
                  </div>

                  <div className="p-2.5 rounded-lg bg-charcoal-50 border border-charcoal-100">
                    <span className="text-charcoal-400 block font-medium">Budget Compliance Check</span>
                    <div className="flex items-start gap-1 mt-0.5 text-charcoal-700">
                      {entry.budgetCompliant ? (
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                      ) : (
                        <XCircle className="w-3.5 h-3.5 text-rose-600 shrink-0 mt-0.5" />
                      )}
                      <span>{entry.complianceSummary}</span>
                    </div>
                  </div>

                  <div className="p-2.5 rounded-lg bg-charcoal-50 border border-charcoal-100 font-mono space-y-0.5">
                    <div className="flex justify-between">
                      <span className="text-charcoal-400 font-sans">PayPal Order:</span>
                      <span className="text-charcoal-800">{entry.paypalOrderId || '—'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-charcoal-400 font-sans">Capture ID:</span>
                      <span className="text-emerald-700 font-semibold">{entry.paypalCaptureId || '—'}</span>
                    </div>
                  </div>
                </div>

                {entry.complianceViolations && entry.complianceViolations.length > 0 && (
                  <div className="p-2.5 rounded-lg bg-rose-50 border border-rose-200 text-[11px] text-rose-800 space-y-0.5">
                    {entry.complianceViolations.map((v, i) => (
                      <div key={i}>• {v}</div>
                    ))}
                  </div>
                )}

                {entry.diagnosticDetails && entry.diagnosticDetails.length > 0 && (
                  <div className="p-2 rounded-lg bg-charcoal-50 border border-charcoal-200 text-[10px] font-mono text-charcoal-600">
                    {entry.diagnosticDetails.join(' | ')}
                  </div>
                )}
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
