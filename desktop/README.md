# SportsWave for Windows — preview 0.1.0

Portable Windows x64 application. This is a separate desktop implementation, not an Android APK wrapper. The self-contained executable includes .NET; Microsoft Edge WebView2 Runtime must be installed. Runtime installation: https://developer.microsoft.com/microsoft-edge/webview2/

Includes selected-date ESPN calendars and scores for football, NBA, NFL, NHL, MLB, ATP tennis and F1; live-screen refresh every 30 seconds; team images supplied by ESPN; saved events; six bundled public sports-channel URLs; a labeled HLS sample; HTTPS HLS/progressive playback with fullscreen; JSON catalog import. Live data availability depends on ESPN; coverage is not exhaustive. F1 classification and Android provider adapters have not been ported.

Import accepts an array of direct HTTPS sources or `{ "sources": [...], "events": [...] }`, compatible with `examples/catalog.json`. An imported event opens only the source identified by its exact `sourceId`. No channel is inferred from team names. Android resolver, custom-header, DRM and authenticated sources are rejected with an explicit explanation. DASH and external provider web-page playback are not included in this preview. Upstream failures and CORS restrictions can prevent a public source from playing.

Data is stored under `%LOCALAPPDATA%\SportsWave\Desktop`, separate from Android. Import replaces the imported catalog; bundled channels remain available. Saved event IDs persist, and saved events are shown when present in the selected day's loaded schedule.

Build with .NET SDK 10:

```powershell
dotnet publish desktop/SportsWave.csproj -c Release -r win-x64 --self-contained true -o artifacts/windows
```

The application embeds its UI and hls.js 1.6.13 (Apache-2.0, see `HLS-LICENSE`) inside the EXE. It uses WebView2 1.0.4314.35. The executable is currently unsigned.

Verified on Windows on 30 October 2026: packaged EXE launched; real ESPN requests returned 152 events without feed errors for that date; seven channel/sample rows loaded; the labeled HLS sample decoded 1280/1920-pixel video with playback advancing from 9.52 to 13.53 seconds; fullscreen entered and exited. Additional short native-player probes decoded advancing video on Bahrain Sports 1 and 2, Kuwait Sport, Jordan Sports and Oman Sport. Kuwait Sport 2 decoded a frame but did not advance during the two-second comparison; its live reliability is unverified. These are short observations, not long-running availability guarantees. Separate mocked-bridge UI checks passed schedule, live, saved, import and search without JavaScript errors.

UI regression check (run from repository root after installing the `desktop/package.json` development dependency): `node desktop/tests/ui.cjs`. This uses Playwright's installed Chromium. `desktop/tests/native.cjs` checks a running desktop app launched with the test-only environment variable `WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS=--remote-debugging-port=9227`. Normal application launches expose no debugging port. Test outputs go to ignored `artifacts/`.
