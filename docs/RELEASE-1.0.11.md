# SportsWave 1.0.11 — sources, interface and calendar

Investigation and device checks: 23 October 2026. **Every catalog record was delivery-tested; every channel is not working.** Provider failures, unsupported playback modes and wrong upstream labels remain unresolved in part.

## Source investigation

The [FMHY Live TV / Sports section](https://fmhy.net/video#live-tv-sports), cross-checked against its [maintained source](https://github.com/fmhy/FMHY/wiki/Streaming), supplied 182 external links. Screening covered all 126 streaming/index URLs; the remaining links were references such as status pages, communities and mirrors. This is homepage/index screening, not decoder verification of every stream on every site.

Results: 106 HTTP 200, twelve 403, four 451, one 503, one 202 and two network failures. [Per-URL results](FMHY-SITE-SCREENING-2026-10-23.csv) record these individually. DaddyLive listed beIN 9 Arabic, XTRA 1 and MAX Arabic. Their normal embed chain exposed media candidates, but all three returned HTTP 403 with both the basic and exact page-referrer requests. None was bundled as a working replacement. French MAX and US XTRA were not substituted for Arabic feeds.

A fresh provider catalog contained the same 675 IDs across 32 packages as the captured catalog. All 675 endpoints were checked again: 218 delivered media, 436 failed/used unsupported modes, twenty supplied no streams, and one API request timed out. [Complete delivery results](CHANNEL-AUDIT-2026-10-23.csv) distinguish these outcomes. Media delivery is not decoded-video or channel-identity verification. Across the original endpoints and tested alternatives, 266 catalog IDs had at least one passing transport or decoder observation; this includes ambiguous/mislabeled beIN records and therefore is not a count of 266 correctly identified working channels.

## Incorporated replacements

Exact-name public-directory candidates were also checked against failing catalog records. Seven replacements rendered video and had their captured branding reviewed before integration:

- Ajman TV
- Alaraby TV
- Alghad TV
- Al Masar TV
- Alyaum TV
- Maan TV
- MBC Drama +

Discovery: [IPTV-org Arabic directory](https://iptv-org.github.io/iptv/languages/ara.m3u). These are catalog-ID replacements scoped to the original service, not cross-channel substitutions. Both MBC candidates decoded, but one displayed ordinary MBC Drama branding. Only the candidate showing the Plus watermark was incorporated. A Mosaique candidate timed out and was excluded. The purported Lana TV + candidate reused the ordinary Lana TV URL and showed no Plus identity, so it was also excluded. Ajman alternated TV/radio programme branding; an earlier frame showed the Ajman TV mark. These snapshots do not establish uninterrupted uptime or distribution rights.

The seven new replacements passed again through the signed release APK's actual resolver, rather than only as direct test URLs. URLs have no bundled session signatures. With the six sports feeds from v1.0.10, there are now forty-nine bundled exact-ID public alternatives. Forty-seven rendered frames in this investigation; Tunisia 1 and Amozhgary TV returned HTTP 404 on the bundled URL. The four official Alkass feeds and the separate US XTRA/Red Bull sources also rendered frames.

## beIN identity check

All eighty beIN Sports quality records were decoder-tested: 36 rendered frames, 24 returned playback HTTP 404 and twenty failed resolution. A second pass captured all 36 successful frames for visual review:

- 31 numbered Sports 1–8 records showed the matching number watermark.
- Two XTRA 2 records showed **beIN SPORTS 7**, so they do not establish XTRA playback.
- Three generically named `beIN SPORTS` records also showed Sports 7; the generic title remains ambiguous.

[Individual results and observed branding](BEIN-IDENTITY-2026-10-23.csv) preserve each entry. These are four quality packages, not eighty distinct Arabic channels. Some footage/graphics referenced older competitions, so a live timeline does not establish current live-event programming. No verified Arabic beIN 9, XTRA or MAX replacement was added. Existing provider mappings were not renamed based on assumptions about future upstream content.

## Interface and recovery

Channel rows are more compact, retain logos and have touch feedback. Search includes bundled channel aliases and omits unrelated empty collection/test sections. The catalog count says listings to distinguish quality records from unique channels. Playback history records the actual first rendered frame or final failure; listings show “Last playback worked”, “Last attempt failed” or an unchecked state. Results expire after six hours and are not presented as uptime guarantees. Unchecked provider entries no longer advertise successful playback merely because they exist in the catalog.

If a bundled public alternative fails, the player can try the original provider once for that same catalog ID. Resolution failures can also advance to an unused quality of that same channel. Existing bounded session retries remain in place. These mechanisms cannot supply a missing upstream broadcast or correct its video content.

## Calendar and data

Bundesliga requests previously ignored the chosen date and fetched the current matchday. They now fetch the selected season and filter by local kickoff date. Arabic beIN/Alkass names and Arabic digits normalize for exact channel matching while preserving XTRA/MAX and channel-number distinctions.

The final Android data checks returned 45 real broadcast listings with no guide/calendar errors, 108 football fixtures, twelve NBA fixtures and 162 ATP tennis entries. Bundesliga returned one fixture on 23 October and six on 24 October, with the requested local-date assertion passing. OpenF1 supplied 22 positions for its latest returned completed race, Singapore on 11 October; this is race-result data, not a claim of a currently running live race.

Calendar routing uses published broadcast references and the airing interval. Availability of a calendar listing does not establish that the upstream channel is showing the advertised event. The app retains this distinction in event playback.

## Validation and artifact

Signed release/debug/test APK builds passed with the installed Gradle 8.14.3 and JDK 17. The user's wrapper setting was preserved. All 27 JVM tests and three Python audit regression tests passed. Android catalog/import, playback-history scoping and real data checks passed. UI checks covered upcoming/ended gating, airing-event sample playback for football, basketball, tennis, motorsport, cycling and cricket, and two fullscreen/Back cycles retaining the player. Emulator-only routing fixtures were restored afterward. These sample-routing checks do not establish real match-video coverage.

The signed APK is provided locally as `artifacts/SportsWave-1.0.11.apk`, SHA-256 `065b34a71eae0a4902dae52ba345d96fc02ab024434588ba8d2854288d2e053f`. Private manifests, player configuration, signed URLs and raw research captures remain in ignored storage.
