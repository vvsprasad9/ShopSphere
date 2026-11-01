import React from 'react';
import { getOrderStatusBadge, getPaymentStatusBadge, getReviewStatusBadge } from '../../utils/formatters';

export function OrderBadge({ status }) {
  const { label, color } = getOrderStatusBadge(status);
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${color}`}>
      {label}
    </span>
  );
}

export function PaymentBadge({ status }) {
  const { label, color } = getPaymentStatusBadge(status);
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${color}`}>
      {label}
    </span>
  );
}

export function ReviewBadge({ status }) {
  const { label, color } = getReviewStatusBadge(status);
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${color}`}>
      {label}
    </span>
  );
}

export function StockBadge({ quantity, inStock }) {
  if (!inStock || quantity <= 0) {
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-rose-100 text-rose-800 border border-rose-200">
        Out of Stock
      </span>
    );
  }

  if (quantity <= 5) {
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-100 text-amber-800 border border-amber-200">
        Only {quantity} left
      </span>
    );
  }

  return (
    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
      In Stock
    </span>
  );
}
