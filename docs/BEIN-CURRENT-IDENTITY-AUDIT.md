# beIN playback and channel identity recheck

Recheck: 22 October 2026. **No new Arabic beIN 9, XTRA or MAX replacement passed both playback and channel-identity verification.** No unverified replacement was incorporated under those names.

## Current SportsWave results

All 80 records in the captured beIN SPORTS 1080P, 720P, 360P and 244P packages were tested individually through the signed v1.0.10 APK's resolver and decoder harness. These are four quality variants of 20 catalog slots, including French/English channels and News, rather than 80 distinct Arabic channels. A separate workstation probe used the same `okhttp/4.12.0` API user agent as the app.

| Check | Results |
| --- | --- |
| Channel API delivery, 80 records | 80 HTTP 404 responses |
| Android playback, same 80 records | 80 resolution failures; zero rendered frames |
| Current title-to-video identity | Unverified for all 80: no current video to inspect |
| Existing beIN SPORTS XTRA (US) direct source | Rendered a frame with a live timeline; retained as US, excluded from Arabic XTRA coverage |

The US source was also opened from the actual Channels row. Its inspected frame showed English “COMMERCIAL BREAK / WE'LL BE BACK” graphics and beIN SPORTS XTRA branding, matching the explicitly US-labelled entry rather than establishing Arabic XTRA playback.

[Individual current and earlier identity observations](BEIN-CURRENT-IDENTITY-AUDIT.csv) preserve each record's ID, package, delivery result, Android result and earlier visual notes. The audit fixture contains channel metadata only and is read independently of the user's catalog; the test does not import 80 temporary sources or alter saved selections.

These failures describe this test window. The earlier 47 successful beIN decoder observations remain valid historical results, rather than a guarantee of current availability. The captured catalog was used because the channel endpoints were failing; this is not a claim that a current backend refresh supplied a new complete catalog.

## Earlier title mismatches

The earlier original-app visual audit documented these explicit XTRA mismatches:

| Selected record | Package | Visible branding in earlier observation |
| --- | --- | --- |
| XTRA 1, ID 8093614024250808790 | 1080P | beIN SPORTS 8 |
| XTRA 2, ID 8093614024251429345 | 1080P | beIN SPORTS 7 |
| XTRA 2, ID 8093614023576211642 | 720P | beIN SPORTS 7 |

Three other observations selected the generic `beIN SPORTS` label and showed Sports 7. Those labels are ambiguous. Neither those generic entries nor the three XTRA mismatches establish verified XTRA playback. No guessed rename or substitution was added: the upstream mapping may change, and current frames were unavailable to reassess it.

## Additional sources checked

- Ostora's fresh FHD/H.265 “9” entry did play cycling video in its publisher-linked companion after the normal ad was dismissed. Two inspected captures showed different Tour of Oman footage with **Ostora TV 9** branding. An unobscured beIN 9 identity was not established. Consequently this is not incorporated as verified beIN 9.
- That source's manifest used an embedded 48-byte key and media responses beginning with a PNG signature despite an HTML content type. This is a custom transport path; the manifest alone does not establish compatibility with SportsWave's native HLS path. The delivery audit now rejects nonstandard AES-128 key lengths and image wrappers as ordinary HLS evidence. Three regression checks pass.
- Eight fresh Ostora MAX 1–4 records across two packages failed network delivery, including after URL-space normalization. No MAX frame or Arabic channel identity was verified.
- [Fasterkoora's XTRA 1 page](https://www.fasterkoora.net/2022/04/bein-sport-extra-1.html) pointed to an unreachable player host. [Foottn's MAX 1 page](https://www.foottn.tn/2026/06/1-bein-sports-max-1-live.html) embedded a URL named `bein-sports-hd-1`, not an established MAX feed, and that host was unreachable. TVHub returned HTTP 403; Telfaz's beIN 9 playback endpoint failed. These site labels were not treated as proof of channel identity.

Signed URLs, full manifests, private player configuration and screenshots remain in ignored local research storage. The public report contains no session signatures. The six public Arabic sports feeds added in v1.0.10 remain unchanged.
