-- V4__vehicle_availability.sql  (H2)
CREATE TABLE vehicle_availability (
    festival_year SMALLINT NOT NULL,
    vehicle_id    INT      NOT NULL,
    festival_date DATE     NOT NULL,
    CONSTRAINT pk_vehicle_availability PRIMARY KEY (festival_year, vehicle_id, festival_date),
    CONSTRAINT fk_va_vehicle FOREIGN KEY (festival_year, vehicle_id)
        REFERENCES vehicles (festival_year, vehicle_id) ON DELETE CASCADE,
    CONSTRAINT fk_va_opening_hours FOREIGN KEY (festival_year, festival_date)
        REFERENCES opening_hours (festival_year, festival_date) ON DELETE CASCADE
);

