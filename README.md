# MediCare: Hospital Management System

A full-stack hospital management system built with **Spring Boot 3**, **React 18** and **MySQL**.
Admins, receptionists, doctors and patients each sign in to their own role-based view of the hospital.
The system covers appointments, medical records, prescriptions, billing and automatic SMS reminders.

## Features

- **JWT authentication** with role-based access for four roles: `ADMIN`, `RECEPTIONIST`, `DOCTOR` and `PATIENT`
- **Patients**: register, search and manage records (doctors see only their own patients)
- **Doctors and departments**: manage staff and hospital departments
- **Appointments**: book, reschedule and track status
- **Medical records and prescriptions**: written by doctors, visible to their patients
- **Billing**: itemised bills with payment tracking
- **SMS reminders**: a scheduled job sends appointment reminders through Twilio, or through a mock provider during development
- **CSV bulk import**: import patients and other data in bulk
- **Account management**: admins create logins and reset passwords
- **Dashboard**: an overview tailored to each role

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Bean Validation |
| Auth | JWT (jjwt 0.12) |
| Database | MySQL 8 |
| Frontend | React 18, Vite 6, lucide-react |
| SMS | Twilio (switchable to a mock provider) |
| Build | Maven |

## Project structure

```
medicare/
├── src/main/java/com/medicare/
│   ├── config/         # Security, scheduling, SMS, admin bootstrap
│   ├── controller/     # REST endpoints (/api/...)
│   ├── service/        # Business logic
│   ├── repository/     # JPA repositories
│   ├── entity/         # JPA entities
│   ├── dto/            # Request/response objects
│   ├── security/       # JWT filter and utilities
│   ├── scheduler/      # Appointment reminder job
│   └── integration/sms # SMS providers (Twilio and mock)
├── src/main/resources/
│   ├── application.properties
│   └── static/         # Built React app, served by Spring Boot
└── frontend/           # React source code (Vite)
```

## Getting started

### Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8 (running locally)
- Node.js 20+ (needed only if you change the frontend)

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/medicare.git
cd medicare
```

### 2. Configure environment variables

Copy `.env.example` to `.env`, or set the variables in your shell or IDE. At minimum, set:

| Variable | Description | Default |
|---|---|---|
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | none |
| `DB_NAME` | Database name (created automatically) | `medicare_db` |
| `JWT_SECRET` | Signing key, at least 32 characters | dev-only value |
| `BOOTSTRAP_ADMIN_USERNAME` | Creates the first admin account | none |
| `BOOTSTRAP_ADMIN_PASSWORD` | Password for that admin | none |
| `SMS_PROVIDER` | `mock` (log only) or `twilio` | `mock` |

For real SMS, set `SMS_PROVIDER=twilio` and fill in `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` and `TWILIO_FROM_NUMBER`.
See `.env.example` for the full list, including reminder schedule and timezone settings.

### 3. Run the app

**Windows (cmd):**
```cmd
set DB_PASSWORD=your_mysql_password
set BOOTSTRAP_ADMIN_USERNAME=admin
set BOOTSTRAP_ADMIN_PASSWORD=ChooseSomethingStrong1
mvn spring-boot:run
```

**macOS / Linux:**
```bash
export DB_PASSWORD=your_mysql_password
export BOOTSTRAP_ADMIN_USERNAME=admin
export BOOTSTRAP_ADMIN_PASSWORD=ChooseSomethingStrong1
mvn spring-boot:run
```

Open **http://localhost:8080** and sign in as the admin. From the **Accounts** screen you can create logins for doctors, receptionists and patients.

> The bootstrap admin is created only if that username does not already exist. An existing account is never changed.

## Frontend development

The built frontend is already in `src/main/resources/static`, so you don't need Node.js just to run the project.
To change the UI:

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173 with live reload; /api is proxied to :8080
npm run build    # writes production files into ../src/main/resources/static
```

Start the backend before you run `npm run dev`. After `npm run build`, commit the rebuilt `static` folder.

## Roles at a glance

| Role | Can do |
|---|---|
| Admin | Everything, including departments, doctors, accounts and running reminders |
| Receptionist | Patients, appointments, billing, SMS log and creating patient logins |
| Doctor | Their own patients and appointments, medical records and prescriptions |
| Patient | Their own appointments, records, prescriptions and bills |

## Security notes

- Never commit your real `.env` file. It is already listed in `.gitignore`.
- Outside local development, set your own `JWT_SECRET`. You can generate one with `openssl rand -base64 32`.
- In production, set `DDL_AUTO=validate` and use a migration tool such as Flyway.

## License

This project is for educational purposes. Add a license of your choice, such as MIT.
