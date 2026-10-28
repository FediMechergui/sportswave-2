# Other-provider investigation — 28 October 2026

The application fixes were committed and pushed as `a340b82`; signed [v1.0.13](https://github.com/FediMechergui/sportswave-2/releases/tag/v1.0.13) and its checksum were published. VIPRow research was subsequently pushed as `f197843`. This report broadens the investigation beyond VIPRow. It does not claim these additional providers are integrated into that APK.

Reference: [SportsWave-Android](https://github.com/FediMechergui/SportsWave-Android), current main commit `2a9ea16b4bf98771bb59a1663cd11c2990d41ca9`. Checked its catalogue implementations, seven player rules, published source directory, and newly discovered public API documentation.

## Directory screening

Made ordinary bounded GET requests to **all 93 directory entries**, with at most six workers and 1.5 MB per response. Results: 66 HTTP 200, 14 HTTP 403, four HTTP 451, five local URL/network errors, one timeout, one HTTP 406, one HTTP 410 and one HTTP 307. No access-denial workaround was attempted.

These are page checks, not 66 working providers. Some successful pages are closure notices or directories rather than video. See [all directory results](PROVIDER-DIRECTORY-2026-10-28.csv).

## Catalogue and sports-data adapters

| Source | Observed result | Suitable use and limits |
| --- | --- | --- |
| Livekora | 16 raw match cards; 15 published channel-page links | Arabic discovery metadata. One card named beIN MENA 6 but linked a `/bein4/` page. Neither the card text nor URL slug establishes the actual feed identity. |
| Yalla Shoot Koora | 29 kickoff-bearing cards | Arabic fixtures with explicit UTC starts. Three sampled match pages returned HTTP 200 but no static iframe or compatible published player link. Do not interpret CSS live labels or displayed scores as independently verified score data. |
| FilGoal | 58 raw match cards; returned date explicitly 2026-10-28 | Arabic football fixtures/results. Cards still need the adapter's per-row validation and sport filtering; the raw count is not a validated fixture count. Match pages are not assumed to be video players. |
| Raceweek | Root and F1 series page available; 23 F1 SportsEvent meeting intervals | Meeting calendars, not individual practice/qualifying sessions, live timing, or streaming sources. |
| ESPN | 156 football events, 12 NBA events, four NHL events in selected-date responses | Public score/fixture metadata. Other routes in the reference cover NFL, tennis, golf, motorsport, rugby and more; not all routes were retested here. |
| ESPN F1 | 25 season meetings containing 125 competitions | Per-session start times and FP1/FP2/FP3/Qual/Race abbreviations. The reference adapter already traverses competitions and preserves session identities. Regional broadcast metadata must not be assumed to describe beIN MENA coverage. |
| OpenLigaDB | Nine Bundesliga and seven DEL records | Community football/hockey matchday data. Current-matchday endpoints need selected-date filtering; these counts do not mean all records occur today. |
| KorahLive | DNS lookup failed locally | Its adapter cannot populate from the configured host in this test. No substitute hostname was guessed. |
| Streamed | Both configured API hosts returned HTML closure pages instead of JSON | The [current homepage](https://streamed.pk/) states the service is no longer operating. This is a service closure, not a JSON-parser fix or evidence that old embed URLs remain available. |

See [endpoint results](PROVIDER-ADAPTERS-2026-10-28.csv). Data sources and video providers must remain separate in the app.

## Player families and direct samples

Opened seven public player entries with ordinary browser navigation and sampled video state before/after a five-second interval. Six produced decoded frames; one did not. Some first samples were still buffering before the later frame, so this is not a long-term stability benchmark. A decoded frame is not sufficient to verify event identity or current programming.

| Family / entry | Observation | Decision |
| --- | --- | --- |
| DaddyLive → Dembed: beIN Sports 9 Arabic, published ID 99 | Video rendered with a visible beIN SPORTS 9 watermark. Initial media requests included a 403, followed by normal player recovery and video. | New channel candidate, not yet a verified current-event replacement. The official guide listed Cairo equestrian programming during the check, while the captured feed showed football. Do not auto-route the scheduled event to this candidate. |
| DaddyLive → Dembed: XTRA 1, published ID 100 | Video rendered with a visible beIN SPORTS XTRA watermark and equestrian footage. | XTRA-family candidate. The watermark did not independently establish numbered XTRA 1 or Arabic commentary; audio was muted. No claim of full Arabic XTRA coverage. |
| DaddyLive → Dembed: MAX AR, published ID 597 | Page HTTP 200, repeated media-edge HTTP 404, no frame | Rejected as a working Arabic MAX replacement. French MAX and US feeds were not substituted. |
| SNRT Arryadia, direct official embed | Advancing 854-pixel-wide video, matching Arryadia/3 HD watermark, football scoreboard | Working independent broadcaster-player candidate. |
| AlbaPlayer Arryadia wrapper | Same SNRT nested player and advancing video | Works in the browser, but is not a second independent source of coverage. Prefer the direct broadcaster path. |
| AlbaPlayer `/bein4/` | Advancing football video; KORA TV branding, Golgohar/Arkadag scoreboard | Video works, but this is not verified as beIN SPORTS 4 or the fixture referenced by another card. Do not map it by its URL name alone. |
| StreamCenter-advertised embed | Advancing 1920-pixel-wide video through `streamec.st` / `embedcenter.st`; captured Sky Sports station ident | Playback-capable candidate; the captured ident does not establish that the advertised Manchester City/Brighton fixture was airing. |

The ordinary published DaddyLive channel directory exposed 37 beIN-labelled entries, including Arabic 1–9, XTRA, MAX AR, and separate English/French/Turkish/Australian services. Only the three priority entries above were tested here. Listing a channel is not a playback guarantee.

A second, longer sample did **not** reproduce beIN 9 playback: the media edge returned 403/404/502 responses and the video never decoded. This candidate is intermittent and must not be promoted as a reliable working replacement. The XTRA-labelled entry played again, advancing from 33.96 to 49.92 seconds over the later observation interval. Its numbered identity and commentary-language limits still apply. See [player samples and follow-up](OTHER-PROVIDERS-PLAYBACK-2026-10-28.csv).

YasirTV (`yasirtv.com`) also failed DNS resolution locally. That configured spelling must not be silently replaced with a similarly named website. The reference's old Streamed embed rule was not tested with invented IDs after its catalogue service closed.

TodayStreams, DaddyLive and Dembed often form one nested delivery chain, not three independent backups. The earlier [VIPRow investigation](VIPROW-INVESTIGATION-2026-10-28.md) reproduced required-host omissions in that chain and verified a minimal browser compatibility change. Its additional nested providers and loading-overlay failures remain relevant.

## Additional API candidate: StreamCenter

[StreamCenter documents an anonymous public catalogue and stream API](https://streamcenter.st/docs). Ordinary requests returned four sports and 22 match records: 12 basketball, seven soccer, two American football and one baseball. Resolved one advertised source group for each of three returned matches; each returned one embed URL. One embed was playback-sampled above. No arbitrary source IDs were enumerated.

This is not established as a successor to, affiliate of, or interchangeable mirror for the closed Streamed service. Its categories differ:

| API ID | Actual sport |
| --- | --- |
| `soccer` | Association football |
| `football` | American football |
| `basketball` | Basketball |
| `baseball` | Baseball |

Blindly replacing the Streamed hostname would mix association football with NFL records. Follow the returned catalogue's source references, explicit sport labels and documented request limits. Embed responses are web players, not native HLS URLs, and their programme identity still needs verification.

## Integration priorities

1. Retain the already tested provider recovery and channel-identity guards in SportsWave 2.
2. Use ESPN/FilGoal/OpenLigaDB for fixtures/results and Raceweek for meeting calendars. Keep scores, event status and channel availability independent.
3. Add a deliberate web-player integration for reviewed public provider pages; the current native Add Source field cannot play arbitrary HTML URLs.
4. Prefer direct broadcaster embeds such as SNRT over duplicate wrappers. Treat beIN 9/XTRA discoveries as candidates until sustained playback, identity, commentary and current-event checks pass.
5. Preserve provider choices per exact event and sport. Never hide upstream 404, unavailable language, programme mismatches or loading overlays behind an unrelated working feed.

No application code or playback sessions were changed by this research, and no temporary signed stream URLs are included in these reports. Browser findings do not certify Android WebView compatibility. The research emulator was no longer connected during this broader pass; no new native-player verification is claimed for these candidates.
