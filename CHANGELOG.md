# Changelog

## 1.7.0
- **Smoother interface.** The floating navigation bar now slides: the selected tab expands to show its label with a spring, others shrink to an icon. Switching tabs slides and fades in the direction you are moving. The status badge morphs between icons, changes colour smoothly, pops when the state changes and shows a ring while connecting. Cards enter one after another, panels animate when their content changes, buttons shrink slightly under your finger and give a light haptic tick, list items animate when added, removed or moved, and Back returns to the first tab before closing the app (with the Android 13+ back preview).
- No new libraries; the animation code is original and uses only Jetpack Compose.
- Includes the 1.6.1 battery fix.

## 1.6.1
- **Battery fix.** The CPU wake lock and the Wi-Fi lock were held for as long as the service ran, even when the phone was not on campus Wi-Fi (for example at home, or while waiting for the network). On one test phone that meant a wake lock for about 12 h 42 m in a day and roughly 10% of the battery. The locks are now held only while the app is connecting, signed in, or retrying on campus Wi-Fi, and released otherwise.

## 1.6.0
- New: a **Network events** counter on the Connection tab. It counts the Wi-Fi and VPN callbacks the app receives, to help find out whether network chatter keeps the CPU awake (battery tuning).
- New: the activity log notes which XML fields the portal's login and keepalive replies contain (names only, never values or credentials), once per kind. This shows whether the portal reports data usage, which a usage bar would need.

## 1.5.0
- **Security: releases are now signed with a private key.** Until now releases were signed with a debug key that is public in the repository, which meant anyone could build an APK that Android would accept as an update. The new key lives only in GitHub Secrets and offline backups, and the release workflow refuses to publish an APK signed with the debug key. The signing certificate's SHA-256 is printed in each release's notes (see `SECURITY.md`).
- **Because the signing key changed, Android will not update 1.4.x in place.** Uninstall the old version and install 1.5.0, then add your accounts again. Future updates install normally.
- Added `SECURITY.md`.

## 1.4.1
- Fix: with a VPN on, sign-in and keepalives could stop after a while with "Binding socket to network … failed: EPERM". Some VPN setups make Android refuse a direct connection to the Wi-Fi. UniLink now retries once over the default route (safe, because the portal certificate is pinned and checked before any password is sent), says clearly what is wrong if that fails too, retries every minute at most, and retries immediately when a VPN starts or stops.
- Polish: language chips, the activity log in a glass panel, a clearer Pause button.

## 1.4.0
- New: glass-style interface with a soft gradient backdrop, frosted cards, a floating navigation bar and an animated status badge on the Connection tab. It follows the phone's light or dark theme.
- New: small home-screen widget showing the status with a Connect / Pause button (plain RemoteViews, no extra library, so the APK stays small).
- The project is now free software under **GPL-3.0-or-later** (see `LICENSE`) and carries F-Droid / IzzyOnDroid metadata in `fastlane/`.
- Build: the dependency-info blob is no longer embedded in the APK (required by F-Droid), and CI uploads the R8 `mapping.txt` as an artifact.

## 1.3.0
- New: battery tools. The old single lock is now two switches in Settings: **Keep CPU awake** and **Keep Wi-Fi at full power**, so you can find the lightest setting that still keeps your session alive.
- New: the Connection tab shows keepalive health (successful, failed, and late wake-ups), and the Activity log notes when a keepalive ran late because the phone was asleep.
- Smaller and faster: release builds now use R8 code shrinking, unused resources are removed, only English and Persian translations are bundled, and baseline profiles are installed for a faster first launch.
- The published APK is now the minified release build, signed with the same key as before, so it installs over earlier versions. An unminified debug APK is still produced as a fallback artifact.

## 1.2.0
- New: Persian (فارسی) interface with right-to-left layout. It follows the phone's language by default; change it in Settings → Language (System / English / فارسی).
- All screens, dialogs, status messages, the notification and the Quick Settings tile are translated. The Activity log stays in English on purpose so it is easy to share.

## 1.1.0
- New: first-run checklist on the Connection tab (account, location, notifications, battery, certificate). It hides itself when everything is ready.
- New: Quick Settings tile. Tap to connect, tap again to pause and sign out. Add it from the edit panel of Quick Settings.
- New: existing sessions are detected. If the portal already has a live session for one of your accounts (signed in by hand, or before the app restarted), UniLink keeps it alive instead of signing in again. Only the user name is sent for this check. The Connect button always signs in fresh.
- Fix: notification status now detects notifications blocked in system settings (the old check said "OK" even when they were blocked), and the Allow button opens the right settings page.

## 1.0.1
- Fix: Wi-Fi name (SSID) not detected on some phones, leaving the app stuck on "Waiting for campus Wi-Fi".
- Fallback to the portal when the SSID is hidden and only one network is configured.
- Activity log now shows the detected Wi-Fi and matched network.

## 1.0.0
- First Android debug release.
- Five tabs: Connection, Accounts, Networks, Activity, Settings.
- Wi-Fi-bound requests (VPN-safe), system proxy ignored, redirects refused.
- Portal certificate pinning with user approval.
- Multi-account rotation with monthly quota detection and automatic reset.
- Encrypted storage, foreground service, start on boot.
