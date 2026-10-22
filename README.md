# QAT Java Test Automation

A reference Java test automation framework demonstrating UI and API test
patterns with Serenity BDD, Cucumber, Selenium WebDriver, and REST Assured.

- **UI tests** target the public [SauceDemo](https://www.saucedemo.com/) site.
- **API tests** target the public [Postcodes.io](https://api.postcodes.io/docs) service.

## Quick start

```bash
git clone <repo-url>
cd java-assessment
./mvnw clean verify -Dcucumber.filter.tags="@Smoke"
```

A successful run will:
- download the Maven dependencies
- launch Chrome (headless) against SauceDemo
- exercise the public Postcodes.io API
- write a Serenity report to `target/site/serenity/index.html`

## Stack

| Layer | Tool |
|------|------|
| Test runner       | JUnit Platform Suite |
| BDD               | Cucumber 7 (Gherkin features under `src/test/resources/features`) |
| UI automation     | Serenity BDD + Selenium WebDriver |
| API automation    | Serenity BDD + REST Assured |
| Reporting         | Serenity reports (`target/site/serenity/index.html`) |
| Container exec    | Docker Compose + Selenium Grid (optional, for remote runs) |

The framework follows the standard Serenity three-layer pattern:

```
features/*.feature                 ← Gherkin (business language)
  └─► StepsDefinitions/*.java      ← Cucumber glue
        └─► actions/*Steps.java    ← UIInteractionSteps (behaviour, assertions)
              └─► PageObject/*.java ← Locators (PageObject)
```

See `SauceDemoHome` / `LoginSteps` / `LoginStepDefinitions` for the canonical UI
flow, and `SauceDemoInventory` / `InventorySteps` / `InventoryStepDefinitions`
for a second example covering catalogue interactions.

## Prerequisites

- JDK 11 or higher
- Maven 3.8.4 or higher (or use the included `./mvnw` wrapper)
- Chrome installed locally
- Docker + Docker Compose (only for remote / grid execution)

## Running tests

### Local

```bash
# all tests
./mvnw clean verify

# UI only
./mvnw clean verify -Dcucumber.filter.tags="@UI"

# API only
./mvnw clean verify -Dcucumber.filter.tags="@API"

# Smoke tests across UI + API
./mvnw clean verify -Dcucumber.filter.tags="@Smoke"

# Regression tests
./mvnw clean verify -Dcucumber.filter.tags="@Regression"
```

### Remote (Selenium Grid via Docker)

```bash
docker-compose up -d --build
./mvnw clean verify -Denvironment=remote
docker-compose down
```

## Test reports

Serenity generates a full HTML report after each run:

```
target/site/serenity/index.html
```

Open it in a browser to see scenario steps, screenshots (UI), request/response
detail (API), and timings.

## Tags in use

| Tag | Meaning |
|------|--------|
| `@UI`         | UI tests against SauceDemo |
| `@API`        | API tests against Postcodes.io |
| `@Smoke`      | Critical-path subset, used as a quick verification |
| `@Regression` | Broader coverage suite |

## Configuration

Environment-specific config lives in [`src/test/resources/serenity.conf`](src/test/resources/serenity.conf).

- `default` — local Chrome, headless mode on
- `remote`  — Selenium Grid endpoint at `http://selenium-hub:4444/wd/hub`

Parallel execution is **disabled by default** (`parallel.methods = false`). Set
to `true` and tune `thread.count` to enable.

## Repository layout

```
src/test/java/homeoffice/
├── CucumberTestSuite.java            ← entry point, picks up features
├── PageObject/                       ← locators + @DefaultUrl
│   ├── SauceDemoHome.java
│   └── SauceDemoInventory.java
├── actions/                          ← UIInteractionSteps (behaviour)
│   ├── LoginSteps.java
│   ├── InventorySteps.java
│   └── NavigateSteps.java
└── StepsDefinitions/                 ← Cucumber glue
    ├── LoginStepDefinitions.java
    ├── InventoryStepDefinitions.java
    └── PostcodeAPISteps.java

src/test/resources/
├── features/
│   ├── ui_tests/
│   │   ├── Sauce_Login.feature
│   │   └── Sauce_Inventory.feature
│   └── api_tests/
│       └── Postcodes_API.feature
├── serenity.conf
├── junit-platform.properties
└── logback-test.xml
```

## Troubleshooting

| Symptom | First thing to check |
|---------|----------------------|
| Maven dependency download fails  | Corporate proxy / VPN; try a different network |
| ChromeDriver version mismatch    | Serenity auto-downloads; ensure Chrome is up to date |
| Tests pass locally, fail in grid | Inspect `docker-compose logs selenium-hub` |
| No Serenity report generated     | Run `./mvnw clean verify` (not `test`) — report binds to `verify` phase |
