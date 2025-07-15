import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import "./App.css";
import Checkout from './assets/Components/Checkout';
import "../src/assets/style/Navbar.css";
import HomePage from "./assets/Pages/HomePage";
import SignUp from "./assets/Pages/SignUp";
import Login from "./assets/Pages/Login";
import TermsConditionsPage from "./assets/Pages/TermsConditionsPage";
import AccountPage from "./assets/Pages/AccountPage";
import OrderManagement from "./assets/Components/OrderManagement";
import ProductPage from "./assets/Pages/ProductPage";
import SearchResultsPage from "./assets/Pages/SearchResultsPage";
import StorePage from "./assets/Components/ProductPage/StorePage";
import Cart from './assets/Components/Cart'
import PaypalSuccess from './assets/Components/payment/PaypalSuccess';
import StripeProvider from './assets/components/payment/StripeProvider';
import OrderConfirmation from './assets/Components/payment/PaypalSuccess';
function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/sign-up" element={<SignUp />} />
        <Route path="/sign-in" element={<Login />} />
        <Route path="/termsandcondition" element={<TermsConditionsPage />} />
        <Route path="/account/*" element={<AccountPage />} />
        <Route path="/client-profile-editor" element={<AccountPage />} />
        <Route path="/product/:id" element={<ProductPage />} />
        <Route path="/produits/:id" element={<ProductPage />} />
        <Route path="/search" element={<SearchResultsPage />} />
        <Route path="/boutiques/:id" element={<StorePage />} />
        <Route path="/cart" element={<Cart />} />
        <Route path="/checkout" element={<Checkout />} />

        <Route path="/paypal/success" element={<PaypalSuccess />} />
        <Route path="/paypal/cancel" element={<Navigate to="/checkout" />} />

        <Route path="/checkout/paypal/success" element={<PaypalSuccess />} />
        <Route path="/checkout/confirmation" element={<OrderConfirmation />} />
      </Routes>
    </Router>
  );
}

export default App;
