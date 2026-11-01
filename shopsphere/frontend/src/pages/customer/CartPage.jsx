import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Trash2, ShoppingBag, ArrowRight, AlertTriangle, ShieldCheck, Plus, Minus } from 'lucide-react';
import { cartApi } from '../../api/cartApi';
import { formatCurrency } from '../../utils/formatters';
import { useToastStore } from '../../store/useToastStore';
import ConfirmModal from '../../components/common/ConfirmModal';

export default function CartPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { success, error } = useToastStore();
  const [confirmClearOpen, setConfirmClearOpen] = useState(false);

  // Fetch Authoritative Cart
  const { data: cart, isLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: () => cartApi.getCart(),
  });

  // Update item quantity mutation
  const updateQuantityMutation = useMutation({
    mutationFn: ({ itemId, quantity }) => cartApi.updateCartItem(itemId, quantity),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
    onError: (err) => {
      error(err.message || 'Could not update quantity', 'Error');
    },
  });

  // Remove single item mutation
  const removeItemMutation = useMutation({
    mutationFn: (itemId) => cartApi.removeCartItem(itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      success('Item removed from cart', 'Cart Updated');
    },
    onError: (err) => {
      error(err.message || 'Could not remove item', 'Error');
    },
  });

  // Clear entire cart mutation
  const clearCartMutation = useMutation({
    mutationFn: () => cartApi.clearCart(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      setConfirmClearOpen(false);
      success('Your shopping cart has been cleared', 'Cart Cleared');
    },
    onError: (err) => {
      error(err.message || 'Could not clear cart', 'Error');
    },
  });

  const items = cart?.items || [];
  const hasItems = items.length > 0;
  const hasStockIssues = items.some((item) => !item.inStock || item.quantity > item.stockAvailable || !item.active);

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto py-12 px-4 space-y-6 animate-pulse">
        <div className="h-6 bg-slate-200 rounded w-1/4"></div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-4">
            <div className="h-28 bg-slate-200 rounded-2xl"></div>
            <div className="h-28 bg-slate-200 rounded-2xl"></div>
          </div>
          <div className="h-64 bg-slate-200 rounded-2xl"></div>
        </div>
      </div>
    );
  }

  if (!hasItems) {
    return (
      <div className="max-w-2xl mx-auto py-20 px-4 text-center space-y-4">
        <div className="w-16 h-16 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
          <ShoppingBag className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-black text-slate-900">Your Cart is Empty</h2>
        <p className="text-xs sm:text-sm text-slate-500 max-w-sm mx-auto">
          Looks like you haven't added any products to your cart yet. Explore our curated collections!
        </p>
        <Link
          to="/catalog"
          className="inline-flex items-center px-6 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-md shadow-emerald-600/20 transition-all"
        >
          <span>Start Shopping</span>
          <ArrowRight className="w-4 h-4 ml-2" />
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Title */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900">Shopping Cart</h1>
          <p className="text-xs text-slate-500 mt-1">
            {cart.totalItems} {cart.totalItems === 1 ? 'item' : 'items'} in your bag
          </p>
        </div>
        <button
          type="button"
          onClick={() => setConfirmClearOpen(true)}
          className="text-xs font-semibold text-rose-600 hover:text-rose-700 flex items-center space-x-1"
        >
          <Trash2 className="w-3.5 h-3.5" />
          <span>Clear Cart</span>
        </button>
      </div>

      {/* Stock warning banner */}
      {hasStockIssues && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl flex items-center space-x-3 text-amber-800 text-xs font-medium">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />
          <span>
            Some items in your cart exceed available stock or have become inactive. Please adjust quantities before proceeding.
          </span>
        </div>
      )}

      {/* Grid: Items + Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Left: Cart Items */}
        <div className="lg:col-span-2 space-y-4">
          {items.map((item) => {
            const isOutOfStock = !item.inStock || item.stockAvailable <= 0;
            const exceedsStock = item.quantity > item.stockAvailable;

            return (
              <div
                key={item.id}
                className={`bg-white rounded-2xl border p-4 sm:p-5 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 transition-all ${
                  isOutOfStock || exceedsStock || !item.active
                    ? 'border-amber-300 bg-amber-50/20'
                    : 'border-slate-200'
                }`}
              >
                <div className="flex items-center space-x-4">
                  <div className="w-20 h-20 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-100">
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
                  <div>
                    <Link
                      to={`/product/${item.productSlug}`}
                      className="text-sm font-bold text-slate-900 hover:text-emerald-600 line-clamp-1"
                    >
                      {item.productName}
                    </Link>
                    {item.variantName && (
                      <span className="text-xs text-slate-500 block">
                        Variant: {item.variantName}
                      </span>
                    )}
                    <span className="text-xs text-slate-400 font-mono">
                      SKU: {item.productSku}
                    </span>

                    {/* Stock / Inactive warnings */}
                    {!item.active ? (
                      <span className="text-[11px] font-bold text-rose-600 block mt-1">
                        Product is no longer available
                      </span>
                    ) : isOutOfStock ? (
                      <span className="text-[11px] font-bold text-rose-600 block mt-1">
                        Currently Out of Stock
                      </span>
                    ) : exceedsStock ? (
                      <span className="text-[11px] font-bold text-amber-600 block mt-1">
                        Only {item.stockAvailable} available in stock
                      </span>
                    ) : null}

                    {item.priceChanged && (
                      <span className="text-[11px] text-blue-600 font-semibold block mt-1">
                        Price updated to latest catalog rate
                      </span>
                    )}
                  </div>
                </div>

                {/* Quantity + Price + Remove */}
                <div className="flex items-center justify-between sm:justify-end space-x-6 w-full sm:w-auto pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                  {/* Stepper */}
                  <div className="flex items-center border border-slate-200 rounded-xl bg-white shadow-sm overflow-hidden">
                    <button
                      type="button"
                      onClick={() =>
                        updateQuantityMutation.mutate({
                          itemId: item.id,
                          quantity: item.quantity - 1,
                        })
                      }
                      disabled={item.quantity <= 1 || updateQuantityMutation.isPending}
                      className="p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30"
                      aria-label="Decrease quantity"
                    >
                      <Minus className="w-3.5 h-3.5" />
                    </button>
                    <span className="w-8 text-center text-xs font-bold text-slate-800">
                      {item.quantity}
                    </span>
                    <button
                      type="button"
                      onClick={() =>
                        updateQuantityMutation.mutate({
                          itemId: item.id,
                          quantity: item.quantity + 1,
                        })
                      }
                      disabled={
                        item.quantity >= item.stockAvailable || updateQuantityMutation.isPending
                      }
                      className="p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30"
                      aria-label="Increase quantity"
                    >
                      <Plus className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Total Line Price */}
                  <div className="text-right">
                    <div className="text-sm font-black text-slate-900">
                      {formatCurrency(item.totalPrice)}
                    </div>
                    <div className="text-[10px] text-slate-400">
                      {formatCurrency(item.unitPrice)} each
                    </div>
                  </div>

                  {/* Remove Button */}
                  <button
                    type="button"
                    onClick={() => removeItemMutation.mutate(item.id)}
                    disabled={removeItemMutation.isPending}
                    className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                    title="Remove item"
                    aria-label="Remove item"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* Right: Authoritative Cart Summary */}
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-6">
          <h3 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">
            Summary
          </h3>

          <div className="space-y-3 text-xs">
            <div className="flex justify-between text-slate-600">
              <span>Cart Subtotal</span>
              <span className="font-semibold text-slate-900">
                {formatCurrency(cart.subtotal)}
              </span>
            </div>

            <div className="flex justify-between text-slate-600">
              <span>Estimated Shipping</span>
              <span className="font-semibold text-slate-900">
                {Number(cart.shippingFee) === 0 ? (
                  <span className="text-emerald-600 font-bold uppercase">Free</span>
                ) : (
                  formatCurrency(cart.shippingFee)
                )}
              </span>
            </div>

            {Number(cart.discountAmount) > 0 && (
              <div className="flex justify-between text-emerald-600">
                <span>Discount</span>
                <span className="font-bold">-{formatCurrency(cart.discountAmount)}</span>
              </div>
            )}

            <div className="border-t border-slate-200 pt-3 flex justify-between items-baseline">
              <span className="text-sm font-extrabold text-slate-900">Estimated Total</span>
              <span className="text-xl font-black text-slate-900">
                {formatCurrency(cart.totalAmount)}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => navigate('/checkout')}
            disabled={hasStockIssues}
            className={`w-full py-3.5 px-4 rounded-xl text-xs font-bold text-white transition-all shadow-md flex items-center justify-center space-x-2 ${
              hasStockIssues
                ? 'bg-slate-300 cursor-not-allowed shadow-none'
                : 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/20'
            }`}
          >
            <span>Proceed to Checkout</span>
            <ArrowRight className="w-4 h-4" />
          </button>

          <div className="flex items-center space-x-2 text-[11px] text-slate-500 bg-slate-50 p-2.5 rounded-xl">
            <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Taxes & final discounts calculated during checkout preview</span>
          </div>
        </div>
      </div>

      {/* Clear Cart Confirmation Dialog */}
      <ConfirmModal
        isOpen={confirmClearOpen}
        title="Clear Shopping Cart"
        message="Are you sure you want to remove all items from your shopping cart? This action cannot be undone."
        confirmText="Clear All Items"
        confirmVariant="danger"
        isLoading={clearCartMutation.isPending}
        onConfirm={() => clearCartMutation.mutate()}
        onClose={() => setConfirmClearOpen(false)}
      />
    </div>
  );
}
