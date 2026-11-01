import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShoppingBag, Heart, ShieldCheck, Truck, RotateCcw, AlertCircle, Plus, Minus } from 'lucide-react';
import { productApi } from '../../api/productApi';
import { reviewApi } from '../../api/reviewApi';
import { cartApi } from '../../api/cartApi';
import { wishlistApi } from '../../api/wishlistApi';
import { formatCurrency } from '../../utils/formatters';
import { useAuthStore } from '../../store/useAuthStore';
import { useToastStore } from '../../store/useToastStore';
import ImageGallery from '../../components/product/ImageGallery';
import VariantSelector from '../../components/product/VariantSelector';
import ReviewSummary from '../../components/product/ReviewSummary';
import ReviewList from '../../components/product/ReviewList';
import ReviewForm from '../../components/product/ReviewForm';
import ProductCard from '../../components/storefront/ProductCard';
import StarRating from '../../components/common/StarRating';
import { StockBadge } from '../../components/common/Badge';
import { DetailSkeleton } from '../../components/common/LoadingSkeleton';

export default function ProductDetailPage() {
  const { slug, id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuthStore();
  const { success, error } = useToastStore();

  const [selectedVariant, setSelectedVariant] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [editingReview, setEditingReview] = useState(null);
  const [showReviewForm, setShowReviewForm] = useState(false);

  // Fetch product details
  const { data: product, isLoading: productLoading, error: productError } = useQuery({
    queryKey: ['productDetail', slug || id],
    queryFn: () => (slug ? productApi.getProductBySlug(slug) : productApi.getProductById(id)),
  });

  const productId = product?.id;

  // Check wishlist status
  const { data: isWishlistedData } = useQuery({
    queryKey: ['isWishlisted', productId],
    queryFn: () => wishlistApi.isWishlisted(productId),
    enabled: !!productId && isAuthenticated,
  });

  // Fetch rating breakdown summary
  const { data: ratingSummary } = useQuery({
    queryKey: ['productRatingSummary', productId],
    queryFn: () => reviewApi.getProductRatingSummary(productId),
    enabled: !!productId,
  });

  // Fetch approved reviews
  const { data: reviewsData } = useQuery({
    queryKey: ['productReviews', productId],
    queryFn: () => reviewApi.getProductReviews(productId),
    enabled: !!productId,
  });

  // Fetch related products
  const { data: relatedProducts } = useQuery({
    queryKey: ['relatedProducts', productId],
    queryFn: () => productApi.getRelatedProducts(productId, 4),
    enabled: !!productId,
  });

  // Add to cart mutation
  const addToCartMutation = useMutation({
    mutationFn: () =>
      cartApi.addToCart({
        productId,
        variantId: selectedVariant?.id || null,
        quantity,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      success(`Added ${quantity} item(s) to your cart!`, 'Added to Cart');
    },
    onError: (err) => {
      error(err.message || 'Could not add product to cart', 'Error');
    },
  });

  // Wishlist toggle mutation
  const toggleWishlistMutation = useMutation({
    mutationFn: async () => {
      if (isWishlistedData) {
        return wishlistApi.removeFromWishlist(productId);
      } else {
        return wishlistApi.addToWishlist(productId);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['isWishlisted', productId] });
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
      success(
        isWishlistedData ? 'Removed from wishlist' : 'Added to wishlist',
        'Wishlist'
      );
    },
    onError: (err) => {
      error(err.message || 'Could not update wishlist', 'Error');
    },
  });

  // Review create mutation
  const createReviewMutation = useMutation({
    mutationFn: (reviewData) => reviewApi.createReview(productId, reviewData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productReviews', productId] });
      queryClient.invalidateQueries({ queryKey: ['productRatingSummary', productId] });
      success('Review submitted successfully! It is now pending moderation.', 'Review Submitted');
      setShowReviewForm(false);
    },
    onError: (err) => {
      error(err.message || 'Could not submit review', 'Submission Failed');
    },
  });

  // Review update mutation
  const updateReviewMutation = useMutation({
    mutationFn: (reviewData) => reviewApi.updateReview(editingReview.id, reviewData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productReviews', productId] });
      queryClient.invalidateQueries({ queryKey: ['productRatingSummary', productId] });
      success('Review updated and resubmitted for admin moderation.', 'Review Updated');
      setEditingReview(null);
      setShowReviewForm(false);
    },
    onError: (err) => {
      error(err.message || 'Could not update review', 'Update Failed');
    },
  });

  // Review delete mutation
  const deleteReviewMutation = useMutation({
    mutationFn: (reviewId) => reviewApi.deleteReview(reviewId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productReviews', productId] });
      queryClient.invalidateQueries({ queryKey: ['productRatingSummary', productId] });
      success('Review removed successfully', 'Deleted');
    },
    onError: (err) => {
      error(err.message || 'Could not delete review', 'Delete Failed');
    },
  });

  if (productLoading) {
    return <DetailSkeleton />;
  }

  if (productError || !product) {
    return (
      <div className="max-w-2xl mx-auto py-16 text-center space-y-4">
        <div className="w-16 h-16 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-slate-900">Product Not Found</h2>
        <p className="text-xs text-slate-500">
          The requested product may have been archived or does not exist.
        </p>
        <Link
          to="/catalog"
          className="inline-block px-5 py-2.5 rounded-xl bg-slate-900 text-white text-xs font-bold"
        >
          Return to Catalog
        </Link>
      </div>
    );
  }

  // Authoritative prices and stock
  const basePrice = Number(product.discountPrice || product.price);
  const adjustment = selectedVariant?.priceAdjustment ? Number(selectedVariant.priceAdjustment) : 0;
  const effectivePrice = basePrice + adjustment;
  const availableStock = selectedVariant ? selectedVariant.stockQuantity : product.stockQuantity;
  const inStock = selectedVariant ? (selectedVariant.inStock && selectedVariant.stockQuantity > 0) : product.inStock;

  const handleAddToCart = () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    addToCartMutation.mutate();
  };

  const handleToggleWishlist = () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    toggleWishlistMutation.mutate();
  };

  const handleReviewSubmit = (reviewData) => {
    if (editingReview) {
      updateReviewMutation.mutate(reviewData);
    } else {
      createReviewMutation.mutate(reviewData);
    }
  };

  return (
    <div className="space-y-16 pb-16">
      {/* Breadcrumb */}
      <nav className="text-xs text-slate-500 flex items-center space-x-2">
        <Link to="/" className="hover:text-emerald-600">Home</Link>
        <span>/</span>
        <Link to="/catalog" className="hover:text-emerald-600">Catalog</Link>
        {product.categorySlug && (
          <>
            <span>/</span>
            <Link to={`/catalog?categorySlug=${product.categorySlug}`} className="hover:text-emerald-600">
              {product.categoryName}
            </Link>
          </>
        )}
        <span>/</span>
        <span className="text-slate-800 font-semibold truncate max-w-xs">{product.name}</span>
      </nav>

      {/* Main Product Showcase */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
        {/* Gallery */}
        <div>
          <ImageGallery images={product.images} productName={product.name} />
        </div>

        {/* Product Details & Purchase Form */}
        <div className="space-y-6">
          <div>
            {product.brand && (
              <span className="text-xs font-bold uppercase tracking-wider text-emerald-600">
                {product.brand}
              </span>
            )}
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 mt-1 leading-snug">
              {product.name}
            </h1>
            <div className="flex items-center space-x-4 mt-2.5">
              <div className="flex items-center space-x-1">
                <StarRating rating={product.averageRating || 0} size="sm" />
                <span className="text-xs font-bold text-slate-800 ml-1">
                  {product.averageRating ? product.averageRating.toFixed(1) : 'No reviews'}
                </span>
              </div>
              <span className="text-xs text-slate-400">•</span>
              <span className="text-xs text-slate-500">
                {product.reviewCount || 0} {product.reviewCount === 1 ? 'review' : 'reviews'}
              </span>
              <span className="text-xs text-slate-400">•</span>
              <span className="text-xs text-slate-500 font-mono">SKU: {selectedVariant?.sku || product.sku}</span>
            </div>
          </div>

          {/* Pricing & Stock */}
          <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 flex items-center justify-between">
            <div>
              <div className="text-2xl font-black text-slate-900">
                {formatCurrency(effectivePrice)}
              </div>
              {product.discountPrice && (
                <div className="text-xs text-slate-400 line-through">
                  Original: {formatCurrency(Number(product.price) + adjustment)}
                </div>
              )}
            </div>
            <div>
              <StockBadge quantity={availableStock} inStock={inStock} />
            </div>
          </div>

          {/* Short Description */}
          {product.shortDescription && (
            <p className="text-sm text-slate-600 leading-relaxed">
              {product.shortDescription}
            </p>
          )}

          {/* Variant Selector */}
          {product.variants && product.variants.length > 0 && (
            <VariantSelector
              variants={product.variants}
              selectedVariantId={selectedVariant?.id}
              onSelectVariant={(variant) => {
                setSelectedVariant(variant);
                setQuantity(1);
              }}
            />
          )}

          {/* Quantity Selector & Action Buttons */}
          <div className="space-y-4 pt-2">
            <div className="flex items-center space-x-4">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Quantity
              </span>
              <div className="flex items-center border border-slate-200 rounded-xl bg-white shadow-sm overflow-hidden">
                <button
                  type="button"
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  disabled={quantity <= 1 || !inStock}
                  className="p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-40"
                  aria-label="Decrease quantity"
                >
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <span className="w-10 text-center text-xs font-bold text-slate-800">
                  {quantity}
                </span>
                <button
                  type="button"
                  onClick={() => setQuantity(Math.min(availableStock, quantity + 1))}
                  disabled={quantity >= availableStock || !inStock}
                  className="p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-40"
                  aria-label="Increase quantity"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
              <span className="text-xs text-slate-400">
                ({availableStock} available)
              </span>
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={handleAddToCart}
                disabled={!inStock || addToCartMutation.isPending}
                className={`flex-1 py-3.5 px-6 rounded-2xl font-bold text-sm flex items-center justify-center space-x-2 shadow-lg transition-all ${
                  inStock
                    ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-600/30'
                    : 'bg-slate-200 text-slate-400 cursor-not-allowed shadow-none'
                }`}
              >
                {addToCartMutation.isPending ? (
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <ShoppingBag className="w-4 h-4" />
                )}
                <span>{inStock ? 'Add to Shopping Cart' : 'Out of Stock'}</span>
              </button>

              <button
                type="button"
                onClick={handleToggleWishlist}
                disabled={toggleWishlistMutation.isPending}
                className={`p-3.5 rounded-2xl border transition-all ${
                  isWishlistedData
                    ? 'bg-rose-50 border-rose-200 text-rose-600'
                    : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
                title={isWishlistedData ? 'Remove from wishlist' : 'Save to wishlist'}
                aria-label="Wishlist"
              >
                <Heart className={`w-5 h-5 ${isWishlistedData ? 'fill-rose-600' : ''}`} />
              </button>
            </div>
          </div>

          {/* Guarantees */}
          <div className="grid grid-cols-3 gap-3 pt-4 border-t border-slate-200 text-center">
            <div className="p-3 bg-slate-50 rounded-xl">
              <Truck className="w-4 h-4 text-emerald-600 mx-auto mb-1" />
              <span className="text-[11px] font-bold text-slate-700 block">Fast Shipping</span>
              <span className="text-[10px] text-slate-400">Express delivery</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl">
              <ShieldCheck className="w-4 h-4 text-emerald-600 mx-auto mb-1" />
              <span className="text-[11px] font-bold text-slate-700 block">Genuine Item</span>
              <span className="text-[10px] text-slate-400">100% verified</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl">
              <RotateCcw className="w-4 h-4 text-emerald-600 mx-auto mb-1" />
              <span className="text-[11px] font-bold text-slate-700 block">Easy Returns</span>
              <span className="text-[10px] text-slate-400">Hassle free policy</span>
            </div>
          </div>
        </div>
      </div>

      {/* Description Section */}
      <div className="bg-white rounded-3xl border border-slate-200 p-8 shadow-sm space-y-4">
        <h2 className="text-lg font-bold text-slate-900 border-b border-slate-100 pb-3">
          Product Description & Specifications
        </h2>
        <div className="text-sm text-slate-600 leading-relaxed whitespace-pre-line">
          {product.description || 'No detailed description available.'}
        </div>
      </div>

      {/* Reviews & Ratings Section */}
      <section className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">
              Customer Reviews & Ratings
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Real feedback from verified purchasers
            </p>
          </div>
          {isAuthenticated && !showReviewForm && (
            <button
              type="button"
              onClick={() => {
                setEditingReview(null);
                setShowReviewForm(true);
              }}
              className="px-4 py-2.5 rounded-xl bg-slate-900 text-white text-xs font-bold hover:bg-slate-800 transition-colors self-start sm:self-center"
            >
              Write a Review
            </button>
          )}
        </div>

        {/* Rating Breakdown Summary */}
        <ReviewSummary summary={ratingSummary} />

        {/* Review Form (if open) */}
        {showReviewForm && (
          <ReviewForm
            initialData={editingReview}
            isEditing={!!editingReview}
            isLoading={createReviewMutation.isPending || updateReviewMutation.isPending}
            onSubmit={handleReviewSubmit}
            onCancel={() => {
              setShowReviewForm(false);
              setEditingReview(null);
            }}
          />
        )}

        {/* Reviews List */}
        <ReviewList
          reviews={reviewsData?.content || []}
          onEditReview={(review) => {
            setEditingReview(review);
            setShowReviewForm(true);
            window.scrollTo({ top: 600, behavior: 'smooth' });
          }}
          onDeleteReview={(reviewId) => deleteReviewMutation.mutate(reviewId)}
        />
      </section>

      {/* Related Products */}
      {relatedProducts && relatedProducts.length > 0 && (
        <section className="space-y-6">
          <h2 className="text-xl font-extrabold text-slate-900">Related Products</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
            {relatedProducts.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
