import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download, Users, UserPlus, Repeat, ShoppingCart } from 'lucide-react';
import analyticsApi from '../../../api/analyticsApi';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function CustomerAnalyticsView() {
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 29);
    return d.toISOString().slice(0, 10);
  });
  const [endDate, setEndDate] = useState(() => new Date().toISOString().slice(0, 10));

  const { data, isLoading } = useQuery({
    queryKey: ['adminCustomerAnalytics', startDate, endDate],
    queryFn: () => analyticsApi.getCustomerAnalytics({ startDate, endDate }),
  });

  const customerData = data?.data || {};
  const distribution = customerData.orderFrequencyDistribution || {};

  const formatINR = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val || 0);
  };

  return (
    <div className="space-y-8">
      {/* View Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Customer Intelligence & Retention</h1>
          <p className="text-xs text-slate-500 mt-1">
            Aggregated acquisition, buyer lifetime frequency, and repeat purchasing behavior.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <div className="flex items-center space-x-1.5 bg-white border border-slate-200 rounded-xl px-2.5 py-1 text-xs">
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="outline-none text-slate-700 text-xs"
            />
            <span className="text-slate-400">to</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="outline-none text-slate-700 text-xs"
            />
          </div>

          <button
            onClick={() => analyticsApi.downloadCustomersCsv({ startDate, endDate })}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition shadow-sm"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export Customers CSV</span>
          </button>
        </div>
      </div>

      {isLoading ? (
        <LoadingSpinner text="Analyzing customer cohort metrics..." />
      ) : (
        <>
          {/* Metrics Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm">
              <div className="flex items-center justify-between text-slate-400 mb-1">
                <span className="text-[10px] font-bold uppercase tracking-wider">Registered Accounts</span>
                <Users className="w-4 h-4 text-purple-600" />
              </div>
              <div className="text-2xl font-black text-slate-900">{customerData.totalCustomers || 0}</div>
              <p className="text-[11px] text-slate-400 mt-1">Platform user base</p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm">
              <div className="flex items-center justify-between text-slate-400 mb-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700">New Registrations</span>
                <UserPlus className="w-4 h-4 text-emerald-600" />
              </div>
              <div className="text-2xl font-black text-emerald-700">+{customerData.newCustomersInPeriod || 0}</div>
              <p className="text-[11px] text-slate-400 mt-1">Acquired in this period</p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm">
              <div className="flex items-center justify-between text-slate-400 mb-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-blue-700">Repeat Purchasers</span>
                <Repeat className="w-4 h-4 text-blue-600" />
              </div>
              <div className="text-2xl font-black text-blue-700">{customerData.returningBuyersCount || 0}</div>
              <p className="text-[11px] text-slate-400 mt-1">≥ 2 lifetime orders</p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm">
              <div className="flex items-center justify-between text-slate-400 mb-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-purple-700">Cohort AOV</span>
                <ShoppingCart className="w-4 h-4 text-purple-600" />
              </div>
              <div className="text-2xl font-black text-purple-700">{formatINR(customerData.averageOrderValue)}</div>
              <p className="text-[11px] text-slate-400 mt-1">Average order value</p>
            </div>
          </div>

          {/* Purchasing Frequency Cohort */}
          <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
            <h2 className="text-base font-extrabold text-slate-900 mb-1">Lifetime Purchasing Frequency</h2>
            <p className="text-xs text-slate-400 mb-6">Customer retention grouped by total orders placed over lifetime.</p>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {Object.entries(distribution).map(([tier, count]) => {
                const totalBuyers = customerData.totalBuyers || 1;
                const pct = Math.round((Number(count || 0) / totalBuyers) * 100);
                return (
                  <div key={tier} className="p-5 rounded-xl bg-slate-50 border border-slate-100">
                    <div className="text-xs font-bold text-slate-500 uppercase tracking-wider">{tier}</div>
                    <div className="text-3xl font-black text-slate-900 mt-1">{count}</div>
                    <div className="flex items-center justify-between text-xs text-slate-500 mt-3">
                      <span>Buyer Share:</span>
                      <span className="font-bold text-purple-700">{pct}%</span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-200 rounded-full mt-1.5 overflow-hidden">
                      <div className="h-full bg-purple-600 rounded-full" style={{ width: `${pct}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
