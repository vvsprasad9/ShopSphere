import { create } from 'zustand';

export const useCartStore = create((set) => ({
  itemCount: 0,
  setItemCount: (count) => set({ itemCount: count }),
  increment: (by = 1) => set((state) => ({ itemCount: state.itemCount + by })),
  decrement: (by = 1) => set((state) => ({ itemCount: Math.max(0, state.itemCount - by) })),
  clearCartCount: () => set({ itemCount: 0 }),
}));
