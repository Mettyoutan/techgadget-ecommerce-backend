# TechGadget E‑Commerce Backend

A production‑style e‑commerce backend built with **Java & Spring Boot 4**, featuring JWT authentication, product catalog, cart, orders, reviews, and admin tools.  
Designed as a learning and portfolio project, but structured with real‑world patterns (DTOs, validations, global error handling, and OpenAPI docs).[web:63][web:71]

---

## Features

### Authentication & Users
- User registration and login with **JWT**.
- Role‑based access control: `USER` and `ADMIN`.
- Secured endpoints for carts, orders, reviews, and admin operations.[web:69][web:72]

### Product Catalog
- Public endpoints to list and view product details.
- Admin CRUD for products: create, update, delete, manage stock and pricing.
- Products include stock, price, category, description, and other metadata.[web:63]

### Cart & Checkout
- Each authenticated user has a personal cart.
- Add/update/remove items, view current cart.
- Checkout converts selected cart items into an order.[web:68][web:71]

### Orders & Status Flow
- Order lifecycle with controlled status transitions, for example:
  - `PENDING → PAID → SHIPPED → COMPLETED`
  - Cancellation rules enforced in the service layer.
- Admin endpoints to:
  - Search all orders with filters + pagination.
  - Retrieve orders by user.
  - View order details.
  - Update order status (ship, complete, cancel).[web:71]

### Payment (Dummy)
- Simulated payment flow (no real payment gateway yet).
- Endpoints to mark orders as paid, useful for future gateway integration.[web:69]

### Shipping & Address
- Simple shipping information stored per order.
- Basic shipping fee handling (can be extended with zones/couriers later).[web:68]

### Reviews
- Users can review only products they have actually ordered.
- Anti‑spam rules: one review per order/product combination and validation of review ownership.[web:63]

### Admin & Analytics
- Admin endpoints to:
  - Manage products.
  - View and manage orders.
- Simple analytics/summary endpoints (e.g., total orders, revenue, etc.) for dashboard use.[web:63][web:71]

---

## Technical Stack

- **Language**: Java  
- **Framework**: Spring Boot 4 (Spring Web, Spring Security, Spring Data JPA)  
- **Database**: MySQL  
- **ORM**: JPA / Hibernate  
- **Auth**: JWT (JSON Web Token) based auth & authorization  
- **Validation**: `jakarta.validation` (e.g. `@NotNull`, `@Positive`)  
- **Documentation**: springdoc‑openapi + Swagger UI  
- **Logging**: Business‑level logging for key events (order creation, status changes, etc.).[web:69][web:72]

---

## Architecture & Best Practices

- **Layered architecture**: Controller → Service → Repository → Entity.
- **DTOs** for request/response to keep entities clean and API stable.
- **Global error handling** via `@RestControllerAdvice`, returning a consistent `ErrorResponse` (code, message, timestamp, path) for 4xx/5xx errors.
- **Paginated responses** for list endpoints (products, orders).
- **Role‑based authorization** enforced at controller/service level.[web:69][web:71]

---

## API Documentation

Interactive API documentation is available via Swagger UI:

- OpenAPI spec auto‑generated using **springdoc‑openapi**.
- Each main controller is annotated with:
  - `@Operation` for summary/description.
  - `@ApiResponses` including schemas for `ErrorResponse`, `OrderResponse`, `PaginatedResponse`, etc.
- Great for frontend integration and manual testing.[web:69][web:71]

> Once the application is running, visit:  
> `http://localhost:8080/swagger-ui.html` or `/swagger-ui/index.html` (depending on configuration).[web:69]

---
## Getting Started

### Prerequisites

- Java 17+  
- Maven or Gradle  
- MySQL running locally (or accessible via network)  
- An IDE such as IntelliJ IDEA or Eclipse[web:69][web:71]

### Configuration

1. Create a MySQL database, for example:

   ```sql
   CREATE DATABASE techgadget_ecommerce;
