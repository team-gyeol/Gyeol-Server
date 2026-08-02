#!/usr/bin/env bash
# 부하 테스트용 사용자를 로컬 MySQL 에 시딩한다.
# 스키마는 앱을 한 번 띄워 ddl-auto: update 로 생성된 뒤에 실행할 것.
#
#   ./load-test/seed-users.sh          # 기본 20명
#   ./load-test/seed-users.sh 100      # 100명
set -euo pipefail

COUNT="${1:-20}"
CONTAINER="${MYSQL_CONTAINER:-gyeol-mysql}"

read -r -d '' SQL <<SQL || true
SET @n = ${COUNT};
INSERT INTO users (user_id, email, name, role, oauth_id)
SELECT seq, CONCAT('loadtest', seq, '@example.com'), CONCAT('loadtest-user-', seq), 'USER', CONCAT('loadtest-', seq)
FROM (
  SELECT (a.i + b.i * 10 + c.i * 100 + 1) AS seq
  FROM (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
  CROSS JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
  CROSS JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c
) s
WHERE seq <= @n
ON DUPLICATE KEY UPDATE name = VALUES(name);

SELECT COUNT(*) AS seeded_users FROM users WHERE email LIKE 'loadtest%@example.com';
SQL

docker exec -i "${CONTAINER}" mysql -ugyeol -pgyeol gyeol -e "${SQL}" 2>&1 | grep -v '^mysql: \[Warning\]'
echo "테스트 사용자 ${COUNT}명 시딩 완료 (user_id 1..${COUNT})"
