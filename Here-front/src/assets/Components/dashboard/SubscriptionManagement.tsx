import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  CreditCard,
  CheckCircle,
  Clock,
  HardDrive,
  Calendar,
  ChevronRight,
  Award,
  ShieldCheck,
} from "lucide-react";
import { auth, db } from "../../../config/Firebase";
import DashboardNavigation from "./DashboardNavigation";

const SubscriptionManagement = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [currentPlan, setCurrentPlan] = useState(null);
  const [userDetails, setUserDetails] = useState(null);

  console.log("SubscriptionManagement rendered at path:", location.pathname);

  // Available subscription plans
  const subscriptionPlans = [
    {
      id: "basic",
      name: "Starter",
      storage: "30 Go",
      duration: "1 an",
      price: 599,
      priceUnit: "MAD",
      features: [
        "Accès à la plateforme marchande",
        "Gestion des commandes",
        "Gestion des produits (jusqu'à 100)",
        "Support par email",
        "1 boutique",
      ],
      popular: false,
    },
    {
      id: "business",
      name: "Business",
      storage: "100 Go",
      duration: "1 an",
      price: 1299,
      priceUnit: "MAD",
      features: [
        "Tout ce qui est inclus dans Starter",
        "Gestion des produits illimitée",
        "Statistiques avancées",
        "Support prioritaire",
        "3 boutiques",
        "Promotions illimitées",
      ],
      popular: true,
    },
    {
      id: "enterprise",
      name: "Enterprise",
      storage: "500 Go",
      duration: "1 an",
      price: 2999,
      priceUnit: "MAD",
      features: [
        "Tout ce qui est inclus dans Business",
        "Gestion multi-boutiques (10 max)",
        "API pour intégrations personnalisées",
        "Support dédié 24/7",
        "Rapports personnalisés",
        "Formation et onboarding",
      ],
      popular: false,
    },
  ];

  // Check user authentication and role
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (!user) {
        navigate("/login?redirect=account/subscriptions");
        return;
      }

      try {
        // In a real app, fetch the current subscription from Firestore
        // For demo purposes, we'll simulate a user with a subscription
        setCurrentPlan(mockData.subscription);
        setUserDetails(mockData.user);
        setLoading(false);
      } catch (error) {
        console.error("Error fetching data:", error);
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, [navigate]);

  // Function to handle subscription change or purchase
  const handleSubscribe = (planId) => {
    // In a real app, this would redirect to a payment gateway or checkout page
    alert(
      `Vous avez choisi le plan ${planId}. Dans une application réelle, vous seriez redirigé vers une passerelle de paiement.`
    );
  };

  // Format date
  const formatDate = (dateString) => {
    const options = { year: "numeric", month: "long", day: "numeric" };
    return new Date(dateString).toLocaleDateString("fr-FR", options);
  };

  if (loading) {
    return (
      <div className="w-full p-6 flex justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Dashboard Navigation */}
      <DashboardNavigation />

      {/* Subscription Management Content */}
      <div className="space-y-8">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Gestion d'Abonnement
          </h1>
          <p className="text-gray-500 mt-1">
            Gérez votre abonnement et découvrez nos différentes offres
          </p>
        </div>

        {/* Current Subscription Info */}
        {currentPlan && (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="px-6 py-5 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-800">
                Votre Abonnement Actuel
              </h2>
            </div>
            <div className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <div className="space-y-2">
                  <div className="text-sm text-gray-500">Plan</div>
                  <div className="flex items-center">
                    <Award className="h-5 w-5 text-teal-500 mr-2" />
                    <span className="font-semibold text-gray-900">
                      {currentPlan.plan}
                    </span>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="text-sm text-gray-500">Stockage</div>
                  <div className="flex items-center">
                    <HardDrive className="h-5 w-5 text-teal-500 mr-2" />
                    <span className="font-semibold text-gray-900">
                      {currentPlan.usedStorage} / {currentPlan.totalStorage}
                    </span>
                  </div>
                  <div className="relative w-full h-2 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className="absolute h-full bg-teal-500 rounded-full"
                      style={{
                        width: `${
                          (parseInt(currentPlan.usedStorage) /
                            parseInt(currentPlan.totalStorage)) *
                          100
                        }%`,
                      }}
                    ></div>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="text-sm text-gray-500">Statut</div>
                  <div className="flex items-center">
                    <CheckCircle className="h-5 w-5 text-green-500 mr-2" />
                    <span className="font-semibold text-gray-900">Actif</span>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="text-sm text-gray-500">Renouvellement</div>
                  <div className="flex items-center">
                    <Calendar className="h-5 w-5 text-teal-500 mr-2" />
                    <span className="font-semibold text-gray-900">
                      {formatDate(currentPlan.renewalDate)}
                    </span>
                  </div>
                </div>
              </div>

              <div className="mt-6 pt-6 border-t border-gray-200">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm text-gray-500">
                      Moyen de paiement
                    </div>
                    <div className="mt-1 flex items-center">
                      <CreditCard className="h-5 w-5 text-gray-400 mr-2" />
                      <span className="text-gray-900">
                        •••• •••• •••• {currentPlan.paymentCard}
                      </span>
                    </div>
                  </div>

                  <div>
                    <button className="px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
                      Gérer le paiement
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Subscription Plans */}
        <div>
          <h2 className="text-xl font-semibold text-gray-800 mb-4">
            Nos Offres d'Abonnement
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {subscriptionPlans.map((plan) => (
              <div
                key={plan.id}
                className={`bg-white rounded-lg shadow overflow-hidden border-2 transition-all ${
                  plan.popular
                    ? "border-teal-500 transform -translate-y-1"
                    : "border-transparent"
                }`}
              >
                {plan.popular && (
                  <div className="bg-teal-500 text-white text-xs font-semibold px-4 py-1 text-center">
                    Offre la plus populaire
                  </div>
                )}

                <div className="p-6">
                  <h3 className="text-lg font-semibold text-gray-900">
                    {plan.name}
                  </h3>
                  <div className="mt-4 flex items-baseline">
                    <span className="text-3xl font-extrabold text-gray-900">
                      {plan.price}
                    </span>
                    <span className="ml-1 text-xl font-semibold text-gray-500">
                      {plan.priceUnit}
                    </span>
                    <span className="ml-2 text-sm text-gray-500">
                      / {plan.duration}
                    </span>
                  </div>

                  <div className="mt-4 space-y-3">
                    <div className="flex items-center text-sm">
                      <HardDrive className="h-5 w-5 text-teal-500 mr-2" />
                      <span>{plan.storage} de stockage</span>
                    </div>
                    <div className="flex items-center text-sm">
                      <Clock className="h-5 w-5 text-teal-500 mr-2" />
                      <span>Durée: {plan.duration}</span>
                    </div>
                  </div>

                  <div className="mt-6">
                    <h4 className="text-sm font-medium text-gray-900 mb-3">
                      Fonctionnalités
                    </h4>
                    <ul className="space-y-2">
                      {plan.features.map((feature, idx) => (
                        <li key={idx} className="flex items-start">
                          <CheckCircle className="h-5 w-5 text-teal-500 mr-2 flex-shrink-0 mt-0.5" />
                          <span className="text-sm text-gray-700">
                            {feature}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  <div className="mt-6">
                    <button
                      onClick={() => handleSubscribe(plan.id)}
                      className={`w-full flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium ${
                        currentPlan && currentPlan.planId === plan.id
                          ? "bg-gray-100 text-gray-800 cursor-not-allowed"
                          : "bg-teal-600 text-white hover:bg-teal-700"
                      }`}
                      disabled={currentPlan && currentPlan.planId === plan.id}
                    >
                      {currentPlan && currentPlan.planId === plan.id ? (
                        <>
                          <ShieldCheck className="mr-2 h-5 w-5" />
                          Abonnement actuel
                        </>
                      ) : (
                        <>
                          S'abonner
                          <ChevronRight className="ml-1 h-4 w-4" />
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* FAQs */}
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="px-6 py-5 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-800">
              Questions Fréquentes
            </h2>
          </div>
          <div className="p-6 space-y-4">
            <div>
              <h3 className="text-base font-medium text-gray-900">
                Comment puis-je changer mon plan d'abonnement ?
              </h3>
              <p className="mt-2 text-sm text-gray-500">
                Vous pouvez changer votre plan d'abonnement à tout moment en
                sélectionnant un nouveau plan ci-dessus. Le changement prendra
                effet à la fin de votre période de facturation actuelle.
              </p>
            </div>
            <div>
              <h3 className="text-base font-medium text-gray-900">
                Que se passe-t-il si j'atteins ma limite de stockage ?
              </h3>
              <p className="mt-2 text-sm text-gray-500">
                Si vous atteignez votre limite de stockage, vous ne pourrez plus
                ajouter de nouveaux produits ou télécharger des images jusqu'à
                ce que vous libériez de l'espace ou que vous passiez à un
                forfait supérieur.
              </p>
            </div>
            <div>
              <h3 className="text-base font-medium text-gray-900">
                Comment annuler mon abonnement ?
              </h3>
              <p className="mt-2 text-sm text-gray-500">
                Vous pouvez annuler votre abonnement à tout moment en contactant
                notre service client. Votre accès restera actif jusqu'à la fin
                de la période de facturation en cours.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// Mock data for demo purposes
const mockData = {
  user: {
    id: "user123",
    name: "Jean Dupont",
    email: "jean.dupont@example.com",
  },
  subscription: {
    planId: "business",
    plan: "Business",
    startDate: "2023-01-15",
    renewalDate: "2024-01-15",
    totalStorage: "100 Go",
    usedStorage: "42 Go",
    paymentCard: "4567",
  },
};

export default SubscriptionManagement;
