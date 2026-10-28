# beIN provider recovery — 28 October 2026

This supersedes the earlier same-day backend-unavailable finding in `RECHECK-2026-10-28.md`.

## Causes established

1. SportsWave's default API host `def67.ycnapi.com` no longer resolved locally. Freshly launching the supplied original Yacine APK on emulator-5560 downloaded remote configuration identifying `def001.ycnapi.com` as its current default host. The new host returned 32 packages and 675 channel records using the existing response format. No authentication challenge was encountered for this catalogue; the observed DNS failure was before stream authorization or decoding.
2. Channel IDs changed with the catalogue. Verified 142 old-to-new mappings using unique, exact full channel names and package names, covering beIN and bundled public alternatives. Ambiguous catalogue pairs were excluded. Old saved channel references and requests must follow these mappings, while independently configured provider endpoints remain unchanged.
3. XTRA 1 offers two native stream descriptors in each quality package. The first returned HTTP 404 and the second delivered conventional HLS segments. SportsWave previously selected only the first descriptor; refreshing that descriptor or changing quality could repeatedly miss a working alternative.

## Implemented in 1.0.13

- Updated default API host and migrated requests using the exact old default, including stored endpoint preferences.
- Retained old-to-new channel identities for request resolution, calendar references and public alternatives. Unknown IDs and custom service addresses are not guessed or remapped.
- Forced one catalogue refresh for pre-migration caches. Successful refresh replaces obsolete package/channel records through the existing cache mechanism.
- Player now tries subsequent supported descriptors for the same selected channel before session refresh and quality fallback. It fetches a fresh descriptor response for each attempt, rather than bundling temporary signed URLs. At most eight descriptors per record are considered.
- Explicit retry and broadcast changes reset stream selection. Stale player callbacks cannot trigger a new fallback after their player has been replaced.
- Added a guard for six verified media-path fingerprints shared under incompatible channel labels. XTRA 1's alternate points to SPORTS 8; XTRA 2 and generic SPORTS records share SPORTS 7 paths. Those known mismatches are rejected instead of presented under the requested label. The guard stores hashes, not signed playback URLs, and does not permanently reject a channel whose provider returns a different, repaired path.

## Transport audit

Tested all 93 records in the four beIN Sports packages and beIN Entertainment: 51 had a delivering native stream, 24 failed or required unsupported modes, and 18 returned no streams. Of these, the 80 sports records had 40 delivering records. These are quality-specific records, not 40 distinct sports channels.

- beIN 9: HTTP 404 in all four quality packages.
- A fresh 720P descriptor check followed ordinary redirects: SPORTS 1 reached a media edge with HTTP 200; SPORTS 9 reached a different edge with HTTP 404. Both `e` values, if interpreted as Unix expiry timestamps, were still in the future. This does not support expired local URLs or a login challenge as the cause of that particular SPORTS 9 failure. No signature values were modified.
- XTRA 1: first descriptor HTTP 404, second descriptor delivering HLS in all four qualities, but its media path is identical to SPORTS 8. Not accepted as XTRA 1.
- XTRA 2: delivering in 1080P and 720P, but its media path is identical to SPORTS 7. Not accepted as XTRA 2. Lower qualities returned 404.
- XTRA 3: HTTP 404 in all four qualities.
- No Arabic MAX entries exist in this returned catalogue.

Sanitized transport results: `BEIN-RECOVERY-TRANSPORT-2026-10-28.csv`. Media URLs, signatures and unrelated application configuration values are excluded.

## Native audit before identity guards

All 93 records were requested individually through the updated Android resolver: 48 decoded a frame using the first supported descriptor. All four XTRA 1 records decoded using their second descriptor. Of the resulting 52 delivering records, nine have a known conflicting label: four XTRA 1, two XTRA 2, and three generic SPORTS records. They must not be counted as correctly identified channels.

The remaining 43 delivering records comprise 31 numbered SPORTS 1–8 quality records and 12 entertainment records. SPORTS 8 at 1080P returned 404; its 720P/360P/244P records decoded. SPORTS 9 never decoded. These counts are availability at test time, not unique-channel counts or guarantees of current live programming. The native audit recovered one more entertainment record than the earlier transport audit.

See `BEIN-RECOVERY-NATIVE-2026-10-28.csv`, which preserves decoder results and identity warnings separately. Captured numbered sports samples include advertising and replays; their content must not automatically be treated as today's scheduled live match.

Calendar instrumentation returned 67 listings with no source errors after backend recovery. Data/import/favorites instrumentation passed.

## Final build verification

Signed 1.0.13 release/debug builds passed, along with 35 JVM tests and three Python HLS audit tests. Installed the release on emulator-5560. A focused native rerun after adding identity guards decoded all eight numbered SPORTS 1–8 records at 720P. Both XTRA 2 mismatches and all four XTRA 1 secondary mismatches were rejected during resolution. XTRA 1's primary descriptors still returned 404, as expected.

Final release UI checks passed: catalogue refreshed to 675 listings; SPORTS 1 and SPORTS 8 both displayed “Playing inside SportsWave”; XTRA 2 stopped with the explicit SPORTS 7 mismatch message. These checks used normal channel search and selection. Release APK: `artifacts/SportsWave-1.0.13.apk`, SHA-256 `6d6fc6af7ae62f4181b596a958febc8f4873d614ad3d2fe5dad74139bb4a9b56`.

## Limits

An advancing HLS timeline or decoded frame is not proof of the advertised channel number, current event, commentary language, or realtime broadcast. Captured samples include promotional transitions and generic beIN branding. No login, subscription, DRM, or provider denial was bypassed. The remaining upstream 404 and empty-stream responses cannot be repaired by changing the player's fullscreen or codec configuration.
