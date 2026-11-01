import React, { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight, SearchX } from 'lucide-react';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import ProductCard from '../../components/storefront/ProductCard';
import ProductFilters from '../../components/storefront/ProductFilters';
import { ProductGridSkeleton } from '../../components/common/LoadingSkeleton';

export default function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Extract filter parameters from URL
  const filters = useMemo(() => ({
    keyword: searchParams.get('keyword') || '',
    categorySlug: searchParams.get('categorySlug') || '',
    categoryId: searchParams.get('categoryId') || '',
    minPrice: searchParams.get('minPrice') || '',
    maxPrice: searchParams.get('maxPrice') || '',
    inStockOnly: searchParams.get('inStockOnly') === 'true',
    featured: searchParams.get('featured') === 'true' ? true : undefined,
    sortBy: searchParams.get('sortBy') || 'newest',
    page: parseInt(searchParams.get('page') || '0', 10),
    size: parseInt(searchParams.get('size') || '12', 10),
  }), [searchParams]);

  // Fetch Categories for sidebar
  const { data: categoriesData } = useQuery({
    queryKey: ['categoriesActive'],
    queryFn: () => categoryApi.getAllCategories(true),
    staleTime: 120000,
  });

  // Fetch paginated products matching criteria
  const { data: productsData, isLoading } = useQuery({
    queryKey: ['productsSearch', filters],
    queryFn: () => productApi.searchProducts(filters),
    keepPreviousData: true,
  });

  const categories = categoriesData || [];
  const products = productsData?.content || [];
  const totalElements = productsData?.totalElements || 0;
  const totalPages = productsData?.totalPages || 0;
  const currentPage = productsData?.pageNumber || filters.page;

  const handleFilterChange = (newFilters) => {
    const updated = new URLSearchParams(searchParams);

    Object.entries(newFilters).forEach(([key, val]) => {
      if (val === undefined || val === null || val === '') {
        updated.delete(key);
      } else {
        updated.set(key, val.toString());
      }
    });

    setSearchParams(updated);
  };

  const handleResetFilters = () => {
    setSearchParams(new URLSearchParams());
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      handleFilterChange({ page: newPage });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  return (
    <div className="space-y-6 pb-16">
      {/* Header */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-extrabold text-slate-900">
            {filters.keyword
              ? `Search Results for "${filters.keyword}"`
              : filters.categorySlug
              ? `Category: ${filters.categorySlug}`
              : 'Product Catalog'}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Showing {products.length} of {totalElements} {totalElements === 1 ? 'item' : 'items'}
          </p>
        </div>

        {/* Quick clear search */}
        {filters.keyword && (
          <button
            type="button"
            onClick={() => handleFilterChange({ keyword: '', page: 0 })}
            className="text-xs font-semibold text-rose-600 hover:text-rose-700 self-start sm:self-center"
          >
            Clear Search Keyword ✕
          </button>
        )}
      </div>

      {/* Main Grid: Sidebar + Products */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
        {/* Left Filter Sidebar */}
        <div className="lg:col-span-1">
          <ProductFilters
            filters={filters}
            categories={categories}
            onFilterChange={handleFilterChange}
            onReset={handleResetFilters}
          />
        </div>

        {/* Right Products Area */}
        <div className="lg:col-span-3 space-y-8">
          {isLoading ? (
            <ProductGridSkeleton count={8} />
          ) : products.length === 0 ? (
            <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center shadow-sm space-y-4">
              <div className="w-16 h-16 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
                <SearchX className="w-8 h-8" />
              </div>
              <h3 className="text-lg font-bold text-slate-800">No products match your criteria</h3>
              <p className="text-xs text-slate-500 max-w-md mx-auto">
                Try loosening your filters, adjusting your price range, or clearing keywords to see more results.
              </p>
              <button
                type="button"
                onClick={handleResetFilters}
                className="px-5 py-2.5 rounded-xl bg-slate-900 text-white text-xs font-bold hover:bg-slate-800 transition-colors"
              >
                Reset All Filters
              </button>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between bg-white rounded-2xl border border-slate-200 p-4 shadow-sm">
                  <button
                    type="button"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 disabled:cursor-not-allowed flex items-center space-x-1"
                  >
                    <ChevronLeft className="w-4 h-4" />
                    <span>Previous</span>
                  </button>

                  <span className="text-xs font-bold text-slate-700">
                    Page {currentPage + 1} of {totalPages}
                  </span>

                  <button
                    type="button"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 disabled:cursor-not-allowed flex items-center space-x-1"
                  >
                    <span>Next</span>
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
