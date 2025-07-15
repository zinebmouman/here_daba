import React, { useState, useEffect, useRef } from "react";
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
  Upload,
  AlertCircle,
  CheckCircle,
} from "lucide-react";
import { auth } from "../../../config/Firebase";
import CategoryIcon from "./CategoryIcon";

const CategoryForm = ({ category, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    icon: "tag", // Default icon
    customIcon: null, // For custom SVG icon
  });

  const [suggestedIcons, setSuggestedIcons] = useState([]);
  const [showIconSuggestions, setShowIconSuggestions] = useState(false);
  const [customIconPreview, setCustomIconPreview] = useState(null);
  const [customIconError, setCustomIconError] = useState("");
  const [activeTab, setActiveTab] = useState("suggested"); // "suggested", "all", or "custom"
  const [isSubmitting, setIsSubmitting] = useState(false);
  const fileInputRef = useRef(null);

  // Available icons with their related keywords
  const iconOptions = [
    {
      name: "smartphone",
      icon: <Smartphone />,
      keywords: ["electronic", "phone", "mobile", "device", "tech", "gadget"],
    },
    {
      name: "shopping-bag",
      icon: <ShoppingBag />,
      keywords: ["shopping", "retail", "purchase", "bag", "buy"],
    },
    {
      name: "home",
      icon: <Home />,
      keywords: ["home", "house", "building", "decor", "furniture", "living"],
    },
    {
      name: "music",
      icon: <Music />,
      keywords: ["music", "song", "audio", "sound", "instrument"],
    },
    {
      name: "book",
      icon: <Book />,
      keywords: ["book", "read", "literature", "novel", "education"],
    },
    {
      name: "gift",
      icon: <Gift />,
      keywords: ["gift", "present", "birthday", "holiday", "christmas"],
    },
    {
      name: "coffee",
      icon: <Coffee />,
      keywords: ["coffee", "tea", "drink", "beverage", "cafe"],
    },
    {
      name: "utensils",
      icon: <Utensils />,
      keywords: ["food", "kitchen", "cook", "meal", "restaurant", "dining"],
    },
    {
      name: "tv",
      icon: <Tv />,
      keywords: [
        "tv",
        "television",
        "entertainment",
        "media",
        "screen",
        "movie",
      ],
    },
    {
      name: "shirt",
      icon: <Shirt />,
      keywords: ["clothing", "fashion", "apparel", "shirt", "dress", "wear"],
    },
    {
      name: "cpu",
      icon: <Cpu />,
      keywords: ["computer", "hardware", "technology", "laptop", "processor"],
    },
    {
      name: "tag",
      icon: <Tag />,
      keywords: ["general", "category", "tag", "label", "price"],
    },
    {
      name: "headphones",
      icon: <Headphones />,
      keywords: ["audio", "headphones", "music", "sound", "listening"],
    },
    {
      name: "watch",
      icon: <Watch />,
      keywords: ["watch", "time", "accessory", "wearable", "clock"],
    },
    {
      name: "car",
      icon: <Car />,
      keywords: ["car", "auto", "vehicle", "transportation", "drive"],
    },
    {
      name: "plane",
      icon: <Plane />,
      keywords: ["travel", "flight", "vacation", "tourism", "airplane"],
    },
    {
      name: "baby",
      icon: <Baby />,
      keywords: ["baby", "child", "kid", "infant", "toddler"],
    },
    {
      name: "camera",
      icon: <Camera />,
      keywords: ["camera", "photo", "photography", "picture", "image"],
    },
    {
      name: "pen-tool",
      icon: <PenTool />,
      keywords: ["art", "design", "creative", "drawing", "stationery"],
    },
    {
      name: "hammer",
      icon: <Hammer />,
      keywords: ["tools", "hardware", "diy", "construction", "repair"],
    },
    {
      name: "briefcase",
      icon: <Briefcase />,
      keywords: ["business", "work", "office", "professional", "career"],
    },
  ];

  // Initialize form data when editing a category
  useEffect(() => {
    console.log("Category data loaded:", category);
    
    if (category) {
      setFormData({
        name: category.nom || "",
        description: category.description || "",
        icon: category.icon || "tag",
        customIcon: category.customIcon || null,
      });

      // If category has custom icon, set preview
      if (category.customIcon) {
        setCustomIconPreview(category.customIcon);
        setActiveTab("custom");
      }

      updateSuggestedIcons(category.nom || "");
    }
  }, [category]);

  // Update suggested icons based on category name input
  const updateSuggestedIcons = (categoryName) => {
    if (!categoryName) {
      setSuggestedIcons([]);
      return;
    }

    const lowercaseName = categoryName.toLowerCase();

    // Find matching icons based on keywords
    const matchingIcons = iconOptions.filter((option) => {
      // Check if category name contains any of the icon's keywords
      return option.keywords.some((keyword) => lowercaseName.includes(keyword));
    });

    // Always include default icon if it's not already in the list
    const defaultIcon = iconOptions.find((option) => option.name === "tag");

    // Combine matching icons with default
    const uniqueIcons = [...new Set([...matchingIcons, defaultIcon])];

    // Limit to top 5 suggestions
    setSuggestedIcons(uniqueIcons.slice(0, 5));
  };

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    console.log(`Input changed: ${name} = ${value}`);
    setFormData({ ...formData, [name]: value });

    // When category name changes, update icon suggestions
    if (name === "name") {
      updateSuggestedIcons(value);
      setShowIconSuggestions(true);
    }
  };

  // Handle icon selection
  const handleIconSelect = (iconName) => {
    setFormData({ ...formData, icon: iconName, customIcon: null });
    setCustomIconPreview(null);
    setCustomIconError("");
  };

  // Handle custom icon upload
  const handleCustomIconUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Check file type
    if (!file.type.includes("image/")) {
      setCustomIconError("Please upload an image file");
      return;
    }

    // Check file size (max 2MB)
    if (file.size > 2 * 1024 * 1024) {
      setCustomIconError("File size should be less than 2MB");
      return;
    }

    setCustomIconError("");
    
    // Create a preview
    const reader = new FileReader();
    reader.onload = (event) => {
      const result = event.target.result;
      setCustomIconPreview(result);
      setFormData({ ...formData, icon: "custom", customIcon: result });
    };
    reader.readAsDataURL(file);
    
    // Switch to custom tab
    setActiveTab("custom");
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    // Validation
    if (!formData.name.trim()) {
      console.error("Category name is required");
      setIsSubmitting(false);
      return;
    }
    
    try {
      const user = auth.currentUser;
      if (!user) {
        console.error("You must be logged in to perform this action");
        setIsSubmitting(false);
        return;
      }

      // Use existing ID if editing, otherwise it will be generated on server
      const idCategorie = category ? category.idCategorie : "";
      
      // Prepare category data for API
      const categoryData = {
        idCategorie: idCategorie,
        nom: formData.name,
        description: formData.description,
        icon: formData.icon,
        customIcon: formData.icon === "custom" ? formData.customIcon : null,
      };

      console.log("Submitting category data:", categoryData);
      
      // Notify parent component
      onSubmit(categoryData);
      
    } catch (error) {
      console.error("Error saving category:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-white shadow-lg rounded-lg overflow-hidden border border-gray-200 mb-6">
      <div className="px-6 py-5 border-b border-gray-200 bg-gradient-to-r from-teal-500 to-teal-600">
        <h3 className="text-lg font-medium text-white">
          {category ? "Edit Category" : "Add New Category"}
        </h3>
      </div>
      <form onSubmit={handleSubmit} className="p-6">
        <div className="grid grid-cols-1 gap-y-6 gap-x-4 sm:grid-cols-6">
          {/* Category Name */}
          <div className="sm:col-span-3">
            <label
              htmlFor="name"
              className="block text-sm font-medium text-gray-700"
            >
              Category Name *
            </label>
            <div className="mt-1 relative">
              <input
                type="text"
                name="name"
                id="name"
                value={formData.name}
                onChange={handleInputChange}
                onFocus={() => formData.name && setShowIconSuggestions(true)}
                required
                className="shadow-sm focus:ring-teal-500 focus:border-teal-500 block w-full sm:text-sm border-gray-300 rounded-md py-3"
                placeholder="Enter category name"
              />
            </div>
          </div>

          {/* Current Icon Selection */}
          <div className="sm:col-span-3">
            <label
              htmlFor="icon"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Selected Icon
            </label>
            <div className="flex items-center justify-between bg-gray-50 p-3 rounded-md border border-gray-200">
              <div className="flex items-center">
                {formData.icon === "custom" && customIconPreview ? (
                  <div className="h-12 w-12 flex items-center justify-center rounded-md bg-white border border-gray-200 p-2">
                    <img src={customIconPreview} alt="Custom icon" className="h-8 w-8 object-contain" />
                  </div>
                ) : (
                  <div className="h-12 w-12 flex items-center justify-center">
                    {iconOptions.find(opt => opt.name === formData.icon)?.icon || <Tag size={24} />}
                  </div>
                )}
                <span className="ml-3 text-sm text-gray-500">
                  {formData.icon === "custom" ? "Custom Icon" : formData.icon}
                </span>
              </div>
              <div>
                <button
                  type="button"
                  onClick={() => setShowIconSuggestions(!showIconSuggestions)}
                  className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500"
                >
                  Change Icon
                </button>
              </div>
            </div>
          </div>

          {/* Icon Selection Panel */}
          {showIconSuggestions && (
            <div className="sm:col-span-6 bg-white border border-gray-200 rounded-lg shadow-md overflow-hidden">
              {/* Tabs */}
              <div className="flex border-b border-gray-200">
                <button
                  type="button"
                  onClick={() => setActiveTab("suggested")}
                  className={`flex-1 py-3 px-4 text-center text-sm font-medium ${
                    activeTab === "suggested"
                      ? "border-b-2 border-teal-500 text-teal-600"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                >
                  Suggested Icons
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab("all")}
                  className={`flex-1 py-3 px-4 text-center text-sm font-medium ${
                    activeTab === "all"
                      ? "border-b-2 border-teal-500 text-teal-600"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                >
                  All Icons
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab("custom")}
                  className={`flex-1 py-3 px-4 text-center text-sm font-medium ${
                    activeTab === "custom"
                      ? "border-b-2 border-teal-500 text-teal-600"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                >
                  Custom Icon
                </button>
              </div>

              {/* Suggested Icons Tab */}
              {activeTab === "suggested" && (
                <div className="p-4">
                  <p className="text-sm text-gray-500 mb-3">
                    Suggested icons based on your category name:
                  </p>
                  {suggestedIcons.length > 0 ? (
                    <div className="flex flex-wrap gap-3">
                      {suggestedIcons.map((option) => (
                        <button
                          key={option.name}
                          type="button"
                          onClick={() => handleIconSelect(option.name)}
                          className={`p-4 rounded-md hover:bg-gray-100 transition-colors duration-150 ${
                            formData.icon === option.name
                              ? "bg-teal-100 text-teal-700 ring-2 ring-teal-500"
                              : "bg-white border border-gray-200 text-gray-700"
                          }`}
                        >
                          {option.icon}
                        </button>
                      ))}
                    </div>
                  ) : (
                    <p className="text-gray-400 text-center py-4">
                      Enter a category name to see suggestions
                    </p>
                  )}
                </div>
              )}

              {/* All Icons Tab */}
              {activeTab === "all" && (
                <div className="p-4">
                  <div className="grid grid-cols-4 sm:grid-cols-6 md:grid-cols-8 lg:grid-cols-10 gap-3">
                    {iconOptions.map((option) => (
                      <button
                        key={option.name}
                        type="button"
                        onClick={() => handleIconSelect(option.name)}
                        className={`p-3 rounded-md hover:bg-gray-100 transition-colors duration-150 ${
                          formData.icon === option.name
                            ? "bg-teal-100 text-teal-700 ring-2 ring-teal-500"
                            : "bg-white border border-gray-200 text-gray-700"
                        }`}
                      >
                        {option.icon}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Custom Icon Tab */}
              {activeTab === "custom" && (
                <div className="p-4">
                  <div className="flex flex-col items-center justify-center p-4 border-2 border-dashed border-gray-300 rounded-md bg-gray-50">
                    {customIconPreview ? (
                      <div className="flex flex-col items-center">
                        <div className="mb-3 p-3 bg-white rounded-md border border-gray-200">
                          <img src={customIconPreview} alt="Custom icon" className="h-24 w-24 object-contain" />
                        </div>
                        <div className="flex space-x-3">
                          <button
                            type="button"
                            onClick={() => fileInputRef.current?.click()}
                            className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500"
                          >
                            <Upload size={16} className="mr-2" />
                            Change
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setCustomIconPreview(null);
                              setFormData({ ...formData, icon: "tag", customIcon: null });
                              setActiveTab("suggested");
                            }}
                            className="inline-flex items-center px-3 py-2 border border-red-300 shadow-sm text-sm font-medium rounded-md text-red-700 bg-white hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                          >
                            Remove
                          </button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-teal-50 mb-4">
                          <Upload size={28} className="text-teal-500" />
                        </div>
                        <div className="text-center">
                          <button
                            type="button"
                            onClick={() => fileInputRef.current?.click()}
                            className="inline-flex items-center px-3 py-2 border border-teal-300 shadow-sm text-sm font-medium rounded-md text-teal-700 bg-teal-50 hover:bg-teal-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500"
                          >
                            <Upload size={16} className="mr-2" />
                            Upload Custom Icon
                          </button>
                          <p className="mt-2 text-xs text-gray-500">
                            PNG, JPG, GIF up to 2MB
                          </p>
                        </div>
                      </>
                    )}
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      onChange={handleCustomIconUpload}
                      className="hidden"
                    />
                  </div>
                  {customIconError && (
                    <div className="mt-2 flex items-center text-sm text-red-600">
                      <AlertCircle size={16} className="mr-1" />
                      {customIconError}
                    </div>
                  )}
                </div>
              )}
              
              <div className="bg-gray-50 px-4 py-3 flex justify-end">
                <button
                  type="button"
                  onClick={() => setShowIconSuggestions(false)}
                  className="inline-flex items-center px-3 py-2 border border-transparent text-sm font-medium rounded-md text-teal-700 bg-teal-100 hover:bg-teal-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500"
                >
                  <CheckCircle size={16} className="mr-2" />
                  Done
                </button>
              </div>
            </div>
          )}

          {/* Description */}
          <div className="sm:col-span-6">
            <label
              htmlFor="description"
              className="block text-sm font-medium text-gray-700"
            >
              Description
            </label>
            <div className="mt-1">
              <textarea
                id="description"
                name="description"
                rows="3"
                value={formData.description}
                onChange={handleInputChange}
                className="shadow-sm focus:ring-teal-500 focus:border-teal-500 block w-full sm:text-sm border-gray-300 rounded-md"
                placeholder="Category description (optional)"
              ></textarea>
              <p className="mt-1 text-xs text-gray-500">
                Provide a short description of what this category includes
              </p>
            </div>
          </div>
        </div>

        {/* Form Actions */}
        <div className="mt-8 flex justify-end space-x-3">
          <button
            type="button"
            onClick={onCancel}
            className="px-5 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors duration-150"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="inline-flex justify-center items-center py-2 px-5 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-teal-600 hover:bg-teal-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors duration-150"
          >
            {isSubmitting ? (
              <>
                <div className="animate-spin h-4 w-4 border-2 border-white rounded-full border-t-transparent mr-2"></div>
                {category ? "Updating..." : "Creating..."}
              </>
            ) : (
              <>
                <CheckCircle size={16} className="mr-2" />
                {category ? "Update Category" : "Create Category"}
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CategoryForm;