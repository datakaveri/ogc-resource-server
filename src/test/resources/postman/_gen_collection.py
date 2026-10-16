#!/usr/bin/env python3
"""Generator for the dx-dataplane-ogc Postman collection (v2.1.0).

Produces ~200 leaf requests covering token retrieval + every OGC/STAC/Metering
endpoint across the v2 auth matrix (no-auth / Bearer JWT / Basic AppId /
delegation / negative cases). Run:  python3 _gen_collection.py
"""
import json, uuid

# ----------------------------------------------------------------------------- helpers
def _id():
    return str(uuid.uuid4())

def test_script(lines):
    return {
        "listen": "test",
        "script": {"type": "text/javascript", "exec": lines},
    }

def status_test(name, code):
    """A simple pm.test asserting the HTTP status code."""
    return test_script([
        f"pm.test(\"{name} -> {code}\", function () {{",
        f"    pm.expect(pm.response.code).to.eql({code});",
        "});",
    ])

def status_one_of(name, codes):
    arr = ", ".join(str(c) for c in codes)
    return test_script([
        f"pm.test(\"{name} -> one of [{arr}]\", function () {{",
        f"    pm.expect(pm.response.code).to.be.oneOf([{arr}]);",
        "});",
    ])

def url_obj(path, query=None):
    # path is like "/collections/{{collectionId}}/items"
    segs = [s for s in path.split("/") if s != ""]
    u = {
        "raw": "{{baseUrl}}" + path + (("?" + "&".join(f"{k}={v}" for k, v in query.items())) if query else ""),
        "host": ["{{baseUrl}}"],
        "path": segs,
    }
    if query:
        u["query"] = [{"key": k, "value": str(v)} for k, v in query.items()]
    return u

# auth header variants -------------------------------------------------------
H_JSON = {"key": "Content-Type", "value": "application/json"}
H_BEARER = {"key": "Authorization", "value": "Bearer {{accessToken}}"}
H_BEARER_BAD = {"key": "Authorization", "value": "Bearer not-a-real-jwt.aaa.bbb"}
H_BEARER_EXPIRED = {"key": "Authorization", "value": "Bearer {{expiredToken}}"}
H_MALFORMED = {"key": "Authorization", "value": "{{accessToken}}"}  # missing "Bearer "
H_DID = {"key": "did", "value": "{{did}}"}

def basic_auth():
    return {
        "type": "basic",
        "basic": [
            {"key": "username", "value": "{{appId}}", "type": "string"},
            {"key": "password", "value": "{{appSecret}}", "type": "string"},
        ],
    }

def basic_auth_bad():
    return {
        "type": "basic",
        "basic": [
            {"key": "username", "value": "invalid-app-id", "type": "string"},
            {"key": "password", "value": "invalid-app-secret", "type": "string"},
        ],
    }

def req(name, method, path, *, code=None, codes=None, headers=None, query=None,
        body=None, auth=None, extra_test=None, note=None):
    headers = list(headers or [])
    request = {
        "method": method,
        "header": headers,
        "url": url_obj(path, query),
    }
    if auth is not None:
        request["auth"] = auth
    if body is not None:
        request["body"] = {"mode": "raw", "raw": json.dumps(body, indent=2),
                           "options": {"raw": {"language": "json"}}}
    # No pm.test assertions are added (per request). The expected status is recorded
    # only as a human-readable hint in the request description.
    expect = None
    if codes is not None:
        expect = "Expected status: one of " + ", ".join(str(c) for c in codes)
    elif code is not None:
        expect = f"Expected status: {code}"
    desc_parts = [p for p in (note, expect) if p]
    if desc_parts:
        request["description"] = "\n\n".join(desc_parts)
    item = {"name": name, "request": request, "response": []}
    # Only functional scripts (e.g. saving a captured jobId) are kept; never assertions.
    if extra_test is not None:
        item["event"] = [extra_test]
    return item

def folder(name, items, description=None):
    f = {"name": name, "item": items}
    if description:
        f["description"] = description
    return f

# ----------------------------------------------------------------------------- 00 · Auth & Token
# Functional only (no assertions): capture the token into collection variables.
save_kc_token = test_script([
    "var j = {};",
    "try { j = pm.response.json(); } catch (e) {}",
    "if (j.access_token) {",
    "    pm.collectionVariables.set(\"accessToken\", j.access_token);",
    "    console.log(\"saved accessToken\");",
    "}",
    "if (j.refresh_token) { pm.collectionVariables.set(\"refreshToken\", j.refresh_token); }",
])

save_service_token = test_script([
    "try { var j = pm.response.json(); if (j.access_token) pm.collectionVariables.set(\"serviceToken\", j.access_token); } catch (e) {}",
])

def kc_url():
    return {"raw": "{{keycloakTokenUrl}}", "host": ["{{keycloakTokenUrl}}"]}

def kc_form(fields):
    return {"mode": "urlencoded",
            "urlencoded": [{"key": k, "value": v} for k, v in fields]}

def kc_req(name, fields, event=None, note=None):
    request = {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/x-www-form-urlencoded"}],
        "body": kc_form(fields),
        "url": kc_url(),
    }
    if note:
        request["description"] = note
    item = {"name": name, "request": request, "response": []}
    if event is not None:
        item["event"] = [event]   # functional capture only, never assertions
    return item

auth_items = [
    kc_req("Keycloak · Get User Token (password grant) ⭐",
           [("grant_type", "password"), ("client_id", "{{clientId}}"),
            ("client_secret", "{{clientSecret}}"), ("username", "{{username}}"),
            ("password", "{{password}}"), ("scope", "openid")],
           save_kc_token),
    kc_req("Keycloak · Get Service Token (client_credentials)",
           [("grant_type", "client_credentials"), ("client_id", "{{clientId}}"),
            ("client_secret", "{{clientSecret}}")],
           save_service_token),
    kc_req("Keycloak · Refresh Token",
           [("grant_type", "refresh_token"), ("client_id", "{{clientId}}"),
            ("client_secret", "{{clientSecret}}"), ("refresh_token", "{{refreshToken}}")],
           save_kc_token),
    kc_req("Keycloak · Wrong password (expect 401)",
           [("grant_type", "password"), ("client_id", "{{clientId}}"),
            ("client_secret", "{{clientSecret}}"), ("username", "{{username}}"),
            ("password", "wrong-password")],
           note="Expected status: 401"),
    kc_req("Keycloak · Unknown client (expect 401)",
           [("grant_type", "password"), ("client_id", "no-such-client"),
            ("client_secret", "x"), ("username", "{{username}}"), ("password", "{{password}}")],
           note="Expected status: one of 400, 401"),
    kc_req("Keycloak · Missing grant_type (expect 400)",
           [("client_id", "{{clientId}}"), ("client_secret", "{{clientSecret}}")],
           note="Expected status: 400"),
]

# A DX-AAA style token request kept as an optional alternative flow
auth_items.append({
    "name": "DX-AAA · Get Resource Token (alternative flow)",
    "request": {
        "method": "POST",
        "header": [H_JSON, {"key": "Authorization", "value": "Bearer {{keycloakIdToken}}"}],
        "body": {"mode": "raw", "raw": json.dumps({
            "itemId": "{{itemId}}", "itemType": "resource", "role": "{{role}}"
        }, indent=2), "options": {"raw": {"language": "json"}}},
        "url": {"raw": "{{authBaseUrl}}/auth/v1/token", "host": ["{{authBaseUrl}}"],
                "path": ["auth", "v1", "token"]},
    },
    "response": [],
    "event": [test_script([
        "if (pm.response.code === 200) {",
        "  try { var j = pm.response.json(); var t = (j.results && j.results.accessToken) || j.accessToken;",
        "  if (t) pm.collectionVariables.set(\"accessToken\", t); } catch(e){} }",
    ])],
})

# ----------------------------------------------------------------------------- endpoint catalogue
# Each entry: (name, method, path, secured, has_query, body)
OPEN_DISCOVERY = [
    ("Landing page", "GET", "/", False),
    ("OGC OpenAPI spec", "GET", "/api", False),
    ("Conformance classes", "GET", "/conformance", False),
    ("List collections", "GET", "/collections", False),
    ("Describe collection", "GET", "/collections/{{collectionId}}", False),
    ("Coverage schema", "GET", "/collections/{{collectionId}}/schema", False),
    ("Tile matrix sets list", "GET", "/tileMatrixSets", False),
    ("Tile matrix set", "GET", "/tileMatrixSets/{{tileMatrixSetId}}", False),
    ("Tileset list (collection)", "GET", "/collections/{{collectionId}}/map/tiles", False),
    ("Tileset (collection)", "GET", "/collections/{{collectionId}}/map/tiles/{{tileMatrixSetId}}", False),
    ("List processes", "GET", "/processes", False),
    ("Describe process", "GET", "/processes/{{processId}}", False),
    ("List configured S3 buckets", "GET", "/listConfiguredS3Buckets", False),
    ("Record catalog", "GET", "/collections/{{recordCatalogId}}", False),
    ("Record items search", "GET", "/collections/{{recordCatalogId}}/items", False),
    ("Record item by id", "GET", "/collections/{{recordCatalogId}}/items/{{recordId}}", False),
    ("STAC landing page", "GET", "/stac", False),
    ("STAC OpenAPI spec", "GET", "/stac/api", False),
    ("STAC conformance", "GET", "/stac/conformance", False),
    ("STAC list collections", "GET", "/stac/collections", False),
    ("STAC describe collection", "GET", "/stac/collections/{{collectionId}}", False),
]
N_OGC_DISCOVERY = 16  # first N entries above are OGC; the rest are STAC discovery

# secured data endpoints: (name, method, path, query, body)
SECURED = [
    ("Get features", "GET", "/collections/{{collectionId}}/items",
     {"limit": "10", "offset": "0"}, None),
    ("Get single feature", "GET", "/collections/{{collectionId}}/items/{{featureId}}", None, None),
    ("Get collection coverage", "GET", "/collections/{{collectionId}}/coverage", None, None),
    ("Get tile (z/x/y)", "GET",
     "/collections/{{collectionId}}/map/tiles/{{tileMatrixSetId}}/{{tileMatrix}}/{{tileRow}}/{{tileCol}}",
     None, None),
    ("STAC features (items)", "GET", "/stac/collections/{{collectionId}}/items",
     {"limit": "10"}, None),
    ("STAC get asset", "GET", "/assets/{{assetId}}", None, None),
]

EXECUTE_BODY = {
    "inputs": {"collectionId": "{{collectionId}}", "fileType": "geojson"},
    "response": "raw",
}

METERING = [
    ("Metering summary", "GET", "/ngsi-ld/v1/summary",
     {"starttime": "2024-01-01T00:00:00Z", "endtime": "2024-12-31T00:00:00Z"}),
    ("Metering monthly overview", "GET", "/ngsi-ld/v1/overview", None),
    ("Provider audit detail", "GET", "/ngsi-ld/v1/provider/audit",
     {"timerelation": "during", "starttime": "2024-01-01T00:00:00Z",
      "endtime": "2024-12-31T00:00:00Z"}),
    ("Consumer audit detail", "GET", "/ngsi-ld/v1/consumer/audit",
     {"timerelation": "during", "starttime": "2024-01-01T00:00:00Z",
      "endtime": "2024-12-31T00:00:00Z"}),
]

# ----------------------------------------------------------------------------- build folders
def discovery_variants(name, method, path):
    """3 cases per open/discovery endpoint."""
    out = []
    is_param = "{{" in path
    out.append(req(f"{name} · no auth (happy path)", method, path, code=200,
                   note="Discovery/open endpoint should not require a token."))
    out.append(req(f"{name} · with Bearer (still 200)", method, path, code=200,
                   headers=[H_BEARER],
                   note="Providing a valid token must not break open endpoints."))
    if is_param:
        bad = path.replace("{{collectionId}}", "00000000-0000-0000-0000-000000000000") \
                  .replace("{{tileMatrixSetId}}", "NoSuchTMS") \
                  .replace("{{processId}}", "no-such-process") \
                  .replace("{{recordCatalogId}}", "00000000-0000-0000-0000-000000000000") \
                  .replace("{{recordId}}", "no-such-record")
        if bad != path:
            out.append(req(f"{name} · unknown id (404)", method, bad, codes=[404, 400]))
    return out

def secured_variants(name, method, path, query, body):
    """The full auth matrix per secured endpoint (~10 cases)."""
    out = []
    common = dict(query=query, body=body)
    # 1 no auth -> 401
    out.append(req(f"{name} · no auth (401)", method, path, code=401, **common,
                   note="Restricted item with no credentials must be rejected by the AuthenticationHandler."))
    # 2 valid Bearer JWT -> 200 (open dataset) / depends; accept 200/403
    out.append(req(f"{name} · Bearer JWT (granted)", method, path, codes=[200, 206],
                   headers=[H_BEARER], **common,
                   note="Valid JWT + granted CheckItemAccess. Use an OPEN or granted dataset id."))
    # 3 Bearer on restricted, no access -> 403
    out.append(req(f"{name} · Bearer JWT, no item access (403)", method,
                   path.replace("{{collectionId}}", "{{restrictedCollectionId}}"),
                   code=403, headers=[H_BEARER], **common,
                   note="Valid token but CheckItemAccess denies -> 403."))
    # 4 invalid/garbage bearer -> 401
    out.append(req(f"{name} · invalid Bearer (401)", method, path, code=401,
                   headers=[H_BEARER_BAD], **common))
    # 5 expired bearer -> 401
    out.append(req(f"{name} · expired Bearer (401)", method, path, code=401,
                   headers=[H_BEARER_EXPIRED], **common))
    # 6 malformed Authorization (no scheme) -> 401
    out.append(req(f"{name} · malformed Authorization (401)", method, path, code=401,
                   headers=[H_MALFORMED], **common))
    # 7 Basic AppId valid -> 200
    out.append(req(f"{name} · Basic AppId (granted)", method, path, codes=[200, 206],
                   auth=basic_auth(), **common,
                   note="Basic appId:appSecret -> GrpcAppCredentialsResolver -> VerifyAppId."))
    # 8 Basic AppId invalid -> 401
    out.append(req(f"{name} · Basic AppId invalid (401)", method, path, code=401,
                   auth=basic_auth_bad(), **common))
    # 9 Bearer + did delegation (KNOWN GAP) -> currently denied
    out.append(req(f"{name} · Bearer + did delegation (KNOWN GAP)", method, path,
                   codes=[200, 403], headers=[H_BEARER, H_DID], **common,
                   note="KNOWN BUG: OGC sends (delegator,delegator) to CheckItemAccess -> "
                        "delegated access wrongly denied (403). Expected 200 once the "
                        "delegated-item-access bridge is applied. See ogc-delegated-item-access-gap."))
    # 10 Basic + did delegation
    out.append(req(f"{name} · Basic AppId + did delegation (KNOWN GAP)", method, path,
                   codes=[200, 403], auth=basic_auth(), headers=[H_DID], **common))
    return out

# OGC core / features / tiles folder
core_items = []
for name, method, path, _sec in OPEN_DISCOVERY[:N_OGC_DISCOVERY]:
    core_items.extend(discovery_variants(name, method, path))

# extra records-search variants (q= full-text, bbox, datetime — OGC API Records)
core_items.append(req("Record items search · q full-text", "GET",
                      "/collections/{{recordCatalogId}}/items", codes=[200, 404],
                      query={"q": "water", "limit": "5"},
                      note="Full-text search over record metadata (tsvector-backed q= param)."))
core_items.append(req("Record items search · bbox filter", "GET",
                      "/collections/{{recordCatalogId}}/items", codes=[200, 404],
                      query={"bbox": "68.0,6.0,98.0,36.0", "limit": "5"}))

stac_disc_items = []
for name, method, path, _sec in OPEN_DISCOVERY[N_OGC_DISCOVERY:]:
    stac_disc_items.extend(discovery_variants(name, method, path))

secured_ogc_items = []
for name, method, path, q, b in SECURED[:4]:
    secured_ogc_items.extend(secured_variants(name, method, path, q, b))
    # bad query param case for endpoints with query
    if q:
        bad_q = dict(q); bad_q["limit"] = "-5"
        secured_ogc_items.append(req(f"{name} · invalid query param (400)", method, path,
                                     code=400, headers=[H_BEARER], query=bad_q))
        bad_bbox = dict(q); bad_bbox["bbox"] = "10,20"  # too few coords
        secured_ogc_items.append(req(f"{name} · malformed bbox (400)", method, path,
                                     code=400, headers=[H_BEARER], query=bad_bbox))

secured_stac_items = []
for name, method, path, q, b in SECURED[4:]:
    secured_stac_items.extend(secured_variants(name, method, path, q, b))

# Processes / Jobs folder
proc_items = []
proc_items.append(req("Execute process · no auth (401)", "POST",
                      "/processes/{{processId}}/execution", code=401,
                      headers=[H_JSON], body=EXECUTE_BODY))
proc_items.append(req("Execute process · Bearer (started)", "POST",
                     "/processes/{{processId}}/execution", codes=[200, 201, 202],
                     headers=[H_JSON, H_BEARER], body=EXECUTE_BODY,
                     extra_test=test_script([
                         "try { var j = pm.response.json();",
                         "  var id = j.jobId || j.id; if (id) pm.collectionVariables.set('jobId', id);",
                         "} catch(e){}"])))
proc_items.append(req("Execute process · invalid Bearer (401)", "POST",
                     "/processes/{{processId}}/execution", code=401,
                     headers=[H_JSON, H_BEARER_BAD], body=EXECUTE_BODY))
proc_items.append(req("Execute process · Basic AppId (started)", "POST",
                     "/processes/{{processId}}/execution", codes=[200, 201, 202],
                     headers=[H_JSON], auth=basic_auth(), body=EXECUTE_BODY))
proc_items.append(req("Execute process · empty body (400)", "POST",
                     "/processes/{{processId}}/execution", codes=[400, 422],
                     headers=[H_JSON, H_BEARER], body={}))
proc_items.append(req("Execute process · unknown process (404)", "POST",
                     "/processes/no-such-process/execution", codes=[404, 400],
                     headers=[H_JSON, H_BEARER], body=EXECUTE_BODY))
proc_items.append(req("Job status · no auth (401)", "GET", "/jobs/{{jobId}}", code=401))
proc_items.append(req("Job status · Bearer (200)", "GET", "/jobs/{{jobId}}",
                     codes=[200, 404], headers=[H_BEARER]))
proc_items.append(req("Job status · invalid Bearer (401)", "GET", "/jobs/{{jobId}}",
                     code=401, headers=[H_BEARER_BAD]))
proc_items.append(req("Job status · unknown job (404)", "GET",
                     "/jobs/00000000-0000-0000-0000-000000000000", codes=[404, 400],
                     headers=[H_BEARER]))
proc_items.append(req("List all jobs · no auth (401)", "GET", "/jobs", code=401))
proc_items.append(req("List all jobs · Bearer (200)", "GET", "/jobs", code=200,
                     headers=[H_BEARER],
                     note="Lists only the caller's own jobs (user_id scoped)."))
proc_items.append(req("Job results · no auth (401)", "GET", "/jobs/{{jobId}}/results",
                     code=401))
proc_items.append(req("Job results · Bearer", "GET", "/jobs/{{jobId}}/results",
                     codes=[200, 404],
                     headers=[H_BEARER],
                     note="Returns the stored output once the job is SUCCESSFUL; 404 before."))
proc_items.append(req("Job results · unknown job (404)", "GET",
                     "/jobs/00000000-0000-0000-0000-000000000000/results", codes=[404, 400],
                     headers=[H_BEARER]))

# Metering folder
metering_items = []
for name, method, path, q in METERING:
    metering_items.append(req(f"{name} · no auth (401)", method, path, code=401, query=q))
    metering_items.append(req(f"{name} · Bearer (200)", method, path, codes=[200, 204],
                             headers=[H_BEARER], query=q))
    metering_items.append(req(f"{name} · invalid Bearer (401)", method, path, code=401,
                             headers=[H_BEARER_BAD], query=q))
    metering_items.append(req(f"{name} · Basic AppId (200)", method, path, codes=[200, 204],
                             auth=basic_auth(), query=q))
    if q:
        bad = dict(q)
        # break the time window
        if "endtime" in bad:
            bad["endtime"] = "not-a-date"
        metering_items.append(req(f"{name} · bad time param (400)", method, path,
                                 codes=[400], headers=[H_BEARER], query=bad))

# Negative / smoke folder (extra coverage)
smoke_items = [
    req("Unknown route (404)", "GET", "/this/does/not/exist", code=404),
    req("Wrong method on landing (405)", "POST", "/", codes=[404, 405]),
    req("Health/landing reachability", "GET", "/", code=200),
    req("Features · only did header, no Authorization (401)", "GET",
        "/collections/{{collectionId}}/items", code=401, headers=[H_DID]),
    req("Features · both Bearer and Basic (ambiguous 400/401)", "GET",
        "/collections/{{collectionId}}/items", codes=[400, 401],
        headers=[H_BEARER], auth=basic_auth()),
    req("Get feature · unknown collection (404)", "GET",
        "/collections/00000000-0000-0000-0000-000000000000/items/{{featureId}}",
        codes=[404, 401], headers=[H_BEARER]),
    req("Get feature · unknown feature (404)", "GET",
        "/collections/{{collectionId}}/items/does-not-exist", codes=[404, 401],
        headers=[H_BEARER]),
    req("STAC asset · no auth (401)", "GET", "/assets/{{assetId}}", code=401),
    req("Tile · out-of-range z/x/y (400/404)", "GET",
        "/collections/{{collectionId}}/map/tiles/{{tileMatrixSetId}}/99/99999/99999",
        codes=[400, 404, 401], headers=[H_BEARER]),
    req("CORS preflight (OPTIONS) on features", "OPTIONS",
        "/collections/{{collectionId}}/items", codes=[200, 204, 401, 404],
        headers=[{"key": "Origin", "value": "https://example.com"},
                 {"key": "Access-Control-Request-Method", "value": "GET"}]),
    req("Features · unsupported Accept (406)", "GET", "/collections/{{collectionId}}/items",
        codes=[406, 200], headers=[H_BEARER, {"key": "Accept", "value": "application/xml"}]),
    req("Features · empty did with Bearer", "GET", "/collections/{{collectionId}}/items",
        codes=[200, 403, 400], headers=[H_BEARER, {"key": "did", "value": ""}]),
    req("Asset · invalid Bearer (401)", "GET", "/assets/{{assetId}}", code=401,
        headers=[H_BEARER_BAD]),
]

# ----------------------------------------------------------------------------- STAC write & search (provider)
STAC_COLL = {"type": "Collection", "id": "{{collectionId}}",
             "stac_version": "1.0.0", "description": "test", "license": "various",
             "extent": {"spatial": {"bbox": [[-180, -90, 180, 90]]},
                        "temporal": {"interval": [["2020-01-01T00:00:00Z", None]]}},
             "links": []}
STAC_ITEM = {"type": "Feature", "id": "{{stacItemId}}", "stac_version": "1.0.0",
             "geometry": {"type": "Point", "coordinates": [77.5, 12.9]},
             "properties": {"datetime": "2024-01-01T00:00:00Z"},
             "assets": {}, "links": []}

stac_write_items = []
# postStacCollection
stac_write_items.append(req("Create STAC collection · no auth (401)", "POST",
                            "/stac/collections", code=401, headers=[H_JSON], body=STAC_COLL))
stac_write_items.append(req("Create STAC collection · Bearer (provider)", "POST",
                            "/stac/collections", codes=[200, 201, 403], headers=[H_JSON, H_BEARER],
                            body=STAC_COLL,
                            note="Provider-only (ownership). 403 if the token is not the provider."))
stac_write_items.append(req("Create STAC collection · Basic AppId", "POST",
                            "/stac/collections", codes=[200, 201, 403], headers=[H_JSON],
                            auth=basic_auth(), body=STAC_COLL))
stac_write_items.append(req("Create STAC collection · invalid Bearer (401)", "POST",
                            "/stac/collections", code=401, headers=[H_JSON, H_BEARER_BAD],
                            body=STAC_COLL))
stac_write_items.append(req("Create STAC collection · bad body (400)", "POST",
                            "/stac/collections", codes=[400, 422], headers=[H_JSON, H_BEARER],
                            body={"bad": True}))
# updateStacCollection (PUT)
stac_write_items.append(req("Update STAC collection · no auth (401)", "PUT",
                            "/stac/collections/{{collectionId}}", code=401, headers=[H_JSON],
                            body=STAC_COLL))
stac_write_items.append(req("Update STAC collection · Bearer (provider)", "PUT",
                            "/stac/collections/{{collectionId}}", codes=[200, 204, 403],
                            headers=[H_JSON, H_BEARER], body=STAC_COLL))
stac_write_items.append(req("Update STAC collection · unknown id (404)", "PUT",
                            "/stac/collections/00000000-0000-0000-0000-000000000000",
                            codes=[404, 403, 400], headers=[H_JSON, H_BEARER], body=STAC_COLL))
# getStacItemById
stac_write_items.append(req("Get STAC item by id · no auth (401)", "GET",
                            "/stac/collections/{{collectionId}}/items/{{stacItemId}}", code=401))
stac_write_items.append(req("Get STAC item by id · Bearer", "GET",
                            "/stac/collections/{{collectionId}}/items/{{stacItemId}}",
                            codes=[200, 404], headers=[H_BEARER]))
stac_write_items.append(req("Get STAC item by id · unknown item (404)", "GET",
                            "/stac/collections/{{collectionId}}/items/does-not-exist",
                            codes=[404, 401], headers=[H_BEARER]))
# postStacItem (create)
stac_write_items.append(req("Create STAC item · no auth (401)", "POST",
                            "/stac/collections/{{collectionId}}/items", code=401,
                            headers=[H_JSON], body=STAC_ITEM))
stac_write_items.append(req("Create STAC item · Bearer (provider)", "POST",
                            "/stac/collections/{{collectionId}}/items", codes=[200, 201, 403],
                            headers=[H_JSON, H_BEARER], body=STAC_ITEM))
stac_write_items.append(req("Create STAC item · bad body (400)", "POST",
                            "/stac/collections/{{collectionId}}/items", codes=[400, 422],
                            headers=[H_JSON, H_BEARER], body={"bad": True}))
# updateStacItem (PATCH)
stac_write_items.append(req("Update STAC item · no auth (401)", "PATCH",
                            "/stac/collections/{{collectionId}}/items/{{stacItemId}}", code=401,
                            headers=[H_JSON], body=STAC_ITEM))
stac_write_items.append(req("Update STAC item · Bearer (provider)", "PATCH",
                            "/stac/collections/{{collectionId}}/items/{{stacItemId}}",
                            codes=[200, 204, 403, 404], headers=[H_JSON, H_BEARER], body=STAC_ITEM))
# deleteStacItem (DELETE)
stac_write_items.append(req("Delete STAC item · no auth (401)", "DELETE",
                            "/stac/collections/{{collectionId}}/items/{{stacItemId}}", code=401))
stac_write_items.append(req("Delete STAC item · Bearer (provider)", "DELETE",
                            "/stac/collections/{{collectionId}}/items/{{stacItemId}}",
                            codes=[200, 204, 403, 404], headers=[H_BEARER]))
stac_write_items.append(req("Delete STAC item · unknown item (404)", "DELETE",
                            "/stac/collections/{{collectionId}}/items/does-not-exist",
                            codes=[404, 403, 401], headers=[H_BEARER]))
# search GET /stac/search
stac_write_items.append(req("STAC search (GET) · no auth (401)", "GET", "/stac/search",
                            codes=[200, 401], query={"limit": "10"},
                            note="Search may be open or token-gated depending on config."))
stac_write_items.append(req("STAC search (GET) · Bearer", "GET", "/stac/search",
                            codes=[200], headers=[H_BEARER], query={"limit": "10"}))
stac_write_items.append(req("STAC search (GET) · with bbox", "GET", "/stac/search",
                            codes=[200, 400], headers=[H_BEARER],
                            query={"bbox": "-180,-90,180,90", "limit": "5"}))
# search POST /stac/search
stac_write_items.append(req("STAC search (POST) · no auth", "POST", "/stac/search",
                            codes=[200, 401], headers=[H_JSON],
                            body={"limit": 10, "collections": ["{{collectionId}}"]}))
stac_write_items.append(req("STAC search (POST) · Bearer", "POST", "/stac/search",
                            codes=[200], headers=[H_JSON, H_BEARER],
                            body={"limit": 10, "bbox": [-180, -90, 180, 90],
                                  "collections": ["{{collectionId}}"]}))
stac_write_items.append(req("STAC search (POST) · bad body (400)", "POST", "/stac/search",
                            codes=[400, 422, 200], headers=[H_JSON, H_BEARER],
                            body={"limit": "not-a-number"}))

# ----------------------------------------------------------------------------- Features query matrix
FPATH = "/collections/{{collectionId}}/items"
feat_matrix = [
    req("Features · bbox filter", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"bbox": "77.0,12.0,78.0,13.0", "limit": "10"}),
    req("Features · bbox + bbox-crs", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"bbox": "77.0,12.0,78.0,13.0",
               "bbox-crs": "http://www.opengis.net/def/crs/OGC/1.3/CRS84"}),
    req("Features · datetime instant", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"datetime": "2024-01-01T00:00:00Z"}),
    req("Features · datetime closed interval", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"datetime": "2024-01-01T00:00:00Z/2024-12-31T23:59:59Z"}),
    req("Features · datetime open-start interval", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"datetime": "../2024-12-31T23:59:59Z"}),
    req("Features · datetime open-end interval", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"datetime": "2024-01-01T00:00:00Z/.."}),
    req("Features · limit=1", "GET", FPATH, codes=[200], headers=[H_BEARER], query={"limit": "1"}),
    req("Features · limit at max", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"limit": "10000"}),
    req("Features · limit=0 (400)", "GET", FPATH, code=400, headers=[H_BEARER], query={"limit": "0"}),
    req("Features · negative offset (400)", "GET", FPATH, code=400, headers=[H_BEARER],
        query={"offset": "-1"}),
    req("Features · offset paging", "GET", FPATH, codes=[200], headers=[H_BEARER],
        query={"limit": "5", "offset": "5"}),
    req("Features · crs param", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"crs": "http://www.opengis.net/def/crs/EPSG/0/4326"}),
    req("Features · invalid crs (400)", "GET", FPATH, codes=[400], headers=[H_BEARER],
        query={"crs": "not-a-crs"}),
    req("Features · combined bbox+datetime+limit", "GET", FPATH, codes=[200, 400], headers=[H_BEARER],
        query={"bbox": "77.0,12.0,78.0,13.0", "datetime": "2024-01-01T00:00:00Z/..", "limit": "20"}),
    req("Features · f=application/geo+json", "GET", FPATH, codes=[200], headers=[H_BEARER],
        query={"f": "application/geo+json"}),
]

# ----------------------------------------------------------------------------- Content negotiation
GEOJSON = {"key": "Accept", "value": "application/geo+json"}
JSONA = {"key": "Accept", "value": "application/json"}
HTMLA = {"key": "Accept", "value": "text/html"}
PNGA = {"key": "Accept", "value": "image/png"}
MVTA = {"key": "Accept", "value": "application/vnd.mapbox-vector-tile"}
TPATH = "/collections/{{collectionId}}/map/tiles/{{tileMatrixSetId}}/{{tileMatrix}}/{{tileRow}}/{{tileCol}}"
content_neg = [
    req("Landing · Accept application/json", "GET", "/", codes=[200, 406], headers=[JSONA]),
    req("Landing · Accept text/html", "GET", "/", codes=[200, 406], headers=[HTMLA]),
    req("Conformance · Accept application/json", "GET", "/conformance", codes=[200, 406], headers=[JSONA]),
    req("Collections · Accept application/json", "GET", "/collections", codes=[200, 406], headers=[JSONA]),
    req("Describe collection · Accept application/json", "GET", "/collections/{{collectionId}}",
        codes=[200, 406, 404], headers=[JSONA]),
    req("Processes · Accept application/json", "GET", "/processes", codes=[200, 406], headers=[JSONA]),
    req("Tile matrix sets · Accept application/json", "GET", "/tileMatrixSets",
        codes=[200, 406], headers=[JSONA]),
    req("Features · Accept application/geo+json", "GET", FPATH, codes=[200, 406],
        headers=[H_BEARER, GEOJSON], query={"limit": "5"}),
    req("STAC landing · Accept application/json", "GET", "/stac", codes=[200, 406], headers=[JSONA]),
    req("STAC collections · Accept application/json", "GET", "/stac/collections",
        codes=[200, 406], headers=[JSONA]),
    req("Tile · Accept image/png", "GET", TPATH, codes=[200, 404, 400, 406],
        headers=[H_BEARER, PNGA]),
    req("Tile · Accept mvt", "GET", TPATH, codes=[200, 404, 400, 406],
        headers=[H_BEARER, MVTA]),
]

# ----------------------------------------------------------------------------- 11 · Provider workflow
# End-to-end onboarding walkthrough with the REAL input bodies of every process
# (keys match processes_table exactly, incl. s3BucketIdentifier added by V22_2).
# Needs only a plain provider Bearer token in {{accessToken}}.

def pid(title):
    """Process ids differ per deployment; Step 0 captures them as pid_<Title> vars."""
    return "{{pid_" + title + "}}"

capture_pids = test_script([
    "try { var j = pm.response.json();",
    "  (j.processes || []).forEach(function (p) {",
    "    if (p.title && p.id) pm.collectionVariables.set('pid_' + p.title, p.id);",
    "  });",
    "  console.log('captured pid_<Title> variables for all processes');",
    "} catch (e) {}",
])
capture_presign = test_script([
    "try { var j = pm.response.json();",
    "  if (j.S3PreSignedUrl) pm.collectionVariables.set('s3PresignedUrl', j.S3PreSignedUrl);",
    "  if (j.s3ObjectKeyName) pm.collectionVariables.set('s3ObjectKeyName', j.s3ObjectKeyName);",
    "} catch (e) {}",
])
capture_job = test_script([
    "try { var j = pm.response.json();",
    "  var id = j.jobId || j.id; if (id) pm.collectionVariables.set('jobId', id);",
    "} catch (e) {}",
])
capture_upload = test_script([
    "try { var j = pm.response.json();",
    "  if (j.uploadId) pm.collectionVariables.set('uploadId', j.uploadId);",
    "  if (j.presignedUrls && j.presignedUrls.length)",
    "    pm.collectionVariables.set('s3PartUrl', j.presignedUrls[0].url);",
    "} catch (e) {}",])

def raw_url_req(name, method, raw_url, *, note=None, body_mode=None):
    """Request to an absolute (non-{{baseUrl}}) URL, e.g. a presigned S3 URL."""
    request = {"method": method, "header": [],
               "url": {"raw": raw_url, "host": [raw_url]}}
    if body_mode == "file":
        request["body"] = {"mode": "file", "file": {"src": ""}}
    if note:
        request["description"] = note
    return {"name": name, "request": request, "response": []}

provider_flow = [
    req("Step 0 · List processes (auto-captures pid_* vars) ⭐", "GET", "/processes",
        code=200, extra_test=capture_pids,
        note="RUN THIS FIRST. Saves each process id into pid_<Title> collection variables "
             "so the steps below work without hand-copying UUIDs."),
    # --- A. Feature (vector) onboarding: Story 1 of DEVELOPER_GUIDE_2 §12 ---
    req("Step 1 · S3PreSignedURLGeneration (SYNC) — get upload URL", "POST",
        f"/processes/{pid('S3PreSignedURLGeneration')}/execution", codes=[200, 403, 409],
        headers=[H_JSON, H_BEARER], extra_test=capture_presign,
        body={"inputs": {"resourceId": "{{resourceId}}", "fileType": "geopackage",
                          "s3BucketIdentifier": "{{s3BucketIdentifier}}"}},
        note="Provider token required. 409 if the resource is already onboarded. "
             "Saves {{s3PresignedUrl}} and {{s3ObjectKeyName}} (5-minute expiry!)."),
    raw_url_req("Step 2 · Upload GeoPackage directly to S3 (PUT presigned URL)", "PUT",
                "{{s3PresignedUrl}}", body_mode="file",
                note="Select your .gpkg file in Body → binary. Must run within 5 min of "
                     "Step 1. Expected status: 200 (from S3, not the OGC server)."),
    req("Step 3 · CollectionOnboarding (ASYNC) — GeoPackage → live collection", "POST",
        f"/processes/{pid('CollectionOnboarding')}/execution", codes=[201, 403],
        headers=[H_JSON, H_BEARER], extra_test=capture_job,
        body={"inputs": {"resourceId": "{{resourceId}}", "title": "My dataset",
                          "description": "Onboarded via Postman walkthrough",
                          "fileName": "{{s3ObjectKeyName}}", "version": "1.0.0",
                          "s3BucketIdentifier": "{{s3BucketIdentifier}}"}},
        note="201 + Location: /jobs/{jobId}. Saves {{jobId}} for polling."),
    req("Step 4 · Poll job until SUCCESSFUL", "GET", "/jobs/{{jobId}}", code=200,
        headers=[H_BEARER],
        note="Re-send until status becomes SUCCESSFUL (watch progress/message). "
             "On failure the message column tells you which onboarding step broke."),
    req("Step 5 · Verify: describe the new collection", "GET",
        "/collections/{{resourceId}}", code=200,
        note="Router auto-refreshed via NOTIFY — the collection endpoints now exist."),
    req("Step 6 · Verify: query its features", "GET",
        "/collections/{{resourceId}}/items", codes=[200, 401],
        headers=[H_BEARER], query={"limit": "5"}),
    req("Step 7 · CollectionAppending (ASYNC) — add more data (optional)", "POST",
        f"/processes/{pid('CollectionAppending')}/execution", codes=[201, 403],
        headers=[H_JSON, H_BEARER], extra_test=capture_job,
        body={"inputs": {"resourceId": "{{resourceId}}", "title": "My dataset",
                          "description": "Appending new file",
                          "fileName": "{{s3ObjectKeyName}}", "version": "1.0.0",
                          "s3BucketIdentifier": "{{s3BucketIdentifier}}"}},
        note="Validates schema+CRS of the new file match, then ogr2ogr -append."),
    # --- B. Tiles from that feature collection: Story 1 step 6-7 ---
    req("Step 8 · TilesOnboardingFromExistingFeature (ASYNC)", "POST",
        f"/processes/{pid('TilesOnboardingFromExistingFeature')}/execution",
        codes=[201, 403], headers=[H_JSON, H_BEARER], extra_test=capture_job,
        body={"inputs": {"resourceId": "{{resourceId}}", "minZoomLevel": "0",
                          "maxZoomLevel": "8", "pointOfOrigin": "[-180, 90]",
                          "s3BucketIdentifier": "{{s3BucketIdentifier}}"}},
        note="Server-side GDAL generates the MVT pyramid into S3 (WorldCRS84Quad). "
             "Fails if tiles already exist at that S3 location. Poll {{jobId}} again."),
    req("Step 9 · Verify: tileset list for the collection", "GET",
        "/collections/{{resourceId}}/map/tiles", code=200,
        note="The templated 'item' link here is what you give MapLibre."),
    req("Step 10 · Verify: fetch one vector tile", "GET",
        "/collections/{{resourceId}}/map/tiles/WorldCRS84Quad/0/0/0",
        codes=[200, 404, 401],
        headers=[H_BEARER,
                 {"key": "Accept", "value": "application/vnd.mapbox-vector-tile"}]),
    req("Step 11 · TilesMetaDataOnboarding (ASYNC) — register offline-made tiles", "POST",
        f"/processes/{pid('TilesMetaDataOnboarding')}/execution", codes=[201, 403],
        headers=[H_JSON, H_BEARER], extra_test=capture_job,
        body={"inputs": {"resourceId": "{{tileResourceId}}", "title": "My tileset",
                          "description": "Pre-rendered tiles uploaded to S3",
                          "tileMatrixSet": "WorldCRS84Quad", "encoding": "MVT",
                          "pointOfOrigin": [-180, 90],
                          "bbox": [68.17, 6.75, 97.41, 35.5],
                          "temporal": ["2024-01-01T00:00:00Z", "2024-12-31T00:00:00Z"],
                          "testTileCoordinateIndexes": "0/0/0",
                          "s3BucketIdentifier": "{{s3BucketIdentifier}}"}},
        note="Only for tiles you generated OUTSIDE the server and uploaded to S3 "
             "yourself; it verifies the test tile exists before registering."),
    # --- C. STAC archive + big asset upload: Story 2 of DEVELOPER_GUIDE_2 §12 ---
    req("Step 12 · Create STAC collection (Transaction)", "POST", "/stac/collections",
        codes=[201, 403, 409], headers=[H_JSON, H_BEARER],
        body={"type": "Collection", "id": "{{stacResourceId}}",
              "stac_version": "1.0.0", "description": "Scenes archive",
              "license": "proprietary",
              "extent": {"spatial": {"bbox": [[68.17, 6.75, 97.41, 35.5]]},
                          "temporal": {"interval": [["2024-01-01T00:00:00Z", None]]}},
              "links": []},
        note="{{stacResourceId}} must be a Catalogue resource UUID you own. "
             "Creates the item partition too."),
    req("Step 13 · Create STAC item (footprint + datetime)", "POST",
        "/stac/collections/{{stacResourceId}}/items", codes=[201, 403],
        headers=[H_JSON, H_BEARER],
        body={"type": "Feature", "id": "{{stacItemId}}", "stac_version": "1.0.0",
              "geometry": {"type": "Polygon",
                            "coordinates": [[[73.8, 18.4], [74.0, 18.4], [74.0, 18.7],
                                             [73.8, 18.7], [73.8, 18.4]]]},
              "bbox": [73.8, 18.4, 74.0, 18.7],
              "properties": {"datetime": "2024-03-01T10:32:00Z"},
              "assets": {}, "links": []}),
    req("Step 14 · S3InitiateMultipartUpload (SYNC) — presigned part URLs", "POST",
        f"/processes/{pid('S3InitiateMultipartUpload')}/execution", codes=[200, 403],
        headers=[H_JSON, H_BEARER], extra_test=capture_upload,
        body={"inputs": {"collectionId": "{{stacResourceId}}", "itemId": "{{stacItemId}}",
                          "fileName": "scene.tif", "fileSize": 20000000,
                          "fileType": "image/tiff",
                          "s3BucketIdentifier": "{{s3BucketIdentifier}}"}},
        note="Saves {{uploadId}} and the first part URL as {{s3PartUrl}}. "
             "Object must not already exist in S3."),
    raw_url_req("Step 15 · Upload part 1 directly to S3 (PUT part URL)", "PUT",
                "{{s3PartUrl}}", body_mode="file",
                note="Body → binary → your file chunk. COPY THE ETag RESPONSE HEADER "
                     "into the {{eTag1}} variable (strip quotes) for Step 16."),
    req("Step 16 · S3CompleteMultipartUpload (SYNC) — stitch the parts", "POST",
        f"/processes/{pid('S3CompleteMultipartUpload')}/execution", codes=[200, 403],
        headers=[H_JSON, H_BEARER],
        body={"inputs": {"uploadId": "{{uploadId}}",
                          "filePath": "{{stacResourceId}}/{{stacItemId}}/scene.tif",
                          "parts": ["1:{{eTag1}}"],
                          "s3BucketIdentifier": "{{s3BucketIdentifier}}"}}),
    req("Step 17 · Consumer view: STAC item by id (presigned asset hrefs)", "GET",
        "/stac/collections/{{stacResourceId}}/items/{{stacItemId}}",
        codes=[200, 404], headers=[H_BEARER],
        note="For an authorized token, relative asset hrefs become presigned S3 GET "
             "URLs valid until the token expires."),
    # --- D. Utility ---
    req("Step 18 · FeatureAttributesExtraction (SYNC) — filter-dropdown values", "POST",
        f"/processes/{pid('FeatureAttributesExtraction')}/execution", codes=[200, 403],
        headers=[H_JSON, H_BEARER],
        body={"inputs": {"collectionId": "{{resourceId}}",
                          "attributes": ["{{attributeName}}"]}},
        note="Set {{attributeName}} to a real column of your collection "
             "(see them documented under /api for your collection)."),
    req("Step 19 · All my jobs (the paper trail)", "GET", "/jobs", code=200,
        headers=[H_BEARER]),
    req("Step 20 · Job results of the last ASYNC job", "GET",
        "/jobs/{{jobId}}/results", codes=[200, 404], headers=[H_BEARER]),
]

# ----------------------------------------------------------------------------- assemble
collection = {
    "info": {
        "_postman_id": _id(),
        "name": "dx-dataplane-ogc · v2 Auth E2E",
        "description": (
            "End-to-end test collection for dx-dataplane-ogc with the new v2 auth "
            "(JWT + Basic AppId + delegation) and gRPC item-access.\n\n"
            "## Quick start\n"
            "1. Set the collection variables (baseUrl, keycloakTokenUrl, clientId, "
            "clientSecret, username, password, collectionId, etc.).\n"
            "2. Run **00 · Auth & Token → Keycloak · Get User Token** first — it saves "
            "`accessToken` automatically.\n"
            "3. Run any other folder; secured requests use `Authorization: Bearer "
            "{{accessToken}}` or Basic `{{appId}}:{{appSecret}}`.\n\n"
            "## Simple usage (DX consumer/provider token)\n"
            "You do NOT need Keycloak/AppId/delegation to use this collection: paste any "
            "valid DX JWT into the `accessToken` variable (or use the DX-AAA request in "
            "folder 00) and every secured request works. Use a **consumer** token for "
            "the read folders (02, 05, 07, 09) and a **provider** token for the write "
            "folders (03, 06, 11). Ignore the Basic AppId / `did` requests if you are "
            "not on the v2-auth deployment.\n\n"
            "## Learning path\n"
            "Read docs/DEVELOPER_GUIDE_2.md alongside this collection: folder 01 = "
            "discovery counters, 02/07 = Features, 04/05/06 = STAC, 11 = the provider "
            "stories of §12, in runnable form.\n\n"
            "## Notes\n"
            "- `baseUrl` defaults to `http://localhost:8080` (local). For dev use "
            "`https://v2.dev.ogc.iudx.io`.\n"
            "- Delegation (`did`) item-access tests are marked **KNOWN GAP**: they "
            "currently return 403 because OGC sends (delegator,delegator) to "
            "CheckItemAccess. They should pass once the delegated-item-access bridge is "
            "applied.\n"
            "- Status-code assertions are intentionally lenient (oneOf) where the exact "
            "code depends on your data (OPEN vs RESTRICTED dataset, granted vs denied)."
        ),
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "item": [
        folder("00 · Auth & Token", auth_items,
               "Get a token first. The user-token request saves {{accessToken}}."),
        folder("01 · OGC Core & Discovery (open)", core_items,
               "Landing, /api spec, conformance, collections, records, processes, tiles "
               "metadata, S3 bucket list — no token required."),
        folder("02 · OGC Features & Tiles (secured)", secured_ogc_items,
               "Full auth matrix on data endpoints (features, feature, coverage, tile)."),
        folder("03 · Processes & Jobs", proc_items,
               "Execute process + poll job status, list jobs, fetch job results."),
        folder("04 · STAC Discovery (open)", stac_disc_items, "STAC landing/collections."),
        folder("05 · STAC Data & Assets (secured)", secured_stac_items,
               "STAC items + asset download (pre-signed URLs)."),
        folder("06 · STAC Write & Search (provider)", stac_write_items,
               "Create/update/delete STAC collections & items + item search."),
        folder("07 · Features Query Matrix (secured)", feat_matrix,
               "bbox / datetime / crs / paging variants on getFeatures (Bearer)."),
        folder("08 · Content Negotiation", content_neg,
               "Accept-header / f= variants across endpoints."),
        folder("09 · Metering / Audit (secured)", metering_items, "Usage summary & audit."),
        folder("10 · Negative & Smoke", smoke_items, "Edge cases, bad routes, ambiguous auth."),
        folder("11 · Provider Workflow (end-to-end)", provider_flow,
               "The full provider story in order, with the REAL input body of every "
               "process: presigned upload → CollectionOnboarding → job polling → "
               "verify features → tiles generation → STAC collection/item + multipart "
               "asset upload → attribute extraction. Needs only a provider Bearer "
               "token in {{accessToken}} plus {{resourceId}} (and {{stacResourceId}} "
               "for the STAC steps). Run Step 0 first — it captures process ids. "
               "Mirrors the stories in docs/DEVELOPER_GUIDE_2.md §12."),
    ],
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8080"},
        {"key": "authBaseUrl", "value": "https://v2.dev.controlplane.iudx.io"},
        {"key": "keycloakTokenUrl",
         "value": "https://v2.dev.keycloak.iudx.io/auth/realms/iudx-v2/protocol/openid-connect/token"},
        {"key": "clientId", "value": ""},
        {"key": "clientSecret", "value": ""},
        {"key": "username", "value": ""},
        {"key": "password", "value": ""},
        {"key": "accessToken", "value": ""},
        {"key": "refreshToken", "value": ""},
        {"key": "serviceToken", "value": ""},
        {"key": "expiredToken", "value": "paste-an-expired-jwt-here"},
        {"key": "keycloakIdToken", "value": ""},
        {"key": "appId", "value": ""},
        {"key": "appSecret", "value": ""},
        {"key": "did", "value": ""},
        {"key": "itemId", "value": ""},
        {"key": "role", "value": "consumer"},
        {"key": "collectionId", "value": ""},
        {"key": "restrictedCollectionId", "value": ""},
        {"key": "featureId", "value": "1"},
        {"key": "tileMatrixSetId", "value": "WebMercatorQuad"},
        {"key": "tileMatrix", "value": "5"},
        {"key": "tileRow", "value": "10"},
        {"key": "tileCol", "value": "20"},
        {"key": "processId", "value": ""},
        {"key": "jobId", "value": ""},
        {"key": "assetId", "value": ""},
        {"key": "stacItemId", "value": ""},
        {"key": "recordCatalogId", "value": ""},
        {"key": "recordId", "value": ""},
        {"key": "resourceId", "value": ""},
        {"key": "stacResourceId", "value": ""},
        {"key": "tileResourceId", "value": ""},
        {"key": "s3BucketIdentifier", "value": "default"},
        {"key": "s3PresignedUrl", "value": ""},
        {"key": "s3ObjectKeyName", "value": ""},
        {"key": "s3PartUrl", "value": ""},
        {"key": "uploadId", "value": ""},
        {"key": "eTag1", "value": ""},
        {"key": "attributeName", "value": ""},
    ],
}

def count_leaves(items):
    n = 0
    for it in items:
        if "item" in it:
            n += count_leaves(it["item"])
        else:
            n += 1
    return n

total = count_leaves(collection["item"])
out_path = "dx-dataplane-ogc.postman_collection.json"
with open(out_path, "w") as f:
    json.dump(collection, f, indent=2)
print(f"wrote {out_path} with {total} requests")
for fld in collection["item"]:
    print(f"  - {fld['name']}: {count_leaves(fld['item'])}")
