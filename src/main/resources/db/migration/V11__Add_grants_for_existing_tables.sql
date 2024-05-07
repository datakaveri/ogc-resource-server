GRANT SELECT, INSERT ON collections_details TO ${ogcUser};
GRANT SELECT, INSERT ON collection_type TO ${ogcUser};
GRANT SELECT, INSERT ON collection_supported_crs TO ${ogcUser};

GRANT SELECT, INSERT ON crs_to_srid  TO ${ogcUser} ;

GRANT SELECT,UPDATE,INSERT ON jobs_table TO ${ogcUser} ;
GRANT SELECT, INSERT ON processes_table TO ${ogcUser} ;

GRANT SELECT, INSERT ON ri_details TO ${ogcUser} ;
GRANT SELECT, INSERT ON roles TO ${ogcUser} ;

GRANT SELECT, INSERT ON stac_collections_assets TO ${ogcUser} ;

GRANT SELECT, INSERT ON tilematrixset_metadata TO ${ogcUser} ;
GRANT SELECT, INSERT ON tilematrixsets_relation TO ${ogcUser} ;
