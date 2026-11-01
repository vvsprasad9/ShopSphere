import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download, AlertTriangle, Package, CheckCircle2, XCircle, Info } from 'lucide-react';
import analyticsApi from '../../../api/analyticsApi';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function InventoryAnalyticsView() {
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);
  const size = 20;

  const { data, isLoading } = useQuery({
    queryKey: ['adminInventoryHealth', status, page, size],
    queryFn: () => analyticsApi.getInventoryHealth({ status, page, size }),
  });

  const health = data?.data || {};
  const summary = health.summary || {};
  const items = health.items?.content || [];
  const totalPages = health.items?.totalPages || 0;

  return (
    <div className="space-y-6">
      {/* View Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Authoritative Stock Health</h1>
          <p className="text-xs text-slate-500 mt-1">
            Variant-deduplicated physical stock counts and purchasable inventory availability.
          </p>
        </div>

        <div className="flex items-center space-x-2.5">
          <button
            onClick={() => analyticsApi.downloadInventoryCsv({ status })}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition shadow-sm"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export Stock CSV</span>
          </button>
        </div>
      </div>

      {/* Formula Explanation Alert */}
      <div className="bg-purple-50 border border-purple-200 rounded-2xl p-4 flex items-start space-x-3 text-xs text-purple-900">
        <Info className="w-5 h-5 text-purple-600 shrink-0 mt-0.5" />
        <div>
          <p className="font-bold text-sm text-purple-950">Authoritative Purchasable Stock Formula</p>
          <p className="mt-0.5 text-purple-800">
            <code className="font-mono bg-purple-100 px-1.5 py-0.5 rounded text-purple-900 font-semibold">
              purchasableQuantity = Math.max(0, quantityAvailable - quantityReserved)
            </code>
            . Reserved units held for active checkouts are strictly protected and never counted as available for sale.
          </p>
        </div>
      </div>

      {/* Stock Health Metric Cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[10px] font-bold text-slate-400 uppercase">On-Hand Stock</span>
          <div className="text-xl font-black text-slate-900 mt-0.5">{summary.totalUnitsAvailable || 0}</div>
          <span className="text-[10px] text-slate-400">Physical units in warehouse</span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[10px] font-bold text-slate-400 uppercase">Reserved Stock</span>
          <div className="text-xl font-black text-amber-600 mt-0.5">{summary.totalUnitsReserved || 0}</div>
          <span className="text-[10px] text-slate-400">Held for checkout/orders</span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[10px] font-bold text-slate-400 uppercase">Net Purchasable</span>
          <div className="text-xl font-black text-purple-700 mt-0.5">{summary.totalPurchasableUnits || 0}</div>
          <span className="text-[10px] text-slate-400">Available to customers</span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[10px] font-bold text-amber-600 uppercase">Low Stock Alerts</span>
          <div className="text-xl font-black text-amber-600 mt-0.5">{summary.lowStockCount || 0}</div>
          <span className="text-[10px] text-slate-400">≤ threshold units</span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
          <span className="text-[10px] font-bold text-rose-600 uppercase">Out of Stock</span>
          <div className="text-xl font-black text-rose-600 mt-0.5">{summary.outOfStockCount || 0}</div>
          <span className="text-[10px] text-slate-400">0 purchasable units</span>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-200 pb-3">
        {[
          { id: 'ALL', label: 'All Items' },
          { id: 'LOW_STOCK', label: 'Low Stock Alerts' },
          { id: 'OUT_OF_STOCK', label: 'Out of Stock' },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => {
              setStatus(tab.id);
              setPage(0);
            }}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all ${
              status === tab.id
                ? 'bg-purple-600 text-white shadow-sm'
                : 'text-slate-600 hover:text-slate-900 bg-slate-100'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Stock Items Table */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12">
            <LoadingSpinner text="Loading inventory records..." />
          </div>
        ) : items.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-400">
            No inventory items matching status: {status}.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100 text-slate-600 font-bold uppercase tracking-wider text-[10px]">
                  <th className="py-3.5 px-4">Item & Variant</th>
                  <th className="py-3.5 px-4">SKU</th>
                  <th className="py-3.5 px-4">Category</th>
                  <th className="py-3.5 px-4 text-center">Available</th>
                  <th className="py-3.5 px-4 text-center">Reserved</th>
                  <th className="py-3.5 px-4 text-center">Purchasable Stock</th>
                  <th className="py-3.5 px-4 text-center">Threshold</th>
                  <th className="py-3.5 px-4 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {items.map((item) => (
                  <tr key={item.inventoryId} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4 font-bold text-slate-900">{item.itemName}</td>
                    <td className="py-3 px-4 text-slate-500 font-mono text-[11px]">{item.sku}</td>
                    <td className="py-3 px-4 text-slate-600">{item.categoryName}</td>
                    <td className="py-3 px-4 text-center font-semibold text-slate-900">{item.quantityAvailable}</td>
                    <td className="py-3 px-4 text-center text-amber-600 font-semibold">{item.quantityReserved}</td>
                    <td className="py-3 px-4 text-center font-black text-purple-700 text-sm">
                      {item.purchasableQuantity}
                    </td>
                    <td className="py-3 px-4 text-center text-slate-400">{item.lowStockThreshold}</td>
                    <td className="py-3 px-4 text-center">
                      <span
                        className={`inline-flex items-center text-[10px] font-extrabold px-2.5 py-0.5 rounded-full ${
                          item.stockStatus === 'OUT_OF_STOCK'
                            ? 'bg-rose-50 text-rose-700'
                            : item.stockStatus === 'LOW_STOCK'
                            ? 'bg-amber-50 text-amber-700'
                            : 'bg-emerald-50 text-emerald-700'
                        }`}
                      >
                        {item.stockStatus === 'OUT_OF_STOCK' && <XCircle className="w-3 h-3 mr-1" />}
                        {item.stockStatus === 'LOW_STOCK' && <AlertTriangle className="w-3 h-3 mr-1" />}
                        {item.stockStatus === 'ADEQUATE' && <CheckCircle2 className="w-3 h-3 mr-1" />}
                        {item.stockStatus.replace('_', ' ')}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Controls */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Page {page + 1} of {totalPages}</span>
            <div className="flex items-center space-x-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-40 text-slate-700 font-semibold"
              >
                Previous
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 disabled:opacity-40 text-slate-700 font-semibold"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
