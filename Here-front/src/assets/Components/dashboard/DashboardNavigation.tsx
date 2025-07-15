import React from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  BarChart2,
  Package,
  Store,
  Tag,
  Percent,
  Database,
  CreditCard,
  User, // Import User icon
} from "lucide-react";

const DashboardNavigation = () => {
  const location = useLocation();
  const navigate = useNavigate();

  console.log("Current location in DashboardNavigation:", location.pathname);

  // Navigation items pour dashboard avec les nouvelles sections
  const navItems = [
    {
      path: "/account/dashboard",
      label: "Dashboard",
      icon: <BarChart2 size={20} className="mr-2" />,
    },
    {
      path: "/account", // Updated to match Navbar's navigation
      label: "Profil",
      icon: <User size={20} className="mr-2" />,
    },
    {
      path: "/account/categories",
      label: "Catégories",
      icon: <Tag size={20} className="mr-2" />,
    },
    {
      path: "/account/stores",
      label: "Boutiques",
      icon: <Store size={20} className="mr-2" />,
    },
    {
      path: "/account/promotions",
      label: "Promotions",
      icon: <Percent size={20} className="mr-2" />,
    },
    {
      path: "/account/stock",
      label: "Stock",
      icon: <Database size={20} className="mr-2" />,
    },
    {
      path: "/account/subscriptions",
      label: "Abonnement",
      icon: <CreditCard size={20} className="mr-2" />,
    },
  ];

  // Active path check
  const isActive = (path) => {
    console.log(`Checking if ${path} matches ${location.pathname}`);
    return location.pathname === path;
  };

  // Navigate programmatically on click
  const handleNavigation = (path) => {
    console.log("Navigating to:", path);
    navigate(path);
  };

  return (
    <div className="bg-white shadow-sm mb-6">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex overflow-x-auto space-x-4 py-3">
          {navItems.map((item) => (
            <button
              key={item.path}
              onClick={() => handleNavigation(item.path)}
              className={`inline-flex items-center px-3 py-2 text-sm font-medium rounded-md whitespace-nowrap ${
                isActive(item.path)
                  ? "bg-teal-500 text-white"
                  : "text-gray-600 hover:bg-gray-100"
              }`}
            >
              {item.icon}
              {item.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default DashboardNavigation;