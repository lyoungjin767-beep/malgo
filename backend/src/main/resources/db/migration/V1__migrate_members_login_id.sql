USE malgo;

CREATE TABLE IF NOT EXISTS members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(30) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS email_verifications;

DROP PROCEDURE IF EXISTS migrate_members_to_login_id;

DELIMITER //

CREATE PROCEDURE migrate_members_to_login_id()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'login_id'
    ) THEN
        ALTER TABLE members ADD COLUMN login_id VARCHAR(30) NULL AFTER id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'username'
    ) THEN
        UPDATE members
        SET login_id = username
        WHERE (login_id IS NULL OR login_id = '')
          AND username IS NOT NULL
          AND username <> '';
    END IF;

    UPDATE members
    SET login_id = CONCAT('user', id)
    WHERE login_id IS NULL
       OR login_id = '';

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND INDEX_NAME = 'uk_members_login_id'
    ) THEN
        ALTER TABLE members ADD CONSTRAINT uk_members_login_id UNIQUE (login_id);
    END IF;

    ALTER TABLE members MODIFY COLUMN login_id VARCHAR(30) NOT NULL;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'username'
    ) THEN
        ALTER TABLE members DROP COLUMN username;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'email'
    ) THEN
        ALTER TABLE members DROP COLUMN email;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'name'
    ) THEN
        ALTER TABLE members DROP COLUMN name;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'email_verified'
    ) THEN
        ALTER TABLE members DROP COLUMN email_verified;
    END IF;
END //

DELIMITER ;

CALL migrate_members_to_login_id();

DROP PROCEDURE IF EXISTS migrate_members_to_login_id;
