import React from 'react';
import StarRating from '../common/StarRating';

export default function ReviewSummary({ summary }) {
  const averageRating = summary?.averageRating || 0;
  const totalReviews = summary?.totalReviews || 0;
  const ratingDistribution = summary?.ratingDistribution || {};
  const ratingPercentages = summary?.ratingPercentages || {};

  return (
    <div className="bg-slate-50 rounded-2xl border border-slate-200 p-6 flex flex-col md:flex-row items-center md:items-start gap-8">
      {/* Overall Score */}
      <div className="flex flex-col items-center justify-center text-center md:border-r md:border-slate-200 md:pr-8 shrink-0">
        <span className="text-5xl font-black text-slate-900 leading-none">
          {averageRating ? averageRating.toFixed(1) : '0.0'}
        </span>
        <div className="mt-3">
          <StarRating rating={averageRating} size="md" />
        </div>
        <p className="text-xs text-slate-500 mt-2 font-medium">
          Based on {totalReviews} {totalReviews === 1 ? 'review' : 'reviews'}
        </p>
      </div>

      {/* Distribution Bars */}
      <div className="w-full space-y-2 flex-1">
        {[5, 4, 3, 2, 1].map((stars) => {
          const count = ratingDistribution[stars] || 0;
          const percentage = ratingPercentages[stars] || (totalReviews > 0 ? (count / totalReviews) * 100 : 0);

          return (
            <div key={stars} className="flex items-center space-x-3 text-xs">
              <span className="w-12 text-slate-600 font-semibold">{stars} stars</span>
              <div className="flex-1 h-2.5 bg-slate-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-amber-400 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(100, Math.max(0, percentage))}%` }}
                />
              </div>
              <span className="w-10 text-right text-slate-500 font-mono text-[11px]">
                {count}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
