import React from "react";
import {
  DollarSign,
  ShoppingBag,
  Package,
  AlertTriangle,
  Activity,
} from "lucide-react";

const StatCard = ({ title, value, change, positive, icon, bgClass }) => {
  // Function to render the appropriate icon
  const renderIcon = () => {
    switch (icon) {
      case "dollar":
        return <DollarSign size={20} className="text-green-500" />;
      case "shopping-bag":
        return <ShoppingBag size={20} className="text-blue-500" />;
      case "package":
        return <Package size={20} className="text-purple-500" />;
      case "alert-triangle":
        return <AlertTriangle size={20} className="text-amber-500" />;
      default:
        return <Activity size={20} className="text-gray-500" />;
    }
  };

  return (
    <div
      className={`bg-gradient-to-br ${
        bgClass || "from-white to-gray-50"
      } overflow-hidden shadow rounded-lg border border-gray-200`}
    >
      <div className="p-5">
        <div className="flex items-center">
          <div className="flex-shrink-0 p-2 rounded-md bg-white shadow-sm">
            {renderIcon()}
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

export default StatCard;
