# ShopSphere — Production Deployment & Operations Guide

This guide describes how containerized ShopSphere releases published to the **GitHub Container Registry (GHCR)** are securely pulled, deployed, and maintained on production infrastructure.

---

## 1. Release Architecture & Container Registry

```mermaid
flowchart LR
    Dev[Developer] -->|git push / PR| GH[GitHub Actions CI/CD]
    GH -->|Unit & Integration Tests| Tests{Tests Pass?}
    Tests -->|Yes| Build[Docker Multi-Stage Build]
    Build -->|Publish| GHCR[(GitHub Container Registry: ghcr.io)]
    GHCR -->|Pull Image Tag: sha-XXXXX| ProdHost[Production Linux Host]
    ProdHost -->|docker compose up -d| Stack[Running Stack: Nginx + Spring Boot + MySQL]
```

### Published Container Images
- **Backend**: `ghcr.io/<owner>/shopsphere-backend:<tag>`
- **Frontend**: `ghcr.io/<owner>/shopsphere-frontend:<tag>`

### Image Tagging Strategy
1. **Immutable Commit SHA**: `sha-<short-sha>` (e.g. `sha-7f3b8a1`) — **Primary tag for production deployments**. Guarantees 100% reproducible deployments and safe rollbacks.
2. **Release SemVer**: `v1.0.0` — Assigned on Git release tags.
3. **Branch Reference**: `main` — Reflects latest merge on the default branch.
4. **Floating Latest**: `latest` — Updated only upon merging to `main`.

---

## 2. Server Authentication with GHCR

Production servers authenticate against GHCR using a GitHub **Personal Access Token (Classic)** or **Fine-Grained Personal Access Token** with `read:packages` permission:

```bash
# Set secure environment variables on production host
export GHCR_USER="your-github-username-or-service-account"
export GHCR_PAT="ghp_YourSecureReadOnlyTokenWithReadPackages"

# Authenticate Docker daemon with ghcr.io
echo "$GHCR_PAT" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
```

---

## 3. Production Environment & Configuration

All environment secrets must be stored in a root `.env` file on the production host (never checked into version control).

```bash
# 1. Copy template
cp .env.example .env

# 2. Restrict filesystem permissions (read/write only by owner)
chmod 600 .env

# 3. Specify the exact immutable deployment tag
sed -i 's/IMAGE_TAG=latest/IMAGE_TAG=sha-7f3b8a1/' .env
```

---

## 4. Deploying Pre-Built Images with Docker Compose

Deployments utilize `docker-compose.prod.yml`, which references pre-built images rather than local build contexts:

```bash
# 1. Pull the specified container images from GHCR
docker compose -f docker-compose.prod.yml pull

# 2. Launch the stack in detached mode
docker compose -f docker-compose.prod.yml up -d

# 3. Verify health status of all running containers
docker compose -f docker-compose.prod.yml ps

# 4. Tail startup logs
docker compose -f docker-compose.prod.yml logs -f --tail=100
```

---

## 5. Protecting Database Persistence & Data Safety

### Core Principles
1. **Named Volumes**: MySQL data resides in named volume `shopsphere_mysql_data`. Uploads reside in `shopsphere_uploads_data`.
2. **Never Execute `down -v`**: Running `docker compose down -v` permanently destroys persistent volumes. In production, only use `docker compose stop` or `docker compose down` (without the `-v` flag).
3. **Automated Database Backups**: Prior to any upgrade or deployment, capture a transactional database snapshot:

```bash
# Capture timestamped database snapshot
BACKUP_FILE="/var/backups/shopsphere/db_backup_$(date +%Y%m%d_%H%M%S).sql.gz"
mkdir -p /var/backups/shopsphere

docker exec shopsphere-mysql mysqldump \
  -u root -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  "${DB_NAME}" | gzip > "${BACKUP_FILE}"

chmod 600 "${BACKUP_FILE}"
```

---

## 6. Rolling Updates & Zero-Downtime Deployment Procedure

To update the running application without tearing down the database:

```bash
# Step 1: Update IMAGE_TAG in .env to the target commit SHA
sed -i 's/IMAGE_TAG=.*/IMAGE_TAG=sha-newcommit/' .env

# Step 2: Pull new container images
docker compose -f docker-compose.prod.yml pull

# Step 3: Recreate backend container (MySQL remains untouched)
docker compose -f docker-compose.prod.yml up -d --no-deps backend

# Step 4: Wait for backend healthcheck to reach 'healthy'
until [ "$(docker inspect --format='{{.State.Health.Status}}' shopsphere-backend)" = "healthy" ]; do
  echo "Waiting for backend health check..."
  sleep 5
done

# Step 5: Recreate frontend container
docker compose -f docker-compose.prod.yml up -d --no-deps frontend
```

---

## 7. Fast Rollback Procedure

If issues are detected following a deployment:

```bash
# Step 1: Revert IMAGE_TAG in .env to the prior known-good SHA
sed -i 's/IMAGE_TAG=.*/IMAGE_TAG=sha-previousgood/' .env

# Step 2: Pull previous images (if not cached locally)
docker compose -f docker-compose.prod.yml pull

# Step 3: Restart services with previous images
docker compose -f docker-compose.prod.yml up -d --no-deps backend frontend

# Step 4: Validate health
docker compose -f docker-compose.prod.yml ps
```
