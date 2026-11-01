import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download, Package, Layers, Star, ArrowUpRight } from 'lucide-react';
import analyticsApi from '../../../api/analyticsApi';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function ProductAnalyticsView() {
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 29);
    return d.toISOString().slice(0, 10);
  });
  const [endDate, setEndDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [limit, setLimit] = useState(25);

  const { data: productsData, isLoading: isProductsLoading } = useQuery({
    queryKey: ['adminProductPerformance', startDate, endDate, limit],
    queryFn: () => analyticsApi.getProductPerformance({ startDate, endDate, limit }),
  });

  const { data: categoryData, isLoading: isCategoryLoading } = useQuery({
    queryKey: ['adminCategorySales', startDate, endDate],
    queryFn: () => analyticsApi.getCategorySales({ startDate, endDate }),
  });

  const products = productsData?.data || [];
  const categories = categoryData?.data || [];

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
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Product & Category Performance</h1>
          <p className="text-xs text-slate-500 mt-1">
            Historical sales volume and revenue derived from purchase-time order snapshots.
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
            onClick={() => analyticsApi.downloadProductsCsv({ startDate, endDate })}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition shadow-sm"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Category Performance Breakdown */}
      <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
        <h2 className="text-base font-extrabold text-slate-900 mb-1">Category Market Share</h2>
        <p className="text-xs text-slate-400 mb-6">Revenue and unit distribution across active catalog categories.</p>

        {isCategoryLoading ? (
          <LoadingSpinner text="Computing category distribution..." />
        ) : categories.length === 0 ? (
          <div className="py-6 text-center text-xs text-slate-400">No category sales recorded.</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {categories.map((c) => (
              <div key={c.categoryId || c.categoryName} className="p-4 rounded-xl bg-slate-50 border border-slate-100">
                <div className="flex justify-between items-start mb-2">
                  <span className="text-xs font-bold text-slate-900 truncate">{c.categoryName}</span>
                  <span className="text-[11px] font-extrabold text-purple-700 bg-purple-100 px-2 py-0.5 rounded-full">
                    {c.percentageOfTotalSales}%
                  </span>
                </div>
                <div className="flex justify-between text-xs text-slate-500">
                  <span>{formatINR(c.totalRevenue)}</span>
                  <span>{c.unitsSold} units</span>
                </div>
                <div className="w-full h-1.5 bg-slate-200 rounded-full mt-2.5 overflow-hidden">
                  <div
                    className="h-full bg-purple-600 rounded-full"
                    style={{ width: `${Math.min(100, Number(c.percentageOfTotalSales || 0))}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Top Performing Products Table */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="text-base font-extrabold text-slate-900">Bestselling Products</h2>
            <p className="text-xs text-slate-400 mt-0.5">Ranked by physical unit volume and revenue.</p>
          </div>
          <span className="text-xs text-slate-400 font-medium">Top {limit} Items</span>
        </div>

        {isProductsLoading ? (
          <div className="p-12">
            <LoadingSpinner text="Ranking product performance..." />
          </div>
        ) : products.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-400">
            No products sold within the specified range.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100 text-slate-600 font-bold uppercase tracking-wider text-[10px]">
                  <th className="py-3 px-4">Rank</th>
                  <th className="py-3 px-4">Product Name & SKU</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4 text-center">Units Sold</th>
                  <th className="py-3 px-4 text-right">Total Revenue</th>
                  <th className="py-3 px-4 text-center">Purchasable Stock</th>
                  <th className="py-3 px-4 text-center">Customer Rating</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {products.map((p, idx) => (
                  <tr key={p.productId || idx} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4 font-black text-slate-400">#{idx + 1}</td>
                    <td className="py-3 px-4">
                      <div className="font-bold text-slate-900">{p.productName}</div>
                      <div className="text-[10px] text-slate-400">{p.sku}</div>
                    </td>
                    <td className="py-3 px-4 text-slate-600">{p.categoryName}</td>
                    <td className="py-3 px-4 text-center font-bold text-slate-900">{p.unitsSold}</td>
                    <td className="py-3 px-4 text-right font-black text-purple-700">{formatINR(p.totalRevenue)}</td>
                    <td className="py-3 px-4 text-center">
                      <span
                        className={`inline-block font-bold px-2 py-0.5 rounded-full text-[10px] ${
                          p.currentPurchasableStock <= 0
                            ? 'bg-rose-50 text-rose-700'
                            : p.currentPurchasableStock <= 5
                            ? 'bg-amber-50 text-amber-700'
                            : 'bg-emerald-50 text-emerald-700'
                        }`}
                      >
                        {p.currentPurchasableStock} in stock
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center font-semibold text-slate-700">
                      <span className="inline-flex items-center">
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400 mr-1" />
                        {Number(p.averageRating || 0).toFixed(1)} ({p.reviewCount || 0})
                      </span>
                    </td>
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
