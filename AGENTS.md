# iCloud Photo Public Share

## What this is

A Kotlin library which allows you to download photos from a publicly shared
iCloud Photos (public shared folder).

## Promises

- **Public links only.** Works from a shared-album URL alone; never asks for an Apple ID or any credential.
- **Runs on Android 5.0+ and JDK 8+.** No API, bytecode or dependency that would break either.
- **Small and dependency-light.** Kotlin stdlib, OkHttp and Gson; nothing else on the runtime classpath.

## Design docs

| File | Owns | Loaded |
|---|---|---|
| `README.md` | the pitch, usage, the Maven coordinates, compatibility per version | — |
| `CONTRIBUTING.md` | how to contribute | — |
| `RELEASING.md` | the release procedure | — |
| `AGENTS.md` (this) | promises, invariants, the module map, conventions, commands | every turn |
| doc comments | what one symbol does and why it is shaped so | at the symbol |

Every fact lives in exactly one of these; the others link to it.

## Invariants

- **Every iCloud request goes through `ICloud.httpClient`.** A request on any other `OkHttpClient` escapes `FakeICloud`'s rerouting, so the tests hit the real iCloud.
- **JVM target 1.8, plain `kotlin-stdlib`, no JDK 9+ API.** Anything newer compiles here and crashes on Android 5.0.
- **Every property of a Gson-mapped class has a default value.** Without a no-arg constructor Gson allocates through `Unsafe`, so a missing JSON field leaves a non-null Kotlin property null.

## Module map

- `src/main/kotlin` — the whole library, one package `com.gitlab.mvysny.icloudphotopublicshare`: client, JSON model, OkHttp helpers. Entry point `ICloudClient.parseURL`; `Main.kt` is a demo against a live album.
- `src/test/kotlin` — JUnit 5 tests against `FakeICloud`, a MockWebServer replaying the recorded iCloud responses in `src/test/resources`.

## Conventions

- **Kotlin with `explicitApi()`.** Every public declaration says `public`; anything else is `internal` or `private`.
- **Tests never leave localhost.** A new iCloud response is recorded into `src/test/resources` and served by `FakeICloud`.
- **`mockwebserver` and `okhttp` share one version.** MockWebServer 4.x needs the matching OkHttp internals.
- **Errors are loud.** Non-2xx raises `IOException` (404 → `FileNotFoundException`) carrying code, body and URL.
- **Pre-1.0, `-SNAPSHOT` between releases.** Publish with JDK 11 or lower — the build refuses otherwise; steps in `RELEASING.md`.

## Commands

- `./gradlew` — `clean build`: compile, tests, then `design/verify_design_tripwires.sh` (skipped under CI). CI (`.gitlab-ci.yml`) runs it on JDK 8, 11, 16 and 17, plus the tripwire in a job of its own.
- `./gradlew test --tests '*ICloudClientTest'` — one test class.
- `mise exec -- ./gradlew` when the shell hasn't activated mise — `mise.toml` pins JetBrains 17; under JDK 25 Kotlin 2.0.20 fails to parse the Java version.

## Maintenance of this file

Loaded every turn; cap 34 KB, a module's own `AGENTS.md` 10 KB. Over it, in this order:
delete what has no home — status, history, class lists, what the code already says; trim
each line to its fact plus one clause and send the explanation home — why →
`design/decisions.md`, how across symbols → `design/architecture.md`, how in one symbol →
its doc comment, what upstream does → `design/research.md`; only then a module's own
`AGENTS.md`, peripheral modules first, never the core. Never paraphrase a lazy entry into a
line here. `design/verify_design_tripwires.sh` checks the caps and the cites.
