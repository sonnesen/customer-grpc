CREATE TABLE customers (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL UNIQUE,
    phone     VARCHAR(50)  NOT NULL,
    street    VARCHAR(255) NOT NULL,
    city      VARCHAR(255) NOT NULL,
    state     VARCHAR(100) NOT NULL,
    zip_code  VARCHAR(20)  NOT NULL,
    status    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);
