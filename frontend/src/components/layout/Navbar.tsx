import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import {
  Sparkles,
  Bot,
  Store,
  ShoppingBag,
  LogOut,
  User as UserIcon,
  Settings,
  Store as StoreIcon,
  ShieldCheck,
  Menu,
  X,
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isActive = (path: string) => location.pathname === path;

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  return (
    <header className="sticky top-0 z-40 w-full border-b border-charcoal-200/80 bg-white/85 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <Link to="/" className="flex items-center gap-2.5 group">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-600 to-primary-700 flex items-center justify-center text-white shadow-subtle group-hover:scale-105 transition-transform">
            <Bot className="w-5 h-5" />
          </div>
          <div>
            <span className="font-bold text-lg text-charcoal-900 tracking-tight">
              Trade<span className="text-primary-600">Twin</span>
            </span>
            <span className="hidden sm:inline-block ml-2 text-[10px] font-semibold uppercase tracking-wider text-charcoal-400 border border-charcoal-200 px-1.5 py-0.5 rounded">
              Agentic Commerce
            </span>
          </div>
        </Link>

        {/* Center Nav Links - Conditionally adapted by role */}
        <nav className="hidden md:flex items-center gap-1 font-medium text-sm text-charcoal-600">
          <Link
            to="/"
            className={`px-3.5 py-2 rounded-lg transition-colors flex items-center gap-1.5 ${
              isActive('/')
                ? 'text-primary-700 bg-primary-50 font-semibold'
                : 'hover:text-charcoal-900 hover:bg-charcoal-100/60'
            }`}
          >
            <Sparkles className="w-4 h-4 text-primary-600" />
            AI Shopper
          </Link>

          <Link
            to="/catalog"
            className={`px-3.5 py-2 rounded-lg transition-colors flex items-center gap-1.5 ${
              isActive('/catalog')
                ? 'text-primary-700 bg-primary-50 font-semibold'
                : 'hover:text-charcoal-900 hover:bg-charcoal-100/60'
            }`}
          >
            <ShoppingBag className="w-4 h-4 text-charcoal-400" />
            Marketplace
          </Link>

          {/* Buyer Navigation Links */}
          {isAuthenticated && (user?.role === 'ROLE_BUYER' || user?.role === 'ROLE_ADMIN') && (
            <Link
              to="/buyer/dashboard"
              className={`px-3.5 py-2 rounded-lg transition-colors flex items-center gap-1.5 ${
                isActive('/buyer/dashboard')
                  ? 'text-primary-700 bg-primary-50 font-semibold'
                  : 'hover:text-charcoal-900 hover:bg-charcoal-100/60'
              }`}
            >
              <Sparkles className="w-4 h-4 text-primary-500" />
              Buyer Studio
            </Link>
          )}

          {/* Seller Navigation Links */}
          {isAuthenticated && (user?.role === 'ROLE_SELLER' || user?.role === 'ROLE_ADMIN') && (
            <>
              <Link
                to="/seller/dashboard"
                className={`px-3.5 py-2 rounded-lg transition-colors flex items-center gap-1.5 ${
                  isActive('/seller/dashboard')
                    ? 'text-purple-700 bg-purple-50 font-semibold'
                    : 'hover:text-charcoal-900 hover:bg-charcoal-100/60'
                }`}
              >
                <Store className="w-4 h-4 text-purple-500" />
                Seller Portal
              </Link>
              <Link
                to="/seller/onboard"
                className={`px-3.5 py-2 rounded-lg transition-colors flex items-center gap-1.5 ${
                  isActive('/seller/onboard')
                    ? 'text-purple-700 bg-purple-50 font-semibold'
                    : 'hover:text-charcoal-900 hover:bg-charcoal-100/60'
                }`}
              >
                <StoreIcon className="w-4 h-4 text-charcoal-400" />
                Store Setup
              </Link>
            </>
          )}

          {/* Account Profile Link for all Authenticated Users */}
          {isAuthenticated && (
            <Link
              to="/profile"
              className={`px-3.5 py-2 rounded-lg transition-colors flex items-center gap-1.5 ${
                isActive('/profile')
                  ? 'text-charcoal-900 bg-charcoal-100 font-semibold'
                  : 'hover:text-charcoal-900 hover:bg-charcoal-100/60'
              }`}
            >
              <Settings className="w-4 h-4 text-charcoal-400" />
              Account Settings
            </Link>
          )}
        </nav>

        {/* Right CTA / User State */}
        <div className="flex items-center gap-3">
          {isAuthenticated && user ? (
            <div className="flex items-center gap-3">
              <Link
                to="/profile"
                className="hidden sm:flex flex-col text-right hover:opacity-80 transition-opacity"
              >
                <span className="text-xs font-semibold text-charcoal-900 flex items-center justify-end gap-1">
                  {user.fullName}
                  {user.role === 'ROLE_ADMIN' && <ShieldCheck className="w-3.5 h-3.5 text-primary-600" />}
                </span>
                <div className="flex items-center justify-end gap-1.5 mt-0.5">
                  <Badge
                    variant={
                      user.role === 'ROLE_ADMIN'
                        ? 'primary'
                        : user.role === 'ROLE_SELLER'
                        ? 'purple'
                        : 'outline'
                    }
                    size="sm"
                  >
                    {user.role === 'ROLE_ADMIN'
                      ? 'Administrator'
                      : user.role === 'ROLE_SELLER'
                      ? 'Verified Merchant'
                      : 'Buyer Agent'}
                  </Badge>
                  <span className="text-[10px] text-charcoal-400">
                    ★ {user.reputationScore ? user.reputationScore.toFixed(0) : '100'}
                  </span>
                </div>
              </Link>

              <Button
                variant="ghost"
                size="sm"
                onClick={logout}
                className="text-charcoal-500 hover:text-charcoal-800"
                title="Sign out"
              >
                <LogOut className="w-4 h-4" />
              </Button>
            </div>
          ) : (
            <div className="hidden sm:flex items-center gap-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate('/login')}
                leftIcon={<UserIcon className="w-4 h-4" />}
              >
                Sign In
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={() => navigate('/register')}
              >
                Get Started
              </Button>
            </div>
          )}

          {/* Mobile Hamburger Toggle */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen((prev) => !prev)}
            className="md:hidden p-2 rounded-lg text-charcoal-600 hover:bg-charcoal-100 hover:text-charcoal-900 focus:outline-none focus:ring-2 focus:ring-primary-500/30"
            aria-label={mobileMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
            aria-expanded={mobileMenuOpen}
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Responsive Mobile Navigation Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-charcoal-200 bg-white px-4 py-3 space-y-1.5 shadow-lg">
          <Link
            to="/"
            onClick={() => setMobileMenuOpen(false)}
            className={`px-3 py-2.5 rounded-lg text-sm font-medium flex items-center gap-2 ${
              isActive('/') ? 'bg-primary-50 text-primary-700 font-semibold' : 'text-charcoal-700 hover:bg-charcoal-50'
            }`}
          >
            <Sparkles className="w-4 h-4 text-primary-600" />
            AI Shopper
          </Link>

          <Link
            to="/catalog"
            onClick={() => setMobileMenuOpen(false)}
            className={`px-3 py-2.5 rounded-lg text-sm font-medium flex items-center gap-2 ${
              isActive('/catalog') ? 'bg-primary-50 text-primary-700 font-semibold' : 'text-charcoal-700 hover:bg-charcoal-50'
            }`}
          >
            <ShoppingBag className="w-4 h-4 text-charcoal-500" />
            Marketplace
          </Link>

          {isAuthenticated && (user?.role === 'ROLE_BUYER' || user?.role === 'ROLE_ADMIN') && (
            <Link
              to="/buyer/dashboard"
              onClick={() => setMobileMenuOpen(false)}
              className={`px-3 py-2.5 rounded-lg text-sm font-medium flex items-center gap-2 ${
                isActive('/buyer/dashboard') ? 'bg-primary-50 text-primary-700 font-semibold' : 'text-charcoal-700 hover:bg-charcoal-50'
              }`}
            >
              <Sparkles className="w-4 h-4 text-primary-500" />
              Buyer Studio
            </Link>
          )}

          {isAuthenticated && (user?.role === 'ROLE_SELLER' || user?.role === 'ROLE_ADMIN') && (
            <>
              <Link
                to="/seller/dashboard"
                onClick={() => setMobileMenuOpen(false)}
                className={`px-3 py-2.5 rounded-lg text-sm font-medium flex items-center gap-2 ${
                  isActive('/seller/dashboard') ? 'bg-purple-50 text-purple-700 font-semibold' : 'text-charcoal-700 hover:bg-charcoal-50'
                }`}
              >
                <Store className="w-4 h-4 text-purple-500" />
                Seller Portal
              </Link>
              <Link
                to="/seller/onboard"
                onClick={() => setMobileMenuOpen(false)}
                className={`px-3 py-2.5 rounded-lg text-sm font-medium flex items-center gap-2 ${
                  isActive('/seller/onboard') ? 'bg-purple-50 text-purple-700 font-semibold' : 'text-charcoal-700 hover:bg-charcoal-50'
                }`}
              >
                <StoreIcon className="w-4 h-4 text-charcoal-400" />
                Store Setup
              </Link>
            </>
          )}

          {isAuthenticated && (
            <Link
              to="/profile"
              onClick={() => setMobileMenuOpen(false)}
              className={`px-3 py-2.5 rounded-lg text-sm font-medium flex items-center gap-2 ${
                isActive('/profile') ? 'bg-charcoal-100 text-charcoal-900 font-semibold' : 'text-charcoal-700 hover:bg-charcoal-50'
              }`}
            >
              <Settings className="w-4 h-4 text-charcoal-500" />
              Account Settings
            </Link>
          )}

          {!isAuthenticated && (
            <div className="pt-2 border-t border-charcoal-100 flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                className="flex-1"
                onClick={() => {
                  setMobileMenuOpen(false);
                  navigate('/login');
                }}
              >
                Sign In
              </Button>
              <Button
                variant="primary"
                size="sm"
                className="flex-1"
                onClick={() => {
                  setMobileMenuOpen(false);
                  navigate('/register');
                }}
              >
                Get Started
              </Button>
            </div>
          )}
        </div>
      )}
    </header>
  );
};
