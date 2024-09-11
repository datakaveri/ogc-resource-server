import urllib.request
import urllib.parse
import json
import base64
import time


class DxUtils:

    def __init__(self, rs_url, dx_url, client_id, client_secret):
        self.rs_url = rs_url
        self.dx_url = dx_url
        self.client_id = client_id
        self.client_secret = client_secret
        self.open_token, self.open_token_expiry = self.__get_token(
            rs_url, "resource_server", "consumer"
        )

    def set_dx_token(self, collection):
        id = collection.get_specific_collection().id
        access_policy = self.__check_access_policy_in_catalogue(id)
        token = ""

        if "OPEN" == access_policy:
            if time.time() > self.open_token_expiry:
                self.open_token, self.open_token_expiry = self.__get_token(
                    rs_url, "resource_server", "consumer"
                )
            token = self.open_token
        elif "SECURE" == access_policy:
            token = self.__get_token(id, "resource", "consumer")[0]
        else:
            raise Exception(
                f"Access policy for the resource is {access_policy}, cannot fetch token for it"
            )

        collection.api_client.configuration.access_token = token

        return

    def get_all_features_as_geojson(self, collection):
        all_data = []
        offset = 1
        count = 0

        while True:
            response = collection.get_features_without_preload_content(offset=offset)
            data = json.load(response)

            max = data["numberMatched"]
            returned = data["numberReturned"]

            all_data.extend(data["features"])

            next_link_arr = [i["href"] for i in data["links"] if i["rel"] == "next"]

            count = count + returned
            print(f"\rGot {count} of {max} features", end="", flush=True)

            if len(next_link_arr) == 0:
                return all_data

            next_link_query_params = urllib.parse.parse_qs(next_link_arr[0])
            new_offset_arr = next_link_query_params["offset"]

            offset = int(new_offset_arr[0])

    def get_all_features_as_geojson_iter(self, collection):
        offset = 1
        count = 0

        while True:
            response = collection.get_features_without_preload_content(offset=offset)
            data = json.load(response)

            max = data["numberMatched"]
            returned = data["numberReturned"]

            yield from data["features"]

            next_link_arr = [i["href"] for i in data["links"] if i["rel"] == "next"]

            count = count + returned
            print(f"\rGot {count} of {max} features", end="", flush=True)

            if len(next_link_arr) == 0:
                return

            next_link_query_params = urllib.parse.parse_qs(next_link_arr[0])
            new_offset_arr = next_link_query_params["offset"]

            offset = int(new_offset_arr[0])

    def __get_token(self, item_id, item_type, role):
        headers = {
            "Content-type": "application/json",
            "clientId": self.client_id,
            "clientSecret": self.client_secret,
        }

        url = f"https://{self.dx_url}/auth/v1/token"

        request_body = json.dumps(
            {"itemId": item_id, "itemType": item_type, "role": role}
        ).encode("utf-8")

        request = urllib.request.Request(url, bytes(request_body), headers)

        try:
            response = urllib.request.urlopen(request)
            json_resp = json.load(response)

            if (
                "results" not in json_resp
                or "accessToken" not in json_resp["results"]
                or "expiry" not in json_resp["results"]
            ):
                raise Exception("Invalid response sent by DX AAA server : {json_resp}")

            token = json_resp["results"]["accessToken"]
            expiry_unix = json_resp["results"]["expiry"]

            return (token, int(expiry_unix))
        except json.JSONDecodeError as e:
            raise Exception(f"Non JSON response sent from DX AAA server : {e.doc}")
        except urllib.error.HTTPError as e:
            raise Exception(
                f"Non 2xx status code received from DX AAA server : {e.code}, {e.fp.read()}"
            )
        except urllib.error.URLError as e:
            raise Exception(e)

    def __check_access_policy_in_catalogue(self, resource_id):

        try:
            response = urllib.request.urlopen(
                f"https://{self.dx_url}/ugix/cat/v1/item?id={resource_id}"
            )
            json_resp = json.load(response)

            if "results" not in json_resp or len(json_resp["results"]) == 0:
                raise Exception(f"Invalid response sent by DX catalogue server : {js}")

            item_info = json_resp["results"][0]

            if "accessPolicy" not in item_info:
                raise Exception(f"Invalid response sent by DX catalogue server : {js}")

            return item_info["accessPolicy"]

        except json.JSONDecodeError as e:
            raise Exception(
                f"Non JSON response sent from DX catalogue server : {e.doc}"
            )
        except urllib.error.HTTPError as e:
            raise Exception(
                f"Non 2xx status code received from DX catalogue server : {e.code}, {e.fp.read()}"
            )
        except urllib.error.URLError as e:
            raise Exception(e)
