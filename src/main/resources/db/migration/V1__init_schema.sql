-- V1__init_schema.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE TYPE task_status AS ENUM ('ORDERED','STARTED','DONE','CANCELLED');

CREATE TABLE vehicles (
    festival_year INTEGER NOT NULL, vehicle_id SERIAL,
    vehicle_no VARCHAR(20) NOT NULL, registration VARCHAR(20), model TEXT,
    total_weight_kg NUMERIC(8,0), payload_kg NUMERIC(8,0),
    vehicle_length_mm NUMERIC(7,0), vehicle_width_mm NUMERIC(7,0), vehicle_height_mm NUMERIC(7,0),
    cargo_length_mm NUMERIC(7,0), cargo_width_mm NUMERIC(7,0), cargo_height_mm NUMERIC(7,0),
    has_pallet_lifter BOOLEAN NOT NULL DEFAULT FALSE,
    has_sack_truck BOOLEAN NOT NULL DEFAULT FALSE,
    can_fetch BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    CONSTRAINT pk_vehicles PRIMARY KEY (festival_year, vehicle_id),
    CONSTRAINT uq_vehicle_no UNIQUE (festival_year, vehicle_no)
);

CREATE TABLE employees (
    festival_year INTEGER NOT NULL, employee_id SERIAL,
    first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL,
    phone_mobile VARCHAR(30), email VARCHAR(255),
    is_admin BOOLEAN NOT NULL DEFAULT FALSE, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_employees PRIMARY KEY (festival_year, employee_id)
);

CREATE TABLE user_accounts (
    festival_year INTEGER NOT NULL, employee_id INT NOT NULL,
    username VARCHAR(100) NOT NULL, password_hash TEXT NOT NULL,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_user_accounts PRIMARY KEY (festival_year, employee_id),
    CONSTRAINT uq_user_accounts_name UNIQUE (festival_year, username),
    CONSTRAINT fk_user_accounts_emp FOREIGN KEY (festival_year, employee_id) REFERENCES employees (festival_year, employee_id) ON DELETE CASCADE
);

CREATE TABLE tasks (
    festival_year INTEGER NOT NULL, task_id SERIAL,
    start_ts TIMESTAMPTZ NOT NULL, end_ts TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    start_point TEXT, end_point TEXT, via_point TEXT,
    division TEXT, team TEXT, affiliation TEXT,
    booker_name TEXT, booker_phone VARCHAR(30), contact_name TEXT, contact_phone VARCHAR(30),
    vehicle_id INT, driver_name TEXT, description TEXT,
    status task_status NOT NULL DEFAULT 'ORDERED', received_by TEXT,
    CONSTRAINT pk_tasks PRIMARY KEY (festival_year, task_id),
    CONSTRAINT fk_tasks_vehicle FOREIGN KEY (festival_year, vehicle_id) REFERENCES vehicles (festival_year, vehicle_id)
);

CREATE INDEX idx_tasks_day ON tasks (festival_year, ((start_ts AT TIME ZONE 'Europe/Copenhagen')::date));
CREATE INDEX idx_tasks_status  ON tasks (festival_year, status);
CREATE INDEX idx_tasks_vehicle ON tasks (festival_year, vehicle_id);
CREATE INDEX idx_vehicles_no   ON vehicles (festival_year, vehicle_no);