import React, { useState, useEffect } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { auth, db } from "../../config/Firebase"; // Adjust path as needed
import { doc, getDoc, onSnapshot } from "firebase/firestore";
import { signOut, onAuthStateChanged } from "firebase/auth"; // Import signOut from Firebase auth

// Import icons that match the screenshot more closely
import {
  MdDashboard,
  MdShoppingBag,
  MdPlace,
  MdOutlineAccountCircle,
  MdLogout,
} from "react-icons/md";
import { TbTruckDelivery } from "react-icons/tb";

const AccountSidebar = () => {
  const [userRole, setUserRole] = useState("client");
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  console.log("Current location:", location.pathname); // Debug current location
  console.log("Current user role:", userRole); // Debug user role

  useEffect(() => {
    const unsubAuth = onAuthStateChanged(auth, async (user) => {
      if (!user) {
        setLoading(false);
        return;
      }

      try {
        // Set up real-time listener for the user document
        const userRef = doc(db, "users", user.uid);
        const unsubDoc = onSnapshot(
          userRef,
          // Success handler
          (docSnapshot) => {
            if (docSnapshot.exists()) {
              const userData = docSnapshot.data();
              console.log("Firestore role update:", userData.role);
              setUserRole(userData.role || "client");
            } else {
              console.log("No user document found yet");
              setUserRole("client");
            }
            setLoading(false);
          },
          // Error handler
          (error) => {
            console.error("Error listening to user document:", error);
            setLoading(false);
          }
        );

        // Return cleanup function for both listeners
        return () => {
          unsubDoc();
        };
      } catch (error) {
        console.error("Error setting up user role listener:", error);
        setLoading(false);
      }
    });

    // Return cleanup for auth listener
    return () => unsubAuth();
  }, []);

  // Check if we should redirect to dashboard based on URL params
  useEffect(() => {
    // Check if URL has a redirect parameter
    const params = new URLSearchParams(location.search);
    const redirectTo = params.get("redirect");

    console.log("Redirect parameter:", redirectTo); // Debug redirect parameter
    console.log("User role for redirect check:", userRole); // Debug user role for redirect check

    if (redirectTo === "dashboard" && userRole === "vendeur") {
      console.log("Redirecting to dashboard from redirect parameter");
      navigate("/account/dashboard");
    }
  }, [location.search, userRole, navigate]);

  // Handle navigation with click handler instead of relying only on Link
  const handleNavigation = (path) => {
    navigate(path);
  };

  // Handle logout
  const handleLogout = async () => {
    try {
      setLoggingOut(true);
      await signOut(auth);
      console.log("User signed out successfully");
      // Redirect to home page after successful logout
      navigate("/");
    } catch (error) {
      console.error("Error signing out:", error);
      setLoggingOut(false);
    }
  };

  // Active path check - simplified and fixed
  const isActive = (path) => {
    if (
      path === "/account" &&
      (location.pathname === "/account" || location.pathname === "/account/")
    ) {
      return true;
    }
    return location.pathname === path;
  };

  if (loading) {
    return (
      <div className="w-full p-4">
        <div className="space-y-3">
          {/* Animate first button with a teal glow */}
          <div className="h-14 relative overflow-hidden rounded-[10px] bg-gray-100">
            <div className="absolute inset-0 bg-gradient-to-r from-gray-100 via-teal-100 to-gray-100 opacity-60 animate-pulse"></div>
            <div className="absolute left-4 top-1/2 transform -translate-y-1/2 w-6 h-6 rounded-full bg-gray-200"></div>
            <div className="absolute left-14 top-1/2 transform -translate-y-1/2 w-24 h-3 rounded-full bg-gray-200"></div>
          </div>

          {/* Other buttons with simpler animation */}
          <div className="h-14 rounded-[10px] bg-white border border-gray-200 animate-pulse flex items-center">
            <div className="ml-4 w-6 h-6 rounded-full bg-gray-200"></div>
            <div className="ml-3 w-20 h-3 rounded-full bg-gray-200"></div>
          </div>

          <div className="h-14 rounded-[10px] bg-white border border-gray-200 animate-pulse flex items-center">
            <div className="ml-4 w-6 h-6 rounded-full bg-gray-200"></div>
            <div className="ml-3 w-28 h-3 rounded-full bg-gray-200"></div>
          </div>

          <div className="h-14 rounded-[10px] bg-white border border-gray-200 animate-pulse flex items-center">
            <div className="ml-4 w-6 h-6 rounded-full bg-gray-200"></div>
            <div className="ml-3 w-24 h-3 rounded-full bg-gray-200"></div>
          </div>
        </div>
      </div>
    );
  }

  // Force check for vendeur role (this is just for debugging)
  console.log("Final check - is vendeur?", userRole === "vendeur");

  return (
    <div className="flex flex-col gap-3 lg:mt-8 lg:ml-5">
      {/* Account Details - Now first in the list */}
      <button
        onClick={() => handleNavigation("/account")}
        className={`flex items-center w-[230px] h-[56px] px-4 rounded-[10px] transition-colors duration-200 ${
          isActive("/account")
            ? "bg-teal-500 text-white"
            : "bg-white text-gray-500 hover:bg-gray-50 border border-gray-200"
        }`}
      >
        <MdOutlineAccountCircle className="h-6 w-6 mr-3" />
        <span className="text-lg font-medium">Account details</span>
      </button>

      {/* Dashboard - only visible for vendeur */}
      {userRole === "vendeur" && (
        <button
          onClick={() => handleNavigation("/account/dashboard")}
          className={`flex items-center w-[230px] h-[56px] px-4 rounded-[10px] transition-colors duration-200 ${
            isActive("/account/dashboard")
              ? "bg-teal-500 text-white"
              : "bg-white text-gray-500 hover:bg-gray-50 border border-gray-200"
          }`}
        >
          <MdDashboard className="h-6 w-6 mr-3" />
          <span className="text-lg font-medium">Dashboard</span>
        </button>
      )}

      {/* Orders */}
      <button
        onClick={() => handleNavigation("/account/orders")}
        className={`flex items-center w-[230px] h-[56px] px-4 rounded-[10px] transition-colors duration-200 ${
          isActive("/account/orders")
            ? "bg-teal-500 text-white"
            : "bg-white text-gray-500 hover:bg-gray-50 border border-gray-200"
        }`}
      >
        <MdShoppingBag className="h-6 w-6 mr-3" />
        <span className="text-lg font-medium">Orders</span>
      </button>

      {/* Track Your Order */}
      <button
        onClick={() => handleNavigation("/account/track-order")}
        className={`flex items-center w-[230px] h-[56px] px-4 rounded-[10px] transition-colors duration-200 ${
          isActive("/account/track-order")
            ? "bg-teal-500 text-white"
            : "bg-white text-gray-500 hover:bg-gray-50 border border-gray-200"
        }`}
      >
        <TbTruckDelivery className="h-6 w-6 mr-3" />
        <span className="text-lg font-medium">Track Your Order</span>
      </button>

      {/* Logout */}
      <button
        onClick={handleLogout}
        disabled={loggingOut}
        className="flex items-center w-[230px] h-[56px] px-4 rounded-[10px] bg-white text-gray-500 hover:bg-gray-50 border border-gray-200 transition-colors duration-200"
      >
        {loggingOut ? (
          <div className="flex items-center">
            <div className="h-5 w-5 mr-3 border-t-2 border-r-2 border-gray-500 rounded-full animate-spin"></div>
            <span className="text-lg font-medium">Logging out...</span>
          </div>
        ) : (
          <>
            <MdLogout className="h-6 w-6 mr-3" />
            <span className="text-lg font-medium">Logout</span>
          </>
        )}
      </button>
    </div>
  );
};

export default AccountSidebar;
