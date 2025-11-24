# S3 and MinIO Audit Logs Integration

---

##  Overview
This section describes the components responsible for ingesting and processing **data access audit logs** related to STAC asset downloads using pre-signed URLs from both **AWS S3 (CloudTrail)** and **MinIO** object storage systems. These logs capture download events (`GetObject`) and forward structured entries to the **OGC Resource Server Process API** for metering and analytics.

##  Flow Summary



#### **1. AWS S3 Flow**
```
AWS S3 → CloudTrail → EventBridge / S3 Event Notification → Lambda → OGC RS /processes endpoint → PostgreSQL auditing table
```
#### **2. MinIO Flow**
```
MinIO → Webhook → FastAPI Audit Logs Processor → OGC RS /processes endpoint → PostgreSQL auditing table
```
---

##  Components

| Component | Description |
|------------|--------------|
| **OGC Resource Server (OGC RS)** | Receives processed audit logs and persists them into the `auditing` table. |
| **FastAPI Audit Logs Processor** | Ingests MinIO audit log webhooks, filters `GetObject` events, and forwards them to OGC RS. |
| **AWS Lambda Function** | Processes S3 CloudTrail or EventBridge events and posts relevant logs to the OGC RS `/processes` API. |
| **PostgreSQL (auditing table)** | Stores log entries (user ID, collection ID, item ID, timestamp, response size, etc.). |
| **Auth Server** | Issues JWT tokens for authenticated ingestion via the process API. |

---

##  Architecture Diagram

```
              ┌──────────────┐
              │   S3 / MinIO │
              └──────┬───────┘
                     │  (Audit Event)
                     ▼
         ┌────────────────────────────┐
         │  FastAPI / Lambda Processor│
         ├────────────────────────────┤
         │ Parses GetObject logs      │
         │ Fetches token from Auth Svc│
         │ Calls OGC RS /processes API│
         └────────────┬───────────────┘
                      │
                      ▼
            ┌──────────────────┐
            │OGC ResourceServer│
            │ (/processes API) │
            └───────┬──────────┘
                    ▼
            ┌──────────────────┐
            │PostgreSQL (audit)│
            └──────────────────┘
```

---
##  AWS S3 Flow Setup
In the AWS setup, all STAC assets are served through presigned URLs, and every download triggers an S3 GetObject event. To capture these access events, CloudTrail Data Events must be enabled for the S3 bucket. CloudTrail streams logs to a Lambda function, which extracts key fields like itemId, collectionId, userId, and bytesTransferred, and forwards them to the OGC Resource Server’s metering endpoint.

### 1. Configure S3 Event Notifications

In target CloudTrailLogs bucket's **Properties → Event Notifications**, configure:
- **Event type**: `Object Accessed` or `GetObject`
- **Destination**: AWS Lambda
- **Prefix/Suffix filters**: Optional for asset paths

### 2. Deploy AWS Lambda

Create a Lambda function (`ogc-s3-audit-logs-processor`) with the following responsibilities:
- Triggered by S3 or CloudTrail event.
- Extract user ID, collection ID, item ID, timestamp, response size.
- Obtain access token from Auth service.
- Send POST request to OGC RS `/processes/{process-id}/execution`.

Example payload:
```json
{
  "inputs": {
    "logs": [
      "user-uuid|collection-uuid|item-uuid|2025-10-27T10:12:30Z|12345|s3"
    ]
  }
}
```
**Example AWS Lambda Function (Python)**

Below is an example implementation of the `ogc-s3-audit-logs-processor` Lambda.
It consumes S3/CloudTrail GetObject events, extracts required fields, fetches an access token, and posts structured audit logs to the OGC RS `/processes/{process-id}/execution` endpoint.
```declarative
import json
import gzip
import boto3
import urllib3
import os

# --- Configuration from environment variables ---
AWS_CLOUDTRAIL_BUCKET = os.getenv("CLOUDTRAIL_BUCKET")     
AWS_REGION = os.getenv("CLOUDTRAIL_REGION")                

AUTH_URL = os.getenv("AUTH_URL")
PROCESS_API_URL = os.getenv("PROCESS_API_URL")

CLIENT_ID = os.getenv("CLIENT_ID")
CLIENT_SECRET = os.getenv("CLIENT_SECRET")

# Token payload parameters (generalized)
AUTH_ITEM_ID = os.getenv("AUTH_ITEM_ID")       
AUTH_ITEM_TYPE = os.getenv("AUTH_ITEM_TYPE")    
AUTH_ROLE = os.getenv("AUTH_ROLE")              

# --- AWS and HTTP clients ---
s3 = boto3.client("s3", region_name=AWS_REGION)
http = urllib3.PoolManager()


def get_access_token():
"""
Fetch an access token from the authorization server.
All token details are taken from environment variables.
"""
payload = {
"itemId": AUTH_ITEM_ID,
"itemType": AUTH_ITEM_TYPE,
"role": AUTH_ROLE
}

headers = {
"clientId": CLIENT_ID,
"clientSecret": CLIENT_SECRET,
"Content-Type": "application/json"
}

resp = http.request(
"POST",
AUTH_URL,
body=json.dumps(payload).encode("utf-8"),
headers=headers
)

if resp.status != 200:
raise Exception(f"Auth server returned {resp.status}: {resp.data}")

token_data = json.loads(resp.data.decode("utf-8"))
return token_data["results"]["accessToken"]


def parse_cloudtrail_object(bucket, key):
"""
Parse a single CloudTrail log (.gz file) and return
presigned URL access audit entries.
"""
print(f"[Lambda] Reading CloudTrail log from: s3://{bucket}/{key}")
resp = s3.get_object(Bucket=bucket, Key=key)

with gzip.GzipFile(fileobj=resp["Body"]) as f:
data = json.load(f)

logs = []

for rec in data.get("Records", []):
event_name = rec.get("eventName")
additional = rec.get("additionalEventData", {})
params = rec.get("requestParameters", {})

if (
event_name == "GetObject"
and additional.get("AuthenticationMethod") == "QueryString"
and "X-Amz-Expires" in json.dumps(params)
and params.get("collectionId")
and params.get("itemId")
):
user_id = params.get("userId")
collection_id = params.get("collectionId")
item_id = params.get("itemId")
timestamp = rec.get("eventTime")
resp_size = additional.get("bytesTransferredOut", 0)
storage_backend = "aws_s3"

log_entry = (
f"{user_id}|{collection_id}|{item_id}|"
f"{timestamp}|{resp_size}|{storage_backend}"
)
logs.append(log_entry)

return logs


def send_to_process_endpoint(token, logs):
"""
Send logs to the OGC Resource Server Process API.
"""
payload = {"inputs": {"logs": logs}}
headers = {
"Authorization": f"Bearer {token}",
"Content-Type": "application/json"
}

print(f"[Lambda] Sending {len(logs)} logs to Process API")

resp = http.request(
"POST",
PROCESS_API_URL,
body=json.dumps(payload).encode("utf-8"),
headers=headers
)

print(f"[Lambda] API Response: {resp.status}")
return resp.status


def lambda_handler(event, context):
"""
Lambda entrypoint — triggered when a CloudTrail log file
is uploaded into the S3 bucket.
"""
print("[Lambda] CloudTrail ingestion triggered")
all_logs = []

for record in event["Records"]:
bucket = record["s3"]["bucket"]["name"]
key = record["s3"]["object"]["key"]

logs = parse_cloudtrail_object(bucket, key)
all_logs.extend(logs)

if not all_logs:
print("[Lambda] No presigned URL logs found.")
return {"status": "no_logs_found"}

token = get_access_token()
status = send_to_process_endpoint(token, all_logs)

return {
"status": "completed",
"logs_count": len(all_logs),
"api_status": status
}


```
---

##  MinIO Flow Setup
MinIO does not produce CloudTrail-like logs automatically. Instead, audit logs are emitted to a configured webhook target.
The ogc-minio-audit-logs-processor (FastAPI service) listens to this webhook, processes logs, and forwards them to the OGC Resource Server.
### 1. Deploy the FastAPI Processor

Clone the repository:
```bash

git clone https://github.com/datakaveri/ogc-minio-audit-logs-processor.git
cd ogc-minio-audit-logs-processor
```

Create a `.env` file:
```bash

AUTH_URL=<auth-service-endpoint>
PROCESS_API_URL=<ogc-rs-process-endpoint>
CLIENT_ID=<client-id>
CLIENT_SECRET=<client-secret>
AUTH_ITEM_ID=<item-id>
AUTH_ITEM_TYPE=<item-type>
AUTH_ROLE=<role>
PORT=8092
```

Run the processor:
```bash

python3 -m uvicorn app:app --host 0.0.0.0 --port 8092
```

---

### 2. Configure MinIO Webhook

Edit MinIO configuration to send audit logs to the FastAPI processor:

```bash

mc admin config set myminio/ audit_webhook:1 endpoint="http://<processor-host>:8092/auditlogs/processor" enable=on
mc admin service restart myminio
```

---

### 3. Verify Audit Logs

Accessing any protected STAC asset in MinIO (via presigned URLs) will trigger a `GetObject` audit log.  
The processor logs can be seen in the FastAPI server like:

```
[Server] Received audit log
[Server] Sending 1 logs to http://<OGC-RS>/processes/<process-id>/execution
[Server] Process API response: 200 - OK
```

---



##  Auditing Table Schema

| Column | Type | Description |
|--------|------|-------------|
| `id` | `SERIAL PRIMARY KEY` | Auto incremented ID |
| `user_id` | `UUID` | ID of the user accessing data |
| `collection_id` | `UUID` | Related collection |
| `api_path` | `TEXT` | API or object path |
| `timestamp` | `TIMESTAMP` | Access time |
| `resp_size` | `BIGINT` | Response size in bytes |

---

##  Verification Steps

| Step | Check |
|------|--------|
| FastAPI Processor is reachable | `curl http://<processor-host>:8092` → `{ "status": "ok" }` |
| MinIO webhook successfully posts logs | Logs appear in FastAPI stdout |
| OGC RS `/processes` endpoint reachable | `curl -X POST <PROCESS_API_URL>` returns 200 |
| Database ingestion working | Query `SELECT COUNT(*) FROM auditing;` |



