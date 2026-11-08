# Fork update contract

This fork follows upstream Buddy while retaining the reviewed phone-control protections. Invoke `$buddy-update` in Codex/T3 or `/buddy-update` in Claude Code/T3 for read-only update analysis. For the full workflow, explicitly request syncing upstream, resolving conflicts, committing, pushing the current branch to your fork, building and deploying to the configured phone. Its Grant section preserves those boundaries; the owning thread commits and deploys. No scheduled or unattended updates are configured.

## Repositories and baseline

- Fork/push remote: `origin`, `https://github.com/zees-dev/buddy-android.git`.
- Read-only source remote: `upstream`, `https://github.com/ghorbelhamdi/buddy-android.git`.
- Latest integrated stable release: `v2.2.26`, commit `23e119d1f2518764be29c6fe6011e962774e32d5`.
- Also included: upstream main `6f9bc9c9725469bf87db0ec29d8f047bda4eb75b`, a descendant of that release.
- Current maintained app branch: `develop`. Preserve the caller's branch for future updates.

On the initial workflow run, both upstream revisions were already ancestors of the fork; no upstream merge or conflict resolution was needed. The earlier 49 fork commits were retained. They are not equivalent to configuration-only changes.

The first push of `develop` also publishes those 49 commits and its local phone-skill commit. It creates a remote branch separate from `origin/main`, which has a patch-equivalent phone-skill commit. The user selected this maintained fork after the inventory and explicitly requested pushing the current branch; no other history is silently added or rewritten.

## Retained behavior

- `Guard.java`, `Tools.java`, `BuddyService.java`, `McpServer.java`, `Bubble.java`: executing-request/session-bound approval, credential-window guards, human-only system/installer/Store surfaces, cancellation, token revocation, bounded admission, and scrolling confined to its owning window. Ordinary numeric input and safe navigation remain usable.
- `Backup.java`, `Prefs.java`, `StepLog.java`: exclude credentials from backups, rotate phone credentials, and retain redaction. Keep operator tokens and private phone transcripts outside git.
- `Account.java`, `Linux.java`, `Helper.java`, `linux/buddy-codex`, `linux/install-codex.sh`, `linux/codex.sha256`: verified pinned Codex installation and the Alpine security-upgrade gate.
- `Updates.java`: updates come from this fork; verify package, version and signing identity. Play Store catalog links stay package-pinned while Store input and installs remain human-controlled.
- Gradle verification/checksum files and pinned CI inputs: preserve supply-chain verification. Refresh checksums only for reviewed intentional dependency changes.
- `.github/workflows/build.yml`, `.github/release/`, `docs/RELEASING.md`: manual-only develop releases, signing secrets restricted to dispatch, pinned compatible signer, pre-publication validation, monotonic versions, stale-branch rejection, atomic branch/tag push and no overwriting releases.
- `.agents/skills/`, `.claude/skills/`, `mcp-bridge/phone-link.sh`: T3 connection/update skills and the private SSH workflow. [Phone setup and recovery](T3-PHONE.md) owns its details.

Prefer preserving upstream structure and a small behavior delta. Remove a fork patch only after verifying equivalent upstream behavior and its checks. Resolve conflicts explicitly; do not bulk-select one side or rewrite published history.

## Checks and release

Run `bash linux/test/run.sh`, `bun test ./.agents/skills/phone/scripts/call.test.mjs`, `bun test ./.github/release/prepare.test.mjs`, ShellCheck for changed shell files, actionlint for workflow changes and `git diff --check`. Build native libraries from the merged submodules with the pinned NDK, then build the GitHub APK with the declared Gradle wrapper and JDK 21. Run focused checks for changed security/runtime paths plus the full council panel; preserve and report unrelated baseline failures. Earlier optional Android lint failures are not represented as green.

Use `BuildInfo.java` for a fork suffix and monotonically increasing version code. Verify the APK's package is `app.buddy.assistant`, its signer matches the installed app and its code is newer. Verify signature and `zipalign -c -P 16 4`, plus ELF LOAD alignment for all bundled arm64 libraries. A review candidate may be built before commit; the final build must use a clean committed tree. Record source commit/tree and APK hash outside git, and check both again before delivery. Do not commit build output.

The current phone was provisioned with this Mac's Android debug signing identity. The first update builds the release variant with that same identity to preserve data; this is a compatibility/test release, not a move to production signing. Prefer the standard Gradle signing configuration; a temporary init script can select the existing `debug` signing config for the release variant without copying a key or printing passwords. Pass that script explicitly: `./gradlew --no-daemon --init-script build/compatibility-signing.gradle assembleGithubRelease`. A dedicated production-key migration is a separate decision. Do not upload signing material during an upstream update. The separately approved [manual release workflow](RELEASING.md) uses encrypted GitHub Actions signing secrets; never expose them in source, artifacts or logs.

Deployment is to the configured CPH2671 through its existing private connection. A private APK download followed by the user's Android Install/Update tap works without wireless debugging. Keep any temporary download server limited to the private interface/target phone and stop it after transfer. Do not uninstall, clear data, approve the installer or enable developer/security permissions for the user.

Installing an update can kill Buddy and its SSH terminal. The user must reopen Buddy and start `sh /root/bin/phone-link`; Sol's saved configuration remains. Verify the new app version, phone tools, Home/app navigation and a bubble reply. Automatic reboot startup, guaranteed network recovery, effective Fast processing and the inherited radial-menu crash are not solved by updating this skill.

After completing an update, ask whether to publish it through [buddy-release](../.agents/skills/buddy-release/SKILL.md). That separate approval authorizes its version commit, tag and GitHub release. Declining leaves the local APK available.
