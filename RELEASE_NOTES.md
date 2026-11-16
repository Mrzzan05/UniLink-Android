## UniLink Android v1.12.0

- **Most data first** (Settings, off by default): sign in with the account that has the most data left; nearly empty accounts (under 200 MB) go last.
- **One quiet notification when no account can sign in** (out of data, disabled or wrong password). Switch off in Settings.
- **Data left on the widget and the Quick Settings tile** for the account in use.
- **Faster sign-in after joining the Wi-Fi:** shorter reachability check and quick retries for the first two minutes.
- **Wi-Fi marked "validated" after sign-in:** UniLink tells Android the network works so the system re-checks it (helps tethering and apps that prefer mobile data); the result is logged.
- Unit tests for the portal parsers, usage parser, account ordering and version check now run on every build.

### Update
Install `UniLink-v1.12.0.apk` over the previous version; accounts and settings are kept. The signing certificate SHA-256 is at the bottom of these notes (see SECURITY.md).

> Unofficial project, not affiliated with any university or with Sophos. Use only accounts you are allowed to use.
