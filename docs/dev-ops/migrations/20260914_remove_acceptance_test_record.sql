-- Remove the single acceptance-test draw accidentally written at 2026-09-14 05:16:48.
-- Preconditions are verified by the deployment procedure before this transaction is run.
START TRANSACTION;

DELETE FROM task
WHERE id = 267
  AND user_id = 'xiaofuge'
  AND topic = 'send.award'
  AND message LIKE '%676434601142%'
  AND create_time = '2026-09-14 05:16:48';
SELECT 'task_deleted' AS item, ROW_COUNT() AS affected_rows;

DELETE FROM user_award_record_000
WHERE id = 21
  AND user_id = 'xiaofuge'
  AND activity_id = 100301
  AND strategy_id = 100006
  AND order_id = '676434601142'
  AND award_id = 107
  AND create_time = '2026-09-14 05:16:48';
SELECT 'award_deleted' AS item, ROW_COUNT() AS affected_rows;

DELETE FROM user_raffle_order_000
WHERE id = 5
  AND user_id = 'xiaofuge'
  AND activity_id = 100301
  AND strategy_id = 100006
  AND order_id = '676434601142'
  AND create_time = '2026-09-14 05:16:48';
SELECT 'raffle_order_deleted' AS item, ROW_COUNT() AS affected_rows;

UPDATE raffle_activity_account
SET total_count_surplus = total_count_surplus + 1,
    update_time = NOW()
WHERE id = 3
  AND user_id = 'xiaofuge'
  AND activity_id = 100301;
SELECT 'total_account_restored' AS item, ROW_COUNT() AS affected_rows;

UPDATE raffle_activity_account_day
SET day_count_surplus = day_count_surplus + 1,
    update_time = NOW()
WHERE id = 19
  AND user_id = 'xiaofuge'
  AND activity_id = 100301
  AND day = '2026-09-14';
SELECT 'day_account_restored' AS item, ROW_COUNT() AS affected_rows;

UPDATE raffle_activity_account_month
SET month_count_surplus = month_count_surplus + 1,
    update_time = NOW()
WHERE id = 11
  AND user_id = 'xiaofuge'
  AND activity_id = 100301
  AND month = '2026-09';
SELECT 'month_account_restored' AS item, ROW_COUNT() AS affected_rows;

COMMIT;
