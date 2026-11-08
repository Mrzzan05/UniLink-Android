## UniLink Android v1.0.1

Early debug release of the Android version of UniLink: automatic sign-in and session keepalive for university Wi-Fi captive portals (Sophos / Cyberoam), tuned for Urmia University (UUWN).

### What's new in 1.0.1
- Fixed: the app could stay on "Waiting for campus Wi-Fi" because Android hid the Wi-Fi name (SSID). The name is now read through the modern network-callback API, and with a single configured network the app falls back to the portal instead of waiting forever.
- Added: the Activity tab now logs what was detected (`Wi-Fi detected: SSID=… group=…`) to make problems easier to diagnose.

### Highlights
- Sign-in and keepalive pinned to the physical Wi-Fi interface, even while a VPN is on
- Several accounts with automatic rotation when a monthly data allowance runs out
- Portal HTTPS certificate pinning (SHA-256) with explicit user approval
- Encrypted storage (Android Keystore), background service, start on boot

### Install
1. Download `UniLink-v1.0.1.apk` below.
2. Open it on your phone and allow "Install unknown apps" if asked.
3. Allow Location (needed only to read the Wi-Fi name), Notifications and Unrestricted battery.
4. Add an account on the Accounts tab, connect to UUWN, review and trust the portal certificate.

Verify the download with `UniLink-v1.0.1.apk.sha256` if you like. This is a debug-signed build; updates install over earlier builds signed with the same key.

> Unofficial project, not affiliated with any university or with Sophos. Use only accounts you are allowed to use.
