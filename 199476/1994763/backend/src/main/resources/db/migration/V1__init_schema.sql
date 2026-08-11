CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(20) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    nickname VARCHAR(40) NULL,
    avatar_url VARCHAR(500) NULL,
    accepting_inquiries BOOLEAN NOT NULL DEFAULT TRUE,
    answerer_status VARCHAR(30) NOT NULL DEFAULT 'NOT_APPLIED',
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE auth_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE certifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    certification_type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    years INT NULL,
    required_item BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500) NULL,
    submitted_at DATETIME(6) NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_certification_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_certification_user_category (user_id, category),
    INDEX idx_certification_search (status, certification_type, title)
);

CREATE TABLE certification_materials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    certification_id BIGINT NOT NULL,
    material_kind VARCHAR(30) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_material_certification FOREIGN KEY (certification_id) REFERENCES certifications(id) ON DELETE CASCADE
);

CREATE TABLE wallet_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    available_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    frozen_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total_withdrawn DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE inquiries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    questioner_id BIGINT NOT NULL,
    answerer_id BIGINT NOT NULL,
    topic VARCHAR(120) NULL,
    source_type VARCHAR(30) NOT NULL DEFAULT 'PROFILE',
    question VARCHAR(1000) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    funds_status VARCHAR(30) NOT NULL,
    response_deadline DATETIME(6) NULL,
    confirmation_deadline DATETIME(6) NULL,
    accepted_at DATETIME(6) NULL,
    ended_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inquiry_questioner FOREIGN KEY (questioner_id) REFERENCES users(id),
    CONSTRAINT fk_inquiry_answerer FOREIGN KEY (answerer_id) REFERENCES users(id),
    INDEX idx_inquiry_questioner (questioner_id, created_at),
    INDEX idx_inquiry_answerer (answerer_id, created_at),
    INDEX idx_inquiry_timeout (status, response_deadline, confirmation_deadline)
);

CREATE TABLE inquiry_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    content TEXT NOT NULL,
    attachment_url VARCHAR(500) NULL,
    attachment_name VARCHAR(255) NULL,
    attachment_size BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_message_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    INDEX idx_message_inquiry_time (inquiry_id, created_at)
);

CREATE TABLE wallet_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    transaction_type VARCHAR(40) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    available_after DECIMAL(14, 2) NOT NULL,
    frozen_after DECIMAL(14, 2) NOT NULL,
    reference_type VARCHAR(40) NULL,
    reference_id BIGINT NULL,
    description VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_transaction_user_time (user_id, created_at)
);

CREATE TABLE bank_cards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    holder_name VARCHAR(80) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    card_number_ciphertext VARCHAR(512) NOT NULL,
    card_last_four CHAR(4) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_bank_card_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE recharges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    channel VARCHAR(30) NOT NULL DEFAULT 'ALIPAY',
    amount DECIMAL(14, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    paid_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_recharge_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE withdrawals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bank_card_id BIGINT NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    fee DECIMAL(14, 2) NOT NULL,
    arrival_amount DECIMAL(14, 2) NOT NULL,
    bank_name_snapshot VARCHAR(100) NOT NULL,
    card_last_four_snapshot CHAR(4) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    CONSTRAINT fk_withdrawal_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_withdrawal_bank_card FOREIGN KEY (bank_card_id) REFERENCES bank_cards(id)
);

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500) NOT NULL,
    target_path VARCHAR(255) NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_notification_user_read (user_id, read_flag, created_at)
);

CREATE TABLE feedback_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_user_id BIGINT NULL,
    feedback_type VARCHAR(30) NOT NULL,
    category VARCHAR(80) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_feedback_target_user FOREIGN KEY (target_user_id) REFERENCES users(id)
);

CREATE TABLE business_cooperations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    contact VARCHAR(120) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_business_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE customer_service_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_customer_service_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_customer_service_user_time (user_id, created_at)
);
