# MediCare web app (React + Vite)

The browser front end for the MediCare backend. **You do not need Node.js to run the project** -
the finished app is already built into `../src/main/resources/static`, and Spring Boot serves it.

## Just run it

```
cd medicare
set DB_USERNAME=root
set DB_PASSWORD=...your MySQL password...
mvn spring-boot:run
```
Open **http://localhost:8080** and sign in.

No admin login yet (fresh database, or forgotten password)? Create one at startup, then sign in and
use the *Accounts* screen to create everyone else / reset passwords:
```
set BOOTSTRAP_ADMIN_USERNAME=admin
set BOOTSTRAP_ADMIN_PASSWORD=ChooseSomethingStrong1
mvn spring-boot:run
```
(It only creates the account if that username doesn't exist; it never changes an existing one.)

## Changing the front end (needs Node.js 20+)

```
cd frontend
npm install
npm run dev        # http://localhost:5173 with live reload; /api is forwarded to :8080
npm run build      # writes the production files into ../src/main/resources/static
```
Start the backend first for `npm run dev`. Commit/ship the rebuilt `static` folder.

## How it is organised

| Folder | What lives there |
|---|---|
| `src/lib/api.js` | the ONLY place that calls the backend (token, errors, 401 handling) |
| `src/lib/access.js` | which role sees which pages/buttons (mirrors `SecurityConfig.java`) |
| `src/context/` | signed-in user + auto sign-out at token expiry; toast messages |
| `src/components/` | reusable pieces: table, form dialogs, line-item editor, bulk import, layout |
| `src/pages/` | one file per screen |

Roles: **ADMIN** (everything administrative), **RECEPTIONIST** (patients, appointments, billing),
**DOCTOR** (own appointments/patients, records, prescriptions), **PATIENT** (own data, read-only).
Hiding a button is only a convenience - the backend enforces every rule.
