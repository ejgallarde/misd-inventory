#!/bin/bash
# Loads the reference schema dumps (src/main/resources/db/misd_inventory_*.sql,
# mounted at /schema) into the e2e database. Each dump disables foreign-key
# checks itself, so file order does not matter.
set -euo pipefail
for f in /schema/misd_inventory_*.sql; do
  echo "Loading schema: $f"
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$f"
done
