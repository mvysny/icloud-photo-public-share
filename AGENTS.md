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

- **Every iCloud request goes through `ICloudClient`'s `httpClient`.** A request on any other `OkHttpClient` escapes `FakeICloud`'s rerouting, so the tests hit the real iCloud.
- **JVM target 1.8, plain `kotlin-stdlib`, no JDK 9+ API.** Anything newer compiles here and crashes on Android 5.0; nothing in this build checks it — PhotoCloud (`../photocloud`), which uses this library, is the verification.
- **Gson maps only the internal `*Json` classes (`ICloudJson.kt`), never the public model.** Their properties are `var`s with defaults; why is at the top of that file.

## Module map

- `src/main/kotlin` — the whole library, one package `com.gitlab.mvysny.icloudphotopublicshare`: client, public model, internal JSON mapping, OkHttp helpers. Entry point `ICloudClient.parseURL`.
- `src/test/kotlin` — JUnit 5 tests against `FakeICloud`, a MockWebServer replaying the recorded iCloud responses in `src/test/resources`.

## Conventions

- **Kotlin with `explicitApi()`.** Every public declaration says `public`; anything else is `internal` or `private`.
- **Tests never leave localhost.** Every `ICloudClient` in a test takes `fakeICloud.client`; a new iCloud response is recorded into `src/test/resources` and served by `FakeICloud`.
- **`mockwebserver` and `okhttp` share one version.** MockWebServer 4.x needs the matching OkHttp internals.
- **Errors are loud.** Non-2xx raises `IOException` (404 → `FileNotFoundException`) carrying code, body and URL.
- **Pre-1.0, `-SNAPSHOT` between releases.** Publish with JDK 11 or lower — the build refuses otherwise; steps in `RELEASING.md`.

## Commands

- `./gradlew` — `clean build`: compile, tests, then `design/verify_design_tripwires.sh` (skipped under CI). CI (`.github/workflows/build.yml`) runs it on JDK 8, 11, 17 and 21, plus the tripwire in a job of its own.
- `./gradlew test --tests '*ICloudClientTest'` — one test class.
- `mise exec -- ./gradlew` when the shell hasn't activated mise — `mise.toml` pins JetBrains 17; Gradle 8.14 runs on JDK 24 at most.

## Maintenance of this file

Loaded every turn; cap 34 KB, a module's own `AGENTS.md` 10 KB. Over it, in this order:
delete what has no home — status, history, class lists, what the code already says; trim
each line to its fact plus one clause and send the explanation home — why →
`design/decisions.md`, how across symbols → `design/architecture.md`, how in one symbol →
its doc comment, what upstream does → `design/research.md`; only then a module's own
`AGENTS.md`, peripheral modules first, never the core. Never paraphrase a lazy entry into a
line here. `design/verify_design_tripwires.sh` checks the caps and the cites.
