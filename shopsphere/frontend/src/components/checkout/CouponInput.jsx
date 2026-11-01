import React, { useState } from 'react';
import { Tag, Check, X } from 'lucide-react';

export default function CouponInput({
  couponCode = '',
  couponApplied = false,
  couponMessage = '',
  onApplyCoupon,
  onRemoveCoupon,
  isLoading = false,
}) {
  const [inputVal, setInputVal] = useState(couponCode);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (inputVal.trim()) {
      onApplyCoupon(inputVal.trim().toUpperCase());
    }
  };

  const handleRemove = () => {
    setInputVal('');
    onRemoveCoupon();
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3">
      <div className="flex items-center space-x-2 text-slate-900 font-bold text-sm">
        <Tag className="w-4 h-4 text-emerald-600" />
        <span>Discount Coupon</span>
      </div>

      {couponApplied ? (
        <div className="flex items-center justify-between p-3 bg-emerald-50 border border-emerald-200 rounded-xl">
          <div className="flex items-center space-x-2">
            <div className="w-5 h-5 rounded-full bg-emerald-600 text-white flex items-center justify-center">
              <Check className="w-3.5 h-3.5 stroke-[3]" />
            </div>
            <div>
              <span className="text-xs font-bold text-emerald-800 tracking-wider">
                {couponCode}
              </span>
              {couponMessage && (
                <p className="text-[11px] text-emerald-700">{couponMessage}</p>
              )}
            </div>
          </div>
          <button
            type="button"
            onClick={handleRemove}
            className="text-xs font-semibold text-rose-600 hover:text-rose-700 p-1"
          >
            Remove
          </button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="flex gap-2">
          <input
            type="text"
            placeholder="Enter coupon code (e.g. WELCOME10)"
            value={inputVal}
            onChange={(e) => setInputVal(e.target.value.toUpperCase())}
            disabled={isLoading}
            className="flex-1 p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-500/30 uppercase tracking-wider"
          />
          <button
            type="submit"
            disabled={!inputVal.trim() || isLoading}
            className="px-4 py-2.5 rounded-xl text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 disabled:opacity-50 transition-colors shrink-0"
          >
            {isLoading ? 'Applying...' : 'Apply'}
          </button>
        </form>
      )}

      {!couponApplied && couponMessage && (
        <p className="text-xs text-rose-600">{couponMessage}</p>
      )}
    </div>
  );
}
