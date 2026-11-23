CREATE TABLE users (
    id            UUID                        NOT NULL PRIMARY KEY,
    email         VARCHAR(320)                NOT NULL,
    name          VARCHAR(120)                NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    active        BOOLEAN                     NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id UUID        NOT NULL,
    role    VARCHAR(32) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE refresh_tokens (
    token_hash VARCHAR(64)                 NOT NULL PRIMARY KEY,
    user_id    UUID                        NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
