import React, { useState } from "react";
import { ChevronDown, ChevronUp } from "lucide-react";
import "../style/Navbar.css";

const TermsConditionsPage = () => {
  // État pour suivre quels dropdowns sont ouverts
  const [openSections, setOpenSections] = useState({
    "1.1": false,
    "1.2": false,
    "1.3": false,
    "1.4": false,
    "2.1": false,
    "2.2": false,
    "3.1": false,
    "3.2": false,
    "3.3": false,
    "4.1": false,
    "4.2": false,
  });

  // Fonction pour basculer l'état d'un dropdown
  const toggleSection = (section) => {
    setOpenSections((prev) => ({
      ...prev,
      [section]: !prev[section],
    }));
  };

  // Composant pour un dropdown simple
  const Dropdown = ({ id, title, content, isOpen }) => {
    return (
      <div className="mb-4 mt-10">
        <div
          className="flex items-center justify-between p-4 cursor-pointer bg-gray-50 hover:bg-gray-100 transition-colors rounded-lg"
          onClick={() => toggleSection(id)}
        >
          <h3 className="all text-base font-semibold text-gray-800">{title}</h3>
          <div className="text-gray-500">
            {isOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </div>
        </div>

        {isOpen && (
          <div className="p-4 rounded-b-lg mt-1">
            <p className="text-sm text-gray-600">{content}</p>
          </div>
        )}
      </div>
    );
  };

  // Les contenus des sections
  const sections = {
    "1.1": {
      title: "1.1 Our Terms & Conditions",
      content:
        "Here you can put any text that you think would be suitable and relevant to this particular section of the website.",
    },
    "1.2": {
      title: "1.2 Collection of personal data",
      content:
        "This place is reserved for you to put some text content that you think would make sense here.",
    },
    "1.3": {
      title: "1.3 Purpose of collection of personal data",
      content:
        "Detailed explanation about why we collect personal data and how it helps us improve our services and your experience.",
    },
    "1.4": {
      title: "1.4 Usage of your personal data",
      content:
        "Information about how we use your data, including processing orders, improving our services, and personalization.",
    },
    "2.1": {
      title: "2.1 Different payment methods on our website",
      content:
        "Details about all the payment methods we accept, including credit cards, PayPal, and bank transfers.",
    },
    "2.2": {
      title: "2.2 Our right to cancel your payment",
      content:
        "Just put any text here that would be suitable for this particular section of the website.",
    },
    "3.1": {
      title: "3.1 Order processing on our website",
      content:
        "Information about how we process your orders, from confirmation to preparation.",
    },
    "3.2": {
      title: "3.2 Dispatch and shipping times for different types of orders",
      content:
        "Details about shipping times for different product categories and delivery options available.",
    },
    "3.3": {
      title: "3.3 Return and refund policies for all online orders",
      content:
        "All you need to do is to put your own text here and that is going to be it, all done. This section can be used for really long pieces of text that explain a lot of small details that are required.",
    },
    "4.1": {
      title: "4.1 Our right to change Terms & Conditions",
      content:
        "Information about when and why we might update our terms and conditions, and how we'll notify you.",
    },
    "4.2": {
      title: "4.2 Notice of change in Terms & Conditions",
      content:
        "This place is reserved for you to put some text content that you think would make sense here.",
    },
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-6xl mx-auto">
        <h1 className="all w-96 text-4xl font-bold text-gray-900 mb-6">
          LOGO - Terms and Conditions
        </h1>

        {/* Section 1: Introduction */}
        <div className="mb-8">
          <h2 className="all mt-10 text-3xl font-bold text-gray-800 mb-4">
            1. Introduction
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Dropdown
              id="1.1"
              title={sections["1.1"].title}
              content={sections["1.1"].content}
              isOpen={openSections["1.1"]}
            />
            <Dropdown
              id="1.2"
              title={sections["1.2"].title}
              content={sections["1.2"].content}
              isOpen={openSections["1.2"]}
            />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
            <Dropdown
              id="1.3"
              title={sections["1.3"].title}
              content={sections["1.3"].content}
              isOpen={openSections["1.3"]}
            />
            <Dropdown
              id="1.4"
              title={sections["1.4"].title}
              content={sections["1.4"].content}
              isOpen={openSections["1.4"]}
            />
          </div>
        </div>

        {/* Section 2: Payment Terms */}
        <div className="mb-8">
          <h2 className="all mt-10 text-3xl font-bold text-gray-800 mb-4">
            2. Payment Terms
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Dropdown
              id="2.1"
              title={sections["2.1"].title}
              content={sections["2.1"].content}
              isOpen={openSections["2.1"]}
            />
            <Dropdown
              id="2.2"
              title={sections["2.2"].title}
              content={sections["2.2"].content}
              isOpen={openSections["2.2"]}
            />
          </div>
        </div>

        {/* Section 3: Orders */}
        <div className="mb-8">
          <h2 className="all mt-10 text-3xl font-bold text-gray-800 mb-4">
            3. Orders
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Dropdown
              id="3.1"
              title={sections["3.1"].title}
              content={sections["3.1"].content}
              isOpen={openSections["3.1"]}
            />
            <Dropdown
              id="3.2"
              title={sections["3.2"].title}
              content={sections["3.2"].content}
              isOpen={openSections["3.2"]}
            />
          </div>
          <div className="mt-4">
            <Dropdown
              id="3.3"
              title={sections["3.3"].title}
              content={sections["3.3"].content}
              isOpen={openSections["3.3"]}
            />
          </div>
        </div>

        {/* Section 4: Changes */}
        <div className="mb-8">
          <h2 className="all mt-10 text-3xl font-semibold text-gray-800 mb-4">
            4. Changes
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Dropdown
              id="4.1"
              title={sections["4.1"].title}
              content={sections["4.1"].content}
              isOpen={openSections["4.1"]}
            />
            <Dropdown
              id="4.2"
              title={sections["4.2"].title}
              content={sections["4.2"].content}
              isOpen={openSections["4.2"]}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default TermsConditionsPage;
