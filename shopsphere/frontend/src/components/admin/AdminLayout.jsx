import React from 'react';
import { NavLink, Outlet, Link } from 'react-router-dom';
import {
  LayoutDashboard,
  Package,
  Layers,
  ShoppingBag,
  MessageSquare,
  ArrowLeft,
  Shield,
  TrendingUp,
  BarChart3,
  Archive,
  Users,
  FileText,
} from 'lucide-react';
import { useAuthStore } from '../../store/useAuthStore';

export default function AdminLayout() {
  const { user } = useAuthStore();

  const analyticsNav = [
    { to: '/admin', end: true, label: 'Analytics Dashboard', icon: LayoutDashboard },
    { to: '/admin/analytics/sales', label: 'Sales & Revenue', icon: TrendingUp },
    { to: '/admin/analytics/products', label: 'Product Performance', icon: BarChart3 },
    { to: '/admin/analytics/inventory', label: 'Stock Health', icon: Archive },
    { to: '/admin/analytics/customers', label: 'Customer Cohorts', icon: Users },
    { to: '/admin/analytics/audit', label: 'Audit Trail', icon: FileText },
  ];

  const managementNav = [
    { to: '/admin/products', label: 'Products & Inventory', icon: Package },
    { to: '/admin/categories', label: 'Category Hierarchy', icon: Layers },
    { to: '/admin/orders', label: 'Order Processing', icon: ShoppingBag },
    { to: '/admin/reviews', label: 'Review Moderation', icon: MessageSquare },
  ];

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col md:flex-row">
      {/* Sidebar */}
      <aside className="w-full md:w-64 bg-slate-900 text-white shrink-0 p-5 flex flex-col justify-between">
        <div>
          {/* Logo & Portal Header */}
          <div className="flex items-center space-x-3 mb-6 pb-4 border-b border-slate-800">
            <div className="w-9 h-9 rounded-xl bg-purple-600 flex items-center justify-center text-white font-bold text-lg shadow-md shadow-purple-600/30">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-extrabold text-white leading-tight">Admin Portal</h2>
              <span className="text-[10px] font-semibold text-purple-400 uppercase tracking-wider">
                Management Console
              </span>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="space-y-4">
            <div>
              <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500 px-3.5 mb-2">
                Business Intelligence
              </div>
              <div className="space-y-1">
                {analyticsNav.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      end={item.end}
                      className={({ isActive }) =>
                        `flex items-center space-x-3 px-3.5 py-2 rounded-xl text-xs font-semibold transition-colors ${
                          isActive
                            ? 'bg-purple-600 text-white shadow-sm'
                            : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                        }`
                      }
                    >
                      <Icon className="w-4 h-4" />
                      <span>{item.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </div>

            <div>
              <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500 px-3.5 mb-2">
                Store Operations
              </div>
              <div className="space-y-1">
                {managementNav.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      end={item.end}
                      className={({ isActive }) =>
                        `flex items-center space-x-3 px-3.5 py-2 rounded-xl text-xs font-semibold transition-colors ${
                          isActive
                            ? 'bg-purple-600 text-white shadow-sm'
                            : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                        }`
                      }
                    >
                      <Icon className="w-4 h-4" />
                      <span>{item.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </div>
          </nav>
        </div>

        {/* Back to Store & User */}
        <div className="pt-6 border-t border-slate-800 space-y-4">
          <Link
            to="/"
            className="flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Return to Storefront</span>
          </Link>
          <div className="flex items-center space-x-2.5 bg-slate-800/60 p-2.5 rounded-xl">
            <div className="w-8 h-8 rounded-lg bg-purple-500/20 text-purple-300 font-bold text-xs flex items-center justify-center">
              {user?.firstName?.[0] || 'A'}
            </div>
            <div className="truncate">
              <p className="text-xs font-bold text-white truncate">{user?.firstName} {user?.lastName}</p>
              <p className="text-[10px] text-slate-400 truncate">{user?.email}</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 p-6 md:p-10 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}
