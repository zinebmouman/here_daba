import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getAnalytics } from "firebase/analytics";
import { getFirestore } from 'firebase/firestore';
// Configuration Firebase
const firebaseConfig = {
  apiKey: "AIzaSyDkZpXkFKq--JPdw19A3_Kwe4c1p4_5XFo",
  authDomain: "here-545a9.firebaseapp.com",
  projectId: "here-545a9",
  storageBucket: "here-545a9.appspot.com", // Correction ici
  messagingSenderId: "528284722054",
  appId: "1:528284722054:web:54f2c02320562c3bfe8207",
  measurementId: "G-SSLT9MH13C"
};

// Initialisation de Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const googleProvider = new GoogleAuthProvider();
const analytics = getAnalytics(app);

// Obtenir l'instance Firestore déjà initialisée
const db = getFirestore(app);

// Configuration d'Axios pour les appels API
export { auth, googleProvider, analytics, db };
export const axiosConfig = {
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json'
  }
};