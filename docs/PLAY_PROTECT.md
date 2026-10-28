# Play Protect warnings — technical analysis (v1.0.17)

This document is the technical record behind the landing-page Play Protect
notice. It states only facts that were actually verified, separates the
different warnings a user may see, and describes the corrective actions taken.
It deliberately does **not** instruct users to bypass warnings or disable
Google Play Protect.

## What warnings exist (and which one is which)

| Warning | Who shows it | What it actually means |
|---|---|---|
| «معطل تثبيت تطبيقات غير معروفة» / "install unknown apps" | Android system (Settings) | The *source* that started the install (the browser, file manager, or this app's updater) has not been granted the one-time per-app "install unknown apps" grant. It is a permission gate, not a malware verdict. |
| «قد يكون التطبيق ضارًا» / "harmful app blocked" with a *Scan app* sheet | Google Play Protect | Play Protect scans every APK installed from outside Play. Apps signed with a certificate that has **no Play Store reputation** routinely trigger this caution. It is a reputation heuristic, not a confirmed malware detection. |
| «تم حظر التطبيق» + refusal to install at all | Play Protect / system | Hard block. For this app the only plausible hard block is a signing-certificate change between the installed version and the update — which our own updater refuses earlier, and which the release guard now prevents at the source. |

## What was actually verified (2026-10-28)

- **APK integrity.** For every published release v1.0.13 … v1.0.16 the versioned
  APK asset matches its published `.sha256` asset byte-for-byte, and the
  stable-named asset (`noor-al-sabeel.apk`) is byte-identical to the versioned
  one in the same release.
- **Signature presence and validity.** Each APK carries an APK Signing Block
  with a **v2** signature (signer public-key SHA-256
  `522575c3bc08f7874ecab07e7d083f89e1a21c831155c7555e182739c9ada408`).
  The APKs are **not** v1/JAR-signed — `minSdk 24` (Android 7.0) accepts v2
  alone. (An earlier version of the landing page claimed "strict v1+v2 dual
  signing"; that claim was wrong and has been removed.)
- **Certificate stability.** The signer certificate (SHA-256
  `a0ccfb6ac39a957e91caf5f5c4e5293a6dc14762600b945f7da44d934768a21f`) is
  **identical across v1.0.13, v1.0.14, v1.0.15 and v1.0.16**, parsed directly
  from the published APK signing blocks. The signing keystore SHA-256 logged by
  the release workflow (`115c5b9b…`) is likewise unchanged between runs. An
  in-place update from any published version therefore passes the OS signature
  check.
- **No manifest red flags.** `v1.0.16` removed
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; the remaining notable permission is
  `REQUEST_INSTALL_PACKAGES`, required by the in-app GitHub-Releases updater
  and gated by the standard "install unknown apps" flow. No accessibility
  services, no SMS/contacts permissions, no hidden receivers.

## Why the warning can appear for this app

Play Protect's install-time scan is *reputation-based* for sideloaded apps:
this project is distributed through GitHub Releases, is self-signed (not
Play-signed), and has no Play Store listing — so Play Protect cannot vouch for
its publisher and shows the caution sheet. That is a property of the
*distribution channel*, not a defect in a specific release, and no evidence of
tampering or malware exists in the published bytes (see the checks above and
the automated post-release verification in `.github/workflows/verify-download.yml`).

## Corrective actions actually taken

1. **v1.0.17 release guard** (`.github/workflows/release.yml`): a published
   release is never mutated — a re-run for an existing tag either no-ops
   (identical bytes) or fails the workflow. This removes the mid-publish asset
   swaps that produced false SHA-256 mismatches during the v1.0.16 double
   publish.
2. **v1.0.17 updater hardening** (`UpdateManager` + `UpdateDownloadPolicy`):
   a checksum mismatch re-fetches the published checksum once (and only
   re-downloads if the bytes still disagree), so a release published while an
   update is in flight no longer fails the update. No verification step was
   weakened: checksum, package name, versionCode and signing certificate must
   all still match exactly.
3. **Honest public notice** (`src/components/PlayProtectNotice.tsx`): removed
   the "tap Install anyway" instruction and the incorrect "v1+v2 dual signing"
   and "battery permission was the main cause" claims; added self-verification
   steps (compare the published `.sha256`) and the official appeal form link.
4. **Unsigned builds are no longer publishable**: the release workflow fails if
   the signing secrets are missing, so a signature-breaking "update" can never
   reach users.

## The durable fix

The structural fix is indexing the app with Google: publishing through Google
Play (or filing the Play Protect appeal for the signing certificate) gives the
certificate a reputation, which is what the heuristic reacts to. Until then,
the app *cannot* and *should not* try to make the warning disappear on the
device — that is not technically possible without disabling Play Protect,
which this project will never ask users to do.
