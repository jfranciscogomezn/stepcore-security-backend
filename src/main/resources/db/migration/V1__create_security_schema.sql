CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE menu_options (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(100) NOT NULL UNIQUE,
    label      VARCHAR(150) NOT NULL,
    route      VARCHAR(200) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE role_menu_options (
    role_id        BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    menu_option_id BIGINT NOT NULL REFERENCES menu_options(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, menu_option_id)
);
