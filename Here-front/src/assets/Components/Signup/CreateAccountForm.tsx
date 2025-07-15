import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { auth, db } from "../../../config/Firebase";
import {
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  signInWithPopup,
  updateProfile,
} from "firebase/auth";
import { setDoc, doc, getDoc } from "firebase/firestore";
import "../../style/Style.css";
// Dans CreateAccountForm.tsx, ligne 12
import { syncUserWithPostgre, verifyTokenWithBackend ,updateUserRole} from "./services/authService";
const CreateAccountForm: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const [formData, setFormData] = useState({
    lastName: "",
    firstName: "",
    phoneCode: "+212",
    phoneNumber: "",
    email: "",
    password: "",
    agreeToTerms: false,
  });

  const [isPhoneDropdownOpen, setIsPhoneDropdownOpen] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);
  const [formErrors, setFormErrors] = useState({
    lastName: "",
    firstName: "",
    phoneNumber: "",
    email: "",
    password: "",
    agreeToTerms: "",
    submit: "",
  });

  // Update password strength whenever password changes
  useEffect(() => {
    calculatePasswordStrength(formData.password);
  }, [formData.password]);

  const calculatePasswordStrength = (password: string): void => {
    // Calculate password strength (0-5)
    let strength = 0;

    if (password.length >= 8) strength += 1;
    if (password.match(/[A-Z]/)) strength += 1;
    if (password.match(/[a-z]/)) strength += 1;
    if (password.match(/[0-9]/)) strength += 1;
    if (password.match(/[^A-Za-z0-9]/)) strength += 1;

    setPasswordStrength(strength);
  };

  const getStrengthColor = (): string => {
    if (passwordStrength === 0) return "bg-gray-200";
    if (passwordStrength === 1) return "bg-red-500";
    if (passwordStrength === 2) return "bg-orange-500";
    if (passwordStrength === 3) return "bg-yellow-500";
    if (passwordStrength === 4) return "bg-blue-500";
    return "bg-green-500";
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
    const { name, value, type, checked } = e.target;
    const newValue = type === "checkbox" ? checked : value;

    // Special handling for phone number to enforce format based on prefix
    if (name === "phoneNumber") {
      // Strip non-numeric characters
      const numericValue = value.replace(/\D/g, "");

      // Check length based on country code
      if (formData.phoneCode === "+212" && numericValue.length > 9) {
        return; // Don't update if exceeding max length for Morocco
      }

      // For other prefixes, limit to reasonable length
      if (formData.phoneCode !== "+212" && numericValue.length > 15) {
        return;
      }
    }

    // Clear the error for this field
    setFormErrors({
      ...formErrors,
      [name]: "",
    });

    setFormData((prev) => ({
      ...prev,
      [name]: newValue,
    }));
  };

  const validateForm = (): boolean => {
    let isValid = true;
    const errors = {
      lastName: "",
      firstName: "",
      phoneNumber: "",
      email: "",
      password: "",
      agreeToTerms: "",
      submit: "",
    };

    // Last name validation
    if (!formData.lastName.trim()) {
      errors.lastName = "Last name is required";
      isValid = false;
    }

    // First name validation
    if (!formData.firstName.trim()) {
      errors.firstName = "First name is required";
      isValid = false;
    }

    // Phone number validation with prefix-specific rules
    if (!formData.phoneNumber.trim()) {
      errors.phoneNumber = "Phone number is required";
      isValid = false;
    } else {
      const numericPhone = formData.phoneNumber.replace(/\D/g, "");

      if (formData.phoneCode === "+212" && numericPhone.length !== 9) {
        errors.phoneNumber = "Moroccan numbers must be 9 digits";
        isValid = false;
      } else if (numericPhone.length < 7) {
        errors.phoneNumber = "Phone number is too short";
        isValid = false;
      }
    }

    // Email validation
    if (!formData.email.trim()) {
      errors.email = "Email is required";
      isValid = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = "Please enter a valid email address";
      isValid = false;
    }

    // Password validation
    if (!formData.password) {
      errors.password = "Password is required";
      isValid = false;
    } else if (formData.password.length < 8) {
      errors.password = "Password must be at least 8 characters";
      isValid = false;
    } else if (passwordStrength < 3) {
      errors.password = "Password is too weak";
      isValid = false;
    }

    // Terms validation
    if (!formData.agreeToTerms) {
      errors.agreeToTerms = "You must agree to the terms and conditions";
      isValid = false;
    }

    setFormErrors(errors);
    return isValid;
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
  
    if (!validateForm()) return;
  
    setLoading(true);
  
    try {
      // Création utilisateur Firebase
      const userCredential = await createUserWithEmailAndPassword(
        auth,
        formData.email,
        formData.password
      );
      const user = userCredential.user;
  
      // Création du nom complet
      const displayName = `${formData.firstName} ${formData.lastName}`;
  
      // Mise à jour du profil
      await updateProfile(user, { displayName });
  
      // Récupération du token
      const token = await user.getIdToken();
  
      // Préparation des données utilisateur
      const userData = {
        uid: user.uid,
        email: user.email,
        displayName: displayName,
        firstName: formData.firstName,
        lastName: formData.lastName,
        phone: `${formData.phoneCode}${formData.phoneNumber}`,
        role: "client", // Rôle par défaut
        createdAt: new Date().toISOString()
      };
  
      // Sauvegarde dans Firestore
      try {
        const syncResult = await syncUserWithPostgre(token, userData);
        
        if (syncResult.error) {
          console.warn("⚠️ Synchronisation PostgreSQL partielle:", syncResult.message);
          // Optionnel : afficher un message à l'utilisateur
        } else {
          console.log("Utilisateur synchronisé avec PostgreSQL");
          // Appeler updateUserRole après la synchronisation réussie
          await updateUserRole(user.uid, "client");
        }
      } catch (syncError) {
        console.error("Erreur lors de la synchronisation:", syncError);
      }
  
      // Redirection
      navigate("/");
    } catch (error: any) {
      // Gestion des erreurs existante
      console.error("Erreur d'inscription:", error);
      
      // Gestion des messages d'erreur spécifiques
      const errorMessages: Record<string, string> = {
        'auth/email-already-in-use': 'Cet email est déjà utilisé',
        'auth/invalid-email': 'Email invalide',
        'default': 'Erreur lors de la création de compte'
      };
  
      setFormErrors({
        ...formErrors,
        submit: errorMessages[error.code] || errorMessages['default']
      });
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async (): Promise<void> => {
    setLoading(true);
    try {
      const provider = new GoogleAuthProvider();
      const result = await signInWithPopup(auth, provider);
      const user = result.user;
  
      // Récupérer le token Firebase
      const token = await user.getIdToken();
  
      // Préparer les données utilisateur
      const userData = {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        firstName: user.displayName?.split(" ")[0] || "",
        lastName: user.displayName?.split(" ")[1] || "",
        role: "client", // Rôle par défaut
        createdAt: new Date().toISOString()
      };
  
      // Vérifier si l'utilisateur existe dans Firestore
      const userDoc = await getDoc(doc(db, "users", user.uid));
  
      // Créer/mettre à jour dans Firestore
      if (!userDoc.exists()) {
        await setDoc(doc(db, "users", user.uid), userData);
      }
  
      // Synchronisation avec PostgreSQL
      try {
        const syncResult = await syncUserWithPostgre(token, userData);
        
        if (syncResult.error) {
          console.warn("⚠️ Synchronisation PostgreSQL partielle:", syncResult.message);
          // Vous pouvez choisir d'afficher un message à l'utilisateur
        } else {
          console.log("Utilisateur Google synchronisé avec PostgreSQL");
        }
      } catch (syncError) {
        console.error("Erreur lors de la synchronisation Google avec PostgreSQL:", syncError);
        // Gestion de l'erreur de synchronisation
      }
  
      // Redirection après connexion réussie
      navigate("/");
    } catch (error: any) {
      console.error("Erreur de connexion Google:", error);
      setFormErrors({
        ...formErrors,
        submit: 'Échec de la connexion Google. Veuillez réessayer.'
      });
    } finally {
      setLoading(false);
    }
  };

  // Function to navigate to login page
  const handleLoginClick = (): void => {
    console.log("Login button clicked - navigating to login page");
    navigate("/sign-in"); // Navigate to the login route
  };

  return (
    <div className="max-w-4xl mx-auto p-6 font-sans all">
      <h1 className="text-3xl font-bold text-gray-800 mb-8">Create Account</h1>

      <form onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-16 gap-y-6">
          {/* Left Column */}
          <div className="space-y-6">
            {/* Last Name */}
            <div className="space-y-2">
              <label className="block text-sm text-gray-700">Nom</label>
              <div className="relative">
                <input
                  type="text"
                  placeholder="Enter your last name"
                  value={formData.lastName}
                  onChange={handleInputChange}
                  name="lastName"
                  className={`w-full h-12 px-4 py-2 bg-[#F6F7FB] border-2 rounded-[32px] text-gray-800 ${
                    formErrors.lastName ? "border-red-500" : "border-[#F6F7FB]"
                  }`}
                />
                {formErrors.lastName && (
                  <p className="text-red-500 text-xs mt-1 px-4">
                    {formErrors.lastName}
                  </p>
                )}
              </div>
            </div>

            {/* First Name */}
            <div className="space-y-2">
              <label className="block text-sm text-gray-700">Prenom</label>
              <div className="relative">
                <input
                  type="text"
                  placeholder="Enter your first name"
                  value={formData.firstName}
                  onChange={handleInputChange}
                  name="firstName"
                  className={`w-full h-12 px-4 py-2 bg-[#F6F7FB] border-2 rounded-[32px] text-gray-800 ${
                    formErrors.firstName ? "border-red-500" : "border-[#F6F7FB]"
                  }`}
                />
                {formErrors.firstName && (
                  <p className="text-red-500 text-xs mt-1 px-4">
                    {formErrors.firstName}
                  </p>
                )}
              </div>
            </div>

            {/* Phone */}
            <div className="space-y-2">
              <label className="block text-sm text-gray-700">Telephone</label>
              <div className="flex">
                {/* Country Code Dropdown */}
                <div className="relative z-10">
                  <button
                    type="button"
                    onClick={() => setIsPhoneDropdownOpen(!isPhoneDropdownOpen)}
                    className="flex items-center h-12 px-4 bg-[#F6F7FB] rounded-l-[32px] border-2 border-[#F6F7FB]"
                  >
                    <span className="text-gray-700 font-medium">
                      {formData.phoneCode}
                    </span>
                    <svg
                      className="w-4 h-4 ml-2 text-gray-500"
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                    >
                      <path
                        fillRule="evenodd"
                        d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                        clipRule="evenodd"
                        />
                    </svg>
                  </button>

                  {isPhoneDropdownOpen && (
                    <div className="absolute mt-1 w-full bg-white rounded-lg shadow-lg border border-gray-200">
                      <div className="py-1 max-h-60 overflow-y-auto">
                        {["+212", "+1", "+44", "+33", "+49"].map((prefix) => (
                          <button
                            key={prefix}
                            type="button"
                            className="block w-full px-4 py-2 text-left hover:bg-gray-50"
                            onClick={() => {
                              console.log(`Phone code changed: ${prefix}`);
                              // Clear phone number when changing prefix to avoid validation conflicts
                              setFormData({
                                ...formData,
                                phoneCode: prefix,
                                phoneNumber: "",
                              });
                              setIsPhoneDropdownOpen(false);
                            }}
                          >
                            <span className="text-gray-800">{prefix}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>

                {/* Phone Number Input */}
                <div className="flex-grow relative">
                  <input
                    type="tel"
                    placeholder={
                      formData.phoneCode === "+212"
                        ? "XX XXX XX XX"
                        : "XXX XXX XXXX"
                    }
                    value={formData.phoneNumber}
                    onChange={handleInputChange}
                    name="phoneNumber"
                    className={`w-full h-12 px-4 py-2 bg-[#F6F7FB] border-2 border-l-0 rounded-r-[32px] text-gray-800 ${
                      formErrors.phoneNumber
                        ? "border-red-500"
                        : "border-[#F6F7FB]"
                    }`}
                  />
                  {formErrors.phoneNumber && (
                    <p className="text-red-500 text-xs mt-1 px-4">
                      {formErrors.phoneNumber}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Right Column */}
          <div className="space-y-6">
            {/* Email */}
            <div className="space-y-2">
              <label className="block text-sm text-gray-700">
                Email Address
              </label>
              <div className="relative">
                <input
                  type="email"
                  placeholder="youremail@example.com"
                  value={formData.email}
                  onChange={handleInputChange}
                  name="email"
                  className={`w-full h-12 px-4 py-2 bg-[#F6F7FB] border-2 rounded-[32px] text-gray-800 ${
                    formErrors.email ? "border-red-500" : "border-[#F6F7FB]"
                  }`}
                />
                {formErrors.email && (
                  <p className="text-red-500 text-xs mt-1 px-4">
                    {formErrors.email}
                  </p>
                )}
              </div>
            </div>

            {/* Password */}
            <div className="space-y-2">
              <label className="block text-sm text-gray-700">
                Create Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter your password"
                  value={formData.password}
                  onChange={handleInputChange}
                  name="password"
                  className={`w-full h-12 px-4 py-2 bg-[#F6F7FB] border-2 rounded-[32px] text-gray-800 ${
                    formErrors.password ? "border-red-500" : "border-[#F6F7FB]"
                  }`}
                />
                <button
                  type="button"
                  className="absolute right-4 top-3.5 text-gray-500"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? (
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      className="h-5 w-5"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                    >
                      <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                      <path
                        fillRule="evenodd"
                        d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z"
                        clipRule="evenodd"
                      />
                    </svg>
                  ) : (
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      className="h-5 w-5"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                    >
                      <path
                        fillRule="evenodd"
                        d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z"
                        clipRule="evenodd"
                      />
                      <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
                    </svg>
                  )}
                </button>
                {formErrors.password && (
                  <p className="text-red-500 text-xs mt-1 px-4">
                    {formErrors.password}
                  </p>
                )}
              </div>

              {/* Password Strength Indicator */}
              <div className="mt-2">
                <div className="h-1 w-full bg-gray-200 rounded-full overflow-hidden">
                  <div
                    className={`h-full ${getStrengthColor()} transition-all duration-300`}
                    style={{ width: `${passwordStrength * 20}%` }}
                  />
                </div>
                <p className="text-xs text-gray-500 mt-1 px-4">
                  {passwordStrength === 0 && "Enter a password"}
                  {passwordStrength === 1 && "Very weak"}
                  {passwordStrength === 2 && "Weak"}
                  {passwordStrength === 3 && "Medium"}
                  {passwordStrength === 4 && "Strong"}
                  {passwordStrength === 5 && "Very strong"}
                </p>
              </div>
            </div>

            {/* Terms Checkbox */}
            <div className="flex items-start mt-8">
              <div className="relative flex items-start">
                <div className="flex items-center h-5">
                  <input
                    type="checkbox"
                    id="agreeToTerms"
                    name="agreeToTerms"
                    checked={formData.agreeToTerms}
                    onChange={handleInputChange}
                    className="opacity-0 absolute h-5 w-5"
                  />
                  <div
                    className={`mr-3 flex items-center justify-center w-6 h-6 ${
                      formData.agreeToTerms
                        ? "btncolor"
                        : `border-2 ${
                            formErrors.agreeToTerms
                              ? "border-red-500"
                              : "border-gray-300"
                          }`
                    } rounded-full transition-colors duration-200`}
                  >
                    {formData.agreeToTerms && (
                      <svg
                        className="w-4 h-4 text-white"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" />
                      </svg>
                    )}
                  </div>
                </div>
                <div className="ml-3 text-sm">
                  <span
                    className={`${
                      formErrors.agreeToTerms ? "text-red-500" : "text-gray-700"
                    }`}
                  >
                    I have read and agree to{" "}
                    <a href="#" className="font-medium text-gray-900">
                      terms & conditions
                    </a>
                  </span>
                  {formErrors.agreeToTerms && (
                    <p className="text-red-500 text-xs mt-1">
                      {formErrors.agreeToTerms}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Submission Error Message */}
            {formErrors.submit && (
              <div className="flex items-center p-4 mb-4 text-red-800 rounded-lg bg-red-50">
                <span>{formErrors.submit}</span>
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex items-center space-x-4 mt-6">
              <button
                type="submit"
                disabled={loading}
                className="px-6 py-3 btncolor text-white font-bold rounded-full hover:bg-green-600 focus:outline-none transition-colors"
              >
                {loading ? "Creating Account..." : "Create Account"}
              </button>

              <button
                type="button"
                className="px-16 py-2.5 text-gray-700 font-bold border-2 btnbgcolor rounded-full hover:bg-green-50 transition-colors"
                onClick={handleLoginClick}
              >
                Login
              </button>
            </div>

            {/* Google Login */}
            <div className="mt-6">
              <button
                type="button"
                className="flex items-center justify-center space-x-2 lg:w-96 px-4 py-3 border-2 border-[#F6F7FB] rounded-full hover:bg-gray-50 transition-colors"
                onClick={handleGoogleLogin}
                disabled={loading}
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24">
                  <path
                    fill="#EA4335"
                    d="M5.266 9.765A7.077 7.077 0 0 1 12 4.909c1.69 0 3.218.6 4.418 1.582L19.91 3C17.782 1.145 15.055 0 12 0 7.27 0 3.198 2.698 1.24 6.65l4.026 3.115Z"
                  />
                  <path
                    fill="#34A853"
                    d="M16.04 18.013c-1.09.703-2.474 1.078-4.04 1.078a7.077 7.077 0 0 1-6.723-4.823l-4.04 3.067A11.965 11.965 0 0 0 12 24c2.933 0 5.735-1.043 7.834-3l-3.793-2.987Z"
                  />
                  <path
                    fill="#4A90E2"
                    d="M19.834 21c2.195-2.048 3.62-5.096 3.62-9 0-.71-.109-1.473-.272-2.182H12v4.637h6.436c-.317 1.559-1.17 2.766-2.395 3.558L19.834 21Z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M5.277 14.268A7.12 7.12 0 0 1 4.909 12c0-.782.125-1.533.357-2.235L1.24 6.65A11.934 11.934 0 0 0 0 12c0 1.92.445 3.73 1.237 5.335l4.04-3.067Z"
                  />
                </svg>
                <span className="font-bold">Login with google</span>
              </button>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
};

export default CreateAccountForm;