import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  signInWithEmailAndPassword,
  GoogleAuthProvider,
  signInWithPopup,
  sendPasswordResetEmail
} from "firebase/auth";
import { doc, getDoc, setDoc } from "firebase/firestore";
import { auth, db } from "../../../config/Firebase";
import "../../style/Style.css";
import { 
  verifyTokenWithBackend, 
  syncUserWithPostgre, 
  checkUserRole,updateUserRole, 
  //testApiConnection 
} from "./services/authService";


const LoginForm: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({
    email: "",
    password: "",
    general: "",
    submit: ""
  });

  useEffect(() => {
    const testConnection = async () => {
      try {
        const result = await testApiConnection();
        console.log("🌐 Test de connexion à l'API:", result ? "Réussi" : "Échoué");
      } catch (error) {
        console.error("🌐 Erreur lors du test de connexion:", error);
      }
    };
    
    testConnection();
  }, []);

  const validateForm = () => {
    let isValid = true;
    const newErrors = {
      email: "",
      password: "",
      general: "",
      submit: ""
    };

    if (!email.trim()) {
      newErrors.email = "L'adresse email est requise";
      isValid = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = "Veuillez entrer une adresse email valide";
      isValid = false;
    }

    if (!password) {
      newErrors.password = "Le mot de passe est requis";
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    if (!validateForm()) {
      setLoading(false);
      return;
    }

    try {
      // Connexion Firebase
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;

      // Récupération du token
      const token = await user.getIdToken();

      // Récupération/création des données utilisateur
      const userDocRef = doc(db, "users", user.uid);
      const userDoc = await getDoc(userDocRef);

      let userData;
      if (!userDoc.exists()) {
        // Création d'un profil minimal si inexistant
        userData = {
          uid: user.uid,
          email: user.email,
          displayName: user.displayName || email.split('@')[0],
          role: "client", // Rôle par défaut
          createdAt: new Date().toISOString()
        };
        
        // Sauvegarde dans Firestore
        await setDoc(userDocRef, userData);
      } else {
        userData = userDoc.data();
      }

      // Synchronisation avec PostgreSQL
      const syncResult = await syncUserWithPostgre(token, userData);
      
      if (syncResult.error) {
        console.warn("⚠️ Synchronisation PostgreSQL partielle:", syncResult.message);
        setErrors(prev => ({
          ...prev,
          general: syncResult.message || "Problème de synchronisation"
        }));
      }else {
        // Appeler updateUserRole après la synchronisation réussie
        await updateUserRole(user.uid, userData.role);
      }

      // Redirection
      navigate("/");
    } catch (error: any) {
      console.error("Erreur de connexion:", error);

      const errorMessages: Record<string, string> = {
        'auth/user-not-found': 'Aucun compte trouvé',
        'auth/wrong-password': 'Mot de passe incorrect',
        'auth/too-many-requests': 'Trop de tentatives. Réessayez plus tard.',
        'default': 'Échec de la connexion'
      };

      setErrors(prev => ({
        ...prev,
        general: errorMessages[error.code] || errorMessages['default']
      }));
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
          setErrors(prev => ({
            ...prev,
            submit: syncResult.message || "Problème de synchronisation"
          }));
        } else {
          console.log("Utilisateur synchronisé avec PostgreSQL");
        }
      } catch (syncError) {
        console.error("Erreur lors de la synchronisation:", syncError);
        setErrors(prev => ({
          ...prev,
          submit: "Erreur lors de la synchronisation"
        }));
      }

      // Redirection après connexion réussie
      navigate("/");
    } catch (error: any) {
      console.error("Erreur de connexion Google:", error);
      setErrors(prev => ({
        ...prev,
        submit: 'Échec de la connexion Google. Veuillez réessayer.'
      }));
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    if (!email) {
      setErrors(prev => ({ 
        ...prev, 
        general: "Veuillez saisir votre email." 
      }));
      return;
    }
    setLoading(true);
    try {
      await sendPasswordResetEmail(auth, email);
      setErrors(prev => ({
        ...prev,
        general: "Vérifiez votre email pour réinitialiser.",
      }));
    } catch (error) {
      setErrors(prev => ({
        ...prev, 
        general: "Erreur d'envoi de l'email de réinitialisation." 
      }));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto mt-10 p-6 rounded-lg all">
      <h1 className="text-3xl font-bold text-gray-800 mb-8">
        Connexion à votre compte
      </h1>

      {(errors.general || errors.submit) && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-600 rounded-md">
          {errors.general || errors.submit}
        </div>
      )}

      <form onSubmit={handleLogin}>
        <div className="mb-6">
          <label className="block text-sm text-gray-700 mb-2">
            Adresse email
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="votre-email@exemple.com"
            className={`w-full h-12 px-4 py-2 bg-[#F6F7FB] border-2 rounded-[32px] text-gray-800 ${
              errors.email ? "border-red-500" : "border-[#F6F7FB]"
            }`}
            disabled={loading}
          />
          {errors.email && (
            <p className="text-red-500 text-xs mt-1 px-4">{errors.email}</p>
          )}
        </div>

        <div className="mb-6">
          <label className="block text-sm text-gray-700 mb-2">Mot de passe</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••••••"
            className={`w-full h-12 px-4 py-2 bg-[#F6F7FB] border-2 rounded-[32px] text-gray-800 ${
              errors.password ? "border-red-500" : "border-[#F6F7FB]"
            }`}
            disabled={loading}
          />
          {errors.password && (
            <p className="text-red-500 text-xs mt-1 px-4">{errors.password}</p>
          )}
        </div>

        <div className="flex items-center mb-6">
          <div className="relative flex items-center">
            <input
              type="checkbox"
              id="rememberMe"
              checked={rememberMe}
              onChange={() => setRememberMe(!rememberMe)}
              className="opacity-0 absolute h-5 w-5"
              disabled={loading}
            />
            <div
              className={`mr-3 flex items-center justify-center w-6 h-6 ${
                rememberMe ? "btncolor" : "border-2 border-gray-300"
              } rounded-full transition-colors duration-200`}
            >
              {rememberMe && (
                <svg
                  className="w-4 h-4 text-white"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" />
                </svg>
              )}
            </div>
            <label
              htmlFor="rememberMe"
              className="text-gray-700 select-none cursor-pointer"
            >
              Se souvenir de moi
            </label>
          </div>
        </div>

        <button
          type="submit"
          className="w-full h-12 btncolor text-white font-bold rounded-full hover:bg-green-600 transition-colors"
          disabled={loading}
        >
          {loading ? "Connexion..." : "Connexion"}
        </button>

        <div className="my-6 flex items-center">
          <div className="flex-grow border-t border-gray-300"></div>
          <span className="px-3 text-gray-500 text-sm">OU</span>
          <div className="flex-grow border-t border-gray-300"></div>
        </div>

        <button
          type="button"
          className="w-full flex items-center justify-center space-x-2 mb-6 px-4 py-3 border-2 border-[#F6F7FB] rounded-full hover:bg-gray-50 transition-colors"
          onClick={handleGoogleLogin}
          disabled={loading}
        >
          <svg width="20" height="20" viewBox="0 0 24 24">
            <path
              fill="#4285F4"
              d="M21.35 11.1h-9.17v2.83h6.51c-.33 3.81-3.5 5.44-6.5 5.44C8.36 19.37 5 16.25 5 12c0-4.1 3.2-7.27 7.2-7.27 3.09 0 4.9 1.97 4.9 1.97L19 4.72S16.56 2 12.1 2C6.42 2 2.03 6.8 2.03 12c0 5.05 4.13 10 10.22 10 5.35 0 9.25-3.67 9.25-9.09 0-1.15-.15-1.81-.15-1.81z"
            />
          </svg>
          <span className="font-medium">Connexion avec Google</span>
        </button>

        <div className="flex justify-between items-center mt-6">
          <button
            type="button"
            onClick={() => navigate("/sign-up")}
            className="text-gray-800 border-[#F6F7FB] border-2 rounded-4xl px-9 py-4 font-bold hover:text-green-600 transition-colors"
            disabled={loading}
          >
            Créer un compte
          </button>

          <button
            type="button"
            className="text-gray-600 hover:text-green-600 transition-colors"
            onClick={handleForgotPassword}
            disabled={loading}
          >
            Mot de passe oublié ?
          </button>
        </div>
      </form>
    </div>
  );
};

export default LoginForm;