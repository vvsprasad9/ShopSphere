import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  ShoppingBag,
  Package,
  Users,
  AlertTriangle,
  Download,
  Calendar,
  CreditCard,
  Clock,
  ArrowUpRight,
  ArrowDownRight,
  Filter,
} from 'lucide-react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import analyticsApi from '../../../api/analyticsApi';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

const COLORS = ['#8b5cf6', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#6366f1'];

export default function AnalyticsDashboardPage() {
  const [preset, setPreset] = useState('30d');
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');

  // Calculate dates based on preset
  const getDateRange = () => {
    const today = new Date();
    const formatDate = (d) => d.toISOString().slice(0, 10);

    if (preset === '7d') {
      const start = new Date();
      start.setDate(today.getDate() - 6);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    if (preset === 'month') {
      const start = new Date(today.getFullYear(), today.getMonth(), 1);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    if (preset === '90d') {
      const start = new Date();
      start.setDate(today.getDate() - 89);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    if (preset === 'custom' && customStart && customEnd) {
      return { startDate: customStart, endDate: customEnd };
    }
    // Default 30d
    const start = new Date();
    start.setDate(today.getDate() - 29);
    return { startDate: formatDate(start), endDate: formatDate(today) };
  };

  const dates = getDateRange();

  // Queries
  const { data: overviewData, isLoading: isOverviewLoading } = useQuery({
    queryKey: ['adminAnalyticsOverview', dates.startDate, dates.endDate],
    queryFn: () => analyticsApi.getOverview(dates),
  });

  const { data: salesTrendData, isLoading: isSalesLoading } = useQuery({
    queryKey: ['adminAnalyticsSalesTrend', dates.startDate, dates.endDate],
    queryFn: () => analyticsApi.getSalesTrend({ ...dates, interval: 'DAILY' }),
  });

  const { data: paymentsData, isLoading: isPaymentsLoading } = useQuery({
    queryKey: ['adminAnalyticsPayments', dates.startDate, dates.endDate],
    queryFn: () => analyticsApi.getPaymentAnalytics(dates),
  });

  const overview = overviewData?.data || {};
  const financials = overview?.financials || {};
  const orders = overview?.orders || {};
  const catalog = overview?.catalog || {};
  const customers = overview?.customers || {};
  const salesTrends = salesTrendData?.data || [];
  const paymentBreakdown = paymentsData?.data || {};

  const formatINR = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val || 0);
  };

  const renderTrendBadge = (pct) => {
    const num = Number(pct || 0);
    const isPositive = num >= 0;
    return (
      <span
        className={`inline-flex items-center text-[11px] font-bold px-2 py-0.5 rounded-full ${
          isPositive ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
        }`}
      >
        {isPositive ? (
          <ArrowUpRight className="w-3.5 h-3.5 mr-0.5" />
        ) : (
          <ArrowDownRight className="w-3.5 h-3.5 mr-0.5" />
        )}
        {Math.abs(num).toFixed(1)}% vs prev
      </span>
    );
  };

  if (isOverviewLoading) {
    return <LoadingSpinner text="Aggregating financial and store analytics..." />;
  }

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Executive Analytics</h1>
          <p className="text-xs text-slate-500 mt-1">
            Authoritative revenue accounting, cash collection lifecycle, and real-time store metrics.
          </p>
        </div>

        {/* Date Filter & Export */}
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="inline-flex rounded-xl bg-slate-100 p-1 text-xs font-semibold">
            {[
              { id: '7d', label: 'Last 7 Days' },
              { id: '30d', label: 'Last 30 Days' },
              { id: 'month', label: 'This Month' },
              { id: '90d', label: 'Last 90 Days' },
              { id: 'custom', label: 'Custom' },
            ].map((p) => (
              <button
                key={p.id}
                onClick={() => setPreset(p.id)}
                className={`px-3 py-1.5 rounded-lg transition-all ${
                  preset === p.id
                    ? 'bg-white text-purple-700 font-bold shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>

          {preset === 'custom' && (
            <div className="flex items-center space-x-1.5 bg-white border border-slate-200 rounded-xl px-2.5 py-1 text-xs">
              <input
                type="date"
                value={customStart}
                onChange={(e) => setCustomStart(e.target.value)}
                className="outline-none text-slate-700 text-xs"
              />
              <span className="text-slate-400">to</span>
              <input
                type="date"
                value={customEnd}
                onChange={(e) => setCustomEnd(e.target.value)}
                className="outline-none text-slate-700 text-xs"
              />
            </div>
          )}

          <button
            onClick={() => analyticsApi.downloadSalesCsv(dates)}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition shadow-sm"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Primary Financial KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Gross Sales */}
        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-slate-500 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Gross Sales</span>
              <div className="p-2 rounded-xl bg-purple-50 text-purple-600">
                <DollarSign className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-slate-900">
              {formatINR(financials.grossSales)}
            </div>
            <p className="text-[11px] text-slate-400 mt-1">Invoiced value before discounts</p>
          </div>
          <div className="mt-4 pt-3 border-t border-slate-50 flex items-center justify-between">
            <span className="text-[11px] text-slate-500">Period Trend:</span>
            {renderTrendBadge(financials.grossSalesChangePercent)}
          </div>
        </div>

        {/* Net Collected Revenue */}
        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-slate-500 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-emerald-700">Net Collected Cash</span>
              <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-emerald-700">
              {formatINR(financials.netCollectedRevenue)}
            </div>
            <p className="text-[11px] text-slate-400 mt-1">Settled cash minus confirmed refunds</p>
          </div>
          <div className="mt-4 pt-3 border-t border-slate-50 flex items-center justify-between">
            <span className="text-[11px] text-slate-500">Cash Growth:</span>
            {renderTrendBadge(financials.netCollectedRevenueChangePercent)}
          </div>
        </div>

        {/* Total Orders & AOV */}
        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-slate-500 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Total Orders</span>
              <div className="p-2 rounded-xl bg-blue-50 text-blue-600">
                <ShoppingBag className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-slate-900">
              {orders.totalOrders || 0}
            </div>
            <p className="text-[11px] text-slate-500 mt-1">
              AOV: <span className="font-bold text-slate-800">{formatINR(financials.averageOrderValue)}</span>
            </p>
          </div>
          <div className="mt-4 pt-3 border-t border-slate-50 flex items-center justify-between">
            <span className="text-[11px] text-slate-500">Volume:</span>
            {renderTrendBadge(financials.orderCountChangePercent)}
          </div>
        </div>

        {/* Pending Collections (COD + Online) */}
        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-slate-500 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-amber-700">Pending Collections</span>
              <div className="p-2 rounded-xl bg-amber-50 text-amber-600">
                <Clock className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-amber-700">
              {formatINR(financials.pendingCollections)}
            </div>
            <div className="text-[11px] text-slate-500 mt-1 flex items-center space-x-2">
              <span>COD: <strong className="text-slate-800">{formatINR(financials.pendingCodCollections)}</strong></span>
              <span>•</span>
              <span>Online: <strong className="text-slate-800">{formatINR(financials.pendingOnlineCollections)}</strong></span>
            </div>
          </div>
          <div className="mt-4 pt-3 border-t border-slate-50 flex items-center justify-between text-[11px] text-slate-500">
            <span>Refunds:</span>
            <span className="font-bold text-rose-600">-{formatINR(financials.confirmedRefunds)}</span>
          </div>
        </div>
      </div>

      {/* Inventory & Operational Pulse */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm flex items-center space-x-3.5">
          <div className="p-3 bg-purple-50 text-purple-600 rounded-xl">
            <Package className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-500 uppercase">Purchasable Stock</div>
            <div className="text-lg font-black text-slate-900">
              {catalog.totalPurchasableUnits || 0} units
            </div>
            <div className="text-[11px] text-slate-400">
              {catalog.totalReservedUnits || 0} units currently reserved
            </div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm flex items-center space-x-3.5">
          <div className="p-3 bg-amber-50 text-amber-600 rounded-xl">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-500 uppercase">Stock Health Alerts</div>
            <div className="text-lg font-black text-slate-900">
              {catalog.lowStockItemCount || 0} low / {catalog.outOfStockItemCount || 0} out
            </div>
            <div className="text-[11px] text-slate-400">Deduplicated across variants</div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm flex items-center space-x-3.5">
          <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-500 uppercase">Customer Acquisition</div>
            <div className="text-lg font-black text-slate-900">
              +{customers.newCustomersInPeriod || 0} new buyers
            </div>
            <div className="text-[11px] text-slate-400">
              {customers.returningBuyersCount || 0} repeat purchasers
            </div>
          </div>
        </div>
      </div>

      {/* Chart: Sales Trends (Gross vs Net Collected) */}
      <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-base font-extrabold text-slate-900">Revenue & Cash Flow Velocity</h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Comparison between Invoiced Gross Sales and Settled Cash Collections over time.
            </p>
          </div>
          <div className="flex items-center space-x-4 text-xs font-semibold">
            <span className="flex items-center">
              <span className="w-3 h-3 rounded-full bg-purple-600 mr-1.5" /> Gross Sales
            </span>
            <span className="flex items-center">
              <span className="w-3 h-3 rounded-full bg-emerald-500 mr-1.5" /> Net Collected Cash
            </span>
          </div>
        </div>

        <div className="h-72 w-full">
          {salesTrends.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={salesTrends} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="grossGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="netGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="periodLabel" tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <Tooltip
                  formatter={(val) => formatINR(val)}
                  contentStyle={{ backgroundColor: '#1e293b', borderRadius: '12px', border: 'none', color: '#fff', fontSize: '12px' }}
                />
                <Area type="monotone" dataKey="grossSales" name="Gross Sales" stroke="#8b5cf6" strokeWidth={2.5} fillOpacity={1} fill="url(#grossGradient)" />
                <Area type="monotone" dataKey="netCollectedRevenue" name="Net Collected Cash" stroke="#10b981" strokeWidth={2.5} fillOpacity={1} fill="url(#netGradient)" />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-full flex items-center justify-center text-xs text-slate-400">
              No sales records found in this interval.
            </div>
          )}
        </div>
      </div>

      {/* Grid: Order Status Funnel & Payment Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Order Status Breakdown */}
        <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
          <h2 className="text-base font-extrabold text-slate-900 mb-1">Fulfillment Status Breakdown</h2>
          <p className="text-xs text-slate-400 mb-6">Distribution of non-cancelled orders across fulfillment stages.</p>

          <div className="space-y-3.5">
            {[
              { label: 'Pending Payment / Auth', count: orders.pendingOrders, color: 'bg-amber-400' },
              { label: 'Confirmed & Awaiting Pick', count: orders.confirmedOrders, color: 'bg-blue-500' },
              { label: 'Processing in Warehouse', count: orders.processingOrders, color: 'bg-purple-500' },
              { label: 'Shipped / In Transit', count: orders.shippedOrders, color: 'bg-indigo-500' },
              { label: 'Successfully Delivered', count: orders.deliveredOrders, color: 'bg-emerald-500' },
              { label: 'Cancelled / Expired', count: orders.cancelledOrders, color: 'bg-rose-500' },
            ].map((item) => {
              const total = orders.totalOrders || 1;
              const pct = Math.round(((item.count || 0) / total) * 100);
              return (
                <div key={item.label}>
                  <div className="flex justify-between text-xs font-semibold mb-1">
                    <span className="text-slate-700">{item.label}</span>
                    <span className="text-slate-500">{item.count || 0} ({pct}%)</span>
                  </div>
                  <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                    <div className={`h-full ${item.color} rounded-full`} style={{ width: `${pct}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Payment Method Distribution */}
        <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <div>
            <h2 className="text-base font-extrabold text-slate-900 mb-1">Payment Method Distribution</h2>
            <p className="text-xs text-slate-400 mb-4">Volume breakdown across active payment gateways and COD.</p>

            <div className="h-56 w-full flex items-center justify-center">
              {paymentBreakdown?.methodDistribution?.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={paymentBreakdown.methodDistribution}
                      dataKey="totalAmount"
                      nameKey="paymentMethod"
                      cx="50%"
                      cy="50%"
                      innerRadius={50}
                      outerRadius={80}
                      paddingAngle={4}
                    >
                      {paymentBreakdown.methodDistribution.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(val) => formatINR(val)} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="text-xs text-slate-400">No payment transaction records in this period.</div>
              )}
            </div>
          </div>

          <div className="pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Collected Cash Ratio:</span>
            <span className="font-bold text-slate-800">
              {paymentBreakdown?.cashBreakdown?.collectedRevenue
                ? formatINR(paymentBreakdown.cashBreakdown.collectedRevenue)
                : '₹0'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
