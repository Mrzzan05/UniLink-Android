## UniLink Android v1.4.1

### What's new
- **VPN fix.** With a VPN on, UniLink could suddenly stop signing in and keeping the session alive ("Binding socket to network … EPERM"). It now falls back to the VPN route when Android refuses a direct Wi-Fi connection, retries every minute at most, and reacts at once when a VPN starts or stops. If your VPN still blocks the portal, the app tells you what to change: exclude UniLink from the VPN tunnel, or allow the local network.
- **Polish.** Clearer language chips, the activity log in a glass panel, a more visible Pause button.
- Includes everything from 1.4.0: glass interface, home-screen widget, GPL-3.0-or-later license.

### Install
Download `UniLink-v1.4.1.apk` below and install it over the previous version. Your accounts and settings are kept.

Verify the download with `UniLink-v1.4.1.apk.sha256` if you like. Signed with the project's debug key.

> Unofficial project, not affiliated with any university or with Sophos. Use only accounts you are allowed to use.
