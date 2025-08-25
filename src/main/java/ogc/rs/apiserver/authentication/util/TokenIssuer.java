package ogc.rs.apiserver.authentication.util;

public enum TokenIssuer {
  KEYCLOAK,
  CONTROL_PANE, //auth v2 server from control plane
  AAA; // old AAA server

}