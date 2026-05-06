-- V3__opening_hours.sql
-- Stores service opening hours per calendar date.
-- Replaces the hardcoded InsertTime() logic from the Delphi app.

CREATE TABLE opening_hours (
    festival_year   SMALLINT    NOT NULL,
    festival_date   DATE        NOT NULL,
    open_from       TIME        NOT NULL DEFAULT '08:00',
    open_until      TIME        NOT NULL DEFAULT '22:00',
    CONSTRAINT pk_opening_hours PRIMARY KEY (festival_year, festival_date)
);

COMMENT ON TABLE opening_hours IS
    'Service opening hours per day. Drives the fleet grid row range.';

-- Seed 2025 festival opening hours (from Delphi InsertTime logic)
-- Setup week (21-25 Jun): 09:00-19:00
INSERT INTO opening_hours (festival_year, festival_date, open_from, open_until)
SELECT 2025, d::DATE, '09:00', '19:00'
FROM generate_series('2025-06-21'::date, '2025-06-25'::date, '1 day') d;

-- Main festival week (26 Jun - 2 Jul): 08:00-22:00
INSERT INTO opening_hours (festival_year, festival_date, open_from, open_until)
SELECT 2025, d::DATE, '08:00', '22:00'
FROM generate_series('2025-06-26'::date, '2025-07-02'::date, '1 day') d;

-- Wind-down day (3 Jul): 08:00-19:00
INSERT INTO opening_hours VALUES (2025, '2025-07-03', '08:00', '19:00');

-- Teardown (4-6 Jul): 09:00-18:00
INSERT INTO opening_hours (festival_year, festival_date, open_from, open_until)
SELECT 2025, d::DATE, '09:00', '18:00'
FROM generate_series('2025-07-04'::date, '2025-07-06'::date, '1 day') d;

-- Late teardown (7-9 Jul): 09:00-19:00
INSERT INTO opening_hours (festival_year, festival_date, open_from, open_until)
SELECT 2025, d::DATE, '09:00', '19:00'
FROM generate_series('2025-07-07'::date, '2025-07-09'::date, '1 day') d;
