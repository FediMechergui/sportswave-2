# Yacine backend integration

The app now connects directly to the observed backend. It does not launch Yacine TV or YTV Player Pro to obtain playback URLs.

1. Load `categories`, recursively traverse `categories/{id}`, then load `categories/{id}/channels` for every leaf package. The 21 October audit returned 675 channel records across 32 leaf packages. Nested Arabic packages are included.
2. Cache channel IDs, names, logos, and the service address in app-private preferences. Do not cache resolved media URLs or stream-specific headers.
3. For channels without a verified public alternative, request `channel/{id}` on player opening or Retry and decode the response using its `t` header. The inspected transformation is Base64 followed by repeating XOR with the protocol prefix and timestamp.
4. Pass the returned native stream URL, user agent and request headers directly to Media3. Playback modes 1 and 3 are supported. Unsupported web/YouTube/token-generation modes and DRM records are rejected explicitly.
5. On an expired authorization response, unavailable redirect (HTTP 503), or a lost live window, attempt one automatic re-resolution. Delivery failures can then try an unused quality variant of the exact same provider/channel name. Attempts are bounded; unrelated channels are never substituted.

The default catalog shows all packages, with sports first, global search and incremental rows. Quality variants share a channel row and remain selectable inside the player. The package picker can restrict the list. Metadata is cached; Refresh synchronizes the complete catalog again.

Version 1.0.5 includes 36 verified public HTTPS alternatives for failing catalog records. `PublicChannelStreams` maps exact channel IDs only for the original service endpoint, clearing old media headers and license settings. These query-free delivery URLs are bundled; expiring Yacine sessions are not. Each alternative decoded a video frame in the signed app process. The complete audit and unresolved coverage are documented in `CHANNEL-AUDIT.md` and `CHANNEL-AUDIT.csv`.

Alkass uses the broadcaster's public `https://shoofapi.alkass.net/Shoof/liveV3.php` metadata to obtain a current HTTPS HLS URL when the record is unlocked and non-premium. Alkass 1–4 rendered video in the signed v1.0.4 APK on a fresh emulator without companion apps. Records 5–8 were locked/premium and are not unlocked by this integration. The ordinary Yacine resolver remains the fallback when no public broadcaster record is available. Signed URLs stay in memory. Catalog completeness does not imply every upstream stream is available; HTTP errors and geographic restrictions remain provider-controlled.

The service address defaults to the observed `http://def67.ycnapi.com/api/`. The original API uses unencrypted HTTP in the verified flow. The Channels screen has a Service address setting because this host may change. Changing it clears the old catalog cache. This does not weaken HTTPS certificate checks or change the separate generic resolver's HTTPS requirement.

Two backend calls may return the same still-current URL. Fetching the current record does not guarantee a distinct signature for every opening; URL issuance and delivery availability remain server-controlled.

Channel packages are provider data, not a bundled playlist. Calendar broadcast references can match cached Yacine channels by provider/channel ID or exact name. Public scoreboards still do not establish regional TV broadcast assignments.

Validation includes response-decoder fixtures, provider catalog round trips without media URLs, required header forwarding, unsupported-mode rejection, invalid-ID rejection, and existing routing/import tests. Runtime verification and delivery limitations are recorded in each release's notes. No full signed URLs are printed by these checks or committed to the repository.
