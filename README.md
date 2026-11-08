<div align="center">

# UniLink for Android

**Stay signed in to your university Wi-Fi: automatically, reliably, and without touching the browser.**

[![Build APK](../../actions/workflows/build-apk.yml/badge.svg)](../../actions/workflows/build-apk.yml)
![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)

[**Download the latest APK**](../../releases/latest) · [فارسی](README_FA.md) · [Changelog](CHANGELOG.md)

</div>

---

UniLink signs you in to Sophos / Cyberoam captive portals (built for Urmia University's **UUWN**) and keeps the session alive in the background. It is the Android companion to the Windows version of UniLink.

Phones close browser tabs, sleep the screen and often run a VPN, and each of those breaks a portal session. UniLink does the sign-in itself from a lightweight background service, so none of that matters.

## Features

| | |
|---|---|
| **VPN-safe** | Portal traffic is bound to the physical Wi-Fi interface, so it works even while a VPN is on. The system proxy is ignored. |
| **Multiple accounts** | Accounts are tried one at a time, in your order. When the portal reports the monthly allowance is used up, the account is skipped until the next month (Asia/Tehran time by default) and the next one is used. |
| **Smart error handling** | A rejected password blocks only that account. Network errors and unknown replies are retried with growing delays and never burn an account. |
| **Certificate pinning** | The portal uses a self-signed certificate. UniLink shows its SHA-256 fingerprint once, you approve it, and from then on only that certificate is accepted. No password is sent before approval. |
| **Redirects refused** | The portal can't bounce your credentials to another address. |
| **Encrypted storage** | Accounts and settings are encrypted with a key held in the Android Keystore and never leave the phone. |
| **Runs in the background** | Foreground service with a persistent notification, optional wake lock, and start on boot. |
| **Quick tile and checklist** | A Quick Settings tile to connect or pause, and a first-run checklist that walks you through permissions. |
| **Keeps existing sessions** | If the portal already has a live session for your account, UniLink keeps it alive instead of signing in again. |
| **Diagnostics** | In-memory activity log you can share. Passwords and student numbers are never logged. |

## Install

1. Open the [latest release](../../releases/latest) on your phone and download `UniLink-vX.Y.Z.apk`.
2. Open the file and allow **Install unknown apps** if Android asks.
3. Allow **Location** (Android only reveals the Wi-Fi name to apps that have it; location is never stored or sent), **Notifications**, and **Unrestricted battery** so Android doesn't pause the service.
4. **Accounts** tab → **+** → add your student number and password.
5. Connect to `UUWN`. On the **Connection** tab a certificate card appears: compare the fingerprint with the one your browser shows on the normal login page, then tap **Trust this certificate**.

After that it signs in on its own whenever you're on campus Wi-Fi.

## The five tabs

- **Connection**: current state, active account, last keepalive, Connect / Pause & sign out.
- **Accounts**: add, edit, reorder, disable, set default, clear a block.
- **Networks**: Wi-Fi names, portal URLs, keepalive interval (30–180 s), time zone, extra quota phrases, pinned certificates.
- **Activity**: what happened and why, with a share button.
- **Settings**: automatic sign-in, start on boot, wake lock, permission shortcuts.

## Build it yourself

You don't need Android Studio: push the repo to GitHub and the **Build APK** workflow builds it for you.

- Every push to `main` produces a debug APK under **Actions → latest run → Artifacts**.
- Pushing a tag such as `v1.0.2` also publishes a **GitHub Release** with the APK and its SHA-256:

```bash
git tag v1.0.2
git push origin v1.0.2
```

Locally: open the folder in Android Studio (JDK 17, Android SDK 35) and press Run.

The APK is signed with the debug key committed in `app/debug.keystore`, so new builds install over old ones. This is fine for personal use, but it is not a Play Store release key.

## Status and limitations

- Early release. The portal protocol was reimplemented from the Windows version's documented behavior, so portals that reply differently may need small adjustments. If something doesn't work, open an issue with the **Share log** output from the Activity tab.
- If a VPN's kill switch blocks the local network, enable "Allow LAN" in that VPN app.
- The interface is English only for now.

## Roadmap

Home-screen widget · import/export of network profiles · per-account usage notes · Persian interface · battery tuning

## Disclaimer

UniLink is an independent, unofficial project. It is not affiliated with or endorsed by any university or by Sophos. Use it only with accounts you are authorized to use, and follow your institution's network policy.
