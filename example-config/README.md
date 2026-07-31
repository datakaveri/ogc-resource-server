# `config.json` Field Reference — ogc-resource-server

Complete reference for every field in [`config-example.json`](./config-example.json): what it
configures, what breaks if it is wrong, and where to obtain its value. Written for whoever deploys
and operates the OGC resource server.

**Where to start.** §1 explains how config blocks reach each verticle — the most common source of
"the key is set, but the code reads `null`" (and, here, of "I edited the module and nothing
changed"). §2 documents every field individually. §3 groups fields by category (credentials, URLs,
tuning knobs, feature flags) so a whole category can be checked at once. §4 lists dead fields and
known inconsistencies between the config and the code.

This example file mirrors the working `secrets/configs/dev.json` (the dev deployment's actual
config, which is the reference for this file's shape) — same keys, same module order, with all
credentials and infrastructure hosts blanked. The deliberate differences are listed in
*Deviations from the working dev config* below. When the working config changes shape, re-sync
this file and this document.

Every field was traced to the code that reads it. Code references name the **consuming class only,
never a line number** — line numbers go stale on the first unrelated edit, and this document is
meant to outlive that.

## 0. Document header

| |                                                   |
|---|---------------------------------------------------|
| **Service** | OGC resource server (ogc-resource-server)         |
| **Code repo / branch** | `datakaveri/ogc-resource-server` — `dev` (source of truth) |
| **Config schema version** | `1.0` (top-level `version`)                       |
| **Maintainer / point of contact** | *Ankit Singh*                                     |
| **Last updated** | 2026-07-31                                        |

**Files in this directory:**

| File | Purpose |
|---|---|
| [`config-example.json`](./config-example.json) | Example config — the subject of this document. Synced from the working `secrets/configs/dev.json` with credentials blanked |
| [`integration-test-db-config.properties`](./integration-test-db-config.properties) | JDBC URL/user/password used by the integration-test suite (Flyway + test fixtures), unrelated to the server config |

**Where the file goes.** `Deployer` takes the path from the `--config`/`-c` CLI option. Locally
that is `secrets/configs/<name>.json`; in Docker Swarm, `ogc-rs-stack.yaml` mounts
`./secrets/configs/config.json` as a secret at `configs/config.json` and launches with
`-c configs/config.json`. Nothing scans the directory — the file name is whatever you pass.

**Deviations from the working dev config.** Three, all deliberate:

| Deviation | Why |
|---|---|
| `issuers.leeway` → `issuers.jwtLeeway` | The code reads `jwtLeeway`; `leeway` is ignored — see §4 |
| `s3*` keys and `jwksRefreshIntervalMs` on the `ApiServerVerticle` entry dropped | No read site under those names; the live S3 settings are the `s3BucketsConfig` block — see §4 |
| `automaticRecoveryEnabled` and `externalVhost` on the `DataBrokerVerticle` entry dropped | No read site — see §4 |

Every other key and module position matches the working config. Two values differ on purpose:
`auditingExchange` uses the default `auditing` rather than the dev override `test-auditing`, and
all credentials and hosts are blanked or generic.

## 1. Top-level structure

| Key | Configures | Consumed by |
|---|---|---|
| `version` | Config schema marker | Nothing — see §4 |
| `zookeepers` | Zookeeper hosts for clustered Vert.x | Nothing in this repo — see §4 |
| `clusterId` | Hazelcast discovery group | Nothing in this repo — see §4 |
| `host` | *(nothing — copied into every module's config by `Deployer` and never read; see §4)* | — |
| `commonConfig` | Cross-cutting base paths, catalogue endpoint, the OGC Postgres database, audit destination, pool size | **Every** module |
| `s3BucketsConfig` | One or more S3/S3-compatible bucket configurations | `ApiServerVerticle` via `S3ConfigsHolder`; validated at startup |
| `modules` | Verticle deployment list, deployed **in array order** | `Deployer` |

**The merge rule is the key mechanism.** For each module, `Deployer.getConfigForModule` does:

```java
config.mergeIn(commonConfigs, true).put("s3BucketsConfig", s3Config)
```

so every module's `config()` is *its own entry* + **all of `commonConfig`** + the whole
`s3BucketsConfig` block. Two consequences:

1. There is no `required` list — anything in `commonConfig` reaches every verticle. A key only
   needed by one verticle can still live on that module's entry.
2. **`commonConfig` overwrites the module entry**, not the other way round. A key present in both
   takes the `commonConfig` value. This is the opposite of what most people assume when they edit
   a module block and see no change.

   `dxCatalogueBasePath` and `catRequestItemsUri` are in both `commonConfig` and the
   `CatalogueVerticle` entry. The values match today, so nothing looks wrong — but editing only
   the module entry changes nothing. Put each key in exactly one place.

`Deployer` also injects the top-level `host` into every module config, and reads
`verticleInstances` from each entry (unboxed — a missing value NPEs the deploy loop). Deployment
is strictly sequential: a module is deployed only after the previous one succeeds, so **order
matters** for anything whose event-bus service another verticle calls at request time.

Startup fails fast in exactly two places: `s3BucketsConfig` missing/empty (`Deployer` logs fatal
and returns, no verticle deploys) and an invalid bucket block (`S3ConfigsHolder` throws inside
`ApiServerVerticle.start`). Everything else surfaces later, usually per request.

There is **no clustered mode in this repo.** `Deployer` builds a plain `Vertx.vertx(...)`; the CLI
declares only `--help` and `--config`. `zookeepers`, `clusterId` and the `--host` argument in
`ogc-rs-stack.yaml` have no effect.

A Prometheus metrics endpoint is started unconditionally on **port 9000** (`Deployer.getMetricsOptions`)
— not configurable, but it must be free and it should not be exposed publicly.

---

## 2. Field blocks

### `version`

- **Type / format:** string
- **Required:** no
- **Purpose:** Schema marker only. No code reads it.
- **Example value:** `"1.0"`
- **Failure mode:** none.

### `zookeepers` / `clusterId`

- **Type / format:** array of strings / string
- **Required:** no — **dead keys** in this repo. See §4.
- **Purpose:** none here. Kept for shape-compatibility with the other DX services, whose deployers
  build a clustered Vert.x.

### `host` *(top-level)*

- **Type / format:** string; hostname
- **Required:** no — **dead key**
- **Purpose:** none. `Deployer` copies it into every module's config, but no verticle reads
  `"host"`. The public hostname used in responses and specs is `commonConfig.hostName`.
- **Failure mode:** none — but editing it and expecting URLs to change is a trap. Change
  `hostName` instead.

---

### `commonConfig` — merged into every module

### `commonConfig.ogcBasePath`

- **Type / format:** string; path prefix, trailing slash included
- **Required:** yes
- **Purpose:** Prefix used when `ApiServerVerticle` builds `href`s for the OGC and STAC landing
  pages, conformance and collection links.
- **Example value:** `"/"`
- **Default if omitted:** `null` → landing-page links render as `https://hostnull...`.
- **Notes / gotchas:** Concatenated directly onto `hostName`, so it must start (and, if non-root,
  end) with `/`.

### `commonConfig.hostName`

- **Type / format:** string; **full URL with scheme**, no trailing slash
- **Required:** yes
- **Purpose:** The server's own public base URL. Substituted for `$HOSTNAME` in
  `docs/landingPage.json` and for `${HOSTNAME}` in both OpenAPI templates
  (`RouterManager`), and used to build job links (`JobsServiceImpl`), asset/tile links
  (`DatabaseServiceImpl`) and process result links (`ProcessesRunnerImpl`).
- **Example value:** `"https://ogc.example.org"`
- **Default if omitted:** `null` → `NullPointerException` in `RouterManager` while replacing the
  spec placeholder; the router is never built and the HTTP server never starts (see the
  "Waiting for router to be initialized" loop).
- **Change impact:** must match the public ingress route. Every link the server hands out is
  built from it — wrong value means clients follow links to the wrong host.
- **Notes / gotchas:** Scheme **required** here, unlike `catServerHost`/`controlPanelHost`.

### `commonConfig.catalogId`

- **Type / format:** string
- **Required:** yes
- **Purpose:** The `id` of the STAC catalogue in the STAC landing page (`ApiServerVerticle`).
- **Example value:** `"stac"`
- **Default if omitted:** `null` → the STAC landing page advertises `"id": null`, which fails
  strict STAC clients and validators.

### `commonConfig.catServerHost` + `catServerPort` + `dxCatalogueBasePath` + `catRequestItemsUri`

**Documented as a set — the DX catalogue (controlplane) HTTP endpoint.**

- **Type / format:** string (**bare hostname, no scheme, no path**) / int / string (path) /
  string (path)
- **Required:** yes
- **Purpose:** `CatalogueVerticle` builds a `WebClient` against `catServerHost:catServerPort` and
  calls two endpoints: `dxCatalogueBasePath + "/search"` (item lookup by id) and
  `catRequestItemsUri` (full item fetch, used by the authorization handlers). The same three keys
  are read again by `S3PreSignedURLGenerationProcess` for its own catalogue call.
- **Example value:** `"controlplane.example.org"` / `443` / `"/controlplane/iudx/v2/cat"` /
  `"/controlplane/iudx/v2/cat/item"`
- **Default if omitted:** `catServerPort` is unboxed — missing key NPEs `CatalogueVerticle.start`
  and the whole deployment chain stops there.
- **Failure mode:** wrong host/path → every catalogue lookup fails, so authorization on secure
  collections/assets fails and those APIs 401/500 while open collections keep working.
- **Change impact:** cross-service — must match the controlplane's catalogue ingress and its
  base path.
- **Notes / gotchas:** **This must be a bare hostname.** It is passed straight to
  `WebClient.get(port, host, uri)`, which resolves it via DNS — a value with a path glued on
  cannot resolve. The path belongs in `dxCatalogueBasePath` and `catRequestItemsUri`.

### `commonConfig.databaseHost` / `databasePort` / `databaseUser` / `databasePassword` / `databaseName`

**Documented as a set — the OGC Postgres database.**

- **Type / format:** string (host) / int / string / string (secret) / string
- **Required:** yes
- **Purpose:** The server's main database. Read by `DatabaseVerticle`, `JobsVerticle`,
  `ProcessVerticle` and `MeteringVerticle` (as its second pool), by `RouterManager` for the
  Postgres `LISTEN/NOTIFY` channel that triggers OpenAPI-spec and router regeneration, and by the
  onboarding/appending processes, which shell out to **GDAL** (`ogr2ogr`) with these values.
- **Privileges required:** full read/write **and DDL** on the OGC schema — the onboarding
  processes create and drop tables/indexes, and the collections tables are written at runtime.
  Read-only will not work.
- **Default if omitted:** `databasePort` is unboxed → NPE at verticle start. Wrong credentials →
  the pool fails on first query; the verticle still deploys, so the failure shows up per request.
- **Failure mode:** if `RouterManager` cannot connect to the `LISTEN/NOTIFY` channel it logs
  fatal and keeps running with a **static router** — newly onboarded collections never appear
  until restart. It retries every 5 s for ~30 min, then gives up.
- **Notes / gotchas:** The keys are `databaseUser`/`databasePassword`. `dbUser`/`dbPassword` are
  not read.

### `commonConfig.jwtIgnoreExpiry` / `issuer`

- **Type / format:** bool (**feature flag**) / string; issuer as it appears in the token's `iss`
  claim
- **Required:** no — **currently no read site.** They configured
  `DxTokenAuthenticationHandler`, which is no longer constructed.
- **Example value:** `false` / `"dx.example.org/controlplane"`
- **Failure mode:** none today.
- **Notes / gotchas:** Authentication on the routes is `MultiIssuerJwtAuthHandler`, which reads
  the `issuers` block on the `ApiServerVerticle` entry: its own `jwtIgnoreExpiry` lives there and
  the accepted issuers are the keys of that map. Keep `issuer` equal to one of those keys and
  `jwtIgnoreExpiry` equal to `issuers.jwtIgnoreExpiry`, so the two never contradict each other if
  something starts reading these again.

### `commonConfig.auditingExchange` / `auditingRoutingKey`

- **Type / format:** string; RabbitMQ exchange name / string; routing key
- **Required:** no — default `"auditing"` / `"##"` (`Constants.DEFAULT_AUDITING_EXCHANGE`,
  `DEFAULT_AUDITING_ROUTING_KEY`, applied in `EntityRouterBuilder`)
- **Purpose:** Where `AuditingHandler` publishes audit records, on the **internal vhost**, after a
  200/201/204 response. It governs **one of the two audit publishers**, on these three routes
  only:
  `GET /collections/{collectionId}/items`, `GET /collections/{collectionId}/items/{featureId}`
  and `GET /assets/{assetId}`.
- **Example value:** `"auditing"` / `"##"`
- **Failure mode:** **silent.** If the exchange does not exist, or no queue is bound with this
  exact routing key, records are published and dropped by the broker — neither side logs an
  error and no request fails.
- **Change impact:** cross-service — the exchange and binding must exist on the broker before
  startup; the server declares neither.
- **Notes / gotchas:** `##` is a **literal** routing key, not a wildcard; the binding must match
  it character for character. Changing these keys moves **only** the `AuditingHandler` stream —
  see the scope table below, and §4. The working dev config points `auditingExchange` at
  `"test-auditing"`; this example uses the default.

**What these keys do and do not cover.** Each request on the three routes above writes **three**
audit records, and only the third is configurable:

| # | Written by | Destination | Configurable? |
|---|---|---|---|
| 1 | `insertIntoPostgresAuditTable` | `metering` table in the **OGC** database (`commonConfig.database*`) | no |
| 2 | `MeteringServiceImpl.insertMeteringValuesInRmq` | exchange `auditing`, routing key `#` | **no** — both hardcoded in `MeteringConstant` |
| 3 | `AuditingHandler.publishAuditLogs` | `auditingExchange` + `auditingRoutingKey` | **yes** |

The two RabbitMQ messages are not copies of one record — they carry different payloads. #2 sends
the metering shape (`userId`, `id`, `api`, `response_size`, `epochTime`, `isoTime`, `providerId`,
`primaryKey`, `origin`); #3 sends `OgcRsAuditLog` (audit id, item id, `ASSET`, operation,
timestamp, endpoint, method, role, userId, `OGC_RS`, organisation id/name, `iss`, delegator id, IP,
user agent). Tile requests emit **only** #2, through `TilesMeteringHandler`.

Consequence for provisioning: unless `auditingExchange` is left at `auditing`, **two** exchanges
and two bindings must exist, or one of the two streams is silently discarded.

### `commonConfig.poolSize`

- **Type / format:** int — **tuning knob**
- **Required:** yes — read unboxed by `DatabaseVerticle`, `JobsVerticle`, `ProcessVerticle` and
  `MeteringVerticle`; a missing value NPEs at start.
- **Purpose:** `PoolOptions.setMaxSize` for every Postgres pool.
- **Example value:** `5`
- **Notes / gotchas:** It is applied **per verticle instance**, and because `commonConfig`
  overwrites module entries, the value here silently replaces any per-module `poolSize`. Total
  connections ≈ `poolSize` × (instances of each pooling verticle) + 1 for the `LISTEN/NOTIFY`
  subscriber — size Postgres `max_connections` accordingly.

---

### `s3BucketsConfig` — object storage

An object of **named bucket configurations**; the key is the bucket *identifier* used elsewhere in
the system (collections reference a bucket by identifier in the database), and `"default"` is the
identifier used when a collection names none. Consumed by `S3ConfigsHolder`/`S3Config`, built in
`ApiServerVerticle.start` and used for pre-signed URLs, tile and asset serving, and the
onboarding/publish processes.

| Key | Type | Notes |
|---|---|---|
| `bucket` | string | bucket name |
| `endpoint` | string | **full URL with `http`/`https` scheme** — validated; AWS regional endpoint or a MinIO/S3-compatible URL |
| `region` | string | e.g. `ap-south-1` |
| `accessKey` / `secretKey` | string (secrets) | IAM or S3-compatible credentials |
| `pathBasedAccess` | bool | `true` → path-style (`endpoint/bucket/key`, what MinIO wants), `false` → virtual-hosted style. Must be a real boolean, not `"true"` |
| `readAccess` | string | exactly `PUBLIC` or `PRIVATE`. `PUBLIC` makes the server rewrite STAC asset `href`s into absolute, directly-fetchable bucket URLs (built from `endpoint`, `bucket` and `pathBasedAccess`); `PRIVATE` leaves them relative, so reads go through the server / a pre-signed URL |

- **Required:** yes — **this block is fail-fast, twice.** `Deployer` refuses to deploy anything if
  the block is missing or empty (`"s3BucketsConfig JSON object not present or empty in config"`),
  and `S3ConfigsHolder.validate` throws inside `ApiServerVerticle.start` if any bucket block is
  missing a key, has an empty string, a non-boolean `pathBasedAccess`, an unknown `readAccess`, or
  an endpoint without an `http`/`https` scheme.
- **Privileges required:** get/put/delete on the bucket (the onboarding and pre-signed-URL
  processes write), plus list for the tile paths.
- **Failure mode:** wrong credentials → S3 operations fail at request time with `403 AccessDenied`
  while the server stays up. `readAccess`/`pathBasedAccess` wrong → URLs are generated in a form
  the store rejects, usually seen as 403/404 on assets only.
- **Notes / gotchas:** More than one bucket may be configured — add another key beside `default`.
  The blanked example **will not boot as-is**: fill the credentials or the API server verticle
  fails to deploy.

---

### `modules[]` — deployment list

#### `modules[].id`

- **Type / format:** string; fully-qualified Java class name
- **Required:** yes
- **Failure mode:** `ClassNotFoundException` at deploy; the chain stops and nothing after it in
  the array is deployed.
- **Notes / gotchas:** Deployment is sequential and a failure stops the chain, so a broken module
  hides every module after it. `DatabaseVerticle` should come first — `RouterManager` queries it
  while generating the specs. The rest are order-tolerant: event-bus proxies are created eagerly
  but only need their target at the moment a message is sent, which is per request. That is why
  `DataBrokerVerticle` works after `ApiServerVerticle` — the only exposure is a request arriving
  in the second or two before it deploys, whose audit publish would fail.

#### `modules[].verticleInstances`

- **Type / format:** int — **tuning knob**
- **Required:** yes — read unboxed by `Deployer`; a missing value NPEs the deploy loop.
- **Purpose:** Number of instances. For `ApiServerVerticle` it is read a second time by
  `RouterManager`, which **waits until exactly this many instances have registered** before it
  generates the specs and builds the routers.
- **Failure mode:** if the value does not match the instances that actually start, the router is
  never built: the server logs "Waiting for router to be initialized" for 60 s and then throws,
  leaving a process that is up but serving nothing. Probe the port, not the process.
- **Notes / gotchas:** Multiplies pooled resources — see `poolSize`.

#### `modules[].isWorkerVerticle`

- **Required:** no — **dead key.** `Deployer` never calls `setWorker`. See §4.

---

### `ogc.rs.database.DatabaseVerticle`

Registers `DatabaseService` on the event bus; owns the main Postgres pool. All keys come from
`commonConfig` (`databaseHost`, `databasePort`, `databaseName`, `databaseUser`,
`databasePassword`, `poolSize`) — the module entry needs only `id` and `verticleInstances`.

- **Failure mode:** deploys even if Postgres is unreachable (the pool is lazy); every API that
  touches the database then fails at request time.

### `ogc.rs.catalogue.CatalogueVerticle`

Registers `CatalogueInterface` on the event bus. Reads `catServerHost`, `catServerPort`,
`dxCatalogueBasePath`, `catRequestItemsUri` — documented above. The last two are repeated on the
module entry, as in the working config; the `commonConfig` copies are what actually apply.

- **Failure mode:** if this verticle is absent or its calls fail, every authorization check that
  needs item metadata fails — secure collections, STAC assets, processes.

### `ogc.rs.databroker.DataBrokerVerticle` — RabbitMQ block

Registers `DataBrokerService`, used by `AuditingHandler` (mounted on the OGC feature and STAC
routes) and by `MeteringServiceImpl` to publish audit records to the **internal vhost**.

#### `dataBrokerIP` / `dataBrokerPort`

- **Type / format:** string (hostname, no scheme) / int (AMQP port)
- **Required:** yes (`dataBrokerPort` unboxed — NPE if missing)
- **Notes / gotchas:** `24568` is the DX dev broker's non-standard AMQP port; a plain in-cluster
  RabbitMQ uses `5672`.

#### `dataBrokerManagementPort`

- **Type / format:** int
- **Required:** yes (unboxed)
- **Purpose:** Port of the RabbitMQ **management HTTP API**, used for the admin `WebClient`.
- **Notes / gotchas:** `443` matches a broker behind an HTTPS ingress; a plain broker uses
  `15672`. Unlike the other DX services there is no `portSsl` flag here.

#### `dataBrokerUserName` + `dataBrokerPassword`

**Documented as a pair.**

- **Type / format:** string / string (secret)
- **Required:** yes
- **How to obtain:** RabbitMQ credentials from the databroker provisioning step.
- **Privileges required:** configure/write/read on the prod and internal vhosts. This server only
  publishes — it does not create users or queues — so a `management`-tagged user is enough.
- **Failure mode:** `ACCESS_REFUSED` on connect; audit publishing fails while the APIs keep
  serving. Nothing rejects a request because auditing failed.

#### `prodVhost` / `internalVhost`

- **Type / format:** string; vhost names (uppercase by convention)
- **Required:** yes — both clients are created at startup.
- **Expected value:** `<TENANT>` and `<TENANT>-INTERNAL`, e.g. `IUDX-V2` / `IUDX-V2-INTERNAL`
- **Purpose:** `internalVhost` carries all audit traffic (`publishMessageInternal`). `prodVhost`
  is connected but currently unused by any publish path in this repo.
- **Failure mode:** `NOT_ALLOWED - vhost ... not found`; the affected client fails to start.
- **Notes / gotchas:** Case-sensitive. `externalVhost`, present in the working dev config, is
  never read here — see §4.

#### `connectionTimeout` / `requestedHeartbeat` / `handshakeTimeout` / `requestedChannelMax` / `networkRecoveryInterval`

- **Type / format:** int (ms; heartbeat in seconds; channel max a count) — **tuning knobs**
- **Required:** yes — all read unboxed; omitting any NPEs at deploy. Ship the example values
  (`6000`, `60`, `6000`, `5000`, `500`) unless proven wrong.
- **Notes / gotchas:** Automatic recovery is hardcoded on; `automaticRecoveryEnabled` in the
  working config is dead — see §4.

##### RabbitMQ topology to provision

The server declares nothing — these must exist **before** startup:

| Object | Vhost | Notes |
|---|---|---|
| exchange named by `commonConfig.auditingExchange` (direct, durable) | internal | Default `auditing`; dev currently uses `test-auditing` |
| queue bound to it with key `commonConfig.auditingRoutingKey` | internal | Default `##` — a **literal**, not a wildcard |
| `auditing` exchange + queue bound with key `#` | internal | Second publisher, both values hardcoded in `MeteringConstant`. Needed **in addition** whenever `auditingExchange` is not `auditing` — see §4 |

### `ogc.rs.apiserver.ApiServerVerticle`

The HTTP server, routers and all API handlers.

#### `httpPort`

- **Type / format:** int
- **Required:** no — defaults `8080`
- **Failure mode:** collision → the listen fails and is only printed as a stack trace; the process
  stays up. Must match the container's exposed port and the ingress/service definition.

#### `ssl` / `keystore` / `keystorePassword`

- **Required:** no — **dead keys.** `ApiServerVerticle` creates a plain `HttpServerOptions`
  (there is a literal `// TODO: ssl configuration` at the site). The server is **HTTP only**;
  TLS must terminate at the ingress. See §4.

#### `geomSpecificMaxLimits`

- **Type / format:** object; PostGIS geometry type → int (max features per request)
- **Required:** in practice yes — see the default below
- **Purpose:** Per-geometry-type cap on the `limit` parameter of the OGC Features API; written
  into the generated OpenAPI spec for each feature collection, so it is enforced by **spec
  validation**, not by handler code.
- **Expected value:** keys must be `PostgisGeomTypes` names — `GEOMETRY`, `POINT`, `LINESTRING`,
  `LINEARRING`, `POLYGON`, `MULTIPOINT`, `MULTILINESTRING`, `MULTIPOLYGON`, `GEOMETRYCOLLECTION`,
  `POLYHEDRALSURFACE`, `TRIANGLE`, `TIN`. Unknown keys are ignored.
- **Default if omitted:** **`5`** (`OgcFeaturesMetadata.OGC_LIMIT_PARAM_MAX_DEFAULT`), applied
  per type — so any geometry type left out of this object is capped at 5 features per request.
  That is almost never what you want; list every type you serve.
- **Failure mode:** too high → a single request can scan and serialize an unbounded number of
  features; too low → clients get 400s from spec validation on `limit`.
- **Notes / gotchas:** Because the value lands in the generated spec, changing it requires a spec
  regeneration (restart, or the `NOTIFY` trigger) to take effect.

#### `issuers` — the live JWT auth configuration

- **Type / format:** object keyed by issuer string; each issuer value has `type`, `jwksUrl`,
  `audience`; two scalar settings sit alongside at the same level
- **Required:** yes
- **Purpose:** The set of JWT issuers this server accepts. `JwksResolver` builds one `JWTAuth` per
  issuer from its JWKS, and `MultiIssuerJwtAuthHandler` is registered as the security handler for
  every operation that declares bearer security in the OpenAPI spec. A token whose `iss` has no
  entry here is rejected with `Unknown issuer`.
- **Expected value:** two entries — the controlplane issuer with its `/iudx/v2/auth/jwks`
  endpoint, and the Keycloak realm with its `protocol/openid-connect/certs` endpoint. The map key
  must match the token's `iss` claim **exactly** (the controlplane's is not a URL — no scheme).
- **Per-issuer keys:** `type` — `remote` (fetch JWKS over HTTPS) is the only supported value;
  anything else fails with `Unknown issuer type`. `jwksUrl` — required. `audience` — **no
  consumer**; empty arrays are shipped and the value is ignored (see §4).
- **Scalar settings inside the map:**
  - `jwtIgnoreExpiry` *(bool — feature flag)* — default `false`. **Never `true` in production.**
  - `jwtLeeway` *(int, seconds — tuning knob)* — clock-skew allowance, default `60`. The working
    dev config spells it `leeway`, which is dead; see §4.
- **Failure mode:** issuer missing → every request bearing that issuer's token is rejected.
  `jwksUrl` wrong or unreachable → signature validation fails for that issuer only, and because
  the resolver caches per issuer only on success, it retries on every request.
- **Notes / gotchas:** **The JWKS is cached forever.** There is no refresh — after a Keycloak key
  rotation, valid tokens are rejected until the server is restarted (see §4,
  `jwksRefreshIntervalMs`).
- **Change impact:** cross-service — entries must track the controlplane's issuer/JWKS route and
  the Keycloak realm.

#### `controlPanelHost` + `controlPanelPort` + `controlPanelSearchPath`

**Documented as a set — the controlplane access-check endpoint.**

- **Type / format:** string (**bare hostname, no scheme, no path**) / int / string (path)
- **Required:** yes — `controlPanelPort` is unboxed and read while the router is being built, so a
  missing key breaks router construction, and the HTTP server never starts.
- **Purpose:** `AclClient` POSTs `{"itemId": ...}` with the caller's bearer token to
  `https://host:port + controlPanelSearchPath`; a `200` means access granted. This is the
  authorization decision for secure collections, STAC assets and STAC items.
- **Example value:** `"controlplane.example.org"` / `443` /
  `"/controlplane/iudx/acl/apd/v2/access_request/has_access"`
- **Failure mode:** anything other than `200` is treated as **access denied** — an unreachable or
  misconfigured controlplane looks exactly like "user has no access", with a 401/403 to the client
  and no distinguishing log. Check this first when access suddenly fails for everyone.
- **Change impact:** cross-service — must match the controlplane's ACL/APD route.
- **Notes / gotchas:** Same bare-hostname rule as `catServerHost`; TLS is always on
  (`setSsl(true)`, `trustAll` off), so the certificate must be valid for this hostname.

#### `audience`

- **Type / format:** string; this server's identifier as it appears in a resource-server token
- **Required:** yes, for the provider-audit API
- **Purpose:** `ApiServerVerticle` substitutes it as the `iid` when a resource-server token calls
  `GET /ngsi-ld/v1/provider/audit`:
  `if (authInfo.isRsToken()) provider.put("iid", config().getString("audience"))`.
- **Example value:** `"ogc.example.org"`
- **Failure mode:** `null` → the provider-audit query filters on a null instance id and returns
  nothing for RS tokens.
- **Notes / gotchas:** A single string, unrelated to `issuers.*.audience` (which nothing reads).
  No audience validation happens during authentication.

### `ogc.rs.metering.MeteringVerticle`

Registers `MeteringService`; owns a **second** Postgres pool for the metering/auditing database
and publishes audit records to RabbitMQ.

#### `meteringDatabaseHost` / `meteringDatabasePort` / `meteringDatabaseUser` / `meteringDatabasePassword` / `meteringDatabaseName`

- **Type / format:** string / int (unboxed) / string / string (secret) / string
- **Required:** yes
- **Purpose:** The auditing/metering database — a **different database (and usually different
  credentials)** from the OGC one. The verticle opens both pools: the metering/overview read APIs
  query this database, while tile-usage rows are inserted into a `metering` table in the **OGC**
  database (so the OGC user needs insert rights on it too).
- **Privileges required:** read/write on the auditing tables (schema managed by
  `auditing-db_flyway.conf`).
- **Failure mode:** wrong credentials → the metering APIs fail at request time; the verticle
  still deploys.
- **Notes / gotchas:** Do not point these at the OGC database — they are separate on purpose, and
  the metering user typically has no rights on the OGC schema.

### `ogc.rs.jobs.JobsVerticle` / `ogc.rs.processes.ProcessVerticle`

Both take only `id`, `verticleInstances` and `poolSize`; everything else (`databaseHost` … ,
`hostName`, and for the pre-signed-URL process `catServerHost`/`catServerPort`/
`catRequestItemsUri`) arrives from `commonConfig`.

- **Notes / gotchas:** `ProcessVerticle` runs the GDAL-backed onboarding/appending processes,
  which shell out to `ogr2ogr` using the **OGC database** credentials and write to
  `/usr/share/app/storage/temp-dir` in the container. They are long-running; `Deployer` sets
  `maxWorkerExecuteTime` to 1 hour globally for this reason.

---

## 3. Extra requirements by field category

### Credentials

Four credential sets, each in a different system. Never reuse one across systems.

| Credential | System | Detail |
|---|---|---|
| `databaseUser` + `databasePassword` (`commonConfig`) | Postgres (OGC db) | read/write **plus DDL** — onboarding creates and drops tables |
| `meteringDatabaseUser` + `meteringDatabasePassword` | Postgres (auditing db) | read/write on the auditing tables; separate database |
| `s3BucketsConfig.*.accessKey` + `secretKey` | S3 / S3-compatible | get/put/delete on the bucket |
| `dataBrokerUserName` + `dataBrokerPassword` | RabbitMQ | publish on the internal vhost; `management` tag is enough |

There is no keystore secret — the server does not terminate TLS (see §4).

### Domains / URLs

Scheme and slash rules are enforced inconsistently — follow this table literally.

| Field | Scheme? | Must match |
|---|---|---|
| `hostName` | **yes**, no trailing slash | this server's public ingress |
| `catServerHost` | **no** — bare hostname, no path | controlplane catalogue ingress |
| `controlPanelHost` | **no** — bare hostname, no path | controlplane ACL/APD ingress |
| `dataBrokerIP` | no | in-cluster broker |
| `s3BucketsConfig.*.endpoint` | **yes** — `http`/`https`, validated at startup | S3 or MinIO endpoint |
| `issuers` keys and `issuer` | as issued in the token's `iss` claim | controlplane / Keycloak realm |
| `issuers.*.jwksUrl` | yes — full URL | issuer's JWKS endpoint |

**Cross-service values** — change in lockstep with the other service or break:
`catServerHost`+`dxCatalogueBasePath`+`catRequestItemsUri`, `controlPanelHost`+
`controlPanelSearchPath`, the `issuers` entries, the RabbitMQ vhosts, `auditingExchange` and
`auditingRoutingKey`,
and `hostName` (clients follow the links it produces).

### Tuning knobs

| Field | Default | Symptom if wrong |
|---|---|---|
| `verticleInstances` | — (required) | mismatch with `ApiServerVerticle` instances → router never built, server serves nothing |
| `poolSize` | — (required) | applied per instance; too high exhausts Postgres `max_connections` |
| `geomSpecificMaxLimits` | 5 per unlisted type | too high → unbounded feature scans; unlisted type → clients capped at `limit=5` |
| `issuers.jwtLeeway` | 60 s | clock skew rejects otherwise-valid tokens |
| RMQ timeouts / channel max / heartbeat | as shipped | reconnect storms, channel exhaustion, stalled audit messages |

### Feature flags

| Flag | Default | Note |
|---|---|---|
| `issuers.jwtIgnoreExpiry` | `false` | **never `true` in production** — this is the flag the route authentication uses |
| `commonConfig.jwtIgnoreExpiry` | `false` | no read site today; keep it `false` so it never contradicts the flag above |
| `ssl` | — | **dead** — the server is HTTP only; terminate TLS at the ingress |

Two **system properties** (not config keys) change behaviour drastically and must never be set in
production: `-Ddisable.auth` turns off all token authentication and authorization —
`RouterBuilderOptions.setRequireSecurityHandlers(false)`, so `MultiIssuerJwtAuthHandler` is applied
to nothing, and the AuthZ handlers wave every request through (`Deployer` logs it as fatal) — and
`-Dfake-token=true`, which unblocks the echo test process in `ProcessAuthZHandler`.

## 4. Findings — fields to resolve

These issues live in the **working config's shape** (and therefore in this synced example); fixing
them means changing `secrets/configs/dev.json` first, then re-syncing.

**Dead keys — present in the working config, read by nothing:**

| Field | Notes |
|---|---|
| `version` | Documentation-only marker |
| `zookeepers`, `clusterId` | No clustered deployer in this repo; `Deployer` builds a standalone Vert.x |
| `host` (top-level) | Copied into every module config by `Deployer`, never read. `ogc-rs-stack.yaml` also passes a `--host` flag the CLI does not declare |
| `modules[].isWorkerVerticle` | `Deployer` never calls `setWorker` |
| `ssl`, `keystore`, `keystorePassword` | `ApiServerVerticle` builds a plain `HttpServerOptions` — there is a `// TODO: ssl configuration` at the site. **The server does not serve HTTPS**, whatever this says |
| `s3Bucket`, `s3Endpoint`, `s3Region`, `s3AccessKey`, `s3SecretKey`, `s3PathBasedAccess` (on the `ApiServerVerticle` entry) | Superseded by the `s3BucketsConfig` block; nothing reads these names. Dropped from this example — they duplicate live credentials |
| `jwksRefreshIntervalMs` (both copies) | No read site. `JwksResolver` caches each issuer's `JWTAuth` **forever**; a Keycloak key rotation requires a restart |
| `issuers.leeway` | Wrong key name — the code reads `jwtLeeway` (default 60). The shipped `leeway: 30` is silently ignored |
| `issuers.<issuer>.audience` | Parsed nowhere; no audience checking during authentication. Not the same key as the module-level `audience`, which **is** read by the provider-audit API |
| `automaticRecoveryEnabled` | `DataBrokerVerticle` hardcodes `setAutomaticRecoveryEnabled(true)` |
| `externalVhost` | `DataBrokerVerticle` reads only `prodVhost` and `internalVhost` |
| `commonConfig.jwtIgnoreExpiry`, `commonConfig.issuer` | Read by `DxTokenAuthenticationHandler`, which is no longer constructed. Kept in the config; see §2 for how to keep them consistent with `issuers` |

**Structural problems in the working dev config:**

| Problem | Impact |
|---|---|
| Only one of the two audit publishers reads the config | Every request on `/collections/{id}/items`, `/collections/{id}/items/{featureId}` and `/assets/{assetId}` writes one Postgres row plus **two** RabbitMQ messages with different payloads: `MeteringServiceImpl` to `auditing` + `#` (hardcoded in `MeteringConstant`), `AuditingHandler` to `auditingExchange` + `auditingRoutingKey` (currently `test-auditing` + `##`). Both exchanges and bindings must exist or one stream is silently dropped. `MeteringVerticle` already receives the merged config, so `MeteringServiceImpl` could read the same two keys |
| The two publishers are order-coupled | `setAuditLog` — the only code that populates the `AuditLog` that `AuditingHandler` publishes — runs inside `auditAfterApiEnded`. Both register body-end handlers, and Vert.x runs those in **reverse** registration order, so `auditAfterApiEnded` (mounted second) runs first and `AuditingHandler` finds the log. Swapping the two `.handler(...)` lines on those routes, or removing `auditAfterApiEnded`, silently stops stream #3 with only a `No auditing log found in context` warning |

**Example values that must not ship to production:**

- `issuers.jwtIgnoreExpiry: true` — expired tokens accepted indefinitely, silently.
- The `-Ddisable.auth` and `-Dfake-token` system properties.
- All blanked credentials (`databasePassword`, `meteringDatabasePassword`, the S3 keys,
  `dataBrokerPassword`) and blanked hosts must be filled per environment — **the file will not
  boot as-is**: an empty `s3BucketsConfig` credential fails `S3ConfigsHolder` validation and the
  API server verticle does not deploy.

## 5. Submission checklist

- [x] Every leaf field in `config-example.json` has a block; the three `modules[]` scaffolding
      keys are documented once as a pattern.
- [x] Every credential documents its privileges and origin — OGC Postgres (DDL), metering
      Postgres, S3, RabbitMQ.
- [x] Cross-service fields are flagged (catalogue endpoint, ACL endpoint, issuer entries, RMQ
      vhosts/exchange, `hostName`).
- [x] Each field states its failure mode, including the two fail-fast ones (`s3BucketsConfig`,
      `hostName`) and the ones that only fail per request.
- [x] Example file synced to the working `secrets/configs/dev.json`, credentials blanked;
      deviations listed in §0 and remaining shape issues called out in §4.
