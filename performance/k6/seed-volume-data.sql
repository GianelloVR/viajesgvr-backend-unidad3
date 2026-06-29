DROP PROCEDURE IF EXISTS seed_k6_volume;

DELIMITER //

CREATE PROCEDURE seed_k6_volume(IN target_total INT)
BEGIN
  DECLARE current_paid INT DEFAULT 0;
  DECLARE rows_to_insert INT DEFAULT 0;

  SELECT COUNT(*) INTO current_paid FROM payments;

  SET rows_to_insert = GREATEST(target_total - current_paid, 0);

  DROP TEMPORARY TABLE IF EXISTS tmp_k6_numbers;

  CREATE TEMPORARY TABLE tmp_k6_numbers (
    n INT PRIMARY KEY
  );

  INSERT INTO tmp_k6_numbers (n)
  SELECT generated_number
  FROM (
    SELECT 
      ones.n
      + tens.n * 10
      + hundreds.n * 100
      + thousands.n * 1000
      + 1 AS generated_number
    FROM
      (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
       UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
    CROSS JOIN
      (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
       UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
    CROSS JOIN
      (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
       UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
    CROSS JOIN
      (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
       UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands
  ) numbers
  WHERE generated_number <= rows_to_insert;

  INSERT INTO reservations (
    discount_amount,
    discount_description,
    final_total_amount,
    original_total_amount,
    passenger_count,
    payment_deadline,
    purchase_group_code,
    reservation_date,
    special_requests,
    status,
    tour_end_date,
    tour_start_date,
    tour_package_id,
    user_id
  )
  SELECT
    50000,
    'Descuento generado para pruebas de volumen K6',
    450000 + (n * 100),
    500000 + (n * 100),
    1 + MOD(n, 5),
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    CONCAT('K6VOL-', target_total, '-', n),
    DATE_ADD('2026-01-01 10:00:00', INTERVAL MOD(n, 330) DAY),
    'Reserva generada automáticamente para pruebas de volumen K6',
    'CONFIRMED',
    DATE_ADD(DATE_ADD('2026-01-01', INTERVAL MOD(n, 330) DAY), INTERVAL 5 DAY),
    DATE_ADD('2026-01-01', INTERVAL MOD(n, 330) DAY),
    (SELECT id FROM tour_packages ORDER BY id LIMIT 1),
    (SELECT id FROM users ORDER BY id LIMIT 1)
  FROM tmp_k6_numbers;

  INSERT INTO payments (
    amount,
    card_cvv,
    card_expiration,
    card_number,
    payment_date,
    payment_method,
    payment_status,
    reservation_id
  )
  SELECT
    r.final_total_amount,
    '123',
    '1230',
    '4111111111111111',
    r.reservation_date,
    'CREDIT_CARD',
    'APPROVED',
    r.id
  FROM reservations r
  LEFT JOIN payments p ON p.reservation_id = r.id
  WHERE r.purchase_group_code LIKE CONCAT('K6VOL-', target_total, '-%')
    AND p.id IS NULL;

  DROP TEMPORARY TABLE IF EXISTS tmp_k6_numbers;
END //

DELIMITER ;
