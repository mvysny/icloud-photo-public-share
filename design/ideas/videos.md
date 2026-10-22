# Videos

**On hold:** PhotoCloud, this library's user, neither shows nor supports videos, so nobody needs
them yet; picking this up also needs a public shared album that holds a video.

Shared albums can hold videos; from memory (unverified) they carry `mediaAssetType` and
non-numeric derivative keys like `PosterFrame`, `720p`, which would contradict the "keyed by the
longer side in pixels" doc on `ICloudPhoto.derivatives`.

- `Q_video_shape`: record a webstream + webasseturls response from an album with a video, then
  decide how the model exposes it.
- Snake-case `WebItem.url_*` names leak iCloud's JSON into the public API; now that Gson maps only
  the internal `*Json` classes, renaming them is free — do it with this reshaping, as one API break.
