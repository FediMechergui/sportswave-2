# Additional sports feeds — 22 October 2026

SportsWave 1.0.10 adds six public Arabic sports feeds to **Channels → Included live channels**. They are bundled as channel metadata and HTTPS URLs, so opening this section does not require a successful Yacine catalog synchronization. Existing remote channel IDs also resolve to these alternatives, preserving saved selections and calendar references.

| Channel | Existing catalog ID | Delivery check |
| --- | --- | --- |
| Bahrain Sports 1 | 8093614024085928711 | HLS media segment received |
| Bahrain Sports 2 | 8093614024087628142 | HLS media segment received |
| Kuwait Sport | 8093614024090866910 | HLS media segment received |
| Kuwait Sport Plus (catalog: Kuwait Sport 2) | 8093614024091471159 | HLS media segment received |
| Jordan Sport | 8093614023844642330 | HLS media segment received |
| Oman Sport | 8093614023881577308 | HLS media segment received |

Discovery: [IPTV-org sports directory](https://iptv-org.github.io/iptv/categories/sports.m3u) and its Bahrain/Kuwait/Oman country lists. Channel naming references include [Kuwait Ministry of Information](https://media.gov.kw/Frequency.aspx?FreqType=TVKSC), [JRTV](https://www.jrtv.gov.jo/ar) and [Oman's Ministry of Information](https://omaninfo.om/ar/omantv/3). Public directory entries are discovery evidence; they do not establish a distribution agreement or guarantee future availability. Regional and event-specific restrictions may apply.

The six exact-ID replacements apply only to the original Yacine service endpoint. Media URLs contain no bundled session token. Old provider headers and license settings are cleared when selecting these direct alternatives. Custom provider catalogs retain their own mappings.

## Android playback verification

All six entries rendered a decoded video frame through SportsWave's real resolver and Media3 player on Android 15. Each reported `liveTimeline=true`; [per-channel results](ADDITIONAL-SPORTS-PLAYBACK.csv) record the initial checks using cached provider records, exercising the exact-ID replacement path. A second pass in the signed v1.0.10 release APK also decoded all six built-in direct entries with live timelines. The Channels UI exposed the included feeds, and Kuwait Sport 2 was additionally opened through the actual row and visually showed video with the sports-2 watermark. This is a playback snapshot, not a guarantee of future uptime or that an advertised event is on air.

Release/debug/test APK builds and all 24 JVM tests passed. The signed APK is available locally at `artifacts/SportsWave-1.0.10.apk`, SHA-256 `5c46e10371a61010e5ea88f00514e46031a739a80b380470bb1787b56b627e1c`. The user's Gradle wrapper setting was preserved; the successful build used the already installed Gradle 8.14.3 distribution with JDK 17 after a separate Gradle 9.3.1 cache failed.

## Remaining beIN priority coverage

The new Arabic-search lead for beIN 9 exposed an HLS proxy address, but both the normal app user agent and the page's browser/referrer combination returned HTTP 403. It was excluded. Three additional publicly listed MAX candidates failed network requests and were excluded. Listings for Turkish MAX or the existing English US XTRA feed were not substituted for Arabic channels.

A new transport recheck of the original backend's 16 entries for beIN 9 and XTRA 1–3 across four qualities returned HTTP 404 at the channel API. This is the state at this recheck; the earlier original-app comparison still records 47 beIN entries rendering video in its separate test window. Results can change between observations.

No new working Arabic beIN 9, XTRA or MAX replacement is claimed by this update. Original integrations and same-channel recovery remain available.
