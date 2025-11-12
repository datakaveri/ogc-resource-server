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

### 1. Configure S3 Event Notifications

In target bucket’s **Properties → Event Notifications**, configure:
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

---

##  MinIO Flow Setup

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



