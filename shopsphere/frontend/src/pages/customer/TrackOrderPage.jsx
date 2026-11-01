import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, Package, Truck, MapPin, Calendar, CheckCircle2, AlertCircle } from 'lucide-react';
import { orderApi } from '../../api/orderApi';
import { formatDate } from '../../utils/formatters';
import { OrderBadge } from '../../components/common/Badge';

export default function TrackOrderPage() {
  const { orderNumber: paramOrderNumber } = useParams();
  const navigate = useNavigate();
  const [searchInput, setSearchInput] = useState(paramOrderNumber || '');
  const [activeNumber, setActiveNumber] = useState(paramOrderNumber || '');

  useEffect(() => {
    if (paramOrderNumber) {
      setSearchInput(paramOrderNumber);
      setActiveNumber(paramOrderNumber);
    }
  }, [paramOrderNumber]);

  const { data: tracking, isLoading, error } = useQuery({
    queryKey: ['orderTracking', activeNumber],
    queryFn: () => orderApi.trackOrder(activeNumber),
    enabled: !!activeNumber,
    retry: false,
  });

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchInput.trim()) {
      setActiveNumber(searchInput.trim());
      navigate(`/track/${encodeURIComponent(searchInput.trim())}`);
    }
  };

  const steps = [
    { key: 'PLACED', label: 'Order Placed' },
    { key: 'CONFIRMED', label: 'Confirmed' },
    { key: 'PROCESSING', label: 'Processing' },
    { key: 'SHIPPED', label: 'Shipped' },
    { key: 'DELIVERED', label: 'Delivered' },
  ];

  const currentStepIdx = tracking ? steps.findIndex((s) => s.key === tracking.orderStatus) : -1;

  return (
    <div className="max-w-3xl mx-auto space-y-8 py-8 px-4 pb-16">
      <div className="text-center space-y-2">
        <div className="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto">
          <Truck className="w-6 h-6" />
        </div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900">
          Track Your Delivery
        </h1>
        <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto">
          Enter your order reference number to check the real-time shipping status and estimated delivery date.
        </p>
      </div>

      {/* Search Input Form */}
      <form onSubmit={handleSearch} className="max-w-lg mx-auto">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              placeholder="e.g. ORD-20261101-ABC12"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              required
              className="w-full pl-10 pr-4 py-3 bg-white border border-slate-200 rounded-2xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500/30 uppercase tracking-wider"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          </div>
          <button
            type="submit"
            className="px-6 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-md shadow-emerald-600/20 transition-all shrink-0"
          >
            Track
          </button>
        </div>
      </form>

      {/* Loading State */}
      {isLoading && (
        <div className="bg-white rounded-3xl border border-slate-200 p-8 text-center animate-pulse space-y-4">
          <div className="h-6 bg-slate-200 rounded w-1/3 mx-auto"></div>
          <div className="h-20 bg-slate-200 rounded-2xl"></div>
        </div>
      )}

      {/* Error State */}
      {error && !isLoading && (
        <div className="bg-white rounded-3xl border border-rose-200 p-8 text-center shadow-sm space-y-3">
          <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
            <AlertCircle className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-slate-900">Order Not Found</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            We couldn't find tracking information for reference "{activeNumber}". Please verify your order number and try again.
          </p>
        </div>
      )}

      {/* Sanitized Public Tracking Results */}
      {tracking && !isLoading && (
        <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm space-y-8">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-100 gap-4">
            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                Order Reference
              </span>
              <h2 className="text-lg font-black text-slate-900 font-mono">
                #{tracking.orderNumber}
              </h2>
            </div>
            <div>
              <OrderBadge status={tracking.orderStatus} />
            </div>
          </div>

          {/* Delivery Milestone Progress */}
          <div className="space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Shipment Status
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
              {steps.map((step, idx) => {
                const isCompleted = currentStepIdx >= idx;
                const isCurrent = currentStepIdx === idx;

                return (
                  <div key={step.key} className="flex flex-col items-center text-center space-y-1.5">
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold ${
                        isCompleted
                          ? 'bg-emerald-600 text-white'
                          : 'bg-slate-100 text-slate-400'
                      }`}
                    >
                      {isCompleted ? <CheckCircle2 className="w-4 h-4" /> : idx + 1}
                    </div>
                    <span
                      className={`text-[11px] font-semibold ${
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
          </div>

          {/* Delivery Details Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-4 border-t border-slate-100">
            <div className="p-4 bg-slate-50 rounded-2xl space-y-1">
              <div className="flex items-center space-x-1.5 text-xs text-slate-500 font-semibold">
                <MapPin className="w-4 h-4 text-emerald-600" />
                <span>Destination</span>
              </div>
              <p className="text-xs font-bold text-slate-900">
                {tracking.deliveryDestination?.city
                  ? `${tracking.deliveryDestination.city}, ${tracking.deliveryDestination.state}`
                  : 'Pending Address Confirmation'}
              </p>
            </div>

            <div className="p-4 bg-slate-50 rounded-2xl space-y-1">
              <div className="flex items-center space-x-1.5 text-xs text-slate-500 font-semibold">
                <Calendar className="w-4 h-4 text-emerald-600" />
                <span>Order Date</span>
              </div>
              <p className="text-xs font-bold text-slate-900">
                {formatDate(tracking.orderDate)}
              </p>
            </div>

            <div className="p-4 bg-slate-50 rounded-2xl space-y-1">
              <div className="flex items-center space-x-1.5 text-xs text-slate-500 font-semibold">
                <Truck className="w-4 h-4 text-emerald-600" />
                <span>Tracking Carrier</span>
              </div>
              <p className="text-xs font-bold text-slate-900 font-mono">
                {tracking.trackingNumber || 'Awaiting Dispatch'}
              </p>
            </div>
          </div>

          {/* Items Summary (Sanitized) */}
          {tracking.items && tracking.items.length > 0 && (
            <div className="pt-4 border-t border-slate-100 space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Package Contents ({tracking.totalItems} items)
              </h4>
              <div className="divide-y divide-slate-100">
                {tracking.items.map((item, idx) => (
                  <div key={idx} className="py-2.5 flex items-center justify-between text-xs">
                    <span className="font-semibold text-slate-800">
                      {item.productName} {item.variantName ? `(${item.variantName})` : ''}
                    </span>
                    <span className="text-slate-500 font-medium">Qty: {item.quantity}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
