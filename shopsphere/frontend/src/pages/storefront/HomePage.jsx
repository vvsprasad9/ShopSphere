import React from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowRight, Sparkles, TrendingUp, ShieldCheck } from 'lucide-react';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import ProductCard from '../../components/storefront/ProductCard';
import { ProductGridSkeleton } from '../../components/common/LoadingSkeleton';

export default function HomePage() {
  // Featured products
  const { data: featuredData, isLoading: featuredLoading } = useQuery({
    queryKey: ['featuredProducts'],
    queryFn: () => productApi.getFeaturedProducts(8),
    staleTime: 60000,
  });

  // Top categories
  const { data: categoriesData, isLoading: categoriesLoading } = useQuery({
    queryKey: ['categoryHierarchy'],
    queryFn: () => categoryApi.getCategoryHierarchy(),
    staleTime: 120000,
  });

  const featuredProducts = featuredData || [];
  const categories = categoriesData || [];

  return (
    <div className="space-y-16 pb-16">
      {/* Hero Banner */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-slate-950 via-slate-900 to-emerald-950 text-white shadow-2xl p-8 sm:p-14">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center space-x-2 px-3.5 py-1 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-emerald-300 text-xs font-semibold mb-4 backdrop-blur-md">
            <Sparkles className="w-3.5 h-3.5" />
            <span>New Season Collections Live</span>
          </div>

          <h1 className="text-3xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-tight mb-4">
            Curated Products. <br />
            <span className="bg-clip-text text-transparent bg-gradient-to-r from-emerald-400 to-teal-300">
              Direct to Your Door.
            </span>
          </h1>

          <p className="text-slate-300 text-sm sm:text-base mb-8 leading-relaxed">
            Experience authoritative inventory tracking, instant Razorpay checkout, and express nationwide delivery on premium lifestyle and tech essentials.
          </p>

          <div className="flex flex-wrap gap-4">
            <Link
              to="/catalog"
              className="inline-flex items-center px-6 py-3.5 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm shadow-lg shadow-emerald-600/30 transition-all hover:gap-3 group"
            >
              <span>Explore Entire Catalog</span>
              <ArrowRight className="w-4 h-4 ml-2 transition-transform group-hover:translate-x-1" />
            </Link>

            <Link
              to="/catalog?sortBy=popular"
              className="inline-flex items-center px-6 py-3.5 rounded-2xl bg-white/10 hover:bg-white/20 text-white font-semibold text-sm border border-white/10 backdrop-blur-md transition-colors"
            >
              <TrendingUp className="w-4 h-4 mr-2 text-emerald-400" />
              <span>Best Sellers</span>
            </Link>
          </div>
        </div>

        {/* Decorative background grid */}
        <div className="absolute right-0 top-0 bottom-0 w-1/2 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-emerald-500/10 via-transparent to-transparent pointer-events-none" />
      </section>

      {/* Featured Categories */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">Featured Categories</h2>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">Browse our hand-picked product hierarchies</p>
          </div>
          <Link
            to="/catalog"
            className="text-xs sm:text-sm font-bold text-emerald-600 hover:text-emerald-700 flex items-center space-x-1"
          >
            <span>View All</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {categoriesLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4 animate-pulse">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-28 bg-slate-200 rounded-2xl"></div>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
            {categories.slice(0, 6).map((cat) => (
              <Link
                key={cat.id}
                to={`/catalog?categorySlug=${encodeURIComponent(cat.slug)}`}
                className="group p-5 bg-white rounded-2xl border border-slate-200 hover:border-emerald-500 hover:shadow-md transition-all flex flex-col justify-between"
              >
                <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-700 font-black flex items-center justify-center text-sm group-hover:bg-emerald-600 group-hover:text-white transition-colors">
                  {cat.name[0]}
                </div>
                <div className="mt-4">
                  <h3 className="text-xs font-bold text-slate-900 group-hover:text-emerald-600 truncate transition-colors">
                    {cat.name}
                  </h3>
                  <span className="text-[10px] text-slate-400">Explore items →</span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Featured Products Showcase */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">Featured Releases</h2>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">Authoritative stock and trending products</p>
          </div>
          <Link
            to="/catalog?featured=true"
            className="text-xs sm:text-sm font-bold text-emerald-600 hover:text-emerald-700 flex items-center space-x-1"
          >
            <span>View All Featured</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {featuredLoading ? (
          <ProductGridSkeleton count={8} />
        ) : featuredProducts.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8">
            <p className="text-sm font-semibold text-slate-600">No featured products currently available.</p>
            <Link
              to="/catalog"
              className="inline-block mt-3 px-4 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold"
            >
              Browse Full Catalog
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {featuredProducts.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>

      {/* Trust & Guarantee Banner */}
      <section className="bg-gradient-to-r from-emerald-900 to-teal-900 rounded-3xl p-8 sm:p-12 text-white flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="max-w-xl">
          <div className="inline-flex items-center space-x-2 text-emerald-300 text-xs font-bold mb-2">
            <ShieldCheck className="w-4 h-4" />
            <span>Guaranteed Shopping Protection</span>
          </div>
          <h3 className="text-2xl sm:text-3xl font-extrabold mb-2">
            Authoritative Concurrency & Secure Payments
          </h3>
          <p className="text-slate-300 text-xs sm:text-sm leading-relaxed">
            Every transaction is protected by atomic inventory reservation and Razorpay 256-bit SSL token encryption to ensure complete purchase safety.
          </p>
        </div>
        <Link
          to="/catalog"
          className="px-6 py-3.5 rounded-2xl bg-white text-emerald-950 font-bold text-xs sm:text-sm hover:bg-emerald-50 transition-colors shadow-lg shrink-0"
        >
          Start Shopping Now
        </Link>
      </section>
    </div>
  );
}
