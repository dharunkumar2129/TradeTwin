import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { Product, BuyerIntent, SearchResultItem, ParseIntentResponse } from '../types';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { formatCurrency, extractErrorMessage } from '../lib/utils';
import { useAuth } from '../context/AuthContext';
import { NaturalLanguageSearch } from '../components/buyer/NaturalLanguageSearch';
import { IntentReviewCard } from '../components/buyer/IntentReviewCard';
import { ProductComparisonMatrix } from '../components/buyer/ProductComparisonMatrix';
import { Search, Bot, Zap, ChevronUp, ChevronDown } from 'lucide-react';

export const CatalogPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryParam = searchParams.get('q') || '';
  const [searchTerm, setSearchTerm] = useState(queryParam);
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');

  // AI Natural Language Shopping State
  const [showAiAssistant, setShowAiAssistant] = useState<boolean>(true);
  const [isAiSearching, setIsAiSearching] = useState(false);
  const [parsedIntent, setParsedIntent] = useState<BuyerIntent | null>(null);
  const [candidateResults, setCandidateResults] = useState<SearchResultItem[]>([]);
  const [searchSummary, setSearchSummary] = useState<string>('');
  const [isConfirming, setIsConfirming] = useState(false);
  const [isStartingNegotiation, setIsStartingNegotiation] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const { isAuthenticated, quickLoginAsBuyer } = useAuth();
  const navigate = useNavigate();
  const aiSectionRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetchProducts(queryParam);
    if (queryParam && queryParam.length > 25) {
      // Long query parameter: trigger natural language parser automatically
      handleAiSearch(queryParam);
    }
  }, [queryParam]);

  const fetchProducts = async (q: string) => {
    setIsLoading(true);
    try {
      const url = q ? `/catalog?search=${encodeURIComponent(q)}` : '/catalog';
      const res = await apiClient.get<{ data: Product[] }>(url);
      setProducts(res.data.data);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchParams(searchTerm ? { q: searchTerm } : {});
  };

  const handleAiSearch = async (text: string) => {
    setIsAiSearching(true);
    setErrorMsg(null);
    try {
      const res = await apiClient.post<{ data: ParseIntentResponse }>('/intents/parse', { query: text });
      setParsedIntent(res.data.data.intent);
      setCandidateResults(res.data.data.candidates || []);
      setSearchSummary(res.data.data.searchSummary || '');
      setShowAiAssistant(true);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Failed to parse natural language intent'));
    } finally {
      setIsAiSearching(false);
    }
  };

  const handleConfirmIntent = async (confirmedIntent: BuyerIntent) => {
    setIsConfirming(true);
    try {
      const res = await apiClient.post<{ data: BuyerIntent }>('/intents', confirmedIntent);
      setParsedIntent(res.data.data);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Failed to confirm intent parameters'));
    } finally {
      setIsConfirming(false);
    }
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

      const res = await apiClient.post<{ data: { id: string } }>('/negotiations/start', {
        productId: item.product.id,
        preferences: {
          targetPrice: Number(targetBudget.toFixed(2)),
          maxCeilingPrice: Number(maxCeiling.toFixed(2)),
          urgency: 'MEDIUM',
          desiredDeliveryDays: parsedIntent?.maxDeliveryDays || item.estimatedDeliveryDays,
          requireExtendedWarranty: (parsedIntent?.minWarrantyMonths || 12) >= 12,
        },
      });

      navigate(`/arena/${res.data.data.id}`);
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Failed to start negotiation'));
    } finally {
      setIsStartingNegotiation(false);
    }
  };

  const categories = ['ALL', 'Electronics', 'Audio & Acoustics', 'Developer Tools', 'Smart Devices', 'Wearables', 'Monitors & Displays'];

  const filteredProducts = products.filter(p => {
    if (selectedCategory === 'ALL') return true;
    const cat = p.category?.toLowerCase() || '';
    const sel = selectedCategory.toLowerCase();
    return cat === sel || cat.includes(sel) || sel.includes(cat);
  });

  return (
    <div className="space-y-8">
      {/* Prominent Natural Language Shopping Assistant Banner */}
      <div ref={aiSectionRef} className="rounded-2xl border border-primary-200 bg-gradient-to-r from-primary-50/50 via-white to-primary-50/30 p-6 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-primary-600 flex items-center justify-center text-white shadow-subtle">
              <Bot className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-extrabold text-charcoal-900 tracking-tight">AI Natural-Language Shopping Assistant</h2>
                <Badge variant="primary" size="sm">No Filters Required</Badge>
              </div>
              <p className="text-xs text-charcoal-500">
                State your budget, brand, warranty and delivery preferences in plain English.
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => setShowAiAssistant(!showAiAssistant)}
            rightIcon={showAiAssistant ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
            className="text-xs text-charcoal-600 self-start sm:self-center"
          >
            {showAiAssistant ? 'Minimize Assistant' : 'Open Assistant'}
          </Button>
        </div>

        {showAiAssistant && (
          <div className="space-y-6 pt-2">
            <NaturalLanguageSearch
              onSearch={handleAiSearch}
              isLoading={isAiSearching}
              initialQuery={queryParam}
            />

            {errorMsg && <Alert type="error" title="Error">{errorMsg}</Alert>}

            {parsedIntent && (
              <div className="space-y-6 pt-2 border-t border-charcoal-200/80">
                {searchSummary && (
                  <p className="text-xs text-charcoal-600 font-medium bg-primary-50/50 p-2.5 rounded-lg border border-primary-200/60">
                    {searchSummary}
                  </p>
                )}
                <IntentReviewCard
                  intent={parsedIntent}
                  onConfirm={handleConfirmIntent}
                  onRefineWithAnswer={(ans) => handleAiSearch(`${parsedIntent.rawQuery}. Clarification: ${ans}`)}
                  isConfirming={isConfirming}
                />

                <ProductComparisonMatrix
                  candidates={candidateResults}
                  intent={parsedIntent}
                  onSelectForNegotiation={handleDeployAgent}
                  isStartingNegotiation={isStartingNegotiation}
                />
              </div>
            )}
          </div>
        )}
      </div>

      {/* Manual Marketplace Catalog Browser */}
      <div className="pt-4 space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-charcoal-200/80">
          <div>
            <h2 className="text-xl font-extrabold text-charcoal-900 tracking-tight">Explore Complete Marketplace Catalog</h2>
            <p className="text-xs text-charcoal-500 mt-0.5">Browse all verified merchant listings standing by with automated AI policies.</p>
          </div>

          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 max-w-md w-full">
            <Input
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by keywords, tags..."
              leftIcon={<Search className="w-4 h-4" />}
              className="w-full text-xs"
            />
            <Button type="submit" variant="primary" size="md" className="text-xs">
              Filter
            </Button>
          </form>
        </div>

        {/* Category Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors shrink-0 ${
                selectedCategory === cat
                  ? 'bg-charcoal-900 text-white shadow-subtle'
                  : 'bg-white border border-charcoal-200 text-charcoal-600 hover:bg-charcoal-100'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* Products Grid */}
        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[1, 2, 3].map((n) => (
              <div key={n} className="rounded-xl border border-charcoal-200/80 bg-white p-5 animate-pulse space-y-4">
                <div className="aspect-[16/10] bg-charcoal-100 rounded-lg" />
                <div className="h-4 bg-charcoal-100 rounded w-3/4" />
                <div className="h-3 bg-charcoal-100 rounded w-1/2" />
              </div>
            ))}
          </div>
        ) : filteredProducts.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-2xl border border-charcoal-200/80 p-8">
            <Bot className="w-12 h-12 text-charcoal-400 mx-auto mb-3" />
            <h3 className="font-semibold text-charcoal-900 text-base mb-1">No products found</h3>
            <p className="text-xs text-charcoal-500 mb-4">Try searching with different keywords or reset filters.</p>
            <Button variant="outline" size="sm" onClick={() => { setSearchTerm(''); setSearchParams({}); }}>
              Clear Search
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {filteredProducts.map((product) => (
              <Card
                key={product.id}
                hoverable
                onClick={() => navigate(`/catalog/${product.id}`)}
                className="flex flex-col justify-between group"
              >
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
                      <Badge variant="primary" size="sm" className="bg-white/95 backdrop-blur-sm shadow-subtle">
                        <Zap className="w-3 h-3 text-primary-600 fill-primary-600" /> Auto-Negotiate
                      </Badge>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 mb-2">
                    <Badge variant="default" size="sm">{product.category}</Badge>
                    <span className="text-[11px] text-charcoal-400">In stock ({product.availableInventory})</span>
                  </div>

                  <h3 className="font-semibold text-charcoal-900 text-base line-clamp-1 group-hover:text-primary-600 transition-colors">
                    {product.title}
                  </h3>
                  <p className="text-xs text-charcoal-500 mt-1 line-clamp-2 leading-relaxed">
                    {product.description}
                  </p>

                  {product.tags && product.tags.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-3">
                      {product.tags.slice(0, 3).map((tag, idx) => (
                        <span key={idx} className="text-[10px] text-charcoal-400 bg-charcoal-100/60 px-2 py-0.5 rounded">
                          #{tag}
                        </span>
                      ))}
                    </div>
                  )}
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
        )}
      </div>
    </div>
  );
};
