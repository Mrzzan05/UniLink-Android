## UniLink Android v1.5.0

### Security: new signing key
Releases are now signed with a **private key** that is never stored in the repository. Before this, releases were signed with a debug key that is public in the repo, so anyone could have built an APK that Android accepts as an update to yours. The release workflow now refuses to publish an APK signed with that key.

### You must reinstall once
Because the key changed, Android can't update 1.4.x in place:
1. Note down your accounts (student numbers and passwords): they are removed with the app.
2. Uninstall UniLink, then install `UniLink-v1.5.0.apk` from below.
3. Add your accounts again. Updates after this install normally.

### Verify the download
The signing certificate SHA-256 is at the bottom of these notes. Check it with `apksigner verify --print-certs UniLink-v1.5.0.apk` (see SECURITY.md). If it doesn't match, don't install the file.

> Unofficial project, not affiliated with any university or with Sophos. Use only accounts you are allowed to use.
