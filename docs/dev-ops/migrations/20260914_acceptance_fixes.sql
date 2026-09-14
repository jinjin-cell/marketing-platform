-- Acceptance fixes. Run against the big_market configuration database only after a full backup.
START TRANSACTION;

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_name VARCHAR(32) NOT NULL,
    user_id VARCHAR(32) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    account_status VARCHAR(16) NOT NULL DEFAULT 'open',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_account_name (account_name),
    UNIQUE KEY uq_user_account_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A successful 3-draw unlock must continue to stock deduction before awarding.
UPDATE rule_tree_node_line
SET rule_node_to = 'rule_stock', update_time = NOW()
WHERE tree_id = 'tree_lock_3'
  AND rule_node_from = 'rule_lock'
  AND rule_limit_value = 'ALLOW';

-- Only random credit is automatically fulfilled. Other configured prizes require manual fulfillment.
UPDATE award SET award_key = 'manual_fulfillment', award_config = 'OpenAI会员卡', award_desc = 'OpenAI会员卡'
WHERE award_id = 102;
UPDATE award SET award_key = 'manual_fulfillment', award_config = '支付优惠券', award_desc = '支付优惠券'
WHERE award_id = 103;
UPDATE award SET award_key = 'manual_fulfillment', award_config = '小米台灯', award_desc = '小米台灯'
WHERE award_id = 104;
UPDATE award SET award_key = 'manual_fulfillment', award_config = '小米SU7周体验', award_desc = '小米SU7周体验'
WHERE award_id = 105;
UPDATE award SET award_key = 'manual_fulfillment', award_config = '轻奢办公椅', award_desc = '轻奢办公椅'
WHERE award_id = 106;
UPDATE award SET award_key = 'manual_fulfillment', award_config = '小霸王游戏机', award_desc = '小霸王游戏机'
WHERE award_id = 107;
UPDATE award SET award_key = 'manual_fulfillment', award_config = '暴走玩偶', award_desc = '暴走玩偶'
WHERE award_id = 108;

-- SKU 9011 historically means one draw. Keep the existing SKU semantics stable.
UPDATE raffle_activity_count
SET total_count = 1, day_count = 1, month_count = 1, update_time = NOW()
WHERE activity_count_id = (
    SELECT activity_count_id FROM raffle_activity_sku WHERE sku = 9011
);

COMMIT;
