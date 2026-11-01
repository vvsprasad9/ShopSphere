import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { CheckCircle2, Package, ArrowRight, Home } from 'lucide-react';

export default function OrderSuccessPage() {
  const { orderNumber } = useParams();

  return (
    <div className="max-w-xl mx-auto py-16 px-4 text-center space-y-6">
      <div className="w-20 h-20 rounded-3xl bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-inner shadow-emerald-500/20">
        <CheckCircle2 className="w-10 h-10 stroke-[2.5]" />
      </div>

      <div className="space-y-2">
        <h1 className="text-3xl font-black text-slate-900">Order Confirmed!</h1>
        <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto">
          Thank you for shopping with ShopSphere. Your order has been placed successfully and is being prepared for shipment.
        </p>
      </div>

      {orderNumber && (
        <div className="p-4 bg-white rounded-2xl border border-slate-200 inline-block shadow-sm">
          <span className="text-xs text-slate-400 uppercase tracking-wider block font-semibold">
            Order Reference
          </span>
          <span className="text-base font-mono font-bold text-slate-900 tracking-wider">
            {orderNumber}
          </span>
        </div>
      )}

      <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
        {orderNumber && (
          <Link
            to={`/track/${orderNumber}`}
            className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-md transition-colors flex items-center justify-center space-x-2"
          >
            <Package className="w-4 h-4" />
            <span>Track Order Delivery</span>
          </Link>
        )}

        <Link
          to="/orders"
          className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold text-xs transition-colors flex items-center justify-center space-x-2"
        >
          <span>View Order History</span>
        </Link>
      </div>

      <div className="pt-6">
        <Link
          to="/"
          className="inline-flex items-center text-xs font-semibold text-emerald-600 hover:text-emerald-700"
        >
          <Home className="w-3.5 h-3.5 mr-1" />
          <span>Return to Homepage</span>
        </Link>
      </div>
    </div>
  );
}
