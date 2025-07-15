import React from "react";

const StorePerformance = ({ stores = [] }) => {
  return (
    <div className="bg-gradient-to-br from-white to-teal-50 rounded-lg shadow overflow-hidden border border-teal-100">
      <div className="p-5 border-b border-teal-100">
        <h3 className="text-lg font-medium text-gray-900">Store Performance</h3>
        <div className="mt-1 text-sm text-gray-500">
          Top 3 performing stores this month
        </div>
      </div>

      {stores.length > 0 ? (
        <div className="px-5 py-4">
          <div className="space-y-4">
            {stores.map((store, index) => (
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
      ) : (
        <div className="px-5 py-6 text-center">
          <p className="text-gray-500">No store performance data available.</p>
        </div>
      )}

      <div className="bg-teal-50 px-5 py-3 border-t border-teal-100">
        <button
          onClick={() => (window.location.href = "/account/stores")}
          className="text-sm font-medium text-teal-700 hover:text-teal-500"
        >
          View all stores →
        </button>
      </div>
    </div>
  );
};

export default StorePerformance;
