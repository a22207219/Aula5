#!/bin/sh
set -eu

# Values are passed through Compose. Use psql's quoted variable substitution
# plus format(%I/%L) so usernames and passwords are escaped safely.
psql \
  --set=ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname postgres \
  --set=product_db_user="$PRODUCT_DB_USER" \
  --set=product_db_password="$PRODUCT_DB_PASSWORD" \
  --set=supplier_db_user="$SUPPLIER_DB_USER" \
  --set=supplier_db_password="$SUPPLIER_DB_PASSWORD" \
  --set=order_db_user="$ORDER_DB_USER" \
  --set=order_db_password="$ORDER_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'product_db_user', :'product_db_password') \gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'supplier_db_user', :'supplier_db_password') \gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'order_db_user', :'order_db_password') \gexec
SELECT format('CREATE DATABASE %I OWNER %I', 'product_db', :'product_db_user') \gexec
SELECT format('CREATE DATABASE %I OWNER %I', 'supplier_db', :'supplier_db_user') \gexec
SELECT format('CREATE DATABASE %I OWNER %I', 'order_db', :'order_db_user') \gexec
SQL
