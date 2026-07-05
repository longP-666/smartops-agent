CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    enabled TINYINT NOT NULL DEFAULT 1,
    create_user_id BIGINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    knowledge_base_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(32),
    category VARCHAR(64),
    parse_status VARCHAR(32) NOT NULL,
    chunk_count INT NOT NULL DEFAULT 0,
    create_user_id BIGINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_kb_id (knowledge_base_id)
);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding MEDIUMTEXT NOT NULL,
    metadata JSON,
    create_time DATETIME NOT NULL,
    INDEX idx_document_id (document_id),
    INDEX idx_kb_id (knowledge_base_id)
);

CREATE TABLE IF NOT EXISTS agent_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    title VARCHAR(128),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS agent_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_conversation_id (conversation_id)
);

CREATE TABLE IF NOT EXISTS agent_conversation_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL UNIQUE,
    summary TEXT NOT NULL,
    message_count INT NOT NULL,
    update_time DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_no VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(128),
    priority INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    assignee_name VARCHAR(64),
    creator_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_ticket_no (ticket_no),
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS ticket_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_ticket_no (ticket_no)
);

CREATE TABLE IF NOT EXISTS agent_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL UNIQUE,
    conversation_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    final_answer TEXT,
    status VARCHAR(32) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    total_cost_ms BIGINT,
    create_time DATETIME NOT NULL,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS agent_step_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    step_type VARCHAR(64) NOT NULL,
    input TEXT,
    output TEXT,
    status VARCHAR(32) NOT NULL,
    cost_ms BIGINT,
    error_message TEXT,
    create_time DATETIME NOT NULL,
    INDEX idx_trace_id (trace_id)
);

CREATE TABLE IF NOT EXISTS tool_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64),
    tool_name VARCHAR(64) NOT NULL,
    request_body TEXT,
    response_body TEXT,
    status VARCHAR(32) NOT NULL,
    cost_ms BIGINT,
    error_message TEXT,
    create_time DATETIME NOT NULL,
    INDEX idx_tool_trace (trace_id)
);

INSERT INTO knowledge_base (id, name, description, enabled, create_user_id, create_time, update_time)
SELECT 1, '默认运维知识库', '账号、订单、退款、系统故障与售后处理 SOP', 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE id = 1);

INSERT INTO ticket_category (code, name, description)
SELECT 'ACCOUNT_LOGIN', '账号登录', '密码错误、验证码、账号冻结、权限异常'
WHERE NOT EXISTS (SELECT 1 FROM ticket_category WHERE code = 'ACCOUNT_LOGIN');

INSERT INTO ticket_category (code, name, description)
SELECT 'REFUND', '退款售后', '退款失败、到账延迟、订单状态不一致'
WHERE NOT EXISTS (SELECT 1 FROM ticket_category WHERE code = 'REFUND');

INSERT INTO ticket_category (code, name, description)
SELECT 'SYSTEM_FAULT', '系统故障', '接口报错、页面不可用、性能异常'
WHERE NOT EXISTS (SELECT 1 FROM ticket_category WHERE code = 'SYSTEM_FAULT');

INSERT INTO ticket_category (code, name, description)
SELECT 'ORDER', '订单问题', '订单创建、支付状态、履约状态异常'
WHERE NOT EXISTS (SELECT 1 FROM ticket_category WHERE code = 'ORDER');
