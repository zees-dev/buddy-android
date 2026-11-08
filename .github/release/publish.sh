#!/usr/bin/env bash
set -euo pipefail
[[ $(git rev-parse HEAD) == "$RELEASE_COMMIT" && -z $(git status --porcelain) ]]
[[ $(git ls-remote origin refs/heads/develop | cut -f1) == "$GITHUB_SHA" ]] || { echo 'develop moved; dispatch a fresh release' >&2; exit 1; }
# Branch and immutable tag advance together, only after all build checks pass.
git tag "$RELEASE_TAG" "$RELEASE_COMMIT"
git push --atomic origin HEAD:refs/heads/develop "refs/tags/$RELEASE_TAG"
(
  cd dist
  sha256sum buddy*.apk > SHA256SUMS
)
jq -n --arg commit "$RELEASE_COMMIT" --arg tree "$(git rev-parse 'HEAD^{tree}')" \
  --arg tag "$RELEASE_TAG" --arg version "$RELEASE_NAME" --argjson code "$RELEASE_CODE" \
  --arg signer "$BUDDY_SIGNER_SHA256" --arg sha256 "$(sha256sum dist/buddy.apk | cut -d' ' -f1)" \
  '{commit:$commit,tree:$tree,tag:$tag,version:$version,versionCode:$code,signerSha256:$signer,apkSha256:$sha256}' > dist/release.json
# shellcheck disable=SC2016
printf 'Download **buddy.apk** to update Buddy. This release retains the compatibility/test signer.\n\nSource: `%s`\nVersion: `%s` (%s)\n\nInstalling may stop the Buddy terminal. Reopen Sessions > Terminal and run `sh /root/bin/phone-link`.\n' \
  "$RELEASE_COMMIT" "$RELEASE_NAME" "$RELEASE_CODE" > "$RUNNER_TEMP/notes.md"
gh release create "$RELEASE_TAG" dist/buddy*.apk dist/SHA256SUMS dist/release.json \
  --verify-tag --draft --title "Buddy $RELEASE_NAME" --notes-file "$RUNNER_TEMP/notes.md"
gh release edit "$RELEASE_TAG" --draft=false --prerelease=false --latest
