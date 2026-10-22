@API
Feature: UK Postcode API Testing Suite
  As a QAT Engineer
  I want to thoroughly test the postcode API's functionality and reliability
  So that I can ensure the service meets quality standards

  Background:
    Given the API service is healthy

  @Smoke @DataValidation
  Scenario: Comprehensive validation of single postcode data
    When I lookup the postcode "SW1A 1AA"
    Then the response status code should be 200
    And the response should contain the following fields:
      | field           | value            |
      | postcode       | SW1A 1AA         |
      | country        | England          |
      | region         | London           |
    And all coordinate values should be within UK boundaries

  @Performance @Bulk @Regression
  Scenario: Multiple postcode lookup performance test
    When I perform a bulk lookup with the following postcodes:
      | SW1A 1AA |
      | SW1A 2AA |
      | W1A 1AA  |
    Then the response status code should be 200
    And all responses should be received within 2000 milliseconds
    And each postcode result should contain required fields

  @Integration @Regression
  Scenario: Cross-validate postcode data with nearest endpoint
    When I lookup the postcode "SW1A 1AA"
    And I search for nearest postcodes using its coordinates
    Then the response status code should be 200
    And the nearest postcodes should be within 1000 meters
    And results should be ordered by distance

  @Validation @Negative @Regression
  Scenario Outline: Invalid postcode format handling
    When I lookup the postcode "<postcode>"
    Then the response status code should be <status_code>
    And the error message should contain "<error_text>"

    Examples:
      | postcode | status_code | error_text        |
      | INVALID  | 404         | Invalid postcode  |
      | @@@@     | 404         | Invalid postcode  |
      | 123      | 404         | Invalid postcode  |

  @Boundaries @Regions @Regression
  Scenario: Regional boundary and administrative area validation
    When I lookup the postcode "SW1A 1AA"
    Then verify administrative hierarchy:
      | level          | value                               |
      | admin_ward     | St James's                          |
      | admin_district | Westminster                         |
      | region         | London                              |

  @Integration @Regression @PostcodeChain
  Scenario: SW1A 1AA is the nearest postcode to its own coordinates
    When I lookup the postcode "SW1A 1AA"
    Then the response status code should be 200
    And the response should contain the following fields:
      | field    | value    |
      | postcode | SW1A 1AA |
    And I extract the longitude and latitude from the lookup response
    And I search for nearest postcodes with its coordinates
    Then the response status code should be 200
    And the nearest postcode should be "SW1A 1AA"
    And the nearest postcodes should be within 1000 meters
    And results should be ordered by distance

  # Predicted practical interview questions implemented as executable API scenarios:
  # 1) How would you test prefix search and enforce a caller-supplied result limit?
  @API @Search @Regression
  Scenario: Prefix search respects the requested result limit
    When I search for postcodes matching "SW1A" with limit 5
    Then the response status code should be 200
    And the result count should be at most 5

  # 2) How would you validate an outward-code resource and its response schema?
  @API @Outcode @Regression
  Scenario: Lookup an outward code and validate its location
    When I lookup the outcode "SW1A"
    Then the response status code should be 200
    And the outcode response should contain its outward code "SW1A"

  # 3) How would you test the postcode-specific nearest endpoint and limit parameter?
  @API @Nearest @Regression
  Scenario: Nearest endpoint returns the requested number of postcode matches
    When I lookup the postcode "SW1A 1AA"
    And I request the nearest postcodes for the current postcode with limit 5
    Then the response status code should be 200
    And the nearest postcode list should contain at most 5 results
    And results should be ordered by distance

  # 4) How would you cover bulk lookup with a mixture of valid and invalid entries?
  @API @Bulk @Negative @Regression
  Scenario: Bulk lookup reports valid and invalid postcode entries independently
    When I perform a bulk lookup with the following postcodes:
      | SW1A 1AA |
      | INVALID  |
    Then the response status code should be 200
    And the bulk lookup should return a result for each input

  # 5) How would you test reverse geocoding in bulk and relate it to a known lookup?
  @API @ReverseGeocoding @Regression
  Scenario: Bulk reverse geocoding resolves coordinates to the source postcode
    When I lookup the postcode "SW1A 1AA"
    And I extract the longitude and latitude from the lookup response
    And I bulk reverse geocode the current coordinates
    Then the response status code should be 200
    And the bulk reverse result should resolve to "SW1A 1AA"

  # 6) How would you test that the API rejects a missing required coordinate?
  @API @Validation @Negative @Regression
  Scenario: Reverse geocoding rejects a request missing its required latitude
    When I search nearest postcodes with longitude 0 but without latitude
    Then the response status code should be 400
    And the response should include a validation error

  # Interview edge-case scenarios

  # 1) Does the lookup normalize postcode casing and spacing?
  @EdgeCase @Normalization
  Scenario: Lookup accepts lowercase postcode without an internal space
    When I lookup the postcode "sw1a1aa"
    Then the response status code should be 200
    And the response should contain the following fields:
      | field    | value    |
      | postcode | SW1A 1AA |

  # 2) Does a validly shaped but unmatched prefix return a safe empty result?
  @EdgeCase @Search
  Scenario: Prefix search with no matches returns no postcode matches
    When I search for postcodes matching "NOTAREALPOSTCODE999" with limit 10
    Then the response status code should be 200
    And the search result should contain exactly 0 postcodes

  # 3) Is the minimum useful search limit of one respected?
  @EdgeCase @Search @Boundary
  Scenario: Prefix search with the minimum limit returns no more than one result
    When I search for postcodes matching "SW1A" with limit 1
    Then the response status code should be 200
    And the result count should be at most 1

  # 4) Is the documented maximum search limit of 100 respected?
  @EdgeCase @Search @Boundary
  Scenario: Prefix search at the maximum documented limit never exceeds 100 results
    When I search for postcodes matching "SW1A" with limit 100
    Then the response status code should be 200
    And the result count should be at most 100

  # 5) Does the outcode endpoint return a clear not-found response?
  @EdgeCase @Outcode @Negative
  Scenario: An unknown outcode returns a not-found error
    When I lookup the outcode "ZZ99"
    Then the response status code should be 404
    And the response should include a validation error

  # 6) Does nearest-by-postcode honor the smallest valid result limit?
  @EdgeCase @Nearest @Boundary
  Scenario: Nearest-by-postcode with limit one returns one nearest result
    When I lookup the postcode "SW1A 1AA"
    And I request the nearest postcodes for the current postcode with limit 1
    Then the response status code should be 200
    And the nearest postcode list should contain at most 1 results

  # 7) Does reverse geocoding include the source postcode at the minimum radius?
  @EdgeCase @ReverseGeocoding @Boundary
  Scenario: Coordinate search at the minimum radius finds SW1A 1AA
    When I lookup the postcode "SW1A 1AA"
    And I extract the longitude and latitude from the lookup response
    And I search for the 1 nearest postcode within 1 meters using the lookup coordinates
    Then the response status code should be 200
    And the nearest postcode should be "SW1A 1AA"
    And the nearest postcodes should be within 1 meters

  # 8) Does reverse geocoding enforce the documented maximum radius?
  @EdgeCase @ReverseGeocoding @Boundary
  Scenario: Coordinate search at the maximum documented radius returns ordered results
    When I lookup the postcode "SW1A 1AA"
    And I extract the longitude and latitude from the lookup response
    And I search for the 100 nearest postcode within 2000 meters using the lookup coordinates
    Then the response status code should be 200
    And the result count should be at most 100
    And the nearest postcodes should be within 2000 meters
    And results should be ordered by distance

  # 9) Does bulk lookup accept exactly its documented maximum of 100 inputs?
  @EdgeCase @Bulk @Boundary
  Scenario: Bulk postcode lookup accepts the documented maximum of 100 inputs
    When I perform a bulk lookup with 100 copies of postcode "SW1A 1AA"
    Then the response status code should be 200
    And the bulk lookup should return exactly 100 results

  # 10) Does bulk lookup preserve valid results when another item is invalid?
  @EdgeCase @Bulk @Negative
  Scenario: Bulk lookup keeps a valid postcode result alongside an invalid input
    When I perform a bulk lookup with the following postcodes:
      | SW1A 1AA |
      | INVALID  |
    Then the response status code should be 200
    And the bulk lookup should return a result for each input
