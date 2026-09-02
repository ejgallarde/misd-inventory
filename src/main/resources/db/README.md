# `db/` contents

Two kinds of file live here. They are not interchangeable.

## Reference schema — `misd_inventory_*.sql`

`mysqldump --no-data` output for the fourteen tables the application uses, kept
so the schema can be read without a database connection. They are documentation.

The `DROP TABLE IF EXISTS` statement mysqldump normally emits at the top of each
file has been removed. As dumped, opening one in Workbench and pressing execute
against a populated database would have dropped that table and everything
cascading from it, without a confirmation. Recreating a table is now a
deliberate two-step action.

Regenerate with:

```
mysqldump -u<user> -p --no-data --skip-add-drop-table misd_inventory <table>   > src/main/resources/db/misd_inventory_<table>.sql
```

## Corrective migrations — `migration_<date>_<subject>.sql`

Dated, idempotent, additive scripts that fix data or indexes. Each opens with a
read-only preflight section, wraps its changes in transactions, and ends with
verification queries. Run the preflight first and read its output.

Take a backup before running one:

```
mysqldump -u<user> -p misd_inventory > misd_inventory_backup_<date>.sql
mysql    -u<user> -p misd_inventory < src/main/resources/db/migration_<date>_<subject>.sql
```

## One-off data corrections

`psgc_province_city_fixes_3_ascii.sql` repairs mis-imported PSGC place names and
is not part of normal migrations. `psgc-schema.sql` and
`realestateproperties-area-ddl.sql` are historical DDL additions.
