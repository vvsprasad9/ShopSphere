# ===================================================================
# Stage 1: Build the React SPA application using Node.js 20 LTS
# ===================================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Cache dependencies using package lockfile
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build production assets
COPY . .
RUN npm run build

# ===================================================================
# Stage 2: Minimal Production Runtime Image (Nginx Alpine Non-Root)
# ===================================================================
FROM nginx:1.27-alpine AS runner

# Remove default static files and sample configs
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/*

# Copy production build output from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy security headers snippet and production Nginx configuration
COPY security-headers.conf /etc/nginx/security-headers.conf
COPY nginx.conf /etc/nginx/nginx.conf

# Configure unprivileged permissions for non-root user (nginx:nginx UID/GID 101)
RUN touch /tmp/nginx.pid && \
    chown -R nginx:nginx /tmp/nginx.pid /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx

# Switch to unprivileged non-root user
USER nginx:nginx

# Expose unprivileged application port inside container
EXPOSE 8080

# Health check verifying Nginx is actively serving the SPA root
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/ || exit 1

# Start Nginx in foreground mode
CMD ["nginx", "-g", "daemon off;"]
