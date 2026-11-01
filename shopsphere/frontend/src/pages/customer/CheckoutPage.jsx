import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CreditCard, Banknote, Shield, AlertTriangle, ArrowLeft, CheckCircle2 } from 'lucide-react';
import { addressApi } from '../../api/addressApi';
import { orderApi } from '../../api/orderApi';
import { paymentApi } from '../../api/paymentApi';
import { loadRazorpayScript } from '../../utils/razorpayLoader';
import { useToastStore } from '../../store/useToastStore';
import AddressSelector from '../../components/checkout/AddressSelector';
import CouponInput from '../../components/checkout/CouponInput';
import OrderSummary from '../../components/checkout/OrderSummary';

export default function CheckoutPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { success, error, warning } = useToastStore();

  // Saved Addresses
  const { data: addressesData, isLoading: addressesLoading } = useQuery({
    queryKey: ['addresses'],
    queryFn: () => addressApi.getUserAddresses(),
  });

  const addresses = addressesData || [];
  const defaultAddr = addresses.find((a) => a.isDefault) || addresses[0];

  const [selectedShippingId, setSelectedShippingId] = useState(null);
  const [useSameBilling, setUseSameBilling] = useState(true);
  const [selectedBillingId, setSelectedBillingId] = useState(null);
  const [couponCode, setCouponCode] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('RAZORPAY'); // RAZORPAY, MOCK_TEST, COD
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Logical checkout attempt idempotency key (generated once per logical attempt)
  const idempotencyKeyRef = useRef(crypto.randomUUID());

  // Set default address when loaded
  useEffect(() => {
    if (defaultAddr && !selectedShippingId) {
      setSelectedShippingId(defaultAddr.id);
      setSelectedBillingId(defaultAddr.id);
    }
  }, [defaultAddr, selectedShippingId]);

  // Server-Authoritative Checkout Preview
  const {
    data: preview,
    isLoading: previewLoading,
    refetch: refetchPreview,
  } = useQuery({
    queryKey: ['checkoutPreview', selectedShippingId, couponCode],
    queryFn: () =>
      orderApi.previewCheckout({
        shippingAddressId: selectedShippingId,
        couponCode: couponCode || null,
      }),
    enabled: !!selectedShippingId,
  });

  // Create new address inline mutation
  const createAddressMutation = useMutation({
    mutationFn: (newAddr) => addressApi.createAddress(newAddr),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['addresses'] });
      setSelectedShippingId(created.id);
      if (useSameBilling) setSelectedBillingId(created.id);
      success('Address saved and selected', 'Address Added');
    },
    onError: (err) => {
      error(err.message || 'Could not save address', 'Error');
    },
  });

  // Handle Order Placement
  const handlePlaceOrder = async () => {
    if (isSubmitting) return;

    if (!selectedShippingId) {
      warning('Please select a shipping address before proceeding', 'Shipping Required');
      return;
    }

    if (preview?.hasStockIssues) {
      error('Cannot place order: one or more items exceed available inventory', 'Inventory Alert');
      return;
    }

    setIsSubmitting(true);

    const payload = {
      shippingAddressId: selectedShippingId,
      billingAddressId: useSameBilling ? selectedShippingId : selectedBillingId || selectedShippingId,
      couponCode: preview?.couponApplied ? couponCode : null,
      paymentMethod,
      notes: notes.trim() || null,
      idempotencyKey: idempotencyKeyRef.current,
    };

    try {
      // Step 1: Authoritative Checkout on Backend
      const order = await orderApi.checkout(payload, idempotencyKeyRef.current);
      queryClient.invalidateQueries({ queryKey: ['cart'] });

      // Step 2: Handle Payment according to selected method
      if (paymentMethod === 'RAZORPAY') {
        const scriptLoaded = await loadRazorpayScript();
        const razorpayOrderId = order.payment?.razorpayOrderId;

        if (scriptLoaded && window.Razorpay && razorpayOrderId) {
          const razorpayKey = import.meta.env.VITE_RAZORPAY_KEY_ID || 'rzp_test_shopsphere';

          const options = {
            key: razorpayKey,
            amount: Math.round(Number(order.totalAmount) * 100),
            currency: 'INR',
            name: 'ShopSphere',
            description: `Order #${order.orderNumber}`,
            order_id: razorpayOrderId,
            prefill: {
              email: order.customerEmail || '',
              contact: order.shippingAddress?.phoneNumber || '',
            },
            handler: async (response) => {
              try {
                // Verify signature on backend
                await paymentApi.verifyPayment({
                  razorpayOrderId: response.razorpay_order_id,
                  razorpayPaymentId: response.razorpay_payment_id,
                  razorpaySignature: response.razorpay_signature,
                });
                success('Payment verified! Your order is confirmed.', 'Order Confirmed');
                navigate(`/order-success/${order.orderNumber}`);
              } catch (verifyErr) {
                error(verifyErr.message || 'Payment verification failed', 'Payment Error');
                navigate(`/orders/${order.id}`);
              }
            },
            modal: {
              ondismiss: () => {
                warning('Payment window closed. You can complete payment from your orders page.', 'Payment Pending');
                navigate(`/orders/${order.id}`);
              },
            },
            theme: { color: '#059669' },
          };

          const rzp = new window.Razorpay(options);
          rzp.open();
        } else {
          // Razorpay gateway info fallback
          warning('Razorpay gateway SDK unavailable. Order placed with payment pending.', 'Notice');
          navigate(`/orders/${order.id}`);
        }
      } else if (paymentMethod === 'MOCK_TEST') {
        // Sandbox Simulated Payment for automated test / developer verification
        try {
          await paymentApi.processMockPayment({
            orderId: order.id,
            status: 'SUCCESS',
          });
          success('Sandbox simulated payment successful!', 'Order Confirmed');
          navigate(`/order-success/${order.orderNumber}`);
        } catch (mockErr) {
          error(mockErr.message || 'Mock payment simulation failed', 'Payment Error');
          navigate(`/orders/${order.id}`);
        }
      } else if (paymentMethod === 'COD') {
        // Cash on Delivery
        success('Order placed successfully via Cash on Delivery!', 'Order Placed');
        navigate(`/order-success/${order.orderNumber}`);
      }
    } catch (checkoutErr) {
      error(checkoutErr.message || 'Checkout failed. Please review your order.', 'Checkout Failed');
      setIsSubmitting(false);
    }
  };

  const isDevOrTest = import.meta.env.DEV || import.meta.env.MODE === 'test';

  return (
    <div className="space-y-8 pb-16">
      {/* Header */}
      <div className="flex items-center space-x-3">
        <Link
          to="/cart"
          className="p-2 rounded-xl bg-white border border-slate-200 text-slate-500 hover:text-slate-800"
          aria-label="Back to Cart"
        >
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <div>
          <h1 className="text-2xl font-black text-slate-900">Checkout</h1>
          <p className="text-xs text-slate-500">Secure authoritative payment & address verification</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Left Column: Addresses + Coupon + Payment */}
        <div className="lg:col-span-2 space-y-6">
          {/* 1. Shipping Address */}
          <AddressSelector
            title="1. Shipping Destination"
            addresses={addresses}
            selectedAddressId={selectedShippingId}
            onSelectAddress={(id) => {
              setSelectedShippingId(id);
              if (useSameBilling) setSelectedBillingId(id);
            }}
            onAddNewAddress={(data) => createAddressMutation.mutate(data)}
          />

          {/* Billing address toggle */}
          <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-4">
            <label className="flex items-center space-x-2.5 cursor-pointer">
              <input
                type="checkbox"
                checked={useSameBilling}
                onChange={(e) => {
                  setUseSameBilling(e.target.checked);
                  if (e.target.checked) setSelectedBillingId(selectedShippingId);
                }}
                className="w-4 h-4 text-emerald-600 border-slate-300 rounded focus:ring-emerald-500"
              />
              <span className="text-xs font-semibold text-slate-800">
                Billing address matches shipping address
              </span>
            </label>

            {!useSameBilling && (
              <AddressSelector
                title="Billing Address"
                addresses={addresses}
                selectedAddressId={selectedBillingId}
                onSelectAddress={setSelectedBillingId}
                onAddNewAddress={(data) => createAddressMutation.mutate(data)}
              />
            )}
          </div>

          {/* 2. Coupon Application */}
          <CouponInput
            couponCode={couponCode}
            couponApplied={preview?.couponApplied}
            couponMessage={preview?.couponMessage}
            onApplyCoupon={(code) => setCouponCode(code)}
            onRemoveCoupon={() => setCouponCode('')}
            isLoading={previewLoading}
          />

          {/* 3. Payment Method Selection */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
            <h3 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">
              2. Select Payment Method
            </h3>

            <div className="space-y-3">
              {/* Option A: Razorpay */}
              <label
                className={`p-4 rounded-xl border-2 flex items-start space-x-3 cursor-pointer transition-all ${
                  paymentMethod === 'RAZORPAY'
                    ? 'border-emerald-600 bg-emerald-50/40 ring-2 ring-emerald-500/20'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <input
                  type="radio"
                  name="paymentMethod"
                  value="RAZORPAY"
                  checked={paymentMethod === 'RAZORPAY'}
                  onChange={() => setPaymentMethod('RAZORPAY')}
                  className="mt-1 text-emerald-600 focus:ring-emerald-500"
                />
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-900">
                      Razorpay Online Gateway
                    </span>
                    <CreditCard className="w-4 h-4 text-emerald-600" />
                  </div>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    Credit/Debit Cards, UPI, NetBanking, and Wallets with 256-bit encryption.
                  </p>
                </div>
              </label>

              {/* Option B: Cash on Delivery */}
              <label
                className={`p-4 rounded-xl border-2 flex items-start space-x-3 cursor-pointer transition-all ${
                  paymentMethod === 'COD'
                    ? 'border-emerald-600 bg-emerald-50/40 ring-2 ring-emerald-500/20'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <input
                  type="radio"
                  name="paymentMethod"
                  value="COD"
                  checked={paymentMethod === 'COD'}
                  onChange={() => setPaymentMethod('COD')}
                  className="mt-1 text-emerald-600 focus:ring-emerald-500"
                />
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-900">Cash On Delivery (COD)</span>
                    <Banknote className="w-4 h-4 text-emerald-600" />
                  </div>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    Pay in cash upon doorstep delivery. Payment remains <strong>PENDING</strong> until collected.
                  </p>
                </div>
              </label>

              {/* Option C: Mock Payment Sandbox (Dev / Test only) */}
              {isDevOrTest && (
                <label
                  className={`p-4 rounded-xl border-2 flex items-start space-x-3 cursor-pointer transition-all ${
                    paymentMethod === 'MOCK_TEST'
                      ? 'border-purple-600 bg-purple-50/40 ring-2 ring-purple-500/20'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="MOCK_TEST"
                    checked={paymentMethod === 'MOCK_TEST'}
                    onChange={() => setPaymentMethod('MOCK_TEST')}
                    className="mt-1 text-purple-600 focus:ring-purple-500"
                  />
                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-purple-900">
                        Sandbox Mock Simulator (Dev/Test Only)
                      </span>
                      <Shield className="w-4 h-4 text-purple-600" />
                    </div>
                    <p className="text-[11px] text-purple-700 mt-0.5">
                      Instantly simulates successful payment confirmation for test scenarios without live banking.
                    </p>
                  </div>
                </label>
              )}
            </div>
          </div>

          {/* Delivery Instructions / Notes */}
          <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-2">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
              Delivery Instructions / Notes (Optional)
            </label>
            <textarea
              rows={2}
              placeholder="e.g. Leave with security guard, call before arrival..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs resize-none"
            />
          </div>
        </div>

        {/* Right Column: Order Summary & Place Order */}
        <div className="space-y-4">
          <OrderSummary preview={preview} isLoading={previewLoading} />

          <button
            type="button"
            onClick={handlePlaceOrder}
            disabled={isSubmitting || previewLoading || !selectedShippingId || preview?.hasStockIssues}
            className={`w-full py-4 px-6 rounded-2xl text-sm font-extrabold text-white shadow-xl flex items-center justify-center space-x-2 transition-all ${
              isSubmitting || previewLoading || !selectedShippingId || preview?.hasStockIssues
                ? 'bg-slate-300 text-slate-500 cursor-not-allowed shadow-none'
                : 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/30'
            }`}
          >
            {isSubmitting ? (
              <>
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                <span>Processing Order...</span>
              </>
            ) : (
              <>
                <Shield className="w-4 h-4" />
                <span>Place Order & Pay</span>
              </>
            )}
          </button>

          <p className="text-[11px] text-center text-slate-400 leading-tight">
            Protected by idempotent transaction control. Duplicate submissions are automatically prevented.
          </p>
        </div>
      </div>
    </div>
  );
}
