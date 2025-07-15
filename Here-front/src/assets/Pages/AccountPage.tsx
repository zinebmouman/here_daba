import React, { useEffect, useState } from "react";
import {
  Routes,
  Route,
  Navigate,
  useLocation,
  useNavigate,
} from "react-router-dom";
import ClientProfileEditor from "../Components/ClientProfileEditor";
import AccountSidebar from "../Components/AccountSidebar";
import Footer from "../Components/Footer";
import Navbar from "../Components/Navbar";
import OrderManagement from "../Components/OrderManagement";
import { auth, db } from "../../config/Firebase";
import { doc, getDoc } from "firebase/firestore";
import TrackOrderComponent from "../Components/TrackOrderComponent";
import Dashboard from "../Components/dashboard/Dashboard";
import ProductsManagement from "../Components/dashboard/ProductsManagement";
import CategoriesManagement from "../Components/dashboard/CategoriesManagement";
import StoresManagement from "../Components/dashboard/StoresManagement";
import PromotionsManagement from "../Components/dashboard/PromotionsManagement";
import StockManagement from "../Components/dashboard/StockManagement";
import SubscriptionManagement from "../Components/dashboard/SubscriptionManagement";

const AccountPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [userRole, setUserRole] = useState("client");
  const [loading, setLoading] = useState(true);

  // Log the current path for debugging
  console.log("Account page rendered at path:", location.pathname);

  // Fetch user role to determine access rights
  useEffect(() => {
    const fetchUserRole = async () => {
      try {
        const user = auth.currentUser;
        if (!user) {
          setLoading(false);
          navigate("/sign-in"); // Redirect to sign in if not logged in
          return;
        }
        const userDoc = await getDoc(doc(db, "users", user.uid));
        if (userDoc.exists()) {
          setUserRole(userDoc.data().role || "client");
        }
        setLoading(false);
      } catch (error) {
        console.error("Error fetching user role:", error);
        setLoading(false);
      }
    };
    fetchUserRole();
  }, [navigate]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  return (
    <>
      <Navbar />
      <div className="container mx-auto px-4 py-8">
        <div className="flex flex-col md:flex-row gap-8">
          {/* Sidebar */}
          <div className="w-full md:w-auto">
            <AccountSidebar />
          </div>
          {/* Main Content */}
          <div className="w-full flex-1">
            <Routes>
              <Route path="/" element={<ClientProfileEditor />} />
              <Route path="orders" element={<OrderManagement />} />
              <Route path="track-order" element={<TrackOrderComponent />} />

              {/* Dashboard route with role check */}
              <Route
                path="dashboard"
                element={
                  userRole === "vendeur" ? (
                    <Dashboard />
                  ) : (
                    <Navigate to="/account" replace />
                  )
                }
              />

              {/* Routes pour les sections produits, catégories et magasins */}
              <Route
                path="products"
                element={
                  userRole === "vendeur" ? (
                    <ProductsManagement />
                  ) : (
                    <Navigate to="/account" replace />
                  )
                }
              />
              <Route
                path="categories"
                element={
                  userRole === "vendeur" ? (
                    <CategoriesManagement />
                  ) : (
                    <Navigate to="/account" replace />
                  )
                }
              />
              <Route
                path="stores"
                element={
                  userRole === "vendeur" ? (
                    <StoresManagement />
                  ) : (
                    <Navigate to="/account" replace />
                  )
                }
              />

              {/* Nouvelles routes ajoutées */}
              <Route
                path="promotions"
                element={
                  userRole === "vendeur" ? (
                    <PromotionsManagement />
                  ) : (
                    <Navigate to="/account" replace />
                  )
                }
              />
              <Route
                path="stock"
                element={
                  userRole === "vendeur" ? (
                    <StockManagement />
                  ) : (
                    <Navigate to="/account" replace />
                  )
                }
              />
              <Route
                path="subscriptions"
                element={
                  userRole === "vendeur" ? (
                    <SubscriptionManagement />
                  ) : (
                    <Navigate to="/account" replace />
                  )
                }
              />

              {/* Catch-all redirect */}
              <Route path="*" element={<Navigate to="/account" replace />} />
            </Routes>
          </div>
        </div>
      </div>
      <Footer />
    </>
  );
};

export default AccountPage;
