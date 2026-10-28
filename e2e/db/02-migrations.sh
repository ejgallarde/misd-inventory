#!/bin/bash
# Applies the dated corrective/schema migrations (src/main/resources/db/
# migration_*.sql, mounted at /schema) in date order on top of the reference
# dumps, the same path a production database takes. Each script is idempotent.
set -euo pipefail
for f in $(ls /schema/migration_*.sql | sort); do
  echo "Applying migration: $f"
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$f" > /dev/null
done
