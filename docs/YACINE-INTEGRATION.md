# Yacine backend integration

The app now connects directly to the observed backend. It does not launch Yacine TV or YTV Player Pro to obtain playback URLs.

1. Load `categories`, then `categories/{id}/channels` for the selected package.
2. Cache channel IDs, names, logos, and the service address in app-private preferences. Do not cache resolved media URLs or stream-specific headers.
3. On every player opening or Retry, request `channel/{id}` and decode the response using its `t` header. The inspected transformation is Base64 followed by repeating XOR with the protocol prefix and timestamp.
4. Pass the returned native stream URL, user agent and request headers directly to Media3. Playback modes 1 and 3 are supported. Unsupported web/YouTube/token-generation modes and DRM records are rejected explicitly.
5. On an expired authorization response, unavailable redirect (HTTP 503), or a lost live window, attempt one automatic re-resolution. Further failures show Retry rather than looping or substituting an unrelated channel.

The service address defaults to the observed `http://def67.ycnapi.com/api/`. The original API uses unencrypted HTTP in the verified flow. The Channels screen has a Service address setting because this host may change. Changing it clears the old catalog cache. This does not weaken HTTPS certificate checks or change the separate generic resolver's HTTPS requirement.

Two backend calls may return the same still-current URL. Fetching the current record does not guarantee a distinct signature for every opening; URL issuance and delivery availability remain server-controlled.

Channel packages are provider data, not a bundled playlist. Calendar broadcast references can match cached Yacine channels by provider/channel ID or exact name. Public scoreboards still do not establish regional TV broadcast assignments.

Validation includes response-decoder fixtures, provider catalog round trips without media URLs, required header forwarding, unsupported-mode rejection, invalid-ID rejection, and existing routing/import tests. Runtime verification and delivery limitations are recorded in each release's notes. No full signed URLs are printed by these checks or committed to the repository.
