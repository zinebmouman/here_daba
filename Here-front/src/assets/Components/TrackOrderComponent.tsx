import React, { useState } from "react";
import {
  Search,
  Package,
  MapPin,
  Truck,
  CheckCircle,
  Clock,
  ChevronDown,
  ChevronUp,
  Calendar,
  Phone,
  AlertTriangle,
  AlertCircle,
  RefreshCw,
  Navigation,
  MessageSquare,
} from "lucide-react";

const TrackOrderComponent = () => {
  const [orderNumber, setOrderNumber] = useState("");
  const [loading, setLoading] = useState(false);
  const [trackingResult, setTrackingResult] = useState(null);
  const [activeStep, setActiveStep] = useState(2); // Current delivery step (for demo: 0-3)
  const [showOrderDetails, setShowOrderDetails] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [error, setError] = useState(null);

  // Sample tracking data for demonstration - this would come from your backend API
  const sampleTrackingData = {
    orderNumber: "ORD-2632-8544",
    customerName: "Brooklyn Zoe",
    orderDate: "10 March, 2023",
    estimatedDelivery: "15 March, 2023",
    total: "£42.80",
    paymentMethod: "Credit Card",
    address: "24 Oxford Street, London, W1D 1AP",
    phone: "+44 7700 900123",
    items: [
      { name: "Classic White T-Shirt", quantity: 2, price: "£12.99" },
      { name: "Denim Jeans - Blue", quantity: 1, price: "£29.99" },
    ],
    trackingHistory: [
      {
        status: "Order Placed",
        date: "10 March, 2023",
        time: "10:24 AM",
        completed: true,
        notes: "Order confirmed and payment received",
      },
      {
        status: "Order Processed",
        date: "11 March, 2023",
        time: "9:15 AM",
        completed: true,
        notes: "Your order has been packed and is ready for shipping",
      },
      {
        status: "Out for Delivery",
        date: "15 March, 2023",
        time: "8:30 AM",
        completed: true,
        notes: "Your order is on its way to you",
      },
      {
        status: "Delivered",
        date: "Expected Today",
        time: "By 8:00 PM",
        completed: false,
        notes: "Estimated delivery time may vary based on traffic",
      },
    ],
    courierName: "Swift Express",
    courierTracking: "SWF89532175",
    driverName: "Michael Smith",
    driverPhone: "+44 7700 900456",
    currentLocation: { lat: 51.515, lng: -0.141 }, // London coordinates for demo
    destinationLocation: { lat: 51.518, lng: -0.129 }, // Oxford Street for demo
    estimatedTimeRemaining: "35 minutes",
    deliveryDistance: "1.3 miles",
    deliveryFee: "£2.99",
    subtotal: "£39.81",
  };

  // This will simulate a real API integration
  const fetchOrderTracking = (orderNumber) => {
    setLoading(true);
    setError(null);

    // Simulate API call with timeout
    setTimeout(() => {
      try {
        // In a real implementation, you would fetch from your API:
        // const response = await fetch(`/api/tracking/${orderNumber}`);
        // const data = await response.json();

        // For now, use sample data
        setTrackingResult(sampleTrackingData);
        setLastUpdated(new Date());
        setLoading(false);
      } catch (err) {
        setError("Failed to load tracking information. Please try again.");
        setLoading(false);
      }
    }, 1000);
  };

  // Function to refresh tracking data
  const refreshTracking = () => {
    if (!trackingResult) return;

    setLoading(true);

    // Simulate API refresh with delay
    setTimeout(() => {
      // Simulate a small position change
      const updatedData = {
        ...sampleTrackingData,
        estimatedTimeRemaining: "28 minutes", // Updated time
      };

      setTrackingResult(updatedData);
      setLastUpdated(new Date());
      setLoading(false);
    }, 700);
  };

  // Format the last updated time
  const formatLastUpdated = () => {
    if (!lastUpdated) return "";

    const now = new Date();
    const diffMs = now.getTime() - lastUpdated.getTime();
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) return "just now";
    if (diffMins === 1) return "1 minute ago";
    return `${diffMins} minutes ago`;
  };

  // Handle form submission to track order
  const handleTrackOrder = (e) => {
    e.preventDefault();
    if (!orderNumber) return;

    fetchOrderTracking(orderNumber);
  };

  // Contact courier function - in a real app, this would open a modal or call functionality
  const handleContactCourier = () => {
    if (!trackingResult) return;
    alert(
      `Contacting courier: ${trackingResult.driverName} at ${trackingResult.driverPhone}`
    );
    // You would implement actual contact functionality here
  };

  // Report issue function - in a real app, this would open a support form
  const handleReportIssue = () => {
    if (!trackingResult) return;
    alert(`Reporting issue for order: ${trackingResult.orderNumber}`);
    // You would implement actual issue reporting here
  };

  // For demonstration - in a real app you would replace this with actual Google Maps integration
  const MapPlaceholder = () => (
    <div className="relative h-56 sm:h-64 lg:h-80 w-full bg-gray-50 rounded-lg overflow-hidden border border-gray-200">
      {/* Google Maps placeholder */}
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <div className="text-center px-4">
          <AlertTriangle size={28} className="mx-auto mb-2 text-amber-500" />
          <h3 className="text-lg font-semibold text-gray-800 mb-1">
            Google Maps Not Available
          </h3>
          <p className="text-sm text-gray-600 mb-4">
            Live tracking map will be available when you add Google Maps API
            key.
          </p>
          <div className="bg-gray-100 p-3 rounded-md text-xs text-left font-mono">
            <code>// To enable maps, add your API key in the component</code>
          </div>
        </div>
      </div>

      {/* Map abstract styling - this will be behind the placeholder message */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden opacity-10">
        {/* Abstract map elements */}
        <div className="absolute top-1/4 left-0 right-0 h-1 bg-gray-400"></div>
        <div className="absolute top-2/3 left-0 right-0 h-1 bg-gray-400"></div>
        <div className="absolute bottom-1/4 left-0 right-0 h-1 bg-gray-400"></div>
        <div className="absolute left-1/3 top-0 bottom-0 w-1 bg-gray-400"></div>
        <div className="absolute right-1/4 top-0 bottom-0 w-1 bg-gray-400"></div>
      </div>

      {/* Route info overlay */}
      <div className="absolute bottom-3 left-3 sm:bottom-4 sm:left-4 bg-white rounded-md shadow-md p-2 sm:p-3">
        <div className="flex items-center gap-1.5 sm:gap-2">
          <Navigation size={14} className="sm:w-4 sm:h-4 text-gray-600" />
          <span className="text-xs sm:text-sm font-medium">
            Distance remaining
          </span>
        </div>
        <div className="text-base sm:text-lg font-bold text-gray-800 mt-0.5">
          {trackingResult?.deliveryDistance || "1.3 miles"}
        </div>
      </div>

      {/* ETA overlay */}
      <div className="absolute top-3 right-3 sm:top-4 sm:right-4 bg-white rounded-md shadow-md p-2 sm:p-3">
        <div className="flex items-center gap-1.5 sm:gap-2">
          <Clock size={14} className="sm:w-4 sm:h-4 text-gray-600" />
          <span className="text-xs sm:text-sm font-medium">
            Estimated arrival
          </span>
        </div>
        <div className="text-base sm:text-lg font-bold text-teal-600 mt-0.5">
          {trackingResult?.estimatedTimeRemaining || "35 minutes"}
        </div>
      </div>
    </div>
  );

  return (
    <div className="max-w-5xl mx-auto bg-white p-4 sm:p-6 rounded-lg shadow-sm">
      <div className="mb-4 sm:mb-6">
        <h1 className="text-xl sm:text-2xl font-bold text-gray-800">
          Track Your Order
        </h1>
        <p className="text-sm sm:text-base text-gray-500">
          Enter your order number to track your package in real-time
        </p>
      </div>

      {/* Error message */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-md flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
          <button
            onClick={() => setError(null)}
            className="text-red-500 hover:text-red-700 p-1"
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

      {/* Order Tracking Form */}
      {!trackingResult && (
        <div className="bg-gray-50 rounded-lg p-4 sm:p-6 border border-gray-200 max-w-xl mx-auto">
          <form onSubmit={handleTrackOrder} className="space-y-4">
            <div>
              <label
                htmlFor="orderNumber"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Order Number
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Package
                    size={16}
                    className="sm:w-[18px] sm:h-[18px] text-gray-400"
                  />
                </div>
                <input
                  type="text"
                  id="orderNumber"
                  value={orderNumber}
                  onChange={(e) => setOrderNumber(e.target.value)}
                  placeholder="Enter your order number"
                  className="pl-9 sm:pl-10 py-2.5 sm:py-3 text-sm block w-full border border-gray-200 rounded-md focus:ring-teal-500 focus:border-teal-500"
                />
              </div>
              <p className="mt-1 text-xs sm:text-sm text-gray-500">
                You can find your order number in your confirmation email.
              </p>
            </div>

            <button
              type="submit"
              disabled={loading || !orderNumber}
              className={`w-full flex items-center justify-center py-2.5 sm:py-3 px-4 border border-transparent rounded-md shadow-sm text-white bg-teal-500 ${
                loading || !orderNumber
                  ? "opacity-70 cursor-not-allowed"
                  : "hover:bg-teal-600"
              } focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500`}
            >
              {loading ? (
                <>
                  <div className="animate-spin h-4 w-4 sm:h-5 sm:w-5 mr-2 border-2 border-white border-t-transparent rounded-full"></div>
                  <span className="text-sm sm:text-base">Tracking...</span>
                </>
              ) : (
                <>
                  <Search size={16} className="sm:w-[18px] sm:h-[18px] mr-2" />
                  <span className="text-sm sm:text-base">Track Order</span>
                </>
              )}
            </button>
          </form>

          <div className="mt-6 sm:mt-8">
            <h2 className="text-base sm:text-lg font-medium text-gray-700 mb-2 sm:mb-3">
              Need Help?
            </h2>
            <div className="flex flex-col gap-3">
              <div className="flex items-start gap-2 sm:gap-3">
                <div className="mt-1 bg-teal-100 text-teal-600 p-1 sm:p-1.5 rounded-md">
                  <Package size={16} className="sm:w-[18px] sm:h-[18px]" />
                </div>
                <div>
                  <h3 className="text-sm sm:text-base font-medium text-gray-800">
                    Can't find your order?
                  </h3>
                  <p className="text-xs sm:text-sm text-gray-500">
                    Check your email for your order confirmation and tracking
                    number.
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-2 sm:gap-3">
                <div className="mt-1 bg-teal-100 text-teal-600 p-1 sm:p-1.5 rounded-md">
                  <Phone size={16} className="sm:w-[18px] sm:h-[18px]" />
                </div>
                <div>
                  <h3 className="text-sm sm:text-base font-medium text-gray-800">
                    Contact Support
                  </h3>
                  <p className="text-xs sm:text-sm text-gray-500">
                    Our team is available Mon-Fri, 9am-5pm at
                    support@yourstore.com.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tracking Results */}
      {trackingResult && (
        <div className="space-y-4 sm:space-y-6">
          {/* Tracking Header */}
          <div className="bg-gray-50 rounded-lg p-4 sm:p-6 border border-gray-200">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="border-b pb-3 sm:border-b-0 sm:pb-0">
                <span className="text-xs sm:text-sm font-medium text-gray-500">
                  ORDER NUMBER
                </span>
                <h2 className="text-base sm:text-lg font-bold text-gray-800">
                  {trackingResult.orderNumber}
                </h2>
              </div>

              <div className="border-b pb-3 sm:border-b-0 sm:pb-0">
                <span className="text-xs sm:text-sm font-medium text-gray-500">
                  ESTIMATED DELIVERY
                </span>
                <h2 className="text-base sm:text-lg font-bold text-gray-800">
                  {trackingResult.estimatedDelivery}
                </h2>
              </div>

              <div className="border-b pb-3 sm:border-b-0 sm:pb-0">
                <span className="text-xs sm:text-sm font-medium text-gray-500">
                  COURIER
                </span>
                <h2 className="text-base sm:text-lg font-bold text-gray-800">
                  {trackingResult.courierName}
                </h2>
                <p className="text-xs sm:text-sm text-gray-500">
                  Tracking: {trackingResult.courierTracking}
                </p>
              </div>

              <div className="flex items-center sm:justify-end">
                <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
                  <button
                    onClick={refreshTracking}
                    disabled={loading}
                    className="flex items-center justify-center px-3 py-2 text-xs sm:text-sm text-teal-600 border border-teal-200 rounded-md hover:bg-teal-50 disabled:opacity-50"
                  >
                    <RefreshCw
                      size={14}
                      className={`mr-1.5 ${loading ? "animate-spin" : ""}`}
                    />
                    Refresh
                  </button>
                  <button
                    onClick={() => {
                      setTrackingResult(null);
                      setOrderNumber("");
                    }}
                    className="flex items-center justify-center px-3 py-2 text-xs sm:text-sm text-gray-600 border border-gray-200 rounded-md hover:bg-gray-50"
                  >
                    Track Different Order
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Order Status Steps */}
          <div className="relative">
            <div className="absolute left-5 sm:left-8 top-0 bottom-0 w-0.5 bg-gray-200 z-0"></div>

            {trackingResult.trackingHistory.map((step, index) => (
              <div
                key={index}
                className={`relative z-10 flex mb-6 sm:mb-8 ${
                  index === trackingResult.trackingHistory.length - 1
                    ? "mb-0"
                    : ""
                }`}
              >
                <div className="flex-shrink-0 flex items-center justify-center h-10 w-10 sm:h-16 sm:w-16 rounded-full border-2 bg-white mr-3 sm:mr-4">
                  {step.completed ? (
                    <div className="bg-teal-500 text-white p-1.5 sm:p-2 rounded-full">
                      <CheckCircle size={16} className="sm:w-6 sm:h-6" />
                    </div>
                  ) : (
                    <div className="bg-gray-100 text-gray-400 p-1.5 sm:p-2 rounded-full">
                      <Clock size={16} className="sm:w-6 sm:h-6" />
                    </div>
                  )}
                </div>

                <div
                  className={`pt-1 sm:pt-2 ${
                    index === activeStep ? "opacity-100" : "opacity-80"
                  }`}
                >
                  <p
                    className={`text-base sm:text-lg font-bold ${
                      step.completed ? "text-teal-600" : "text-gray-500"
                    }`}
                  >
                    {step.status}
                  </p>
                  <p className="text-xs sm:text-sm text-gray-600">
                    {step.date} • {step.time}
                  </p>

                  {step.notes && (
                    <p className="text-xs sm:text-sm text-gray-500 mt-1">
                      {step.notes}
                    </p>
                  )}

                  {index === 2 && (
                    <div className="mt-2 bg-teal-50 rounded-md p-2 sm:p-3 border border-teal-100">
                      <div className="flex items-start gap-2 sm:gap-3">
                        <div className="mt-0.5 text-teal-500">
                          <Truck size={16} className="sm:w-5 sm:h-5" />
                        </div>
                        <div>
                          <p className="text-xs sm:text-sm font-medium text-teal-800">
                            Your delivery is on the way!
                          </p>
                          <p className="text-xs sm:text-sm text-teal-600">
                            Courier: {trackingResult.driverName} •{" "}
                            {trackingResult.driverPhone}
                          </p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Live Tracking Map */}
          <div className="mt-4 sm:mt-6">
            <div className="flex justify-between items-center mb-2 sm:mb-3">
              <h3 className="text-base sm:text-lg font-bold text-gray-800">
                Live Tracking
              </h3>
              {lastUpdated && (
                <p className="text-xs text-gray-500">
                  Last updated: {formatLastUpdated()}
                </p>
              )}
            </div>
            <MapPlaceholder />
            <p className="mt-2 text-xs sm:text-sm text-gray-500 text-center">
              Full map functionality will be available when Google Maps API is
              connected.
            </p>
          </div>

          {/* Order Details (Collapsible) */}
          <div className="mt-4 sm:mt-6 border border-gray-200 rounded-lg overflow-hidden">
            <button
              onClick={() => setShowOrderDetails(!showOrderDetails)}
              className="w-full px-4 sm:px-6 py-3 sm:py-4 bg-gray-50 flex justify-between items-center"
            >
              <span className="text-sm sm:text-base font-semibold text-gray-700">
                Order Details
              </span>
              {showOrderDetails ? (
                <ChevronUp size={18} className="sm:w-5 sm:h-5 text-gray-500" />
              ) : (
                <ChevronDown
                  size={18}
                  className="sm:w-5 sm:h-5 text-gray-500"
                />
              )}
            </button>

            {showOrderDetails && (
              <div className="p-4 sm:p-6 bg-white">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Customer Details */}
                  <div>
                    <h4 className="text-sm sm:text-base font-medium text-gray-700 mb-2">
                      Customer Information
                    </h4>
                    <div className="space-y-3">
                      <div className="flex gap-2">
                        <div className="w-5 sm:w-6 flex-shrink-0 text-gray-400">
                          <Calendar
                            size={16}
                            className="sm:w-[18px] sm:h-[18px]"
                          />
                        </div>
                        <div>
                          <p className="text-xs sm:text-sm text-gray-500">
                            Order Date
                          </p>
                          <p className="text-sm sm:text-base font-medium">
                            {trackingResult.orderDate}
                          </p>
                        </div>
                      </div>

                      <div className="flex gap-2">
                        <div className="w-5 sm:w-6 flex-shrink-0 text-gray-400">
                          <MapPin
                            size={16}
                            className="sm:w-[18px] sm:h-[18px]"
                          />
                        </div>
                        <div>
                          <p className="text-xs sm:text-sm text-gray-500">
                            Delivery Address
                          </p>
                          <p className="text-sm sm:text-base font-medium">
                            {trackingResult.address}
                          </p>
                        </div>
                      </div>

                      <div className="flex gap-2">
                        <div className="w-5 sm:w-6 flex-shrink-0 text-gray-400">
                          <Phone
                            size={16}
                            className="sm:w-[18px] sm:h-[18px]"
                          />
                        </div>
                        <div>
                          <p className="text-xs sm:text-sm text-gray-500">
                            Contact Number
                          </p>
                          <p className="text-sm sm:text-base font-medium">
                            {trackingResult.phone}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Order Items */}
                  <div className="mt-4 md:mt-0">
                    <h4 className="text-sm sm:text-base font-medium text-gray-700 mb-2">
                      Order Summary
                    </h4>
                    <div className="space-y-3">
                      {trackingResult.items.map((item, index) => (
                        <div
                          key={index}
                          className="flex justify-between text-xs sm:text-sm"
                        >
                          <div className="flex-1 pr-2">
                            <p className="font-medium">{item.name}</p>
                            <p className="text-gray-500">
                              Quantity: {item.quantity}
                            </p>
                          </div>
                          <p className="font-medium">{item.price}</p>
                        </div>
                      ))}

                      <div className="border-t border-gray-200 pt-3 mt-3">
                        <div className="flex justify-between text-xs sm:text-sm">
                          <p className="text-gray-500">Subtotal</p>
                          <p className="font-medium">
                            {trackingResult.subtotal}
                          </p>
                        </div>
                        <div className="flex justify-between text-xs sm:text-sm mt-1">
                          <p className="text-gray-500">Delivery Fee</p>
                          <p className="font-medium">
                            {trackingResult.deliveryFee}
                          </p>
                        </div>
                        <div className="flex justify-between mt-2">
                          <p className="text-sm sm:text-base font-medium">
                            Total
                          </p>
                          <p className="text-sm sm:text-base font-bold text-teal-600">
                            {trackingResult.total}
                          </p>
                        </div>
                        <div className="text-xs sm:text-sm text-gray-500 mt-1">
                          Paid by {trackingResult.paymentMethod}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Help & Support */}
          <div className="mt-4 sm:mt-6">
            <div className="flex flex-col sm:flex-row gap-4 justify-between items-center p-4 bg-gray-50 rounded-lg border border-gray-200">
              <div className="text-center sm:text-left">
                <p className="text-sm sm:text-base text-gray-700 font-medium">
                  Need help with your delivery?
                </p>
                <p className="text-xs sm:text-sm text-gray-500">
                  Our support team is here to help if you have any issues
                </p>
              </div>
              <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
                <button
                  onClick={handleContactCourier}
                  className="flex items-center justify-center py-2 px-4 text-sm bg-white text-teal-600 border border-teal-200 rounded-md hover:bg-teal-50 sm:font-medium"
                >
                  <Phone size={16} className="mr-1.5" />
                  Contact Courier
                </button>
                <button
                  onClick={handleReportIssue}
                  className="flex items-center justify-center py-2 px-4 text-sm bg-white text-gray-600 border border-gray-200 rounded-md hover:bg-gray-50 sm:font-medium"
                >
                  <MessageSquare size={16} className="mr-1.5" />
                  Report an Issue
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TrackOrderComponent;
