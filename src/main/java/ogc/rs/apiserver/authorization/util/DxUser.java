package ogc.rs.apiserver.authorization.util;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class DxUser {
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
  private Boolean account_enabled; // newly added field
  // Constructor
  public DxUser(List<String> roles, String organisationId, String organisationName, UUID sub,
                boolean emailVerified, boolean kycVerified, String name, String preferredUsername,
                String givenName, String familyName, String email, List<String> pendingRoles,
                JsonObject organisation, LocalDateTime createdAt, JsonObject kycData,
                String twitter_account, String linkedin_account, String github_account, Boolean account_enabled) {
    this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
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
    this.pendingRoles = pendingRoles != null ? new ArrayList<>(pendingRoles) : new ArrayList<>();
    this.organisation = organisation != null ? organisation : new JsonObject();
    this.createdAt = createdAt;
    this.kycData = kycData;
    this.twitter_account = twitter_account;
    this.linkedin_account = linkedin_account;
    this.github_account = github_account;
    this.account_enabled = account_enabled;
  }
  // Getters
  public List<String> getRoles() {
    return roles;
  }
  public String getOrganisationId() {
    return organisationId;
  }
  public String getOrganisationName() {
    return organisationName;
  }
  public UUID getSub() {
    return sub;
  }
  public boolean isEmailVerified() {
    return emailVerified;
  }
  public boolean isKycVerified() {
    return kycVerified;
  }
  public String getName() {
    return name;
  }
  public String getPreferredUsername() {
    return preferredUsername;
  }
  public String getGivenName() {
    return givenName;
  }
  public String getFamilyName() {
    return familyName;
  }
  public String getEmail() {
    return email;
  }
  public List<String> getPendingRoles() {
    return pendingRoles;
  }
  public JsonObject getOrganisation() {
    return organisation;
  }
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
  public JsonObject getKycData() {
    return kycData;
  }
  public String getTwitterAccount() {
    return twitter_account;
  }
  public String getLinkedinAccount() {
    return linkedin_account;
  }
  public String getGithubAccount() {
    return github_account;
  }
  public Boolean getAccountEnabled() {
    return account_enabled;
  }
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
        .put("account_enabled", account_enabled);
  }
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
        user.getTwitterAccount(),
        user.getLinkedinAccount(),
        user.getGithubAccount(),
        user.getAccountEnabled() // retain createdAt
    );
  }
}