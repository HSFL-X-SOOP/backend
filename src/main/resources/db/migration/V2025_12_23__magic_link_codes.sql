CREATE TABLE marlin.magic_link_code (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES marlin."user"(id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_magic_link_code_code ON marlin.magic_link_code(code);
CREATE INDEX idx_magic_link_code_user_id ON marlin.magic_link_code(user_id);
CREATE INDEX idx_magic_link_code_created_at ON marlin.magic_link_code(created_at);
