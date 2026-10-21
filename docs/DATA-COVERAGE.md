# Data and channel coverage

Verified on 21 October 2026.

| Feature | Connected source | Coverage and limits |
| --- | --- | --- |
| Football fixtures and scores | OpenLigaDB `getmatchdata/bl1` | Bundesliga current matchday. Community updates may lag. Future matches have no invented score. |
| Additional scoreboards | ESPN public website JSON | Champions League, Premier League, LaLiga, Ligue 1, Serie A, NBA, NFL. Current provider date; endpoints have no guaranteed integration contract. Live status and clocks are displayed only when supplied by the provider. |
| Live tennis | ESPN ATP / WTA nested tournament competitions | Player names, country flags, set scores and live set status. Live ATP data and automatic refresh verified in the signed release on a fresh emulator. |
| Team crests | ESPN logos / OpenLigaDB teamIconUrl | PNG/JPEG and SVG rendering, with initials on download failure. No crests assigned to fictional demo teams. |
| Race classification | OpenF1 latest session + drivers + session_result | Latest available Formula 1 results, including position, gaps, laps and retirements. This is not live timing. |
| Included live channel | Red Bull TV publisher HLS | Video rendered inside SportsWave in the emulator. Availability may vary by region. |
| beIN / other Yacine providers | Direct catalog and channel API integration | beIN SPORTS 1 video rendered in the signed release on a fresh emulator without a companion app. Modes 1/3 supported; other channels can fail independently (HTTP 503/404/403 observed). Resolved URLs remain in memory. |
| Full channel catalog | Recursive Yacine category traversal | 675 records across 32 leaf packages observed, including nested Arabic packages. Sports appear first; duplicate qualities share a row and remain available in the player. This is catalog coverage, not 675 verified video feeds. |
| Alkass | Official Shoof public live metadata | Alkass 1, 2, 3 and 4 each rendered video inside the signed v1.0.4 APK. Fresh public HLS URLs replace failing Yacine delivery URLs. Alkass 5–8 were marked locked/premium; access is not bypassed. |
| Event-to-channel routing | Explicit imported broadcast references | Public fixtures do not supply TV broadcast assignments; no channel is guessed from sport or team. |

The provider calendar is now the default. The fictional calendar remains explicitly selectable for UI/player demonstrations. Scores refresh every 30 seconds while the Live screen is visible. F1 public results are distinguished from live race order; the latter requires a separate connected feed. No provider account secrets or captured signed URLs are stored in this repository.
