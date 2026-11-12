import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Bot, ArrowLeft } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="text-center py-24 space-y-4 max-w-md mx-auto">
      <div className="w-16 h-16 rounded-2xl bg-charcoal-100 flex items-center justify-center mx-auto text-charcoal-400">
        <Bot className="w-8 h-8" />
      </div>
      <h1 className="text-3xl font-extrabold text-charcoal-900 tracking-tight">404 - Page Not Found</h1>
      <p className="text-xs text-charcoal-500 leading-relaxed">
        The route you are looking for does not exist or has been moved.
      </p>
      <div className="pt-2">
        <Button variant="primary" size="md" onClick={() => navigate('/')} leftIcon={<ArrowLeft className="w-4 h-4" />}>
          Back to Homepage
        </Button>
      </div>
    </div>
  );
};
