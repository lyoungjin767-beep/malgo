DROP PROCEDURE IF EXISTS add_target_language_to_user_customizations;

DELIMITER //

CREATE PROCEDURE add_target_language_to_user_customizations()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_customizations'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'user_customizations'
              AND COLUMN_NAME = 'target_language'
        ) THEN
            ALTER TABLE user_customizations
                ADD COLUMN target_language VARCHAR(10) NULL AFTER expression;
        END IF;

        UPDATE user_customizations
        SET target_language = 'EN'
        WHERE target_language IS NULL
           OR target_language = '';

        ALTER TABLE user_customizations
            MODIFY COLUMN target_language VARCHAR(10) NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_customization_speech_styles'
    ) THEN
        UPDATE user_customization_speech_styles
        SET speech_style = 'WARM'
        WHERE speech_style = 'AFFECTIONATE';

        UPDATE user_customization_speech_styles
        SET speech_style = 'SINCERE'
        WHERE speech_style = 'DIRECT';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_partners'
    ) THEN
        UPDATE ai_partners
        SET speech_style = 'FRIENDLY'
        WHERE name = 'kash'
          AND speech_style = 'CASUAL';
    END IF;
END //

DELIMITER ;

CALL add_target_language_to_user_customizations();

DROP PROCEDURE IF EXISTS add_target_language_to_user_customizations;
