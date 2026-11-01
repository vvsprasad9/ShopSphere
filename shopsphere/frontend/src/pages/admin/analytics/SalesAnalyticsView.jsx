import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download, Filter, TrendingUp, Calendar } from 'lucide-react';
import analyticsApi from '../../../api/analyticsApi';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function SalesAnalyticsView() {
  const [interval, setInterval] = useState('DAILY');
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 29);
    return d.toISOString().slice(0, 10);
  });
  const [endDate, setEndDate] = useState(() => new Date().toISOString().slice(0, 10));

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['adminSalesAnalytics', startDate, endDate, interval],
    queryFn: () => analyticsApi.getSalesTrend({ startDate, endDate, interval }),
  });

  const trends = data?.data || [];

  const formatINR = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val || 0);
  };

  const totals = trends.reduce(
    (acc, cur) => ({
      orders: acc.orders + (cur.orderCount || 0),
      gross: acc.gross + Number(cur.grossSales || 0),
      net: acc.net + Number(cur.netSales || 0),
      discounts: acc.discounts + Number(cur.discounts || 0),
      tax: acc.tax + Number(cur.tax || 0),
      shipping: acc.shipping + Number(cur.shippingCharges || 0),
      collected: acc.collected + Number(cur.collectedRevenue || 0),
      refunds: acc.refunds + Number(cur.confirmedRefunds || 0),
      netCash: acc.netCash + Number(cur.netCollectedRevenue || 0),
    }),
    { orders: 0, gross: 0, net: 0, discounts: 0, tax: 0, shipping: 0, collected: 0, refunds: 0, netCash: 0 }
  );

  return (
    <div className="space-y-6">
      {/* View Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Sales & Revenue Breakdown</h1>
          <p className="text-xs text-slate-500 mt-1">
            Interval ledger distinguishing Invoiced Gross Sales, Accrued Discounts, and Settled Cash Collections.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Interval Switcher */}
          <div className="inline-flex rounded-xl bg-slate-100 p-1 text-xs font-semibold">
            {['DAILY', 'WEEKLY', 'MONTHLY'].map((i) => (
              <button
                key={i}
                onClick={() => setInterval(i)}
                className={`px-3 py-1.5 rounded-lg transition-all ${
                  interval === i
                    ? 'bg-white text-purple-700 font-bold shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {i.charAt(0) + i.slice(1).toLowerCase()}
              </button>
            ))}
          </div>

          {/* Date Picker */}
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
            onClick={() => analyticsApi.downloadSalesCsv({ startDate, endDate })}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition shadow-sm"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Summary KPI Strips */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">Total Orders</span>
          <div className="text-xl font-black text-slate-900 mt-0.5">{totals.orders}</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">Gross Invoiced</span>
          <div className="text-xl font-black text-purple-700 mt-0.5">{formatINR(totals.gross)}</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">Discounts Given</span>
          <div className="text-xl font-black text-amber-600 mt-0.5">-{formatINR(totals.discounts)}</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">Net Settled Cash</span>
          <div className="text-xl font-black text-emerald-700 mt-0.5">{formatINR(totals.netCash)}</div>
        </div>
      </div>

      {/* Tabular Interval Breakdown */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12">
            <LoadingSpinner text="Computing interval sales ledger..." />
          </div>
        ) : trends.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-400">
            No sales recorded in the selected date range.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100 text-slate-600 font-bold uppercase tracking-wider text-[10px]">
                  <th className="py-3.5 px-4">Period</th>
                  <th className="py-3.5 px-4 text-center">Orders</th>
                  <th className="py-3.5 px-4 text-right">Gross Sales</th>
                  <th className="py-3.5 px-4 text-right">Discounts</th>
                  <th className="py-3.5 px-4 text-right">Shipping</th>
                  <th className="py-3.5 px-4 text-right">Tax</th>
                  <th className="py-3.5 px-4 text-right">Net Sales</th>
                  <th className="py-3.5 px-4 text-right">Collected Cash</th>
                  <th className="py-3.5 px-4 text-right">Refunds</th>
                  <th className="py-3.5 px-4 text-right">Net Cash</th>
                  <th className="py-3.5 px-4 text-right">AOV</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {trends.map((t, idx) => (
                  <tr key={idx} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4 font-bold text-slate-900">{t.periodLabel}</td>
                    <td className="py-3 px-4 text-center font-semibold">{t.orderCount}</td>
                    <td className="py-3 px-4 text-right font-semibold">{formatINR(t.grossSales)}</td>
                    <td className="py-3 px-4 text-right text-amber-600 font-medium">-{formatINR(t.discounts)}</td>
                    <td className="py-3 px-4 text-right text-slate-500">{formatINR(t.shippingCharges)}</td>
                    <td className="py-3 px-4 text-right text-slate-500">{formatINR(t.tax)}</td>
                    <td className="py-3 px-4 text-right font-bold text-purple-700">{formatINR(t.netSales)}</td>
                    <td className="py-3 px-4 text-right font-semibold text-slate-900">{formatINR(t.collectedRevenue)}</td>
                    <td className="py-3 px-4 text-right font-medium text-rose-600">
                      {Number(t.confirmedRefunds) > 0 ? `-${formatINR(t.confirmedRefunds)}` : '₹0'}
                    </td>
                    <td className="py-3 px-4 text-right font-black text-emerald-700">{formatINR(t.netCollectedRevenue)}</td>
                    <td className="py-3 px-4 text-right font-medium text-slate-600">{formatINR(t.averageOrderValue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
