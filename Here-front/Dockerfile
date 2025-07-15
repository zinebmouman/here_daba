# Use an official Node.js image with Alpine (lightweight)
FROM node:18-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy package.json and package-lock.json first (for caching dependencies)
COPY package.json package-lock.json* ./

# Clear npm cache and install dependencies in one step (optimized)
RUN npm cache clean --force && \
    npm install && \
    npm install react-icons --save

# Create upload directory for images
RUN mkdir -p /app/public/uploads

# Copy all project files into the container
COPY . .

# Expose Vite's default port (5173)
EXPOSE 5173

# Start the Vite development server
CMD ["npm", "run", "dev", "--", "--host"]