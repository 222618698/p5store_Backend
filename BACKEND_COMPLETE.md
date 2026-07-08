# P5Store Backend - Complete Implementation

## ✅ What's Built

### 1. Domain Layer (10 Entities)
- **User** - Authentication, roles (CUSTOMER/ADMIN)
- **Address** - Shipping/billing addresses
- **Category** - Self-referencing product categories
- **Product** - SKU, price, stock, status
- **Cart** - One per user
- **CartItem** - Products in cart
- **Order** - Immutable purchase records
- **OrderItem** - Line items with price snapshots
- **Payment** - Payment tracking (1:1 with Order)
- **Review** - Product reviews (unique per user+product)

### 2. Repository Layer (7 Repositories)
All using Spring Data JPA with custom queries:
- UserRepository
- CategoryRepository  
- ProductRepository (search, price range, featured, new arrivals)
- CartRepository (fetch with items)
- OrderRepository (user orders with items)
- AddressRepository
- ReviewRepository (average ratings)

### 3. Service Layer (4 Services)
**UserService** - Register, login, JWT generation
**ProductService** - CRUD, search, filter, pagination
**CartService** - Add/update/remove items, stock validation
**OrderService** - Place order, stock deduction, shipping calc, cancel

### 4. Controller Layer (4 REST Controllers)
**AuthController** - POST /v1/auth/register, /login
**ProductController** - Full CRUD + search (admin-only write)
**CartController** - Cart management
**OrderController** - Order placement, history, cancel

### 5. Security
- JWT authentication (HS256)
- BCrypt password hashing
- Role-based access (ADMIN/CUSTOMER)
- Stateless sessions

### 6. Tests (4 Test Suites - 30 Tests Total)
- ProductServiceTest (10 tests)
- UserServiceTest (7 tests)
- CartServiceTest (7 tests)
- OrderServiceTest (6 tests)

## 📋 API Endpoints

### Public
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
GET    /api/v1/products
GET    /api/v1/products/{id}
GET    /api/v1/products/sku/{sku}
GET    /api/v1/products/featured
GET    /api/v1/products/new-arrivals
GET    /api/v1/products/category/{categoryId}
GET    /api/v1/products/search?q=
GET    /api/v1/products/price-range?min=&max=
```

### Authenticated
```
GET    /api/v1/users/{userId}/cart
POST   /api/v1/users/{userId}/cart/items
PATCH  /api/v1/users/{userId}/cart/items/{productId}
DELETE /api/v1/users/{userId}/cart/items/{productId}
DELETE /api/v1/users/{userId}/cart
POST   /api/v1/users/{userId}/orders
GET    /api/v1/users/{userId}/orders
GET    /api/v1/orders/{orderId}
GET    /api/v1/orders/number/{orderNumber}
POST   /api/v1/users/{userId}/orders/{orderId}/cancel
```

### Admin Only
```
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
PATCH  /api/v1/orders/{orderId}/status
```

## 🗄️ Database
- Development: H2 (in-memory, MySQL mode)
- Production: MySQL
- Auto-create schema on startup
- JPA Auditing enabled (createdAt/updatedAt)

## ⚙️ Configuration
Edit `src/main/resources/application.yml`:
```yaml
spring.datasource:
  url: jdbc:mysql://localhost:3306/p5store
  username: ${DB_USERNAME:root}
  password: ${DB_PASSWORD:yourpassword}

app.jwt:
  secret: ${JWT_SECRET:changeme}
  expiration-ms: 86400000  # 24 hours
```

## 🚀 Running

### Start Server
```bash
./mvnw spring-boot:run
```
Server runs on http://localhost:8080/api

### Run Tests
```bash
./mvnw test
```

## 📦 Tech Stack
- Java 21
- Spring Boot 3.2.5
- Spring Data JPA + Hibernate
- Spring Security + JWT (jjwt 0.12.5)
- MySQL Connector
- Lombok
- H2 (tests)
- JUnit 5 + Mockito

## ✨ Business Logic Highlights

### Cart
- Merges duplicate products (sums quantity)
- Validates stock availability
- Prevents out-of-stock items

### Order Placement
- Validates all cart items have sufficient stock
- Deducts stock from products
- Sets product status to OUT_OF_STOCK when quantity = 0
- Calculates shipping (free above R500, else R50)
- Snapshots product prices and address
- Clears cart after successful order
- Creates pending payment record

### Order Cancellation
- Only allowed for PENDING/CONFIRMED status
- Restores stock to products
- Reactivates OUT_OF_STOCK products

### Product Management
- SKU uniqueness enforced
- Soft delete (sets status=INACTIVE)
- Auto status=OUT_OF_STOCK when stockQuantity=0
- Price comparison support (compareAtPrice)

### Security
- Passwords hashed with BCrypt
- JWT tokens expire in 24 hours
- Role-based endpoints
- User accounts can be disabled (isActive=false)

## 📝 Next Steps for Frontend
Frontend should consume these endpoints. Key flows:

1. **Registration/Login** → Store JWT in localStorage
2. **Product Browsing** → No auth required
3. **Add to Cart** → Requires auth, sends userId + productId + quantity
4. **Checkout** → Select address, place order
5. **Admin Panel** → Create/update/delete products (requires ADMIN role)

All responses are JSON. Errors return structured format:
```json
{
  "status": 404,
  "message": "Product not found: 123",
  "timestamp": "2024-01-15T10:30:00"
}
```
