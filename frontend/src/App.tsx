import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './components/auth/ProtectedRoute';

// Pages
import { HomePage } from './pages/HomePage';
import { CatalogPage } from './pages/CatalogPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { NegotiationArenaPage } from './pages/NegotiationArenaPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { OrderSuccessPage } from './pages/OrderSuccessPage';
import { SellerDashboardPage } from './pages/SellerDashboardPage';
import { SellerOnboardingPage } from './pages/SellerOnboardingPage';
import { BuyerDashboardPage } from './pages/BuyerDashboardPage';
import { ProfilePage } from './pages/ProfilePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { NotFoundPage } from './pages/NotFoundPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2, // 2 minutes
      retry: 1,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <AppLayout>
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<HomePage />} />
              <Route path="/catalog" element={<CatalogPage />} />
              <Route path="/catalog/:id" element={<ProductDetailPage />} />
              <Route path="/arena/:id" element={<NegotiationArenaPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              {/* Protected Shared Routes (Buyer, Seller, Admin) */}
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <ProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* Protected Buyer Routes */}
              <Route
                path="/checkout/:negotiationId"
                element={
                  <ProtectedRoute>
                    <CheckoutPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/order-success/:orderId"
                element={
                  <ProtectedRoute>
                    <OrderSuccessPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/buyer/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['ROLE_BUYER', 'ROLE_ADMIN']}>
                    <BuyerDashboardPage />
                  </ProtectedRoute>
                }
              />

              {/* Protected Seller Routes */}
              <Route
                path="/seller/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['ROLE_SELLER', 'ROLE_ADMIN']}>
                    <SellerDashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/seller/onboard"
                element={
                  <ProtectedRoute allowedRoles={['ROLE_SELLER', 'ROLE_ADMIN']}>
                    <SellerOnboardingPage />
                  </ProtectedRoute>
                }
              />

              {/* 404 Fallback */}
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </AppLayout>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
