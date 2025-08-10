# DeepComplexity – Developer Guidelines

Short, practical notes to get productive quickly.

## 1) Tech stack snapshot

- Languages: Kotlin (primary), Java
- Build: Gradle (Kotlin DSL) via Gradle Wrapper
- JDK: 17 (kotlin.jvmToolchain(17))
- Platform: IntelliJ Platform plugin (see src/main/resources/META-INF/plugin.xml)
- Testing: JUnit (on JUnit Platform), Kotlin + Java tests
- QA: Kover (coverage), Qodana (static analysis), Plugin Verifier

## 2) Repository layout (what lives where)

- src/main/kotlin | src/main/java – plugin sources
- src/main/resources – resources, META-INF/plugin.xml
- src/test/kotlin | src/test/java – tests and test data
- gradle/*, build.gradle.kts, settings.gradle.kts – build setup
- .junie/guidelines.md – this file

Keep code cohesive by feature:

- evaluation/* – expression/evaluation logic
- staticAnalysis/* – constrained sets, variances, number simplification
- utilities/* – shared helpers

## 3) Prereqs & environment

- Install JDK 17 and IntelliJ IDEA (Community or Ultimate)
- Prefer running via the Gradle Wrapper (no local Gradle needed)

## 4) Common Gradle tasks

Use PowerShell on Windows (./gradlew -> .\gradlew.bat) and Bash elsewhere:

- Build: ./gradlew build
- Run tests (all): ./gradlew test

IntelliJ plugin tasks:

- Run IDE with plugin: ./gradlew runIde
- UI test IDE: ./gradlew runIdeForUiTests
- Package plugin ZIP: ./gradlew buildPlugin
- Verify against IDEs: ./gradlew verifyPlugin

Quality tools:

- Coverage report (XML): ./gradlew koverXmlReport
- Qodana (local): see qodana.yml (requires Qodana CLI/IDE integration)

## 5) Executing scripts / entry points

- Prefer creating/running Kotlin test cases under src/test to exercise logic.
- For ad‑hoc runs, add a minimal @Test or a temporary test class; keep test data in src/test/java/testdata if needed.
- Use runIde to manually test plugin behavior in a sandboxed IDE.

## 6) Coding conventions & structure

- Kotlin first: idiomatic Kotlin (null‑safety, data classes, extensions where sensible).
- Keep evaluation logic pure/deterministic where possible; separate side effects from computation.
- Small, focused functions; avoid deep nesting—prefer early returns.
- Name tests clearly: <UnitUnderTest>_<Scenario>_<Expected>()
- Place shared test helpers in src/test/kotlin/.../TestUtilities.kt
- Document non‑obvious invariants and edge cases with KDoc or brief comments.

## 7) Testing guidance

- Use JUnit5 (runs on JUnit Platform). Mix Java/Kotlin tests as needed.
- Test both "happy path" and edge cases (nulls, empty sets/ranges, overflow, order of evaluation).
- Prefer property‑like tests for algebraic/constraint components when feasible.

## 8) Working with the IntelliJ Platform

- Plugin descriptor: src/main/resources/META-INF/plugin.xml
- Run sandbox: ./gradlew runIde (autoReload enabled)
- If the sandbox gets stale: delete build/idea-sandbox or run clean; a prepareSandbox step prunes old libs pre‑run.

## 9) Contribution practices

- Keep changes focused; include/update tests with behavior changes.
- Run: build, test, (optionally) koverXmlReport and verifyPlugin before PRs.
- Commit messages: short imperative summary + brief body if needed.

## 10) Quick troubleshooting

- Build errors about language level: ensure JDK 17 is selected.
- Tests not discovered: confirm useJUnitPlatform() and correct --tests pattern.
- Plugin not loading: check plugin.xml changes and runIde logs (idea.log in sandbox).

That’s it—keep it small, fast, and well‑tested.
