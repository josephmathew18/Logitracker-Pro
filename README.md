# Delivery Service Provider Management System

A full-stack Java Spring Boot web application designed for managing customer requests, agent allocations, real-time GPS tracking simulation, and refueling expenses.

---

## 🚀 Features

- **Spring Security Authentication**: Role-Based Authorization for three roles:
  - **Admin**: Monitor all bookings, register agents, manage vehicles, allocate orders, view expense logs, and review customer feedback.
  - **Customer**: Book shipments, view request history, track active orders live on an interactive map, and submit ratings/feedback.
  - **Delivery Agent**: View assigned jobs, accept/reject dispatches, update delivery status (Assigned, Picked Up, In Transit, Delivered), update real-time coordinates, upload refueling bills (petrol/diesel), and view expense logs.
- **MySQL Integration**: Standard database schema using Spring Data JPA.
- **Live Tracking System**: Dynamic Leaflet.js interactive maps plotting shipment paths in real-time.
- **Beginner-Friendly Architecture**: Built without Lombok using standard clean Java POJOs (Getters/Setters) to avoid VS Code Lombok plugin issues.
- **Auto-Initialization**: Seed testing accounts and vehicles dynamically on startup for instant execution.

---

## 🛠️ Technology Stack

- **Backend**: Java 21, Spring Boot 3.2.5, Spring Security 6, Spring Data JPA
- **Frontend**: HTML5, custom styled Bootstrap 5 CSS layout, JavaScript (Leaflet.js map tracking, AJAX coordinates polling)
- **Database**: MySQL (Default) / H2 In-Memory (Fallback)
- **Build Tool**: Maven

---

## ⚙️ Step-by-Step Setup Instructions for VS Code

### 1. Prerequisites
Ensure you have the following installed on your machine:
- **Java Development Kit (JDK) 21**: Verify by running `java -version` in your terminal.
- **MySQL Server**: Ensure MySQL is running on your machine.
- **VS Code** with the following extensions (search in VS Code Extensions Marketplace):
  - **Extension Pack for Java** (by Microsoft)
  - **Spring Boot Extension Pack** (by VMware)

---

### 2. Database Setup

#### Option A: MySQL Server (Recommended)
1. Open your MySQL command-line client or administrative tool (like phpMyAdmin or MySQL Workbench).
2. Create the database named `delivery_db`:
   ```sql
   CREATE DATABASE delivery_db;
   ```
3. Open `src/main/resources/application.properties` in VS Code and check the datasource credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/delivery_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=root
   ```
   *Change username/password to match your local MySQL installation settings.*

#### Option B: H2 In-Memory Database (No Installation Needed)
If you don't have MySQL installed and want to run the application immediately:
1. Open [src/main/resources/application.properties](file:///c:/Users/mathu/OneDrive/Desktop/delivery/src/main/resources/application.properties).
2. Follow the comments in the file to:
   - Comment out the MySQL configuration lines.
   - Uncomment the H2 Database Configuration lines.

---

### 3. Open the Project in VS Code
1. Open VS Code.
2. Select **File > Open Folder...** and choose the `delivery` project directory (`C:\Users\mathu\OneDrive\Desktop\delivery`).
3. Wait for the **Java Projects** status bar to finish indexing and resolving Maven dependencies.

---

### 4. Running the Application

#### Option A: Using the VS Code Spring Boot Dashboard
1. Click on the **Spring Boot Dashboard** icon in the VS Code sidebar (looks like a leaf).
2. Find `delivery-service` (or `DeliveryApplication`) and click the **Run** (Play) button next to it.

#### Option B: Using the Terminal (Command Line)
1. Open a terminal in VS Code (**Terminal > New Terminal**).
2. Run the application using the Maven wrapper:
   - **On Windows (PowerShell/CMD)**:
     ```powershell
     ./mvnw spring-boot:run
     ```
   - **On macOS/Linux**:
     ```bash
     chmod +x mvnw
     ./mvnw spring-boot:run
     ```
3. Wait for the terminal log to print:
   ```text
   >>> Database initialized successfully!
   Tomcat started on port 8080 (http) with context path ''
   Started DeliveryApplication in X.XXX seconds
   ```

---

## 🔑 Demo Access Credentials

The database is pre-seeded on first run. You can log in using these default credentials:

| Role | Username / ID | Password | Actions / Access |
|---|---|---|---|
| **Admin** | `admin` | `admin123` | Control Panel, Fleet Management, Dispatch Allocation, Reports |
| **Customer** | `customer` | `customer123` | Booking form, active map tracking, feedback reviews |
| **Agent** | `agent` | `agent123` | Accept dispatches, log locations, upload refueling bill claims |

### How to test the simulation:
1. Log in as **Customer** (`customer` / `customer123`) and click **Book Shipment**. Submit a new request (e.g. from *Baker Street* to *Piccadilly Circus*).
2. Log out, then log in as **Admin** (`admin` / `admin123`). Navigate to **Allocate Deliveries**. You will see the request. Select the driver (`David Smith`) and a vehicle, and click **Allocate & Dispatch**.
3. Log out, then log in as **Agent** using the default agent (`agent` / `agent123`).
4. On the Agent Dashboard, you will see the new job. Click **Accept**.
5. Select a status (e.g., `IN_TRANSIT`), click on one of the simulated shortcut buttons (e.g., `Route Pt A`) to auto-fill latitude/longitude coordinates, and click **Update Location & Status**.
6. Log out, log in back as **Customer** or **Admin**, and click **Track Live / Track GPS**. You will see the agent's location marker plotted on the interactive map!
7. On the **Agent** portal, navigate to **Refuel Expenses**, fill out the quantity and price, select an image file for the receipt, and upload. Log in back as **Admin** and check **Reports & Expenses** to review the claims and download or view the receipt.

---

## 📁 Complete Project Structure

```text
delivery/
├── database/
│   ├── schema.sql             # Reference SQL Schema Script
│   └── data.sql               # Reference SQL Dummy Data Script
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── delivery/
│   │   │           ├── config/
│   │   │           │   ├── DatabaseInitializer.java
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   ├── CustomSuccessHandler.java
│   │   │           │   └── WebConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── AuthController.java
│   │   │           │   ├── AdminController.java
│   │   │           │   ├── CustomerController.java
│   │   │           │   ├── AgentController.java
│   │   │           │   └── TrackingRestController.java
│   │   │           ├── model/
│   │   │           │   ├── User.java
│   │   │           │   ├── Customer.java
│   │   │           │   ├── Agent.java
│   │   │           │   ├── Vehicle.java
│   │   │           │   ├── Delivery.java
│   │   │           │   ├── FuelExpense.java
│   │   │           │   ├── Tracking.java
│   │   │           │   └── Feedback.java
│   │   │           ├── repository/
│   │   │           │   ├── UserRepository.java
│   │   │           │   ├── CustomerRepository.java
│   │   │           │   ├── AgentRepository.java
│   │   │           │   ├── VehicleRepository.java
│   │   │           │   ├── DeliveryRepository.java
│   │   │           │   ├── FuelExpenseRepository.java
│   │   │           │   ├── TrackingRepository.java
│   │   │           │   └── FeedbackRepository.java
│   │   │           ├── service/
│   │   │           │   ├── UserService.java
│   │   │           │   ├── AgentService.java
│   │   │           │   ├── DeliveryService.java
│   │   │           │   ├── ExpenseService.java
│   │   │           │   ├── FeedbackService.java
│   │   │           │   ├── VehicleService.java
│   │   │           │   └── CustomUserDetailsService.java
│   │   │           └── DeliveryApplication.java
│   │   └── resources/
│   │       ├── static/
│   │       │   └── css/
│   │       │       └── custom.css
│   │       ├── templates/
│   │       │   ├── admin/
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── agents.html
│   │       │   │   ├── vehicles.html
│   │       │   │   ├── assign.html
│   │       │   │   ├── monitor.html
│   │       │   │   └── reports.html
│   │       │   ├── customer/
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── request.html
│   │       │   │   └── track.html
│   │       │   ├── agent/
│   │       │   │   ├── dashboard.html
│   │       │   │   └── expenses.html
│   │       │   ├── login.html
│   │       │   └── register.html
│   │       └── application.properties
│   └── test/                  # Test Classes
├── pom.xml                    # Maven Dependency Build Descriptor
└── README.md                  # Setup & Instruction Documentation
```
