import React from "react";
import { AlertTriangle } from "lucide-react";

const LowStockAlert = ({ products = [] }) => {
  return (
    <div className="bg-white rounded-lg shadow overflow-hidden border border-red-100">
      <div className="px-4 py-5 border-b border-gray-200 bg-gradient-to-r from-red-50 to-amber-50">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-medium leading-6 text-gray-900 flex items-center">
            <AlertTriangle size={18} className="text-red-500 mr-2" />
            Low Stock Alert
          </h3>
          <span className="bg-red-100 text-red-800 text-xs font-semibold py-1 px-2 rounded-full">
            {products.length} Items
          </span>
        </div>
        <p className="mt-1 text-sm text-gray-500">
          Products below critical threshold
        </p>
      </div>

      {products.length > 0 ? (
        <ul className="divide-y divide-gray-200 max-h-[420px] overflow-y-auto">
          {products.map((product) => (
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
      ) : (
        <div className="px-4 py-6 text-center">
          <p className="text-gray-500">No low stock items at the moment.</p>
        </div>
      )}

      <div className="bg-gray-50 px-4 py-3 border-t border-gray-200 text-center">
        <button
          onClick={() => (window.location.href = "/account/inventory")}
          className="text-sm font-medium text-teal-600 hover:text-teal-500"
        >
          View all inventory
        </button>
      </div>
    </div>
  );
};

export default LowStockAlert;
