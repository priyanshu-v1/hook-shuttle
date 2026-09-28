<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/priyanshu-v1/hook-shuttle/dev/assets/logo-dark.png">
    <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/priyanshu-v1/hook-shuttle/dev/assets/logo-light.png">
    <img src="https://raw.githubusercontent.com/priyanshu-v1/hook-shuttle/dev/assets/logo-light.png" alt="HookShuttle Logo" width="200">
  </picture>
</p>

<h1 align="center" style="margin-top: -10px;">HookShuttle</h1>

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=java&logoColor=white)](https://www.java.com/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)

A high-performance, secure, and observable **Enterprise Webhook Gateway and Management Console**. Built from scratch to handle millions of webhooks with reliable delivery, retries, and real-time observability.

---

## 🚀 Why HookShuttle?

Third-party webhooks are notoriously difficult to manage. HookShuttle solves the operational nightmare by providing a centralized, robust pipeline.

* **Ingestion at Scale**: Securely ingest raw webhooks via API Keys (User X) and return a 202 Accepted in <15ms.
* **Guaranteed Delivery**: Asynchronous processing with RabbitMQ ensures the ingress never slows down.
* **Smart Retries**:
  * **Short-Term**: Exponential backoff using Redisson's high-performance delay queues ($2^n$).
  * **Long-Term**: PostgreSQL cold-retry sweeper for extended backoff periods.
* **Enterprise Security**: End-to-end AES-256 payload encryption, HMAC signature verification, and JWT refresh token rotation.
* **Real-Time Observability**: A clean React + Shadcn UI dashboard to track every event, inspect payloads, and manually replay failed webhooks (DLQ).

---

## 🏗️ Architecture Overview

HookShuttle is a monorepo split into two core components:

1. **Backend (Spring Boot)**: The engine that handles ingestion, security, message queuing (RabbitMQ), caching (Redis/Redisson), and asynchronous delivery workers.
2. **Frontend (React)**: The management console. A Vite SPA built with TanStack Router and TanStack Query, providing a live view of the gateway's performance and logs.

---

## 🛠️ Local Development

This project requires a full infrastructure stack running via Docker.

### Prerequisites

* Java 25+
* Node.js 20+ & npm
* Docker & Docker Compose

### 1. Start Infrastructure (Docker)

Spin up PostgreSQL, RabbitMQ (with delayed-message plugin), and Redis.

```bash
# From the project root
docker compose up -d
```

### 2. Configure Environment Files

Navigate to your backend resources folder and ensure your local development configuration file is set up:

```bash
cd backend/src/main/resources
# Ensure application-dev.properties is populated with your local DB, Redis, and RabbitMQ credentials.
```

### 3. Run the Backend

From the root project directory:

```bash
./mvnw spring-boot:run -Dspring.profiles.active=dev
```
(The backend API starts on `http://localhost:8080`)

### 4. Run the Frontend Dashboard

Open a separate terminal window, navigate to the frontend directory, install dependencies, and start the development server:

```bash
cd frontend
npm install
npm run dev
```
(The frontend dashboard starts on `http://localhost:5000`)

> **Note on CORS:** If you run the frontend on a custom port (e.g., `http://localhost:5173`), ensure your `application-dev.properties` (or environment variables) has CORS enabled and your origin allowed:
> ```properties
> hook-shuttle.cors.enabled=true
> hook-shuttle.cors.allowed-origins=${ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}
> ```

---

## 🚢 Production Deployment Guide

To run HookShuttle in a production environment, you must pass the production Spring profile and provide all necessary environment variables.

### 1. Required Production Environment Variables

Set the following environment variables in your production host or deployment pipeline (e.g., Docker, Kubernetes, or a Linux service file):

| Environment Variable | Description | Example / Required Format |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://your-db-host:5432/webhook_gateway` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `SecureDBPass123!` |
| `SPRING_RABBITMQ_HOST` | RabbitMQ host address | `your-rabbitmq-host` |
| `SPRING_RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `SPRING_RABBITMQ_USERNAME` | RabbitMQ username | `admin` |
| `SPRING_RABBITMQ_PASSWORD` | RabbitMQ password | `RabbitPass123!` |
| `REDIS_HOST` | Redis cache host address | `your-redis-host` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | Strong secret key for signing JWTs | `64-character hex string` |
| `ENCRYPTION_KEY` | 32-byte Base64-encoded AES key | `6qlZF66XR5hJ1g1qkGWCf8noobfc/LTHpW5mtUNUez0=` |
| `ALLOWED_ORIGINS` | Comma-separated CORS allowed origins | `https://yourdomain.com` |
| `SERVE_FRONTEND` | Whether Spring Boot serves the built frontend SPA | `true` or `false` (default: `true`) |


### 2. Running in Production Mode

Start the Spring Boot application using the production profile flag:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://..."
export SPRING_DATASOURCE_USERNAME="..."
export SPRING_DATASOURCE_PASSWORD="..."
export SPRING_RABBITMQ_HOST="..."
export SPRING_RABBITMQ_USERNAME="..."
export SPRING_RABBITMQ_PASSWORD="..."
export REDIS_HOST="..."
export JWT_SECRET="..."
export ENCRYPTION_KEY="..."
export ALLOWED_ORIGINS="[https://yourdomain.com](https://yourdomain.com)"

# Start application with the production profile
./mvnw spring-boot:run -Dspring.profiles.active=prod
```
Alternatively, if you are running a pre-built fat JAR:

```bash
java -Dspring.profiles.active=prod -jar target/webhook-gateway-0.0.1-SNAPSHOT.jar
```
> **Note:** You can also pass configuration properties directly as Java system properties via `-D` flags if you prefer not to export shell environment variables. 
> 
> *Example:*
> ```bash
> java -Dspring.profiles.active=prod -Dspring.datasource.url="jdbc:postgresql://..." -jar target/webhook-gateway-0.0.1-SNAPSHOT.jar
> ```

---

## 📜 License

This project is licensed under the MIT License - see the `LICENSE` file for details.