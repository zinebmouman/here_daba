import React from "react";
import {
  Smartphone,
  ShoppingBag,
  Home,
  Music,
  Book,
  Gift,
  Coffee,
  Utensils,
  Tv,
  Shirt,
  Cpu,
  Tag,
  Headphones,
  Watch,
  Car,
  Plane,
  Baby,
  Camera,
  PenTool,
  Hammer,
  Briefcase,
} from "lucide-react";

const CategoryIcon = ({ category, size = 18, className = "" }) => {
  // Function to get the correct icon component based on the icon name
  const getIconComponent = (iconName) => {
    switch (iconName) {
      case "smartphone":
        return <Smartphone size={size} />;
      case "shopping-bag":
        return <ShoppingBag size={size} />;
      case "home":
        return <Home size={size} />;
      case "music":
        return <Music size={size} />;
      case "book":
        return <Book size={size} />;
      case "gift":
        return <Gift size={size} />;
      case "coffee":
        return <Coffee size={size} />;
      case "utensils":
        return <Utensils size={size} />;
      case "tv":
        return <Tv size={size} />;
      case "shirt":
        return <Shirt size={size} />;
      case "cpu":
        return <Cpu size={size} />;
      case "headphones":
        return <Headphones size={size} />;
      case "watch":
        return <Watch size={size} />;
      case "car":
        return <Car size={size} />;
      case "plane":
        return <Plane size={size} />;
      case "baby":
        return <Baby size={size} />;
      case "camera":
        return <Camera size={size} />;
      case "pen-tool":
        return <PenTool size={size} />;
      case "hammer":
        return <Hammer size={size} />;
      case "briefcase":
        return <Briefcase size={size} />;
      default:
        return <Tag size={size} />;
    }
  };

  // Compute background and text colors based on icon name
  const getColorClasses = (iconName) => {
    switch (iconName) {
      case "smartphone":
        return "bg-blue-100 text-blue-600";
      case "shopping-bag":
        return "bg-purple-100 text-purple-600";
      case "home":
        return "bg-yellow-100 text-yellow-600";
      case "music":
        return "bg-indigo-100 text-indigo-600";
      case "book":
        return "bg-red-100 text-red-600";
      case "gift":
        return "bg-pink-100 text-pink-600";
      case "coffee":
        return "bg-amber-100 text-amber-600";
      case "utensils":
        return "bg-green-100 text-green-600";
      case "tv":
        return "bg-orange-100 text-orange-600";
      case "shirt":
        return "bg-purple-100 text-purple-600";
      case "cpu":
        return "bg-cyan-100 text-cyan-600";
      case "headphones":
        return "bg-indigo-100 text-indigo-600";
      case "watch":
        return "bg-gray-100 text-gray-600";
      case "car":
        return "bg-blue-100 text-blue-600";
      case "plane":
        return "bg-sky-100 text-sky-600";
      case "baby":
        return "bg-pink-100 text-pink-600";
      case "camera":
        return "bg-purple-100 text-purple-600";
      case "pen-tool":
        return "bg-violet-100 text-violet-600";
      case "hammer":
        return "bg-slate-100 text-slate-600";
      case "briefcase":
        return "bg-gray-100 text-gray-600";
      default:
        return "bg-teal-100 text-teal-600";
    }
  };

  const iconName = category?.icon || "tag";
  const colorClasses = getColorClasses(iconName);
  const baseClasses =
    "inline-flex items-center justify-center h-10 w-10 rounded-md";

  return (
    <div className={`${baseClasses} ${colorClasses} ${className}`}>
      {getIconComponent(iconName)}
    </div>
  );
};

export default CategoryIcon;
