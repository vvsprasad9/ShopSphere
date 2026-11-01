import { create } from 'zustand';

export const useAuthStore = create((set, get) => ({
  user: JSON.parse(localStorage.getItem('shopsphere_user') || 'null'),
  token: localStorage.getItem('shopsphere_access_token') || null,
  isAuthenticated: !!localStorage.getItem('shopsphere_access_token'),

  setAuth: (user, token, refreshToken) => {
    localStorage.setItem('shopsphere_user', JSON.stringify(user));
    localStorage.setItem('shopsphere_access_token', token);
    if (refreshToken) {
      localStorage.setItem('shopsphere_refresh_token', refreshToken);
    }
    set({ user, token, isAuthenticated: true });
  },

  updateUser: (updatedUser) => {
    localStorage.setItem('shopsphere_user', JSON.stringify(updatedUser));
    set({ user: updatedUser });
  },

  logout: () => {
    localStorage.removeItem('shopsphere_user');
    localStorage.removeItem('shopsphere_access_token');
    localStorage.removeItem('shopsphere_refresh_token');
    set({ user: null, token: null, isAuthenticated: false });
    window.dispatchEvent(new Event('shopsphere_logout'));
  },

  isAdmin: () => {
    const roles = get().user?.roles || [];
    return roles.includes('ROLE_ADMIN');
  },

  isManager: () => {
    const roles = get().user?.roles || [];
    return roles.includes('ROLE_MANAGER');
  },

  isStaff: () => {
    const roles = get().user?.roles || [];
    return roles.includes('ROLE_ADMIN') || roles.includes('ROLE_MANAGER');
  },

  hasRole: (role) => {
    const roles = get().user?.roles || [];
    return roles.includes(role);
  },
}));
