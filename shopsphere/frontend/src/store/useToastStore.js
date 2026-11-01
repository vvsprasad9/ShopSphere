import { create } from 'zustand';

export const useToastStore = create((set) => ({
  toasts: [],

  addToast: ({ type = 'info', message, title, duration = 4000 }) => {
    const id = Date.now() + Math.random().toString(36).substring(2, 9);
    set((state) => ({
      toasts: [...state.toasts, { id, type, message, title }],
    }));

    if (duration > 0) {
      setTimeout(() => {
        set((state) => ({
          toasts: state.toasts.filter((t) => t.id !== id),
        }));
      }, duration);
    }

    return id;
  },

  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    }));
  },

  success: (message, title = 'Success') => {
    useToastStore.getState().addToast({ type: 'success', message, title });
  },

  error: (message, title = 'Error') => {
    useToastStore.getState().addToast({ type: 'error', message, title, duration: 6000 });
  },

  info: (message, title = 'Information') => {
    useToastStore.getState().addToast({ type: 'info', message, title });
  },

  warning: (message, title = 'Warning') => {
    useToastStore.getState().addToast({ type: 'warning', message, title, duration: 5000 });
  },
}));
