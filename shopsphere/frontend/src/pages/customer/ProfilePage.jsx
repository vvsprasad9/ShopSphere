import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { User, Lock, MessageSquare, CheckCircle2, AlertCircle } from 'lucide-react';
import { userApi } from '../../api/userApi';
import { reviewApi } from '../../api/reviewApi';
import { useAuthStore } from '../../store/useAuthStore';
import { useToastStore } from '../../store/useToastStore';
import { ReviewBadge } from '../../components/common/Badge';
import StarRating from '../../components/common/StarRating';
import { formatDate } from '../../utils/formatters';

export default function ProfilePage() {
  const queryClient = useQueryClient();
  const { user, updateUser } = useAuthStore();
  const { success, error } = useToastStore();

  const [activeTab, setActiveTab] = useState('profile'); // profile, password, reviews

  // Profile Form state
  const [profileData, setFormData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    phone: user?.phone || '',
  });

  // Password Form state
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  // Fetch fresh profile
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const data = await userApi.getProfile();
      setFormData({
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        phone: data.phone || '',
      });
      return data;
    },
  });

  // Fetch Customer's own reviews
  const { data: myReviewsData } = useQuery({
    queryKey: ['myReviews'],
    queryFn: () => reviewApi.getCustomerReviews({ page: 0, size: 20 }),
    enabled: activeTab === 'reviews',
  });

  // Update profile mutation
  const updateProfileMutation = useMutation({
    mutationFn: (data) => userApi.updateProfile(data),
    onSuccess: (updated) => {
      updateUser(updated);
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      success('Profile details updated successfully', 'Profile Updated');
    },
    onError: (err) => {
      error(err.message || 'Could not update profile', 'Update Failed');
    },
  });

  // Change password mutation
  const changePasswordMutation = useMutation({
    mutationFn: (data) => userApi.changePassword(data),
    onSuccess: () => {
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      success('Password changed successfully! Existing sessions revoked.', 'Password Changed');
    },
    onError: (err) => {
      error(err.message || 'Could not change password', 'Error');
    },
  });

  // Delete review mutation
  const deleteReviewMutation = useMutation({
    mutationFn: (reviewId) => reviewApi.deleteReview(reviewId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myReviews'] });
      success('Review deleted successfully', 'Deleted');
    },
    onError: (err) => {
      error(err.message || 'Could not delete review', 'Error');
    },
  });

  const handleProfileSubmit = (e) => {
    e.preventDefault();
    updateProfileMutation.mutate(profileData);
  };

  const handlePasswordSubmit = (e) => {
    e.preventDefault();
    if (passwordData.newPassword.length < 8) {
      error('New password must be at least 8 characters long', 'Validation');
      return;
    }
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      error('New passwords do not match', 'Validation');
      return;
    }
    changePasswordMutation.mutate(passwordData);
  };

  const myReviews = myReviewsData?.content || [];

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-16">
      <div>
        <h1 className="text-2xl font-black text-slate-900">Account Settings</h1>
        <p className="text-xs text-slate-500 mt-1">Manage your identity, security and feedback</p>
      </div>

      {/* Navigation Tabs */}
      <div className="flex border-b border-slate-200 space-x-8">
        <button
          type="button"
          onClick={() => setActiveTab('profile')}
          className={`pb-3 text-xs font-bold transition-colors flex items-center space-x-2 border-b-2 ${
            activeTab === 'profile'
              ? 'border-emerald-600 text-emerald-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <User className="w-4 h-4" />
          <span>Personal Information</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('password')}
          className={`pb-3 text-xs font-bold transition-colors flex items-center space-x-2 border-b-2 ${
            activeTab === 'password'
              ? 'border-emerald-600 text-emerald-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Lock className="w-4 h-4" />
          <span>Security & Password</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('reviews')}
          className={`pb-3 text-xs font-bold transition-colors flex items-center space-x-2 border-b-2 ${
            activeTab === 'reviews'
              ? 'border-emerald-600 text-emerald-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <MessageSquare className="w-4 h-4" />
          <span>My Reviews</span>
        </button>
      </div>

      {/* Tab 1: Profile Information */}
      {activeTab === 'profile' && (
        <form onSubmit={handleProfileSubmit} className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm space-y-4 max-w-xl">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
                First Name
              </label>
              <input
                type="text"
                required
                value={profileData.firstName}
                onChange={(e) => setFormData({ ...profileData, firstName: e.target.value })}
                className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium"
              />
            </div>
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
                Last Name
              </label>
              <input
                type="text"
                required
                value={profileData.lastName}
                onChange={(e) => setFormData({ ...profileData, lastName: e.target.value })}
                className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
              Email Address (Immutable)
            </label>
            <input
              type="email"
              disabled
              value={user?.email || ''}
              className="w-full p-2.5 bg-slate-100 border border-slate-200 rounded-xl text-xs font-medium text-slate-500 cursor-not-allowed"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
              Phone Number
            </label>
            <input
              type="tel"
              value={profileData.phone}
              onChange={(e) => setFormData({ ...profileData, phone: e.target.value })}
              className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium"
            />
          </div>

          <button
            type="submit"
            disabled={updateProfileMutation.isPending}
            className="px-6 py-2.5 rounded-xl text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 transition-colors shadow-sm"
          >
            {updateProfileMutation.isPending ? 'Saving...' : 'Save Profile Changes'}
          </button>
        </form>
      )}

      {/* Tab 2: Change Password */}
      {activeTab === 'password' && (
        <form onSubmit={handlePasswordSubmit} className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm space-y-4 max-w-xl">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
              Current Password
            </label>
            <input
              type="password"
              required
              value={passwordData.currentPassword}
              onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })}
              className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
              New Password (min 8 chars)
            </label>
            <input
              type="password"
              required
              minLength={8}
              value={passwordData.newPassword}
              onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
              className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
              Confirm New Password
            </label>
            <input
              type="password"
              required
              minLength={8}
              value={passwordData.confirmPassword}
              onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
              className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium"
            />
          </div>

          <button
            type="submit"
            disabled={changePasswordMutation.isPending}
            className="px-6 py-2.5 rounded-xl text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 transition-colors shadow-sm"
          >
            {changePasswordMutation.isPending ? 'Updating...' : 'Change Password'}
          </button>
        </form>
      )}

      {/* Tab 3: My Reviews */}
      {activeTab === 'reviews' && (
        <div className="space-y-4">
          {myReviews.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8">
              <p className="text-sm font-semibold text-slate-600">You haven't written any reviews yet.</p>
            </div>
          ) : (
            myReviews.map((rev) => (
              <div key={rev.id} className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3">
                <div className="flex items-start justify-between">
                  <div>
                    <h4 className="text-sm font-bold text-slate-900">{rev.productName}</h4>
                    <div className="flex items-center space-x-2 mt-1">
                      <StarRating rating={rev.rating} size="sm" />
                      <span className="text-xs text-slate-400">{formatDate(rev.createdAt)}</span>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <ReviewBadge status={rev.status} />
                    <button
                      type="button"
                      onClick={() => deleteReviewMutation.mutate(rev.id)}
                      className="text-xs text-rose-600 hover:text-rose-700 font-semibold ml-2"
                    >
                      Delete
                    </button>
                  </div>
                </div>

                {rev.title && <p className="text-xs font-bold text-slate-800">{rev.title}</p>}
                <p className="text-xs text-slate-600 leading-relaxed">{rev.comment}</p>
                {rev.moderationNotes && (
                  <p className="text-[11px] text-amber-700 bg-amber-50 p-2 rounded-lg">
                    Moderator note: {rev.moderationNotes}
                  </p>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
