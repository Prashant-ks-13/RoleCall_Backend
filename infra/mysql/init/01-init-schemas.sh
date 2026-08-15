#!/bin/bash
# Runs once, on first container start, as docker-entrypoint-initdb.d convention.
# Creates one schema + one least-privilege user per service (database-per-service pattern).
set -euo pipefail

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
  CREATE DATABASE IF NOT EXISTS auth_db;
  CREATE USER IF NOT EXISTS 'auth_service'@'%' IDENTIFIED BY '${AUTH_DB_PASSWORD}';
  GRANT ALL PRIVILEGES ON auth_db.* TO 'auth_service'@'%';

  CREATE DATABASE IF NOT EXISTS user_db;
  CREATE USER IF NOT EXISTS 'user_service'@'%' IDENTIFIED BY '${USER_DB_PASSWORD}';
  GRANT ALL PRIVILEGES ON user_db.* TO 'user_service'@'%';

  CREATE DATABASE IF NOT EXISTS job_db;
  CREATE USER IF NOT EXISTS 'job_service'@'%' IDENTIFIED BY '${JOB_DB_PASSWORD}';
  GRANT ALL PRIVILEGES ON job_db.* TO 'job_service'@'%';

  CREATE DATABASE IF NOT EXISTS application_db;
  CREATE USER IF NOT EXISTS 'application_service'@'%' IDENTIFIED BY '${APPLICATION_DB_PASSWORD}';
  GRANT ALL PRIVILEGES ON application_db.* TO 'application_service'@'%';

  CREATE DATABASE IF NOT EXISTS payment_db;
  CREATE USER IF NOT EXISTS 'payment_service'@'%' IDENTIFIED BY '${PAYMENT_DB_PASSWORD}';
  GRANT ALL PRIVILEGES ON payment_db.* TO 'payment_service'@'%';

  FLUSH PRIVILEGES;
EOSQL
