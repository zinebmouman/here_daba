import React from "react";
import Navbar from "../Components/Navbar";
import LoginForm from "../Components/Signup/LoginForm";
import Footer from "../Components/Footer";
const Login: React.FC = () => {
  return (
    <>
      <Navbar />
      <LoginForm />
      <Footer />
    </>
  );
};
export default Login;
