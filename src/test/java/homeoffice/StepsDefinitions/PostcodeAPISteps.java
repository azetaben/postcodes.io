package homeoffice.StepsDefinitions;

import homeoffice.PageObject.PostcodeApi;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;
import io.restassured.response.Response;
import net.serenitybdd.core.environment.WebDriverConfiguredEnvironment;
import net.serenitybdd.model.environment.EnvironmentSpecificConfiguration;
import org.junit.jupiter.api.Assertions;

import java.util.*;

import static org.hamcrest.Matchers.*;

public class PostcodeAPISteps {
    private final PostcodeApi api = new PostcodeApi(EnvironmentSpecificConfiguration
            .from(WebDriverConfiguredEnvironment.getEnvironmentVariables())
            .getProperty("postcode.base.url"));
    private Response response;
    private String postcode;
    private double latitude;
    private double longitude;
    private boolean coordinatesAvailable;

    @Given("the API service is healthy")
    public void verifyAPIHealth() {
        response = api.health();
        response.then().statusCode(200).body("status", equalTo(200));
    }

    @When("I lookup the postcode {string}")
    public void lookupPostcode(String value) {
        postcode = value;
        coordinatesAvailable = false;
        response = api.lookup(value);
    }

    @When("I look up postcode {string}")
    public void lookupPostcodeForCoordinatesFeature(String value) { lookupPostcode(value); }

    @Then("the lookup response status should be {int}")
    public void lookupResponseStatus(int expected) { response.then().statusCode(expected); }

    @Then("the nearest search response status should be {int}")
    public void nearestResponseStatus(int expected) { response.then().statusCode(expected); }

    @And("the lookup response postcode should be {string}")
    public void lookupResponsePostcode(String expected) {
        response.then().body("result.postcode", equalTo(expected));
    }

    @And("the lookup response should contain valid longitude and latitude")
    public void lookupHasCoordinates() {
        extractCoordinates(response);
        Assertions.assertTrue(longitude >= -180 && longitude <= 180, "Longitude is outside WGS84 range");
        Assertions.assertTrue(latitude >= -90 && latitude <= 90, "Latitude is outside WGS84 range");
        coordinatesAvailable = true;
    }

    @And("the lookup response should match the postcode schema:")
    public void lookupResponseMatchesPostcodeSchema(DataTable schemaTable) {
        response.then().contentType("application/json").statusCode(200).body("status", equalTo(200));
        Map<String, Object> result = response.jsonPath().getMap("result");
        Assertions.assertNotNull(result, "Lookup result must be a JSON object");
        validateSchemaFields(result, schemaTable);
    }

    @And("the nearest response should match the postcode schema")
    public void nearestResponseMatchesPostcodeSchema() {
        response.then().contentType("application/json").statusCode(200).body("status", equalTo(200));
        List<Map<String, Object>> results = response.jsonPath().getList("result");
        Assertions.assertNotNull(results, "Nearest result must be a JSON array");
        Assertions.assertFalse(results.isEmpty(), "Nearest search must return at least one result");
        for (Map<String, Object> result : results) {
            assertStringField(result, "postcode", false);
            assertStringField(result, "outcode", false);
            assertStringField(result, "incode", false);
            assertStringField(result, "country", false);
            assertStringField(result, "region", true);
            assertNumberField(result, "quality", false);
            assertNumberField(result, "latitude", true);
            assertNumberField(result, "longitude", true);
            assertNumberField(result, "eastings", false);
            assertNumberField(result, "northings", false);
            assertNumberField(result, "distance", true);
            Assertions.assertInstanceOf(Map.class, result.get("codes"), "codes must be a JSON object");
        }
    }

    @When("I search for nearest postcodes using the lookup coordinates")
    public void nearestUsingLookupCoordinates() {
        Assertions.assertTrue(coordinatesAvailable, "Cannot search nearest postcodes without lookup coordinates");
        response = api.nearest(longitude, latitude, 10);
    }

    @And("the first nearest postcode should be {string}")
    public void firstNearestPostcode(String expected) {
        response.then().body("result[0].postcode", equalTo(expected));
    }

    @And("the nearest results should be ordered by ascending distance")
    public void nearestResultsAscending() { verifyDistanceOrdering(); }

    @And("the lookup response should contain an error message")
    public void lookupContainsError() {
        String error = response.jsonPath().getString("error");
        Assertions.assertNotNull(error, "Error response should include an error message");
        Assertions.assertFalse(error.isBlank(), "Error message should not be blank");
    }

    @And("no coordinates should be available for the nearest search")
    public void noCoordinatesAvailable() {
        Assertions.assertFalse(coordinatesAvailable, "Invalid lookup must not provide coordinates");
        Assertions.assertNull(response.jsonPath().get("result.latitude"), "Invalid postcode must not have latitude");
        Assertions.assertNull(response.jsonPath().get("result.longitude"), "Invalid postcode must not have longitude");
    }

    @When("I search for the {int} nearest postcode within {int} meters using the lookup coordinates")
    public void nearestWithLimitAndRadius(int limit, int radius) {
        Assertions.assertTrue(coordinatesAvailable, "Cannot search nearest postcodes without lookup coordinates");
        response = api.nearest(longitude, latitude, limit, radius);
    }

    @And("the nearest search should return at most {int} result")
    public void nearestAtMost(int count) {
        response.then().body("result.size()", lessThanOrEqualTo(count));
    }

    @And("the nearest search should return exactly {int} result")
    public void nearestExactly(int count) {
        response.then().body("result.size()", equalTo(count));
    }

    @And("the first nearest result should be within {int} meters")
    public void firstNearestWithinDistance(int maxDistance) {
        Number distance = response.jsonPath().get("result[0].distance");
        Assertions.assertNotNull(distance, "Nearest result must include distance");
        Assertions.assertTrue(distance.doubleValue() <= maxDistance,
                "Nearest result is outside the requested " + maxDistance + " meter radius");
    }

    @When("I request nearest postcodes for postcode {string} with limit {int}")
    public void nearestForPostcodeValue(String value, int limit) {
        postcode = value;
        response = api.nearestForPostcode(value, limit);
    }

    @Then("the response status code should be {int}")
    public void verifyStatusCode(int expected) { response.then().statusCode(expected); }

    @And("the response should contain the following fields:")
    public void verifyResponseFields(DataTable table) {
        for (Map<String, String> field : table.asMaps(String.class, String.class))
            response.then().body("result." + field.get("field"), is(field.get("value")));
    }

    @And("all coordinate values should be within UK boundaries")
    public void verifyCoordinateBoundaries() {
        double lat = response.jsonPath().getDouble("result.latitude");
        double lon = response.jsonPath().getDouble("result.longitude");
        Assertions.assertTrue(lat > 49.8 && lat < 60.9, "Latitude outside UK bounds");
        Assertions.assertTrue(lon > -8.74 && lon < 1.96, "Longitude outside UK bounds");
    }

    @When("I perform a bulk lookup with the following postcodes:")
    public void bulkLookupPostcodes(List<String> values) { response = api.bulkLookup(values); }

    @And("all responses should be received within {int} milliseconds")
    public void verifyResponseTime(int maxMillis) {
        Assertions.assertTrue(response.getTime() < maxMillis,
                "API response took " + response.getTime() + " ms; limit is " + maxMillis + " ms");
    }

    @And("each postcode result should contain required fields")
    public void verifyRequiredFields() {
        response.then().body("result.size()", greaterThan(0))
                .body("result.every { it.result != null }", is(true))
                .body("result.every { it.result.postcode != null }", is(true))
                .body("result.every { it.result.country != null }", is(true));
    }

    @When("I search for nearest postcodes using its coordinates")
    public void searchNearestPostcodes() {
        extractCoordinates(response);
        response = api.nearest(longitude, latitude, 10);
    }

    @And("the nearest postcodes should be within {int} meters")
    public void verifyNearestPostcodesDistance(int maxDistance) {
        response.then().body("result", everyItem(hasKey("distance")))
                .body("result.findAll { it.distance <= " + maxDistance + " }.size()", equalTo(response.path("result.size()")));
    }

    @And("results should be ordered by distance")
    public void verifyDistanceOrdering() {
        List<Float> distances = response.jsonPath().getList("result.distance", Float.class);
        for (int i = 1; i < distances.size(); i++)
            Assertions.assertTrue(distances.get(i - 1) <= distances.get(i), "Results not ordered by distance");
    }

    @And("the error message should contain {string}")
    public void verifyErrorMessage(String text) { response.then().body("error", containsString(text)); }

    @And("the response should include a validation error")
    public void responseIncludesValidationError() {
        String error = response.jsonPath().getString("error");
        Assertions.assertNotNull(error, "Validation response should include an error message");
        Assertions.assertFalse(error.isBlank(), "Validation error should not be blank");
    }

    @Then("verify administrative hierarchy:")
    public void verifyAdministrativeHierarchy(DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class))
            response.then().body("result." + row.get("level"), equalTo(row.get("value")));
    }

    @And("I extract the longitude and latitude from the lookup response")
    public void extractLookupCoordinates() { extractCoordinates(response); }

    @And("I search for nearest postcodes with its coordinates")
    public void searchNearestWithExtractedCoordinates() { response = api.nearest(longitude, latitude, 10); }

    @And("the nearest postcode should be {string}")
    public void verifyNearestPostcode(String expected) {
        response.then().body("result[0].postcode", equalTo(expected));
    }

    @When("I search for postcodes matching {string} with limit {int}")
    public void searchPostcodes(String query, int limit) { response = api.search(query, limit); }

    @And("the result count should be at most {int}")
    public void resultCountAtMost(int limit) { response.then().body("result.size()", lessThanOrEqualTo(limit)); }

    @And("the search result should contain exactly {int} postcodes")
    public void searchResultExactly(int count) {
        Object result = response.jsonPath().get("result");
        if (count == 0) {
            Assertions.assertTrue(result == null || (result instanceof List<?> results && results.isEmpty()),
                    "Expected no postcode matches, but the API returned: " + result);
            return;
        }

        Assertions.assertInstanceOf(List.class, result, "Search result must be a JSON array");
        Assertions.assertEquals(count, ((List<?>) result).size(), "Unexpected number of postcode matches");
    }

    @When("I perform a bulk lookup with {int} copies of postcode {string}")
    public void bulkLookupRepeatedPostcodes(int count, String value) {
        Assertions.assertTrue(count >= 1 && count <= 100, "Bulk lookup count must be between 1 and 100");
        response = api.bulkLookup(Collections.nCopies(count, value));
    }

    @And("the bulk lookup should return exactly {int} results")
    public void bulkLookupExactlyResults(int count) {
        response.then().body("result.size()", equalTo(count));
    }

    @When("I lookup the outcode {string}")
    public void lookupOutcode(String outcode) { response = api.outcode(outcode); }

    @And("the outcode response should contain its outward code {string}")
    public void verifyOutcode(String expected) { response.then().body("result.outcode", equalTo(expected)); }

    @When("I request the nearest postcodes for the current postcode with limit {int}")
    public void nearestForCurrentPostcode(int limit) { response = api.nearestForPostcode(postcode, limit); }

    @And("the nearest postcode list should contain at most {int} results")
    public void nearestCountAtMost(int limit) { response.then().body("result.size()", lessThanOrEqualTo(limit)); }

    @When("I bulk reverse geocode the current coordinates")
    public void bulkReverseGeocode() { response = api.bulkReverse(longitude, latitude); }

    @And("the bulk reverse result should resolve to {string}")
    public void verifyBulkReverse(String expected) {
        response.then().body("result[0].result.postcode", hasItem(expected));
    }

    @And("the bulk lookup should return a result for each input")
    public void bulkLookupResultForEachInput() {
        response.then().body("result.size()", equalTo(2))
                .body("result[0].result.postcode", equalTo("SW1A 1AA"))
                .body("result[1].result", nullValue())
                .body("result[1].query", equalTo("INVALID"));
    }

    @When("I search nearest postcodes with longitude {double} but without latitude")
    public void nearestWithoutRequiredLatitude(double lon) {
        response = api.nearestWithoutLatitude(lon);
    }

    private void extractCoordinates(Response from) {
        Number lat = from.jsonPath().get("result.latitude");
        Number lon = from.jsonPath().get("result.longitude");
        Assertions.assertNotNull(lat, "Lookup latitude was null");
        Assertions.assertNotNull(lon, "Lookup longitude was null");
        latitude = lat.doubleValue();
        longitude = lon.doubleValue();
        coordinatesAvailable = true;
    }

    private void assertStringField(Map<String, Object> object, String field, boolean nullable) {
        Object value = object.get(field);
        if (nullable && value == null) return;
        Assertions.assertInstanceOf(String.class, value, field + " must be a string" + (nullable ? " or null" : ""));
        String text = (String) value;
        Assertions.assertFalse(text.isBlank(), field + " must not be blank");
    }

    private void assertNumberField(Map<String, Object> object, String field, boolean nullable) {
        Object value = object.get(field);
        if (nullable && value == null) return;
        Assertions.assertInstanceOf(Number.class, value, field + " must be numeric" + (nullable ? " or null" : ""));
    }

    private void validateSchemaFields(Map<String, Object> object, DataTable schemaTable) {
        for (Map<String, String> row : schemaTable.asMaps(String.class, String.class)) {
            String field = row.get("field");
            String type = row.get("type");
            boolean nullable = Boolean.parseBoolean(row.get("nullable"));
            switch (type.toLowerCase(Locale.ROOT)) {
                case "string" -> assertStringField(object, field, nullable);
                case "number" -> assertNumberField(object, field, nullable);
                case "object" -> {
                    Object value = object.get(field);
                    if (nullable && value == null) break;
                    Assertions.assertInstanceOf(Map.class, value, field + " must be a JSON object" + (nullable ? " or null" : ""));
                }
                default -> throw new IllegalArgumentException("Unsupported schema type: " + type);
            }
        }
    }
}
