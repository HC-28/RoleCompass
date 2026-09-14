# ==============================================================================
# Frontend Dockerfile — React/Vite → Nginx
# Two-stage build: Node build → Nginx static serve
# ==============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --silent

COPY . .

# Inject the backend URL so Axios points to the correct host at build time.
# When running via docker compose, the browser reaches the backend at
# http://localhost:8080 (the host-mapped port), not the container hostname.
ENV VITE_API_BASE_URL=http://localhost:8080

RUN npm run build

# ── Stage 2: Serve with Nginx ─────────────────────────────────────────────────
FROM nginx:1.27-alpine

# Remove the default Nginx page
RUN rm -rf /usr/share/nginx/html/*

# Copy the Vite production build
COPY --from=build /app/dist /usr/share/nginx/html

# Nginx config: serve index.html for all routes (SPA fallback)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["/bin/sh", "-c", "echo '---------------------------------------------------------' && echo '  RoleCompass Frontend running at: http://localhost:5174' && echo '---------------------------------------------------------' && nginx -g 'daemon off;'"]
