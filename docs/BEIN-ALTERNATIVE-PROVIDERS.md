# Alternative provider investigation — 22 October 2026

**No requested channel has passed a rendered-video test in this investigation.** Catalog entries and HTTP 200 manifests are not evidence of working live television. No candidate from this audit was added to the production SportsWave catalog.

Priority: beIN SPORTS 9, Arabic beIN SPORTS XTRA, Arabic beIN SPORTS MAX.

## Emulator results

| Source | Download actually tested | Observed result |
| --- | --- | --- |
| [Ostora](https://www.ostora.tv) | `com.t4w.ostora516`, 7.0 | App remained at its startup spinner. Its anonymous catalog API was independently reachable. Catalog supplied 32 priority candidate records across different servers and qualities; transport probes found no successful playable candidate. |
| [RedBox](https://redboxtvapk.com.co) | `com.reddish.redbox`, 2.10 | Site advertises a different version. Downloaded app opened after its emulator warning, but the catalog and beIN search were empty. |
| [Cricfy](https://www.cricfy.net/android-11-apk-395/) | `com.cricfytv.sports`, 7.3 | Events loaded, including items marked live. Sports channel category and beIN search were empty. An event count does not establish channel playback. |
| [Live NetTV](https://livenettv.mn) | Phone 5.0.1; TV 5.0.5 | Phone build rejected the emulator as a TV device. Publisher TV build installed but crashed before its catalog, with a provider class-loading error. TV APK SHA-256 matched the publisher's download page. |

Tests used Android 15 x86_64 on the existing research emulator. App warnings were acknowledged through their normal UI; device identity and app security checks were not patched. These results describe this test environment, rather than proving what happens on every physical phone.

## Priority channel findings

### beIN SPORTS 9

Ostora supplied server-1 H.265 FHD, HD, SD and LOW records, plus older alternate-server records. A fresh server-1 URL returned an HLS manifest. Following the media path failed: initial probes returned HTTP 403; a subsequent segment request returned an HTML-typed response instead of confirmed decodable media. The manifest's embedded key decoded to 48 bytes, which is not an ordinary 16-byte AES-128 key.

An explicit test in SportsWave's native player failed with `ERROR_CODE_IO_NETWORK_CONNECTION_FAILED`. No rendered frame or advancing live video was verified. The temporary source and private session file were removed from SportsWave afterward.

The source includes a `413<F>` prefix and is handed to Ostora's separate `info.t4w.vp` player with `url`, `player` and `agent` intent extras. Removing the prefix and treating the result as conventional HLS has not been validated. The publisher-linked companion APK was retrieved intact from [gabar99/new](https://github.com/gabar99/new), version 4.0 / code 413, and installed. Launch commands with fresh channel entries were accepted, but the captured screen showed Android's “System UI isn't responding” dialog rather than playback. This companion test is inconclusive; a successful rendered-video test remains necessary before claiming this path works.

### Arabic beIN SPORTS MAX

Inspected Ostora categories list MAX 1–4 and duplicate quality/backup records. Several old playback hosts no longer resolve: independent DNS checks returned NXDOMAIN for two MAX hostnames. Four source strings also contain unescaped URL spaces, so those four raw transport results are `InvalidURL`; they are not decoder tests. Correcting URL escaping alone would not repair a non-resolving hostname.

No MAX stream rendered successfully. Catalog labels have not independently established the current programme or audio language.

### Arabic beIN SPORTS XTRA

No explicitly labelled Arabic beIN XTRA record was found in the inspected Ostora categories. SSC EXTRA and CricLife MAX are different channels and were excluded. The other three apps did not yield a usable beIN channel catalog in the emulator. This is a coverage gap, not evidence that every possible provider lacks these channels.

## Candidate audit and limits

[Per-record transport results](BEIN-ALTERNATIVE-CANDIDATES.csv) contain 32 Ostora records: 23 `URLError`, four `InvalidURL`, one timeout and four HTTP 403 results. These are bounded manifest/key/segment probes, not 32 manual decoder tests. The source mode may require companion-player processing that the transport probe does not implement.

APK files, decoded private configuration, signed session URLs, emulator preferences and detailed runtime captures remain in ignored local investigation storage. The report and CSV deliberately omit session tokens and packaged configuration secrets. Signed URLs expire; a copied URL is not a renewal mechanism.

Emulator instability interrupted further original-player tests. The remaining useful test is fresh playback in the publisher's companion, followed by repeat playback after session expiry if it succeeds. Until that produces video, these entries should not be offered as verified working channels in SportsWave.
