import React from "react";
import { useNavigate } from "react-router-dom";
import { X, User } from "lucide-react";

interface QuickSignInModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSignIn: () => void;
}

const QuickSignInModal: React.FC<QuickSignInModalProps> = ({
  isOpen,
  onClose,
  onSignIn,
}) => {
  const navigate = useNavigate();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 modalS" onClick={onClose}></div>
      <div className="relative bg-white rounded-lg w-full max-w-sm p-8 mx-4 text-center">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-900"
          aria-label="Close"
        >
          <X size={24} />
        </button>

        <div className="mb-6">
          <div className="bg-teal-100 rounded-full h-16 w-16 flex items-center justify-center text-teal-500 mx-auto mb-4">
            <User className="h-8 w-8" />
          </div>
          <h2 className="text-xl font-semibold mb-2">
            You should sign in first
          </h2>
          <p className="text-gray-500 mb-6">
            Please sign in to access this feature
          </p>
        </div>

        <button
          onClick={() => {
            onClose();
            onSignIn();
          }}
          className="w-full bg-teal-500 text-white py-3 rounded-full hover:bg-teal-600 transition-colors"
        >
          Sign In
        </button>

        <div className="mt-4">
          <button
            onClick={() => {
              onClose();
              navigate("/sign-up");
            }}
            className="text-sm text-gray-600 hover:text-teal-500"
          >
            Don't have an account? Sign up
          </button>
        </div>
      </div>
    </div>
  );
};

export default QuickSignInModal;
