import React from 'react';
import { formatCurrency } from '../../utils/formatters';
import { AlertTriangle, ShieldCheck } from 'lucide-react';

export default function OrderSummary({ preview, isLoading = false }) {
  if (isLoading || !preview) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4 animate-pulse">
        <div className="h-4 bg-slate-200 rounded w-1/3"></div>
        <div className="space-y-2">
          <div className="h-3 bg-slate-200 rounded w-full"></div>
          <div className="h-3 bg-slate-200 rounded w-full"></div>
          <div className="h-3 bg-slate-200 rounded w-2/3"></div>
        </div>
      </div>
    );
  }

  const items = preview.items || [];
  const hasWarnings = preview.warnings && preview.warnings.length > 0;

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-6">
      <h3 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">
        Order Summary ({preview.totalQuantity || items.length} {items.length === 1 ? 'item' : 'items'})
      </h3>

      {/* Warnings if any */}
      {hasWarnings && (
        <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl space-y-1">
          <div className="flex items-center space-x-1.5 text-xs font-bold text-amber-800">
            <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
            <span>Stock or Pricing Notices</span>
          </div>
          {preview.warnings.map((w, idx) => (
            <p key={idx} className="text-[11px] text-amber-700 leading-snug">
              • {w}
            </p>
          ))}
        </div>
      )}

      {/* Items List */}
      <div className="space-y-3 max-h-60 overflow-y-auto pr-1">
        {items.map((item) => (
          <div key={item.id} className="flex items-center justify-between text-xs py-1">
            <div className="flex items-center space-x-2.5 truncate mr-2">
              <div className="w-10 h-10 rounded-lg bg-slate-100 overflow-hidden shrink-0 border border-slate-100">
                {item.productImageUrl ? (
                  <img
                    src={item.productImageUrl}
                    alt={item.productName}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center font-bold text-slate-300">
                    S
                  </div>
                )}
              </div>
              <div className="truncate">
                <p className="font-semibold text-slate-800 truncate">{item.productName}</p>
                <p className="text-[11px] text-slate-400">
                  Qty: {item.quantity} {item.variantName ? `• ${item.variantName}` : ''}
                </p>
              </div>
            </div>
            <span className="font-bold text-slate-900 shrink-0">
              {formatCurrency(item.totalPrice)}
            </span>
          </div>
        ))}
      </div>

      {/* Server-Authoritative Totals Breakdown */}
      <div className="border-t border-slate-100 pt-4 space-y-2.5 text-xs">
        <div className="flex justify-between text-slate-600">
          <span>Cart Subtotal</span>
          <span className="font-semibold text-slate-900">{formatCurrency(preview.subtotal)}</span>
        </div>

        <div className="flex justify-between text-slate-600">
          <span>GST / Tax (5% included)</span>
          <span className="font-semibold text-slate-900">{formatCurrency(preview.taxAmount)}</span>
        </div>

        <div className="flex justify-between text-slate-600">
          <span>Shipping Fee</span>
          <span className="font-semibold text-slate-900">
            {Number(preview.shippingFee) === 0 ? (
              <span className="text-emerald-600 font-bold uppercase">Free</span>
            ) : (
              formatCurrency(preview.shippingFee)
            )}
          </span>
        </div>

        {Number(preview.discountAmount) > 0 && (
          <div className="flex justify-between text-emerald-600">
            <span>Coupon Discount</span>
            <span className="font-bold">-{formatCurrency(preview.discountAmount)}</span>
          </div>
        )}

        <div className="border-t border-slate-200 pt-3 flex justify-between items-baseline">
          <span className="text-sm font-extrabold text-slate-900">Total Payable</span>
          <span className="text-lg font-black text-slate-900">
            {formatCurrency(preview.totalAmount)}
          </span>
        </div>
      </div>

      <div className="flex items-center space-x-2 text-[11px] text-slate-500 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
        <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
        <span>Price, tax and inventory verified authoritatively by server</span>
      </div>
    </div>
  );
}
