```markdown
# TechGadget E‑Commerce Backend

A production‑style e‑commerce backend built with **Java & Spring Boot 4**, featuring JWT authentication, product catalog, cart, orders, reviews, and admin tools.[web:63][web:71]  
Designed as a learning and portfolio project, but structured with real‑world patterns (DTOs, validations, global error handling, and OpenAPI docs).[web:68][web:72]

---

## Features

### Authentication & Users
- User registration and login with **JWT**.
- Role‑based access control: `USER` and `ADMIN`.
- Secured endpoints for carts, orders, reviews, and admin operations.[web:69][web:72]

### Product Catalog
- Public endpoints to list and view product details.
- Admin CRUD for products: create, update, delete, manage stock and pricing.
- Products include stock, price, category, description, and other metadata.[web:63][web:71]

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
  - Update order status (ship, complete, cancel).[web:71][web:72]

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
- Helpful for frontend integration and manual testing.[web:69][web:71]

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
   ```

2. Configure `application.properties` (or `application.yml`):

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/techgadget_ecommerce
   spring.datasource.username=your_username
   spring.datasource.password=your_password

   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true

   # JWT settings (example)
   app.jwt.secret=your_jwt_secret
   app.jwt.expiration-ms=3600000
   ```

### Run the Application

Using Maven:

```bash
mvn spring-boot:run
```

Or run the main class from your IDE.  
The API will be available at: `http://localhost:8080/api/...`.[web:65][web:71]

---

## Core Endpoints (Overview)

> Exact paths may vary slightly depending on your controller mappings, but the structure follows this style.[web:63][web:68][web:71]

### Auth

- `POST /api/auth/register` – Register a new user  
- `POST /api/auth/login` – Login and receive a JWT token  

### Products

- `GET /api/products` – List products (with pagination & filters)  
- `GET /api/products/{id}` – Get product detail  
- `POST /api/admin/products` – Create product (ADMIN)  
- `PUT /api/admin/products/{id}` – Update product (ADMIN)  
- `DELETE /api/admin/products/{id}` – Delete product (ADMIN)

### Cart

- `GET /api/cart` – Get current user cart  
- `POST /api/cart/items` – Add item to cart  
- `PUT /api/cart/items/{itemId}` – Update quantity  
- `DELETE /api/cart/items/{itemId}` – Remove item[web:68][web:71]

### Orders

- `POST /api/orders` – Checkout selected cart items → create order  
- `GET /api/orders` – Get current user orders  
- `GET /api/orders/{id}` – Get order details  
- `POST /api/orders/{id}/cancel` – Cancel order (if allowed by status)  
- `POST /api/orders/{id}/pay` – Mark order as paid (dummy payment)[web:71]

### Admin Orders

- `GET /api/admin/orders` – Search all orders with filters & pagination  
- `GET /api/admin/orders/user/{userId}` – Get all orders for a specific user  
- `GET /api/admin/orders/{id}` – Get order details (admin view)  
- `PUT /api/admin/orders/{id}/status` – Update order status (ship, complete, cancel)[web:71][web:72]

### Reviews

- `POST /api/reviews` – Create a review for a purchased product  
- `GET /api/products/{id}/reviews` – List reviews for a product  
- Validation ensures the user has actually ordered the product and prevents spam.[web:63]

---

## Frontend Integration

This backend is designed to be consumed by a modern SPA/SSR frontend (e.g. **Next.js (React + TypeScript)**):

- JWT is returned on login and expected in the `Authorization: Bearer <token>` header.
- CORS can be configured to allow a frontend origin such as `http://localhost:3000`.
- Pagination and filter parameters are ready for product listing and admin dashboards.[web:19][web:69]

---

## Roadmap / Future Improvements

- Real payment gateway integration (Midtrans, Stripe, etc.).
- More advanced shipping logic (multiple addresses, couriers, tracking).
- Inventory reservations and concurrent stock handling.
- More detailed analytics (conversion rates, best‑selling products).
- Test coverage: unit & integration tests for critical flows (auth, checkout, status transitions).[web:70][web:72]

---

## License

This project is intended for learning and portfolio purposes.  
You are free to explore, fork, and adapt it for your own educational use.
```
