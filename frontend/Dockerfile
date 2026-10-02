# Name the node stage "builder"
FROM node:16 AS builder

# # Set working directory
WORKDIR /app

# Copy all files from current directory to working dir in image
COPY . .

# install node modules and build assets
RUN npm install && npm run build

# nginx state for serving content
FROM nginx:alpine


# Set working directory to nginx asset directory
RUN mkdir -p /usr/share/nginx/html/cortana 
WORKDIR /usr/share/nginx/html/cortana

# Remove default nginx static assets
RUN rm -rf ./*

# Copy static assets from builder stage
COPY --from=builder /app/dist .

WORKDIR /etc/nginx/
RUN mkdir templates
WORKDIR /etc/nginx/templates
COPY templates/* .

WORKDIR /etc/nginx
