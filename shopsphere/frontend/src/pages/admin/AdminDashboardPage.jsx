import React from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Package,
  ShoppingBag,
  MessageSquare,
  AlertCircle,
  TrendingUp,
  ArrowRight,
  ShieldCheck,
  Layers,
} from 'lucide-react';
import { orderApi } from '../../api/orderApi';
import { productApi } from '../../api/productApi';
import { reviewApi } from '../../api/reviewApi';
import AdminStatsCard from '../../components/admin/AdminStatsCard';

export default function AdminDashboardPage() {
  // Authoritative Total Orders from Backend PageResponse.totalElements
  const { data: ordersPage, isLoading: ordersLoading } = useQuery({
    queryKey: ['adminTotalOrdersCount'],
    queryFn: () => orderApi.getAllOrders({ page: 0, size: 1 }),
  });

  // Authoritative Active Catalog Items from Backend PageResponse.totalElements
  const { data: productsPage, isLoading: productsLoading } = useQuery({
    queryKey: ['adminActiveProductsCount'],
    queryFn: () => productApi.searchProducts({ active: true, page: 0, size: 1 }),
  });

  // Authoritative Pending Reviews count from Backend PageResponse.totalElements
  const { data: pendingReviewsPage, isLoading: reviewsLoading } = useQuery({
    queryKey: ['adminPendingReviewsCount'],
    queryFn: () => reviewApi.getAdminReviews({ status: 'PENDING', page: 0, size: 1 }),
  });

  const totalOrders = ordersPage?.totalElements ?? '—';
  const activeProducts = productsPage?.totalElements ?? '—';
  const pendingReviews = pendingReviewsPage?.totalElements ?? '—';

  return (
    <div className="space-y-8 pb-16">
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900">
          Admin Executive Dashboard
        </h1>
        <p className="text-xs sm:text-sm text-slate-500 mt-1">
          Authoritative catalog statistics, pending moderations, and operational metrics
        </p>
      </div>

      {/* Authoritative Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <AdminStatsCard
          title="Total Orders"
          value={ordersLoading ? '...' : totalOrders}
          description="Customer orders recorded across system"
          icon={ShoppingBag}
          color="emerald"
        />

        <AdminStatsCard
          title="Active Catalog"
          value={productsLoading ? '...' : activeProducts}
          description="Products published and purchasable"
          icon={Package}
          color="blue"
        />

        <AdminStatsCard
          title="Pending Reviews"
          value={reviewsLoading ? '...' : pendingReviews}
          description="Customer reviews awaiting moderation"
          icon={MessageSquare}
          color="amber"
        />

        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                Revenue Analytics
              </span>
              <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center border border-purple-100">
                <TrendingUp className="w-5 h-5" />
              </div>
            </div>
            <div className="text-sm font-bold text-slate-700 mt-2">
              Backend Aggregation
            </div>
            <p className="text-[11px] text-slate-400 mt-1 leading-snug">
              Global revenue & GMV calculation requires dedicated reporting service.
            </p>
          </div>
          <span className="inline-block px-2.5 py-1 rounded-full text-[10px] font-bold bg-slate-100 text-slate-600 mt-3 self-start">
            Scheduled Service
          </span>
        </div>
      </div>

      {/* Operations Quick Shortcuts */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm space-y-4">
          <h3 className="text-base font-bold text-slate-900">Catalog Operations</h3>
          <p className="text-xs text-slate-500 leading-relaxed">
            Manage product descriptions, variants with independent inventory, image galleries, and category taxonomy.
          </p>
          <div className="flex flex-wrap gap-3 pt-2">
            <Link
              to="/admin/products"
              className="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition-colors flex items-center space-x-1.5"
            >
              <Package className="w-4 h-4" />
              <span>Manage Products</span>
            </Link>
            <Link
              to="/admin/categories"
              className="px-4 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition-colors flex items-center space-x-1.5"
            >
              <Layers className="w-4 h-4" />
              <span>Category Tree</span>
            </Link>
          </div>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm space-y-4">
          <h3 className="text-base font-bold text-slate-900">Order & Customer Fulfillment</h3>
          <p className="text-xs text-slate-500 leading-relaxed">
            Update order lifecycle milestones, assign carrier tracking numbers, verify Cash-On-Delivery collections, and moderate reviews.
          </p>
          <div className="flex flex-wrap gap-3 pt-2">
            <Link
              to="/admin/orders"
              className="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-colors flex items-center space-x-1.5"
            >
              <ShoppingBag className="w-4 h-4" />
              <span>Process Orders</span>
            </Link>
            <Link
              to="/admin/reviews"
              className="px-4 py-2.5 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 text-xs font-bold transition-colors flex items-center space-x-1.5"
            >
              <MessageSquare className="w-4 h-4" />
              <span>Moderate Reviews</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
