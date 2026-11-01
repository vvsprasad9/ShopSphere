import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Package, ChevronRight, ChevronLeft, Calendar, ArrowRight } from 'lucide-react';
import { orderApi } from '../../api/orderApi';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { OrderBadge, PaymentBadge } from '../../components/common/Badge';

export default function OrdersPage() {
  const [page, setPage] = useState(0);

  const { data: ordersData, isLoading } = useQuery({
    queryKey: ['customerOrders', page],
    queryFn: () => orderApi.getCustomerOrders({ page, size: 10, sortBy: 'createdAt', direction: 'desc' }),
  });

  const orders = ordersData?.content || [];
  const totalPages = ordersData?.totalPages || 0;
  const totalElements = ordersData?.totalElements || 0;

  if (isLoading) {
    return (
      <div className="max-w-5xl mx-auto py-10 px-4 space-y-4 animate-pulse">
        <div className="h-6 bg-slate-200 rounded w-1/4"></div>
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-32 bg-slate-200 rounded-2xl"></div>
          ))}
        </div>
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="max-w-2xl mx-auto py-20 px-4 text-center space-y-4">
        <div className="w-16 h-16 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
          <Package className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-black text-slate-900">No Orders Yet</h2>
        <p className="text-xs sm:text-sm text-slate-500 max-w-sm mx-auto">
          You haven't placed any orders yet. Once you complete a purchase, it will appear here.
        </p>
        <Link
          to="/catalog"
          className="inline-flex items-center px-6 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-md transition-colors"
        >
          <span>Discover Products</span>
          <ArrowRight className="w-4 h-4 ml-2" />
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8 pb-16">
      <div>
        <h1 className="text-2xl font-black text-slate-900">My Orders</h1>
        <p className="text-xs text-slate-500 mt-1">
          {totalElements} total {totalElements === 1 ? 'order' : 'orders'} placed
        </p>
      </div>

      <div className="space-y-4">
        {orders.map((order) => {
          const itemCount = order.items?.reduce((sum, item) => sum + item.quantity, 0) || order.items?.length || 0;

          return (
            <div
              key={order.id}
              className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm hover:shadow-md transition-all flex flex-col md:flex-row md:items-center justify-between gap-6"
            >
              <div className="space-y-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-mono font-bold text-sm text-slate-900">
                    #{order.orderNumber}
                  </span>
                  <OrderBadge status={order.orderStatus} />
                  {order.payment?.paymentStatus && (
                    <PaymentBadge status={order.payment.paymentStatus} />
                  )}
                </div>

                <div className="flex items-center space-x-4 text-xs text-slate-500">
                  <div className="flex items-center space-x-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>{formatDate(order.createdAt, true)}</span>
                  </div>
                  <span>•</span>
                  <span>{itemCount} {itemCount === 1 ? 'item' : 'items'}</span>
                  {order.trackingNumber && (
                    <>
                      <span>•</span>
                      <span className="font-mono text-emerald-700 font-semibold">
                        Tracking: {order.trackingNumber}
                      </span>
                    </>
                  )}
                </div>

                {/* Items preview preview */}
                <div className="flex items-center space-x-2 overflow-x-auto pt-1">
                  {order.items?.slice(0, 4).map((item) => (
                    <div
                      key={item.id}
                      className="w-10 h-10 rounded-lg bg-slate-100 border border-slate-100 overflow-hidden shrink-0"
                      title={item.productName}
                    >
                      {item.productImageUrl ? (
                        <img
                          src={item.productImageUrl}
                          alt={item.productName}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center font-bold text-slate-300 text-xs">
                          S
                        </div>
                      )}
                    </div>
                  ))}
                  {order.items && order.items.length > 4 && (
                    <span className="text-[11px] font-bold text-slate-400">
                      +{order.items.length - 4} more
                    </span>
                  )}
                </div>
              </div>

              {/* Price & Action */}
              <div className="flex items-center justify-between md:flex-col md:items-end gap-3 pt-3 md:pt-0 border-t md:border-t-0 border-slate-100">
                <div className="text-right">
                  <span className="text-xs text-slate-400 block">Total</span>
                  <span className="text-lg font-black text-slate-900">
                    {formatCurrency(order.totalAmount)}
                  </span>
                </div>

                <Link
                  to={`/orders/${order.id}`}
                  className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition-colors flex items-center space-x-1"
                >
                  <span>View Details</span>
                  <ChevronRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          );
        })}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between bg-white rounded-2xl border border-slate-200 p-4 shadow-sm">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 flex items-center space-x-1"
          >
            <ChevronLeft className="w-4 h-4" />
            <span>Previous</span>
          </button>
          <span className="text-xs font-bold text-slate-700">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            onClick={() => setPage((p) => p + 1)}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 flex items-center space-x-1"
          >
            <span>Next</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
}
