# Sample data

Builds a demo dataset for all three modules and runs it in a separate database.

```
cd tools/sample-data
npm install
npm run generate            # writes out/sample-data.sql (gitignored)
cd ../../e2e
npm run demo:up             # MySQL on :3308, schema + migrations + sample data
npm run demo:app            # the app on http://localhost:8091
```

Sign in with any of these (password: `MISD_DEMO_USER_PASSWORD`, `e2e-password` by default in `demo:app`):

| Account | Role | Person |
| --- | --- | --- |
| earl.gallarde@phlpost.gov.ph | Admin | Earl John C. Gallarde, IT Officer II |
| joshua.dejesus@phlpost.gov.ph | Clerk | Joshua de Jesus, Computer Programmer III |
| don.melgarejo@phlpost.gov.ph | Clerk | Don Melgarejo, Admin / Manager, LPMD |
| marigold.libuit-arroyo@phlpost.gov.ph | Finance | Marigold C. Libuit-Arroyo, APMG for MSS |

## What it contains

- **People:** the four staff above, all Regular, plus two clearly fictional extras that show the accountability rule. One is a Job Order driver under the LPMD manager; the other is a Contract of Service IT staffer under the IT Officer.
- **IT assets:** a laptop, desktop, firewall appliance (FortiGate 100F), barcode scanner and heavy-duty printer, each assigned to different people. Spares are in storage.
- **Fleet:** BYD (T3 EV van, Atto 3), Toyota Hiace, Nissan Navara (under repair) and BMW 320i (executive car with a chosen accountable owner). One registration has lapsed.
- **Properties:** 10 lots picked with a fixed seed from the `ALL-INVENTORY working` tab of `docs/LAND INVENTORY.xlsx`, plus a building record for 3 of those lots that have a PPC building. Only workbook values are used: name, Postal Area, province, location, lot area, valuation and acquisition mode. Title and tax-declaration numbers are left blank rather than invented, and each record's Remarks cite its source row.

The workbook and the generated SQL contain PHLPost data and are gitignored. Only this generator is committed.
Override the workbook location with `LAND_INVENTORY=/path/to/file.xlsx npm run generate`.
