import React, { useState, useEffect } from "react";
import {
  Home,
  Package,
  ShoppingBag,
  Store,
  Boxes,
  Tag,
  Truck,
  Percent,
  Settings,
  Search,
  Plus,
  Edit,
  Trash2,
  ChevronDown,
  X,
  AlertCircle,
  Calendar,
  DollarSign,
  Archive,
  Clock,
  Filter,
  Eye,
  Menu,
  MapPin,
  Check,
  AlertTriangle,
  Bell,
  User,
  LogOut,
  ChevronRight,
  BarChart2,
  Users,
  Phone,
  Map,
  Activity,
  FileText,
  Shield,
  Save,
  XCircle,
} from "lucide-react";

// Main Dashboard Component
const VendorDashboard = () => {
  // Backend API configuration
  const [apiUrl, setApiUrl] = useState("https://your-api-endpoint.com/api");
  const [token, setToken] = useState(null);

  // State for active section and UI
  const [activeSection, setActiveSection] = useState("dashboard");
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);

  // State for data
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [notifications, setNotifications] = useState([
    {
      id: 1,
      message: "Low stock alert: 3 products below threshold",
      type: "warning",
      time: "2 hours ago",
    },
    {
      id: 2,
      message: "New order #2845 received",
      type: "info",
      time: "3 hours ago",
    },
    {
      id: 3,
      message: "Promotion 'Summer Sale' is ending tomorrow",
      type: "info",
      time: "5 hours ago",
    },
  ]);

  // Initialize token from localStorage or other authentication source
  useEffect(() => {
    const storedToken = localStorage.getItem("authToken");
    if (storedToken) {
      setToken(storedToken);
    }
  }, []);

  // Function to fetch data from backend
  const fetchData = async (endpoint) => {
    setLoading(true);
    setError(null);
    try {
      // Check if API call should be made
      const shouldUseRealApi = false; // Set to true when your backend is ready

      if (shouldUseRealApi && token) {
        // Real API implementation
        const response = await fetch(`${apiUrl}/${endpoint}`, {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (!response.ok) {
          throw new Error(`Error: ${response.status}`);
        }

        const data = await response.json();
        return data;
      } else {
        // For development/demo, return mock data
        console.log(`Using mock data for ${endpoint}`);
        return mockData[endpoint];
      }
    } catch (err) {
      console.error(`API Error:`, err);
      setError(`Failed to load ${endpoint}: ${err.message}`);
      return null;
    } finally {
      setLoading(false);
    }
  };

  // Render the appropriate section based on activeSection state
  const renderSection = () => {
    switch (activeSection) {
      case "dashboard":
        return <DashboardOverview fetchData={fetchData} />;
      case "orders":
        return <OrdersManagement fetchData={fetchData} />;
      case "products":
        return <ProductsManagement fetchData={fetchData} />;
      case "stores":
        return <StoresManagement fetchData={fetchData} />;
      case "inventory":
        return <InventoryManagement fetchData={fetchData} />;
      case "categories":
        return <CategoriesManagement fetchData={fetchData} />;
      case "delivery":
        return <DeliveryManagement fetchData={fetchData} />;
      case "promotions":
        return <PromotionsManagement fetchData={fetchData} />;
      default:
        return <DashboardOverview fetchData={fetchData} />;
    }
  };

  // Navigation items
  const navItems = [
    { id: "dashboard", label: "Dashboard", icon: <Home size={20} /> },
    { id: "orders", label: "Orders", icon: <ShoppingBag size={20} /> },
    { id: "products", label: "Products", icon: <Package size={20} /> },
    { id: "stores", label: "Stores", icon: <Store size={20} /> },
    { id: "inventory", label: "Inventory", icon: <Boxes size={20} /> },
    { id: "categories", label: "Categories", icon: <Tag size={20} /> },
    { id: "delivery", label: "Delivery Staff", icon: <Truck size={20} /> },
    { id: "promotions", label: "Promotions", icon: <Percent size={20} /> },
  ];

  // Handle log out
  const handleLogout = () => {
    localStorage.removeItem("authToken");
    // Redirect to login page or homepage
    window.location.href = "/";
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top Navigation Bar */}
      <header className="bg-gradient-to-r from-teal-600 to-teal-700 shadow-md">
        <div className="mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            {/* Logo and Mobile Menu Button */}
            <div className="flex items-center">
              <div className="flex-shrink-0 flex items-center">
                <Store className="h-8 w-8 text-white" />
                <h1 className="ml-2 text-xl font-bold text-white">
                  Vendor Hub
                </h1>
              </div>

              {/* Mobile menu button */}
              <div className="ml-4 flex items-center md:hidden">
                <button
                  onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                  className="inline-flex items-center justify-center p-2 rounded-md text-teal-200 hover:text-white hover:bg-teal-500 focus:outline-none"
                >
                  <Menu size={24} />
                </button>
              </div>
            </div>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex md:space-x-4 items-center">
              {navItems.map((item) => (
                <button
                  key={item.id}
                  onClick={() => setActiveSection(item.id)}
                  className={`flex items-center px-3 py-2 text-sm font-medium rounded-md ${
                    activeSection === item.id
                      ? "bg-teal-800 text-white"
                      : "text-teal-100 hover:bg-teal-500 hover:text-white"
                  }`}
                >
                  <span className="mr-2">{item.icon}</span>
                  {item.label}
                </button>
              ))}
            </nav>

            {/* Right side controls */}
            <div className="flex items-center">
              {/* Search */}
              <div className="hidden md:block mx-4">
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Search size={16} className="text-gray-400" />
                  </div>
                  <input
                    type="text"
                    placeholder="Search..."
                    className="block w-full pl-10 pr-3 py-2 border border-transparent rounded-md leading-5 bg-teal-700 text-teal-100 placeholder-teal-300 focus:outline-none focus:bg-white focus:placeholder-gray-400 focus:text-gray-900 focus:ring-teal-300 sm:text-sm"
                  />
                </div>
              </div>

              {/* Notifications */}
              <div className="relative ml-3">
                <button
                  className="relative p-1 text-teal-200 rounded-full hover:text-white focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-teal-600 focus:ring-white"
                  onClick={() => setNotificationsOpen(!notificationsOpen)}
                >
                  <Bell size={20} />
                  {notifications.length > 0 && (
                    <span className="absolute top-0 right-0 block h-2 w-2 rounded-full bg-red-400 ring-2 ring-teal-600"></span>
                  )}
                </button>

                {/* Notifications dropdown */}
                {notificationsOpen && (
                  <div className="origin-top-right absolute right-0 mt-2 w-80 rounded-md shadow-lg bg-white ring-1 ring-black ring-opacity-5 z-50">
                    <div className="py-2 px-3 border-b border-gray-200">
                      <div className="flex justify-between items-center">
                        <h3 className="text-sm font-semibold text-gray-700">
                          Notifications
                        </h3>
                        <span className="text-xs font-medium text-teal-600">
                          {notifications.length} new
                        </span>
                      </div>
                    </div>
                    <div className="max-h-72 overflow-y-auto">
                      {notifications.map((notification) => (
                        <div
                          key={notification.id}
                          className="px-4 py-3 border-b border-gray-200 hover:bg-gray-50"
                        >
                          <div className="flex">
                            <div className="flex-shrink-0 mr-3">
                              {notification.type === "warning" ? (
                                <div className="h-8 w-8 rounded-full bg-amber-100 flex items-center justify-center">
                                  <AlertTriangle
                                    size={16}
                                    className="text-amber-600"
                                  />
                                </div>
                              ) : (
                                <div className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center">
                                  <Bell size={16} className="text-blue-600" />
                                </div>
                              )}
                            </div>
                            <div>
                              <p className="text-sm text-gray-700">
                                {notification.message}
                              </p>
                              <p className="text-xs text-gray-500 mt-1">
                                {notification.time}
                              </p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                    <div className="py-2 px-3 border-t border-gray-200">
                      <button className="text-xs font-medium text-teal-600 hover:text-teal-500 w-full text-center">
                        View all notifications
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Profile dropdown */}
              <div className="relative ml-3">
                <div>
                  <button
                    className="flex items-center text-sm rounded-full focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-teal-600 focus:ring-white"
                    onClick={() => setUserMenuOpen(!userMenuOpen)}
                  >
                    <div className="h-8 w-8 rounded-full bg-teal-800 flex items-center justify-center text-white border-2 border-teal-200">
                      VS
                    </div>
                    <span className="hidden lg:flex lg:items-center ml-2">
                      <span className="text-sm font-medium text-white">
                        Vendor Store
                      </span>
                      <ChevronDown size={16} className="ml-1 text-teal-200" />
                    </span>
                  </button>
                </div>

                {/* User menu dropdown */}
                {userMenuOpen && (
                  <div className="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg bg-white ring-1 ring-black ring-opacity-5 z-50">
                    <div className="py-1">
                      <button className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center">
                        <User size={16} className="mr-2 text-gray-500" />
                        Profile
                      </button>
                      <button className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center">
                        <Settings size={16} className="mr-2 text-gray-500" />
                        Settings
                      </button>
                      <div className="border-t border-gray-100 my-1"></div>
                      <button
                        onClick={handleLogout}
                        className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center"
                      >
                        <LogOut size={16} className="mr-2 text-gray-500" />
                        Sign out
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden bg-teal-700 shadow-inner">
            <div className="pt-2 pb-3 space-y-1">
              {navItems.map((item) => (
                <button
                  key={item.id}
                  onClick={() => {
                    setActiveSection(item.id);
                    setMobileMenuOpen(false);
                  }}
                  className={`flex items-center w-full px-3 py-2 text-base font-medium ${
                    activeSection === item.id
                      ? "bg-teal-800 text-white"
                      : "text-teal-100 hover:bg-teal-600 hover:text-white"
                  }`}
                >
                  <span className="mr-3">{item.icon}</span>
                  {item.label}
                </button>
              ))}
            </div>
            {/* Mobile search */}
            <div className="p-3 border-t border-teal-800">
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search size={16} className="text-teal-400" />
                </div>
                <input
                  type="text"
                  placeholder="Search..."
                  className="block w-full pl-10 pr-3 py-2 border border-transparent rounded-md bg-teal-800 text-teal-100 placeholder-teal-400 focus:outline-none focus:bg-teal-900 focus:border-teal-500"
                />
              </div>
            </div>
          </div>
        )}
      </header>

      {/* Page title and breadcrumb */}
      <div className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto py-4 px-4 sm:px-6 lg:px-8">
          <div className="flex items-center space-x-2 text-sm text-gray-500">
            <Home size={14} />
            <ChevronRight size={14} />
            <span className="capitalize">{activeSection}</span>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="py-6">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Error display */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 rounded-md flex items-start">
              <AlertCircle
                size={20}
                className="text-red-500 mr-3 mt-0.5 flex-shrink-0"
              />
              <div>
                <h3 className="font-medium text-red-800">Error</h3>
                <p className="text-sm text-red-700">{error}</p>
              </div>
            </div>
          )}

          {/* Loading indicator */}
          {loading && (
            <div className="flex justify-center my-6">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-500"></div>
            </div>
          )}

          {/* Content area */}
          {renderSection()}
        </div>
      </main>
    </div>
  );
};

// Dashboard Overview Section
const DashboardOverview = ({ fetchData }) => {
  const [stats, setStats] = useState(null);
  const [recentOrders, setRecentOrders] = useState([]);
  const [lowStockProducts, setLowStockProducts] = useState([]);

  useEffect(() => {
    const loadDashboardData = async () => {
      const stats = await fetchData("dashboardStats");
      const orders = await fetchData("recentOrders");
      const lowStock = await fetchData("lowStockProducts");

      if (stats) setStats(stats);
      if (orders) setRecentOrders(orders);
      if (lowStock) setLowStockProducts(lowStock);
    };

    loadDashboardData();
  }, [fetchData]);

  return (
    <div className="space-y-6">
      {/* Dashboard Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-800">Dashboard Overview</h1>
        <div className="flex items-center space-x-2">
          <button className="px-4 py-2 text-sm font-medium bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50">
            <Calendar size={16} className="inline mr-2 text-gray-500" />
            Last 30 days
          </button>
          <button className="px-4 py-2 text-sm font-medium text-white bg-teal-600 rounded-md shadow-sm hover:bg-teal-700">
            <FileText size={16} className="inline mr-2" />
            Export Report
          </button>
        </div>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Revenue"
          value="£18,426.79"
          change="+14.5%"
          positive={true}
          icon={<DollarSign size={20} className="text-green-500" />}
          bgClass="from-green-50 to-teal-50 border-green-200"
        />
        <StatCard
          title="Total Orders"
          value="356"
          change="+8.2%"
          positive={true}
          icon={<ShoppingBag size={20} className="text-blue-500" />}
          bgClass="from-blue-50 to-indigo-50 border-blue-200"
        />
        <StatCard
          title="Products Sold"
          value="1,245"
          change="+18.3%"
          positive={true}
          icon={<Package size={20} className="text-purple-500" />}
          bgClass="from-purple-50 to-pink-50 border-purple-200"
        />
        <StatCard
          title="Low Stock Items"
          value="12"
          change="-2"
          positive={false}
          icon={<AlertTriangle size={20} className="text-amber-500" />}
          bgClass="from-amber-50 to-orange-50 border-amber-200"
        />
      </div>

      {/* Main Content Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Low Stock Products Section */}
        <div className="lg:col-span-1 bg-white rounded-lg shadow overflow-hidden border border-red-100">
          <div className="px-4 py-5 border-b border-gray-200 bg-gradient-to-r from-red-50 to-amber-50">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-medium leading-6 text-gray-900 flex items-center">
                <AlertTriangle size={18} className="text-red-500 mr-2" />
                Low Stock Alert
              </h3>
              <span className="bg-red-100 text-red-800 text-xs font-semibold py-1 px-2 rounded-full">
                {lowStockProducts.length} Items
              </span>
            </div>
            <p className="mt-1 text-sm text-gray-500">
              Products below critical threshold
            </p>
          </div>
          <ul className="divide-y divide-gray-200 max-h-[420px] overflow-y-auto">
            {lowStockProducts.map((product) => (
              <li key={product.id} className="px-4 py-4 hover:bg-gray-50">
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    <div className="flex-shrink-0 h-10 w-10 bg-gray-200 rounded-md"></div>
                    <div>
                      <div className="text-sm font-medium text-gray-900">
                        {product.name}
                      </div>
                      <div className="mt-1 flex items-center">
                        <span className="text-xs text-gray-500">
                          SKU: {product.sku}
                        </span>
                        <span className="mx-2 text-gray-300">|</span>
                        <span className="text-xs text-gray-500">
                          Store: {product.store}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                      {product.stock} left
                    </div>
                    <div className="mt-1 text-xs text-gray-500">
                      Threshold: {product.threshold}
                    </div>
                  </div>
                </div>
                <div className="mt-2 flex justify-between">
                  <div className="w-full bg-gray-200 rounded-full h-2.5">
                    <div
                      className="bg-red-500 h-2.5 rounded-full"
                      style={{
                        width: `${(product.stock / product.threshold) * 100}%`,
                      }}
                    ></div>
                  </div>
                  <button className="ml-3 text-sm font-medium text-teal-600 hover:text-teal-500">
                    Restock
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <div className="bg-gray-50 px-4 py-3 border-t border-gray-200 text-center">
            <button className="text-sm font-medium text-teal-600 hover:text-teal-500">
              View all inventory
            </button>
          </div>
        </div>

        {/* Recent Orders and Store Performance */}
        <div className="lg:col-span-2 space-y-6">
          {/* Store Performance */}
          <div className="bg-gradient-to-br from-white to-teal-50 rounded-lg shadow overflow-hidden border border-teal-100">
            <div className="p-5 border-b border-teal-100">
              <h3 className="text-lg font-medium text-gray-900">
                Store Performance
              </h3>
              <div className="mt-1 text-sm text-gray-500">
                Top 3 performing stores this month
              </div>
            </div>
            <div className="px-5 py-4">
              <div className="space-y-4">
                {mockData.storePerformance.map((store, index) => (
                  <div key={store.id} className="flex items-center">
                    <div className="flex-shrink-0 flex items-center justify-center h-10 w-10 rounded-md bg-teal-100 text-teal-600 mr-4">
                      {index + 1}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between mb-1">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {store.name}
                        </p>
                        <p className="text-sm font-semibold text-gray-900">
                          {store.revenue}
                        </p>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-teal-500 h-2 rounded-full"
                          style={{ width: `${store.percentage}%` }}
                        ></div>
                      </div>
                      <div className="flex items-center justify-between mt-1">
                        <div className="text-xs text-gray-500">
                          <span className="font-medium text-gray-900">
                            {store.orders}
                          </span>{" "}
                          orders
                        </div>
                        <div
                          className={`text-xs ${
                            store.changeUp ? "text-green-600" : "text-red-600"
                          }`}
                        >
                          {store.changeUp ? "↑" : "↓"} {store.change}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="bg-teal-50 px-5 py-3 border-t border-teal-100">
              <a
                href="#"
                className="text-sm font-medium text-teal-700 hover:text-teal-500"
              >
                View all stores →
              </a>
            </div>
          </div>

          {/* Recent Orders */}
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="p-5 border-b border-gray-200 flex justify-between items-center">
              <div>
                <h3 className="text-lg font-medium text-gray-900">
                  Recent Orders
                </h3>
                <div className="mt-1 text-sm text-gray-500">
                  Most recent customer orders
                </div>
              </div>
              <button className="text-sm font-medium text-teal-600 hover:text-teal-500">
                View all
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Order
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Customer
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Total
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Date
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {recentOrders.map((order) => (
                    <tr key={order.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-teal-600">
                        #{order.id}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {order.customer}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(
                            order.status
                          )}`}
                        >
                          {order.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {order.total}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {order.date}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// Categories Management Section - CRUD functionality
const CategoriesManagement = ({ fetchData }) => {
  const [categories, setCategories] = useState([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [formData, setFormData] = useState({
    id: "",
    name: "",
    description: "",
  });

  useEffect(() => {
    const loadCategories = async () => {
      const data = await fetchData("categories");
      if (data) setCategories(data);
    };

    loadCategories();
  }, [fetchData]);

  // Reset form data
  const resetForm = () => {
    setFormData({ id: "", name: "", description: "" });
    setEditingCategory(null);
  };

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  // Handle form submission (create or update)
  const handleSubmit = (e) => {
    e.preventDefault();

    if (editingCategory) {
      // Update existing category
      const updatedCategories = categories.map((cat) =>
        cat.id === editingCategory.id ? { ...formData } : cat
      );
      setCategories(updatedCategories);
      setEditingCategory(null);
    } else {
      // Create new category
      const newCategory = {
        ...formData,
        id: `cat-${Date.now()}`, // Generate a unique ID (in a real app, the backend would do this)
      };
      setCategories([...categories, newCategory]);
    }

    // Reset form and hide it
    resetForm();
    setShowAddForm(false);
  };

  // Handle edit button click
  const handleEdit = (category) => {
    setFormData({
      id: category.id,
      name: category.name,
      description: category.description,
    });
    setEditingCategory(category);
    setShowAddForm(true);
  };

  // Handle delete button click
  const handleDelete = (categoryId) => {
    if (window.confirm("Are you sure you want to delete this category?")) {
      setCategories(categories.filter((cat) => cat.id !== categoryId));
    }
  };

  return (
    <div className="space-y-6">
      {/* Header with add button */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Categories Management
          </h1>
          <p className="text-gray-500 mt-1">Manage your product categories</p>
        </div>
        <button
          onClick={() => {
            resetForm();
            setShowAddForm(true);
          }}
          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-600 hover:bg-teal-700"
        >
          <Plus size={18} className="mr-2" />
          Add Category
        </button>
      </div>

      {/* Category Form (Add/Edit) */}
      {showAddForm && (
        <div className="bg-white shadow-md rounded-lg overflow-hidden border border-gray-200">
          <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h3 className="text-lg font-medium text-gray-900">
              {editingCategory ? "Edit Category" : "Add New Category"}
            </h3>
          </div>
          <form onSubmit={handleSubmit} className="p-6">
            <div className="grid grid-cols-1 gap-y-6 gap-x-4 sm:grid-cols-6">
              <div className="sm:col-span-3">
                <label
                  htmlFor="name"
                  className="block text-sm font-medium text-gray-700"
                >
                  Category Name
                </label>
                <input
                  type="text"
                  name="name"
                  id="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  required
                  className="mt-1 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
                />
              </div>

              <div className="sm:col-span-6">
                <label
                  htmlFor="description"
                  className="block text-sm font-medium text-gray-700"
                >
                  Description
                </label>
                <textarea
                  id="description"
                  name="description"
                  rows="3"
                  value={formData.description}
                  onChange={handleInputChange}
                  className="mt-1 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
                ></textarea>
              </div>
            </div>

            <div className="mt-6 flex justify-end space-x-3">
              <button
                type="button"
                onClick={() => {
                  resetForm();
                  setShowAddForm(false);
                }}
                className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-teal-600 hover:bg-teal-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500"
              >
                {editingCategory ? "Update" : "Create"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Categories List */}
      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Description
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Products
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {categories.map((category) => (
                <tr key={category.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {category.id}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="flex-shrink-0 h-8 w-8 bg-teal-100 rounded-md flex items-center justify-center">
                        <Tag size={14} className="text-teal-600" />
                      </div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-gray-900">
                          {category.name}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 max-w-xs">
                    <div className="text-sm text-gray-500 truncate">
                      {category.description}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-teal-100 text-teal-800">
                      {category.productCount || 0} products
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      onClick={() => handleEdit(category)}
                      className="text-teal-600 hover:text-teal-900 mr-4"
                    >
                      <Edit size={18} />
                    </button>
                    <button
                      onClick={() => handleDelete(category.id)}
                      className="text-red-600 hover:text-red-900"
                    >
                      <Trash2 size={18} />
                    </button>
                  </td>
                </tr>
              ))}

              {categories.length === 0 && (
                <tr>
                  <td
                    colSpan="5"
                    className="px-6 py-10 text-center text-gray-500"
                  >
                    <div className="flex flex-col items-center">
                      <Tag size={40} className="text-gray-300 mb-2" />
                      <p className="text-lg font-medium text-gray-500 mb-1">
                        No categories found
                      </p>
                      <p className="text-sm text-gray-400">
                        Add a new category to get started
                      </p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

// Products Management Section - With CRUD functionality
const ProductsManagement = ({ fetchData }) => {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [stores, setStores] = useState([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [formData, setFormData] = useState({
    id: "",
    name: "",
    sku: "",
    description: "",
    price: "",
    categoryId: "",
    storeId: "",
    stockQuantity: "",
    threshold: "",
  });

  useEffect(() => {
    const loadData = async () => {
      const productsData = await fetchData("products");
      const categoriesData = await fetchData("categories");
      const storesData = await fetchData("stores");

      if (productsData) setProducts(productsData);
      if (categoriesData) setCategories(categoriesData);
      if (storesData) setStores(storesData);
    };

    loadData();
  }, [fetchData]);

  // Reset form data
  const resetForm = () => {
    setFormData({
      id: "",
      name: "",
      sku: "",
      description: "",
      price: "",
      categoryId: "",
      storeId: "",
      stockQuantity: "",
      threshold: "",
    });
    setEditingProduct(null);
  };

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  // Handle form submission (create or update)
  const handleSubmit = (e) => {
    e.preventDefault();

    if (editingProduct) {
      // Update existing product
      const updatedProducts = products.map((product) =>
        product.id === editingProduct.id ? { ...formData } : product
      );
      setProducts(updatedProducts);
      setEditingProduct(null);
    } else {
      // Create new product
      const newProduct = {
        ...formData,
        id: `prod-${Date.now()}`, // Generate a unique ID
      };
      setProducts([...products, newProduct]);
    }

    // Reset form and hide it
    resetForm();
    setShowAddForm(false);
  };

  // Handle edit button click
  const handleEdit = (product) => {
    setFormData({
      id: product.id,
      name: product.name,
      sku: product.sku,
      description: product.description || "",
      price: product.price,
      categoryId: product.categoryId || "",
      storeId: product.storeId || "",
      stockQuantity: product.stock,
      threshold: product.threshold || "",
    });
    setEditingProduct(product);
    setShowAddForm(true);
  };

  // Handle delete button click
  const handleDelete = (productId) => {
    if (window.confirm("Are you sure you want to delete this product?")) {
      setProducts(products.filter((product) => product.id !== productId));
    }
  };

  // Get category name by ID
  const getCategoryName = (categoryId) => {
    const category = categories.find((cat) => cat.id === categoryId);
    return category ? category.name : "Unknown";
  };

  // Get store name by ID
  const getStoreName = (storeId) => {
    const store = stores.find((s) => s.id === storeId);
    return store ? store.name : "Unknown";
  };

  return (
    <div className="space-y-6">
      {/* Header with add button */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Products Management
          </h1>
          <p className="text-gray-500 mt-1">Manage your inventory products</p>
        </div>
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search size={18} className="text-gray-400" />
            </div>
            <input
              type="text"
              placeholder="Search products..."
              className="pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-teal-500 focus:border-teal-500 w-full sm:w-64"
            />
          </div>

          <button
            onClick={() => {
              resetForm();
              setShowAddForm(true);
            }}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-600 hover:bg-teal-700"
          >
            <Plus size={18} className="mr-2" />
            Add Product
          </button>
        </div>
      </div>

      {/* Product Form (Add/Edit) */}
      {showAddForm && (
        <div
          className="fixed inset-0 z-40 overflow-y-auto"
          aria-labelledby="modal-title"
          role="dialog"
          aria-modal="true"
        >
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div
              className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
              aria-hidden="true"
            ></div>

            <span
              className="hidden sm:inline-block sm:align-middle sm:h-screen"
              aria-hidden="true"
            >
              &#8203;
            </span>

            <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-3xl sm:w-full">
              <div className="px-6 py-4 border-b border-gray-200 bg-gray-50 flex items-center justify-between">
                <h3 className="text-lg font-medium text-gray-900">
                  {editingProduct ? "Edit Product" : "Add New Product"}
                </h3>
                <button
                  onClick={() => {
                    resetForm();
                    setShowAddForm(false);
                  }}
                  className="text-gray-400 hover:text-gray-500"
                >
                  <XCircle size={20} />
                </button>
              </div>

              <form onSubmit={handleSubmit} className="p-6">
                <div className="grid grid-cols-1 gap-y-6 gap-x-4 sm:grid-cols-6">
                  <div className="sm:col-span-3">
                    <label
                      htmlFor="name"
                      className="block text-sm font-medium text-gray-700"
                    >
                      Product Name
                    </label>
                    <input
                      type="text"
                      name="name"
                      id="name"
                      value={formData.name}
                      onChange={handleInputChange}
                      required
                      className="mt-1 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
                    />
                  </div>

                  <div className="sm:col-span-3">
                    <label
                      htmlFor="sku"
                      className="block text-sm font-medium text-gray-700"
                    >
                      SKU
                    </label>
                    <input
                      type="text"
                      name="sku"
                      id="sku"
                      value={formData.sku}
                      onChange={handleInputChange}
                      required
                      className="mt-1 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
                    />
                  </div>

                  <div className="sm:col-span-6">
                    <label
                      htmlFor="description"
                      className="block text-sm font-medium text-gray-700"
                    >
                      Description
                    </label>
                    <textarea
                      id="description"
                      name="description"
                      rows="3"
                      value={formData.description}
                      onChange={handleInputChange}
                      className="mt-1 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
                    ></textarea>
                  </div>

                  <div className="sm:col-span-2">
                    <label
                      htmlFor="price"
                      className="block text-sm font-medium text-gray-700"
                    >
                      Price
                    </label>
                    <div className="mt-1 relative rounded-md shadow-sm">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <span className="text-gray-500 sm:text-sm">£</span>
                      </div>
                      <input
                        type="text"
                        name="price"
                        id="price"
                        value={formData.price}
                        onChange={handleInputChange}
                        required
                        className="focus:ring-teal-500 focus:border-teal-500 block w-full pl-7 pr-12 sm:text-sm border-gray-300 rounded-md"
                        placeholder="0.00"
                      />
                    </div>
                  </div>

                  <div className="sm:col-span-2">
                    <label
                      htmlFor="categoryId"
                      className="block text-sm font-medium text-gray-700"
                    >
                      Category
                    </label>
                    <select
                      id="categoryId"
                      name="categoryId"
                      value={formData.categoryId}
                      onChange={handleInputChange}
                      className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-teal-500 focus:border-teal-500 sm:text-sm rounded-md"
                    >
                      <option value="">Select Category</option>
                      {categories.map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="sm:col-span-2">
                    <label
                      htmlFor="storeId"
                      className="block text-sm font-medium text-gray-700"
                    >
                      Store
                    </label>
                    <select
                      id="storeId"
                      name="storeId"
                      value={formData.storeId}
                      onChange={handleInputChange}
                      className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-teal-500 focus:border-teal-500 sm:text-sm rounded-md"
                    >
                      <option value="">Select Store</option>
                      {stores.map((store) => (
                        <option key={store.id} value={store.id}>
                          {store.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="sm:col-span-3">
                    <label
                      htmlFor="stockQuantity"
                      className="block text-sm font-medium text-gray-700"
                    >
                      Stock Quantity
                    </label>
                    <input
                      type="number"
                      name="stockQuantity"
                      id="stockQuantity"
                      value={formData.stockQuantity}
                      onChange={handleInputChange}
                      required
                      min="0"
                      className="mt-1 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
                    />
                  </div>

                  <div className="sm:col-span-3">
                    <label
                      htmlFor="threshold"
                      className="block text-sm font-medium text-gray-700"
                    >
                      Low Stock Threshold
                    </label>
                    <input
                      type="number"
                      name="threshold"
                      id="threshold"
                      value={formData.threshold}
                      onChange={handleInputChange}
                      min="0"
                      className="mt-1 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
                    />
                  </div>
                </div>

                <div className="mt-6 flex justify-end space-x-3">
                  <button
                    type="button"
                    onClick={() => {
                      resetForm();
                      setShowAddForm(false);
                    }}
                    className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="inline-flex justify-center items-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-teal-600 hover:bg-teal-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500"
                  >
                    <Save size={18} className="mr-2" />
                    {editingProduct ? "Update Product" : "Create Product"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Products List */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Product
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Category
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Store
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Price
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Stock
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {products.map((product) => (
                <tr key={product.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="h-10 w-10 flex-shrink-0 bg-gray-200 rounded-md"></div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-gray-900">
                          {product.name}
                        </div>
                        <div className="text-xs text-gray-500">
                          {product.sku}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {getCategoryName(product.categoryId)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {getStoreName(product.storeId)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">
                      {product.price}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div
                      className={`text-sm font-medium ${
                        parseInt(product.stock) <=
                        parseInt(product.threshold || 10)
                          ? "text-red-600"
                          : "text-gray-900"
                      }`}
                    >
                      {product.stock}
                      {parseInt(product.stock) <=
                        parseInt(product.threshold || 10) && (
                        <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-800">
                          Low
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      onClick={() => handleEdit(product)}
                      className="text-teal-600 hover:text-teal-900 mr-4"
                    >
                      <Edit size={18} />
                    </button>
                    <button
                      onClick={() => handleDelete(product.id)}
                      className="text-red-600 hover:text-red-900"
                    >
                      <Trash2 size={18} />
                    </button>
                  </td>
                </tr>
              ))}

              {products.length === 0 && (
                <tr>
                  <td
                    colSpan="6"
                    className="px-6 py-10 text-center text-gray-500"
                  >
                    <div className="flex flex-col items-center">
                      <Package size={40} className="text-gray-300 mb-2" />
                      <p className="text-lg font-medium text-gray-500 mb-1">
                        No products found
                      </p>
                      <p className="text-sm text-gray-400">
                        Add a new product to get started
                      </p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {products.length > 0 && (
          <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
            <div className="flex-1 flex justify-between sm:hidden">
              <button className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
                Previous
              </button>
              <button className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
                Next
              </button>
            </div>
            <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
              <div>
                <p className="text-sm text-gray-700">
                  Showing <span className="font-medium">1</span> to{" "}
                  <span className="font-medium">10</span> of{" "}
                  <span className="font-medium">{products.length}</span> results
                </p>
              </div>
              <div>
                <nav
                  className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px"
                  aria-label="Pagination"
                >
                  <button className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                    <span className="sr-only">Previous</span>
                    <ChevronRight
                      className="h-5 w-5 transform rotate-180"
                      aria-hidden="true"
                    />
                  </button>
                  <button className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">
                    1
                  </button>
                  <button className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">
                    2
                  </button>
                  <button className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                    <span className="sr-only">Next</span>
                    <ChevronRight className="h-5 w-5" aria-hidden="true" />
                  </button>
                </nav>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

// Stores Management Section
const StoresManagement = ({ fetchData }) => {
  const [stores, setStores] = useState([]);

  useEffect(() => {
    const loadStores = async () => {
      const data = await fetchData("stores");
      if (data) setStores(data);
    };

    loadStores();
  }, [fetchData]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Stores Management
          </h1>
          <p className="text-gray-500 mt-1">Manage your store locations</p>
        </div>
        <button className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-600 hover:bg-teal-700">
          <Plus size={18} className="mr-2" />
          Add Store
        </button>
      </div>

      {/* Stores Grid */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {stores.map((store) => (
          <div
            key={store.id}
            className="bg-white overflow-hidden shadow rounded-lg border border-gray-200"
          >
            <div className="px-4 py-5 sm:px-6 bg-gradient-to-r from-teal-50 to-blue-50 border-b border-gray-200">
              <div className="flex justify-between items-start">
                <div className="flex items-center">
                  <div className="flex-shrink-0 h-10 w-10 rounded-full bg-teal-100 flex items-center justify-center text-teal-600">
                    <Store size={20} />
                  </div>
                  <div className="ml-4">
                    <h3 className="text-lg font-medium leading-6 text-gray-900">
                      {store.name}
                    </h3>
                    <p className="text-sm text-gray-500 mt-1">
                      {store.city}, {store.country}
                    </p>
                  </div>
                </div>
                <div className="flex space-x-2">
                  <button className="text-gray-400 hover:text-gray-500">
                    <Edit size={18} />
                  </button>
                </div>
              </div>
            </div>
            <div className="px-4 py-5 sm:p-6 space-y-4">
              <div className="flex items-start">
                <MapPin className="h-5 w-5 text-gray-400 mt-0.5 flex-shrink-0" />
                <div className="ml-3 text-sm text-gray-500">
                  <p>{store.address}</p>
                  <p>
                    {store.postalCode} {store.city}
                  </p>
                  <p>{store.country}</p>
                </div>
              </div>
              <div className="flex items-start">
                <Phone className="h-5 w-5 text-gray-400 mt-0.5 flex-shrink-0" />
                <div className="ml-3 text-sm text-gray-500">
                  <p>{store.contact}</p>
                </div>
              </div>
              <div className="flex items-start">
                <Clock className="h-5 w-5 text-gray-400 mt-0.5 flex-shrink-0" />
                <div className="ml-3 text-sm text-gray-500">
                  <p>{store.hours}</p>
                </div>
              </div>
              <div className="flex justify-between items-center pt-4 border-t border-gray-200">
                <div className="flex items-center">
                  <div className="text-xs font-medium text-gray-500">
                    PRODUCTS
                  </div>
                  <div className="ml-2 text-sm font-medium text-gray-900">
                    {store.productCount}
                  </div>
                </div>
                <button className="text-sm font-medium text-teal-600 hover:text-teal-500">
                  View Inventory
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// Inventory Management Section
const InventoryManagement = ({ fetchData }) => {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">Inventory Management</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">
          Manage your inventory levels, stock alerts, and reordering.
        </p>
      </div>
    </div>
  );
};

// Delivery Management Section
const DeliveryManagement = ({ fetchData }) => {
  const [deliveryStaff, setDeliveryStaff] = useState([]);

  useEffect(() => {
    const loadDeliveryStaff = async () => {
      const data = await fetchData("deliveryStaff");
      if (data) setDeliveryStaff(data);
    };

    loadDeliveryStaff();
  }, [fetchData]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Delivery Staff Management
          </h1>
          <p className="text-gray-500 mt-1">Manage your delivery personnel</p>
        </div>
        <button className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-600 hover:bg-teal-700">
          <Plus size={18} className="mr-2" />
          Add Delivery Person
        </button>
      </div>

      {/* Delivery Staff Grid */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {deliveryStaff.map((staff) => (
          <div
            key={staff.id}
            className="bg-white overflow-hidden shadow rounded-lg border border-gray-200"
          >
            <div className="px-4 py-5 sm:px-6 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-indigo-50">
              <div className="flex justify-between items-start">
                <div className="flex items-center">
                  <div className="flex-shrink-0 h-10 w-10 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600">
                    {staff.firstName.charAt(0)}
                    {staff.lastName.charAt(0)}
                  </div>
                  <div className="ml-4">
                    <h3 className="text-lg font-medium leading-6 text-gray-900">
                      {staff.firstName} {staff.lastName}
                    </h3>
                    <p className="text-sm text-gray-500 mt-1">
                      {staff.location}
                    </p>
                  </div>
                </div>
                <div className="flex space-x-2">
                  <button className="text-gray-400 hover:text-gray-500">
                    <Edit size={18} />
                  </button>
                </div>
              </div>
            </div>
            <div className="px-4 py-5 sm:p-6 space-y-4">
              <div className="flex items-center">
                <Phone className="h-5 w-5 text-gray-400 flex-shrink-0" />
                <div className="ml-3 text-sm text-gray-500">
                  <p>{staff.phone}</p>
                </div>
              </div>
              <div className="flex items-center">
                <Truck className="h-5 w-5 text-gray-400 flex-shrink-0" />
                <div className="ml-3 text-sm text-gray-500">
                  <p>{staff.vehicle}</p>
                </div>
              </div>
              <div className="flex justify-between items-center pt-4 border-t border-gray-200">
                <div className="flex items-center">
                  <div className="text-xs font-medium text-gray-500">
                    STATUS
                  </div>
                  <div className="ml-2">
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        staff.isActive
                          ? "bg-green-100 text-green-800"
                          : "bg-gray-100 text-gray-800"
                      }`}
                    >
                      {staff.isActive ? "Active" : "Inactive"}
                    </span>
                  </div>
                </div>
                <button className="text-sm font-medium text-indigo-600 hover:text-indigo-500">
                  View Deliveries
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// Promotions Management Section
const PromotionsManagement = ({ fetchData }) => {
  const [promotions, setPromotions] = useState([]);

  useEffect(() => {
    const loadPromotions = async () => {
      const data = await fetchData("promotions");
      if (data) setPromotions(data);
    };

    loadPromotions();
  }, [fetchData]);

  // Toggle promo active status
  const togglePromoStatus = (id) => {
    setPromotions(
      promotions.map((promo) =>
        promo.id === id ? { ...promo, active: !promo.active } : promo
      )
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Promotions Management
          </h1>
          <p className="text-gray-500 mt-1">
            Manage your discounts and promotions
          </p>
        </div>
        <button className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-600 hover:bg-teal-700">
          <Plus size={18} className="mr-2" />
          Create Promotion
        </button>
      </div>

      {/* Promotions List */}
      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Discount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Product
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Period
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {promotions.map((promotion) => (
                <tr key={promotion.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="flex-shrink-0 h-8 w-8 rounded-full bg-purple-100 flex items-center justify-center text-purple-600">
                        <Percent size={16} />
                      </div>
                      <div className="ml-3">
                        <div className="text-sm font-medium text-gray-900">
                          {promotion.name}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">
                      {promotion.discount}%
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {promotion.productName}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div>{promotion.period}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <button
                      onClick={() => togglePromoStatus(promotion.id)}
                      className={`relative inline-flex flex-shrink-0 h-6 w-11 border-2 border-transparent rounded-full cursor-pointer transition-colors ease-in-out duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 ${
                        promotion.active ? "bg-teal-600" : "bg-gray-200"
                      }`}
                    >
                      <span className="sr-only">Toggle promotion</span>
                      <span
                        className={`${
                          promotion.active ? "translate-x-5" : "translate-x-0"
                        } pointer-events-none relative inline-block h-5 w-5 rounded-full bg-white shadow transform ring-0 transition ease-in-out duration-200`}
                      >
                        <span
                          className={`${
                            promotion.active
                              ? "opacity-0 ease-out duration-100"
                              : "opacity-100 ease-in duration-200"
                          } absolute inset-0 h-full w-full flex items-center justify-center transition-opacity`}
                        >
                          <X size={12} className="text-gray-400" />
                        </span>
                        <span
                          className={`${
                            promotion.active
                              ? "opacity-100 ease-in duration-200"
                              : "opacity-0 ease-out duration-100"
                          } absolute inset-0 h-full w-full flex items-center justify-center transition-opacity`}
                        >
                          <Check size={12} className="text-teal-600" />
                        </span>
                      </span>
                    </button>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button className="text-teal-600 hover:text-teal-900 mr-4">
                      <Edit size={18} />
                    </button>
                    <button className="text-red-600 hover:text-red-900">
                      <Trash2 size={18} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

// Orders Management Section
const OrdersManagement = ({ fetchData }) => {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">Orders Management</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">
          Manage your customer orders and deliveries.
        </p>
      </div>
    </div>
  );
};

// Stat Card Component with gradient backgrounds
const StatCard = ({ title, value, change, positive, icon, bgClass }) => {
  return (
    <div
      className={`bg-gradient-to-br ${
        bgClass || "from-white to-gray-50"
      } overflow-hidden shadow rounded-lg border border-gray-200`}
    >
      <div className="p-5">
        <div className="flex items-center">
          <div className="flex-shrink-0 p-2 rounded-md bg-white shadow-sm">
            {icon}
          </div>
          <div className="ml-5 w-0 flex-1">
            <dl>
              <dt className="text-sm font-medium text-gray-500 truncate">
                {title}
              </dt>
              <dd>
                <div className="text-lg font-medium text-gray-900">{value}</div>
              </dd>
            </dl>
          </div>
        </div>
      </div>
      <div className="bg-white bg-opacity-40 px-5 py-3 border-t border-gray-200">
        <div className="text-sm">
          <span
            className={`font-medium ${
              positive ? "text-green-600" : "text-red-600"
            } mr-2`}
          >
            {change}
          </span>
          <span className="text-gray-500">from last month</span>
        </div>
      </div>
    </div>
  );
};

// Helper function to get status color
const getStatusColor = (status) => {
  switch (status) {
    case "Delivered":
      return "bg-green-100 text-green-800";
    case "Processing":
      return "bg-blue-100 text-blue-800";
    case "Pending":
      return "bg-yellow-100 text-yellow-800";
    case "Shipped":
      return "bg-purple-100 text-purple-800";
    case "Cancelled":
      return "bg-red-100 text-red-800";
    default:
      return "bg-gray-100 text-gray-800";
  }
};

// Helper function to get text status color
const getTextStatusColor = (status) => {
  switch (status) {
    case "Delivered":
      return "text-green-600";
    case "Processing":
      return "text-blue-600";
    case "Pending":
      return "text-yellow-600";
    case "Shipped":
      return "text-purple-600";
    case "Cancelled":
      return "text-red-600";
    default:
      return "text-gray-600";
  }
};

// Mock data for demo purposes
const mockData = {
  dashboardStats: {
    totalSales: "£18,426.79",
    totalOrders: 356,
    productsSold: 1245,
    lowStockItems: 12,
  },
  recentOrders: [
    {
      id: "2631",
      customer: "John Smith",
      status: "Delivered",
      date: "15 Mar, 2023",
      total: "£86.32",
    },
    {
      id: "2630",
      customer: "Jane Doe",
      status: "Processing",
      date: "15 Mar, 2023",
      total: "£122.99",
    },
    {
      id: "2629",
      customer: "Robert Johnson",
      status: "Pending",
      date: "14 Mar, 2023",
      total: "£42.50",
    },
    {
      id: "2628",
      customer: "Emily Davis",
      status: "Shipped",
      date: "14 Mar, 2023",
      total: "£95.00",
    },
    {
      id: "2627",
      customer: "Michael Brown",
      status: "Cancelled",
      date: "13 Mar, 2023",
      total: "£65.75",
    },
  ],
  lowStockProducts: [
    {
      id: 1,
      name: "Organic Coffee Beans",
      sku: "CB-004",
      store: "Paris Store",
      stock: 5,
      threshold: 10,
    },
    {
      id: 2,
      name: "Premium Tea Selection",
      sku: "TE-021",
      store: "London Store",
      stock: 8,
      threshold: 15,
    },
    {
      id: 3,
      name: "Artisanal Chocolate",
      sku: "CH-089",
      store: "Berlin Store",
      stock: 3,
      threshold: 12,
    },
    {
      id: 4,
      name: "Specialty Olive Oil",
      sku: "OO-134",
      store: "Rome Store",
      stock: 4,
      threshold: 8,
    },
    {
      id: 5,
      name: "Gourmet Spice Set",
      sku: "SP-045",
      store: "Madrid Store",
      stock: 2,
      threshold: 10,
    },
  ],
  storePerformance: [
    {
      id: 1,
      name: "Paris Boutique",
      revenue: "£8,245",
      orders: 124,
      percentage: 85,
      changeUp: true,
      change: "12%",
    },
    {
      id: 2,
      name: "London Shoppe",
      revenue: "£6,128",
      orders: 98,
      percentage: 65,
      changeUp: true,
      change: "8%",
    },
    {
      id: 3,
      name: "Berlin Store",
      revenue: "£4,392",
      orders: 76,
      percentage: 45,
      changeUp: false,
      change: "3%",
    },
  ],
  categories: [
    {
      id: "cat-1",
      name: "Electronics",
      description: "Electronic devices and accessories",
      productCount: 45,
    },
    {
      id: "cat-2",
      name: "Clothing",
      description: "Fashion items for men, women, and children",
      productCount: 72,
    },
    {
      id: "cat-3",
      name: "Food & Beverages",
      description: "Gourmet food products and beverages",
      productCount: 38,
    },
    {
      id: "cat-4",
      name: "Home Decor",
      description: "Items to beautify your living space",
      productCount: 29,
    },
  ],
  products: [
    {
      id: 1,
      name: "Wireless Earbuds",
      sku: "WE-001",
      categoryId: "cat-1",
      storeId: "store-1",
      price: "£49.99",
      stock: "25",
      threshold: "10",
    },
    {
      id: 2,
      name: "Smart Watch",
      sku: "SW-002",
      categoryId: "cat-1",
      storeId: "store-2",
      price: "£129.99",
      stock: "15",
      threshold: "8",
    },
    {
      id: 3,
      name: "Cotton T-Shirt",
      sku: "CT-003",
      categoryId: "cat-2",
      storeId: "store-1",
      price: "£19.99",
      stock: "42",
      threshold: "20",
    },
    {
      id: 4,
      name: "Artisan Coffee Beans",
      sku: "CB-004",
      categoryId: "cat-3",
      storeId: "store-3",
      price: "£12.99",
      stock: "8",
      threshold: "10",
    },
    {
      id: 5,
      name: "Smartphone Case",
      sku: "SC-005",
      categoryId: "cat-1",
      storeId: "store-2",
      price: "£24.99",
      stock: "32",
      threshold: "15",
    },
  ],
  stores: [
    {
      id: "store-1",
      name: "Paris Boutique",
      address: "15 Rue de Rivoli",
      city: "Paris",
      postalCode: "75001",
      country: "France",
      contact: "+33 1 23 45 67 89",
      hours: "Mon-Sat: 10:00-19:00",
      productCount: 124,
    },
    {
      id: "store-2",
      name: "London Shoppe",
      address: "25 Oxford Street",
      city: "London",
      postalCode: "W1D 1BS",
      country: "UK",
      contact: "+44 20 1234 5678",
      hours: "Mon-Sun: 9:00-20:00",
      productCount: 98,
    },
    {
      id: "store-3",
      name: "Berlin Store",
      address: "10 Alexanderplatz",
      city: "Berlin",
      postalCode: "10178",
      country: "Germany",
      contact: "+49 30 1234 567",
      hours: "Mon-Fri: 9:00-18:00",
      productCount: 76,
    },
  ],
  deliveryStaff: [
    {
      id: "driver-1",
      firstName: "Jean",
      lastName: "Dubois",
      phone: "+33 6 12 34 56 78",
      location: "Paris",
      vehicle: "Electric Scooter",
      isActive: true,
    },
    {
      id: "driver-2",
      firstName: "James",
      lastName: "Smith",
      phone: "+44 7700 900123",
      location: "London",
      vehicle: "Bike",
      isActive: true,
    },
    {
      id: "driver-3",
      firstName: "Hans",
      lastName: "Mueller",
      phone: "+49 151 1234 5678",
      location: "Berlin",
      vehicle: "Electric Car",
      isActive: false,
    },
  ],
  promotions: [
    {
      id: "promo-1",
      name: "Summer Sale",
      discount: 20,
      productName: "All Clothing Items",
      period: "Jun 1 - Aug 31, 2023",
      active: true,
    },
    {
      id: "promo-2",
      name: "Welcome Discount",
      discount: 10,
      productName: "First Purchase",
      period: "Ongoing",
      active: true,
    },
    {
      id: "promo-3",
      name: "Holiday Special",
      discount: 15,
      productName: "Electronics",
      period: "Dec 1 - Dec 25, 2023",
      active: false,
    },
  ],
};

export default VendorDashboard;
