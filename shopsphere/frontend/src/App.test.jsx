import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import App from './App';

// Mock axiosClient to avoid actual network calls during component smoke test
vi.mock('./api/axiosClient', () => ({
  default: {
    get: vi.fn().mockImplementation((url) => {
      if (url.includes('/featured')) {
        return Promise.resolve({ data: [] });
      }
      if (url.includes('/tree')) {
        return Promise.resolve({ data: [] });
      }
      return Promise.resolve({ data: null });
    }),
    post: vi.fn().mockResolvedValue({ data: null }),
    put: vi.fn().mockResolvedValue({ data: null }),
    delete: vi.fn().mockResolvedValue({ data: null }),
    patch: vi.fn().mockResolvedValue({ data: null }),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

describe('ShopSphere Frontend Integration Smoke Test', () => {
  it('renders application brand, navigation links, and hero banner', async () => {
    render(<App />);

    // Brand and logo
    const brandElements = screen.getAllByText(/ShopSphere/i);
    expect(brandElements.length).toBeGreaterThan(0);

    // Navigation links
    expect(screen.getByRole('link', { name: /Explore Catalog/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Track Order/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Sign In/i })).toBeInTheDocument();

    // Hero title
    expect(screen.getByText(/Curated Products/i)).toBeInTheDocument();
    expect(screen.getByText(/Direct to Your Door/i)).toBeInTheDocument();
  });
});
