# Token Constraints

This section describes the structure and semantics of constraints in the authorization token used for accessing protected OGC-compliant API resources.

---

##  Token Structure

The token is a JWT (JSON Web Token) and contains several standard and custom claims. Below is an example token payload:

```json
{
  "sub": "fd47486b-3497-4248-ac1e-082e4d37a66c",
  "iss": "fakeauth.ugix.io",
  "aud": "ogc.server.test.com",
  "exp": 1724278224,
  "iat": 1724235024,
  "iid": "ri:83c2e5c2-3574-4e11-9530-2b1fbdfce832",
  "role": "consumer",
  "cons": {
    "access": ["api", "sub", "file", "async"],
    "limits": {
      "bbox": [-123.0, 37.0, -122.0, 38.0],
      "feat": {
        "39b9d0f5-38be-4603-b2db-7b678d9c3870": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
      },
      "dataUsage": "100:gb",
      "apiHits": 100,
      "iat": 1724235024
    }
  },
  "rg": "8b95ab80-2aaf-4636-a65e-7f2563d0d371"
}
```

---

##  `cons` (Constraints) Section

The `cons` object specifies what actions or resources the token holder is authorized to access. It consists of:

### 1. `access` (Array)

Defines the types of operations permitted:

- `"api"` – Access to specific API endpoints.
- `"sub"` – Access to subscriptions.
- `"file"` – Access to file downloads.
- `"async"` – Access to async processing endpoints.

### 2. `limits` (Object)

The `limits` object within the `cons` (constraints) section defines specific restrictions and quotas imposed on the token holder's access and usage.

---

##  Limits Constraints

###  Spatial Limits (Mutually Exclusive)

Only **ONE** of these can exist in a token, but **not both**:

#### a. `bbox` (Bounding Box)

```json
"bbox": [-123.0, 37.0, -122.0, 38.0]
```

- **Type**: Array of numbers `[minLon, minLat, maxLon, maxLat]`
- **Purpose**: Defines a geographical bounding box that restricts the token holder's access to data or services only within these coordinates. Any requests for data falling outside this defined area will be denied
- **Enforcement**: The bounding box is validated and enforced using PostGIS spatial functions like `ST_Within`.
- **Constraint**: If `bbox` is present, `feat` must not be. These two are mutually exclusive.

#### b. `feat` (Feature-Level Access)

```json
"feat": {
  "collection-id": [1, 2, 3]
}
```
- **Type**: Object where keys are collection IDs (UUIDs) and values are arrays of numeric feature IDs.
- **Purpose**: Specifies access to a precise set of individual features within particular feature collections. The token holder can only interact with the listed feature IDs for the corresponding collection.
- **Enforcement**: Validated using the database service against existing feature IDs. Max 10 featureIds allowed. Enforced using PostGIS spatial functions like `ST_Within`.
- **Constraint**: If `feat` is present, `bbox` must not be. These two are mutually exclusive.

---

###  Usage Limits (Mutually Exclusive)

Only **ONE** of these can exist in a token, but **not both**:

#### a. `dataUsage`

```json
"dataUsage": "100:gb"
```

- **Type**: String in the format `"value:unit"` (e.g., `"100:gb"`, `"500:mb"`)
- **Purpose**: Defines the maximum amount of data that the token holder is permitted to consume.
- **Units Supported**: `kb`, `mb`, `gb`, `tb`
- **Enforcement**: Total response size is summed from the `metering` table (`resp_size`) starting from the `iat` timestamp. Enforced per user, API path, and collection combination.
- **Constraint**: If `dataUsage` is present, `apiHits` must not be present - only one usage-based limitation type can exist simultaneously.

#### b. `apiHits`

```json
"apiHits": 100
```

- **Type**: Integer
- **Purpose**: Specifies the maximum number of API requests (hits) the token holder is allowed to make.
- **Enforcement**: Count of requests (`resp_size > 0`) tracked in the `metering` table after the `iat` timestamp. Enforced per user, API path, and collection combination.
- **Constraint**: If `apiHits` is present, `dataUsage` must not be present - only one usage-based limitation type can exist simultaneously.

---

###  `iat` (Issued At)

```json
"iat": 1724235024
```

- **Type**: Unix timestamp (in seconds) when the policy/limits were issued
- **Purpose**: Specifies the start time from which usage limits (dataUsage or apiHits) are calculated. Used as the baseline time for querying metering data from the database
- **Enforcement**: Metrics are collected only from records with `timestamp > to_timestamp(iat)` in the `metering` table.
- This ensures historical data before policy issuance is excluded from enforcement.
---

##  Example Valid Combinations

| bbox/feat | dataUsage/apiHits | Valid |
|-----------|-------------------|-------|
| bbox      | dataUsage         | Yes   |
| bbox      | apiHits           | Yes   |
| feat      | dataUsage         | Yes   |
| feat      | apiHits           | Yes   |
| bbox      | both              | No    |
| feat      | both              | No    |
| both      | dataUsage         | No    |
| both      | apiHits           | No    |



---

##  Invalid Scenarios

- Both `bbox` and `feat` are defined
- Both `apiHits` and `dataUsage` are defined

---

## Summary

The `limits` section ensures that data access is **spatially scoped** and **rate-limited**, with strict rules around **mutual exclusivity**. These constraints are enforced during request handling to protect datasets and ensure fair usage.

