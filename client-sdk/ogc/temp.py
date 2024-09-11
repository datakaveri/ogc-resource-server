from dx_utils import DxUtils
import json
import openapi_client
import geopandas
import pyinstrument

c = openapi_client.ApiClient()

dx = DxUtils('geoserver.dx.ugix.org.in','dx.ugix.org.in','e24cc6c7-c344-4acb-9fa3-60704bb21e1d','5509acc0a1314e2efef07c1bc4797b1c4d6d9ee1')

x = openapi_client.MajorTownsApi(c)
dx.set_dx_token(x)

output = dx.get_all_features_as_geojson(x)
df = geopandas.GeoDataFrame.from_features(output)

df2 = geopandas.GeoDataFrame.from_features(dx.get_all_features_as_geojson_iter(x))
