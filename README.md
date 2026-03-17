# RevPay - Digital Financial Platform

RevPay is a full-stack monolithic financial web application built with Spring Boot, Thymeleaf, and Oracle SQL. It enables secure digital payments and money management for both **personal** and **business** users.

---

## 🛠 Technology Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.x |
| Frontend | Thymeleaf + Vanilla CSS |
| Security | Spring Security 6 (BCrypt, CSRF, Sessions) |
| Database | Oracle XE via JDBC (raw SQL, no ORM) |
| Logging | Log4J2 |
| PDF Export | iTextPDF |
| CSV Export | OpenCSV |

---

## 🗂 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── revature/
│   │           └── revpay/
│   │               ├── RevPayApplication.java
│   │               ├── config/
│   │               │   ├── DatabaseConfig.java
│   │               │   ├── JwtAuthFilter.java
│   │               │   ├── SecurityConfig.java
│   │               │   └── UserDetailsConfig.java
│   │               ├── controller/
│   │               │   ├── AdminController.java
│   │               │   ├── AuthController.java
│   │               │   ├── BusinessController.java
│   │               │   ├── DashboardController.java
│   │               │   ├── GlobalControllerAdvice.java
│   │               │   ├── GlobalModelAdvice.java
│   │               │   ├── HomeController.java
│   │               │   ├── MoneyRequestController.java
│   │               │   ├── NotificationController.java
│   │               │   ├── PaymentMethodController.java
│   │               │   ├── PersonalInvoiceController.java
│   │               │   ├── ProfileController.java
│   │               │   ├── TransactionController.java
│   │               │   └── WalletController.java
│   │               ├── dao/
│   │               │   ├── BusinessProfileDAO.java
│   │               │   ├── InvoiceDAO.java
│   │               │   ├── LoanApplicationDAO.java
│   │               │   ├── MoneyRequestDAO.java
│   │               │   ├── NotificationDAO.java
│   │               │   ├── PaymentMethodDAO.java
│   │               │   ├── TransactionDAO.java
│   │               │   └── UserDAO.java
│   │               ├── exception/
│   │               │   └── GlobalExceptionHandler.java
│   │               ├── model/
│   │               │   ├── BusinessProfile.java
│   │               │   ├── Invoice.java
│   │               │   ├── InvoiceItem.java
│   │               │   ├── LoanApplication.java
│   │               │   ├── MoneyRequest.java
│   │               │   ├── Notification.java
│   │               │   ├── NotificationPreference.java
│   │               │   ├── PaymentMethod.java
│   │               │   ├── Transaction.java
│   │               │   └── User.java
│   │               ├── service/
│   │               │   ├── BusinessService.java
│   │               │   ├── MoneyRequestService.java
│   │               │   ├── NotificationService.java
│   │               │   ├── PaymentMethodService.java
│   │               │   ├── TransactionService.java
│   │               │   └── UserService.java
│   │               └── util/
│   │                   ├── ExportUtil.java
│   │                   ├── JwtUtil.java
│   │                   └── PasswordUtil.java
│   └── resources/
│       ├── application.properties
│       ├── log4j2.xml
│       ├── sql/
│       │   └── database_setup.sql
│       ├── static/
│       │   ├── css/
│       │   │   └── style.css
│       │   └── js/
│       │       └── app.js
│       └── templates/
│           ├── error.html
│           ├── admin/
│           │   ├── gst.html
│           │   ├── history.html
│           │   └── loans.html
│           ├── auth/
│           │   ├── login.html
│           │   └── register.html
│           ├── business/
│           │   ├── analytics.html
│           │   ├── invoice-detail.html
│           │   ├── invoice-form.html
│           │   ├── invoices.html
│           │   ├── loan-detail.html
│           │   ├── loan-form.html
│           │   └── loans.html
│           ├── dashboard/
│           │   ├── business.html
│           │   └── personal.html
│           ├── fragments/
│           │   └── layout.html
│           ├── notifications/
│           │   ├── index.html
│           │   └── preferences.html
│           ├── payment-methods/
│           │   └── index.html
│           ├── personal/
│           │   ├── invoice-detail.html
│           │   └── invoices.html
│           ├── profile/
│           │   └── index.html
│           ├── transactions/
│           │   ├── history.html
│           │   ├── money-requests-out.html
│           │   ├── money-requests.html
│           │   ├── request.html
│           │   └── send.html
│           └── wallet/
│               └── index.html
└── test/
    └── java/
        └── com/
            └── revature/
                └── revpay/
                    ├── HashTest.java
                    └── UpdateDbTest.java

```

---

## 🔧 Setup Instructions

### Prerequisites

- Java 17+
- Maven 3.8+
- Oracle Database XE (local or remote)
- Maven dependencies resolve automatically via `pom.xml`

### 1. Database Setup

1. Open SQL*Plus or Oracle SQL Developer
2. Run the SQL script:
   ```sql
   @src/main/resources/sql/database_setup.sql
   ```
   This creates all tables, sequences, triggers, and stored procedures.

### 2. Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application starts at: **http://localhost:8182**

---

## 🔐 Security Features

- **BCrypt** password hashing
- **JWT Authentication** enabled
- **Session Management** with single-session enforcement (30 min timeout)
- **Role-Based Access Control**:
  - `/business/**` — BUSINESS role only
  - `/admin/**` - ADMIN role only
  - All other pages require authenticated session
- **Transaction PIN** — optional extra layer for sending money

---

## 👤 Account Types

### Personal Account
- Send/receive money by username, email, or phone
- Request money from others
- Add/withdraw funds to/from wallet
- Manage payment methods (credit/debit cards)
- View transaction history with advanced filters
- Export to CSV or PDF
- Notification

### Business Account
- All personal features
- **Invoice Management** — create, send, mark paid
- **Loan Applications** — apply, track repayments
- **Business Analytics** — revenue charts, invoice breakdown
- Business profile management

---

## 📊 Database Schema

| Table | Description |
|-------|-------------|
| `USERS` | Core user accounts (personal & business) |
| `BUSINESS_PROFILES` | Business-specific profile data |
| `TRANSACTIONS` | All payment transactions |
| `PAYMENT_METHODS` | Credit/debit cards |
| `MONEY_REQUESTS` | Peer-to-peer payment requests |
| `NOTIFICATIONS` | User notification messages |
| `NOTIFICATION_PREFS` | Per-type notification preferences |
| `INVOICES` + `INVOICE_ITEMS` | Business invoicing |
| `LOAN_APPLICATIONS` | Business loan tracking |

---

## 📦 Key Maven Dependencies

```xml
spring-boot-starter-web
spring-boot-starter-thymeleaf
spring-boot-starter-security
spring-boot-starter-jdbc
ojdbc11
opencsv
itextpdf
log4j2
```

---

## 🧪 Test Data

Register a new account via `/auth/register`. Use the **Business** tab to create a business account with invoicing and loan features.

---

*Built for Revature Project 2 — RevPay Digital Financial Platform*
