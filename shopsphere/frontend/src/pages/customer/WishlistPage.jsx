import React from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Heart, ShoppingBag, Trash2, ArrowRight } from 'lucide-react';
import { wishlistApi } from '../../api/wishlistApi';
import { cartApi } from '../../api/cartApi';
import { formatCurrency } from '../../utils/formatters';
import { useToastStore } from '../../store/useToastStore';
import { StockBadge } from '../../components/common/Badge';

export default function WishlistPage() {
  const queryClient = useQueryClient();
  const { success, error } = useToastStore();

  const { data: wishlist, isLoading } = useQuery({
    queryKey: ['wishlist'],
    queryFn: () => wishlistApi.getWishlist(),
  });

  // Remove single item from wishlist mutation
  const removeWishlistMutation = useMutation({
    mutationFn: (productId) => wishlistApi.removeFromWishlist(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
      success('Item removed from wishlist', 'Wishlist Updated');
    },
    onError: (err) => {
      error(err.message || 'Could not remove item from wishlist', 'Error');
    },
  });

  // Move item to cart mutation (handles partial failure gracefully)
  const moveToCartMutation = useMutation({
    mutationFn: async (product) => {
      // Step 1: Add to cart
      await cartApi.addToCart({ productId: product.id, quantity: 1 });
      // Step 2: Remove from wishlist only after cart addition succeeds
      await wishlistApi.removeFromWishlist(product.id);
      return product;
    },
    onSuccess: (product) => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
      success(`Moved "${product.name}" to your shopping cart!`, 'Cart Updated');
    },
    onError: (err) => {
      // If adding to cart failed, item stays in wishlist safely
      error(err.message || 'Could not move item to cart', 'Move Failed');
    },
  });

  const items = wishlist?.items || [];
  const hasItems = items.length > 0;

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto py-12 px-4 space-y-4 animate-pulse">
        <div className="h-6 bg-slate-200 rounded w-1/4"></div>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-64 bg-slate-200 rounded-2xl"></div>
          ))}
        </div>
      </div>
    );
  }

  if (!hasItems) {
    return (
      <div className="max-w-2xl mx-auto py-20 px-4 text-center space-y-4">
        <div className="w-16 h-16 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
          <Heart className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-black text-slate-900">Your Wishlist is Empty</h2>
        <p className="text-xs sm:text-sm text-slate-500 max-w-sm mx-auto">
          Explore products you love and save them here for later purchase or price tracking.
        </p>
        <Link
          to="/catalog"
          className="inline-flex items-center px-6 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-md shadow-emerald-600/20 transition-all"
        >
          <span>Explore Catalog</span>
          <ArrowRight className="w-4 h-4 ml-2" />
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-16">
      <div>
        <h1 className="text-2xl font-black text-slate-900">My Wishlist</h1>
        <p className="text-xs text-slate-500 mt-1">
          {items.length} saved {items.length === 1 ? 'item' : 'items'}
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
        {items.map(({ id: itemId, product }) => {
          const effectivePrice = product.discountPrice || product.price;

          return (
            <div
              key={itemId}
              className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm hover:shadow-md transition-all flex flex-col justify-between"
            >
              <div>
                <div className="relative aspect-square bg-slate-100 overflow-hidden">
                  <Link to={`/product/${product.slug}`}>
                    {product.primaryImageUrl ? (
                      <img
                        src={product.primaryImageUrl}
                        alt={product.name}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center font-bold text-3xl text-slate-300">
                        S
                      </div>
                    )}
                  </Link>
                  <div className="absolute top-3 left-3">
                    <StockBadge quantity={product.stockQuantity} inStock={product.inStock} />
                  </div>
                  <button
                    type="button"
                    onClick={() => removeWishlistMutation.mutate(product.id)}
                    disabled={removeWishlistMutation.isPending}
                    className="absolute top-3 right-3 p-2 rounded-xl bg-white/80 hover:bg-white text-slate-400 hover:text-rose-600 shadow-sm transition-colors"
                    title="Remove from wishlist"
                    aria-label="Remove from wishlist"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="p-4 space-y-1">
                  {product.brand && (
                    <span className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider block">
                      {product.brand}
                    </span>
                  )}
                  <Link
                    to={`/product/${product.slug}`}
                    className="text-xs font-bold text-slate-900 hover:text-emerald-600 line-clamp-2"
                  >
                    {product.name}
                  </Link>
                  <div className="text-sm font-extrabold text-slate-900 pt-1">
                    {formatCurrency(effectivePrice)}
                  </div>
                </div>
              </div>

              {/* Move to Cart Action */}
              <div className="p-4 pt-0">
                <button
                  type="button"
                  onClick={() => moveToCartMutation.mutate(product)}
                  disabled={!product.inStock || moveToCartMutation.isPending}
                  className={`w-full py-2.5 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center space-x-2 ${
                    product.inStock
                      ? 'bg-slate-900 hover:bg-slate-800 text-white'
                      : 'bg-slate-100 text-slate-400 cursor-not-allowed'
                  }`}
                >
                  {moveToCartMutation.isPending ? (
                    <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <ShoppingBag className="w-3.5 h-3.5" />
                  )}
                  <span>{product.inStock ? 'Move to Cart' : 'Out of Stock'}</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
