import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  User,
  MapPin,
  Upload,
  Camera,
  Edit3,
  Save,
  X,
  CheckCircle,
  Phone,
  Mail,
  Lock,
  Home,
  Calendar,
  Cloud,
} from "lucide-react";
import { auth, db } from "../../config/Firebase"; // Adjust path as needed
import { updateProfile } from "firebase/auth";
import { doc, getDoc, updateDoc } from "firebase/firestore";
import axios from "axios";

const API_URL = "http://api-gateway-url/user/";

// Mock user data - will be replaced with Firebase data
const initialUserData = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  address: "",
  birthday: "",
  profileImage: null,
  location: {
    lat: 33.5731104,
    lng: -7.5898434,
  },
};

const ClientProfileEditor = () => {
  const [userData, setUserData] = useState(initialUserData);
  const [tempImage, setTempImage] = useState(null);
  const [editing, setEditing] = useState(false);
  const [showMap, setShowMap] = useState(false);
  const [mapLoaded, setMapLoaded] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);

  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const fileInputRef = useRef(null);
  const navigate = useNavigate();

  // Fetch user data from Firebase and API
  useEffect(() => {
    const fetchUserData = async () => {
      try {
        const user = auth.currentUser;
        if (!user) {
          console.error("No user is signed in");
          navigate("/sign-in");
          return;
        }

        // Get the Firebase Auth token for API authorization
        const token = await user.getIdToken();

        // Try to fetch user data from the API first
        try {
          const response = await axios.get(`${API_URL}${user.uid}`, {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });

          console.log("User data fetched from API:", response.data);

          // If API request is successful, use that data
          if (response.data) {
            const apiData = response.data;
            setUserData({
              firstName: apiData.firstName || "",
              lastName: apiData.lastName || "",
              email: apiData.email || user.email || "",
              phone: apiData.phone || "",
              address: apiData.address || "",
              birthday: apiData.birthday || "",
              profileImage: apiData.profileImage || user.photoURL || null,
              location: apiData.location || {
                lat: 33.5731104, // Default to Casablanca, Morocco
                lng: -7.5898434,
              },
            });
            setLoading(false);
            return;
          }
        } catch (apiError) {
          console.error("Error fetching user data from API:", apiError);
          // Continue to fetch from Firebase if API fails
        }

        // Fallback to Firebase if API fails
        const userDoc = await getDoc(doc(db, "users", user.uid));
        if (userDoc.exists()) {
          const data = userDoc.data();

          // Extract phone code and number if present
          const phoneMatch = data.phone
            ? data.phone.match(/^(\+\d+)(.*)$/)
            : null;
          const phoneCode = phoneMatch ? phoneMatch[1] : "+212";
          const phoneNumber = phoneMatch ? phoneMatch[2] : "";

          setUserData({
            firstName: data.firstName || "",
            lastName: data.lastName || "",
            email: data.email || user.email || "",
            phone: data.phone || "",
            address: data.address || "",
            birthday: data.birthday || "",
            profileImage: data.profileImage || user.photoURL || null,
            location: data.location || {
              lat: 33.5731104,
              lng: -7.5898434,
            },
          });
        } else {
          // Create a new user profile if one doesn't exist
          const names = user.displayName
            ? user.displayName.split(" ")
            : ["", ""];
          const newUserData = {
            firstName: names[0] || "",
            lastName: names.slice(1).join(" ") || "",
            email: user.email || "",
            profileImage: user.photoURL || null,
          };
          setUserData({
            ...initialUserData,
            ...newUserData,
          });
        }
      } catch (error) {
        console.error("Error fetching user data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [navigate]);

  // Load Google Maps API
  useEffect(() => {
    if (showMap && !mapLoaded) {
      // First check if Google Maps API is already loaded
      if (window.google && window.google.maps) {
        console.log("Google Maps already loaded");
        setMapLoaded(true);
        initializeMap();
        return;
      }

      // Function to initialize map after Google API is loaded
      window.initializeGoogleMaps = function () {
        console.log("Google Maps API loaded successfully");
        setMapLoaded(true);
        setTimeout(initializeMap, 100); // Small delay to ensure DOM is ready
      };

      // Check if the script is already in the document
      const existingScript = document.querySelector(
        'script[src*="maps.googleapis.com/maps/api/js"]'
      );
      if (existingScript) {
        console.log("Google Maps script tag already exists");
        // If script exists but Google isn't defined, it's still loading
        return;
      }

      // Create and add the script
      console.log("Adding Google Maps script to document");
      const script = document.createElement("script");
      // Replace YOUR_API_KEY with your actual API key
      script.src = `https://maps.googleapis.com/maps/api/js?key=YOUR_API_KEY&libraries=places&callback=initializeGoogleMaps`;
      script.async = true;
      script.defer = true;
      script.onerror = () => console.error("Google Maps script failed to load");
      document.head.appendChild(script);

      return () => {
        // Do not remove the script on cleanup as it may be used by other components
        // Just clean up the callback
        delete window.initializeGoogleMaps;
      };
    }
  }, [showMap, mapLoaded]);

  const initializeMap = () => {
    console.log("Initializing map", {
      mapRef: mapRef.current,
      googleExists: !!window.google,
      userData: userData.location,
    });

    // Now we can safely use the google object because the callback ensures it's loaded
    if (!window.google || !mapRef.current) {
      console.error(
        "Google Maps API not loaded or map reference not available"
      );
      return;
    }

    try {
      // Make sure the container has dimensions before creating the map
      if (
        mapRef.current.offsetWidth === 0 ||
        mapRef.current.offsetHeight === 0
      ) {
        console.error("Map container has zero dimensions");
        // Try again later
        setTimeout(initializeMap, 200);
        return;
      }

      const position = {
        lat: userData.location.lat,
        lng: userData.location.lng,
      };

      console.log("Creating map with position:", position);

      const mapOptions = {
        center: position,
        zoom: 15,
        styles: [
          {
            featureType: "all",
            elementType: "geometry.fill",
            stylers: [{ weight: "2.00" }],
          },
          {
            featureType: "administrative",
            elementType: "all",
            stylers: [{ color: "#f2f2f2" }],
          },
          {
            featureType: "landscape",
            elementType: "all",
            stylers: [{ color: "#f2f2f2" }],
          },
          {
            featureType: "poi",
            elementType: "all",
            stylers: [{ visibility: "off" }],
          },
          {
            featureType: "road",
            elementType: "all",
            stylers: [{ saturation: -100 }, { lightness: 45 }],
          },
          {
            featureType: "road.highway",
            elementType: "all",
            stylers: [{ visibility: "simplified" }],
          },
          {
            featureType: "water",
            elementType: "all",
            stylers: [{ color: "#0D9488" }, { visibility: "on" }],
          },
        ],
      };

      // Create the Google Map
      const map = new window.google.maps.Map(mapRef.current, mapOptions);

      // Wait for the map to be fully loaded
      window.google.maps.event.addListenerOnce(map, "idle", function () {
        console.log("Map fully loaded");
      });

      // Create the marker
      const marker = new window.google.maps.Marker({
        position: position,
        map: map,
        draggable: true,
        animation: window.google.maps.Animation.DROP,
        icon: {
          path: window.google.maps.SymbolPath.CIRCLE,
          scale: 10,
          fillColor: "#0D9488",
          fillOpacity: 1,
          strokeWeight: 2,
          strokeColor: "#FFFFFF",
        },
      });

      markerRef.current = marker;

      // Add search box if the element exists
      const input = document.getElementById("map-search-input");
      if (input && window.google.maps.places) {
        console.log("Setting up Places search box");
        const searchBox = new window.google.maps.places.SearchBox(input);

        map.addListener("bounds_changed", () => {
          searchBox.setBounds(map.getBounds());
        });

        searchBox.addListener("places_changed", () => {
          const places = searchBox.getPlaces();

          if (places.length === 0) return;

          const place = places[0];

          if (!place.geometry || !place.geometry.location) return;

          // Update marker position
          marker.setPosition(place.geometry.location);

          // Update map center
          map.setCenter(place.geometry.location);

          // Update user data
          setUserData((prev) => ({
            ...prev,
            address: place.formatted_address || "",
            location: {
              lat: place.geometry.location.lat(),
              lng: place.geometry.location.lng(),
            },
          }));
        });
      } else {
        console.warn("Search box input not found or Places library not loaded");
      }

      // Update user data on marker drag end
      marker.addListener("dragend", () => {
        const position = marker.getPosition();

        setUserData((prev) => ({
          ...prev,
          location: {
            lat: position.lat(),
            lng: position.lng(),
          },
        }));

        // Reverse geocode to get address
        const geocoder = new window.google.maps.Geocoder();
        geocoder.geocode({ location: position }, (results, status) => {
          if (status === "OK" && results[0]) {
            setUserData((prev) => ({
              ...prev,
              address: results[0].formatted_address,
            }));
          }
        });
      });
    } catch (error) {
      console.error("Error initializing Google Map:", error);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setUserData((prev) => ({ ...prev, [name]: value }));

    // Clear error for this field if it exists
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        setTempImage(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const openGoogleDrivePicker = () => {
    // In a real app, implement Google Drive API integration
    alert("Google Drive integration would be implemented here");
  };

  const validateForm = () => {
    const newErrors = {};

    if (!userData.firstName.trim()) {
      newErrors.firstName = "First name is required";
    }

    if (!userData.lastName.trim()) {
      newErrors.lastName = "Last name is required";
    }

    if (!userData.email.trim()) {
      newErrors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(userData.email)) {
      newErrors.email = "Email is invalid";
    }

    if (!userData.phone.trim()) {
      newErrors.phone = "Phone number is required";
    }

    if (!userData.address.trim()) {
      newErrors.address = "Address is required";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSaving(true);

    try {
      const user = auth.currentUser;
      if (!user) {
        throw new Error("No user is signed in");
      }

      // Get the Firebase Auth token for API authorization
      const token = await user.getIdToken();

      // Prepare the user data
      const updatedUserData = {
        firstName: userData.firstName,
        lastName: userData.lastName,
        displayName: `${userData.firstName} ${userData.lastName}`,
        phone: userData.phone,
        email: userData.email,
        address: userData.address,
        birthday: userData.birthday,
        location: userData.location,
        updatedAt: new Date().toISOString(),
      };

      // If there's a temp image, we'd add it to the data
      // In a real app, you'd upload the image to a storage service first
      if (tempImage) {
        updatedUserData.profileImage = tempImage;
      }

      // Try to update via API first
      try {
        const response = await axios.put(
          `${API_URL}${user.uid}`,
          updatedUserData,
          {
            headers: {
              Authorization: `Bearer ${token}`,
              "Content-Type": "application/json",
            },
          }
        );

        console.log("Profile updated through API:", response.data);
      } catch (apiError) {
        console.error("Error updating profile through API:", apiError);
        // If API update fails, update through Firebase directly

        // Update profile in Firebase Authentication
        await updateProfile(user, {
          displayName: `${userData.firstName} ${userData.lastName}`,
          // photoURL would be updated here if you implement photo storage
        });

        // Update user data in Firestore
        await updateDoc(doc(db, "users", user.uid), updatedUserData);
      }

      // Apply temp image to user data if it exists
      if (tempImage) {
        setUserData((prev) => ({ ...prev, profileImage: tempImage }));
        setTempImage(null);
        // Here you would upload the image to Firebase Storage and update the user's photoURL
      }

      setSaveSuccess(true);
      setEditing(false);

      // Reset success message after 3 seconds
      setTimeout(() => {
        setSaveSuccess(false);
      }, 3000);
    } catch (error) {
      console.error("Error saving profile:", error);
      setErrors({ submit: "Failed to save profile. Please try again." });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  return (
    <div className="bg-gray-50 min-h-screen py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-xl ">
          {/* Header */}
          <div className="bg-teal-500 px-6 py-4">
            <div className="flex justify-between items-center">
              <h1 className="text-white text-xl font-bold">My Profile</h1>
              <button
                onClick={() => setEditing(!editing)}
                className="bg-white text-teal-500 rounded-full p-2 hover:bg-teal-50 transition-colors"
              >
                {editing ? <X size={20} /> : <Edit3 size={20} />}
              </button>
            </div>
          </div>

          {/* Profile content */}
          <form onSubmit={handleSubmit} className="p-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Left column - Profile image */}
              <div className="col-span-1">
                <div className="flex flex-col items-center">
                  <div className="relative mb-4">
                    <div className="w-40 h-40 rounded-full overflow-hidden bg-gray-100 border-4 border-teal-500">
                      {tempImage || userData.profileImage ? (
                        <img
                          src={tempImage || userData.profileImage}
                          alt="Profile"
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center bg-teal-100 text-teal-500">
                          <User size={64} />
                        </div>
                      )}
                    </div>

                    {editing && (
                      <div className="absolute bottom-0 right-0">
                        <input
                          type="file"
                          accept="image/*"
                          ref={fileInputRef}
                          onChange={handleImageUpload}
                          className="hidden"
                        />
                        <button
                          type="button"
                          onClick={() => fileInputRef.current.click()}
                          className="bg-teal-500 text-white rounded-full p-2 shadow-md hover:bg-teal-600 transition-colors"
                        >
                          <Camera size={20} />
                        </button>
                      </div>
                    )}
                  </div>

                  {editing && (
                    <div className="flex flex-col gap-3 w-full">
                      <button
                        type="button"
                        onClick={() => fileInputRef.current.click()}
                        className="flex items-center justify-center gap-2 bg-gray-100 text-gray-700 py-2 px-4 rounded-full hover:bg-gray-200 transition-colors"
                      >
                        <Upload size={16} />
                        <span>Upload Photo</span>
                      </button>

                      <button
                        type="button"
                        onClick={openGoogleDrivePicker}
                        className="flex items-center justify-center gap-2 bg-blue-50 text-blue-700 py-2 px-4 rounded-full hover:bg-blue-100 transition-colors"
                      >
                        <Cloud size={16} />
                        <span>From Google Drive</span>
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* Right column - User details */}
              <div className="col-span-2">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* First Name */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      First Name
                    </label>
                    <div className="relative">
                      <input
                        type="text"
                        name="firstName"
                        value={userData.firstName}
                        onChange={handleInputChange}
                        disabled={!editing}
                        className={`
                          w-full px-4 py-2 rounded-full border ${
                            errors.firstName
                              ? "border-red-500"
                              : "border-gray-300"
                          } 
                          ${!editing ? "bg-gray-50" : "bg-white"} 
                          focus:outline-none focus:ring-2 focus:ring-teal-500
                        `}
                      />
                      {errors.firstName && (
                        <p className="mt-1 text-sm text-red-500">
                          {errors.firstName}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Last Name */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Last Name
                    </label>
                    <div className="relative">
                      <input
                        type="text"
                        name="lastName"
                        value={userData.lastName}
                        onChange={handleInputChange}
                        disabled={!editing}
                        className={`
                          w-full px-4 py-2 rounded-full border ${
                            errors.lastName
                              ? "border-red-500"
                              : "border-gray-300"
                          } 
                          ${!editing ? "bg-gray-50" : "bg-white"} 
                          focus:outline-none focus:ring-2 focus:ring-teal-500
                        `}
                      />
                      {errors.lastName && (
                        <p className="mt-1 text-sm text-red-500">
                          {errors.lastName}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Email */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Email
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <Mail size={16} className="text-gray-400" />
                      </div>
                      <input
                        type="email"
                        name="email"
                        value={userData.email}
                        onChange={handleInputChange}
                        disabled={!editing}
                        className={`
                          w-full pl-10 pr-4 py-2 rounded-full border ${
                            errors.email ? "border-red-500" : "border-gray-300"
                          } 
                          ${!editing ? "bg-gray-50" : "bg-white"} 
                          focus:outline-none focus:ring-2 focus:ring-teal-500
                        `}
                      />
                      {errors.email && (
                        <p className="mt-1 text-sm text-red-500">
                          {errors.email}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Phone */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Phone Number
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <Phone size={16} className="text-gray-400" />
                      </div>
                      <input
                        type="tel"
                        name="phone"
                        value={userData.phone}
                        onChange={handleInputChange}
                        disabled={!editing}
                        className={`
                          w-full pl-10 pr-4 py-2 rounded-full border ${
                            errors.phone ? "border-red-500" : "border-gray-300"
                          } 
                          ${!editing ? "bg-gray-50" : "bg-white"} 
                          focus:outline-none focus:ring-2 focus:ring-teal-500
                        `}
                      />
                      {errors.phone && (
                        <p className="mt-1 text-sm text-red-500">
                          {errors.phone}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Birthday */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Date of Birth
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <Calendar size={16} className="text-gray-400" />
                      </div>
                      <input
                        type="date"
                        name="birthday"
                        value={userData.birthday}
                        onChange={handleInputChange}
                        disabled={!editing}
                        className={`
                          w-full pl-10 pr-4 py-2 rounded-full border ${
                            errors.birthday
                              ? "border-red-500"
                              : "border-gray-300"
                          } 
                          ${!editing ? "bg-gray-50" : "bg-white"} 
                          focus:outline-none focus:ring-2 focus:ring-teal-500
                        `}
                      />
                      {errors.birthday && (
                        <p className="mt-1 text-sm text-red-500">
                          {errors.birthday}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Address */}
                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Address
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <Home size={16} className="text-gray-400" />
                      </div>
                      <input
                        type="text"
                        name="address"
                        value={userData.address}
                        onChange={handleInputChange}
                        disabled={!editing}
                        className={`
                          w-full pl-10 pr-4 py-2 rounded-full border ${
                            errors.address
                              ? "border-red-500"
                              : "border-gray-300"
                          } 
                          ${!editing ? "bg-gray-50" : "bg-white"} 
                          focus:outline-none focus:ring-2 focus:ring-teal-500
                        `}
                      />
                      {errors.address && (
                        <p className="mt-1 text-sm text-red-500">
                          {errors.address}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Location */}
                  {editing && (
                    <div className="md:col-span-2">
                      <div className="flex items-center justify-between mb-2">
                        <label className="block text-sm font-medium text-gray-700">
                          Location on Map
                        </label>
                        <button
                          type="button"
                          onClick={() => setShowMap(!showMap)}
                          className="flex items-center gap-1 text-teal-500 text-sm hover:text-teal-600 transition-colors"
                        >
                          <MapPin size={16} />
                          <span>{showMap ? "Hide Map" : "Show Map"}</span>
                        </button>
                      </div>

                      {showMap && (
                        <div className="mt-2 rounded-lg overflow-hidden border border-gray-300">
                          <div className="h-10 bg-white flex items-center px-3">
                            <input
                              id="map-search-input"
                              type="text"
                              placeholder="Search for a location"
                              className="w-full border-none focus:outline-none text-sm"
                            />
                          </div>
                          <div ref={mapRef} className="h-64 w-full bg-gray-100">
                            {!mapLoaded && (
                              <div className="h-full flex items-center justify-center">
                                <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-teal-500"></div>
                              </div>
                            )}
                          </div>
                          <div className="bg-gray-50 p-3 text-xs text-gray-500">
                            Drag the marker to set your exact location
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Action buttons */}
            {editing && (
              <div className="mt-8 flex justify-end">
                <button
                  type="button"
                  onClick={() => {
                    setEditing(false);
                    setTempImage(null);
                    setErrors({});
                    // Don't reset to initialUserData, as that would lose the data from Firebase
                  }}
                  className="mr-3 px-6 py-2 rounded-full border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={saving}
                  className="px-6 py-2 rounded-full bg-teal-500 text-white hover:bg-teal-600 transition-colors flex items-center gap-2"
                >
                  {saving ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white"></div>
                      <span>Saving...</span>
                    </>
                  ) : (
                    <>
                      <Save size={16} />
                      <span>Save Changes</span>
                    </>
                  )}
                </button>
              </div>
            )}

            {/* Success message */}
            {saveSuccess && (
              <div className="mt-4 bg-green-50 text-green-700 p-3 rounded-lg flex items-center gap-2">
                <CheckCircle size={20} />
                <span>Profile updated successfully!</span>
              </div>
            )}

            {/* Submit error */}
            {errors.submit && (
              <div className="mt-4 bg-red-50 text-red-700 p-3 rounded-lg flex items-center gap-2">
                <X size={20} />
                <span>{errors.submit}</span>
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  );
};

export default ClientProfileEditor;
