DROP TABLE IF EXISTS email_verifications;

DROP PROCEDURE IF EXISTS migrate_members_to_username;

DELIMITER //

CREATE PROCEDURE migrate_members_to_username()
BEGIN
    CREATE TABLE IF NOT EXISTS members (
        id BIGINT NOT NULL AUTO_INCREMENT,
        username VARCHAR(30) NOT NULL,
        password VARCHAR(255) NOT NULL,
        created_at DATETIME(6) NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_members_username (username)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'username'
    ) THEN
        ALTER TABLE members ADD COLUMN username VARCHAR(30) NULL AFTER id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'login_id'
    ) THEN
        UPDATE members
        SET username = login_id
        WHERE (username IS NULL OR username = '')
          AND login_id IS NOT NULL
          AND login_id <> '';
    END IF;

    UPDATE members
    SET username = CONCAT('user', id)
    WHERE username IS NULL
       OR username = '';

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND INDEX_NAME = 'uk_members_username'
    ) THEN
        ALTER TABLE members ADD CONSTRAINT uk_members_username UNIQUE (username);
    END IF;

    ALTER TABLE members MODIFY COLUMN username VARCHAR(30) NOT NULL;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'members'
          AND COLUMN_NAME = 'login_id'
    ) THEN
        ALTER TABLE members DROP COLUMN login_id;
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

CALL migrate_members_to_username();

DROP PROCEDURE IF EXISTS migrate_members_to_username;
