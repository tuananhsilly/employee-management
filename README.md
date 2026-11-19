# Employee Management System

Mini project to practise Spring Boot through a 10-lab roadmap: employee CRUD, register/login, Admin/User roles and employee statistics.

**Stack:** Java 21, Spring Boot, Spring Data JPA, REST API, Thymeleaf, PostgreSQL, Maven.

Each lab is developed on its own branch and merged into `main` through a pull request.

## Running locally

Requires Java 21 and a PostgreSQL database. Connection settings default to `jdbc:postgresql://localhost:5432/employee_management` with `postgres`/`postgres` and can be overridden with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

```bash
DB_PASSWORD=secret ./mvnw spring-boot:run
curl localhost:8080/hello
```

Without a local database, `./mvnw spring-boot:test-run` starts the app against a PostgreSQL container (needs Docker).

## Web pages

With the app running, HR staff can manage Employees in the browser:

- `/employees/list` — paginated Employee list with edit and delete (delete asks for confirmation)
- `/employees/search` — search by name and/or Department
- `/employees/add` and `/employees/{id}/edit` — add or edit an Employee; validation errors appear next to each field

## Tests

```bash
./mvnw verify
```

Tests start the full application with MockMvc against PostgreSQL in Testcontainers (needs Docker).
