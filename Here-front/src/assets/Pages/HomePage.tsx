import CustomScrollbar from "../Components/CustomScrollbar";
import Navbar from "../Components/Navbar";
import Hero from "../Components/Hero";
import PopularCategories from "../Components/PopularCategories";
import ProductSection from "../Components/ProductSection";
import ExploreInterests from "../Components/ExploreInterests";
import Footer from "../Components/Footer";
import "../../assets/style/Navbar.css"
const HomePage: React.FC = () => {
  return (
    <>
      <CustomScrollbar>
        <Navbar />
        <div className="fullbody">
          <Hero />
          <PopularCategories />
          <ProductSection />
          <ExploreInterests />
          <Footer />
        </div>
      </CustomScrollbar>
    </>
  );
};

export default HomePage;
