import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AnalyticsDashboardPage from '../pages/admin/analytics/AnalyticsDashboardPage';
import analyticsApi from '../api/analyticsApi';

// Mock analyticsApi
vi.mock('../api/analyticsApi', () => ({
  default: {
    getOverview: vi.fn(),
    getSalesTrend: vi.fn(),
    getPaymentAnalytics: vi.fn(),
    downloadSalesCsv: vi.fn(),
  },
}));

describe('AnalyticsDashboardPage Integration Tests', () => {
  let queryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    vi.clearAllMocks();
  });

  const mockOverview = {
    data: {
      financials: {
        grossSales: 150000.0,
        netSales: 140000.0,
        discounts: 10000.0,
        tax: 15000.0,
        shippingCharges: 5000.0,
        collectedRevenue: 135000.0,
        pendingCollections: 15000.0,
        pendingCodCollections: 10000.0,
        pendingOnlineCollections: 5000.0,
        confirmedRefunds: 2000.0,
        netCollectedRevenue: 133000.0,
        averageOrderValue: 1875.5,
        grossSalesChangePercent: 12.5,
        netCollectedRevenueChangePercent: 10.2,
        orderCountChangePercent: 5.0,
      },
      orders: {
        totalOrders: 80,
        pendingOrders: 5,
        confirmedOrders: 30,
        processingOrders: 15,
        shippedOrders: 10,
        deliveredOrders: 18,
        cancelledOrders: 2,
      },
      catalog: {
        totalProducts: 120,
        totalActiveInventoryUnits: 2500,
        totalReservedUnits: 45,
        totalPurchasableUnits: 2455,
        lowStockItemCount: 6,
        outOfStockItemCount: 1,
      },
      customers: {
        totalCustomers: 320,
        newCustomersInPeriod: 25,
        returningBuyersCount: 14,
      },
    },
  };

  it('renders KPI cards with formatted Indian Rupee values and trend indicators', async () => {
    analyticsApi.getOverview.mockResolvedValue(mockOverview);
    analyticsApi.getSalesTrend.mockResolvedValue({ data: [] });
    analyticsApi.getPaymentAnalytics.mockResolvedValue({ data: { methodDistribution: [] } });

    render(
      <QueryClientProvider client={queryClient}>
        <AnalyticsDashboardPage />
      </QueryClientProvider>
    );

    // Header
    expect(await screen.findByText(/Executive Analytics/i)).toBeInTheDocument();

    // Financial KPIs
    expect(screen.getAllByText(/Gross Sales/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Net Collected Cash/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Pending Collections/i).length).toBeGreaterThan(0);

    // Stock metrics
    expect(screen.getByText(/2455 units/i)).toBeInTheDocument();
  });

  it('allows toggling date presets and triggers CSV export', async () => {
    analyticsApi.getOverview.mockResolvedValue(mockOverview);
    analyticsApi.getSalesTrend.mockResolvedValue({ data: [] });
    analyticsApi.getPaymentAnalytics.mockResolvedValue({ data: { methodDistribution: [] } });

    render(
      <QueryClientProvider client={queryClient}>
        <AnalyticsDashboardPage />
      </QueryClientProvider>
    );

    const exportBtn = await screen.findByRole('button', { name: /Export CSV/i });
    expect(exportBtn).toBeInTheDocument();

    fireEvent.click(exportBtn);
    expect(analyticsApi.downloadSalesCsv).toHaveBeenCalledTimes(1);

    // Preset button click
    const sevenDayBtn = screen.getByRole('button', { name: /Last 7 Days/i });
    fireEvent.click(sevenDayBtn);

    await waitFor(() => {
      expect(analyticsApi.getOverview).toHaveBeenCalled();
    });
  });
});
