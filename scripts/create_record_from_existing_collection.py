import psycopg2
import requests
import json
from decimal import Decimal

# Database configuration
config = {
    "host": "<elastic-database-host>",
    "dbname": "<postgres-db-name>",
    "port": <postgres-port>,
    "user": "<postgres-user>",
    "password": "<postgres-password>"
}

# Catalogue configuration
cat_host = "<cat-host>"
cat_base = "<cat-base>"
cat_path = "/search"
cat_url = cat_host + cat_base + cat_path

# Helper to convert Decimal to float
def convert_decimal(obj):
    if isinstance(obj, list):
        return [convert_decimal(i) for i in obj]
    elif isinstance(obj, dict):
        return {k: convert_decimal(v) for k, v in obj.items()}
    elif isinstance(obj, Decimal):
        return float(obj)
    else:
        return obj

# Metadata fetch method
def fetch_resource_and_provider_metadata(resource_id):
    resource_params = {
        "property": "[id]",
        "value": f"[[{resource_id}]]",
        "filter": "[id,itemCreatedAt,tags,provider]"
    }

    response = requests.get(cat_url, params=resource_params)
    if response.status_code != 200:
        return response.status_code, None

    resource_json = response.json()
    if not resource_json.get("results"):
        return 200, None

    resource_result = resource_json["results"][0]
    output = {
        "itemCreatedAt": resource_result.get("itemCreatedAt") or None,
        "tags": resource_result.get("tags") or [],
        "providerName": "",
        "providerAdditionalInfo": ""
    }

    provider_id = resource_result.get("provider")
    if provider_id:
        provider_params = {
            "property": "[id]",
            "value": f"[[{provider_id}]]",
            "filter": "[name,providerOrg.additionalInfoURL]"
        }
        provider_response = requests.get(cat_url, params=provider_params)
        if provider_response.status_code == 200:
            provider_json = provider_response.json()
            if provider_json.get("results"):
                provider_result = provider_json["results"][0]
                output["providerName"] = provider_result.get("name") or ""
                output["providerAdditionalInfo"] = provider_result.get("providerOrg", {}).get("additionalInfoURL") or ""

    return 200, output

# SQL query to fetch collections for processing
query = """
SELECT
    collections_details.id,
    collections_details.title,
    collections_details.description,
    collections_details.bbox,
    collections_details.temporal,
    CASE
        WHEN collections_details.bbox IS NOT NULL AND array_length(collections_details.bbox, 1) = 4 THEN
            ST_MakeEnvelope(
                collections_details.bbox[1],
                collections_details.bbox[2],
                collections_details.bbox[3],
                collections_details.bbox[4],
                4326
            )
        ELSE NULL
    END AS geometry
FROM
    collections_details
JOIN
    collection_supported_crs ON collections_details.id = collection_supported_crs.collection_id
JOIN
    crs_to_srid ON crs_to_srid.id = collection_supported_crs.crs_id
JOIN
    collection_type ON collections_details.id = collection_type.collection_id
WHERE
    collection_type.type NOT IN ('STAC', 'COLLECTION')
GROUP BY
    collections_details.id;
"""

# Query to fetch the target table (UUID) dynamically
target_table_query = """
SELECT
    collections_details.id
FROM
    collections_details
JOIN
    collection_supported_crs ON collections_details.id = collection_supported_crs.collection_id
JOIN
    crs_to_srid ON crs_to_srid.id = collection_supported_crs.crs_id
JOIN
    collection_type ON collections_details.id = collection_type.collection_id
WHERE
    collection_type.type = 'COLLECTION'
GROUP BY
    collections_details.id
LIMIT 1;
"""

def insert_into_collection_table(conn, data, table_name):
    with conn.cursor() as cursor:
        # Check if collection_id already exists
        check_query = f"""
        SELECT 1 FROM public."{table_name}" WHERE collection_id = %s LIMIT 1;
        """
        cursor.execute(check_query, (data.get("id"),))
        exists = cursor.fetchone()

        if exists:
            print(f"Record with collection_id {data.get('id')} already exists. Skipping insert.")
            return

        search_text = f"{data.get('title', '')} {data.get('description', '')} {data.get('providerName', '')} {' '.join(data.get('tags', []))}"


        insert_query = f"""
        INSERT INTO public."{table_name}" (
            created,
            title,
            description,
            keywords,
            provider_name,
            provider_contacts,
            bbox,
            temporal,
            geometry,
            collection_id,
            search_vector
        )
        VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, ST_GeomFromWKB(%s, 4326), %s,
             to_tsvector('english', %s)
        )
        """

        values = (
            data.get("itemCreatedAt"),
            data.get("title", ""),
            data.get("description", ""),
            data.get("tags", []),
            data.get("providerName", ""),
            data.get("providerAdditionalInfo", ""),
            data.get("bbox", []),
            data.get("temporal", []),
            bytes.fromhex(data["geometry"]) if data.get("geometry") else None,
            data.get("id"),
            search_text
        )

        cursor.execute(insert_query, values)
        conn.commit()
        print(f"Inserted data for collection_id: {data.get('id')}")

try:
    with psycopg2.connect(
        host=config["host"],
        database=config["dbname"],
        user=config["user"],
        password=config["password"],
        port=config["port"]
    ) as conn:
        with conn.cursor() as cursor:
            # Fetch the target table UUID dynamically
            cursor.execute(target_table_query)
            target_table_row = cursor.fetchone()
            if not target_table_row:
                print("No target table found for type='COLLECTION'. Skipping inserts.")
            else:
                target_table = target_table_row[0]
                print(f"Using target_table: {target_table}")

                # Fetch collection rows for processing
                cursor.execute(query)
                col_names = [desc[0] for desc in cursor.description]
                rows = cursor.fetchall()

                for row in rows:
                    row_dict = dict(zip(col_names, row))
                    collection_id = row_dict["id"]

                    status_code, metadata = fetch_resource_and_provider_metadata(collection_id)

                    if status_code == 200 and metadata:
                        merged_output = {**row_dict, **metadata}
                        insert_into_collection_table(conn, merged_output, target_table)
                    else:
                        print(f"The item with collection_id {collection_id} is not present in catalogue. Skipping insert.")

except psycopg2.Error as e:
    print("Database error:", e)
except Exception as ex:
    print("Runtime error:", ex)
