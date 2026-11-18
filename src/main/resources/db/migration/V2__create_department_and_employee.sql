CREATE TABLE department (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO department (name) VALUES ('IT'), ('HR'), ('Sales'), ('Finance'), ('Marketing');

CREATE TABLE employee (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_code VARCHAR(20)  NOT NULL UNIQUE,
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(30),
    position      VARCHAR(100),
    hire_date     DATE         NOT NULL,
    department_id BIGINT       NOT NULL REFERENCES department (id)
);

CREATE INDEX idx_employee_department_id ON employee (department_id);
