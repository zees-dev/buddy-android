---
name: buddy-release
description: Publish a signed Buddy APK on this fork’s GitHub Releases using its manual develop workflow.
---

Only the user’s explicit release approval in this thread authorizes dispatch, the version commit/push, tag creation and publication. A bare invocation grants none; honor existing approval without asking twice. See [release behavior](../../../docs/RELEASING.md).

1. Verify `origin` is `https://github.com/zees-dev/buddy-android.git`, fetch origin develop, and require the intended release commit (normally local HEAD) to equal `origin/develop`. If the update is unpushed, push only with existing or newly obtained authorization; otherwise stop rather than release stale code. Record that SHA and dispatch: `gh workflow run build.yml --repo zees-dev/buddy-android --ref develop`. Omit inputs unless the user requests a specific version; then add `-f version=<version>`.
2. Identify this dispatch by event, branch, matching `headSha` and start time using `gh run list`; wait for that run with `gh run watch <id> --repo zees-dev/buddy-android --exit-status`. On failure, inspect logs; fix build errors within authorized scope before a fresh dispatch. If a tag or draft exists, stop for an explicit recovery decision; never overwrite a tag/release or retry publication automatically.
3. Verify the run’s release tag, `release.json`, APK checksum and signer against the release contract. Return the release URL and `https://github.com/zees-dev/buddy-android/releases/latest/download/buddy.apk`. Fetch origin and fast-forward a clean local develop if appropriate; preserve local skill edits. Installation remains the user’s action.
