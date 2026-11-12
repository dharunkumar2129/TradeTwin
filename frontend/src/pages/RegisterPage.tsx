import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { extractErrorMessage } from '../lib/utils';
import { Bot } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('ROLE_BUYER');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setIsSubmitting(true);
    try {
      await register(email, password, fullName, role);
      navigate(role === 'ROLE_SELLER' ? '/seller/dashboard' : '/catalog', { replace: true });
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Registration failed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto py-8 space-y-6">
      <div className="text-center space-y-2">
        <div className="w-10 h-10 rounded-xl bg-primary-600 text-white flex items-center justify-center mx-auto shadow-subtle">
          <Bot className="w-6 h-6" />
        </div>
        <h1 className="text-2xl font-extrabold text-charcoal-900 tracking-tight">Create an Account</h1>
        <p className="text-xs text-charcoal-500">Join TradeTwin as a Buyer or Verified Merchant.</p>
      </div>

      {errorMsg && <Alert type="error" title="Registration Error">{errorMsg}</Alert>}

      <Card className="p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Full Name"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            placeholder="Alice Walker"
            required
          />
          <Input
            label="Email Address"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            required
          />
          <Input
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="At least 6 characters"
            required
          />

          <div>
            <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
              Account Role
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setRole('ROLE_BUYER')}
                className={`p-3 rounded-xl border text-xs font-semibold transition-all ${
                  role === 'ROLE_BUYER'
                    ? 'border-primary-600 bg-primary-50/70 text-primary-900 ring-2 ring-primary-500/20'
                    : 'border-charcoal-200 bg-white text-charcoal-600 hover:bg-charcoal-50'
                }`}
              >
                Buyer
                <span className="block text-[10px] font-normal text-charcoal-400 mt-0.5">Deploy AI to negotiate</span>
              </button>
              <button
                type="button"
                onClick={() => setRole('ROLE_SELLER')}
                className={`p-3 rounded-xl border text-xs font-semibold transition-all ${
                  role === 'ROLE_SELLER'
                    ? 'border-purple-600 bg-purple-50/70 text-purple-900 ring-2 ring-purple-500/20'
                    : 'border-charcoal-200 bg-white text-charcoal-600 hover:bg-charcoal-50'
                }`}
              >
                Merchant / Seller
                <span className="block text-[10px] font-normal text-charcoal-400 mt-0.5">Onboard catalog & rules</span>
              </button>
            </div>
          </div>

          <Button
            type="submit"
            variant="primary"
            size="md"
            className="w-full font-semibold rounded-xl mt-2"
            isLoading={isSubmitting}
          >
            Create Account
          </Button>
        </form>
      </Card>

      <p className="text-center text-xs text-charcoal-500">
        Already have an account?{' '}
        <Link to="/login" className="font-semibold text-primary-600 hover:text-primary-700">
          Sign In
        </Link>
      </p>
    </div>
  );
};
