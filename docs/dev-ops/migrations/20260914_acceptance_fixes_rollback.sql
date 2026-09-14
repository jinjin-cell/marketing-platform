-- Run only after restoring/consulting the pre-deployment backup.
-- user_account is intentionally retained so registered users are never silently deleted.
START TRANSACTION;

UPDATE rule_tree_node_line
SET rule_node_to = 'rule_luck_award', update_time = NOW()
WHERE tree_id = 'tree_lock_3'
  AND rule_node_from = 'rule_lock'
  AND rule_limit_value = 'ALLOW';

UPDATE award SET award_key = 'openai_use_count', award_config = '5', award_desc = 'OpenAI 增加使用次数' WHERE award_id = 102;
UPDATE award SET award_key = 'openai_use_count', award_config = '10', award_desc = 'OpenAI 增加使用次数' WHERE award_id = 103;
UPDATE award SET award_key = 'openai_use_count', award_config = '20', award_desc = 'OpenAI 增加使用次数' WHERE award_id = 104;
UPDATE award SET award_key = 'openai_model', award_config = 'gpt-4', award_desc = 'OpenAI 增加模型' WHERE award_id = 105;
UPDATE award SET award_key = 'openai_model', award_config = 'dall-e-2', award_desc = 'OpenAI 增加模型' WHERE award_id = 106;
UPDATE award SET award_key = 'openai_model', award_config = 'dall-e-3', award_desc = 'OpenAI 增加模型' WHERE award_id = 107;
UPDATE award SET award_key = 'openai_use_count', award_config = '100', award_desc = 'OpenAI 增加使用次数' WHERE award_id = 108;

-- Restore raffle_activity_count from the backup because its pre-deployment value is environment-specific.
COMMIT;
