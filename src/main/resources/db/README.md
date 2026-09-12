# `db/` contents

Three kinds of file live here. They are not interchangeable.

## DAR schema — `dar_inventory_schema.sql`

A standalone, from-scratch `CREATE DATABASE` + `CREATE TABLE` script for the
`dar_inventory` database used by this branch's DAR (Department of Agrarian
Reform) deployment. Unlike the `misd_inventory_*.sql` dumps below, it is not a
`mysqldump` of a live database — it is the schema to run once to stand up a new
one. It covers only the three domains this deployment tracks (IT assets, fleet
vehicles, survey assets) plus their shared personnel/location/document/audit
infrastructure; it deliberately omits `realestateproperties` and the PSGC
tables, which are MISD-only. Run it with:

```
mysql -u<user> -p < src/main/resources/db/dar_inventory_schema.sql
```

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
