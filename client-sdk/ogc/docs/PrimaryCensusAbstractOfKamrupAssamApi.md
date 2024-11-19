# openapi_client.PrimaryCensusAbstractOfKamrupAssamApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_features**](PrimaryCensusAbstractOfKamrupAssamApi.md#get_features) | **GET** /collections/41f1f47c-db20-4707-bc7c-c3a46a18172f/items | Get features from Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level
[**get_specific_collection**](PrimaryCensusAbstractOfKamrupAssamApi.md#get_specific_collection) | **GET** /collections/41f1f47c-db20-4707-bc7c-c3a46a18172f | Metadata about Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level
[**get_specific_feature**](PrimaryCensusAbstractOfKamrupAssamApi.md#get_specific_feature) | **GET** /collections/41f1f47c-db20-4707-bc7c-c3a46a18172f/items/{featureId} | Get single feature from Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level


# **get_features**
> FeatureCollectionGeoJSON get_features(bbox_crs=bbox_crs, crs=crs, bbox=bbox, datetime=datetime, limit=limit, offset=offset, tot_m=tot_m, tot_f=tot_f, fpfl_os=fpfl_os, ndr_ner1=ndr_ner1, ndr_ner2=ndr_ner2, mor_bb=mor_bb, non_work_p=non_work_p, marg_ot_32=marg_ot_32, marg_ot_31=marg_ot_31, wwoc_nd=wwoc_nd, non_work_m=non_work_m, tot_p=tot_p, mof_aom=mof_aom, non_work_f=non_work_f, mof_mft=mof_mft, f_ill=f_ill, mdds_vt=mdds_vt, tfufc_elc=tfufc_elc, main_cl_p=main_cl_p, mor_conc=mor_conc, sl_nsrbh=sl_nsrbh, hh_tscs_uc=hh_tscs_uc, mow_gimas=mow_gimas, kf_hk=kf_hk, mdds_dt=mdds_dt, mow_wood=mow_wood, hh_tscs_tt=hh_tscs_tt, os_ren=os_ren, mow_bb=mow_bb, msl_el=msl_el, tfufc_kr=tfufc_kr, mor_pl_pol=mor_pl_pol, marg_ot_3_=marg_ot_3_, hh_cond__3=hh_cond__3, hh_cond__4=hh_cond__4, hh_cond__1=hh_cond__1, hh_cond__2=hh_cond__2, hh_cond__7=hh_cond__7, mof_cem=mof_cem, hh_cond__8=hh_cond__8, hh_cond__5=hh_cond__5, hh_cond__6=hh_cond__6, ans_pl=ans_pl, hh_cond__9=hh_cond__9, marg_cl_3_=marg_cl_3_, mc_5_=mc_5_, mainwork_m=mainwork_m, hhs_4=hhs_4, hhs_5=hhs_5, fpfl_pss=fpfl_pss, mainwork_p=mainwork_p, mow_mub=mow_mub, hh_tscs_sp=hh_tscs_sp, kf_tot=kf_tot, mainwork_f=mainwork_f, mor_mmt=mor_mmt, hhs_1=hhs_1, hhs_2=hhs_2, os_non=os_non, hhs_3=hhs_3, main_cl_m=main_cl_m, no_hh=no_hh, mdds_sdt=mdds_sdt, main_cl_f=main_cl_f, wwoc_od=wwoc_od, sl_nssba=sl_nssba, p_06=p_06, nhhnhl=nhhnhl, kf_coh=kf_coh, ldws_a=ldws_a, tnhh_abs=tnhh_abs, fpfl_st=fpfl_st, msdw_hp=msdw_hp, marg_al_0_=marg_al_0_, aos_tm_bot=aos_tm_bot, mor_gi_mas=mor_gi_mas, mow_aom=mow_aom, marg_hh_3_=marg_hh_3_, msdw_tpl=msdw_tpl, p_st=p_st, p_lit=p_lit, mor_hmt=mor_hmt, ndr_4_r=ndr_4_r, aos_tel=aos_tel, marg_hh_31=marg_hh_31, os_ow=os_ow, mof_bb=mof_bb, marg_cl_f=marg_cl_f, kf_hk1=kf_hk1, mor_aom=mor_aom, marg_cl_m=marg_cl_m, marg_hh_32=marg_hh_32, marg_cl_p=marg_cl_p, marg_al_31=marg_al_31, p_sc=p_sc, marg_al_32=marg_al_32, ndr_3_r=ndr_3_r, f_06=f_06, ndr_tr=ndr_tr, margwork_f=margwork_f, ndr_ner=ndr_ner, f_sc=f_sc, nhh_hbfw_1=nhh_hbfw_1, nhh_hbfw_2=nhh_hbfw_2, margwork_p=margwork_p, margwork_m=margwork_m, marg_al_01=marg_al_01, ldws_np=ldws_np, f_st=f_st, main_al_p=main_al_p, wwoc_cd=wwoc_cd, marg_al_02=marg_al_02, margwork_4=margwork_4, os_ao=os_ao, margwork_3=margwork_3, margwork_2=margwork_2, margwork_1=margwork_1, margwork_0=margwork_0, st_name=st_name, nhh_hlf=nhh_hlf, margwork_5=margwork_5, f_lit=f_lit, sdt_name=sdt_name, msl_kr=msl_kr, mow_pl_pol=mow_pl_pol, m_ill=m_ill, tfufc_lpg_=tfufc_lpg_, tfufc_cc=tfufc_cc, mow_snpwm=mow_snpwm, aos_=aos_, dt_name=dt_name, kf_dnhk1=kf_dnhk1, marg_hh_02=marg_hh_02, marg_hh_01=marg_hh_01, tot_work_p=tot_work_p, tot_work_m=tot_work_m, m_06=m_06, marg_ot_m=marg_ot_m, tot_work_f=tot_work_f, main_al_m=main_al_m, marg_ot_p=marg_ot_p, main_al_f=main_al_f, pl_wsop=pl_wsop, marg_ot_f=marg_ot_f, aos_tm_mo=aos_tm_mo, msl_nl=msl_nl, msl_ool=msl_ool, kf_dnhk=kf_dnhk, tfufc_cr=tfufc_cr, main_hh_p=main_hh_p, hhs_9_=hhs_9_, main_hh_m=main_hh_m, aos_tm_lo=aos_tm_lo, main_hh_f=main_hh_f, hh_tscs_p=hh_tscs_p, ndr_or=ndr_or, kf_nc=kf_nc, msdw_ucw=msdw_ucw, aos_noasic=aos_noasic, vt_name=vt_name, mow_spwm=mow_spwm, aos_cl_win=aos_cl_win, tfufc_=tfufc_, msdw_sp=msdw_sp, marg_al_3_=marg_al_3_, hh_tscs_s=hh_tscs_s, tfufc_ao=tfufc_ao, msdw_cw=msdw_cw, aos_cjv=aos_cjv, marg_hh_f=marg_hh_f, msdw_tb=msdw_tb, marg_hh_0_=marg_hh_0_, marg_hh_m=marg_hh_m, marg_hh_p=marg_hh_p, p_ill=p_ill, aos_bi=aos_bi, tfufc_bio=tfufc_bio, hh_cond_ch=hh_cond_ch, nsdiod=nsdiod, tfufc_clc=tfufc_clc, nhh_hbfwtp=nhh_hbfwtp, marg_cl_31=marg_cl_31, marg_cl_32=marg_cl_32, aos_cl_wii=aos_cl_wii, msdw_tfts=msdw_tfts, m_sc=m_sc, ans_op=ans_op, marg_ot_02=marg_ot_02, marg_ot_01=marg_ot_01, marg_cl_0_=marg_cl_0_, mow_gtb=mow_gtb, m_st=m_st, kf_cih=kf_cih, msl_ao=msl_ao, mow_conc=mow_conc, msdw_rc=msdw_rc, marg_cl_02=marg_cl_02, aos_hhw_tc=aos_hhw_tc, marg_cl_01=marg_cl_01, main_ot_f=main_ot_f, msl_se=msl_se, mof_mud=mof_mud, hhs_6_8=hhs_6_8, aos_smm=aos_smm, mof_wb=mof_wb, main_ot_m=main_ot_m, mor_gr_th_=mor_gr_th_, marg_ot_0_=marg_ot_0_, main_ot_p=main_ot_p, total=total, msdw_os=msdw_os, tfufc_nc=tfufc_nc, mor_ss=mor_ss, marg_al_f=marg_al_f, hh_cond_11=hh_cond_11, mc_1=mc_1, m_lit=m_lit, hh_tscs_ns=hh_tscs_ns, mc_2=mc_2, marg_al_m=marg_al_m, mc_3=mc_3, msdw_tfuts=msdw_tfuts, hh_cond_10=hh_cond_10, mc_4=mc_4, ldws_wp=ldws_wp, marg_al_p=marg_al_p, mof_stone=mof_stone, pl_wsvi=pl_wsvi, mdds_st=mdds_st)

Get features from Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level

### Example

* Bearer (JWT) Authentication (DX-AAA-Token):

```python
import openapi_client
from openapi_client.models.feature_collection_geo_json import FeatureCollectionGeoJSON
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://geoserver.dx.ugix.org.in
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://geoserver.dx.ugix.org.in"
)

# The client must configure the authentication and authorization parameters
# in accordance with the API server security policy.
# Examples for each auth method are provided below, use the example that
# satisfies your auth use case.

# Configure Bearer authorization (JWT): DX-AAA-Token
configuration = openapi_client.Configuration(
    access_token = os.environ["BEARER_TOKEN"]
)

# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.PrimaryCensusAbstractOfKamrupAssamApi(api_client)
    bbox_crs = 'http://www.opengis.net/def/crs/OGC/1.3/CRS84' # str |  (optional) (default to 'http://www.opengis.net/def/crs/OGC/1.3/CRS84')
    crs = 'http://www.opengis.net/def/crs/OGC/1.3/CRS84' # str |  (optional) (default to 'http://www.opengis.net/def/crs/OGC/1.3/CRS84')
    bbox = [3.4] # List[float] | Only features that have a geometry that intersects the bounding box are selected. The bounding box is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis (height or depth):  * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Minimum value, coordinate axis 3 (optional) * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis 2 * Maximum value, coordinate axis 3 (optional)  If the value consists of four numbers, the coordinate reference system is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) unless a different coordinate reference system is specified in the parameter `bbox-crs`.  If the value consists of six numbers, the coordinate reference system is WGS 84 longitude/latitude/ellipsoidal height (http://www.opengis.net/def/crs/OGC/0/CRS84h) unless a different coordinate reference system is specified in the parameter `bbox-crs`.  The query parameter `bbox-crs` is specified in OGC API - Features - Part 2: Coordinate Reference Systems by Reference.  For WGS 84 longitude/latitude the values are in most cases the sequence of minimum longitude, minimum latitude, maximum longitude and maximum latitude. However, in cases where the box spans the antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge).  If the vertical axis is included, the third and the sixth number are the bottom and the top of the 3-dimensional bounding box.  If a feature has multiple spatial geometry properties, it is the decision of the server whether only a single spatial geometry property is used to determine the extent or all relevant geometries. (optional)
    datetime = 'datetime_example' # str | Either a date-time or an interval. Date and time expressions adhere to RFC 3339. Intervals may be bounded or half-bounded (double-dots at start or end).  Examples:  * A date-time: \"2018-02-12T23:20:50Z\" * A bounded interval: \"2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\" * Half-bounded intervals: \"2018-02-12T00:00:00Z/..\" or \"../2018-03-18T12:31:12Z\"  Only features that have a temporal property that intersects the value of `datetime` are selected.  If a feature has multiple temporal properties, it is the decision of the server whether only a single temporal property is used to determine the extent or all relevant temporal properties. (optional)
    limit = 10 # int |  (optional) (default to 10)
    offset = 1 # int | OGC Resource server also offers way to paginate the result for queries.  If a query returns large number of records then user can use additional parameters in query parameters to limit numbers of records  to be returned.  Minimum = 0. Maximum = 1000. Default = 10. (optional) (default to 1)
    tot_m = 3.4 # float |  (optional)
    tot_f = 3.4 # float |  (optional)
    fpfl_os = 3.4 # float |  (optional)
    ndr_ner1 = 3.4 # float |  (optional)
    ndr_ner2 = 3.4 # float |  (optional)
    mor_bb = 3.4 # float |  (optional)
    non_work_p = 3.4 # float |  (optional)
    marg_ot_32 = 3.4 # float |  (optional)
    marg_ot_31 = 3.4 # float |  (optional)
    wwoc_nd = 3.4 # float |  (optional)
    non_work_m = 3.4 # float |  (optional)
    tot_p = 3.4 # float |  (optional)
    mof_aom = 3.4 # float |  (optional)
    non_work_f = 3.4 # float |  (optional)
    mof_mft = 3.4 # float |  (optional)
    f_ill = 3.4 # float |  (optional)
    mdds_vt = 'mdds_vt_example' # str |  (optional)
    tfufc_elc = 3.4 # float |  (optional)
    main_cl_p = 3.4 # float |  (optional)
    mor_conc = 3.4 # float |  (optional)
    sl_nsrbh = 3.4 # float |  (optional)
    hh_tscs_uc = 3.4 # float |  (optional)
    mow_gimas = 3.4 # float |  (optional)
    kf_hk = 3.4 # float |  (optional)
    mdds_dt = 'mdds_dt_example' # str |  (optional)
    mow_wood = 3.4 # float |  (optional)
    hh_tscs_tt = 3.4 # float |  (optional)
    os_ren = 3.4 # float |  (optional)
    mow_bb = 3.4 # float |  (optional)
    msl_el = 3.4 # float |  (optional)
    tfufc_kr = 3.4 # float |  (optional)
    mor_pl_pol = 3.4 # float |  (optional)
    marg_ot_3_ = 3.4 # float |  (optional)
    hh_cond__3 = 3.4 # float |  (optional)
    hh_cond__4 = 3.4 # float |  (optional)
    hh_cond__1 = 3.4 # float |  (optional)
    hh_cond__2 = 3.4 # float |  (optional)
    hh_cond__7 = 3.4 # float |  (optional)
    mof_cem = 3.4 # float |  (optional)
    hh_cond__8 = 3.4 # float |  (optional)
    hh_cond__5 = 3.4 # float |  (optional)
    hh_cond__6 = 3.4 # float |  (optional)
    ans_pl = 3.4 # float |  (optional)
    hh_cond__9 = 3.4 # float |  (optional)
    marg_cl_3_ = 3.4 # float |  (optional)
    mc_5_ = 3.4 # float |  (optional)
    mainwork_m = 3.4 # float |  (optional)
    hhs_4 = 3.4 # float |  (optional)
    hhs_5 = 3.4 # float |  (optional)
    fpfl_pss = 3.4 # float |  (optional)
    mainwork_p = 3.4 # float |  (optional)
    mow_mub = 3.4 # float |  (optional)
    hh_tscs_sp = 3.4 # float |  (optional)
    kf_tot = 3.4 # float |  (optional)
    mainwork_f = 3.4 # float |  (optional)
    mor_mmt = 3.4 # float |  (optional)
    hhs_1 = 3.4 # float |  (optional)
    hhs_2 = 3.4 # float |  (optional)
    os_non = 3.4 # float |  (optional)
    hhs_3 = 3.4 # float |  (optional)
    main_cl_m = 3.4 # float |  (optional)
    no_hh = 3.4 # float |  (optional)
    mdds_sdt = 'mdds_sdt_example' # str |  (optional)
    main_cl_f = 3.4 # float |  (optional)
    wwoc_od = 3.4 # float |  (optional)
    sl_nssba = 3.4 # float |  (optional)
    p_06 = 3.4 # float |  (optional)
    nhhnhl = 3.4 # float |  (optional)
    kf_coh = 3.4 # float |  (optional)
    ldws_a = 3.4 # float |  (optional)
    tnhh_abs = 3.4 # float |  (optional)
    fpfl_st = 3.4 # float |  (optional)
    msdw_hp = 3.4 # float |  (optional)
    marg_al_0_ = 3.4 # float |  (optional)
    aos_tm_bot = 3.4 # float |  (optional)
    mor_gi_mas = 3.4 # float |  (optional)
    mow_aom = 3.4 # float |  (optional)
    marg_hh_3_ = 3.4 # float |  (optional)
    msdw_tpl = 3.4 # float |  (optional)
    p_st = 3.4 # float |  (optional)
    p_lit = 3.4 # float |  (optional)
    mor_hmt = 3.4 # float |  (optional)
    ndr_4_r = 3.4 # float |  (optional)
    aos_tel = 3.4 # float |  (optional)
    marg_hh_31 = 3.4 # float |  (optional)
    os_ow = 3.4 # float |  (optional)
    mof_bb = 3.4 # float |  (optional)
    marg_cl_f = 3.4 # float |  (optional)
    kf_hk1 = 3.4 # float |  (optional)
    mor_aom = 3.4 # float |  (optional)
    marg_cl_m = 3.4 # float |  (optional)
    marg_hh_32 = 3.4 # float |  (optional)
    marg_cl_p = 3.4 # float |  (optional)
    marg_al_31 = 3.4 # float |  (optional)
    p_sc = 3.4 # float |  (optional)
    marg_al_32 = 3.4 # float |  (optional)
    ndr_3_r = 3.4 # float |  (optional)
    f_06 = 3.4 # float |  (optional)
    ndr_tr = 3.4 # float |  (optional)
    margwork_f = 3.4 # float |  (optional)
    ndr_ner = 3.4 # float |  (optional)
    f_sc = 3.4 # float |  (optional)
    nhh_hbfw_1 = 3.4 # float |  (optional)
    nhh_hbfw_2 = 3.4 # float |  (optional)
    margwork_p = 3.4 # float |  (optional)
    margwork_m = 3.4 # float |  (optional)
    marg_al_01 = 3.4 # float |  (optional)
    ldws_np = 3.4 # float |  (optional)
    f_st = 3.4 # float |  (optional)
    main_al_p = 3.4 # float |  (optional)
    wwoc_cd = 3.4 # float |  (optional)
    marg_al_02 = 3.4 # float |  (optional)
    margwork_4 = 3.4 # float |  (optional)
    os_ao = 3.4 # float |  (optional)
    margwork_3 = 3.4 # float |  (optional)
    margwork_2 = 3.4 # float |  (optional)
    margwork_1 = 3.4 # float |  (optional)
    margwork_0 = 3.4 # float |  (optional)
    st_name = 'st_name_example' # str |  (optional)
    nhh_hlf = 3.4 # float |  (optional)
    margwork_5 = 3.4 # float |  (optional)
    f_lit = 3.4 # float |  (optional)
    sdt_name = 'sdt_name_example' # str |  (optional)
    msl_kr = 3.4 # float |  (optional)
    mow_pl_pol = 3.4 # float |  (optional)
    m_ill = 3.4 # float |  (optional)
    tfufc_lpg_ = 3.4 # float |  (optional)
    tfufc_cc = 3.4 # float |  (optional)
    mow_snpwm = 3.4 # float |  (optional)
    aos_ = 3.4 # float |  (optional)
    dt_name = 'dt_name_example' # str |  (optional)
    kf_dnhk1 = 3.4 # float |  (optional)
    marg_hh_02 = 3.4 # float |  (optional)
    marg_hh_01 = 3.4 # float |  (optional)
    tot_work_p = 3.4 # float |  (optional)
    tot_work_m = 3.4 # float |  (optional)
    m_06 = 3.4 # float |  (optional)
    marg_ot_m = 3.4 # float |  (optional)
    tot_work_f = 3.4 # float |  (optional)
    main_al_m = 3.4 # float |  (optional)
    marg_ot_p = 3.4 # float |  (optional)
    main_al_f = 3.4 # float |  (optional)
    pl_wsop = 3.4 # float |  (optional)
    marg_ot_f = 3.4 # float |  (optional)
    aos_tm_mo = 3.4 # float |  (optional)
    msl_nl = 3.4 # float |  (optional)
    msl_ool = 3.4 # float |  (optional)
    kf_dnhk = 3.4 # float |  (optional)
    tfufc_cr = 3.4 # float |  (optional)
    main_hh_p = 3.4 # float |  (optional)
    hhs_9_ = 3.4 # float |  (optional)
    main_hh_m = 3.4 # float |  (optional)
    aos_tm_lo = 3.4 # float |  (optional)
    main_hh_f = 3.4 # float |  (optional)
    hh_tscs_p = 3.4 # float |  (optional)
    ndr_or = 3.4 # float |  (optional)
    kf_nc = 3.4 # float |  (optional)
    msdw_ucw = 3.4 # float |  (optional)
    aos_noasic = 3.4 # float |  (optional)
    vt_name = 'vt_name_example' # str |  (optional)
    mow_spwm = 3.4 # float |  (optional)
    aos_cl_win = 3.4 # float |  (optional)
    tfufc_ = 3.4 # float |  (optional)
    msdw_sp = 3.4 # float |  (optional)
    marg_al_3_ = 3.4 # float |  (optional)
    hh_tscs_s = 3.4 # float |  (optional)
    tfufc_ao = 3.4 # float |  (optional)
    msdw_cw = 3.4 # float |  (optional)
    aos_cjv = 3.4 # float |  (optional)
    marg_hh_f = 3.4 # float |  (optional)
    msdw_tb = 3.4 # float |  (optional)
    marg_hh_0_ = 3.4 # float |  (optional)
    marg_hh_m = 3.4 # float |  (optional)
    marg_hh_p = 3.4 # float |  (optional)
    p_ill = 3.4 # float |  (optional)
    aos_bi = 3.4 # float |  (optional)
    tfufc_bio = 3.4 # float |  (optional)
    hh_cond_ch = 3.4 # float |  (optional)
    nsdiod = 3.4 # float |  (optional)
    tfufc_clc = 3.4 # float |  (optional)
    nhh_hbfwtp = 3.4 # float |  (optional)
    marg_cl_31 = 3.4 # float |  (optional)
    marg_cl_32 = 3.4 # float |  (optional)
    aos_cl_wii = 3.4 # float |  (optional)
    msdw_tfts = 3.4 # float |  (optional)
    m_sc = 3.4 # float |  (optional)
    ans_op = 3.4 # float |  (optional)
    marg_ot_02 = 3.4 # float |  (optional)
    marg_ot_01 = 3.4 # float |  (optional)
    marg_cl_0_ = 3.4 # float |  (optional)
    mow_gtb = 3.4 # float |  (optional)
    m_st = 3.4 # float |  (optional)
    kf_cih = 3.4 # float |  (optional)
    msl_ao = 3.4 # float |  (optional)
    mow_conc = 3.4 # float |  (optional)
    msdw_rc = 3.4 # float |  (optional)
    marg_cl_02 = 3.4 # float |  (optional)
    aos_hhw_tc = 3.4 # float |  (optional)
    marg_cl_01 = 3.4 # float |  (optional)
    main_ot_f = 3.4 # float |  (optional)
    msl_se = 3.4 # float |  (optional)
    mof_mud = 3.4 # float |  (optional)
    hhs_6_8 = 3.4 # float |  (optional)
    aos_smm = 3.4 # float |  (optional)
    mof_wb = 3.4 # float |  (optional)
    main_ot_m = 3.4 # float |  (optional)
    mor_gr_th_ = 3.4 # float |  (optional)
    marg_ot_0_ = 3.4 # float |  (optional)
    main_ot_p = 3.4 # float |  (optional)
    total = 'total_example' # str |  (optional)
    msdw_os = 3.4 # float |  (optional)
    tfufc_nc = 3.4 # float |  (optional)
    mor_ss = 3.4 # float |  (optional)
    marg_al_f = 3.4 # float |  (optional)
    hh_cond_11 = 3.4 # float |  (optional)
    mc_1 = 3.4 # float |  (optional)
    m_lit = 3.4 # float |  (optional)
    hh_tscs_ns = 3.4 # float |  (optional)
    mc_2 = 3.4 # float |  (optional)
    marg_al_m = 3.4 # float |  (optional)
    mc_3 = 3.4 # float |  (optional)
    msdw_tfuts = 3.4 # float |  (optional)
    hh_cond_10 = 3.4 # float |  (optional)
    mc_4 = 3.4 # float |  (optional)
    ldws_wp = 3.4 # float |  (optional)
    marg_al_p = 3.4 # float |  (optional)
    mof_stone = 3.4 # float |  (optional)
    pl_wsvi = 3.4 # float |  (optional)
    mdds_st = 'mdds_st_example' # str |  (optional)

    try:
        # Get features from Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level
        api_response = api_instance.get_features(bbox_crs=bbox_crs, crs=crs, bbox=bbox, datetime=datetime, limit=limit, offset=offset, tot_m=tot_m, tot_f=tot_f, fpfl_os=fpfl_os, ndr_ner1=ndr_ner1, ndr_ner2=ndr_ner2, mor_bb=mor_bb, non_work_p=non_work_p, marg_ot_32=marg_ot_32, marg_ot_31=marg_ot_31, wwoc_nd=wwoc_nd, non_work_m=non_work_m, tot_p=tot_p, mof_aom=mof_aom, non_work_f=non_work_f, mof_mft=mof_mft, f_ill=f_ill, mdds_vt=mdds_vt, tfufc_elc=tfufc_elc, main_cl_p=main_cl_p, mor_conc=mor_conc, sl_nsrbh=sl_nsrbh, hh_tscs_uc=hh_tscs_uc, mow_gimas=mow_gimas, kf_hk=kf_hk, mdds_dt=mdds_dt, mow_wood=mow_wood, hh_tscs_tt=hh_tscs_tt, os_ren=os_ren, mow_bb=mow_bb, msl_el=msl_el, tfufc_kr=tfufc_kr, mor_pl_pol=mor_pl_pol, marg_ot_3_=marg_ot_3_, hh_cond__3=hh_cond__3, hh_cond__4=hh_cond__4, hh_cond__1=hh_cond__1, hh_cond__2=hh_cond__2, hh_cond__7=hh_cond__7, mof_cem=mof_cem, hh_cond__8=hh_cond__8, hh_cond__5=hh_cond__5, hh_cond__6=hh_cond__6, ans_pl=ans_pl, hh_cond__9=hh_cond__9, marg_cl_3_=marg_cl_3_, mc_5_=mc_5_, mainwork_m=mainwork_m, hhs_4=hhs_4, hhs_5=hhs_5, fpfl_pss=fpfl_pss, mainwork_p=mainwork_p, mow_mub=mow_mub, hh_tscs_sp=hh_tscs_sp, kf_tot=kf_tot, mainwork_f=mainwork_f, mor_mmt=mor_mmt, hhs_1=hhs_1, hhs_2=hhs_2, os_non=os_non, hhs_3=hhs_3, main_cl_m=main_cl_m, no_hh=no_hh, mdds_sdt=mdds_sdt, main_cl_f=main_cl_f, wwoc_od=wwoc_od, sl_nssba=sl_nssba, p_06=p_06, nhhnhl=nhhnhl, kf_coh=kf_coh, ldws_a=ldws_a, tnhh_abs=tnhh_abs, fpfl_st=fpfl_st, msdw_hp=msdw_hp, marg_al_0_=marg_al_0_, aos_tm_bot=aos_tm_bot, mor_gi_mas=mor_gi_mas, mow_aom=mow_aom, marg_hh_3_=marg_hh_3_, msdw_tpl=msdw_tpl, p_st=p_st, p_lit=p_lit, mor_hmt=mor_hmt, ndr_4_r=ndr_4_r, aos_tel=aos_tel, marg_hh_31=marg_hh_31, os_ow=os_ow, mof_bb=mof_bb, marg_cl_f=marg_cl_f, kf_hk1=kf_hk1, mor_aom=mor_aom, marg_cl_m=marg_cl_m, marg_hh_32=marg_hh_32, marg_cl_p=marg_cl_p, marg_al_31=marg_al_31, p_sc=p_sc, marg_al_32=marg_al_32, ndr_3_r=ndr_3_r, f_06=f_06, ndr_tr=ndr_tr, margwork_f=margwork_f, ndr_ner=ndr_ner, f_sc=f_sc, nhh_hbfw_1=nhh_hbfw_1, nhh_hbfw_2=nhh_hbfw_2, margwork_p=margwork_p, margwork_m=margwork_m, marg_al_01=marg_al_01, ldws_np=ldws_np, f_st=f_st, main_al_p=main_al_p, wwoc_cd=wwoc_cd, marg_al_02=marg_al_02, margwork_4=margwork_4, os_ao=os_ao, margwork_3=margwork_3, margwork_2=margwork_2, margwork_1=margwork_1, margwork_0=margwork_0, st_name=st_name, nhh_hlf=nhh_hlf, margwork_5=margwork_5, f_lit=f_lit, sdt_name=sdt_name, msl_kr=msl_kr, mow_pl_pol=mow_pl_pol, m_ill=m_ill, tfufc_lpg_=tfufc_lpg_, tfufc_cc=tfufc_cc, mow_snpwm=mow_snpwm, aos_=aos_, dt_name=dt_name, kf_dnhk1=kf_dnhk1, marg_hh_02=marg_hh_02, marg_hh_01=marg_hh_01, tot_work_p=tot_work_p, tot_work_m=tot_work_m, m_06=m_06, marg_ot_m=marg_ot_m, tot_work_f=tot_work_f, main_al_m=main_al_m, marg_ot_p=marg_ot_p, main_al_f=main_al_f, pl_wsop=pl_wsop, marg_ot_f=marg_ot_f, aos_tm_mo=aos_tm_mo, msl_nl=msl_nl, msl_ool=msl_ool, kf_dnhk=kf_dnhk, tfufc_cr=tfufc_cr, main_hh_p=main_hh_p, hhs_9_=hhs_9_, main_hh_m=main_hh_m, aos_tm_lo=aos_tm_lo, main_hh_f=main_hh_f, hh_tscs_p=hh_tscs_p, ndr_or=ndr_or, kf_nc=kf_nc, msdw_ucw=msdw_ucw, aos_noasic=aos_noasic, vt_name=vt_name, mow_spwm=mow_spwm, aos_cl_win=aos_cl_win, tfufc_=tfufc_, msdw_sp=msdw_sp, marg_al_3_=marg_al_3_, hh_tscs_s=hh_tscs_s, tfufc_ao=tfufc_ao, msdw_cw=msdw_cw, aos_cjv=aos_cjv, marg_hh_f=marg_hh_f, msdw_tb=msdw_tb, marg_hh_0_=marg_hh_0_, marg_hh_m=marg_hh_m, marg_hh_p=marg_hh_p, p_ill=p_ill, aos_bi=aos_bi, tfufc_bio=tfufc_bio, hh_cond_ch=hh_cond_ch, nsdiod=nsdiod, tfufc_clc=tfufc_clc, nhh_hbfwtp=nhh_hbfwtp, marg_cl_31=marg_cl_31, marg_cl_32=marg_cl_32, aos_cl_wii=aos_cl_wii, msdw_tfts=msdw_tfts, m_sc=m_sc, ans_op=ans_op, marg_ot_02=marg_ot_02, marg_ot_01=marg_ot_01, marg_cl_0_=marg_cl_0_, mow_gtb=mow_gtb, m_st=m_st, kf_cih=kf_cih, msl_ao=msl_ao, mow_conc=mow_conc, msdw_rc=msdw_rc, marg_cl_02=marg_cl_02, aos_hhw_tc=aos_hhw_tc, marg_cl_01=marg_cl_01, main_ot_f=main_ot_f, msl_se=msl_se, mof_mud=mof_mud, hhs_6_8=hhs_6_8, aos_smm=aos_smm, mof_wb=mof_wb, main_ot_m=main_ot_m, mor_gr_th_=mor_gr_th_, marg_ot_0_=marg_ot_0_, main_ot_p=main_ot_p, total=total, msdw_os=msdw_os, tfufc_nc=tfufc_nc, mor_ss=mor_ss, marg_al_f=marg_al_f, hh_cond_11=hh_cond_11, mc_1=mc_1, m_lit=m_lit, hh_tscs_ns=hh_tscs_ns, mc_2=mc_2, marg_al_m=marg_al_m, mc_3=mc_3, msdw_tfuts=msdw_tfuts, hh_cond_10=hh_cond_10, mc_4=mc_4, ldws_wp=ldws_wp, marg_al_p=marg_al_p, mof_stone=mof_stone, pl_wsvi=pl_wsvi, mdds_st=mdds_st)
        print("The response of PrimaryCensusAbstractOfKamrupAssamApi->get_features:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling PrimaryCensusAbstractOfKamrupAssamApi->get_features: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **bbox_crs** | **str**|  | [optional] [default to &#39;http://www.opengis.net/def/crs/OGC/1.3/CRS84&#39;]
 **crs** | **str**|  | [optional] [default to &#39;http://www.opengis.net/def/crs/OGC/1.3/CRS84&#39;]
 **bbox** | [**List[float]**](float.md)| Only features that have a geometry that intersects the bounding box are selected. The bounding box is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis (height or depth):  * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Minimum value, coordinate axis 3 (optional) * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis 2 * Maximum value, coordinate axis 3 (optional)  If the value consists of four numbers, the coordinate reference system is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) unless a different coordinate reference system is specified in the parameter &#x60;bbox-crs&#x60;.  If the value consists of six numbers, the coordinate reference system is WGS 84 longitude/latitude/ellipsoidal height (http://www.opengis.net/def/crs/OGC/0/CRS84h) unless a different coordinate reference system is specified in the parameter &#x60;bbox-crs&#x60;.  The query parameter &#x60;bbox-crs&#x60; is specified in OGC API - Features - Part 2: Coordinate Reference Systems by Reference.  For WGS 84 longitude/latitude the values are in most cases the sequence of minimum longitude, minimum latitude, maximum longitude and maximum latitude. However, in cases where the box spans the antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge).  If the vertical axis is included, the third and the sixth number are the bottom and the top of the 3-dimensional bounding box.  If a feature has multiple spatial geometry properties, it is the decision of the server whether only a single spatial geometry property is used to determine the extent or all relevant geometries. | [optional] 
 **datetime** | **str**| Either a date-time or an interval. Date and time expressions adhere to RFC 3339. Intervals may be bounded or half-bounded (double-dots at start or end).  Examples:  * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval: \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals: \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot;  Only features that have a temporal property that intersects the value of &#x60;datetime&#x60; are selected.  If a feature has multiple temporal properties, it is the decision of the server whether only a single temporal property is used to determine the extent or all relevant temporal properties. | [optional] 
 **limit** | **int**|  | [optional] [default to 10]
 **offset** | **int**| OGC Resource server also offers way to paginate the result for queries.  If a query returns large number of records then user can use additional parameters in query parameters to limit numbers of records  to be returned.  Minimum &#x3D; 0. Maximum &#x3D; 1000. Default &#x3D; 10. | [optional] [default to 1]
 **tot_m** | **float**|  | [optional] 
 **tot_f** | **float**|  | [optional] 
 **fpfl_os** | **float**|  | [optional] 
 **ndr_ner1** | **float**|  | [optional] 
 **ndr_ner2** | **float**|  | [optional] 
 **mor_bb** | **float**|  | [optional] 
 **non_work_p** | **float**|  | [optional] 
 **marg_ot_32** | **float**|  | [optional] 
 **marg_ot_31** | **float**|  | [optional] 
 **wwoc_nd** | **float**|  | [optional] 
 **non_work_m** | **float**|  | [optional] 
 **tot_p** | **float**|  | [optional] 
 **mof_aom** | **float**|  | [optional] 
 **non_work_f** | **float**|  | [optional] 
 **mof_mft** | **float**|  | [optional] 
 **f_ill** | **float**|  | [optional] 
 **mdds_vt** | **str**|  | [optional] 
 **tfufc_elc** | **float**|  | [optional] 
 **main_cl_p** | **float**|  | [optional] 
 **mor_conc** | **float**|  | [optional] 
 **sl_nsrbh** | **float**|  | [optional] 
 **hh_tscs_uc** | **float**|  | [optional] 
 **mow_gimas** | **float**|  | [optional] 
 **kf_hk** | **float**|  | [optional] 
 **mdds_dt** | **str**|  | [optional] 
 **mow_wood** | **float**|  | [optional] 
 **hh_tscs_tt** | **float**|  | [optional] 
 **os_ren** | **float**|  | [optional] 
 **mow_bb** | **float**|  | [optional] 
 **msl_el** | **float**|  | [optional] 
 **tfufc_kr** | **float**|  | [optional] 
 **mor_pl_pol** | **float**|  | [optional] 
 **marg_ot_3_** | **float**|  | [optional] 
 **hh_cond__3** | **float**|  | [optional] 
 **hh_cond__4** | **float**|  | [optional] 
 **hh_cond__1** | **float**|  | [optional] 
 **hh_cond__2** | **float**|  | [optional] 
 **hh_cond__7** | **float**|  | [optional] 
 **mof_cem** | **float**|  | [optional] 
 **hh_cond__8** | **float**|  | [optional] 
 **hh_cond__5** | **float**|  | [optional] 
 **hh_cond__6** | **float**|  | [optional] 
 **ans_pl** | **float**|  | [optional] 
 **hh_cond__9** | **float**|  | [optional] 
 **marg_cl_3_** | **float**|  | [optional] 
 **mc_5_** | **float**|  | [optional] 
 **mainwork_m** | **float**|  | [optional] 
 **hhs_4** | **float**|  | [optional] 
 **hhs_5** | **float**|  | [optional] 
 **fpfl_pss** | **float**|  | [optional] 
 **mainwork_p** | **float**|  | [optional] 
 **mow_mub** | **float**|  | [optional] 
 **hh_tscs_sp** | **float**|  | [optional] 
 **kf_tot** | **float**|  | [optional] 
 **mainwork_f** | **float**|  | [optional] 
 **mor_mmt** | **float**|  | [optional] 
 **hhs_1** | **float**|  | [optional] 
 **hhs_2** | **float**|  | [optional] 
 **os_non** | **float**|  | [optional] 
 **hhs_3** | **float**|  | [optional] 
 **main_cl_m** | **float**|  | [optional] 
 **no_hh** | **float**|  | [optional] 
 **mdds_sdt** | **str**|  | [optional] 
 **main_cl_f** | **float**|  | [optional] 
 **wwoc_od** | **float**|  | [optional] 
 **sl_nssba** | **float**|  | [optional] 
 **p_06** | **float**|  | [optional] 
 **nhhnhl** | **float**|  | [optional] 
 **kf_coh** | **float**|  | [optional] 
 **ldws_a** | **float**|  | [optional] 
 **tnhh_abs** | **float**|  | [optional] 
 **fpfl_st** | **float**|  | [optional] 
 **msdw_hp** | **float**|  | [optional] 
 **marg_al_0_** | **float**|  | [optional] 
 **aos_tm_bot** | **float**|  | [optional] 
 **mor_gi_mas** | **float**|  | [optional] 
 **mow_aom** | **float**|  | [optional] 
 **marg_hh_3_** | **float**|  | [optional] 
 **msdw_tpl** | **float**|  | [optional] 
 **p_st** | **float**|  | [optional] 
 **p_lit** | **float**|  | [optional] 
 **mor_hmt** | **float**|  | [optional] 
 **ndr_4_r** | **float**|  | [optional] 
 **aos_tel** | **float**|  | [optional] 
 **marg_hh_31** | **float**|  | [optional] 
 **os_ow** | **float**|  | [optional] 
 **mof_bb** | **float**|  | [optional] 
 **marg_cl_f** | **float**|  | [optional] 
 **kf_hk1** | **float**|  | [optional] 
 **mor_aom** | **float**|  | [optional] 
 **marg_cl_m** | **float**|  | [optional] 
 **marg_hh_32** | **float**|  | [optional] 
 **marg_cl_p** | **float**|  | [optional] 
 **marg_al_31** | **float**|  | [optional] 
 **p_sc** | **float**|  | [optional] 
 **marg_al_32** | **float**|  | [optional] 
 **ndr_3_r** | **float**|  | [optional] 
 **f_06** | **float**|  | [optional] 
 **ndr_tr** | **float**|  | [optional] 
 **margwork_f** | **float**|  | [optional] 
 **ndr_ner** | **float**|  | [optional] 
 **f_sc** | **float**|  | [optional] 
 **nhh_hbfw_1** | **float**|  | [optional] 
 **nhh_hbfw_2** | **float**|  | [optional] 
 **margwork_p** | **float**|  | [optional] 
 **margwork_m** | **float**|  | [optional] 
 **marg_al_01** | **float**|  | [optional] 
 **ldws_np** | **float**|  | [optional] 
 **f_st** | **float**|  | [optional] 
 **main_al_p** | **float**|  | [optional] 
 **wwoc_cd** | **float**|  | [optional] 
 **marg_al_02** | **float**|  | [optional] 
 **margwork_4** | **float**|  | [optional] 
 **os_ao** | **float**|  | [optional] 
 **margwork_3** | **float**|  | [optional] 
 **margwork_2** | **float**|  | [optional] 
 **margwork_1** | **float**|  | [optional] 
 **margwork_0** | **float**|  | [optional] 
 **st_name** | **str**|  | [optional] 
 **nhh_hlf** | **float**|  | [optional] 
 **margwork_5** | **float**|  | [optional] 
 **f_lit** | **float**|  | [optional] 
 **sdt_name** | **str**|  | [optional] 
 **msl_kr** | **float**|  | [optional] 
 **mow_pl_pol** | **float**|  | [optional] 
 **m_ill** | **float**|  | [optional] 
 **tfufc_lpg_** | **float**|  | [optional] 
 **tfufc_cc** | **float**|  | [optional] 
 **mow_snpwm** | **float**|  | [optional] 
 **aos_** | **float**|  | [optional] 
 **dt_name** | **str**|  | [optional] 
 **kf_dnhk1** | **float**|  | [optional] 
 **marg_hh_02** | **float**|  | [optional] 
 **marg_hh_01** | **float**|  | [optional] 
 **tot_work_p** | **float**|  | [optional] 
 **tot_work_m** | **float**|  | [optional] 
 **m_06** | **float**|  | [optional] 
 **marg_ot_m** | **float**|  | [optional] 
 **tot_work_f** | **float**|  | [optional] 
 **main_al_m** | **float**|  | [optional] 
 **marg_ot_p** | **float**|  | [optional] 
 **main_al_f** | **float**|  | [optional] 
 **pl_wsop** | **float**|  | [optional] 
 **marg_ot_f** | **float**|  | [optional] 
 **aos_tm_mo** | **float**|  | [optional] 
 **msl_nl** | **float**|  | [optional] 
 **msl_ool** | **float**|  | [optional] 
 **kf_dnhk** | **float**|  | [optional] 
 **tfufc_cr** | **float**|  | [optional] 
 **main_hh_p** | **float**|  | [optional] 
 **hhs_9_** | **float**|  | [optional] 
 **main_hh_m** | **float**|  | [optional] 
 **aos_tm_lo** | **float**|  | [optional] 
 **main_hh_f** | **float**|  | [optional] 
 **hh_tscs_p** | **float**|  | [optional] 
 **ndr_or** | **float**|  | [optional] 
 **kf_nc** | **float**|  | [optional] 
 **msdw_ucw** | **float**|  | [optional] 
 **aos_noasic** | **float**|  | [optional] 
 **vt_name** | **str**|  | [optional] 
 **mow_spwm** | **float**|  | [optional] 
 **aos_cl_win** | **float**|  | [optional] 
 **tfufc_** | **float**|  | [optional] 
 **msdw_sp** | **float**|  | [optional] 
 **marg_al_3_** | **float**|  | [optional] 
 **hh_tscs_s** | **float**|  | [optional] 
 **tfufc_ao** | **float**|  | [optional] 
 **msdw_cw** | **float**|  | [optional] 
 **aos_cjv** | **float**|  | [optional] 
 **marg_hh_f** | **float**|  | [optional] 
 **msdw_tb** | **float**|  | [optional] 
 **marg_hh_0_** | **float**|  | [optional] 
 **marg_hh_m** | **float**|  | [optional] 
 **marg_hh_p** | **float**|  | [optional] 
 **p_ill** | **float**|  | [optional] 
 **aos_bi** | **float**|  | [optional] 
 **tfufc_bio** | **float**|  | [optional] 
 **hh_cond_ch** | **float**|  | [optional] 
 **nsdiod** | **float**|  | [optional] 
 **tfufc_clc** | **float**|  | [optional] 
 **nhh_hbfwtp** | **float**|  | [optional] 
 **marg_cl_31** | **float**|  | [optional] 
 **marg_cl_32** | **float**|  | [optional] 
 **aos_cl_wii** | **float**|  | [optional] 
 **msdw_tfts** | **float**|  | [optional] 
 **m_sc** | **float**|  | [optional] 
 **ans_op** | **float**|  | [optional] 
 **marg_ot_02** | **float**|  | [optional] 
 **marg_ot_01** | **float**|  | [optional] 
 **marg_cl_0_** | **float**|  | [optional] 
 **mow_gtb** | **float**|  | [optional] 
 **m_st** | **float**|  | [optional] 
 **kf_cih** | **float**|  | [optional] 
 **msl_ao** | **float**|  | [optional] 
 **mow_conc** | **float**|  | [optional] 
 **msdw_rc** | **float**|  | [optional] 
 **marg_cl_02** | **float**|  | [optional] 
 **aos_hhw_tc** | **float**|  | [optional] 
 **marg_cl_01** | **float**|  | [optional] 
 **main_ot_f** | **float**|  | [optional] 
 **msl_se** | **float**|  | [optional] 
 **mof_mud** | **float**|  | [optional] 
 **hhs_6_8** | **float**|  | [optional] 
 **aos_smm** | **float**|  | [optional] 
 **mof_wb** | **float**|  | [optional] 
 **main_ot_m** | **float**|  | [optional] 
 **mor_gr_th_** | **float**|  | [optional] 
 **marg_ot_0_** | **float**|  | [optional] 
 **main_ot_p** | **float**|  | [optional] 
 **total** | **str**|  | [optional] 
 **msdw_os** | **float**|  | [optional] 
 **tfufc_nc** | **float**|  | [optional] 
 **mor_ss** | **float**|  | [optional] 
 **marg_al_f** | **float**|  | [optional] 
 **hh_cond_11** | **float**|  | [optional] 
 **mc_1** | **float**|  | [optional] 
 **m_lit** | **float**|  | [optional] 
 **hh_tscs_ns** | **float**|  | [optional] 
 **mc_2** | **float**|  | [optional] 
 **marg_al_m** | **float**|  | [optional] 
 **mc_3** | **float**|  | [optional] 
 **msdw_tfuts** | **float**|  | [optional] 
 **hh_cond_10** | **float**|  | [optional] 
 **mc_4** | **float**|  | [optional] 
 **ldws_wp** | **float**|  | [optional] 
 **marg_al_p** | **float**|  | [optional] 
 **mof_stone** | **float**|  | [optional] 
 **pl_wsvi** | **float**|  | [optional] 
 **mdds_st** | **str**|  | [optional] 

### Return type

[**FeatureCollectionGeoJSON**](FeatureCollectionGeoJSON.md)

### Authorization

[DX-AAA-Token](../README.md#DX-AAA-Token)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/geo+json, application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The response is a document consisting of features in the collection. The features included in the response are determined by the server based on the query parameters of the request. To support access to larger collections without overloading the client, the API supports paged access using &#x60;limit&#x60; and &#x60;offset&#x60; paramters. |  -  |
**400** | A query parameter has an invalid value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_specific_collection**
> Collection get_specific_collection()

Metadata about Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level

### Example


```python
import openapi_client
from openapi_client.models.collection import Collection
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://geoserver.dx.ugix.org.in
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://geoserver.dx.ugix.org.in"
)


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.PrimaryCensusAbstractOfKamrupAssamApi(api_client)

    try:
        # Metadata about Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level
        api_response = api_instance.get_specific_collection()
        print("The response of PrimaryCensusAbstractOfKamrupAssamApi->get_specific_collection:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling PrimaryCensusAbstractOfKamrupAssamApi->get_specific_collection: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**Collection**](Collection.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Information about the feature collection with id &#x60;collectionId&#x60;. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_specific_feature**
> FeatureGeoJSON get_specific_feature(feature_id, crs=crs)

Get single feature from Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level

### Example

* Bearer (JWT) Authentication (DX-AAA-Token):

```python
import openapi_client
from openapi_client.models.feature_geo_json import FeatureGeoJSON
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://geoserver.dx.ugix.org.in
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://geoserver.dx.ugix.org.in"
)

# The client must configure the authentication and authorization parameters
# in accordance with the API server security policy.
# Examples for each auth method are provided below, use the example that
# satisfies your auth use case.

# Configure Bearer authorization (JWT): DX-AAA-Token
configuration = openapi_client.Configuration(
    access_token = os.environ["BEARER_TOKEN"]
)

# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.PrimaryCensusAbstractOfKamrupAssamApi(api_client)
    feature_id = 56 # int | 
    crs = 'http://www.opengis.net/def/crs/OGC/1.3/CRS84' # str |  (optional) (default to 'http://www.opengis.net/def/crs/OGC/1.3/CRS84')

    try:
        # Get single feature from Primary Census Abstract (2011) for Kamrup Metropolitan district in Assam at village level
        api_response = api_instance.get_specific_feature(feature_id, crs=crs)
        print("The response of PrimaryCensusAbstractOfKamrupAssamApi->get_specific_feature:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling PrimaryCensusAbstractOfKamrupAssamApi->get_specific_feature: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **feature_id** | **int**|  | 
 **crs** | **str**|  | [optional] [default to &#39;http://www.opengis.net/def/crs/OGC/1.3/CRS84&#39;]

### Return type

[**FeatureGeoJSON**](FeatureGeoJSON.md)

### Authorization

[DX-AAA-Token](../README.md#DX-AAA-Token)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/geo+json, application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | fetch the feature with id &#x60;featureId&#x60; in the feature collection with id &#x60;collectionId&#x60; |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

