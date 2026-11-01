import React from 'react';
import { formatCurrency } from '../../utils/formatters';

export default function VariantSelector({
  variants = [],
  selectedVariantId,
  onSelectVariant,
}) {
  if (!variants || variants.length === 0) return null;

  return (
    <div className="space-y-3">
      <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
        Choose Option / Variant
      </label>
      <div className="flex flex-wrap gap-2.5">
        {variants.map((v) => {
          const isSelected = selectedVariantId === v.id;
          const isOutOfStock = !v.inStock || v.stockQuantity <= 0;

          return (
            <button
              key={v.id}
              type="button"
              disabled={isOutOfStock}
              onClick={() => onSelectVariant(v)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold border transition-all text-left flex items-center space-x-2 ${
                isSelected
                  ? 'border-emerald-600 bg-emerald-50 text-emerald-800 ring-2 ring-emerald-500/20'
                  : isOutOfStock
                  ? 'border-slate-200 bg-slate-50 text-slate-400 line-through cursor-not-allowed'
                  : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
              }`}
            >
              <span>{v.variantName}</span>
              {v.priceAdjustment && Number(v.priceAdjustment) !== 0 && (
                <span className="text-[11px] text-slate-500">
                  ({Number(v.priceAdjustment) > 0 ? '+' : ''}
                  {formatCurrency(v.priceAdjustment)})
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
