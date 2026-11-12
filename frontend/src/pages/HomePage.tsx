import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { Sparkles, Bot, ShieldCheck, Scale, ArrowRight, Zap, RefreshCw } from 'lucide-react';
import { apiClient } from '../api/client';
import { formatCurrency, extractErrorMessage } from '../lib/utils';
import { useAuth } from '../context/AuthContext';
import { NaturalLanguageSearch } from '../components/buyer/NaturalLanguageSearch';
import { IntentReviewCard } from '../components/buyer/IntentReviewCard';
import { ProductComparisonMatrix } from '../components/buyer/ProductComparisonMatrix';
import type { Product, BuyerIntent, SearchResultItem, ParseIntentResponse } from '../types';

export const HomePage: React.FC = () => {
  const { isAuthenticated, quickLoginAsBuyer } = useAuth();
  const [isSearching, setIsSearching] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [isStartingNegotiation, setIsStartingNegotiation] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Natural Language Intent & Match State
  const [parsedIntent, setParsedIntent] = useState<BuyerIntent | null>(null);
  const [candidateResults, setCandidateResults] = useState<SearchResultItem[]>([]);
  const [searchSummary, setSearchSummary] = useState<string>('');

  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([]);
  const resultsRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    apiClient.get<{ data: Product[] }>('/catalog')
      .then(res => setFeaturedProducts(res.data.data.slice(0, 3)))
      .catch(() => {});
  }, []);

  const handleNaturalLanguageSearch = async (queryText: string) => {
    setIsSearching(true);
    setErrorMsg(null);
    try {
      const res = await apiClient.post<{ data: ParseIntentResponse }>('/intents/parse', {
        query: queryText,
      });

      const data = res.data.data;
      setParsedIntent(data.intent);
      setCandidateResults(data.candidates || []);
      setSearchSummary(data.searchSummary || '');

      setTimeout(() => {
        resultsRef.current?.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Failed to interpret buyer intent. Please try again.'));
    } finally {
      setIsSearching(false);
    }
  };

  const handleConfirmIntent = async (confirmedIntent: BuyerIntent) => {
    setIsConfirming(true);
    setErrorMsg(null);
    try {
      const res = await apiClient.post<{ data: BuyerIntent }>('/intents', confirmedIntent);
      setParsedIntent(res.data.data);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Failed to confirm intent parameters.'));
    } finally {
      setIsConfirming(false);
    }
  };

  const handleRefineWithAnswer = async (answer: string) => {
    if (!parsedIntent) return;
    const combinedQuery = `${parsedIntent.rawQuery}. Clarification: ${answer}`;
    await handleNaturalLanguageSearch(combinedQuery);
  };

  const handleDeployAgent = async (item: SearchResultItem) => {
    if (!isAuthenticated) {
      await quickLoginAsBuyer();
    }

    setIsStartingNegotiation(true);
    setErrorMsg(null);
    try {
      const targetBudget = parsedIntent?.targetBudget
        ? parsedIntent.targetBudget
        : (parsedIntent?.maxBudget ? parsedIntent.maxBudget * 0.88 : item.unitPrice * 0.88);

      const maxCeiling = parsedIntent?.maxBudget
        ? parsedIntent.maxBudget
        : item.estimatedTotalPrice;

      const primaryBrand = parsedIntent?.brandPreferences?.[0];
      const rankedCandidateIds = [
        item.product.id,
        ...candidateResults.filter((c) => c.product.id !== item.product.id).map((c) => c.product.id),
      ];

      const res = await apiClient.post<{ data: { id: string } }>('/negotiations/start', {
        productId: item.product.id,
        buyerQuery: parsedIntent?.rawQuery,
        intentId: parsedIntent?.id,
        candidateProductIds: rankedCandidateIds,
        preferences: {
          targetPrice: Number(targetBudget.toFixed(2)),
          maxCeilingPrice: Number(maxCeiling.toFixed(2)),
          urgency: 'MEDIUM',
          desiredDeliveryDays: parsedIntent?.maxDeliveryDays || item.estimatedDeliveryDays,
          requireExtendedWarranty: (parsedIntent?.minWarrantyMonths || 12) >= 12,
          quantity: parsedIntent?.quantity || 1,
          minWarrantyMonths: parsedIntent?.minWarrantyMonths || 12,
          requiredBrand: primaryBrand,
          preferredBrand: primaryBrand,
          buyerPrompt: parsedIntent?.rawQuery,
        },
      });

      navigate(`/arena/${res.data.data.id}`);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Failed to initiate agent negotiation arena.'));
    } finally {
      setIsStartingNegotiation(false);
    }
  };

  const handleResetSearch = () => {
    setParsedIntent(null);
    setCandidateResults([]);
    setSearchSummary('');
    setErrorMsg(null);
  };

  return (
    <div className="space-y-16 py-4">
      {/* Hero Section */}
      <section className="text-center max-w-4xl mx-auto pt-8 pb-4 space-y-6">
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-primary-50 border border-primary-200 text-primary-700 text-xs font-semibold uppercase tracking-wider">
          <Sparkles className="w-3.5 h-3.5 text-primary-600 animate-spin" />
          Autonomous Multi-Agent Commercial Marketplace
        </div>

        <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-charcoal-900 tracking-tight leading-[1.15]">
          Where AI Agents <span className="text-primary-600">Negotiate</span> the Best Deals for You.
        </h1>

        <p className="text-base sm:text-lg text-charcoal-600 max-w-2xl mx-auto leading-relaxed">
          State your requirements in natural English. TradeTwin extracts your commercial intent, retrieves authentic merchant stock, and unleashes autonomous agents to negotiate optimal terms with private reserve protection.
        </p>

        {/* Natural Language Shopping Interface */}
        <div className="pt-2">
          <NaturalLanguageSearch
            onSearch={handleNaturalLanguageSearch}
            isLoading={isSearching}
          />
        </div>

        {errorMsg && (
          <div className="max-w-2xl mx-auto text-left">
            <Alert type="error" title="Search Error">{errorMsg}</Alert>
          </div>
        )}
      </section>

      {/* Dynamic Intent Review & Comparison Matrix Section */}
      {parsedIntent && (
        <section ref={resultsRef} className="space-y-8 scroll-mt-20 pt-2 border-t border-charcoal-200/90">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-primary-100 flex items-center justify-center text-primary-700 font-bold">
                1
              </div>
              <div>
                <h2 className="text-xl font-bold text-charcoal-900">Step 1: Review & Confirm Extracted Intent</h2>
                <p className="text-xs text-charcoal-500">Your ceiling budget is strictly confidential.</p>
              </div>
            </div>

            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleResetSearch}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
              className="text-xs"
            >
              Reset Search
            </Button>
          </div>

          <IntentReviewCard
            intent={parsedIntent}
            onConfirm={handleConfirmIntent}
            onRefineWithAnswer={handleRefineWithAnswer}
            isConfirming={isConfirming}
          />

          <div className="pt-4 space-y-4">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-700 font-bold">
                2
              </div>
              <div>
                <h2 className="text-xl font-bold text-charcoal-900">Step 2: Compare Eligible Merchant Listings</h2>
                <p className="text-xs text-charcoal-500">
                  {searchSummary || 'Select an eligible merchant listing to deploy your Buyer Agent into live multi-turn bargaining.'}
                </p>
              </div>
            </div>

            <ProductComparisonMatrix
              candidates={candidateResults}
              intent={parsedIntent}
              onSelectForNegotiation={handleDeployAgent}
              isStartingNegotiation={isStartingNegotiation}
            />
          </div>
        </section>
      )}

      {/* Featured Negotiable Products */}
      <section className="space-y-6">
        <div className="flex items-end justify-between">
          <div>
            <h2 className="text-xl font-bold text-charcoal-900 tracking-tight">Active Marketplace Listings</h2>
            <p className="text-xs text-charcoal-500 mt-1">Autonomous merchant agents are standing by to negotiate live.</p>
          </div>
          <Button variant="ghost" size="sm" onClick={() => navigate('/catalog')} rightIcon={<ArrowRight className="w-4 h-4" />}>
            View full catalog
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {featuredProducts.map((product) => (
            <Card key={product.id} hoverable onClick={() => navigate(`/catalog/${product.id}`)} className="flex flex-col justify-between group">
              <div>
                <div className="relative aspect-[16/10] rounded-lg overflow-hidden bg-charcoal-100 mb-4">
                  {product.images[0] ? (
                    <img
                      src={product.images[0]}
                      alt={product.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-charcoal-400">
                      <Bot className="w-8 h-8" />
                    </div>
                  )}
                  <div className="absolute top-2.5 right-2.5">
                    <Badge variant="primary" size="sm" className="bg-white/90 backdrop-blur-sm shadow-subtle">
                      <Zap className="w-3 h-3 text-primary-600 fill-primary-600" /> Auto-Negotiate
                    </Badge>
                  </div>
                </div>

                <div className="flex items-center gap-2 mb-2">
                  <Badge variant="default" size="sm">{product.category}</Badge>
                  <span className="text-[11px] text-charcoal-400">Max {product.maxNegotiationRounds || 5} turns</span>
                </div>

                <h3 className="font-semibold text-charcoal-900 text-base line-clamp-1 group-hover:text-primary-600 transition-colors">
                  {product.title}
                </h3>
                <p className="text-xs text-charcoal-500 mt-1 line-clamp-2 leading-relaxed">
                  {product.description}
                </p>
              </div>

              <div className="pt-4 mt-4 border-t border-charcoal-100 flex items-center justify-between">
                <div>
                  <span className="text-[10px] text-charcoal-400 block uppercase font-medium">List Price</span>
                  <span className="text-lg font-bold text-charcoal-900">{formatCurrency(product.listPrice)}</span>
                </div>
                <Button variant="primary" size="sm" className="rounded-lg">
                  Negotiate
                </Button>
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* Pillars / Architecture Features */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-6">
        <Card className="p-6">
          <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-600 flex items-center justify-center mb-4">
            <Bot className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-charcoal-900 text-base mb-2">Natural-Language Intent</h3>
          <p className="text-xs text-charcoal-500 leading-relaxed">
            State budget limits, delivery windows, warranty requirements, and brands in plain English. TradeTwin extracts validated constraints using Gemini AI.
          </p>
        </Card>

        <Card className="p-6">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-4">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-charcoal-900 text-base mb-2">Private Buyer Ceiling</h3>
          <p className="text-xs text-charcoal-500 leading-relaxed">
            Your maximum budget is private. It is encrypted on the server-side orchestrator and never disclosed to merchant agents or public streams.
          </p>
        </Card>

        <Card className="p-6">
          <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center mb-4">
            <Scale className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-charcoal-900 text-base mb-2">Deterministic Catalog Retrieval</h3>
          <p className="text-xs text-charcoal-500 leading-relaxed">
            AI interprets and ranks requirements, but never invents fake prices or phantom inventory. All matched items are backed by verified MongoDB merchant stock.
          </p>
        </Card>
      </section>
    </div>
  );
};
