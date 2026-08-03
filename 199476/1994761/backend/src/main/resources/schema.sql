CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(190) PRIMARY KEY,
    name VARCHAR(60) NOT NULL DEFAULT '',
    password_hash VARCHAR(100) NOT NULL,
    invite_code VARCHAR(20) NOT NULL UNIQUE,
    used_invite_code VARCHAR(20) NOT NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_sessions (
    token VARCHAR(64) PRIMARY KEY,
    user_email VARCHAR(190) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_session_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE,
    INDEX idx_session_user (user_email),
    INDEX idx_session_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ideas (
    id VARCHAR(64) PRIMARY KEY,
    owner_email VARCHAR(190) NOT NULL,
    type VARCHAR(20) NOT NULL,
    parent_id VARCHAR(64) NULL,
    text VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    level_value INT NULL,
    fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    decision VARCHAR(20) NULL,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NULL,
    reviewed_at DATETIME(3) NULL,
    CONSTRAINT fk_idea_owner FOREIGN KEY (owner_email) REFERENCES users(email) ON DELETE CASCADE,
    CONSTRAINT fk_idea_parent FOREIGN KEY (parent_id) REFERENCES ideas(id) ON DELETE SET NULL,
    INDEX idx_idea_owner (owner_email),
    INDEX idx_idea_public (is_public, type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS idea_likes (
    idea_id VARCHAR(64) NOT NULL,
    user_email VARCHAR(190) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (idea_id, user_email),
    CONSTRAINT fk_like_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_like_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(64) PRIMARY KEY,
    user_email VARCHAR(190) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(80) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(40) NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_product_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS team_applications (
    user_email VARCHAR(190) PRIMARY KEY,
    skill VARCHAR(120) NOT NULL,
    intro VARCHAR(1000) NULL,
    available_time VARCHAR(120) NOT NULL,
    resume_id VARCHAR(64) NULL,
    resume_name VARCHAR(255) NULL,
    resume_size BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NULL,
    CONSTRAINT fk_team_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(64) PRIMARY KEY,
    user_email VARCHAR(190) NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    business_type VARCHAR(60) NULL,
    order_id VARCHAR(64) NULL,
    deposit_id VARCHAR(64) NULL,
    pay_type VARCHAR(20) NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_tx_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE,
    INDEX idx_tx_user (user_email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS package_orders (
    id VARCHAR(64) PRIMARY KEY,
    user_email VARCHAR(190) NOT NULL,
    package_id VARCHAR(30) NOT NULL,
    package_name VARCHAR(80) NOT NULL,
    level_range VARCHAR(40) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    pay_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    duration_days INT NOT NULL,
    project_quota INT NOT NULL,
    iteration_quota INT NOT NULL,
    benefits JSON NULL,
    created_at DATETIME(3) NOT NULL,
    activated_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_order_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS active_packages (
    user_email VARCHAR(190) PRIMARY KEY,
    package_id VARCHAR(30) NOT NULL,
    package_name VARCHAR(80) NOT NULL,
    level_range VARCHAR(40) NOT NULL,
    started_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    project_quota INT NOT NULL,
    iteration_quota INT NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    CONSTRAINT fk_active_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE,
    CONSTRAINT fk_active_order FOREIGN KEY (order_id) REFERENCES package_orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(64) PRIMARY KEY,
    user_email VARCHAR(190) NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    link VARCHAR(255) NOT NULL,
    business_id VARCHAR(64) NULL,
    dedupe_key VARCHAR(190) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_notice_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE,
    UNIQUE KEY uk_notice_dedupe (user_email, dedupe_key),
    INDEX idx_notice_user (user_email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS feedbacks (
    id VARCHAR(64) PRIMARY KEY,
    user_email VARCHAR(190) NULL,
    page VARCHAR(120) NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE SET NULL,
    INDEX idx_feedback_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS feedback_messages (
    id VARCHAR(64) PRIMARY KEY,
    feedback_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    email VARCHAR(190) NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_message_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks(id) ON DELETE CASCADE,
    INDEX idx_message_feedback (feedback_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    action VARCHAR(120) NOT NULL,
    detail VARCHAR(1000) NOT NULL,
    actor VARCHAR(190) NOT NULL,
    target_id VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL,
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cooperation_deposits (
    id VARCHAR(64) PRIMARY KEY,
    user_email VARCHAR(190) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    refunded_at DATETIME(3) NULL,
    CONSTRAINT fk_deposit_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE,
    INDEX idx_deposit_user (user_email, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stored_files (
    id VARCHAR(64) PRIMARY KEY,
    user_email VARCHAR(190) NOT NULL,
    kind VARCHAR(30) NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content LONGBLOB NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_file_user FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
