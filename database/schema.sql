-- ============================================================
-- SmartWaste Management System — PostgreSQL Schema
-- Compatible with Supabase
-- ============================================================

-- Enable UUID extension (optional, we use BIGSERIAL for simplicity)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id               BIGSERIAL PRIMARY KEY,
    first_name       VARCHAR(50)  NOT NULL,
    last_name        VARCHAR(50)  NOT NULL,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password_hash    TEXT         NOT NULL,
    phone_number     VARCHAR(15)  UNIQUE,
    role             VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','CITIZEN','WORKER','RECYCLER')),
    address          TEXT,
    city             VARCHAR(100),
    pincode          VARCHAR(10),
    reward_points    INTEGER      NOT NULL DEFAULT 0,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email  ON users(email);
CREATE INDEX idx_users_role   ON users(role);
CREATE INDEX idx_users_pincode ON users(pincode);

-- ============================================================
-- COMPLAINTS
-- ============================================================
CREATE TABLE IF NOT EXISTS complaints (
    id                      BIGSERIAL PRIMARY KEY,
    citizen_id              BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_worker_id      BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    title                   VARCHAR(150) NOT NULL,
    description             TEXT,
    address                 TEXT         NOT NULL,
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    pincode                 VARCHAR(10),
    waste_type              VARCHAR(30)  CHECK (waste_type IN ('ORGANIC','PLASTIC','ELECTRONIC','HAZARDOUS','CONSTRUCTION','MIXED')),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING','ASSIGNED','IN_PROGRESS','RESOLVED','REJECTED')),
    priority                VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM'
                                CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    image_url               TEXT,
    reward_points_awarded   INTEGER      NOT NULL DEFAULT 0,
    resolved_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_complaints_citizen    ON complaints(citizen_id);
CREATE INDEX idx_complaints_worker     ON complaints(assigned_worker_id);
CREATE INDEX idx_complaints_status     ON complaints(status);
CREATE INDEX idx_complaints_pincode    ON complaints(pincode);
CREATE INDEX idx_complaints_created_at ON complaints(created_at DESC);

-- ============================================================
-- COLLECTION ROUTES
-- ============================================================
CREATE TABLE IF NOT EXISTS collection_routes (
    id                      BIGSERIAL PRIMARY KEY,
    worker_id               BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    route_name              VARCHAR(100) NOT NULL,
    scheduled_date          DATE         NOT NULL,
    area_name               VARCHAR(100),
    pincode                 VARCHAR(10),
    stops                   TEXT,           -- JSON array of {lat, lng, address}
    estimated_distance_km   DOUBLE PRECISION,
    estimated_duration_min  INTEGER,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED'
                                CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routes_worker         ON collection_routes(worker_id);
CREATE INDEX idx_routes_scheduled_date ON collection_routes(scheduled_date);
CREATE INDEX idx_routes_status         ON collection_routes(status);

-- ============================================================
-- AUTO-UPDATE updated_at TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_complaints_updated_at
    BEFORE UPDATE ON complaints
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_routes_updated_at
    BEFORE UPDATE ON collection_routes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- SEED DATA — default admin user
-- Password: Admin@123 (BCrypt hash)
-- ============================================================
INSERT INTO users (first_name, last_name, email, password_hash, role, is_active, is_email_verified)
VALUES (
    'System',
    'Admin',
    'admin@smartwaste.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    TRUE,
    TRUE
) ON CONFLICT (email) DO NOTHING;
