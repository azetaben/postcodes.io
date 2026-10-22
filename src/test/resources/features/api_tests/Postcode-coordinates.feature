@API @PostcodeLookup
Feature: Postcodes.io lookup and coordinate-based nearest search
  As an API consumer
  I want to validate postcode data and use its coordinates in a chained request
  So that the postcode and nearest-search responses conform to the API contract

  @Positive @Smoke @Schema @ChainedRequest
  Scenario: Validate SW1A 1AA schema and find it nearest to its own coordinates
    When I look up postcode "SW1A 1AA"
    Then the lookup response status should be 200
    And the lookup response postcode should be "SW1A 1AA"
    And the lookup response should match the postcode schema:
      | field      | type   | nullable |
      | postcode   | string | false    |
      | outcode    | string | false    |
      | incode     | string | false    |
      | country    | string | false    |
      | region     | string | true     |
      | quality    | number | false    |
      | latitude   | number | true     |
      | longitude  | number | true     |
      | eastings   | number | true     |
      | northings  | number | true     |
      | codes      | object | false    |
    And I extract the longitude and latitude from the lookup response
    When I search for nearest postcodes using the lookup coordinates
    Then the nearest search response status should be 200
    And the nearest response should match the postcode schema
    And the nearest results should be ordered by ascending distance
    And the first nearest postcode should be "SW1A 1AA"

  @Negative
  Scenario: An invalid postcode returns an error and no coordinates
    When I look up postcode "NOT A POSTCODE"
    Then the lookup response status should be 404
    And the lookup response should contain an error message
    And no coordinates should be available for the nearest search

  @Negative @Nearest
  Scenario: The nearest-postcodes endpoint rejects an invalid postcode
    When I request nearest postcodes for postcode "NOT A POSTCODE" with limit 1
    Then the response status code should be 404
    And the response should include a validation error

  @EdgeCase
  Scenario: A one-meter radius search returns exactly the nearest postcode
    When I look up postcode "SW1A 1AA"
    Then the lookup response status should be 200
    And the lookup response should match the postcode schema:
      | field      | type   | nullable |
      | postcode   | string | false    |
      | outcode    | string | false    |
      | incode     | string | false    |
      | country    | string | false    |
      | region     | string | true     |
      | quality    | number | false    |
      | latitude   | number | true     |
      | longitude  | number | true     |
      | eastings   | number | true     |
      | northings  | number | true     |
      | codes      | object | false    |
    And I extract the longitude and latitude from the lookup response
    When I search for the 1 nearest postcode within 1 meters using the lookup coordinates
    Then the nearest search response status should be 200
    And the nearest search should return exactly 1 result
    And the first nearest postcode should be "SW1A 1AA"
    And the first nearest result should be within 1 meters
    And the nearest results should be ordered by ascending distance
