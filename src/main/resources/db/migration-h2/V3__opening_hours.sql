-- V3__opening_hours.sql (H2)
CREATE TABLE opening_hours (
    festival_year   SMALLINT    NOT NULL,
    festival_date   DATE        NOT NULL,
    open_from       TIME        NOT NULL DEFAULT '08:00',
    open_until      TIME        NOT NULL DEFAULT '22:00',
    CONSTRAINT pk_opening_hours PRIMARY KEY (festival_year, festival_date)
);

INSERT INTO opening_hours VALUES (2026, '2026-05-03', '09:00', '19:00');
-- Seed 2026 — setup week 09:00-19:00
INSERT INTO opening_hours VALUES (2026, '2026-06-21', '09:00', '19:00');
INSERT INTO opening_hours VALUES (2026, '2026-06-22', '09:00', '19:00');
INSERT INTO opening_hours VALUES (2026, '2026-06-23', '09:00', '19:00');
INSERT INTO opening_hours VALUES (2026, '2026-06-24', '09:00', '19:00');
INSERT INTO opening_hours VALUES (2026, '2026-06-25', '09:00', '19:00');
-- Main festival week 08:00-22:00
INSERT INTO opening_hours VALUES (2026, '2026-06-26', '08:00', '22:00');
INSERT INTO opening_hours VALUES (2026, '2026-06-27', '08:00', '22:00');
INSERT INTO opening_hours VALUES (2026, '2026-06-28', '08:00', '22:00');
INSERT INTO opening_hours VALUES (2026, '2026-06-29', '08:00', '22:00');
INSERT INTO opening_hours VALUES (2026, '2026-06-30', '08:00', '22:00');
INSERT INTO opening_hours VALUES (2026, '2026-07-01', '08:00', '22:00');
INSERT INTO opening_hours VALUES (2026, '2026-07-02', '08:00', '22:00');
-- Wind-down 08:00-19:00
INSERT INTO opening_hours VALUES (2026, '2026-07-03', '08:00', '19:00');
-- Teardown 09:00-18:00
INSERT INTO opening_hours VALUES (2026, '2026-07-04', '09:00', '18:00');
INSERT INTO opening_hours VALUES (2026, '2026-07-05', '09:00', '18:00');
INSERT INTO opening_hours VALUES (2026, '2026-07-06', '09:00', '18:00');
-- Late teardown 09:00-19:00
INSERT INTO opening_hours VALUES (2026, '2026-07-07', '09:00', '19:00');
INSERT INTO opening_hours VALUES (2026, '2026-07-08', '09:00', '19:00');
INSERT INTO opening_hours VALUES (2026, '2026-07-09', '09:00', '19:00');
