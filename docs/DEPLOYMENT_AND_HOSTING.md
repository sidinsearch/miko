# 🐳 Deployment & Cloud Hosting Guide

This guide covers deploying the Vert.x API Gateway locally, via Docker, and hosting it on modern cloud platforms.

---

## 1. Local Deployment (Standalone Fat JAR)

For production environments without container runtimes, you can package the application into a self-contained executable JAR (shaded with all dependencies and Netty native transport libraries).

### Build the Fat JAR:
```bash
mvn clean package
```
This generates `target/vertx-api-gateway-fat.jar`.

### Run the Service:
```bash
java -jar target/vertx-api-gateway-fat.jar
```

### Override Configuration at Runtime:
You can pass custom environment variables directly:
```bash
SERVER_PORT=9000 REQUEST_TIMEOUT_MS=3000 java -jar target/vertx-api-gateway-fat.jar
```

---

## 2. Docker & Docker Compose Deployment

The repository includes a production-grade, multi-stage `Dockerfile` that compiles the code in a Maven builder container and packages the minimal runtime artifact into an Alpine Linux JRE image.

### Multi-Stage Dockerfile Overview:
- **Stage 1 (Builder)**: Uses `maven:3.9.8-eclipse-temurin-17` to compile and shade the JAR.
- **Stage 2 (Runtime)**: Uses `eclipse-temurin:17-jre-alpine` for an ultra-lightweight, secure production footprint (~150 MB total image size).

### Deploy via Docker Compose (Recommended):
```bash
# Build and start in background
docker compose up --build -d

# Check live logs
docker compose logs -f

# Check container status
docker compose ps

# Stop and remove container
docker compose down
```

---

## 3. Free Cloud Hosting in Under 5 Minutes

You can easily deploy this repository to modern cloud providers directly from your GitHub repository.

### Option A: Railway (railway.app)
1. Log in to [Railway](https://railway.app/) with your GitHub account.
2. Click **New Project** → **Deploy from GitHub repo**.
3. Select this repository (`https://github.com/sidinsearch/miko` or `miko`).
4. Railway will automatically detect the `Dockerfile` and initiate the build.
5. In **Settings** → **Networking**, click **Generate Domain** (e.g., `https://miko.up.railway.app`).
6. Your live `/aggregate` endpoint is ready!

---

### Option B: Render (render.com) - 🚀 Live Demo Available!
1. Log in to [Render](https://render.com/).
2. Click **New** → **Web Service** → Connect your GitHub repository (`https://github.com/sidinsearch/miko`).
3. Choose **Runtime: Docker**.
4. Set **Port** to `8080` (or add an environment variable `SERVER_PORT=10000` if Render assigns port 10000).
5. Click **Create Web Service**.

> **🚀 Live Deployed Instance:** This project is currently deployed and running live on Render!
> - **Service Discovery Index:** [https://miko-3g9m.onrender.com/](https://miko-3g9m.onrender.com/)
> - **Core Aggregation Endpoint:** [https://miko-3g9m.onrender.com/aggregate](https://miko-3g9m.onrender.com/aggregate)

---

### Option C: Fly.io (fly.io)
Ensure you have the `flyctl` CLI installed:
```bash
# Authenticate
fly auth login

# Launch app (detects Dockerfile automatically)
fly launch

# Deploy
fly deploy

# Open live URL
fly open /aggregate
```

---

### Option D: Koyeb (koyeb.com)
1. Log in to [Koyeb](https://www.koyeb.com/).
2. Click **Create Service** → **GitHub**.
3. Select this repository (`https://github.com/sidinsearch/miko`).
4. Set **Builder** to **Dockerfile**.
5. Set **Port** to `8080`.
6. Click **Deploy**.

---

## 4. Production Environment Variables

When hosting in production or Kubernetes clusters, configure the following environment variables to tune performance and resilience:

| Variable Name | Default | Production Recommendation | Purpose |
| :--- | :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | `80` or `443` (or platform default) | Listening HTTP port |
| `REQUEST_TIMEOUT_MS` | `5000` | `2500` | Upstream HTTP read timeout |
| `POST_URL` | `https://.../posts/1` | Production internal service URL | Upstream Posts endpoint |
| `USER_URL` | `https://.../users/1` | Production internal service URL | Upstream Users endpoint |
| `CB_MAX_FAILURES` | `3` | `5` | Failures before tripping circuit |
| `CB_TIMEOUT_MS` | `5000` | `3000` | Circuit breaker execution timeout |
| `CB_RESET_TIMEOUT_MS`| `10000` | `30000` | Wait time before recovery probe |
