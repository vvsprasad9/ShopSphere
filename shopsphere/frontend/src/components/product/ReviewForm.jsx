import React, { useState, useEffect } from 'react';
import StarRating from '../common/StarRating';
import { AlertCircle } from 'lucide-react';

export default function ReviewForm({
  initialData = null,
  isEditing = false,
  isLoading = false,
  onSubmit,
  onCancel,
}) {
  const [rating, setRating] = useState(initialData?.rating || 5);
  const [title, setTitle] = useState(initialData?.title || '');
  const [comment, setComment] = useState(initialData?.comment || '');
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (initialData) {
      setRating(initialData.rating || 5);
      setTitle(initialData.title || '');
      setComment(initialData.comment || '');
    }
  }, [initialData]);

  const handleSubmit = (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (rating < 1 || rating > 5) {
      setErrorMsg('Please select a star rating between 1 and 5');
      return;
    }

    if (!title.trim()) {
      setErrorMsg('Please enter a review headline / title');
      return;
    }

    if (!comment.trim() || comment.trim().length < 10) {
      setErrorMsg('Review comments must be at least 10 characters long');
      return;
    }

    onSubmit({
      rating,
      title: title.trim(),
      comment: comment.trim(),
    });
  };

  return (
    <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
      <h3 className="text-base font-bold text-slate-900">
        {isEditing ? 'Edit Your Review' : 'Write a Product Review'}
      </h3>

      {isEditing && (
        <div className="flex items-start p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-xs">
          <AlertCircle className="w-4 h-4 mr-2 shrink-0 mt-0.5 text-amber-600" />
          <span>
            Note: Updating your review will return it to <strong>PENDING</strong> moderation status. It will reappear publicly once approved by an administrator.
          </span>
        </div>
      )}

      {errorMsg && (
        <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs font-semibold">
          {errorMsg}
        </div>
      )}

      {/* Star Selector */}
      <div>
        <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
          Overall Rating
        </label>
        <div className="flex items-center space-x-2">
          <StarRating rating={rating} size="lg" interactive={true} onChange={setRating} />
          <span className="text-sm font-bold text-slate-700 ml-2">{rating} / 5</span>
        </div>
      </div>

      {/* Title */}
      <div>
        <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
          Review Headline
        </label>
        <input
          type="text"
          placeholder="e.g. Excellent build quality, highly recommend!"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={100}
          className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/30"
          required
        />
      </div>

      {/* Comment */}
      <div>
        <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
          Detailed Comments
        </label>
        <textarea
          rows={4}
          placeholder="What did you like or dislike about this product? How was the fit, material, or performance?"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          maxLength={1000}
          className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/30 resize-none"
          required
        />
        <div className="text-right text-[11px] text-slate-400 mt-0.5">
          {comment.length} / 1000 characters
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center justify-end space-x-3 pt-2">
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors"
          >
            Cancel
          </button>
        )}
        <button
          type="submit"
          disabled={isLoading}
          className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 transition-colors shadow-sm flex items-center space-x-2"
        >
          {isLoading && (
            <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          )}
          <span>{isEditing ? 'Update Review' : 'Submit Review'}</span>
        </button>
      </div>
    </form>
  );
}
