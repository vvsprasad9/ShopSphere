import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Outlet } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Common Components
import Navbar from './components/common/Navbar';
import Footer from './components/common/Footer';
import ToastContainer from './components/common/ToastContainer';
import ProtectedRoute from './components/routes/ProtectedRoute';
import AdminRoute from './components/routes/AdminRoute';

// Storefront Pages
import HomePage from './pages/storefront/HomePage';
import CatalogPage from './pages/storefront/CatalogPage';
import ProductDetailPage from './pages/storefront/ProductDetailPage';

// Auth Pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';

// Customer Pages
import CartPage from './pages/customer/CartPage';
import WishlistPage from './pages/customer/WishlistPage';
import CheckoutPage from './pages/customer/CheckoutPage';
import OrderSuccessPage from './pages/customer/OrderSuccessPage';
import OrdersPage from './pages/customer/OrdersPage';
import OrderDetailPage from './pages/customer/OrderDetailPage';
import TrackOrderPage from './pages/customer/TrackOrderPage';
import ProfilePage from './pages/customer/ProfilePage';
import AddressBookPage from './pages/customer/AddressBookPage';
import NotificationsPage from './pages/customer/NotificationsPage';

// Admin Pages & Layout
import AdminLayout from './components/admin/AdminLayout';
import AnalyticsDashboardPage from './pages/admin/analytics/AnalyticsDashboardPage';
import SalesAnalyticsView from './pages/admin/analytics/SalesAnalyticsView';
import ProductAnalyticsView from './pages/admin/analytics/ProductAnalyticsView';
import InventoryAnalyticsView from './pages/admin/analytics/InventoryAnalyticsView';
import CustomerAnalyticsView from './pages/admin/analytics/CustomerAnalyticsView';
import AuditLogView from './pages/admin/analytics/AuditLogView';
import AdminProductsPage from './pages/admin/AdminProductsPage';
import AdminCategoriesPage from './pages/admin/AdminCategoriesPage';
import AdminOrdersPage from './pages/admin/AdminOrdersPage';
import AdminReviewsPage from './pages/admin/AdminReviewsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30000,
    },
  },
});

function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col justify-between">
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full flex-grow">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

export default function App() {
  // Listen for auth expiration or logout events to reset query client caches
  useEffect(() => {
    const handleAuthReset = () => {
      queryClient.clear();
    };

    window.addEventListener('shopsphere_logout', handleAuthReset);
    window.addEventListener('shopsphere_auth_expired', handleAuthReset);

    return () => {
      window.removeEventListener('shopsphere_logout', handleAuthReset);
      window.removeEventListener('shopsphere_auth_expired', handleAuthReset);
    };
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <Routes>
          {/* Public Storefront Routes with Main Navbar & Footer */}
          <Route element={<PublicLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/catalog" element={<CatalogPage />} />
            <Route path="/product/:slug" element={<ProductDetailPage />} />
            <Route path="/product/id/:id" element={<ProductDetailPage />} />
            <Route path="/track" element={<TrackOrderPage />} />
            <Route path="/track/:orderNumber" element={<TrackOrderPage />} />

            {/* Auth Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />

            {/* Protected Customer Routes */}
            <Route element={<ProtectedRoute />}>
              <Route path="/cart" element={<CartPage />} />
              <Route path="/wishlist" element={<WishlistPage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
              <Route path="/order-success/:orderNumber" element={<OrderSuccessPage />} />
              <Route path="/orders" element={<OrdersPage />} />
              <Route path="/orders/:id" element={<OrderDetailPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/addresses" element={<AddressBookPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
            </Route>
          </Route>

          {/* Admin Routes with Dedicated Admin Shell */}
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<AnalyticsDashboardPage />} />
              <Route path="analytics/sales" element={<SalesAnalyticsView />} />
              <Route path="analytics/products" element={<ProductAnalyticsView />} />
              <Route path="analytics/inventory" element={<InventoryAnalyticsView />} />
              <Route path="analytics/customers" element={<CustomerAnalyticsView />} />
              <Route path="analytics/audit" element={<AuditLogView />} />
              <Route path="products" element={<AdminProductsPage />} />
              <Route path="categories" element={<AdminCategoriesPage />} />
              <Route path="orders" element={<AdminOrdersPage />} />
              <Route path="reviews" element={<AdminReviewsPage />} />
            </Route>
          </Route>

          {/* Fallback Splat Route */}
          <Route path="*" element={<PublicLayout />}>
            <Route path="*" element={<HomePage />} />
          </Route>
        </Routes>

        {/* Global Toast Notification System */}
        <ToastContainer />
      </Router>
    </QueryClientProvider>
  );
}
