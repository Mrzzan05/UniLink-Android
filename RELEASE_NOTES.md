## UniLink Android v1.10.1

- Fix: remaining data now loads from the user portal's usage page (the home page is only a shell). The anti-forgery token is sent when the portal provides one.
- One automatic retry on a DNS miss; clearer diagnostics if the usage table still cannot be found.
- Data usage needs the phone on the campus network without a VPN tunnel for `gate1.urmia.ac.ir`.

### Update
Install `UniLink-v1.10.1.apk` over the previous version; accounts and settings are kept. The signing certificate SHA-256 is at the bottom of these notes (see SECURITY.md).

> Unofficial project, not affiliated with any university or with Sophos. Use only accounts you are allowed to use.
