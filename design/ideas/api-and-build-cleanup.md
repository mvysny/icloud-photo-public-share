# API and build cleanup

Improvements found in a review after the switch to plain OkHttp + MockWebServer. Pre-1.0, so
breaking the API is fine; note each break in README's compatibility list. Numbers are the review's;
1, 2, 7, 8, 9, 10, 13 and 14 are done; 11 (an Android API check in the build) and 12 (publish from
any JDK) are rejected, why in AGENTS.md and beside the check in `build.gradle.kts`. OkHttp 5 is
deferred, why beside the dependency; JDK 25 in CI likewise, why in `.github/workflows/build.yml`.

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
