# ==============================================================================
# Frontend Dockerfile — React/Vite → Nginx
# Two-stage build: Node build → Nginx static serve
#
# API calls: The browser uses RELATIVE paths (/api/...).
# Nginx proxies /api → backend container (rolecompass_backend:8080).
# This avoids baking a hardcoded host URL into the JS bundle at build time,
# and eliminates cross-origin issues between the Nginx container and the browser.
# ==============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Cache npm install layer separately from source
COPY package.json package-lock.json ./
RUN npm ci --silent

COPY . .

# Build with relative API base so Nginx proxy handles routing.
# VITE_API_BASE_URL is intentionally empty — all /api/* calls are relative
# and Nginx proxies them to the backend container.
RUN npm run build

# ── Stage 2: Serve with Nginx ─────────────────────────────────────────────────
FROM nginx:1.27-alpine

# Run Nginx worker processes as a non-root user (nginx user already exists in the image)
# The master process must stay root to bind port 80; workers drop privileges via nginx.conf.

# Remove the default Nginx page
RUN rm -rf /usr/share/nginx/html/*

# Copy the Vite production build
COPY --from=build /app/dist /usr/share/nginx/html

# Nginx config: SPA fallback + /api proxy to backend
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
