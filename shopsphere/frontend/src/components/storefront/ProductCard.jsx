import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Heart, ShoppingBag, Star } from 'lucide-react';
import { formatCurrency } from '../../utils/formatters';
import { cartApi } from '../../api/cartApi';
import { wishlistApi } from '../../api/wishlistApi';
import { useAuthStore } from '../../store/useAuthStore';
import { useToastStore } from '../../store/useToastStore';
import { StockBadge } from '../common/Badge';

export default function ProductCard({ product, isWishlisted = false }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuthStore();
  const { success, error } = useToastStore();
  const [wishlistActive, setWishlistActive] = useState(isWishlisted);

  const productUrl = product.slug ? `/product/${product.slug}` : `/product/id/${product.id}`;
  const effectivePrice = product.discountPrice || product.price;
  const hasDiscount = product.discountPrice && Number(product.discountPrice) < Number(product.price);

  // Quick Add to Cart Mutation
  const addToCartMutation = useMutation({
    mutationFn: () => cartApi.addToCart({ productId: product.id, quantity: 1 }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      success(`Added "${product.name}" to cart`, 'Cart Updated');
    },
    onError: (err) => {
      error(err.message || 'Could not add product to cart', 'Error');
    },
  });

  // Wishlist toggle Mutation
  const toggleWishlistMutation = useMutation({
    mutationFn: async () => {
      if (wishlistActive) {
        return wishlistApi.removeFromWishlist(product.id);
      } else {
        return wishlistApi.addToWishlist(product.id);
      }
    },
    onSuccess: () => {
      setWishlistActive(!wishlistActive);
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
      success(
        wishlistActive
          ? `Removed "${product.name}" from wishlist`
          : `Added "${product.name}" to wishlist`,
        'Wishlist'
      );
    },
    onError: (err) => {
      error(err.message || 'Could not update wishlist', 'Error');
    },
  });

  const handleAddToCart = (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    addToCartMutation.mutate();
  };

  const handleToggleWishlist = (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    toggleWishlistMutation.mutate();
  };

  return (
    <div className="group bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm hover:shadow-lg transition-all flex flex-col justify-between">
      <div>
        {/* Product Image & Badges */}
        <div className="relative aspect-square bg-slate-100 overflow-hidden">
          <Link to={productUrl}>
            {product.primaryImageUrl ? (
              <img
                src={product.primaryImageUrl}
                alt={product.name}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                loading="lazy"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-slate-300 font-bold text-4xl">
                S
              </div>
            )}
          </Link>

          {/* Top badges */}
          <div className="absolute top-3 left-3 flex flex-col gap-1 z-10">
            {hasDiscount && (
              <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-rose-500 text-white shadow-sm">
                SALE
              </span>
            )}
            <StockBadge quantity={product.stockQuantity} inStock={product.inStock} />
          </div>

          {/* Wishlist Button */}
          <button
            type="button"
            onClick={handleToggleWishlist}
            disabled={toggleWishlistMutation.isPending}
            className={`absolute top-3 right-3 p-2 rounded-xl backdrop-blur-md transition-colors z-10 ${
              wishlistActive
                ? 'bg-rose-50 text-rose-500 hover:bg-rose-100'
                : 'bg-white/80 text-slate-600 hover:text-rose-500 hover:bg-white'
            }`}
            aria-label="Wishlist toggle"
          >
            <Heart className={`w-4 h-4 ${wishlistActive ? 'fill-rose-500' : ''}`} />
          </button>
        </div>

        {/* Content */}
        <div className="p-4">
          {product.brand && (
            <p className="text-xs font-semibold text-emerald-600 uppercase tracking-wider mb-1">
              {product.brand}
            </p>
          )}

          <Link to={productUrl} className="block">
            <h3 className="text-sm font-bold text-slate-900 group-hover:text-emerald-600 transition-colors line-clamp-2 leading-snug">
              {product.name}
            </h3>
          </Link>

          {/* Ratings */}
          <div className="flex items-center space-x-1.5 mt-2">
            <div className="flex items-center text-amber-400">
              <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
              <span className="text-xs font-bold text-slate-700 ml-1">
                {product.averageRating ? product.averageRating.toFixed(1) : 'New'}
              </span>
            </div>
            {product.reviewCount > 0 && (
              <span className="text-xs text-slate-400">({product.reviewCount})</span>
            )}
          </div>
        </div>
      </div>

      {/* Price & Action */}
      <div className="p-4 pt-0 border-t border-slate-100 mt-2 flex items-center justify-between">
        <div>
          <div className="text-base font-extrabold text-slate-900">
            {formatCurrency(effectivePrice)}
          </div>
          {hasDiscount && (
            <div className="text-xs text-slate-400 line-through">
              {formatCurrency(product.price)}
            </div>
          )}
        </div>

        <button
          type="button"
          onClick={handleAddToCart}
          disabled={!product.inStock || addToCartMutation.isPending}
          className={`p-2.5 rounded-xl transition-all flex items-center justify-center ${
            product.inStock
              ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm shadow-emerald-600/20'
              : 'bg-slate-100 text-slate-400 cursor-not-allowed'
          }`}
          title={product.inStock ? 'Add to cart' : 'Out of Stock'}
          aria-label="Add to Cart"
        >
          {addToCartMutation.isPending ? (
            <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <ShoppingBag className="w-4 h-4" />
          )}
        </button>
      </div>
    </div>
  );
}
