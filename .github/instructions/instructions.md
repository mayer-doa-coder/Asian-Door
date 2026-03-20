---
description: Asian Wooden Decor project context and guidelines. Load this whenever working on any part of the Asian Wooden Decor e-commerce platform.
applyTo: '**'
---

# Asian Wooden Decor — Project Reference

## Project Overview

**Asian Wooden Decor** is a web-based single-vendor e-commerce platform for selling doors online. The administrator is the sole seller and manages all products, orders, and inventory through a centralized admin panel. The goal is to modernize the traditional door-selling business by providing customers with a convenient online shopping experience.

---

## Tech Stack

- **Backend:** Java, Spring Boot
- **Build Tool:** Maven (`pom.xml`)
- **Frontend:** Thymeleaf (server-side templates in `src/main/resources/templates/`), static assets in `src/main/resources/static/`
- **Database:** (to be configured — likely MySQL or H2)
- **Security:** Spring Security (authentication, authorization, password management)

---

## Core Features

### Customer-Facing
- Browse a structured product catalog of doors
- Product categories: Wooden, Steel, Glass, Security, Interior, Exterior
- Each product shows: name, category, specifications, materials, dimensions, price, images
- Search and filter products by preferences
- User registration and login (authentication)
- Customer profile management and order history
- Shopping cart — add/remove/review items
- Checkout process — place orders
- Order confirmation and order tracking

### Admin Panel
- Secure admin-only dashboard
- Add, update, and delete door products
- Upload and manage product images
- Manage inventory levels and product availability
- Set and update product prices
- View and process customer orders
- Keep the catalog organized and up to date

### Security
- Spring Security-based authentication
- Secure password management (BCrypt hashing)
- Role-based access control: `ROLE_CUSTOMER`, `ROLE_ADMIN`
- Protected admin routes — customers cannot access admin pages

---

## Project Structure

```
src/
  main/
    java/com/asiandoor/
      AsiandoorApplication.java       # Entry point
      controller/                     # MVC controllers (to be created)
      model/                          # JPA entities (to be created)
      repository/                     # Spring Data repositories (to be created)
      service/                        # Business logic (to be created)
      config/                         # Security config, etc. (to be created)
    resources/
      application.properties          # App configuration
      static/                         # CSS, JS, images
      templates/                      # Thymeleaf HTML templates
  test/
    java/com/asiandoor/
      AsiandoorApplicationTests.java
```

---

## Key Entities (Planned)

| Entity | Key Fields |
|---|---|
| `User` | id, name, email, password, role, createdAt |
| `Product` | id, name, category, description, material, dimensions, price, stock, images |
| `Category` | id, name (Wooden, Steel, Glass, Security, Interior, Exterior) |
| `Cart` | id, user, items |
| `CartItem` | id, cart, product, quantity |
| `Order` | id, user, items, totalPrice, status, createdAt |
| `OrderItem` | id, order, product, quantity, price |

---

## Order Status Flow

`PENDING` → `CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED`

Admin can cancel orders: any status → `CANCELLED`

---

## Conventions & Guidelines

- Follow standard Spring Boot MVC layered architecture: Controller → Service → Repository → Entity
- Use Thymeleaf for all HTML templates; keep templates in `src/main/resources/templates/`
- Secure all `/admin/**` routes with `ROLE_ADMIN`; authenticate all `/account/**` and `/orders/**` routes
- Use DTOs where appropriate to avoid exposing JPA entities directly to the view layer
- Keep `application.properties` for environment config (DB URL, credentials, etc.)
- Use BCrypt for password encoding
- Handle images as file uploads stored in `src/main/resources/static/images/products/` or an external store

---

## Reference Notes

- This file is the single source of truth for project context.
- Update this file as new decisions are made (DB choice, deployment strategy, UI framework, etc.).
- When losing context, re-read this file before proceeding with implementation.