import React from 'react';
import { BadgeCheck, Edit2, Trash2 } from 'lucide-react';
import StarRating from '../common/StarRating';
import { formatDate } from '../../utils/formatters';
import { useAuthStore } from '../../store/useAuthStore';

export default function ReviewList({
  reviews = [],
  onEditReview,
  onDeleteReview,
}) {
  const { user } = useAuthStore();

  if (!reviews || reviews.length === 0) {
    return (
      <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8">
        <p className="text-sm font-semibold text-slate-600">No approved reviews yet for this product.</p>
        <p className="text-xs text-slate-400 mt-1">Be the first to share your thoughts!</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {reviews.map((review) => {
        const isAuthor = user && Number(user.id) === Number(review.userId);

        return (
          <div
            key={review.id}
            className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3"
          >
            <div className="flex items-start justify-between">
              <div>
                <div className="flex items-center space-x-2">
                  <span className="text-sm font-bold text-slate-900">
                    {review.reviewerName || 'Customer'}
                  </span>
                  {review.isVerifiedPurchase && (
                    <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                      <BadgeCheck className="w-3 h-3 text-emerald-600" />
                      <span>Verified Purchase</span>
                    </span>
                  )}
                </div>
                <div className="flex items-center space-x-2 mt-1">
                  <StarRating rating={review.rating} size="sm" />
                  <span className="text-xs text-slate-400">
                    {formatDate(review.createdAt)}
                  </span>
                </div>
              </div>

              {/* Author Actions */}
              {isAuthor && (
                <div className="flex items-center space-x-1">
                  {onEditReview && (
                    <button
                      type="button"
                      onClick={() => onEditReview(review)}
                      className="p-1.5 text-slate-400 hover:text-emerald-600 hover:bg-slate-50 rounded-lg transition-colors"
                      title="Edit Review"
                      aria-label="Edit Review"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                  )}
                  {onDeleteReview && (
                    <button
                      type="button"
                      onClick={() => onDeleteReview(review.id)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-slate-50 rounded-lg transition-colors"
                      title="Delete Review"
                      aria-label="Delete Review"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              )}
            </div>

            {review.title && (
              <h4 className="text-sm font-bold text-slate-800">{review.title}</h4>
            )}

            <p className="text-xs text-slate-600 leading-relaxed whitespace-pre-line">
              {review.comment}
            </p>
          </div>
        );
      })}
    </div>
  );
}
