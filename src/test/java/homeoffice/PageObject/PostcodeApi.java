package homeoffice.PageObject;

import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

/** API page object: owns Postcodes.io paths and HTTP requests. */
public class PostcodeApi {
    private final HttpApiClient client;

    public PostcodeApi(String baseUrl) {
        this.client = new HttpApiClient(baseUrl);
    }

    public Response health() {
        // The site root serves the documentation HTML; probe a stable JSON API resource instead.
        return client.get("/postcodes/SW1A1AA");
    }

    public Response lookup(String postcode) {
        return client.getWithPathParams("/postcodes/{postcode}", Map.of("postcode", postcode));
    }

    public Response nearest(double longitude, double latitude, int limit) {
        return client.get("/postcodes", Map.of(
                "lon", longitude, "lat", latitude, "limit", limit));
    }

    public Response nearest(double longitude, double latitude, int limit, int radius) {
        return client.get("/postcodes", Map.of(
                "lon", longitude, "lat", latitude, "limit", limit, "radius", radius));
    }

    public Response nearestWithoutLatitude(double longitude) {
        return client.get("/postcodes", Map.of("lon", longitude));
    }

    public Response bulkLookup(List<String> postcodes) {
        return client.post("/postcodes", Map.of("postcodes", postcodes));
    }

    public Response search(String query, int limit) {
        return client.get("/postcodes", Map.of("query", query, "limit", limit));
    }

    public Response outcode(String outcode) {
        return client.getWithPathParams("/outcodes/{outcode}", Map.of("outcode", outcode));
    }

    public Response nearestForPostcode(String postcode, int limit) {
        return client.getWithPathParams("/postcodes/{postcode}/nearest",
                Map.of("postcode", postcode), Map.of("limit", limit), Map.of());
    }

    public Response bulkReverse(double longitude, double latitude) {
        return client.post("/postcodes",
                Map.of("geolocations", List.of(Map.of("longitude", longitude, "latitude", latitude))));
    }
}
