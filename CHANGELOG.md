# Changelog

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
