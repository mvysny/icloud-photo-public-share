# API and build cleanup

Improvements found in a review after the switch to plain OkHttp + MockWebServer. Pre-1.0, so
breaking the API is fine; note each break in README's compatibility list. Numbers are the review's;
1, 2, 7, 8 and 10 are done; 12 (publish from any JDK) is rejected, why beside the check in
`build.gradle.kts`.

## API

3. **Batch `webasseturls`.** The endpoint takes a `photoGuids` *array*, but we send one GUID per
   request: 86 photos = 86 requests. Add `getWebAssets(photos)`, chunked if iCloud caps it.
   - `Q_batch_size`: does iCloud accept many GUIDs per request, and is there a cap? Needs one live
     check, then a recorded response.
4. **Return `URI` or `String`, not `java.net.URL`.** `URL.equals`/`hashCode` resolve the host via
   DNS — a trap for anyone putting the URLs into a set or a map key.
5. **Size helpers.** `derivatives.values.maxByOrNull { it.height }!!` is the common case; offer
   `ICloudPhoto.largestDerivative` / `derivativeAtLeast(px)`.
6. **Videos.** Shared albums can hold videos; from memory (unverified) they carry `mediaAssetType`
   and non-numeric derivative keys like `PosterFrame`, `720p`, which would contradict the
   "keyed by the longer side in pixels" doc on `ICloudPhoto.derivatives`.
   - `Q_video_shape`: record a webstream + webasseturls response from an album with a video,
     then decide how the model exposes it.
   - Snake-case `WebItem.url_*` names leak iCloud's JSON into the public API; now that Gson maps
     only the internal `*Json` classes, renaming them is free — do it with this reshaping.

## Robustness

9. **Build request bodies with Gson**, not string templates — a `photoGuid` with a quote breaks
   the JSON.

## Build and project

11. **Verify the Android 5.0 promise mechanically.** JDK 8's API is not Android 21's; nothing
    checks it. The animal-sniffer Gradle plugin with the `android-api-level-21` signature would.
13. **CI JDKs.** CI tests on EOL JDK 16 — use 8, 11, 17, 21, 25. (OkHttp 5 is deferred, why beside
    the dependency in `build.gradle.kts`.)
14. **Stale AGENTS.md line.** The `mise` command line blames Kotlin 2.0.20 for failing under JDK 25;
    the build is on Kotlin 2.4.21 now. Re-check, then fix or delete the line.
