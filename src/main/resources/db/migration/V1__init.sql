-- =============================================================
-- TOEIC Flashcard App — PostgreSQL Schema
-- =============================================================
-- Thứ tự tạo bảng: users → topics → flashcards → user_progress
--                  → quiz_attempts → quiz_options → quiz_answers
--                  → audit_logs
-- =============================================================

-- Extension để dùng gen_random_uuid() nếu cần sau này
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -------------------------------------------------------------
-- 1. USERS
-- -------------------------------------------------------------
CREATE TABLE users (
                       id            BIGSERIAL       PRIMARY KEY,
                       username      VARCHAR(50)     NOT NULL UNIQUE,
                       email         VARCHAR(255)    NOT NULL UNIQUE,
                       password_hash VARCHAR(255)    NOT NULL,
                       role          VARCHAR(20)     NOT NULL DEFAULT 'USER'
                           CHECK (role IN ('USER', 'ADMIN')),
                       avatar_url    VARCHAR(500),
                       is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       updated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- -------------------------------------------------------------
-- 2. TOPICS
-- -------------------------------------------------------------
CREATE TABLE topics (
                        id            BIGSERIAL       PRIMARY KEY,
                        name          VARCHAR(100)    NOT NULL,
                        description   TEXT,
                        icon_url      VARCHAR(500),
                        display_order INT             NOT NULL DEFAULT 0,
                        is_system     BOOLEAN         NOT NULL DEFAULT FALSE,
    -- TRUE  = ADMIN tạo, tất cả user thấy
    -- FALSE = user tạo, chỉ mình thấy
                        created_by    BIGINT          NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                        created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                        updated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- -------------------------------------------------------------
-- 3. FLASHCARDS
-- -------------------------------------------------------------
CREATE TABLE flashcards (
                            id               BIGSERIAL       PRIMARY KEY,
                            topic_id         BIGINT          NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
                            created_by       BIGINT          NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                            word             VARCHAR(200)    NOT NULL,
                            pronunciation    VARCHAR(200),
                            definition       TEXT            NOT NULL,
                            example_sentence TEXT,
                            image_url        VARCHAR(500),   -- Cloudinary secure_url
                            image_public_id  VARCHAR(200),   -- Cloudinary public_id (dùng để xóa ảnh)
                            difficulty       VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM'
                                CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
                            is_active        BOOLEAN         NOT NULL DEFAULT TRUE,
                            created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                            updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- -------------------------------------------------------------
-- 4. USER_PROGRESS  (SM-2 Spaced Repetition)
-- -------------------------------------------------------------
CREATE TABLE user_progress (
                               id               BIGSERIAL       PRIMARY KEY,
                               user_id          BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               flashcard_id     BIGINT          NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
                               status           VARCHAR(20)     NOT NULL DEFAULT 'NEW'
                                   CHECK (status IN ('NEW', 'LEARNING', 'REVIEWING', 'MASTERED')),
                               review_count     INT             NOT NULL DEFAULT 0,
                               correct_count    INT             NOT NULL DEFAULT 0,
    -- SM-2 fields
                               easiness_factor  NUMERIC(4,2)    NOT NULL DEFAULT 2.50, -- min 1.3
                               interval_days    INT             NOT NULL DEFAULT 1,
                               sm2_repetitions  INT             NOT NULL DEFAULT 0,
                               last_reviewed_at TIMESTAMPTZ,
                               next_review_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                               UNIQUE (user_id, flashcard_id)   -- mỗi user chỉ có 1 progress record per flashcard
);

-- -------------------------------------------------------------
-- 5. QUIZ_ATTEMPTS
-- -------------------------------------------------------------
CREATE TABLE quiz_attempts (
                               id               BIGSERIAL       PRIMARY KEY,
                               user_id          BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               topic_id         BIGINT          REFERENCES topics(id) ON DELETE SET NULL,
                               quiz_type        VARCHAR(30)     NOT NULL DEFAULT 'MULTIPLE_CHOICE'
                                   CHECK (quiz_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'FILL_IN')),
                               total_questions  INT             NOT NULL DEFAULT 0,
                               correct_answers  INT             NOT NULL DEFAULT 0,
                               score            INT             NOT NULL DEFAULT 0,  -- 0-100
                               duration_seconds INT,
                               started_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                               finished_at      TIMESTAMPTZ
);

-- -------------------------------------------------------------
-- 6. QUIZ_OPTIONS  (các đáp án cho mỗi flashcard)
-- -------------------------------------------------------------
CREATE TABLE quiz_options (
                              id            BIGSERIAL       PRIMARY KEY,
                              flashcard_id  BIGINT          NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
                              option_text   VARCHAR(500)    NOT NULL,
                              is_correct    BOOLEAN         NOT NULL DEFAULT FALSE
);

-- -------------------------------------------------------------
-- 7. QUIZ_ANSWERS  (chi tiết từng câu trả lời trong 1 attempt)
-- -------------------------------------------------------------
CREATE TABLE quiz_answers (
                              id                 BIGSERIAL       PRIMARY KEY,
                              attempt_id         BIGINT          NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
                              flashcard_id       BIGINT          NOT NULL REFERENCES flashcards(id) ON DELETE RESTRICT,
                              selected_answer    VARCHAR(500),   -- nullable nếu user bỏ qua
                              is_correct         BOOLEAN         NOT NULL DEFAULT FALSE,
                              time_spent_seconds INT
);

-- -------------------------------------------------------------
-- 8. AUDIT_LOGS  (ghi bởi AuditAspect — không ghi tay)
-- -------------------------------------------------------------
CREATE TABLE audit_logs (
                            id           BIGSERIAL       PRIMARY KEY,
                            user_id      BIGINT          REFERENCES users(id) ON DELETE SET NULL, -- nullable
                            action       VARCHAR(20)     NOT NULL
                                CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
    entity_type  VARCHAR(50)     NOT NULL, -- 'FLASHCARD', 'TOPIC', 'USER'
    entity_id    BIGINT          NOT NULL,
    old_value    TEXT,           -- JSON snapshot trước thay đổi
    new_value    TEXT,           -- JSON snapshot sau thay đổi
    ip_address   VARCHAR(45),    -- IPv4 hoặc IPv6
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- =============================================================
-- INDEXES
-- =============================================================

-- topics: hay query theo is_system và created_by
CREATE INDEX idx_topics_is_system     ON topics(is_system);
CREATE INDEX idx_topics_created_by    ON topics(created_by);

-- flashcards: hay filter theo topic, created_by, is_active
CREATE INDEX idx_flashcards_topic_id   ON flashcards(topic_id);
CREATE INDEX idx_flashcards_created_by ON flashcards(created_by);
CREATE INDEX idx_flashcards_is_active  ON flashcards(is_active);

-- user_progress: hay query "card nào cần ôn hôm nay" (SM-2)
CREATE INDEX idx_progress_user_id        ON user_progress(user_id);
CREATE INDEX idx_progress_next_review    ON user_progress(user_id, next_review_at);
CREATE INDEX idx_progress_status         ON user_progress(user_id, status);

-- quiz_attempts: lịch sử quiz theo user
CREATE INDEX idx_quiz_attempts_user_id   ON quiz_attempts(user_id);
CREATE INDEX idx_quiz_answers_attempt_id ON quiz_answers(attempt_id);

-- audit_logs: tra cứu theo entity hoặc user
CREATE INDEX idx_audit_entity  ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);

-- =============================================================
-- I18N — Đa ngôn ngữ
-- =============================================================
-- =============================================================
-- 9. SUPPORTED_LOCALES — danh sách ngôn ngữ hỗ trợ
-- =============================================================
CREATE TABLE supported_locales (
                                   code          VARCHAR(10)  PRIMARY KEY,  -- 'vi', 'en', 'ja', ...
                                   name          VARCHAR(50)  NOT NULL,
                                   native_name   VARCHAR(50)  NOT NULL,
                                   is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
                                   is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                                   display_order INT          NOT NULL DEFAULT 0,
                                   created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- chỉ 1 locale mặc định
CREATE UNIQUE INDEX idx_locales_default
    ON supported_locales(is_default)
    WHERE is_default = TRUE;

-- filter locale đang active
CREATE INDEX idx_locales_active
    ON supported_locales(is_active);

-- =============================================================
-- 10. FLASHCARD_TRANSLATIONS
-- =============================================================
CREATE TABLE flashcard_translations (
                                        id               BIGSERIAL      PRIMARY KEY,
                                        flashcard_id     BIGINT         NOT NULL
                                            REFERENCES flashcards(id) ON DELETE CASCADE,
                                        locale           VARCHAR(10)    NOT NULL
                                            REFERENCES supported_locales(code),
                                        definition       TEXT           NOT NULL,
                                        example_sentence TEXT,
                                        created_by       BIGINT
                                                                        REFERENCES users(id) ON DELETE SET NULL,
                                        created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                                        updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    -- mỗi flashcard chỉ có 1 bản dịch / locale
                                        CONSTRAINT uq_flashcard_locale UNIQUE (flashcard_id, locale)
);

-- index cho query theo locale (admin / filter)
CREATE INDEX idx_flashcard_trans_locale
    ON flashcard_translations(locale);

-- =============================================================
-- 11. TOPIC_TRANSLATIONS
-- =============================================================
CREATE TABLE topic_translations (
                                    id           BIGSERIAL      PRIMARY KEY,
                                    topic_id     BIGINT         NOT NULL
                                        REFERENCES topics(id) ON DELETE CASCADE,
                                    locale       VARCHAR(10)    NOT NULL
                                        REFERENCES supported_locales(code),
                                    name         VARCHAR(100)   NOT NULL,
                                    description  TEXT,
                                    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                                    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                                    CONSTRAINT uq_topic_locale UNIQUE (topic_id, locale)
);

-- index cho filter theo locale
CREATE INDEX idx_topic_trans_locale
    ON topic_translations(locale);

-- =============================================================
-- SEED DATA — LOCALES
-- =============================================================

-- Admin account (password: 'changeme' — đổi ngay sau khi setup)
INSERT INTO users (username, email, password_hash, role)
VALUES ('admin', 'admin@toeicapp.com',
        '$2a$12$placeholder_change_this_immediately', 'ADMIN');

-- Supported locales
INSERT INTO supported_locales (code, name, native_name, is_default, display_order)
VALUES
    ('en', 'English',    'English',    FALSE, 1),
    ('vi', 'Vietnamese', 'Tiếng Việt', TRUE,  2),
    ('ja', 'Japanese',   '日本語',       FALSE, 3),
    ('ko', 'Korean',     '한국어',       FALSE, 4);

-- System topics
INSERT INTO topics (name, description, display_order, is_system, created_by)
VALUES
    ('Vocabulary — Business',    'Từ vựng thương mại, văn phòng',        1, TRUE, 1),
    ('Vocabulary — Travel',      'Từ vựng du lịch, khách sạn, sân bay',  2, TRUE, 1),
    ('Vocabulary — Technology',  'Từ vựng công nghệ, IT',                3, TRUE, 1),
    ('Grammar — Tenses',         'Các thì trong tiếng Anh',              4, TRUE, 1),
    ('Grammar — Prepositions',   'Giới từ thường gặp trong TOEIC',       5, TRUE, 1),
    ('Phrasal Verbs',            'Cụm động từ phổ biến',                 6, TRUE, 1),
    ('TOEIC Part 5 — Practice',  'Luyện tập dạng bài điền vào chỗ trống', 7, TRUE, 1);