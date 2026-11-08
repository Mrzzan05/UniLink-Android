# Changelog

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
