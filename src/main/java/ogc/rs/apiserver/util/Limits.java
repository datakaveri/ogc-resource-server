package ogc.rs.apiserver.util;

import io.vertx.core.json.JsonObject;

/**
 * Represents API usage and data usage limits extracted from a user's token.
 */
public class Limits {

    private final Long policyIssuedAt;
    private final Long dataUsageLimitInBytes;
    private final Long apiHitsLimit;

    private Limits(Long policyIssuedAt, Long dataUsageLimitInBytes, Long apiHitsLimit) {
        this.policyIssuedAt = policyIssuedAt;
        this.dataUsageLimitInBytes = dataUsageLimitInBytes;
        this.apiHitsLimit = apiHitsLimit;
    }

    /**
     * Parses the JsonObject representing limits and constructs a Limits instance.
     *
     * @param limitsJson JsonObject with fields like "iat", "dataUsage", "apiHits"
     * @return Limits object, or null if limitsJson is null
     */
    public static Limits fromJson(JsonObject limitsJson) {
        if (limitsJson == null) {
            return null;
        }

        Long iat = limitsJson.getLong("iat", 0L);

        Long dataUsageBytes = null;
        if (limitsJson.containsKey("dataUsage")) {
            String dataUsageString = limitsJson.getString("dataUsage");
            dataUsageBytes = parseDataUsageToBytes(dataUsageString);
        }

        Long apiHits = null;
        if (limitsJson.containsKey("apiHits")) {
            apiHits = limitsJson.getLong("apiHits");
        }

        return new Limits(iat, dataUsageBytes, apiHits);
    }

    /**
     * Converts a human-readable data limit string (e.g., "100:mb") into bytes.
     *
     * @param limit A string in the format "<value>:<unit>", e.g., "500:mb"
     * @return Limit in bytes
     * @throws OgcException if the unit is invalid or unsupported
     */
    private static Long parseDataUsageToBytes(String limit) {
        if (limit == null || limit.isEmpty()) {
            return null;
        }

        String[] parts = limit.split(":");
        if (parts.length != 2) {
            throw new OgcException(400, "Bad Request", "Invalid data usage format: " + limit);
        }

        long value = Long.parseLong(parts[0]);
        String unit = parts[1].toLowerCase();

        switch (unit) {
            case "kb":
                return value * 1024L;
            case "mb":
                return value * 1024L * 1024L;
            case "gb":
                return value * 1024L * 1024L * 1024L;
            case "tb":
                return value * 1024L * 1024L * 1024L * 1024L;
            default:
                throw new OgcException(400, "Bad Request", "Invalid data usage unit: " + unit);
        }
    }

    public Long getPolicyIssuedAt() {
        return policyIssuedAt;
    }

    public Long getDataUsageLimitInBytes() {
        return dataUsageLimitInBytes;
    }

    public Long getApiHitsLimit() {
        return apiHitsLimit;
    }
}
