import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldCheck, Truck, RefreshCcw, Lock } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="bg-slate-900 text-slate-300 pt-12 pb-8 border-t border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Value props */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 pb-12 border-b border-slate-800">
          <div className="flex items-center space-x-3.5">
            <div className="w-12 h-12 rounded-2xl bg-white/5 flex items-center justify-center text-emerald-400 border border-white/10 shrink-0">
              <Truck className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">Express Delivery</h4>
              <p className="text-xs text-slate-400">Direct courier tracking across India</p>
            </div>
          </div>

          <div className="flex items-center space-x-3.5">
            <div className="w-12 h-12 rounded-2xl bg-white/5 flex items-center justify-center text-teal-400 border border-white/10 shrink-0">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">Authentic Products</h4>
              <p className="text-xs text-slate-400">100% genuine guaranteed items</p>
            </div>
          </div>

          <div className="flex items-center space-x-3.5">
            <div className="w-12 h-12 rounded-2xl bg-white/5 flex items-center justify-center text-blue-400 border border-white/10 shrink-0">
              <Lock className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">Secure Payments</h4>
              <p className="text-xs text-slate-400">Razorpay 256-bit SSL encryption</p>
            </div>
          </div>

          <div className="flex items-center space-x-3.5">
            <div className="w-12 h-12 rounded-2xl bg-white/5 flex items-center justify-center text-purple-400 border border-white/10 shrink-0">
              <RefreshCcw className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">Hassle-free Returns</h4>
              <p className="text-xs text-slate-400">Dedicated customer support</p>
            </div>
          </div>
        </div>

        {/* Links section */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8 py-10">
          <div>
            <h5 className="text-xs font-bold uppercase tracking-wider text-white mb-4">Shop</h5>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/catalog" className="hover:text-emerald-400 transition-colors">
                  All Products
                </Link>
              </li>
              <li>
                <Link to="/catalog?sortBy=popular" className="hover:text-emerald-400 transition-colors">
                  Top Rated
                </Link>
              </li>
              <li>
                <Link to="/catalog?featured=true" className="hover:text-emerald-400 transition-colors">
                  Featured Products
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h5 className="text-xs font-bold uppercase tracking-wider text-white mb-4">Orders & Support</h5>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/track" className="hover:text-emerald-400 transition-colors">
                  Track Your Order
                </Link>
              </li>
              <li>
                <Link to="/orders" className="hover:text-emerald-400 transition-colors">
                  Order History
                </Link>
              </li>
              <li>
                <Link to="/cart" className="hover:text-emerald-400 transition-colors">
                  Shopping Cart
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h5 className="text-xs font-bold uppercase tracking-wider text-white mb-4">Account</h5>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/profile" className="hover:text-emerald-400 transition-colors">
                  My Profile
                </Link>
              </li>
              <li>
                <Link to="/addresses" className="hover:text-emerald-400 transition-colors">
                  Saved Addresses
                </Link>
              </li>
              <li>
                <Link to="/wishlist" className="hover:text-emerald-400 transition-colors">
                  Wishlist
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h5 className="text-xs font-bold uppercase tracking-wider text-white mb-4">Enterprise Architecture</h5>
            <p className="text-xs text-slate-400 leading-relaxed mb-3">
              Engineered with Spring Boot 3.3.4, Java 21 LTS, React 18, and MySQL 8. Authoritative inventory and idempotent transaction control.
            </p>
            <span className="inline-block px-2.5 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-mono text-[11px]">
              v1.0.0 Production Ready
            </span>
          </div>
        </div>

        {/* Bottom copyright */}
        <div className="pt-8 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 space-y-4 sm:space-y-0">
          <p>&copy; {new Date().getFullYear()} ShopSphere Inc. All rights reserved.</p>
          <div className="flex space-x-6">
            <span className="hover:text-slate-400 cursor-pointer">Privacy Policy</span>
            <span className="hover:text-slate-400 cursor-pointer">Terms of Service</span>
            <span className="hover:text-slate-400 cursor-pointer">Security Practices</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
