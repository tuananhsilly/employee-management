# Employee Management System

Mini project to practise Spring Boot through a 10-lab roadmap: employee CRUD, register/login, Admin/User roles and employee statistics.

**Stack:** Java 21, Spring Boot, Spring Data JPA, REST API, Thymeleaf, PostgreSQL, Maven.

Each lab is developed on its own branch and merged into `main` through a pull request.

## Running locally

Requires Java 21 and a PostgreSQL database.

The `dev` profile is the default. It connects to `jdbc:postgresql://localhost:5432/employee_management` as `postgres`/`postgres` and logs the application at DEBUG. Override the connection with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`:

```bash
DB_PASSWORD=secret ./mvnw spring-boot:run
curl localhost:8080/hello
```

Without a local database, `./mvnw spring-boot:test-run` starts the app against a PostgreSQL container (needs Docker).

## Profiles

| Profile | Database | Logging |
| --- | --- | --- |
| `dev` (default) | Local PostgreSQL; `DB_*` variables optional | `com.example.employeemanagement` at DEBUG |
| `prod` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` required, no defaults; also `JWT_SECRET`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` | INFO |

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://db-host:5432/employee_management \
  DB_USERNAME=app DB_PASSWORD=... JWT_SECRET=... ADMIN_USERNAME=... ADMIN_PASSWORD=... java -jar target/employee-management-0.0.1-SNAPSHOT.jar
```

Every Employee create, update and delete is logged at INFO once its transaction commits, with the Employee Code and ids. Names and emails stay out of the log.

## Web pages

With the app running, HR staff can manage Employees in the browser:

- `/employees/list` — paginated Employee list with edit and delete (delete asks for confirmation)
- `/employees/search` — search by name and/or Department
- `/employees/add` and `/employees/{id}/edit` — add or edit an Employee; validation errors appear next to each field
- `/employees/statistics` — total Employees, Employees per Department and hires per month (both Roles)

## Security

User Accounts are separate from Employees. Each account has a Role: **User** (read Employees, Departments and reports) or **Admin** (also create, update and delete Employees).

- Pages: `/register`, then log in at `/login` (session); log out from the navigation bar. Add, edit and delete are hidden from Users.
- API: register with `POST /api/auth/register`, then `POST /api/auth/login` returns a JWT valid for 1 hour. Send it as `Authorization: Bearer <token>`. A missing, invalid or expired token gives 401; a User changing an Employee gets 403.
- Public: `/hello`, `/login`, `/register`, `/api/auth/**`, static files and `/actuator/health`. Other Actuator endpoints are for Admins.
- Self-registered accounts always get the User Role. An initial Admin is created at startup if none exists: in `dev` it is `admin` / `admin12345`; in `prod` it comes from `ADMIN_USERNAME` and `ADMIN_PASSWORD`, and the JWT secret from `JWT_SECRET` (at least 32 bytes).

```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' -d '{"username":"hr.lan","password":"matkhau123"}'
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"hr.lan","password":"matkhau123"}'
curl localhost:8080/api/employees -H "Authorization: Bearer <accessToken>"
```

## Operations

- Actuator: `/actuator/health` (public), `/actuator/info` and `/actuator/metrics` (Admins only) are exposed; other endpoints are not.
- `GET /api/reports/employee-count` returns `{"totalEmployees": n}`. The value is cached with Caffeine for 1 minute (expire-after-write) and evicted whenever an Employee is created or deleted.
- Statistics (both Roles, computed by `@Query` aggregates, not cached):
  - `GET /api/statistics/total` returns `{"totalEmployees": n}`
  - `GET /api/statistics/by-department` returns `[{"departmentId", "departmentName", "employeeCount"}]` for every Department, including those with 0
  - `GET /api/statistics/hires-by-month` returns `[{"month": "yyyy-MM", "hires": n}]` for the current month and the 11 before it, oldest first, with 0 for months without hires
- A scheduled task logs `System running` every 30 seconds.

## Tests

```bash
./mvnw verify
```

Tests start the full application with MockMvc against PostgreSQL in Testcontainers (needs Docker).
