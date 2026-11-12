import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { extractErrorMessage } from '../lib/utils';
import { Bot, Sparkles, Store, ShieldCheck } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login, quickLoginAsBuyer, quickLoginAsSeller, quickLoginAsAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname || '/';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setIsSubmitting(true);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Login failed. Check your email and password.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDemoBuyer = async () => {
    setErrorMsg(null);
    setIsSubmitting(true);
    try {
      await quickLoginAsBuyer();
      navigate('/catalog', { replace: true });
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Demo login failed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDemoSeller = async () => {
    setErrorMsg(null);
    setIsSubmitting(true);
    try {
      await quickLoginAsSeller();
      navigate('/seller/dashboard', { replace: true });
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Demo login failed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDemoAdmin = async () => {
    setErrorMsg(null);
    setIsSubmitting(true);
    try {
      await quickLoginAsAdmin();
      navigate('/catalog', { replace: true });
    } catch (err: unknown) {
      setErrorMsg(extractErrorMessage(err, 'Demo login failed'));
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
        <h1 className="text-2xl font-extrabold text-charcoal-900 tracking-tight">Sign In to TradeTwin</h1>
        <p className="text-xs text-charcoal-500">Access your autonomous agent studio and merchant dashboard.</p>
      </div>

      {errorMsg && <Alert type="error" title="Sign In Error">{errorMsg}</Alert>}

      <Card className="p-6 space-y-5">
        <div className="space-y-2 p-3 bg-charcoal-50 rounded-xl border border-charcoal-200/80">
          <div className="flex items-center justify-between gap-2">
            <span className="text-[10px] font-bold uppercase tracking-wider text-charcoal-400">
              One-Click Evaluation Logins
            </span>
            <span className="px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 border border-amber-200 text-[9px] font-extrabold uppercase tracking-wider">
              DEMO DATA
            </span>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDemoBuyer}
              isLoading={isSubmitting}
              leftIcon={<Sparkles className="w-3.5 h-3.5 text-primary-600" />}
              className="text-xs bg-white px-2"
              title="buyer@tradetwin.com"
            >
              Buyer
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDemoSeller}
              isLoading={isSubmitting}
              leftIcon={<Store className="w-3.5 h-3.5 text-purple-600" />}
              className="text-xs bg-white px-2"
              title="seller@tradetwin.com"
            >
              Seller
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDemoAdmin}
              isLoading={isSubmitting}
              leftIcon={<ShieldCheck className="w-3.5 h-3.5 text-charcoal-700" />}
              className="text-xs bg-white px-2"
              title="admin@tradetwin.com"
            >
              Admin
            </Button>
          </div>
        </div>

        <div className="relative flex py-1 items-center">
          <div className="flex-grow border-t border-charcoal-200" />
          <span className="flex-shrink mx-3 text-[10px] text-charcoal-400 uppercase font-semibold">Or with credentials</span>
          <div className="flex-grow border-t border-charcoal-200" />
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
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
            placeholder="••••••••"
            required
          />

          <Button
            type="submit"
            variant="primary"
            size="md"
            className="w-full font-semibold rounded-xl"
            isLoading={isSubmitting}
          >
            Sign In
          </Button>
        </form>
      </Card>

      <p className="text-center text-xs text-charcoal-500">
        Don't have an account?{' '}
        <Link to="/register" className="font-semibold text-primary-600 hover:text-primary-700">
          Create an account
        </Link>
      </p>
    </div>
  );
};
