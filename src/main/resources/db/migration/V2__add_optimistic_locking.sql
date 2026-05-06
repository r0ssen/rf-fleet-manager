-- V2__add_optimistic_locking.sql
ALTER TABLE tasks    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE vehicles ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
