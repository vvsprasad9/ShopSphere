import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ShoppingBag,
  Filter,
  Truck,
  Edit2,
  Banknote,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
} from 'lucide-react';
import { orderApi } from '../../api/orderApi';
import { paymentApi } from '../../api/paymentApi';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { OrderBadge, PaymentBadge } from '../../components/common/Badge';
import { useToastStore } from '../../store/useToastStore';

export default function AdminOrdersPage() {
  const queryClient = useQueryClient();
  const { success, error } = useToastStore();

  const [page, setPage] = useState(0);
  const [selectedStatus, setSelectedStatus] = useState('');

  // Status update modal
  const [updateModalOrder, setUpdateModalOrder] = useState(null);
  const [statusForm, setStatusForm] = useState({
    orderStatus: '',
    trackingNumber: '',
    notes: '',
  });

  const { data: ordersData, isLoading } = useQuery({
    queryKey: ['adminOrders', page, selectedStatus],
    queryFn: () =>
      orderApi.getAllOrders({
        status: selectedStatus || undefined,
        page,
        size: 10,
        sortBy: 'createdAt',
        direction: 'desc',
      }),
  });

  const orders = ordersData?.content || [];
  const totalPages = ordersData?.totalPages || 0;
  const totalElements = ordersData?.totalElements || 0;

  // Update order status mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ id, data }) => orderApi.updateOrderStatus(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminOrders'] });
      setUpdateModalOrder(null);
      success('Order status updated successfully', 'Status Updated');
    },
    onError: (err) => {
      error(err.message || 'Could not update order status', 'Error');
    },
  });

  // Collect COD payment mutation
  const collectCodMutation = useMutation({
    mutationFn: (orderId) => paymentApi.collectCodPayment(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminOrders'] });
      success('Cash on Delivery payment marked as collected!', 'Payment Collected');
    },
    onError: (err) => {
      error(err.message || 'Could not collect COD payment', 'Error');
    },
  });

  const handleOpenStatusModal = (order) => {
    setUpdateModalOrder(order);
    setStatusForm({
      orderStatus: order.orderStatus,
      trackingNumber: order.trackingNumber || '',
      notes: order.notes || '',
    });
  };

  const handleStatusSubmit = (e) => {
    e.preventDefault();
    updateStatusMutation.mutate({
      id: updateModalOrder.id,
      data: {
        orderStatus: statusForm.orderStatus,
        trackingNumber: statusForm.trackingNumber.trim() || null,
        notes: statusForm.notes.trim() || null,
      },
    });
  };

  const orderStatuses = [
    'PENDING',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED',
    'RETURNED',
  ];

  return (
    <div className="space-y-6 pb-16">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900">Order Processing</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {totalElements} total customer orders
          </p>
        </div>

        {/* Status Filter Dropdown */}
        <div className="flex items-center space-x-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value);
              setPage(0);
            }}
            className="p-2.5 bg-white border border-slate-200 rounded-xl text-xs font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-500/30"
          >
            <option value="">All Order Statuses</option>
            {orderStatuses.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[10px]">
                <th className="py-3 px-4">Order #</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Date</th>
                <th className="py-3 px-4">Items</th>
                <th className="py-3 px-4">Amount</th>
                <th className="py-3 px-4">Order Status</th>
                <th className="py-3 px-4">Payment</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-400">
                    Loading orders...
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-500">
                    No orders matching selected criteria.
                  </td>
                </tr>
              ) : (
                orders.map((o) => {
                  const isCod = o.payment?.paymentMethod === 'COD';
                  const codPending = isCod && o.payment?.paymentStatus !== 'SUCCESS';

                  return (
                    <tr key={o.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-3 px-4 font-mono font-bold text-slate-900">
                        #{o.orderNumber}
                      </td>

                      <td className="py-3 px-4 text-slate-600 truncate max-w-[150px]">
                        {o.customerEmail || '—'}
                      </td>

                      <td className="py-3 px-4 text-slate-500">
                        {formatDate(o.createdAt)}
                      </td>

                      <td className="py-3 px-4 font-semibold text-slate-700">
                        {o.items?.length || 0}
                      </td>

                      <td className="py-3 px-4 font-black text-slate-900">
                        {formatCurrency(o.totalAmount)}
                      </td>

                      <td className="py-3 px-4">
                        <OrderBadge status={o.orderStatus} />
                      </td>

                      <td className="py-3 px-4">
                        <div className="space-y-1">
                          <PaymentBadge status={o.payment?.paymentStatus} />
                          <span className="text-[10px] text-slate-400 block font-semibold">
                            {o.payment?.paymentMethod}
                          </span>
                        </div>
                      </td>

                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end space-x-2">
                          {/* Collect COD Button */}
                          {codPending && (
                            <button
                              type="button"
                              onClick={() => collectCodMutation.mutate(o.id)}
                              disabled={collectCodMutation.isPending}
                              className="px-2.5 py-1 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-800 text-[11px] font-bold border border-emerald-200 transition-colors flex items-center space-x-1"
                              title="Mark COD Cash Collected"
                            >
                              <Banknote className="w-3.5 h-3.5" />
                              <span>Collect Cash</span>
                            </button>
                          )}

                          <button
                            type="button"
                            onClick={() => handleOpenStatusModal(o)}
                            className="p-1.5 text-slate-400 hover:text-purple-600 rounded-lg hover:bg-slate-100 transition-colors"
                            title="Update Status / Tracking"
                            aria-label="Update Status"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between p-4 border-t border-slate-200">
            <button
              type="button"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 disabled:opacity-40"
            >
              Previous
            </button>
            <span className="text-xs font-semibold text-slate-600">
              Page {page + 1} of {totalPages}
            </span>
            <button
              type="button"
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= totalPages - 1}
              className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        )}
      </div>

      {/* Update Order Status Modal */}
      {updateModalOrder && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 space-y-6">
            <h3 className="text-lg font-bold text-slate-900">
              Update Order #{updateModalOrder.orderNumber}
            </h3>

            <form onSubmit={handleStatusSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Order Status
                </label>
                <select
                  required
                  value={statusForm.orderStatus}
                  onChange={(e) => setStatusForm({ ...statusForm, orderStatus: e.target.value })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-800"
                >
                  {orderStatuses.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Carrier Tracking Number (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. TRK-BLUEDART-987261"
                  value={statusForm.trackingNumber}
                  onChange={(e) => setStatusForm({ ...statusForm, trackingNumber: e.target.value })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-mono font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Admin Internal Notes / Remarks (Optional)
                </label>
                <textarea
                  rows={2}
                  value={statusForm.notes}
                  onChange={(e) => setStatusForm({ ...statusForm, notes: e.target.value })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs resize-none"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setUpdateModalOrder(null)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updateStatusMutation.isPending}
                  className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-purple-600 hover:bg-purple-700 shadow-sm"
                >
                  Save Status
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
