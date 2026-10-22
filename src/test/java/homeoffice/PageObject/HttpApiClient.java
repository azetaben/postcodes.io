package homeoffice.PageObject;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;

import java.util.Map;

/**
 * Small reusable HTTP client for API page objects.
 *
 * All methods return the complete Rest Assured response. Callers decide which
 * status codes and response fields are expected for their endpoint/scenario.
 */
public class HttpApiClient {
    private final String baseUrl;

    public HttpApiClient(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("API base URL must not be blank");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public Response get(String path) {
        return get(path, Map.of(), Map.of());
    }

    public Response get(String path, Map<String, ?> queryParams) {
        return get(path, queryParams, Map.of());
    }

    public Response get(String path, Map<String, ?> queryParams, Map<String, ?> headers) {
        return request(queryParams, headers, null).get(path);
    }

    public Response getWithPathParams(String path, Map<String, ?> pathParams) {
        return getWithPathParams(path, pathParams, Map.of(), Map.of());
    }

    public Response getWithPathParams(String path,
                                      Map<String, ?> pathParams,
                                      Map<String, ?> queryParams,
                                      Map<String, ?> headers) {
        RequestSpecification request = request(queryParams, headers, null);
        if (pathParams != null && !pathParams.isEmpty()) {
            request.pathParams(pathParams);
        }
        return request.get(path);
    }

    public Response post(String path, Object body) {
        return post(path, body, Map.of(), Map.of());
    }

    public Response post(String path, Object body, Map<String, ?> queryParams, Map<String, ?> headers) {
        return request(queryParams, headers, body).post(path);
    }

    public Response put(String path, Object body) {
        return put(path, body, Map.of(), Map.of());
    }

    public Response put(String path, Object body, Map<String, ?> queryParams, Map<String, ?> headers) {
        return request(queryParams, headers, body).put(path);
    }

    public Response patch(String path, Object body) {
        return patch(path, body, Map.of(), Map.of());
    }

    public Response patch(String path, Object body, Map<String, ?> queryParams, Map<String, ?> headers) {
        return request(queryParams, headers, body).patch(path);
    }

    public Response delete(String path) {
        return delete(path, Map.of(), Map.of());
    }

    public Response delete(String path, Map<String, ?> queryParams, Map<String, ?> headers) {
        return request(queryParams, headers, null).delete(path);
    }

    public Response delete(String path, Object body, Map<String, ?> queryParams, Map<String, ?> headers) {
        return request(queryParams, headers, body).delete(path);
    }

    private RequestSpecification request(Map<String, ?> queryParams,
                                         Map<String, ?> headers,
                                         Object body) {
        RequestSpecification request = SerenityRest.given()
                .baseUri(baseUrl)
                .accept(ContentType.JSON)
                .filter(new PostcodeApiLoggingFilter());

        if (queryParams != null && !queryParams.isEmpty()) {
            request.queryParams(queryParams);
        }
        if (headers != null && !headers.isEmpty()) {
            request.headers(headers);
        }
        if (body != null) {
            request.contentType(ContentType.JSON).body(body);
        }
        return request;
    }
}
