import { useState, useEffect } from "react";
import { auth, db } from "../../../config/Firebase";
import {
  collection,
  getDocs,
  query,
  where,
  onSnapshot,
  doc,
} from "firebase/firestore";
import { 
  Calendar, 
  FileText, 
  TrendingUp, 
  TrendingDown,
  AlertTriangle,
  PieChart as PieChartIcon,
  BarChart2,
  Activity,
  Clock,
  Percent
} from "lucide-react";
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  AreaChart,
  Area,
} from "recharts";

// Import dashboard components
import StatCard from "./StatCard";
import LowStockAlert from "./LowStockAlert";
import StorePerformance from "./StorePerformance";
import RecentOrders from "./RecentOrders";
import DashboardNavigation from "./DashboardNavigation";

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [userData, setUserData] = useState(null);
  const [stats, setStats] = useState(null);
  const [recentOrders, setRecentOrders] = useState([]);
  const [lowStockProducts, setLowStockProducts] = useState([]);
  const [storePerformance, setStorePerformance] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [dateRange, setDateRange] = useState('30');
  const [chartData, setChartData] = useState({
    dailyRevenue: [],
    categoryDistribution: [],
    stockStatus: [],
    monthlyComparison: [],
    topSellingProducts: [],
    revenueByStore: [],
  });

  // Check user authentication and role
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (!user) {
        window.location.href = "/login?redirect=dashboard";
        return;
      }

      try {
        const userRef = doc(db, "users", user.uid);
        const unsubscribeUser = onSnapshot(userRef, (docSnap) => {
          if (docSnap.exists()) {
            const userData = docSnap.data();
            setUserData(userData);

            if (userData.role !== "vendeur") {
              window.location.href = "/account";
            }
          } else {
            window.location.href = "/account";
          }
        });

        loadDashboardData(user.uid);

        return () => unsubscribeUser();
      } catch (error) {
        console.error("Error fetching user data:", error);
      }
    });

    return () => unsubscribe();
  }, []);

  // Load dashboard data
  const loadDashboardData = async (userId) => {
    setLoading(true);
    try {
      // Fetch boutiques
      const boutiquesResponse = await fetch(`/api/boutiques/vendeur/${userId}`);
      const boutiques = await boutiquesResponse.json();

      // Fetch stocks
      let allStocks = [];
      for (const boutique of boutiques) {
        const stocksResponse = await fetch(`/api/stocks/boutique/${boutique.id_boutique}`);
        const stocks = await stocksResponse.json();
        allStocks = [...allStocks, ...stocks];
      }

      // Fetch products
      let allProduits = [];
      for (const stock of allStocks) {
        const produitsResponse = await fetch(`/api/produits/stock/${stock.id}`);
        const produits = await produitsResponse.json();
        allProduits = [...allProduits, ...produits];
      }

      // Fetch notifications
      const notificationsResponse = await fetch(`/api/notifications/vendeur/${userId}`);
      const notifs = await notificationsResponse.json();
      setNotifications(notifs);

      // Fetch transactions
      let allTransactions = [];
      for (const stock of allStocks) {
        const transactionsResponse = await fetch(`/api/stock-transactions/stock/${stock.id}`);
        const trans = await transactionsResponse.json();
        allTransactions = [...allTransactions, ...trans];
      }

      // Fetch categories
      const categoriesResponse = await fetch(`/api/categories`);
      const categories = await categoriesResponse.json();

      // Calculate stats with real percentage changes
      const { stats, percentageChanges } = calculateStats(allTransactions, allProduits);
      setStats({
        ...stats,
        ...percentageChanges
      });

      // Process data for charts
      processChartData(allTransactions, allProduits, boutiques, allStocks, categories);

      // Process low stock products
      const lowStock = allProduits
        .filter(p => p.quantite < p.seuilCritique)
        .map(p => {
          const stock = allStocks.find(s => s.id === p.idStock);
          const boutique = boutiques.find(b => b.id_boutique === stock?.idBoutique);
          return {
            id: p.id,
            name: p.nomProduit,
            sku: `SKU-${p.id}`,
            store: boutique?.nom || 'Unknown Store',
            stock: p.quantite,
            threshold: p.seuilCritique,
          };
        });
      setLowStockProducts(lowStock);

      // Process recent orders
      const recent = allTransactions
        .filter(t => t.type === "REMOVE")
        .sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate))
        .slice(0, 5)
        .map(t => ({
          id: t.id,
          customer: `Client #${t.id}`,
          status: "Delivered",
          date: new Date(t.transactionDate).toLocaleDateString(),
          total: `£${(t.prix * t.quantity).toFixed(2)}`,
        }));
      setRecentOrders(recent);

      // Process store performance
      const performance = calculateStorePerformance(boutiques, allStocks, allTransactions, allProduits);
      setStorePerformance(performance);

      setLoading(false);
    } catch (error) {
      console.error("Error loading dashboard data:", error);
      setLoading(false);
    }
  };

  const calculateStats = (transactions, products) => {
    const currentDate = new Date();
    const currentMonth = currentDate.getMonth();
    const currentYear = currentDate.getFullYear();
    const lastMonth = currentMonth === 0 ? 11 : currentMonth - 1;
    const lastMonthYear = currentMonth === 0 ? currentYear - 1 : currentYear;

    // Filter transactions by month
    const currentMonthTransactions = transactions.filter(t => {
      const date = new Date(t.transactionDate);
      return date.getMonth() === currentMonth && date.getFullYear() === currentYear;
    });

    const lastMonthTransactions = transactions.filter(t => {
      const date = new Date(t.transactionDate);
      return date.getMonth() === lastMonth && date.getFullYear() === lastMonthYear;
    });

    // Calculate revenues
    const calculateRevenue = (transactionsList) => {
      return transactionsList
        .filter(t => t.type === "REMOVE")
        .reduce((sum, t) => {
          const prix = t.prix || products.find(p => p.id === t.productId)?.prix || 0;
          return sum + (prix * t.quantity);
        }, 0);
    };

    const currentRevenue = calculateRevenue(currentMonthTransactions);
    const lastRevenue = calculateRevenue(lastMonthTransactions);
    const revenueChange = lastRevenue ? ((currentRevenue - lastRevenue) / lastRevenue * 100) : 0;

    // Calculate orders
    const currentOrders = currentMonthTransactions.filter(t => t.type === "REMOVE").length;
    const lastOrders = lastMonthTransactions.filter(t => t.type === "REMOVE").length;
    const ordersChange = lastOrders ? ((currentOrders - lastOrders) / lastOrders * 100) : 0;

    // Calculate products sold
    const calculateProductsSold = (transactionsList) => {
      return transactionsList
        .filter(t => t.type === "REMOVE")
        .reduce((sum, t) => sum + t.quantity, 0);
    };

    const currentSold = calculateProductsSold(currentMonthTransactions);
    const lastSold = calculateProductsSold(lastMonthTransactions);
    const soldChange = lastSold ? ((currentSold - lastSold) / lastSold * 100) : 0;

    // Calculate low stock items
    const lowStockItems = products.filter(p => p.quantite < p.seuilCritique).length;

    return {
      stats: {
        totalRevenue: `£${currentRevenue.toFixed(2)}`,
        totalOrders: currentOrders,
        productsSold: currentSold,
        lowStockItems,
      },
      percentageChanges: {
        revenueChange: `${revenueChange >= 0 ? '+' : ''}${revenueChange.toFixed(1)}%`,
        ordersChange: `${ordersChange >= 0 ? '+' : ''}${ordersChange.toFixed(1)}%`,
        soldChange: `${soldChange >= 0 ? '+' : ''}${soldChange.toFixed(1)}%`,
        lowStockChange: lowStockItems.toString(),
      }
    };
  };

  // Function to generate and download PDF report
  const generateReport = async () => {
    try {
      // Create HTML content for PDF
      let htmlContent = `
        <html>
          <head>
            <title>Rapport de Performance</title>
            <style>
              body { font-family: Arial, sans-serif; padding: 20px; }
              h1 { color: #2c3e50; border-bottom: 2px solid #2c3e50; padding-bottom: 10px; }
              h2 { color: #34495e; margin-top: 30px; }
              table { width: 100%; border-collapse: collapse; margin: 20px 0; }
              th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
              th { background-color: #f2f2f2; }
              .summary { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
              .category-chart { margin: 20px 0; }
            </style>
          </head>
          <body>
            <h1>Rapport de Performance - ${new Date().toLocaleDateString('fr-FR')}</h1>
            
            <div class="summary">
              <h2>Résumé</h2>
              <p><strong>Chiffre d'affaires total:</strong> ${stats?.totalRevenue}</p>
              <p><strong>Nombre de commandes:</strong> ${stats?.totalOrders}</p>
              <p><strong>Produits vendus:</strong> ${stats?.productsSold}</p>
              <p><strong>Articles en rupture de stock:</strong> ${stats?.lowStockItems}</p>
            </div>

            <h2>Répartition par Catégorie</h2>
            <table>
              <thead>
                <tr>
                  <th>Catégorie</th>
                  <th>Nombre de produits</th>
                  <th>Pourcentage</th>
                </tr>
              </thead>
              <tbody>
                ${chartData.categoryDistribution.map(cat => `
                  <tr>
                    <td>${cat.name}</td>
                    <td>${cat.value}</td>
                    <td>${cat.percentage}%</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>

            <h2>Top Produits</h2>
            <table>
              <thead>
                <tr>
                  <th>Produit</th>
                  <th>Quantité vendue</th>
                  <th>Chiffre d'affaires</th>
                </tr>
              </thead>
              <tbody>
                ${chartData.topSellingProducts.map(product => `
                  <tr>
                    <td>${product.name}</td>
                    <td>${product.quantity}</td>
                    <td>£${product.revenue.toFixed(2)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>

            <h2>Performance par Boutique</h2>
            <table>
              <thead>
                <tr>
                  <th>Boutique</th>
                  <th>Chiffre d'affaires</th>
                  <th>Commandes</th>
                  <th>Variation</th>
                </tr>
              </thead>
              <tbody>
                ${storePerformance.map(store => `
                  <tr>
                    <td>${store.name}</td>
                    <td>${store.revenue}</td>
                    <td>${store.orders}</td>
                    <td>${store.change}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>

            <h2>Performance Mensuelle</h2>
            <table>
              <thead>
                <tr>
                  <th>Mois</th>
                  <th>Chiffre d'affaires</th>
                  <th>Commandes</th>
                </tr>
              </thead>
              <tbody>
                ${chartData.monthlyComparison.map(month => `
                  <tr>
                    <td>${month.month}</td>
                    <td>£${month.revenue.toFixed(2)}</td>
                    <td>${month.orders}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </body>
        </html>
      `;

      // Create a Blob from the HTML content
      const blob = new Blob([htmlContent], { type: 'text/html' });
      
      // Create a temporary iframe to render the HTML
      const iframe = document.createElement('iframe');
      iframe.style.display = 'none';
      document.body.appendChild(iframe);
      
      // Write the HTML content to the iframe
      const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
      iframeDoc.open();
      iframeDoc.write(htmlContent);
      iframeDoc.close();
      
      // Wait for images to load (if any)
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // Print the iframe content as PDF
      iframe.contentWindow.print();
      
      // Remove the iframe after printing
      setTimeout(() => {
        document.body.removeChild(iframe);
      }, 1000);
      
    } catch (error) {
      console.error("Erreur lors de la génération du rapport:", error);
      alert("Une erreur est survenue lors de la génération du rapport.");
    }
  };

  const processChartData = (transactions, products, boutiques, stocks, categories) => {
    // Daily revenue for the last 30 days
    const last30Days = Array.from({ length: 30 }, (_, i) => {
      const date = new Date();
      date.setDate(date.getDate() - i);
      return date.toISOString().split('T')[0];
    }).reverse();

    const dailyRevenue = last30Days.map(date => {
      const dayTransactions = transactions.filter(t => 
        t.type === "REMOVE" && 
        new Date(t.transactionDate).toISOString().split('T')[0] === date
      );
      
      const revenue = dayTransactions.reduce((sum, t) => {
        const prix = t.prix || products.find(p => p.id === t.productId)?.prix || 0;
        return sum + (prix * t.quantity);
      }, 0);

      return {
        date: new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' }),
        revenue,
      };
    });

    // Category distribution - Count products by category
    const categoryCount = {};
    
    // Debug détaillé
    console.log("=== DEBUT DEBUG CATEGORIES ===");
    console.log("Nombre total de produits:", products.length);
    console.log("Nombre total de catégories:", categories.length);
    console.log("Structure d'un produit exemple:", products[0]);
    console.log("Structure d'une catégorie exemple:", categories[0]);
    
    products.forEach((product, index) => {
      console.log(`Produit ${index + 1}:`, {
        id: product.id,
        nom: product.nomProduit,
        idCategorie: product.idCategorie,
        idCategorieType: typeof product.idCategorie
      });
      
      // Vérifier les différentes propriétés possibles pour l'ID de catégorie
      const possibleCategoryId = product.idCategorie || product.categoryId || product.categorie_id || product.categorieId;
      
      if (possibleCategoryId) {
        // Chercher la catégorie correspondante
        const category = categories.find(c => {
          // Vérifier plusieurs champs possibles pour l'ID
          const categoryId = c.idCategorie || c.id || c.categoryId || c._id;
          
          console.log(`Comparaison catégorie:`, {
            categoryId: categoryId,
            productCategoryId: possibleCategoryId,
            matching: categoryId?.toString() === possibleCategoryId?.toString()
          });
          
          return categoryId?.toString() === possibleCategoryId?.toString();
        });
        
        if (category) {
          // Vérifier le nom de la catégorie
          const categoryName = category.nom || category.name || category.libelle || category.title;
          console.log(`Catégorie trouvée pour le produit ${product.nomProduit}: ${categoryName}`);
          
          if (categoryName) {
            categoryCount[categoryName] = (categoryCount[categoryName] || 0) + 1;
          } else {
            console.log("Catégorie sans nom trouvée:", category);
            categoryCount['Catégorie sans nom'] = (categoryCount['Catégorie sans nom'] || 0) + 1;
          }
        } else {
          console.log(`Aucune catégorie trouvée pour le produit ${product.nomProduit} avec ID catégorie: ${possibleCategoryId}`);
          categoryCount['Non catégorisé'] = (categoryCount['Non catégorisé'] || 0) + 1;
        }
      } else {
        console.log(`Produit ${product.nomProduit} n'a pas d'ID de catégorie`);
        categoryCount['Sans catégorie'] = (categoryCount['Sans catégorie'] || 0) + 1;
      }
    });

    console.log("=== FIN DEBUG CATEGORIES ===");
    console.log("Répartition finale des catégories:", categoryCount);

    const categoryDistribution = Object.entries(categoryCount).map(([name, value]) => ({
      name,
      value,
      percentage: ((value / products.length) * 100).toFixed(1)
    }));

    // Stock status distribution
    const stockStatus = [
      { name: 'Critique', value: products.filter(p => p.quantite === 0).length },
      { name: 'Bas', value: products.filter(p => p.quantite > 0 && p.quantite < p.seuilCritique).length },
      { name: 'Normal', value: products.filter(p => p.quantite >= p.seuilCritique && p.quantite < p.seuilCritique * 2).length },
      { name: 'Élevé', value: products.filter(p => p.quantite >= p.seuilCritique * 2).length },
    ];

    // Monthly comparison
    const last6Months = Array.from({ length: 6 }, (_, i) => {
      const date = new Date();
      date.setMonth(date.getMonth() - i);
      return { month: date.getMonth(), year: date.getFullYear() };
    }).reverse();

    const monthlyComparison = last6Months.map(({ month, year }) => {
      const monthTransactions = transactions.filter(t => {
        const date = new Date(t.transactionDate);
        return date.getMonth() === month && date.getFullYear() === year;
      });

      const revenue = monthTransactions
        .filter(t => t.type === "REMOVE")
        .reduce((sum, t) => {
          const prix = t.prix || products.find(p => p.id === t.productId)?.prix || 0;
          return sum + (prix * t.quantity);
        }, 0);

      const orders = monthTransactions.filter(t => t.type === "REMOVE").length;

      return {
        month: new Date(year, month).toLocaleDateString('fr-FR', { month: 'short' }),
        revenue,
        orders,
      };
    });

    // Top selling products
    const productSales = {};
    transactions.filter(t => t.type === "REMOVE").forEach(t => {
      const product = products.find(p => p.id === t.productId);
      if (product) {
        if (!productSales[product.id]) {
          productSales[product.id] = {
            name: product.nomProduit,
            quantity: 0,
            revenue: 0,
          };
        }
        productSales[product.id].quantity += t.quantity;
        productSales[product.id].revenue += (t.prix || product.prix) * t.quantity;
      }
    });

    const topSellingProducts = Object.values(productSales)
      .sort((a, b) => b.revenue - a.revenue)
      .slice(0, 5);

    // Revenue by store
    const revenueByStore = boutiques.map(boutique => {
      const boutiqueStocks = stocks.filter(s => s.idBoutique === boutique.id_boutique);
      const boutiqueTransactions = transactions.filter(t => 
        boutiqueStocks.some(s => s.id === t.stockId)
      );
      
      const revenue = boutiqueTransactions
        .filter(t => t.type === "REMOVE")
        .reduce((sum, t) => {
          const prix = t.prix || products.find(p => p.id === t.productId)?.prix || 0;
          return sum + (prix * t.quantity);
        }, 0);

      return {
        name: boutique.nom,
        revenue,
      };
    });

    setChartData({
      dailyRevenue,
      categoryDistribution,
      stockStatus,
      monthlyComparison,
      topSellingProducts,
      revenueByStore,
    });
  };

  const calculateStorePerformance = (boutiques, stocks, transactions, products) => {
    return boutiques.map(boutique => {
      const boutiqueStocks = stocks.filter(s => s.idBoutique === boutique.id_boutique);
      const boutiqueTransactions = transactions.filter(t => 
        boutiqueStocks.some(s => s.id === t.stockId)
      );
      
      const revenue = boutiqueTransactions
        .filter(t => t.type === "REMOVE")
        .reduce((sum, t) => {
          const prix = t.prix || products.find(p => p.id === t.productId)?.prix || 0;
          return sum + (prix * t.quantity);
        }, 0);
      
      const orders = boutiqueTransactions.filter(t => t.type === "REMOVE").length;
      
      // Calculate percentage change from last month
      const currentMonth = new Date().getMonth();
      const lastMonth = currentMonth === 0 ? 11 : currentMonth - 1;
      
      const currentMonthOrders = boutiqueTransactions
        .filter(t => t.type === "REMOVE" && new Date(t.transactionDate).getMonth() === currentMonth)
        .length;
      const lastMonthOrders = boutiqueTransactions
        .filter(t => t.type === "REMOVE" && new Date(t.transactionDate).getMonth() === lastMonth)
        .length;
      
      const change = lastMonthOrders ? 
        ((currentMonthOrders - lastMonthOrders) / lastMonthOrders * 100).toFixed(0) :
        "0";
      
      return {
        id: boutique.id_boutique,
        name: boutique.nom,
        revenue: `£${revenue.toFixed(2)}`,
        orders,
        percentage: Math.min(100, (orders / 100) * 100),
        changeUp: parseFloat(change) >= 0,
        change: `${change}%`,
      };
    });
  };

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d'];

  if (loading) {
    return (
      <div className="w-full p-6 flex justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  // Custom Label Renderer for Category Pie Chart
  const renderCategoryLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent, name, value }) => {
    const RADIAN = Math.PI / 180;
    const radius = innerRadius + (outerRadius - innerRadius) * 1.2;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);

    return (
      <text 
        x={x} 
        y={y} 
        fill="black" 
        textAnchor={x > cx ? 'start' : 'end'} 
        dominantBaseline="central"
        className="text-xs font-medium"
      >
        {`${name}: ${(percent * 100).toFixed(1)}%`}
      </text>
    );
  };

  return (
    <div className="space-y-6">
      {/* Dashboard Navigation */}
      <DashboardNavigation userData={userData} notifications={notifications} />

      {/* Dashboard Content */}
      <div className="space-y-6">
        {/* Dashboard Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <h1 className="text-2xl font-bold text-gray-800">
            Dashboard Overview
          </h1>
          <div className="flex items-center space-x-2">
            <select
              value={dateRange}
              onChange={(e) => setDateRange(e.target.value)}
              className="px-4 py-2 text-sm font-medium bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50"
            >
              <option value="7">Last 7 days</option>
              <option value="30">Last 30 days</option>
              <option value="90">Last 90 days</option>
            </select>
            <button 
              onClick={generateReport}
              className="px-4 py-2 text-sm font-medium text-white bg-teal-600 rounded-md shadow-sm hover:bg-teal-700"
            >
              <FileText size={16} className="inline mr-2" />
              Export PDF
            </button>
          </div>
        </div>

        {/* Stats Overview */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            title="Total Revenue"
            value={stats?.totalRevenue}
            change={stats?.revenueChange}
            positive={stats?.revenueChange?.startsWith('+')}
            icon="dollar"
            bgClass="from-green-50 to-teal-50 border-green-200"
          />
          <StatCard
            title="Total Orders"
            value={stats?.totalOrders}
            change={stats?.ordersChange}
            positive={stats?.ordersChange?.startsWith('+')}
            icon="shopping-bag"
            bgClass="from-blue-50 to-indigo-50 border-blue-200"
          />
          <StatCard
            title="Products Sold"
            value={stats?.productsSold}
            change={stats?.soldChange}
            positive={stats?.soldChange?.startsWith('+')}
            icon="package"
            bgClass="from-purple-50 to-pink-50 border-purple-200"
          />
          <StatCard
            title="Low Stock Items"
            value={stats?.lowStockItems}
            change={stats?.lowStockChange}
            positive={false}
            icon="alert-triangle"
            bgClass="from-amber-50 to-orange-50 border-amber-200"
          />
        </div>

        {/* Charts Section */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Revenue Chart */}
          <div className="bg-white p-6 rounded-lg shadow">
            <h3 className="text-lg font-semibold mb-4 flex items-center">
              <TrendingUp className="h-5 w-5 mr-2 text-blue-500" />
              Revenue Trend
            </h3>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={chartData.dailyRevenue}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip />
                <Area 
                  type="monotone" 
                  dataKey="revenue" 
                  stroke="#0088FE" 
                  fill="#0088FE" 
                  fillOpacity={0.3} 
                  name="Revenue"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Category Distribution */}
          <div className="bg-white p-6 rounded-lg shadow">
            <h3 className="text-lg font-semibold mb-4 flex items-center">
              <PieChartIcon className="h-5 w-5 mr-2 text-green-500" />
              Catégories de Produits
            </h3>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={chartData.categoryDistribution}
                  cx="50%"
                  cy="50%"
                  labelLine={true}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                  label={renderCategoryLabel}
                >
                  {chartData.categoryDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  formatter={(value, name, props) => [
                    `${value} produits (${props.payload.percentage}%)`, 
                    name
                  ]} 
                />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
            
            {/* Category Legend */}
            <div className="mt-4 grid grid-cols-2 gap-2">
              {chartData.categoryDistribution.map((category, index) => (
                <div key={index} className="flex items-center">
                  <div 
                    className="w-3 h-3 rounded-full mr-2" 
                    style={{ backgroundColor: COLORS[index % COLORS.length] }}
                  />
                  <span className="text-sm">
                    {category.name}: {category.value} ({category.percentage}%)
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Additional Charts */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Stock Status */}
          <div className="bg-white p-6 rounded-lg shadow">
            <h3 className="text-lg font-semibold mb-4 flex items-center">
              <AlertTriangle className="h-5 w-5 mr-2 text-yellow-500" />
              Stock Status
            </h3>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={chartData.stockStatus}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="value" fill="#8884d8">
                  {chartData.stockStatus.map((entry, index) => (
                    <Cell 
                      key={`cell-${index}`} 
                      fill={index === 0 ? '#ff4d4f' : index === 1 ? '#faad14' : index === 2 ? '#52c41a' : '#1890ff'} 
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Monthly Comparison */}
          <div className="bg-white p-6 rounded-lg shadow">
            <h3 className="text-lg font-semibold mb-4 flex items-center">
              <BarChart2 className="h-5 w-5 mr-2 text-purple-500" />
              Monthly Performance
            </h3>
            <ResponsiveContainer width="100%" height={250}>
              <LineChart data={chartData.monthlyComparison}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis yAxisId="left" />
                <YAxis yAxisId="right" orientation="right" />
                <Tooltip />
                <Legend />
                <Line 
                  yAxisId="left"
                  type="monotone" 
                  dataKey="revenue" 
                  stroke="#8884d8" 
                  name="Revenue"
                />
                <Line 
                  yAxisId="right"
                  type="monotone" 
                  dataKey="orders" 
                  stroke="#82ca9d" 
                  name="Orders"
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* Top Selling Products */}
          <div className="bg-white p-6 rounded-lg shadow">
            <h3 className="text-lg font-semibold mb-4 flex items-center">
              <Activity className="h-5 w-5 mr-2 text-red-500" />
              Top Products
            </h3>
            <div className="space-y-4">
              {chartData.topSellingProducts.map((product, index) => (
                <div key={index} className="flex items-center justify-between">
                  <div className="flex items-center">
                    <span className="w-6 h-6 flex items-center justify-center bg-gray-100 rounded-full text-sm font-medium">
                      {index + 1}
                    </span>
                    <span className="ml-2 text-sm">{product.name}</span>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-medium">£{product.revenue.toFixed(2)}</div>
                    <div className="text-xs text-gray-500">{product.quantity} sold</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Main Content Sections */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Low Stock Products Section */}
          <div className="lg:col-span-1">
            <LowStockAlert products={lowStockProducts} />
          </div>

          {/* Recent Orders and Store Performance */}
          <div className="lg:col-span-2 space-y-6">
            {/* Store Performance */}
            <StorePerformance stores={storePerformance} />
          </div>
        </div>

        {/* Recent Orders and Revenue by Store in a row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Recent Orders */}
          <div>
            <RecentOrders orders={recentOrders} />
          </div>

          {/* Revenue by Store */}
          <div className="bg-white p-6 rounded-lg shadow">
            <h3 className="text-lg font-semibold mb-4 flex items-center">
              <Percent className="h-5 w-5 mr-2 text-indigo-500" />
              Revenue by Store
            </h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chartData.revenueByStore}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="revenue" fill="#8884d8">
                  {chartData.revenueByStore.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;