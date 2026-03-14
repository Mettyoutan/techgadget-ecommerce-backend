# TechGadget E-Commerce Backend

A backend-focused portfolio project built to simulate a production-like e-commerce system for tech gadgets — smartphones, laptops, and accessories. The primary goal of this project is to deepen my understanding of backend architecture, system design decisions, and the engineering tradeoffs that come with building real-world applications.

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-green?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-latest-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-latest-red?style=flat-square&logo=redis)
![MinIO](https://img.shields.io/badge/MinIO-8.6.0-purple?style=flat-square&logo=minio)

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [Technical Decisions](#technical-decisions)
- [API Documentation](#api-documentation)
- [Getting Started](#getting-started)
- [Learning Notes](#learning-notes)
- [Roadmap](#roadmap)

---

## Tech Stack

| Technology | Purpose | Why I Chose It |
|---|---|---|
| **Java 25 + Spring Boot 4** | Core backend framework | Industry-standard for enterprise backend; forces me to understand proper layered architecture |
| **Spring Security + JWT** | Authentication & authorization | Hands-on experience with stateless auth and filter chain internals |
| **PostgreSQL** | Primary database | Relational model fits e-commerce domain well; strong consistency for order and payment data |
| **Flyway** | Database migration | Version-controlled schema changes — a practice I want to carry into professional work |
| **Redis** | Rate limiting & caching | High-performance in-memory store; perfect for sliding window counters |
| **MinIO** | Object storage (images) | S3-compatible API means the same code works with AWS S3 in production — a deliberate future-proof choice |
| **Thumbnailator** | Image processing | Automatic thumbnail generation to reduce bandwidth on product listings |
| **SpringDoc OpenAPI** | API documentation | Auto-generates interactive Swagger UI from code annotations |


---

## Features

### Customer
- Register and login with JWT-based authentication
- Browse and search products with pagination
- Manage cart — add, update, remove, and clear items
- Checkout selected cart items into an order
- View order history and order details
- Cancel a pending order (with automatic stock restoration)
- Pay for an order (dummy payment simulation)
- Write product reviews after purchasing

### Admin
- Create, update, and delete products
- Upload and manage product images with automatic thumbnail generation
- Manage and update order statuses

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   HTTP Request                      │
└──────────────────────┬──────────────────────────────┘
                       │
              ┌────────▼────────┐
              │ RateLimitFilter │  ← Redis sliding window
              └────────┬────────┘
                       │
              ┌────────▼────────────┐
              │ JwtAuthentication   │  ← Validate Bearer token
              │ Filter              │
              └────────┬────────────┘
                       │
              ┌────────▼────────┐
              │   Controller    │  ← Route & validate request
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │    Service      │  ← Business logic
              └────────┬────────┘
                       │
           ┌───────────┼───────────┐
           │           │           │
    ┌──────▼──┐  ┌─────▼───┐  ┌───▼────┐
    │  JPA /  │  │  Redis  │  │  MinIO │
    │Postgres │  │         │  │        │
    └─────────┘  └─────────┘  └────────┘
```

### Filter Chain Order

Requests pass through two filters before reaching any controller:

1. **`RateLimitFilter`** — runs first, checks Redis before any authentication. Rejects excessive requests with `429 Too Many Requests`.
2. **`JwtAuthenticationFilter`** — validates the Bearer token and populates `SecurityContextHolder`.

This order is intentional: rate limiting should protect the server regardless of whether a valid token exists.

---

## Technical Decisions

### 1. Order Item Snapshot Pattern

**Problem:** An order must permanently reflect what the customer actually bought — the price, product name, and image at the time of purchase. If product data is referenced by a foreign key alone, any future price update or product deletion would corrupt historical order records.

**Decision:** When an order is created, `OrderItem` captures a snapshot of the product at that exact moment:

```java
// Fields stored directly on OrderItem — not a FK reference
private String productNameSnapshot;
private BigDecimal priceAtOrder;
private String productImageKeySnapshot;
```

**Why this matters:** This became clear when I realized that two critical validations had to happen at checkout time — stock availability and price consistency. The system checks both before creating the order, then immediately freezes the current price into the snapshot. This means order history stays accurate even if the product is later edited, repriced, or deleted.

> *This was my first time designing a proper order system, and the snapshot pattern was a genuine "aha" moment — I expected order creation to be simple, but the number of validations required made me understand why order systems are among the more complex domains in e-commerce.*

---

### 2. Object Storage with MinIO — Storing Keys, Not URLs

**Problem:** Product images need to be stored somewhere durable and retrievable, while keeping the database clean and the system decoupled from a specific storage provider.

**Decision:** Images are stored in MinIO (an S3-compatible object store). The database never stores image URLs — it stores only the `imageKey`:

```
products/iphone-15/image-1.jpg   ← what's stored in the DB
```

The actual image is served through: `GET /api/images/{imageKey}`

**Why this matters:** Storing URLs directly creates tight coupling — if the storage domain, bucket, or CDN changes, every URL in the database breaks. Storing only the key keeps the database clean and makes the system portable. The same code works with MinIO locally and AWS S3 in production by changing one configuration value.

**Thumbnail optimization:** For every uploaded image, the system automatically generates a compressed thumbnail using Thumbnailator. Product listing endpoints return thumbnails; full images are only loaded on the detail page. This decision came from thinking through a concrete scenario: loading 100 product cards with 10MB images each would be catastrophic for performance.

---

### 3. Rotated Refresh Token with PostgreSQL Persistence

**Problem:** JWTs are stateless and short-lived by design. But forcing users to re-login every 15 minutes is a poor experience. The challenge is enabling long-lived sessions without permanently exposing a token that, if stolen, grants indefinite access.

**Decision:** Implement a rotated refresh token strategy:

- **Access token:** Short-lived (15 minutes), stateless JWT
- **Refresh token:** Long-lived (7 days), stored as an `HttpOnly` cookie and persisted in PostgreSQL

On every refresh request, the old token is invalidated and a new one is issued. If a refresh token is stolen and used, the legitimate user's next refresh will detect the mismatch — the stolen token is already consumed and the session is invalidated.

**Why PostgreSQL over Redis for refresh tokens:** Refresh tokens represent a security boundary and need durable, consistent storage. Redis is appropriate for ephemeral data like rate limit counters, but losing a refresh token record on cache eviction or restart would silently extend access. PostgreSQL gives strong consistency and full auditability.

> *This was my first time implementing token rotation. Understanding why the rotation matters — specifically the "stolen token detection" property — made the extra complexity feel justified rather than arbitrary.*

---

### 4. Sliding Window Rate Limiting with Redis ZSET

**Problem:** The API needs protection against brute force attacks on auth endpoints and abusive request patterns on other endpoints.

**Decision:** Implement a sliding window rate limiter using Redis sorted sets (ZSET), with three tiers:

| Tier | Endpoints | Limit |
|---|---|---|
| `AUTH` | `/auth/login`, `/auth/register` | 10 req / 60s per IP |
| `WRITE` | `/orders`, `/cart`, `/reviews` | 30 req / 60s per user |
| `READ` | Product browsing, search | 100 req / 60s per user |

Each request is stored as a member in a ZSET with a millisecond timestamp as its score. Expired entries are pruned on every check using `ZREMRANGEBYSCORE`.

**Why sliding window over fixed window:** Fixed window has a known boundary spike problem — a user can send the full limit in the last second of one window, then the full limit again in the first second of the next, effectively doubling the rate. Sliding window always evaluates the true last N seconds, eliminating this exploit without the complexity of token bucket.

**Why Lua script for atomicity:**

```lua
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)  -- prune expired
local count = redis.call('ZCOUNT', key, window_start, now)  -- count first
if count < max_requests then
    redis.call('ZADD', key, now, member)  -- only add if allowed
    redis.call('EXPIRE', key, ttl)
end
return count
```

All four operations execute atomically in a single Redis roundtrip. A non-atomic approach would introduce a race condition between the count check and the add.

---

## API Documentation

Interactive Swagger UI is available at: `http://localhost:8080/api/swagger-ui/index.html`

### Endpoint Summary

#### Authentication — Public
| Method   | Endpoint         | Description                                   |
|----------|------------------|-----------------------------------------------|
| `POST`   | `/auth/register` | Register a new customer account               |
| `POST`   | `/auth/login`    | Login and receive access + refresh tokens     |
| `POST`   | `/auth/refresh`  | Rotate refresh token and get new access token |
| `POST`   | `/auth/logout`   | Invalidate refresh token                      |

#### Products — Public (read) / Admin (write)
| Method   | Endpoint                         | Auth     | Description                              |
|----------|----------------------------------|----------|------------------------------------------|
| `GET`    | `/products/search?..`            | Public   | List products with pagination and search |
| `GET`    | `/products/{productId}`          | Public   | Get product detail                       |
| `POST`   | `/products`                      | Admin    | Create product                           |
| `PUT`    | `/products/{productId}`          | Admin    | Update product                           |
| `DELETE` | `/products/{productId}`          | Admin    | Delete product                           |
| `POST`   | `/products/{productId}/images`   | Admin    | Upload product image                     |

#### Cart — Customer
| Method   | Endpoint             | Description               |
|----------|----------------------|---------------------------|
| `GET`    | `/cart`              | Get current user's cart   |
| `POST`   | `/cart`              | Add item to cart          |
| `PUT`    | `/cart/{cartItemId}` | Update cart item quantity |
| `DELETE` | `/cart/{cartItemId}` | Remove cart item          |
| `DELETE` | `/cart`              | Clear all cart items      |
| `GET`    | `/cart/count`        | Get total item count      |

#### Orders — Customer
| Method  | Endpoint                   | Description                                 |
|---------|----------------------------|---------------------------------------------|
| `POST`  | `/orders`                  | Checkout selected cart items into an order  |
| `GET`   | `/orders`                  | Get current user order history with filters |
| `GET`   | `/orders/{orderId}`        | Get current user order detail               |
| `POST`  | `/orders/{orderId}/cancel` | Cancel a pending order                      |        
| `POST`  | `/orders/{orderId}/pay`    | Process payment (dummy)                     |

#### Admin Orders
| Method  | Endpoint                           | Description                     |
|---------|------------------------------------|---------------------------------|
| `GET`   | `/admin/orders`                    | View all orders with filters    |
| `GET`   | `/admin/orders/{orderId}`          | Get order by id                 |
| `GET`   | `/admin/orders/users/{userId}`     | Get all orders from a user      |
| `PATCH` | `/admin/orders/{orderId}/ship`     | Update order status to shipped  |
| `PATCH` | `/admin/orders/{orderId}/complete` | Update order status to complete |
| `PATCH` | `/admin/orders/{orderId}/cancel`   | Update order status to cancel   |

#### Product reviews - Public (read) / Customer (write) 
| Method | Endpoint                        | Description                |
|--------|---------------------------------|----------------------------|
| `POST` | `/products/{productId}/reviews` | Create product review      |
| `GET`  | `/products/{productId}/reviews` | Get reviews from a product |

#### User Profile - Customer
| Method | Endpoint           | Description                |
|--------|--------------------|----------------------------|
| `GET`  | `/users`           | Get current user profile   |
| `POST` | `/users/addresses` | Add address to user profile |

---

## Getting Started

### Prerequisites

- Java 25
- Docker & Docker Compose (for PostgreSQL, Redis, MinIO)
- Maven 3.9+

### 1. Clone the repository

```bash
git clone https://github.com/your-username/techgadget-ecommerce.git
cd techgadget-ecommerce
```

### 2. Start infrastructure services

```bash
docker-compose up -d
```

This starts PostgreSQL, Redis, and MinIO locally.

### 3. Configure environment variables

Create `src/main/resources/application-local.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/techgadget
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

# JWT
app.jwt.secret=your-256-bit-secret-key-here
app.jwt.access-expiration=900000
app.jwt.refresh-expiration=604800000

# MinIO
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket=techgadget

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 4. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway will automatically run all migrations on startup.

### 5. Explore the API

Open Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`

---

## Learning Notes

I want to be honest about what I actually learned building this — not just list the technologies I used, but document the moments where something genuinely clicked.


**Order system is harder than it looks.**

I've touched e-commerce projects before, but I never really built the order flow properly. I thought it would be simple — take the cart items, create an order, done. But the moment I started implementing it, I ran into a question I didn't expect: *what happens if the product price changes after someone orders?*

That's when I learned about the snapshot pattern. Instead of referencing the product by ID and fetching its current data, I copy the name, price, and image key directly into the `OrderItem` at the time of order creation. It felt a bit redundant at first — storing the same data twice. But then it made sense. Orders are historical records. They should never change because the product changed.

There were also more validations than I anticipated — stock availability, whether the cart items actually belong to the user, whether the items are still in stock at checkout time. Each one uncovered a scenario I hadn't thought about. I think this is where backend development gets genuinely interesting, and genuinely humbling.


**I didn't really understand image storage until this project.**

Before this, my mental model was: save the image, get a URL, store the URL. Simple.

But when I started thinking about it more — what if the storage domain changes? What if I move from MinIO to S3? Every stored URL in the database breaks. So instead, I store only the `imageKey`, something like `products/iphone-15/image-1.jpg`, and serve it through an endpoint that resolves the actual file. This way, the database doesn't care where the file actually lives.

I also added thumbnail generation using Thumbnailator. The reasoning was simple: if a product listing page loads 100 full-size images at 10MB each, that's 1GB of data on a single page load. Thumbnails are generated once at upload time and stored separately. It's a small decision that makes a real difference.

MinIO was new to me. What convinced me to use it is that it's S3-compatible — the same SDK and the same code works with AWS S3. So if this ever needs to go to production, I just change a config value.


**Token rotation — I understood the code before I understood the reason.**

I implemented rotated refresh tokens, but at first I was just following the pattern without fully grasping why rotation matters. The access token is short-lived, fine. The refresh token lives in an `HttpOnly` cookie, fine. But why invalidate the old refresh token on every use?

Then it clicked: if someone steals a refresh token and uses it, the real user's next refresh will fail — because the stolen token was already consumed and replaced. The server detects the mismatch. This doesn't prevent theft, but it limits the damage window.

I also made a deliberate choice to store refresh tokens in PostgreSQL instead of Redis. Refresh tokens are a security boundary. If Redis evicts a key or restarts, losing that record silently extends someone's session. PostgreSQL gives me durability and I can audit which tokens exist, when they were created, and revoke them explicitly.


**The rate limiter bug I introduced myself.**

This one was embarrassing but educational.

My first implementation used Redis `MULTI/EXEC` to group the operations — remove expired entries, add the new request, count the total. I thought transactions meant atomic. But I was counting *after* adding the new request, which meant the limit check was always off by one. A user with a limit of 10 could actually make 11 requests before getting blocked.

Then there was a second issue: when I rewrote it using a Lua script, I initially placed the `ZADD` before the `ZCOUNT`. Same problem, different code. The correct order is: prune expired entries → count what's there → only then add if allowed.

The Lua script approach also fixed something I didn't fully appreciate at first. `MULTI/EXEC` in Redis is a transaction, but the operations still go back and forth between the application and Redis. Lua runs entirely on the Redis server in one shot. For a rate limiter that runs on every single request, that difference matters.

Getting this right took a few iterations. But I think I understand sliding window rate limiting properly now — not just how to implement it, but why each step has to happen in that specific order.

---

## Roadmap

- [ ] Integrate Stripe for real payment processing
- [ ] Implement OAuth2 / Google login for better user experience
- [ ] Add product caching layer with Redis
- [ ] Write comprehensive integration tests
- [ ] Containerize with Docker and write deployment configuration