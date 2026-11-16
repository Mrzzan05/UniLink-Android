## UniLink Android v1.12.1

- **Fix: sign-in no longer stalls when the portal ignores the live check.** A timeout of the "is an account already live?" check blocked the sign-in even though the login itself worked in 0.1 s. Now only an unreachable portal triggers the quick retry, and the check gives up after 4 s.
- **Fix: Connect responds immediately** ("Signing in…") instead of looking frozen.
- **Fix: Pause & sign out during a sign-in** discards that attempt instead of reconnecting.

### Update
Install `UniLink-v1.12.1.apk` over the previous version; accounts and settings are kept. The signing certificate SHA-256 is at the bottom of these notes (see SECURITY.md).

> Unofficial project, not affiliated with any university or with Sophos. Use only accounts you are allowed to use.
