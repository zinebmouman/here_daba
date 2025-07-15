import React, { useState, useEffect } from "react";
import {
  Clock,
  MoreVertical,
  RefreshCw,
  Search,
  Calendar,
  Filter,
  ChevronDown,
} from "lucide-react";

interface Order {
  id: string;
  name: string;
  payment: "Cash" | "Paid";
  timeRemaining: string;
  type: "Delivery" | "Collection";
  status: "Delivered" | "Collected" | "Cancelled";
  total: string;
  image: string;
}

interface OrderManagementProps {
  // These will be used later for backend integration
  apiUrl?: string;
  token?: string;
}

const OrderManagement: React.FC<OrderManagementProps> = ({ apiUrl, token }) => {
  const [activeTab, setActiveTab] = useState<
    "All Order" | "Summary" | "Completed" | "Cancelled"
  >("All Order");
  const [startDate, setStartDate] = useState<string>("11-01-2021");
  const [endDate, setEndDate] = useState<string>("11-03-2021");
  const [actionMenuVisible, setActionMenuVisible] = useState<string | null>(
    null
  );
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [showFilters, setShowFilters] = useState<boolean>(false);
  const [selectedStatus, setSelectedStatus] = useState<string>("All");

  // Sample data for frontend development with avatar images
  const sampleOrders: Order[] = [
    {
      id: "#2632",
      name: "Brooklyn Zoe",
      payment: "Cash",
      timeRemaining: "13 min",
      type: "Delivery",
      status: "Delivered",
      total: "£12.00",
      image: "https://i.pravatar.cc/150?img=1",
    },
    {
      id: "#2632",
      name: "Alice Krejčová",
      payment: "Paid",
      timeRemaining: "49 min",
      type: "Collection",
      status: "Collected",
      total: "£14.00",
      image: "https://i.pravatar.cc/150?img=5",
    },
    {
      id: "#2632",
      name: "Jurriaan van",
      payment: "Cash",
      timeRemaining: "07 min",
      type: "Delivery",
      status: "Cancelled",
      total: "£18.00",
      image: "https://i.pravatar.cc/150?img=11",
    },
    {
      id: "#2632",
      name: "Ya Chin-Ho",
      payment: "Paid",
      timeRemaining: "49 min",
      type: "Collection",
      status: "Collected",
      total: "£26.00",
      image: "https://i.pravatar.cc/150?img=13",
    },
    {
      id: "#2632",
      name: "Shaamikh Al",
      payment: "Cash",
      timeRemaining: "13 min",
      type: "Delivery",
      status: "Delivered",
      total: "£08.00",
      image: "https://i.pravatar.cc/150?img=15",
    },
    {
      id: "#2632",
      name: "Niek Bove",
      payment: "Paid",
      timeRemaining: "00 min",
      type: "Collection",
      status: "Cancelled",
      total: "£15.00",
      image: "https://i.pravatar.cc/150?img=8",
    },
    {
      id: "#2632",
      name: "Uruewa Himona",
      payment: "Cash",
      timeRemaining: "15 min",
      type: "Delivery",
      status: "Delivered",
      total: "£19.00",
      image: "https://i.pravatar.cc/150?img=3",
    },
  ];

  useEffect(() => {
    // For frontend development, use sample data
    filterOrders();
  }, [activeTab, searchQuery, selectedStatus]);

  // Function to simulate fetching and filtering orders
  const filterOrders = () => {
    setLoading(true);

    // Simulate API call delay
    setTimeout(() => {
      let filteredOrders = [...sampleOrders];

      // Apply filtering based on active tab
      if (activeTab === "Completed") {
        filteredOrders = filteredOrders.filter(
          (order) =>
            order.status === "Delivered" || order.status === "Collected"
        );
      } else if (activeTab === "Cancelled") {
        filteredOrders = filteredOrders.filter(
          (order) => order.status === "Cancelled"
        );
      }

      // Apply search filter
      if (searchQuery) {
        const query = searchQuery.toLowerCase();
        filteredOrders = filteredOrders.filter(
          (order) =>
            order.name.toLowerCase().includes(query) ||
            order.id.toLowerCase().includes(query)
        );
      }

      // Apply status filter if not "All"
      if (selectedStatus !== "All") {
        filteredOrders = filteredOrders.filter(
          (order) => order.status === selectedStatus
        );
      }

      setOrders(filteredOrders);
      setLoading(false);
    }, 500); // Simulate network delay
  };

  // This will be replaced with actual API call later
  const fetchOrders = () => {
    setLoading(true);

    // Simulate API call
    setTimeout(() => {
      setOrders(sampleOrders);
      setLoading(false);
    }, 500);
  };

  // Placeholder for future backend integration
  const handleRefund = (orderId: string) => {
    console.log(`Processing refund for order ${orderId}`);
    // For now, just show success message
    const newOrders = orders.map((order) =>
      order.id === orderId ? { ...order, status: "Cancelled" } : order
    );
    setOrders(newOrders);
    alert(`Refund processed for order ${orderId}`);
  };

  // Placeholder for future backend integration
  const handleMessage = (orderId: string) => {
    console.log(`Send message for order ${orderId}`);
    // For now, just show info message
    alert(`Message feature for order ${orderId}`);
  };

  const handleTabClick = (
    tab: "All Order" | "Summary" | "Completed" | "Cancelled"
  ) => {
    setActiveTab(tab);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "Delivered":
        return "text-amber-500";
      case "Collected":
        return "text-gray-800";
      case "Cancelled":
        return "text-red-500";
      default:
        return "text-gray-800";
    }
  };

  const getStatusDot = (status: string) => {
    switch (status) {
      case "Delivered":
        return "bg-amber-500";
      case "Collected":
        return "bg-gray-800";
      case "Cancelled":
        return "bg-red-500";
      default:
        return "bg-gray-800";
    }
  };

  const getTypeColor = (type: string) => {
    return type === "Delivery" ? "text-red-500" : "text-gray-800";
  };

  const getTypeBackground = (type: string) => {
    return type === "Delivery" ? "bg-red-50" : "bg-gray-50";
  };

  const toggleActionMenu = (orderId: string) => {
    if (actionMenuVisible === orderId) {
      setActionMenuVisible(null);
    } else {
      setActionMenuVisible(orderId);
    }
  };

  return (
    <div className="max-w-5xl mx-auto bg-white p-6 rounded-lg shadow-sm">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Order Management</h1>
        <p className="text-gray-500">View and manage your customer orders</p>
      </div>

      {/* Error notification */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-md flex items-center justify-between">
          <span>{error}</span>
          <button
            onClick={() => setError(null)}
            className="text-red-500 hover:text-red-700"
          >
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b mb-6">
        {(["All Order", "Summary", "Completed", "Cancelled"] as const).map(
          (tab) => (
            <button
              key={tab}
              className={`py-3 px-5 text-sm font-medium relative ${
                activeTab === tab
                  ? "text-teal-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
              onClick={() => handleTabClick(tab)}
            >
              {tab}
              {activeTab === tab && (
                <span className="absolute bottom-0 left-0 w-full h-0.5 bg-teal-500 transform transition-all duration-200"></span>
              )}
            </button>
          )
        )}
      </div>

      {/* Search and Filter Bar */}
      <div className="flex flex-col lg:flex-row justify-between mb-6 gap-4">
        <div className="relative flex-1 max-w-md">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search size={18} className="text-gray-400" />
          </div>
          <input
            type="text"
            placeholder="Search orders by ID or customer name"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10 pr-4 py-2 w-full border border-gray-200 rounded-md focus:ring-teal-500 focus:border-teal-500 text-sm"
          />
        </div>

        <div className="flex gap-3">
          <div className="relative">
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="flex items-center gap-2 py-2 px-4 text-sm text-gray-600 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded-md transition-colors duration-200"
            >
              <Filter size={16} />
              <span>Filter</span>
              <ChevronDown
                size={16}
                className={`transform transition-transform duration-200 ${
                  showFilters ? "rotate-180" : ""
                }`}
              />
            </button>

            {showFilters && (
              <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg z-10 border border-gray-200 py-1">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 border-b">
                  Filter by Status
                </div>
                {["All", "Delivered", "Collected", "Cancelled"].map(
                  (status) => (
                    <button
                      key={status}
                      onClick={() => {
                        setSelectedStatus(status);
                        setShowFilters(false);
                      }}
                      className={`block w-full text-left px-4 py-2 text-sm ${
                        selectedStatus === status
                          ? "bg-teal-50 text-teal-600"
                          : "text-gray-700 hover:bg-gray-50"
                      }`}
                    >
                      {status}
                    </button>
                  )
                )}
              </div>
            )}
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Calendar size={16} className="text-gray-400" />
              </div>
              <input
                type="text"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="pl-10 py-2 w-28 text-sm border border-gray-200 rounded-md"
              />
            </div>
            <span className="text-gray-500">to</span>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Calendar size={16} className="text-gray-400" />
              </div>
              <input
                type="text"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="pl-10 py-2 w-28 text-sm border border-gray-200 rounded-md"
              />
            </div>
          </div>

          <button
            onClick={fetchOrders}
            className="flex items-center gap-2 py-2 px-4 text-sm text-white bg-teal-500 hover:bg-teal-600 rounded-md transition-colors duration-200"
          >
            <RefreshCw
              size={16}
              className={`${loading ? "animate-spin" : ""}`}
            />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-gradient-to-r from-teal-50 to-teal-100 p-4 rounded-lg shadow-sm">
          <div className="text-xs uppercase text-teal-600 font-semibold">
            Total Orders
          </div>
          <div className="mt-1 text-2xl font-bold text-teal-700">
            {loading ? "-" : orders.length}
          </div>
        </div>
        <div className="bg-gradient-to-r from-amber-50 to-amber-100 p-4 rounded-lg shadow-sm">
          <div className="text-xs uppercase text-amber-600 font-semibold">
            Completed
          </div>
          <div className="mt-1 text-2xl font-bold text-amber-700">
            {loading
              ? "-"
              : orders.filter(
                  (o) => o.status === "Delivered" || o.status === "Collected"
                ).length}
          </div>
        </div>
        <div className="bg-gradient-to-r from-red-50 to-red-100 p-4 rounded-lg shadow-sm">
          <div className="text-xs uppercase text-red-600 font-semibold">
            Cancelled
          </div>
          <div className="mt-1 text-2xl font-bold text-red-700">
            {loading
              ? "-"
              : orders.filter((o) => o.status === "Cancelled").length}
          </div>
        </div>
      </div>

      {/* Table Header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="grid grid-cols-7 py-3 px-4 bg-gray-50 text-sm font-medium text-gray-600 border-b">
          <div className="flex items-center gap-1">
            Order ID
            <svg
              className="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 9l-7 7-7-7"
              />
            </svg>
          </div>
          <div>Customer</div>
          <div>Payment</div>
          <div className="flex items-center gap-1">
            Time
            <svg
              className="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 9l-7 7-7-7"
              />
            </svg>
          </div>
          <div>Type</div>
          <div>Status</div>
          <div>Total</div>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="flex justify-center items-center p-12">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-500"></div>
          </div>
        )}

        {/* Empty State */}
        {!loading && orders.length === 0 && (
          <div className="p-12 text-center">
            <svg
              className="mx-auto h-12 w-12 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
              />
            </svg>
            <h3 className="mt-2 text-lg font-medium text-gray-900">
              No orders found
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              No orders match your current filters. Try changing your search
              criteria or selecting a different status.
            </p>
            <div className="mt-6">
              <button
                onClick={() => {
                  setSearchQuery("");
                  setSelectedStatus("All");
                  setActiveTab("All Order");
                }}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-500 hover:bg-teal-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500"
              >
                Reset Filters
              </button>
            </div>
          </div>
        )}

        {/* Order Rows */}
        {!loading &&
          orders.map((order, index) => (
            <div
              key={index}
              className="grid grid-cols-7 py-4 px-4 border-b text-sm items-center hover:bg-gray-50 transition-colors duration-150"
            >
              <div className="text-gray-700 font-medium">{order.id}</div>
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full overflow-hidden border border-gray-200">
                  <img
                    src={order.image}
                    alt={order.name}
                    className="w-full h-full object-cover"
                  />
                </div>
                <span className="font-medium text-gray-800">{order.name}</span>
              </div>
              <div
                className={`font-medium ${
                  order.payment === "Paid" ? "text-green-600" : "text-blue-600"
                }`}
              >
                {order.payment}
              </div>
              <div className="flex items-center gap-1 text-gray-600">
                <Clock
                  size={16}
                  className={
                    parseInt(order.timeRemaining) < 10
                      ? "text-red-500"
                      : "text-gray-400"
                  }
                />
                <span
                  className={
                    parseInt(order.timeRemaining) < 10
                      ? "font-medium text-red-500"
                      : ""
                  }
                >
                  {order.timeRemaining}
                </span>
              </div>
              <div
                className={`px-2 py-1 rounded-full text-xs font-medium inline-flex items-center ${getTypeBackground(
                  order.type
                )} ${getTypeColor(order.type)}`}
              >
                {order.type}
              </div>
              <div className="flex items-center gap-2">
                <div
                  className={`w-2.5 h-2.5 rounded-full ${getStatusDot(
                    order.status
                  )}`}
                ></div>
                <span className={`font-medium ${getStatusColor(order.status)}`}>
                  {order.status}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-bold">{order.total}</span>
                <div className="relative">
                  <button
                    onClick={() => toggleActionMenu(order.id + index)}
                    className="p-1.5 rounded-full text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                  >
                    <MoreVertical size={18} />
                  </button>
                  {actionMenuVisible === order.id + index && (
                    <div className="absolute right-0 mt-2 w-36 bg-white rounded-md shadow-lg z-10 border overflow-hidden">
                      <div className="py-1">
                        <button
                          onClick={() => {
                            handleRefund(order.id);
                            setActionMenuVisible(null);
                          }}
                          className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
                        >
                          <svg
                            className="w-4 h-4"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"
                            />
                          </svg>
                          Refund
                        </button>
                        <button
                          onClick={() => {
                            handleMessage(order.id);
                            setActionMenuVisible(null);
                          }}
                          className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
                        >
                          <svg
                            className="w-4 h-4"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
                            />
                          </svg>
                          Message
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}

        {/* Table Footer */}
        {!loading && orders.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 bg-gray-50 text-sm border-t">
            <div className="text-gray-600">
              Showing <span className="font-medium">{orders.length}</span>{" "}
              orders
            </div>
            <div className="flex gap-1">
              <button
                className="px-2 py-1 border border-gray-300 rounded-md text-gray-600 hover:bg-gray-100 disabled:opacity-50"
                disabled
              >
                Previous
              </button>
              <button
                className="px-2 py-1 border border-gray-300 rounded-md text-gray-600 hover:bg-gray-100 disabled:opacity-50"
                disabled
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default OrderManagement;
