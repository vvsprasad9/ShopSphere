import React from 'react';
import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { useToastStore } from '../../store/useToastStore';

export default function AdminRoute({ children }) {
  const { isAuthenticated, isStaff } = useAuthStore();
  const { error } = useToastStore();
  const location = useLocation();

  if (!isAuthenticated) {
    const returnUrl = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?redirect=${returnUrl}`} replace />;
  }

  // Backend authorization allows ROLE_ADMIN and ROLE_MANAGER
  if (!isStaff()) {
    error('Access Denied: Administrative or Manager privileges required.', 'Unauthorized');
    return <Navigate to="/" replace />;
  }

  return children ? children : <Outlet />;
}
