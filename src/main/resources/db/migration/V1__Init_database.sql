CREATE DATABASE file_storage;

\c file_storage;

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE files (
                       id BIGSERIAL PRIMARY KEY,
                       filename VARCHAR(255) NOT NULL,
                       size BIGINT NOT NULL,
                       content_type VARCHAR(255) NOT NULL,
                       user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tokens (
                        id BIGSERIAL PRIMARY KEY,
                        token VARCHAR(255) UNIQUE NOT NULL,
                        user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        expires_at TIMESTAMP NOT NULL,
                        active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_files_user_id ON files(user_id);
CREATE INDEX idx_tokens_token ON tokens(token);
CREATE INDEX idx_tokens_expires_at ON tokens(expires_at);
CREATE INDEX idx_users_username ON users(username);