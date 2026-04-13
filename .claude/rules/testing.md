# Testing & Code Quality Rules

## TDD Workflow

This project follows spec-driven TDD. For every new feature:
1. Write or update the BDD spec in `src/test/resources/specs/`
2. Write failing tests that verify the spec
3. Implement the minimum code to make tests pass
4. Refactor while keeping tests green
5. Run SonarQube analysis before considering the feature done

## Test Classification

| Suffix | Type | Runner | Spring Context | Tags |
|---|---|---|---|---|
| `*Test.java` | Unit | Surefire | No | - |
| `*IT.java` | Integration | Failsafe | Yes | - |
| `*PlaywrightIT.java` | E2E | Failsafe | Yes | `@Tag("playwright")` |

- Surefire excludes `playwright` and `huggingface` tags by default
- HuggingFace integration tests use `@Tag("huggingface")` -- skipped when no API token

## Test Requirements

### Unit Tests (`*Test.java`)
- No Spring context, no network, no filesystem (except test resources)
- Use mocks (Mockito) for dependencies -- mock interfaces, not implementations
- Fast: entire unit test suite must run in under 30 seconds
- Every public method in pipeline interfaces must have at least one test
- Use `@ParameterizedTest` with `@MethodSource` for fixture-based tests
- AssertJ for assertions (`assertThat(...)`)

### Integration Tests (`*IT.java`)
- Use `@SpringBootTest` with appropriate profiles
- `@ActiveProfiles("tesseract")` or `@ActiveProfiles("huggingface")`
- Test real backend behavior with fixture images
- Use graduated assertion thresholds by difficulty level:
  - Easy fixtures: exact or near-exact text match
  - Medium fixtures: key phrases must be present
  - Hard fixtures: at least 50% of expected content recognized

### Playwright E2E Tests (`*PlaywrightIT.java`)
- Use `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`
- Reference UI components by their assigned IDs (see CLAUDE.md)
- Test complete user flows: upload -> process -> edit -> preview -> download
- Clean up browser resources in `@AfterEach`

## SonarQube Quality Gate

All code must meet these thresholds (enforced by `mvn sonar:sonar`):

| Metric | Threshold |
|---|---|
| Code coverage (new code) | >= 80% |
| Code coverage (overall) | >= 70% |
| Duplicated lines | < 3% |
| Maintainability rating | A |
| Reliability rating | A |
| Security rating | A |
| Security hotspots reviewed | 100% |
| Bugs | 0 (new code) |
| Vulnerabilities | 0 (new code) |
| Code smells (new code) | 0 (blocker/critical) |

### SonarQube Configuration

The `pom.xml` must include the `sonar-maven-plugin` and these properties:
```xml
<properties>
    <sonar.java.source>21</sonar.java.source>
    <sonar.coverage.jacoco.xmlReportPaths>${project.build.directory}/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
    <sonar.exclusions>**/AppShell.java,**/Application.java</sonar.exclusions>
    <sonar.test.inclusions>**/*Test.java,**/*IT.java</sonar.test.inclusions>
</properties>
```

JaCoCo plugin must be configured for coverage reporting:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution><goals><goal>prepare-agent</goal></goals></execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

## Code Quality Rules

### General
- No `@SuppressWarnings` without a comment explaining why
- No empty catch blocks -- at minimum log the exception
- No raw types -- always use generics
- No `System.out.println` -- use SLF4J logging
- Method length: max 30 lines (excluding blank lines and comments)
- Class length: max 300 lines
- Cyclomatic complexity per method: max 10

### Spring / Vaadin Specific
- No field injection (`@Autowired` on fields) -- use constructor injection
- No `Optional` as method parameter -- only as return type
- Vaadin UI components must have IDs set for Playwright testability
- Use `@Value` or `@ConfigurationProperties` for configuration, never hardcode

### Security (SonarQube will flag these)
- No hardcoded credentials or API tokens
- Validate all file uploads (type, size) before processing
- Sanitize any user input before rendering in Markdown preview
