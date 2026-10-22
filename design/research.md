# Research — iCloud shared albums (`pNN-sharedstreams.icloud.com`)

What the things we don't own actually do. About *them*, never us: a sentence starting "we chose"
is a `D_`. `## R_<slug> — <title>`, one claim per bullet, one provenance marker per claim —
**[docs]**, **[src]**, **[verified <date>, <version>]**, **[unverified]** (a hypothesis; a design
built on it says so). A claim is earned by its provenance, or by having cost real work to find
out. Checked against the live service; Apple publishes no docs for it, so a claim names the date
it was seen on. Cite by slug, `R_<slug>`, never by position; `grep '^## R_' design/research.md` is
the index. The first entry is the ruler: every later one trims to its length — which is how long
this file gets, so keep it short. When you have written an entry, re-read it against the one
above and cut the fat — a marker already says where a claim came from.

---

## R_webasseturls_batch — `webasseturls` takes many photos per request

- One request with all 86 GUIDs of an 86-photo album returned every item; the array was accepted
  at 2,000 entries (GUIDs repeated). No cap found; more than 86 distinct GUIDs is untested.
  **[verified 2026-10-22]**
- An unknown GUID is left out of `items`: 200, not 404. With only unknown GUIDs, `locations` is
  empty too. **[verified 2026-10-22]**
- An empty `photoGuids` answers `400 Validation Failed: missing photoGuids`. **[verified 2026-10-22]**
- `items` is keyed by checksum, and a photo uploaded twice gets new GUIDs but the same checksums,
  so one item serves both; its `url_path` filename may be either upload's. **[verified 2026-10-22]**
