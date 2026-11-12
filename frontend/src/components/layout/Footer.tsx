import React from 'react';
import { Bot, ShieldCheck, Cpu, DollarSign } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full border-t border-charcoal-200/80 bg-white py-12 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <div className="md:col-span-2">
            <div className="flex items-center gap-2 mb-3">
              <div className="w-7 h-7 rounded-lg bg-primary-600 flex items-center justify-center text-white">
                <Bot className="w-4 h-4" />
              </div>
              <span className="font-bold text-base text-charcoal-900 tracking-tight">TradeTwin</span>
            </div>
            <p className="text-xs text-charcoal-500 max-w-sm leading-relaxed">
              Autonomous AI-to-AI agentic commerce marketplace. Governed by verifiable Multi-Agent Negotiation Protocol (MANP), strict seller reserve guardrails, and fair utility surplus arbitration.
            </p>
          </div>

          <div>
            <h4 className="text-xs font-semibold text-charcoal-900 uppercase tracking-wider mb-3">Architecture</h4>
            <ul className="space-y-2 text-xs text-charcoal-500">
              <li className="flex items-center gap-1.5"><Cpu className="w-3.5 h-3.5 text-primary-500" /> Google Gemini Runtime</li>
              <li className="flex items-center gap-1.5"><ShieldCheck className="w-3.5 h-3.5 text-emerald-500" /> Java 21 Modular Monolith</li>
              <li className="flex items-center gap-1.5"><DollarSign className="w-3.5 h-3.5 text-blue-500" /> PayPal Orders v2 Sandbox</li>
            </ul>
          </div>

          <div>
            <h4 className="text-xs font-semibold text-charcoal-900 uppercase tracking-wider mb-3">Platform</h4>
            <ul className="space-y-2 text-xs text-charcoal-500">
              <li><a href="/catalog" className="hover:text-charcoal-900 transition-colors">Product Catalog</a></li>
              <li><a href="/buyer/dashboard" className="hover:text-charcoal-900 transition-colors">Buyer Negotiation Studio</a></li>
              <li><a href="/seller/dashboard" className="hover:text-charcoal-900 transition-colors">Seller Governance Matrix</a></li>
            </ul>
          </div>
        </div>

        <div className="pt-8 border-t border-charcoal-100 flex flex-col sm:flex-row items-center justify-between text-xs text-charcoal-400 gap-4">
          <p>© {new Date().getFullYear()} TradeTwin Agentic Commerce. All rights reserved.</p>
          <div className="flex items-center gap-4">
            <span className="inline-flex items-center gap-1 text-emerald-600 font-medium">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              Agent Protocol Active
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
};
