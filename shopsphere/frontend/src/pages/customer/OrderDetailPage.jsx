import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Package,
  Calendar,
  CreditCard,
  MapPin,
  AlertTriangle,
  ArrowLeft,
  XCircle,
  Truck,
  CheckCircle2,
  Clock,
} from 'lucide-react';
import { orderApi } from '../../api/orderApi';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { OrderBadge, PaymentBadge } from '../../components/common/Badge';
import { useToastStore } from '../../store/useToastStore';
import ConfirmModal from '../../components/common/ConfirmModal';

export default function OrderDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const { success, error } = useToastStore();

  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');

  const { data: order, isLoading } = useQuery({
    queryKey: ['orderDetail', id],
    queryFn: () => orderApi.getOrderById(id),
  });

  // Cancel order mutation
  const cancelOrderMutation = useMutation({
    mutationFn: () => orderApi.cancelOrder(id, cancelReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orderDetail', id] });
      queryClient.invalidateQueries({ queryKey: ['customerOrders'] });
      setCancelModalOpen(false);
      success('Order has been cancelled successfully', 'Order Cancelled');
    },
    onError: (err) => {
      error(err.message || 'Could not cancel order', 'Cancellation Failed');
    },
  });

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4 space-y-6 animate-pulse">
        <div className="h-6 bg-slate-200 rounded w-1/4"></div>
        <div className="h-32 bg-slate-200 rounded-3xl"></div>
        <div className="h-64 bg-slate-200 rounded-3xl"></div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="max-w-md mx-auto py-16 text-center space-y-4">
        <h2 className="text-xl font-bold text-slate-900">Order Not Found</h2>
        <Link to="/orders" className="text-xs font-bold text-emerald-600">
          ← Return to Orders
        </Link>
      </div>
    );
  }

  const isCancellable = order.orderStatus === 'PLACED' || order.orderStatus === 'CONFIRMED';
  const isCancelled = order.orderStatus === 'CANCELLED';

  // Status milestones
  const steps = [
    { key: 'PLACED', label: 'Order Placed' },
    { key: 'CONFIRMED', label: 'Confirmed' },
    { key: 'PROCESSING', label: 'Processing' },
    { key: 'SHIPPED', label: 'Shipped' },
    { key: 'DELIVERED', label: 'Delivered' },
  ];

  const currentStepIdx = steps.findIndex((s) => s.key === order.orderStatus);

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <Link
            to="/orders"
            className="p-2 rounded-xl bg-white border border-slate-200 text-slate-500 hover:text-slate-800"
            aria-label="Back to Orders"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-xl sm:text-2xl font-black text-slate-900">
                Order #{order.orderNumber}
              </h1>
              <OrderBadge status={order.orderStatus} />
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Placed on {formatDate(order.createdAt, true)}
            </p>
          </div>
        </div>

        {/* Cancel button if eligible */}
        {isCancellable && (
          <button
            type="button"
            onClick={() => setCancelModalOpen(true)}
            className="px-4 py-2 rounded-xl border border-rose-200 text-rose-600 bg-rose-50 hover:bg-rose-100 text-xs font-bold transition-colors self-start sm:self-center"
          >
            Cancel Order
          </button>
        )}
      </div>

      {/* Status Progress Timeline */}
      <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm">
        {isCancelled ? (
          <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl flex items-center space-x-3 text-rose-800 text-xs font-medium">
            <XCircle className="w-5 h-5 text-rose-600 shrink-0" />
            <div>
              <span className="font-bold block">This order was cancelled</span>
              {order.notes && <p className="text-[11px] mt-0.5">Reason: {order.notes}</p>}
            </div>
          </div>
        ) : (
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-6">
              Delivery Progress
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
              {steps.map((step, idx) => {
                const isCompleted = currentStepIdx >= idx;
                const isCurrent = currentStepIdx === idx;

                return (
                  <div key={step.key} className="flex flex-col items-center text-center space-y-2">
                    <div
                      className={`w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold transition-colors ${
                        isCompleted
                          ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30'
                          : 'bg-slate-100 text-slate-400'
                      }`}
                    >
                      {isCompleted ? <CheckCircle2 className="w-4 h-4" /> : idx + 1}
                    </div>
                    <span
                      className={`text-xs font-semibold ${
                        isCurrent
                          ? 'text-emerald-700 font-bold'
                          : isCompleted
                          ? 'text-slate-800'
                          : 'text-slate-400'
                      }`}
                    >
                      {step.label}
                    </span>
                  </div>
                );
              })}
            </div>

            {order.trackingNumber && (
              <div className="mt-6 p-4 bg-emerald-50/60 border border-emerald-100 rounded-2xl flex items-center justify-between text-xs">
                <div className="flex items-center space-x-2 text-emerald-900">
                  <Truck className="w-4 h-4 text-emerald-600" />
                  <span>Tracking Number: <strong className="font-mono">{order.trackingNumber}</strong></span>
                </div>
                {order.estimatedDeliveryDate && (
                  <span className="text-emerald-700">
                    Est. Delivery: {formatDate(order.estimatedDeliveryDate)}
                  </span>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Items Snapshot Table */}
      <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm space-y-4">
        <h3 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">
          Order Items Snapshot
        </h3>

        <div className="divide-y divide-slate-100">
          {order.items?.map((item) => (
            <div key={item.id} className="py-4 flex items-center justify-between text-xs gap-4">
              <div className="flex items-center space-x-3 truncate">
                <div className="w-12 h-12 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-100">
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
                  <h4 className="font-bold text-slate-900 truncate">{item.productName}</h4>
                  <p className="text-[11px] text-slate-400 font-mono">
                    SKU: {item.productSku} {item.variantName ? `• ${item.variantName}` : ''}
                  </p>
                  <p className="text-[11px] text-slate-500">
                    {formatCurrency(item.unitPrice)} × {item.quantity}
                  </p>
                </div>
              </div>

              <div className="text-right shrink-0">
                <span className="text-sm font-black text-slate-900">
                  {formatCurrency(item.totalPrice)}
                </span>
              </div>
            </div>
          ))}
        </div>

        {/* Cost Breakdown */}
        <div className="border-t border-slate-100 pt-4 space-y-2 text-xs text-slate-600 max-w-xs ml-auto">
          <div className="flex justify-between">
            <span>Subtotal</span>
            <span className="font-semibold text-slate-900">{formatCurrency(order.subtotal)}</span>
          </div>
          <div className="flex justify-between">
            <span>GST (5%)</span>
            <span className="font-semibold text-slate-900">{formatCurrency(order.taxAmount)}</span>
          </div>
          <div className="flex justify-between">
            <span>Shipping</span>
            <span className="font-semibold text-slate-900">
              {Number(order.shippingFee) === 0 ? 'FREE' : formatCurrency(order.shippingFee)}
            </span>
          </div>
          {Number(order.discountAmount) > 0 && (
            <div className="flex justify-between text-emerald-600">
              <span>Discount</span>
              <span className="font-bold">-{formatCurrency(order.discountAmount)}</span>
            </div>
          )}
          <div className="border-t border-slate-200 pt-2 flex justify-between text-sm font-black text-slate-900">
            <span>Total Amount</span>
            <span>{formatCurrency(order.totalAmount)}</span>
          </div>
        </div>
      </div>

      {/* Address & Payment Info Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Shipping Address */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm space-y-3">
          <div className="flex items-center space-x-2 text-slate-900 font-bold text-sm">
            <MapPin className="w-4 h-4 text-emerald-600" />
            <span>Delivery Destination</span>
          </div>
          {order.shippingAddress ? (
            <p className="text-xs text-slate-600 leading-relaxed">
              <strong className="text-slate-900 font-semibold">{order.shippingAddress.fullName}</strong>
              <br />
              {order.shippingAddress.addressLine1}
              {order.shippingAddress.addressLine2 ? `, ${order.shippingAddress.addressLine2}` : ''}
              <br />
              {order.shippingAddress.city}, {order.shippingAddress.state} - {order.shippingAddress.postalCode}
              <br />
              Phone: {order.shippingAddress.phoneNumber}
            </p>
          ) : (
            <p className="text-xs text-slate-400">No shipping address recorded</p>
          )}
        </div>

        {/* Payment Summary */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm space-y-3">
          <div className="flex items-center space-x-2 text-slate-900 font-bold text-sm">
            <CreditCard className="w-4 h-4 text-emerald-600" />
            <span>Payment Information</span>
          </div>
          {order.payment ? (
            <div className="space-y-1.5 text-xs text-slate-600">
              <div className="flex justify-between">
                <span>Method:</span>
                <span className="font-semibold text-slate-900">{order.payment.paymentMethod}</span>
              </div>
              <div className="flex justify-between items-center">
                <span>Status:</span>
                <PaymentBadge status={order.payment.paymentStatus} />
              </div>
              {order.payment.transactionId && (
                <div className="flex justify-between font-mono text-[11px]">
                  <span>Txn ID:</span>
                  <span className="text-slate-700 truncate max-w-[150px]">{order.payment.transactionId}</span>
                </div>
              )}
            </div>
          ) : (
            <p className="text-xs text-slate-400">No payment transaction recorded</p>
          )}
        </div>
      </div>

      {/* Cancel Order Modal */}
      <ConfirmModal
        isOpen={cancelModalOpen}
        title="Cancel Order"
        message="Are you sure you want to cancel this order? Any reserved inventory will be released."
        confirmText="Confirm Cancellation"
        confirmVariant="danger"
        isLoading={cancelOrderMutation.isPending}
        onConfirm={() => cancelOrderMutation.mutate()}
        onClose={() => setCancelModalOpen(false)}
      />
    </div>
  );
}
