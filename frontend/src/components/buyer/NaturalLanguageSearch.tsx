import React, { useState } from 'react';
import { Button } from '../ui/Button';
import { Bot, Sparkles, ArrowRight, CornerDownLeft, HelpCircle } from 'lucide-react';

interface NaturalLanguageSearchProps {
  onSearch: (query: string) => void;
  isLoading?: boolean;
  initialQuery?: string;
}

export const NaturalLanguageSearch: React.FC<NaturalLanguageSearchProps> = ({
  onSearch,
  isLoading = false,
  initialQuery = '',
}) => {
  const [query, setQuery] = useState(initialQuery);

  const samplePrompts = [
    "Find me Sony wireless noise-cancelling headphones. My maximum total budget is $100. I prefer at least one year warranty and delivery within four days.",
    "Looking for a 14-inch developer workstation with 32GB RAM under $1200 with 2-day delivery.",
    "Titanium sapphire smartwatch with GPS and long battery under $400 in Brand New condition.",
    "Ergonomic split mechanical keyboard with wireless connectivity under $200."
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    onSearch(query.trim());
  };

  const handleSelectSample = (sample: string) => {
    setQuery(sample);
    onSearch(sample);
  };

  return (
    <div className="w-full max-w-4xl mx-auto space-y-4">
      <form onSubmit={handleSubmit} className="relative group">
        <div className="relative flex items-center bg-white rounded-2xl border-2 border-charcoal-200/90 shadow-card hover:border-primary-400 focus-within:border-primary-500 focus-within:ring-4 focus-within:ring-primary-500/10 transition-all p-2.5">
          <div className="pl-3 pr-2 text-primary-600 flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-primary-50 flex items-center justify-center">
              <Bot className="w-5 h-5 text-primary-600" />
            </div>
          </div>

          <textarea
            rows={2}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSubmit(e);
              }
            }}
            placeholder="Describe what you want to buy in natural language... e.g. 'Find me Sony wireless noise-cancelling headphones under $100 with 1-year warranty'"
            className="w-full px-2 py-1 text-sm sm:text-base text-charcoal-900 placeholder:text-charcoal-400 bg-transparent focus:outline-none resize-none leading-relaxed"
          />

          <div className="flex items-center gap-2 shrink-0 pr-1">
            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={isLoading}
              rightIcon={<ArrowRight className="w-4 h-4" />}
              className="rounded-xl shadow-subtle px-5 py-2.5 font-semibold"
            >
              Parse Intent
            </Button>
          </div>
        </div>

        <div className="flex items-center justify-between px-2 pt-1 text-[11px] text-charcoal-400">
          <span className="flex items-center gap-1">
            <Sparkles className="w-3 h-3 text-primary-500" /> Powered by Gemini & Deterministic Catalog Retrieval
          </span>
          <span className="hidden sm:inline-flex items-center gap-1">
            Press <CornerDownLeft className="w-3 h-3" /> Enter to search
          </span>
        </div>
      </form>

      {/* Suggested Prompts Strip */}
      <div className="space-y-1.5 pt-1">
        <div className="flex items-center gap-1.5 text-xs font-semibold text-charcoal-500">
          <HelpCircle className="w-3.5 h-3.5 text-charcoal-400" />
          <span>Try real-world merchant buying prompts:</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {samplePrompts.map((sample, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => handleSelectSample(sample)}
              className="text-xs px-3 py-1.5 rounded-lg bg-white border border-charcoal-200/90 hover:border-primary-400 hover:bg-primary-50/50 text-charcoal-700 transition-all text-left shadow-xs line-clamp-1 max-w-full"
            >
              "{sample.slice(0, 72)}..."
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
