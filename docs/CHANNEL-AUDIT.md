# Complete channel delivery audit

Audit performed 21 October 2026 from this workstation's network. The [CSV](CHANNEL-AUDIT.csv) records all 675 catalog records individually, including package, channel ID, UTC check time, original delivery results and replacement evidence. No captured signed Yacine media URLs or authorization headers are published.

## Results

| Original catalog result | Records |
| --- | ---: |
| Delivered media bytes / HLS segment | 170 |
| Failed, unsupported or incomplete delivery | 485 |
| No stream records | 20 |
| Total checked | 675 |

The audit requests each channel endpoint and examines every returned stream record. Native direct streams are requested with the supplied user agent, referer and headers. HLS checks follow one rendition, request its encryption key if present, and download a small media segment sample. A successful master playlist alone does not pass. Unsupported web and DRM URLs also receive bounded HTTP reachability checks, without executing embedded scripts or requesting DRM licenses; those are recorded separately. DASH manifest-only responses, HTML pages, unsupported modes and DRM records do not count as verified media. These are delivery tests, not 675 decoded-video tests. Only one rendition/segment is sampled per HLS stream; geographic restrictions, transient errors, bitrate variants and future availability remain outside this snapshot.

There were 264 unsupported stream records, 18 DRM records and delivery errors including 403, 404, 451 and 503. These counts are stream records, not distinct channels; a channel may have multiple streams. An unsupported embedded website is not necessarily a dead broadcast.

## Replacements

Arabic searches and public country directories were used to find alternatives. Twenty country directories and 78 matching candidate entries were checked. Sixty-nine candidate entries served an HLS media segment. Thirty-six query-free alternatives corresponded to failing catalog records and were integrated by exact channel ID, scoped to the original service endpoint. Temporary signed URLs are not bundled. Coverage includes Tunisia 1/2, Medi1's three editions, France 24, Sharjah TV, several Saudi channels, and other news/regional broadcasters. The CSV links each replacement to its discovery directory; a directory label is evidence of the intended channel, not a broadcaster integration contract.

The existing dynamic official Shoof resolver covers Alkass 1–4, separately verified in the signed v1.0.4 emulator. Alkass 5–8 were marked locked/premium. Of the 505 original records that did not pass, 36 have new verified delivery alternatives and four have the existing Alkass resolver; **465 remain without a verified replacement in this audit**. Those catalog entries remain available for provider recovery; no unrelated channel is substituted.

Third-party checks included yacinetv.dev (its public API listed 31 channels using web embeds, without a verified same-channel replacement for the failing MENA beIN records), habbabihd.com (directory pages), and yacin.pro (HTTP failure). Old Dubai Sports/Racing Mangomolo candidates failed network resolution. SNRT's Arryadia player supplied a token-protected stream that did not pass the direct delivery check. None of those failed candidates is presented as a working replacement. Existing beIN feeds failed this pass despite earlier successful beIN 1 emulator playback; provider delivery changes over time.

## Android decoder verification

All 36 integrated alternatives decoded a video frame using Media3 in the signed v1.0.5 app process on the Android 15 emulator. The audit uses an independent video surface per channel, waits up to 25 seconds for the first rendered frame and releases the decoder before the next channel. Panorama FM, RT France and France 24 were also opened through the actual channel search and player UI. Fourteen JVM tests and the signed-release catalog/import instrumentation passed. These checks establish playback at the test time; they do not guarantee future uptime.

## Reproduce

Run `python scripts/audit_channels.py` to fetch the current complete catalog and produce an ignored `artifacts/channel-audit.jsonl` report. Four workers limit concurrent requests. Use `--catalog path/to/metadata.json` for a captured catalog and `--resume` only to resume an interrupted report. Default runs start a new report. API/package discovery failures abort rather than silently claiming complete coverage. Python's normal TLS certificate validation stays enabled.

Primary references: [SNRT Arryadia](https://snrtlive.ma/arryadia), [Dubai Sports live](https://www.dubaisports.ae/content/dubaisports/livestreaming/channel-1.html), [Alkass Shoof](https://shoof.alkass.net/), [Medi1 live](https://www.medi1tv.ma/ar/live.aspx). Discovery directory: [IPTV-org](https://github.com/iptv-org/iptv).
