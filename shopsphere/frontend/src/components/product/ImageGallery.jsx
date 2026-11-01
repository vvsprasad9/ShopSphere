import React, { useState } from 'react';

export default function ImageGallery({ images = [], productName = 'Product' }) {
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);

  // If no images provided, show placeholder
  if (!images || images.length === 0) {
    return (
      <div className="w-full aspect-square bg-slate-100 rounded-3xl flex items-center justify-center text-slate-300 font-extrabold text-6xl border border-slate-200">
        S
      </div>
    );
  }

  const activeImage = images[selectedImageIndex] || images[0];

  return (
    <div className="space-y-4">
      {/* Primary Display */}
      <div className="relative aspect-square bg-slate-100 rounded-3xl overflow-hidden border border-slate-200 shadow-sm">
        <img
          src={activeImage.imageUrl}
          alt={productName}
          className="w-full h-full object-cover transition-all duration-300"
        />
        {activeImage.isPrimary && (
          <span className="absolute top-4 left-4 px-2.5 py-1 rounded-full bg-slate-900/70 backdrop-blur-sm text-white text-[10px] font-bold tracking-wide">
            PRIMARY
          </span>
        )}
      </div>

      {/* Thumbnails */}
      {images.length > 1 && (
        <div className="flex items-center space-x-3 overflow-x-auto pb-2">
          {images.map((img, idx) => (
            <button
              key={img.id || idx}
              type="button"
              onClick={() => setSelectedImageIndex(idx)}
              className={`relative w-20 h-20 rounded-xl overflow-hidden border-2 shrink-0 transition-all ${
                selectedImageIndex === idx
                  ? 'border-emerald-600 ring-2 ring-emerald-500/20'
                  : 'border-slate-200 hover:border-slate-300'
              }`}
              aria-label={`Select image ${idx + 1}`}
            >
              <img
                src={img.imageUrl}
                alt={`${productName} thumbnail ${idx + 1}`}
                className="w-full h-full object-cover"
              />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
