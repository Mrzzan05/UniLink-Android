## UniLink Android v1.10.4

- Fix: the usage page address was malformed (portal answered 404); it is now exact.
- Usage requests use browser-like headers; an unreadable usage page reports a short masked excerpt of the reply. (Remaining data now loads when the portal accepts it.) The portal's usage page requires an anti-forgery token that the home page provides; UniLink reads and sends it.
- Needs campus Wi-Fi without a VPN tunnel for `gate1.urmia.ac.ir`.

### Update
Install `UniLink-v1.10.4.apk` over the previous version; accounts and settings are kept. The signing certificate SHA-256 is at the bottom of these notes (see SECURITY.md).

> Unofficial project, not affiliated with any university or with Sophos. Use only accounts you are allowed to use.
