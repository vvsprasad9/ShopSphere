import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  MessageSquare,
  Filter,
  CheckCircle2,
  XCircle,
  EyeOff,
  Trash2,
  BadgeCheck,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { reviewApi } from '../../api/reviewApi';
import { ReviewBadge } from '../../components/common/Badge';
import StarRating from '../../components/common/StarRating';
import { formatDate } from '../../utils/formatters';
import { useToastStore } from '../../store/useToastStore';

export default function AdminReviewsPage() {
  const queryClient = useQueryClient();
  const { success, error } = useToastStore();

  const [page, setPage] = useState(0);
  const [selectedStatus, setSelectedStatus] = useState('PENDING'); // Default to pending queue
  const [moderateModalReview, setModerateModalReview] = useState(null);
  const [actionType, setActionType] = useState('APPROVED'); // APPROVED, REJECTED, HIDDEN
  const [moderationNotes, setModerationNotes] = useState('');

  const { data: reviewsData, isLoading } = useQuery({
    queryKey: ['adminReviews', page, selectedStatus],
    queryFn: () =>
      reviewApi.getAdminReviews({
        status: selectedStatus || undefined,
        page,
        size: 10,
      }),
  });

  const reviews = reviewsData?.content || [];
  const totalPages = reviewsData?.totalPages || 0;
  const totalElements = reviewsData?.totalElements || 0;

  // Moderate review mutation
  const moderateMutation = useMutation({
    mutationFn: ({ id, data }) => reviewApi.moderateReview(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminReviews'] });
      setModerateModalReview(null);
      setModerationNotes('');
      success(`Review status updated to ${actionType}`, 'Moderated');
    },
    onError: (err) => {
      error(err.message || 'Could not moderate review', 'Error');
    },
  });

  // Delete review mutation
  const deleteMutation = useMutation({
    mutationFn: (id) => reviewApi.deleteReview(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminReviews'] });
      success('Review permanently deleted', 'Deleted');
    },
    onError: (err) => {
      error(err.message || 'Could not delete review', 'Error');
    },
  });

  const handleOpenModeration = (review, targetStatus) => {
    setModerateModalReview(review);
    setActionType(targetStatus);
    setModerationNotes(review.moderationNotes || '');
  };

  const handleModerationSubmit = (e) => {
    e.preventDefault();
    moderateMutation.mutate({
      id: moderateModalReview.id,
      data: {
        status: actionType,
        moderationNotes: moderationNotes.trim() || `Marked ${actionType} by admin`,
      },
    });
  };

  return (
    <div className="space-y-6 pb-16">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900">Review Moderation Queue</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {totalElements} reviews in current view
          </p>
        </div>

        {/* Filter Dropdown */}
        <div className="flex items-center space-x-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value);
              setPage(0);
            }}
            className="p-2.5 bg-white border border-slate-200 rounded-xl text-xs font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-500/30"
          >
            <option value="">All Review Statuses</option>
            <option value="PENDING">Pending Moderation</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="HIDDEN">Hidden</option>
          </select>
        </div>
      </div>

      {/* Reviews Table */}
      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[10px]">
                <th className="py-3 px-4">Product</th>
                <th className="py-3 px-4">Reviewer</th>
                <th className="py-3 px-4">Rating</th>
                <th className="py-3 px-4">Headline & Content</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Moderation Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    Loading reviews...
                  </td>
                </tr>
              ) : reviews.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-500">
                    No reviews in this moderation queue.
                  </td>
                </tr>
              ) : (
                reviews.map((rev) => (
                  <tr key={rev.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-3 px-4 font-bold text-slate-900 max-w-[150px] truncate">
                      {rev.productName || `Product #${rev.productId}`}
                    </td>

                    <td className="py-3 px-4">
                      <div className="flex items-center space-x-1.5">
                        <span className="font-semibold text-slate-800">{rev.reviewerName}</span>
                        {rev.isVerifiedPurchase && (
                          <span title="Verified Purchaser">
                            <BadgeCheck className="w-3.5 h-3.5 text-emerald-600" />
                          </span>
                        )}
                      </div>
                      <span className="text-[10px] text-slate-400 block">{formatDate(rev.createdAt)}</span>
                    </td>

                    <td className="py-3 px-4">
                      <div className="flex items-center space-x-1">
                        <StarRating rating={rev.rating} size="sm" />
                        <span className="font-bold text-slate-700 ml-1">({rev.rating})</span>
                      </div>
                    </td>

                    <td className="py-3 px-4 max-w-sm">
                      {rev.title && (
                        <span className="font-bold text-slate-800 block truncate mb-0.5">
                          {rev.title}
                        </span>
                      )}
                      <p className="text-slate-600 line-clamp-2 leading-relaxed">{rev.comment}</p>
                      {rev.moderationNotes && (
                        <p className="text-[10px] text-amber-700 mt-1 italic">
                          Note: {rev.moderationNotes}
                        </p>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <ReviewBadge status={rev.status} />
                    </td>

                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end space-x-1.5">
                        {rev.status !== 'APPROVED' && (
                          <button
                            type="button"
                            onClick={() => handleOpenModeration(rev, 'APPROVED')}
                            className="px-2.5 py-1 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-800 text-[11px] font-bold border border-emerald-200 transition-colors flex items-center space-x-1"
                            title="Approve Review"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                            <span>Approve</span>
                          </button>
                        )}

                        {rev.status !== 'REJECTED' && (
                          <button
                            type="button"
                            onClick={() => handleOpenModeration(rev, 'REJECTED')}
                            className="px-2.5 py-1 rounded-lg bg-rose-50 hover:bg-rose-100 text-rose-800 text-[11px] font-bold border border-rose-200 transition-colors flex items-center space-x-1"
                            title="Reject Review"
                          >
                            <XCircle className="w-3.5 h-3.5 text-rose-600" />
                            <span>Reject</span>
                          </button>
                        )}

                        {rev.status !== 'HIDDEN' && (
                          <button
                            type="button"
                            onClick={() => handleOpenModeration(rev, 'HIDDEN')}
                            className="p-1 text-slate-400 hover:text-slate-800 rounded-lg hover:bg-slate-100"
                            title="Hide Review"
                            aria-label="Hide Review"
                          >
                            <EyeOff className="w-3.5 h-3.5" />
                          </button>
                        )}

                        <button
                          type="button"
                          onClick={() => deleteMutation.mutate(rev.id)}
                          className="p-1 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50"
                          title="Delete Review"
                          aria-label="Delete Review"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between p-4 border-t border-slate-200">
            <button
              type="button"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 disabled:opacity-40"
            >
              Previous
            </button>
            <span className="text-xs font-semibold text-slate-600">
              Page {page + 1} of {totalPages}
            </span>
            <button
              type="button"
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= totalPages - 1}
              className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        )}
      </div>

      {/* Moderation Notes Modal */}
      {moderateModalReview && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 space-y-4">
            <h3 className="text-base font-bold text-slate-900">
              Confirm Action: {actionType} Review
            </h3>
            <p className="text-xs text-slate-500">
              Provide an optional moderation explanation or reason for record-keeping.
            </p>

            <form onSubmit={handleModerationSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Moderation Notes
                </label>
                <textarea
                  rows={3}
                  placeholder={`Reason for marking as ${actionType}...`}
                  value={moderationNotes}
                  onChange={(e) => setModerationNotes(e.target.value)}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs resize-none"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setModerateModalReview(null)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={moderateMutation.isPending}
                  className={`px-5 py-2 rounded-xl text-xs font-bold text-white ${
                    actionType === 'APPROVED'
                      ? 'bg-emerald-600 hover:bg-emerald-700'
                      : 'bg-rose-600 hover:bg-rose-700'
                  }`}
                >
                  Confirm {actionType}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
