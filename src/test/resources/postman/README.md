# dx-dataplane-ogc — Postman collection (v2 auth)

End-to-end Postman collection (**243 requests**) for testing the OGC server.

> **Simple usage (recommended while learning):** you do **not** need Keycloak, AppId or
> delegation. Paste any valid DX JWT into the `accessToken` variable and every secured
> request works — a **consumer** token for the read folders (02, 05, 07, 09), a
> **provider** token for the write folders (03, 06, 11). Ignore the Basic-AppId and
> `did` requests unless you are on the v2-auth deployment.
>
> **Learning path:** read `docs/DEVELOPER_GUIDE_2.md` alongside this collection —
> folder 01 = discovery, 02/07 = Features, 04/05/06 = STAC, **11 = the provider
> onboarding stories of Guide 2 §12 in runnable form (start there)**.

The auth matrix folders additionally cover the v2 authentication
(JWT + Basic AppId + delegation) and gRPC item-access.

> **No test scripts.** These are plain requests — there are no `pm.test` assertions. The
> expected HTTP status for each request is written in the request's **description** as a
> hint only. The only scripts present are functional helpers that capture the token / job id
> into collection variables (see below).

## Files
| File | What |
|------|------|
| `dx-dataplane-ogc.postman_collection.json` | The collection (import this). |
| `dx-dataplane-ogc.local.postman_environment.json` | An environment with all variables (fill in the blanks). |
| `_gen_collection.py` | Generator that produced the collection — edit + re-run to regenerate. |

## Import
1. Postman → **Import** → drop both JSON files.
2. Select the **`dx-dataplane-ogc · LOCAL`** environment (top-right).

## Set these variables first
Minimum for a smoke run:
- `baseUrl` — `http://localhost:8080` (local) or `https://v2.dev.ogc.iudx.io` (dev).
- `keycloakTokenUrl`, `clientId`, `clientSecret`, `username`, `password` — to mint a JWT.
- `collectionId` — an **OPEN** dataset id (for happy-path 200s).
- `restrictedCollectionId` — a **RESTRICTED** dataset id you can be granted/denied on.

For the other auth paths:
- `appId` / `appSecret` — Basic AppId auth.
- `did` — a delegation id (delegator's sub) for the delegation tests.

Endpoint-specific (optional): `featureId`, `tileMatrixSetId`, `processId`, `jobId`,
`assetId`, `stacItemId`, `recordCatalogId`, `recordId`.

## How to run
1. **00 · Auth & Token → `Keycloak · Get User Token (password grant) ⭐`** — run this first.
   Its test script saves `access_token` into the `accessToken` collection variable; every
   secured request then sends `Authorization: Bearer {{accessToken}}`.
2. Run any folder, or the whole collection via the **Collection Runner**.

## Folders
| # | Folder | Coverage |
|---|--------|----------|
| 00 | Auth & Token | Keycloak user/service/refresh tokens + negative cases; DX-AAA alt flow. |
| 01 | OGC Core & Discovery (open) | Landing, `/api` spec, conformance, collections, records (catalog/items/`q=` search), processes, tile metadata, S3 bucket list — no token. |
| 02 | OGC Features & Tiles (secured) | Full auth matrix on features/feature/coverage/tile + bad params. |
| 03 | Processes & Jobs | Execute process, poll job status, list all jobs, fetch job results. |
| 04 | STAC Discovery (open) | STAC landing/`/stac/api` spec/conformance/collections. |
| 05 | STAC Data & Assets (secured) | STAC items + asset download (pre-signed URLs). |
| 06 | STAC Write & Search (provider) | Create/update/delete collections & items, `/stac/search`. |
| 07 | Features Query Matrix (secured) | bbox / datetime / crs / paging variants. |
| 08 | Content Negotiation | `Accept` / `f=` variants. |
| 09 | Metering / Audit (secured) | Usage summary & audit. |
| 10 | Negative & Smoke | Bad routes, ambiguous auth, 404/406/405 edges. |
| 11 | Provider Workflow (end-to-end) | **The learning folder.** Every process with its real input body, in story order: presigned upload → CollectionOnboarding → poll job → verify items → tiles generation → verify tile → TilesMetaDataOnboarding → STAC collection/item + multipart asset upload → FeatureAttributesExtraction → jobs list/results. Step 0 auto-captures all process ids into `pid_<Title>` variables. |

Folder request counts: 00=7, 01=43, 02=42, 03=15, 04=11, 05=20, 06=25, 07=15, 08=12,
09=19, 10=13, 11=21 — **243 total**.

For folder 11 set: `resourceId` (a Catalogue resource UUID you own, not yet onboarded),
`stacResourceId` (a second owned UUID for the STAC steps), `s3BucketIdentifier`
(default `default`), and later `attributeName` / `eTag1` as the steps instruct. The
captured variables (`pid_<Title>`, `s3PresignedUrl`, `s3ObjectKeyName`, `jobId`,
`uploadId`, `s3PartUrl`) fill themselves.

## Auth matrix per secured endpoint
Each secured endpoint exercises: no-auth (401), valid Bearer (200), Bearer on restricted
no-access (403), invalid/expired/malformed Bearer (401), Basic AppId valid (200), Basic
AppId invalid (401), and **Bearer/Basic + `did` delegation**.

## ⚠️ Known gap — delegation (`did`) tests
Requests tagged **KNOWN GAP** test the delegated (`did`) item-access path. They currently
return **403** because OGC sends `(delegator, delegator)` to the controlplane
`CheckItemAccess` for delegated requests, so delegation is wrongly denied. Their description
notes "one of 200, 403" and they should return **200** once the delegated-item-access bridge
is applied. Direct JWT and direct AppId access work correctly.

## Expected status hints
Since there are no assertions, each request's **description** notes its expected status
(e.g. "Expected status: 401" or "one of 200, 403"). It's a hint only — the exact code can
depend on your data (OPEN vs RESTRICTED dataset, provider vs consumer), so eyeball the
response against the hint.

## Regenerate
```bash
cd src/test/resources/postman
python3 _gen_collection.py
```
