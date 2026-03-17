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
src/main/java/com/revature/revpay/
├── RevPayApplication.java
├── config/
│   ├── DatabaseConfig.java         # Oracle DataSource bean
│   └── SecurityConfig.java         # Spring Security configuration
├── model/
│   ├── User.java
│   ├── Transaction.java
│   ├── PaymentMethod.java
│   ├── MoneyRequest.java
│   ├── Notification.java, NotificationPreference.java
│   ├── Invoice.java, InvoiceItem.java
│   ├── LoanApplication.java
│   └── BusinessProfile.java
├── dao/                            # All raw JDBC DAOs
│   ├── UserDAO.java
│   ├── TransactionDAO.java
│   ├── PaymentMethodDAO.java
│   ├── MoneyRequestDAO.java
│   ├── NotificationDAO.java
│   ├── InvoiceDAO.java
│   ├── LoanApplicationDAO.java
│   └── BusinessProfileDAO.java
├── service/                        # Business logic layer
│   ├── UserService.java
│   ├── TransactionService.java
│   ├── NotificationService.java
│   ├── MoneyRequestService.java
│   ├── PaymentMethodService.java
│   └── BusinessService.java
├── controller/                     # Spring MVC Controllers
│   ├── HomeController.java
│   ├── AuthController.java
│   ├── DashboardController.java
│   ├── TransactionController.java
│   ├── MoneyRequestController.java
│   ├── PaymentMethodController.java
│   ├── WalletController.java
│   ├── NotificationController.java
│   ├── ProfileController.java
│   └── BusinessController.java
└── util/
    ├── PasswordUtil.java            # BCrypt helpers
    └── ExportUtil.java              # CSV + PDF export

src/main/resources/
├── application.properties
├── log4j2.xml
├── sql/database_setup.sql           # Complete Oracle schema
├── static/
│   ├── css/style.css                # Premium dark theme
│   └── js/app.js                    # Frontend JS
└── templates/
    ├── fragments/layout.html        # Sidebar layout
    ├── auth/login.html, register.html
    ├── dashboard/personal.html, business.html
    ├── transactions/
    │   ├── send.html, history.html, request.html
    │   └── money-requests.html, money-requests-out.html
    ├── wallet/index.html
    ├── payment-methods/index.html
    ├── notifications/index.html, preferences.html
    ├── profile/index.html
    ├── business/
    │   ├── invoices.html, invoice-form.html, invoice-detail.html
    │   ├── loans.html, loan-form.html, loan-detail.html
    │   └── analytics.html
    └── error.html
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

The application starts at: **http://localhost:8080**

---

## 🔐 Security Features

- **BCrypt** password hashing
- **CSRF** protection on all POST forms
- **Session Management** with single-session enforcement (30 min timeout)
- **Role-Based Access Control**:
  - `/business/**` — BUSINESS role only
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
- Notification preferences

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
