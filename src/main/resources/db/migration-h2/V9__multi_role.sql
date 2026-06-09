CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL
);

INSERT INTO user_roles (user_id, role) SELECT id, role FROM users;

ALTER TABLE users DROP COLUMN role;
