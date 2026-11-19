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
| `prod` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` required, no defaults | INFO |

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://db-host:5432/employee_management \
  DB_USERNAME=app DB_PASSWORD=... java -jar target/employee-management-0.0.1-SNAPSHOT.jar
```

Every Employee create, update and delete is logged at INFO once its transaction commits, with the Employee Code and ids. Names and emails stay out of the log.

## Web pages

With the app running, HR staff can manage Employees in the browser:

- `/employees/list` — paginated Employee list with edit and delete (delete asks for confirmation)
- `/employees/search` — search by name and/or Department
- `/employees/add` and `/employees/{id}/edit` — add or edit an Employee; validation errors appear next to each field

## Operations

- Actuator: `/actuator/health`, `/actuator/info` and `/actuator/metrics` are exposed; other endpoints are not.
- `GET /api/reports/employee-count` returns `{"totalEmployees": n}`. The value is cached with Caffeine for 1 minute (expire-after-write) and evicted whenever an Employee is created or deleted.
- A scheduled task logs `System running` every 30 seconds.

## Tests

```bash
./mvnw verify
```

Tests start the full application with MockMvc against PostgreSQL in Testcontainers (needs Docker).
