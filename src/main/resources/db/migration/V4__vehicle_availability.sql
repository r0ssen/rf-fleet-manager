-- V4__vehicle_availability.sql
-- Tracks which opening-hours days each vehicle is available.
-- No rows for a vehicle = available on all days (backwards compatible).
-- Any rows for a vehicle = available only on those specific dates.

CREATE TABLE vehicle_availability (
    festival_year INTEGER NOT NULL,
    vehicle_id    INT      NOT NULL,
    festival_date DATE     NOT NULL,
    CONSTRAINT pk_vehicle_availability PRIMARY KEY (festival_year, vehicle_id, festival_date),
    CONSTRAINT fk_va_vehicle FOREIGN KEY (festival_year, vehicle_id)
        REFERENCES vehicles (festival_year, vehicle_id) ON DELETE CASCADE,
    CONSTRAINT fk_va_opening_hours FOREIGN KEY (festival_year, festival_date)
        REFERENCES opening_hours (festival_year, festival_date) ON DELETE CASCADE
);

COMMENT ON TABLE vehicle_availability IS
    'Explicit availability per vehicle per day. Empty = available all days.';

