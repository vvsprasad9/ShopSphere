import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, CheckCheck, Trash2, ArrowRight, Clock, Check } from 'lucide-react';
import { notificationApi } from '../../api/notificationApi';
import { formatDate } from '../../utils/formatters';
import { useToastStore } from '../../store/useToastStore';

export default function NotificationsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { success, error } = useToastStore();
  const [page, setPage] = useState(0);

  const { data: notificationsData, isLoading } = useQuery({
    queryKey: ['notifications', page],
    queryFn: () => notificationApi.getUserNotifications({ page, size: 15 }),
  });

  const { data: unreadCountData } = useQuery({
    queryKey: ['unreadNotificationsCount'],
    queryFn: () => notificationApi.getUnreadNotificationCount(),
  });

  // Mark single as read mutation
  const markReadMutation = useMutation({
    mutationFn: (id) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unreadNotificationsCount'] });
    },
    onError: (err) => {
      error(err.message || 'Could not mark as read', 'Error');
    },
  });

  // Mark all as read mutation
  const markAllReadMutation = useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unreadNotificationsCount'] });
      success('All notifications marked as read', 'Notifications Updated');
    },
    onError: (err) => {
      error(err.message || 'Could not mark all as read', 'Error');
    },
  });

  // Delete notification mutation
  const deleteNotificationMutation = useMutation({
    mutationFn: (id) => notificationApi.deleteNotification(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unreadNotificationsCount'] });
      success('Notification deleted', 'Removed');
    },
    onError: (err) => {
      error(err.message || 'Could not delete notification', 'Error');
    },
  });

  const notifications = notificationsData?.content || [];
  const totalPages = notificationsData?.totalPages || 0;
  const unreadCount = unreadCountData?.unreadCount || 0;

  const handleNotificationClick = (notif) => {
    if (!notif.isRead) {
      markReadMutation.mutate(notif.id);
    }
    if (notif.linkUrl) {
      navigate(notif.linkUrl);
    }
  };

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto py-10 px-4 space-y-4 animate-pulse">
        <div className="h-6 bg-slate-200 rounded w-1/4"></div>
        <div className="space-y-3">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-20 bg-slate-200 rounded-2xl"></div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-8 pb-16">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900">Notifications</h1>
          <p className="text-xs text-slate-500 mt-1">
            {unreadCount} unread alert{unreadCount === 1 ? '' : 's'}
          </p>
        </div>

        {unreadCount > 0 && (
          <button
            type="button"
            onClick={() => markAllReadMutation.mutate()}
            disabled={markAllReadMutation.isPending}
            className="px-4 py-2 rounded-xl text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 transition-colors flex items-center space-x-1.5"
          >
            <CheckCheck className="w-4 h-4" />
            <span>Mark All as Read</span>
          </button>
        )}
      </div>

      {notifications.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-3xl border border-slate-200 p-8 space-y-3">
          <Bell className="w-10 h-10 text-slate-400 mx-auto" />
          <h3 className="text-base font-bold text-slate-800">No notifications</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            You are all caught up! Order updates and announcements will appear here.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((notif) => (
            <div
              key={notif.id}
              onClick={() => handleNotificationClick(notif)}
              className={`p-4 rounded-2xl border transition-all flex items-start justify-between gap-4 cursor-pointer ${
                notif.isRead
                  ? 'bg-white border-slate-200 hover:border-slate-300'
                  : 'bg-emerald-50/40 border-emerald-200 ring-2 ring-emerald-500/10'
              }`}
            >
              <div className="flex items-start space-x-3.5">
                <div
                  className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 mt-0.5 ${
                    notif.isRead
                      ? 'bg-slate-100 text-slate-500'
                      : 'bg-emerald-600 text-white shadow-sm'
                  }`}
                >
                  <Bell className="w-4 h-4" />
                </div>
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <h4 className="text-xs font-bold text-slate-900">{notif.title}</h4>
                    {!notif.isRead && (
                      <span className="w-2 h-2 rounded-full bg-emerald-600 shrink-0" />
                    )}
                  </div>
                  <p className="text-xs text-slate-600 leading-relaxed">{notif.message}</p>
                  <span className="text-[10px] text-slate-400 block pt-1">
                    {formatDate(notif.createdAt, true)}
                  </span>
                </div>
              </div>

              {/* Action buttons */}
              <div
                className="flex items-center space-x-1 shrink-0"
                onClick={(e) => e.stopPropagation()}
              >
                {!notif.isRead && (
                  <button
                    type="button"
                    onClick={() => markReadMutation.mutate(notif.id)}
                    className="p-1.5 text-slate-400 hover:text-emerald-600 rounded-lg"
                    title="Mark as read"
                    aria-label="Mark as read"
                  >
                    <Check className="w-4 h-4" />
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => deleteNotificationMutation.mutate(notif.id)}
                  className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg"
                  title="Delete notification"
                  aria-label="Delete notification"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
