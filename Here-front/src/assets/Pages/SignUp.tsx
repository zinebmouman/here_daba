import React from "react";
import Navbar from "../Components/Navbar";

import Footer from "../Components/Footer";
import CreateAccountForm from "../Components/Signup/CreateAccountForm";

const SignUp: React.FC = () => {
  return (
    <>
      <Navbar />
      <CreateAccountForm />
      <Footer />
    </>
  );
};

export default SignUp;
