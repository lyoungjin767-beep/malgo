DROP PROCEDURE IF EXISTS add_target_language_to_ai_partner_and_conversation;

DELIMITER //

CREATE PROCEDURE add_target_language_to_ai_partner_and_conversation()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_partners'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ai_partners'
              AND COLUMN_NAME = 'target_language'
        ) THEN
            ALTER TABLE ai_partners
                ADD COLUMN target_language VARCHAR(10) NULL AFTER target_country;
        END IF;

        UPDATE ai_partners
        SET target_language = CASE
            WHEN target_country = 'JP' THEN 'JA'
            WHEN target_country IN ('CN', 'TW', 'HK') THEN 'ZH'
            WHEN target_country = 'VN' THEN 'VI'
            WHEN target_country IN ('ES', 'MX') THEN 'ES'
            WHEN target_country = 'DE' THEN 'DE'
            ELSE 'EN'
        END
        WHERE target_language IS NULL
           OR target_language = '';

        ALTER TABLE ai_partners
            MODIFY COLUMN target_language VARCHAR(10) NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'conversations'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'conversations'
              AND COLUMN_NAME = 'target_language'
        ) THEN
            ALTER TABLE conversations
                ADD COLUMN target_language VARCHAR(10) NULL AFTER target_country;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ai_partners'
        ) THEN
            UPDATE conversations c
            LEFT JOIN ai_partners p ON c.ai_partner_id = p.id
            SET c.target_language = COALESCE(
                p.target_language,
                CASE
                    WHEN c.target_country = 'JP' THEN 'JA'
                    WHEN c.target_country IN ('CN', 'TW', 'HK') THEN 'ZH'
                    WHEN c.target_country = 'VN' THEN 'VI'
                    WHEN c.target_country IN ('ES', 'MX') THEN 'ES'
                    WHEN c.target_country = 'DE' THEN 'DE'
                    ELSE 'EN'
                END
            )
            WHERE c.target_language IS NULL
               OR c.target_language = '';
        ELSE
            UPDATE conversations
            SET target_language = CASE
                WHEN target_country = 'JP' THEN 'JA'
                WHEN target_country IN ('CN', 'TW', 'HK') THEN 'ZH'
                WHEN target_country = 'VN' THEN 'VI'
                WHEN target_country IN ('ES', 'MX') THEN 'ES'
                WHEN target_country = 'DE' THEN 'DE'
                ELSE 'EN'
            END
            WHERE target_language IS NULL
               OR target_language = '';
        END IF;
    END IF;
END //

DELIMITER ;

CALL add_target_language_to_ai_partner_and_conversation();

DROP PROCEDURE IF EXISTS add_target_language_to_ai_partner_and_conversation;
