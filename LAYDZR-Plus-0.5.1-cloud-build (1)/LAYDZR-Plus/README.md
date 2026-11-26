# LAYDZR+ — Minecraft Java 26.2 / Fabric

## Implemented in this milestone
- Client-side Fabric entrypoint.
- Press **L** to open a custom LAYDZR+ tracker screen.
- Receives server game/system messages using Fabric's `ClientReceiveMessageEvents.GAME`.
- Recognizes only the two payment text shapes implemented in `PaymentMessageParser`.
- Session incoming, outgoing, net cash flow, current/worst outgoing streak, and recent payments.
- Loss-streak notification at the configured threshold (default 5).
- Local CSV portfolio ledger remains available for the later auction/portfolio modules.

## Build setup
Minecraft 26.2 uses Java 25, Fabric Loader 0.19.3, Loom 1.17, Gradle 9.5.1, and Fabric API 0.161.0+26.2. This version uses the non-remapping Loom plugin and does not declare a mappings dependency.

1. Install JDK 25 and Gradle 9.5.1.
2. Open this folder in IntelliJ IDEA.
3. Run `gradle build`.
4. Check `build/libs/laydzr-plus-0.2.0.jar`.

## Important testing notes
This project has not been compiled or tested inside a live Minecraft 26.2 client in this environment. Minecraft/Fabric APIs can change; resolve any compile errors against your exact Fabric installation before using it.
The payment parser expects exact server message formats. If DonutSMP uses a different message format or styled text, send a screenshot of a real payment notification and the parser can be adapted. The mod does not send `/pay`, place bets, or automate server transactions.

## Next modules
- Persisted session history and configurable controls.
- Draggable HUD overlay and toast notifications.
- Item registry catalog and auction/manual transaction capture.
- Profit/loss dashboard for item flips.
- Spotify now-playing integration using user-authorized Spotify access.
- Donut SMP market watchlist with dated, sourced price observations and risk-aware signals.


## Visual theme milestone
- Charcoal / graphite surfaces with thin grey outlines and subtle brighter top edges.
- Restrained LAYDZR silver branding.
- Green incoming / positive values and muted red outgoing / negative values.
- Three compact metric cards, streak strip, and framed recent-payment panel.
- Shared `LaydzrTheme` palette for upcoming auction, Spotify, settings, and portfolio screens.


## Reference-inspired blue HUD
- Compact top-right player/status card with LAYDZR label and player name.
- Session net, incoming/outgoing tracked payment totals, and playtime.
- Separate bottom ribbon labeled TOTAL PROFIT*. Until auction trades are connected, this value is tracked payment net cashflow, not verified item-flip profit.
- Deep graphite background, electric-blue borders and blue highlights throughout.
- Positive/negative cashflow use subtle cyan/rose text for readability; all panel frames and accents stay blue.
- This first HUD is based on locally tracked payment cashflow. It does not yet read the server's actual wallet balance, auction profit, kill/death totals, or external market data. The asterisk is intentional: it avoids presenting cashflow as confirmed trading profit.

## Music foundation milestone (0.3.0)
- Added persistent `music.properties` preferences.
- Lil Uzi Vert, Juice WRLD, and Chief Keef are the fixed top-three headliners and remain first in the featured list.
- Additional artists can be stored separately without displacing the headliners.
- Added a normalized now-playing data model for track, artist, album, artwork URL, playback state, and progress.
- This is the music data foundation only: Spotify OAuth, live playback metadata, artwork rendering, and transport controls are not yet connected.
- Spotify Free does not guarantee API playback-control access; the eventual integration will distinguish metadata display from controls and will never request a Spotify password.

## Spotify live now-playing milestone (0.4.0)
- Press **O** in Minecraft to open Spotify authorization in the system browser.
- Uses Spotify OAuth Authorization Code with PKCE: no client secret and no Spotify password are requested by LAYDZR+.
- Polls the Spotify currently-playing endpoint and displays track + artist in the compact blue HUD and tracker screen.
- Requests currently-playing / playback-state read scopes plus playback-control scope for future transport buttons.
- The three featured artists remain Lil Uzi Vert, Juice WRLD, and Chief Keef; other artists are still supported.

### One-time Spotify setup
1. Create an app in the Spotify Developer Dashboard.
2. In that app's Redirect URIs, add exactly `http://127.0.0.1:8888/callback` and save.
3. Copy the app's Client ID into `config/laydzr_plus/music.properties` as `spotify.client_id=YOUR_CLIENT_ID`.
4. Restart Minecraft and press **O**. Sign in on Spotify's own website and approve access.

LAYDZR+ stores the OAuth refresh token locally in `config/laydzr_plus/spotify-token.properties`. Keep that file private; deleting it disconnects the saved session. The Spotify Client ID is not a password. Playback metadata depends on Spotify API/account availability. Playback controls are not exposed in this milestone and Spotify may restrict control features for Free accounts; the mod does not bypass those restrictions. The redirect listener uses loopback on port 8888, so that port must be available.

## Build / verification status
This source update has not been compiled against the Minecraft 26.2 / Fabric 0.161.0 toolchain in this environment. Treat it as development source, not a verified release JAR.

## Auction tracker milestone (0.5.0 development source)
The blue LAYDZR auction card is now registered alongside the existing status HUD. It shows the tracked item and quantity, top bidder, bid count, estimated item worth, estimated/last flip P&L, all-time realized flip P&L, and LAYDZR branding.

The tracker is local/manual because the project does not contain a verified DonutSMP auction-chat protocol. Use these client commands while playing:
- `/laydzr auction start <quantity> <buyTotal> <item name>` — begin tracking an item (example: `/laydzr auction start 4 68000000 Netherite Ingot`).
- `/laydzr auction bid <amount> <player>` — update the current top bidder and increment the bid count.
- `/laydzr auction worth <amount>` — set the estimated total resale value; the HUD calculates estimated P&L as worth minus buy total.
- `/laydzr auction sold <sellTotal>` — close the flip and save realized P&L in `config/laydzr_plus/portfolio.csv`.
- `/laydzr auction clear` — clear the current card without recording a sale.

Amounts are total amounts for the whole stack, not per-item prices. A completed trade is persisted locally. The mod does not inspect private server auction data, place bids, purchase items, or automate transactions. The current bidder and estimated worth are entered by the player.

## Build verification
This source archive was inspected and extended, but it has not been compiled or tested in a live Minecraft client here. The available environment has Java 21 and no Gradle installation; this project targets Java 25 and Gradle 9.5.1. Install JDK 25 and Gradle 9.5.1, then run `gradle build` from this folder. The resulting JAR should be in `build/libs/` if the build succeeds.


## 0.5.1 source hardening
- Auction start now rejects blank item names, invalid quantities, and negative purchase totals.
- Bid updates now require a strictly higher bid and a non-empty bidder name.
- Estimated worth can only be set while an auction is active and cannot be negative.
- Sale totals are validated before a flip is written to the local ledger.
- Loss-streak alerts are edge-triggered rather than repeating every client tick while the threshold remains reached.

## Verification status for this archive
These source changes have been reviewed and packaged, but a Minecraft 26.2 build has **not** been run. This runtime has Java 21 and no Gradle executable, while the project requests Java 25 and Gradle 9.5.1. Do not treat this ZIP as a tested release JAR.

## Cloud build (Java 25 + Gradle 9.5.1)

This project includes a GitHub Actions workflow at `.github/workflows/build.yml`.
To build without installing Java or Gradle on your phone/PC:

1. Create a GitHub repository and upload the contents of this `LAYDZR-Plus` folder (the files inside it, not the outer folder).
2. Open the repository's **Actions** tab and enable Actions if GitHub asks.
3. Select **Build LAYDZR+ Fabric mod** and press **Run workflow**. A push to `main` or `master` also starts a build automatically.
4. When the run finishes, open it and download the `LAYDZR-Plus-Fabric-1.0` artifact. It contains the compiled JAR if the build succeeds.

A successful cloud build is not a substitute for testing the JAR in the exact Minecraft/Fabric version and on the target server. If the build fails, open the failed run and share its log so the source can be corrected.
