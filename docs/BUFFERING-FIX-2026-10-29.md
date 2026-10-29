# Native playback buffering fix - v1.0.16

The first current beIN SPORTS 1 / 720P descriptor failed with an Android network-connection error. Its host also failed from the development machine. The alternate descriptor delivered an HTTP 200 HLS playlist and a media segment on the development machine, redirecting from HTTP to HTTPS. Playlist availability alone was not treated as playback success.

The native player previously prohibited all cross-protocol redirects and only entered feed recovery after a playback error. A source that remained buffering without emitting an error had no application deadline.

Version 1.0.16 allows cross-protocol redirects only for sources already explicitly configured with allowHttp=true. HTTPS-only sources retain the existing restriction. A continuous 30-second buffering period while playback is requested now enters the existing bounded recovery flow: another descriptor, renewed session or another quality of the same channel; exhausted recovery displays an actionable retry. Pausing, leaving the activity and replacing the player cancel its buffering timer. No new authentication workaround or stored media session is introduced.

Validation: release/debug builds and 52 JVM tests passed; Android data instrumentation passed. An Android 15 emulator decoded a video frame from the alternate beIN SPORTS 1 / 720P descriptor with a live timeline after the redirect change. The first descriptor remains unavailable; this result does not establish permanent availability or every beIN channel.

A local Android HTTP server test kept an HLS segment request alive without supplying decodable media. The player exited buffering and displayed the network-timeout retry action; total launch-to-result time was 36 seconds, including activity startup. This verifies recovery from a stalled HTTP-200 response rather than only HTTP errors.
