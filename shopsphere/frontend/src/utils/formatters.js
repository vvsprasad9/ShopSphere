// Currency formatter for Indian Rupee (INR)
export function formatCurrency(amount) {
  if (amount === null || amount === undefined) return '₹0.00';
  const numeric = typeof amount === 'number' ? amount : parseFloat(amount);
  if (isNaN(numeric)) return '₹0.00';

  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numeric);
}

// Date formatter
export function formatDate(dateString, includeTime = false) {
  if (!dateString) return '—';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '—';

    if (includeTime) {
      return date.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    }

    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return '—';
  }
}

// Status styling mapping for OrderStatus
export function getOrderStatusBadge(status) {
  switch (status) {
    case 'PENDING':
      return { label: 'Pending', color: 'bg-amber-100 text-amber-800 border-amber-200' };
    case 'CONFIRMED':
      return { label: 'Confirmed', color: 'bg-blue-100 text-blue-800 border-blue-200' };
    case 'PROCESSING':
      return { label: 'Processing', color: 'bg-indigo-100 text-indigo-800 border-indigo-200' };
    case 'SHIPPED':
      return { label: 'Shipped', color: 'bg-purple-100 text-purple-800 border-purple-200' };
    case 'OUT_FOR_DELIVERY':
      return { label: 'Out for Delivery', color: 'bg-cyan-100 text-cyan-800 border-cyan-200' };
    case 'DELIVERED':
      return { label: 'Delivered', color: 'bg-emerald-100 text-emerald-800 border-emerald-200' };
    case 'CANCELLED':
      return { label: 'Cancelled', color: 'bg-rose-100 text-rose-800 border-rose-200' };
    case 'RETURNED':
      return { label: 'Returned', color: 'bg-slate-100 text-slate-800 border-slate-200' };
    default:
      return { label: status || 'Unknown', color: 'bg-slate-100 text-slate-800 border-slate-200' };
  }
}

// Status styling mapping for PaymentStatus
export function getPaymentStatusBadge(status) {
  switch (status) {
    case 'SUCCESS':
      return { label: 'Paid', color: 'bg-emerald-100 text-emerald-800 border-emerald-200' };
    case 'PENDING':
      return { label: 'Payment Pending', color: 'bg-amber-100 text-amber-800 border-amber-200' };
    case 'CREATED':
      return { label: 'Initiated', color: 'bg-blue-100 text-blue-800 border-blue-200' };
    case 'FAILED':
      return { label: 'Payment Failed', color: 'bg-rose-100 text-rose-800 border-rose-200' };
    case 'REFUND_PENDING':
      return { label: 'Refund Pending', color: 'bg-orange-100 text-orange-800 border-orange-200' };
    case 'REFUNDED':
      return { label: 'Refunded', color: 'bg-purple-100 text-purple-800 border-purple-200' };
    case 'REFUND_FAILED':
      return { label: 'Refund Failed', color: 'bg-red-100 text-red-800 border-red-200' };
    default:
      return { label: status || 'Unknown', color: 'bg-slate-100 text-slate-800 border-slate-200' };
  }
}

// Review moderation badge
export function getReviewStatusBadge(status) {
  switch (status) {
    case 'APPROVED':
      return { label: 'Approved', color: 'bg-emerald-100 text-emerald-800 border-emerald-200' };
    case 'PENDING':
      return { label: 'Pending Moderation', color: 'bg-amber-100 text-amber-800 border-amber-200' };
    case 'REJECTED':
      return { label: 'Rejected', color: 'bg-rose-100 text-rose-800 border-rose-200' };
    case 'HIDDEN':
      return { label: 'Hidden', color: 'bg-slate-100 text-slate-800 border-slate-200' };
    default:
      return { label: status || 'Unknown', color: 'bg-slate-100 text-slate-800 border-slate-200' };
  }
}
