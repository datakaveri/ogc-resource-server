package ogc.rs.apiserver.authentication.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import ogc.rs.apiserver.handlers.OgcFeaturesAuthZHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DxUser {
  private static final Logger LOGGER = LogManager.getLogger(DxUser.class);
  private List<String> roles;
  private String organisationId;
  private String organisationName;
  private UUID sub;
  private boolean emailVerified;
  private boolean kycVerified;
  private String name;
  private String preferredUsername;
  private String givenName;
  private String familyName;
  private String email;
  private List<String> pendingRoles;
  private JsonObject organisation;
  private LocalDateTime createdAt;
  private JsonObject kycData;
  private String twitter_account;
  private String linkedin_account;
  private String github_account;
  private Boolean account_enabled;
  private Long tokenExpiry;

  // All-args constructor
  public DxUser(List<String> roles,
                String organisationId,
                String organisationName,
                UUID sub,
                boolean emailVerified,
                boolean kycVerified,
                String name,
                String preferredUsername,
                String givenName,
                String familyName,
                String email,
                List<String> pendingRoles,
                JsonObject organisation,
                LocalDateTime createdAt,
                JsonObject kycData,
                String twitter_account,
                String linkedin_account,
                String github_account,
                Boolean account_enabled,
                Long tokenExpiry) {
    this.roles = roles;
    this.organisationId = organisationId;
    this.organisationName = organisationName;
    this.sub = sub;
    this.emailVerified = emailVerified;
    this.kycVerified = kycVerified;
    this.name = name;
    this.preferredUsername = preferredUsername;
    this.givenName = givenName;
    this.familyName = familyName;
    this.email = email;
    this.pendingRoles = pendingRoles;
    this.organisation = organisation;
    this.createdAt = createdAt;
    this.kycData = kycData;
    this.twitter_account = twitter_account;
    this.linkedin_account = linkedin_account;
    this.github_account = github_account;
    this.account_enabled = account_enabled;
    this.tokenExpiry = tokenExpiry;

  }

  // Getters
  public List<String> getRoles() { return roles; }
  public String getOrganisationId() { return organisationId; }
  public String getOrganisationName() { return organisationName; }
  public UUID getSub() { return sub; }
  public boolean isEmailVerified() { return emailVerified; }
  public boolean isKycVerified() { return kycVerified; }
  public String getName() { return name; }
  public String getPreferredUsername() { return preferredUsername; }
  public String getGivenName() { return givenName; }
  public String getFamilyName() { return familyName; }
  public String getEmail() { return email; }
  public List<String> getPendingRoles() { return pendingRoles; }
  public JsonObject getOrganisation() { return organisation; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public JsonObject getKycData() { return kycData; }
  public String getTwitter_account() { return twitter_account; }
  public String getLinkedin_account() { return linkedin_account; }
  public String getGithub_account() { return github_account; }
  public Boolean getAccount_enabled() { return account_enabled; }
  public Long getTokenExpiry() { return tokenExpiry; }


  // Setters
  public void setRoles(List<String> roles) { this.roles = roles; }
  public void setOrganisationId(String organisationId) { this.organisationId = organisationId; }
  public void setOrganisationName(String organisationName) { this.organisationName = organisationName; }
  public void setSub(UUID sub) { this.sub = sub; }
  public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
  public void setKycVerified(boolean kycVerified) { this.kycVerified = kycVerified; }
  public void setName(String name) { this.name = name; }
  public void setPreferredUsername(String preferredUsername) { this.preferredUsername = preferredUsername; }
  public void setGivenName(String givenName) { this.givenName = givenName; }
  public void setFamilyName(String familyName) { this.familyName = familyName; }
  public void setEmail(String email) { this.email = email; }
  public void setPendingRoles(List<String> pendingRoles) { this.pendingRoles = pendingRoles; }
  public void setOrganisation(JsonObject organisation) { this.organisation = organisation; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public void setKycData(JsonObject kycData) { this.kycData = kycData; }
  public void setTwitter_account(String twitter_account) { this.twitter_account = twitter_account; }
  public void setLinkedin_account(String linkedin_account) { this.linkedin_account = linkedin_account; }
  public void setGithub_account(String github_account) { this.github_account = github_account; }
  public void setAccount_enabled(Boolean account_enabled) { this.account_enabled = account_enabled; }
  public void setTokenExpiry(Long tokenExpiry) { this.tokenExpiry = tokenExpiry; }

  // toJson method
  public JsonObject toJson() {
    String isoCreatedAt = createdAt != null ? createdAt.toString() : null;

    return new JsonObject()
        .put("roles", roles != null ? new JsonArray(roles) : new JsonArray())
        .put("organisationId", organisationId)
        .put("organisationName", organisationName)
        .put("sub", sub != null ? sub.toString() : null)
        .put("emailVerified", emailVerified)
        .put("kycVerified", kycVerified)
        .put("name", name)
        .put("preferredUsername", preferredUsername)
        .put("givenName", givenName)
        .put("familyName", familyName)
        .put("email", email)
        .put("pending_roles", pendingRoles != null ? new JsonArray(pendingRoles) : new JsonArray())
        .put("organisation", organisation != null ? organisation : new JsonObject())
        .put("createdAt", isoCreatedAt)
        .put("kycInformation", kycData)
        .put("twitter_account", twitter_account)
        .put("linkedin_account", linkedin_account)
        .put("github_account", github_account)
        .put("account_enabled", account_enabled)
        .put("tokenExpiry", tokenExpiry);

  }

  //fromJsonObject method
  public static DxUser fromJsonObject(JsonObject jsonObject) {
    // check if the roles is not equal to null and then extract the json array
//    similar to this : roles != null ? new JsonArray(roles) : new JsonArray())
    String createdAtStr = jsonObject.getString("createdAt");
    LocalDateTime createdAt = null;
    if (createdAtStr != null && !createdAtStr.isBlank()) {
      try {
        createdAt = LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_DATE_TIME);
      } catch (Exception e) {
        LOGGER.warn("Invalid createdAt format: {}", createdAtStr, e);
      }
    }

    return new DxUser(
        jsonObject.getJsonArray("roles", new JsonArray()).getList(),
        jsonObject.getString("organisationId"),
        jsonObject.getString("organisationName"),
        jsonObject.containsKey("sub") ? UUID.fromString(jsonObject.getString("sub")) : null,
        jsonObject.getBoolean("emailVerified", false),
        jsonObject.getBoolean("kycVerified", false),
        jsonObject.getString("name"),
        jsonObject.getString("preferredUsername"),
        jsonObject.getString("givenName"),
        jsonObject.getString("familyName"),
        jsonObject.getString("email"),
        jsonObject.getJsonArray("pending_roles", new JsonArray()).getList(),
        jsonObject.getJsonObject("organisation", new JsonObject()),
        createdAt,
        jsonObject.getJsonObject("kycInformation", new JsonObject()),
        jsonObject.getString("twitter_account"),
        jsonObject.getString("linkedin_account"),
        jsonObject.getString("github_account"),
        jsonObject.getBoolean("account_enabled"),
        jsonObject.getLong("tokenExpiry")
    );
  }




  // Factory method (like record copy)
  public static DxUser withPendingRoles(DxUser user, List<String> pendingRoles, JsonObject organisation) {
    return new DxUser(
        user.getRoles(),
        user.getOrganisationId(),
        user.getOrganisationName(),
        user.getSub(),
        user.isEmailVerified(),
        user.isKycVerified(),
        user.getName(),
        user.getPreferredUsername(),
        user.getGivenName(),
        user.getFamilyName(),
        user.getEmail(),
        pendingRoles,
        organisation,
        user.getCreatedAt(),
        user.getKycData(),
        user.getTwitter_account(),
        user.getLinkedin_account(),
        user.getGithub_account(),
        user.getAccount_enabled(),
        user.getTokenExpiry()
    );
  }
}