# pom.xml Cleanup

## 2026-03-02 (round 1)

### Removed: `commons-text` dependency + version property
- Zero usages anywhere in `src/` — no import of `org.apache.commons.text.*` in any Java file

### Removed: `gson` explicit dependency + version property
- Only usage is `GsonFactory` in `GoogleTokenExchanger`, which comes from `com.google.api.client.json.gson` — part of `google-api-client`, not the standalone Gson jar
- `google-api-client` already declared; Gson jar was redundant

### Removed: `bouncycastle` (`bcprov-jdk18on`) dependency
- Zero usages of `org.bouncycastle.*` anywhere in `src/` (main or test)

### Removed: `jackson-core` explicit dependency
- `jackson-databind` (pulled by `spring-boot-starter-web`) brings `jackson-core` transitively
- `JsonProcessingException` (the only direct import) is always available via the transitive pull

### Removed: `prod` and `dev` Maven profiles
- `prod` profile re-declared `micrometer-registry-prometheus` already unconditionally in main `<dependencies>` — no-op duplicate
- `dev` profile had activation logic but no content — dead config

## 2026-03-02 (round 2)

### Removed: `spring-boot.version` orphan property
- Declared but never referenced — parent version is hardcoded directly in `<parent>`

### Removed: `jakarta.mail-api` explicit dependency + version property
- Zero direct imports of `jakarta.mail.*` in production code
- `spring-boot-starter-mail` already brings the mail implementation transitively

### Removed: `google-auth-library-oauth2-http` explicit dependency + version property
- Zero direct imports of `com.google.auth.*` in production code
- `google-api-client` already brings it transitively

### Removed: `caffeine` explicit version pin + version property
- Spring Boot 3.5.11 BOM manages caffeine at `3.2.3`; removing the explicit `3.1.8` pin upgrades to the BOM version for free

### Removed: `<scope>compile</scope>` from guava
- `compile` is the Maven default scope — explicit declaration was noise

### Removed: `<generateModels>true</generateModels>` from 4 OpenAPI generator executions
- `true` is the plugin default — was only set on product/user/cart/security executions, not the other 4, making it inconsistent noise

## 2026-03-02 (round 3)

### Removed: `project.version` custom property
- `project.version` is a Maven built-in; the custom `0.0.1` declaration was never referenced and shadowed the built-in

### Fixed: `apiPackage`/`modelPackage` misplaced inside `<configOptions>` for 4 OpenAPI executions (bug)
- `apiPackage` and `modelPackage` are top-level `@Parameter` fields in the plugin, not generator config options
- Were silently ignored inside `<configOptions>` for payment, favorite, order, product-review executions
- Moved to the correct top-level `<configuration>` block, consistent with the other 4 executions

## 2026-03-02 (round 4)

### Removed: `maven.compiler.source` + `maven.compiler.target` properties
- Spring Boot parent already sets `maven.compiler.release=${java.version}` — these were redundant

### Fixed: compiler plugin `<source>`+`<target>` → `<release>`
- `<release>` is the modern flag that also enforces the bootstrap classpath; effective pom confirms `<release>25</release>` resolves correctly

### Dropped findings (false positives)
- `postgresql` version pin (`42.7.7`) — BOM has `42.7.10`; intentional downpin, owner's decision
- `liquibase` version pin (`4.32.0`) — BOM has `4.31.1`; intentional bump, keep
- `commons-lang3` version pin (`3.18.0`) — BOM has `3.17.0`; intentional bump, keep
- `maven-compiler-plugin` version pin (`3.15.0`) — BOM has `3.14.1`; intentional bump, keep
- `maven-surefire-plugin`, `jacoco-maven-plugin` version pins — not managed by Spring Boot parent; needed
- `jackson-databind` explicit dep — not in Spring Boot BOM; needed
- `junit-jupiter` explicit dep — not inside `spring-boot-starter-test`; needed directly

## 2026-03-02 (round 5)

### Fixed: `jjwt-impl` and `jjwt-jackson` scope → `runtime`
- All production imports (`Claims`, `ExpiredJwtException`, `JwtException`, `JwtParser`, `Jwts`, `Decoders`, `Keys`) are from `jjwt-api` only
- `jjwt-impl` and `jjwt-jackson` are implementation/serialization providers with no compile-time API surface — correct scope is `runtime`

### Dropped findings (false positives)
- `guava` version pin — `com.google.guava:guava` is not in Spring Boot BOM (BOM only has `com.github.ben-manes.caffeine:guava`); explicit pin needed
- `<layers><enabled>true</enabled></layers>` — layers are disabled by default in the plugin; this is intentional
- `<showWarnings>false</showWarnings>` — default is `true`; intentional suppression

## 2026-03-02 (round 6 — comments and grouping)

### Fixed: `langchain4j.version` misplaced under `SPRING FRAMEWORK VERSIONS`
- Moved to its own `AI VERSIONS` section; removed the now-empty `SPRING FRAMEWORK VERSIONS` header

### Fixed: AWS deps mixed into `SPRING FRAMEWORK` dependencies section
- `spring-retry`, `aws-sdk:s3`, `aws-sdk:url-connection-client` were all under `SPRING FRAMEWORK`
- `spring-retry` stays in Spring section; AWS deps moved to their own `AWS` section

### Fixed: `aws-sdk-bom.version` misplaced under `THIRD-PARTY INTEGRATIONS VERSIONS`
- Moved to its own `AWS VERSIONS` section to match the new `AWS` deps section

### Fixed: missing blank lines between dependency sections
- Added blank lines between `SECURITY & JWT` / `SERIALIZATION & LOGGING` and `THIRD-PARTY INTEGRATIONS` / `AI` sections

### Removed: redundant `<!-- Dependencies -->` comment above `<dependencies>` tag
