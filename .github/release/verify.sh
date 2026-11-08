#!/usr/bin/env bash
set -euo pipefail
apk=$1
tools="$ANDROID_HOME/build-tools/36.0.0"
[[ $BUDDY_SIGNER_SHA256 == 1bb834b7164eb3f1f54d02e26171363979e1ee93ed8420487d80cc3e5572111c ]]
"$tools/apksigner" verify --verbose --print-certs "$apk" > "$RUNNER_TEMP/signature.txt"
[[ $(grep -Ec '^Signer #[0-9]+ certificate SHA-256 digest:' "$RUNNER_TEMP/signature.txt") == 1 ]]
grep -Fq "Signer #1 certificate SHA-256 digest: $BUDDY_SIGNER_SHA256" "$RUNNER_TEMP/signature.txt"
"$tools/aapt2" dump badging "$apk" > "$RUNNER_TEMP/badging.txt"
grep -Fq "package: name='app.buddy.assistant' versionCode='$RELEASE_CODE' versionName='$RELEASE_NAME'" "$RUNNER_TEMP/badging.txt"
if grep -q 'application-debuggable' "$RUNNER_TEMP/badging.txt"; then exit 1; fi
"$tools/zipalign" -c -P 16 4 "$apk"
mkdir -p "$RUNNER_TEMP/elf-check"
unzip -j "$apk" 'lib/arm64-v8a/*.so' -d "$RUNNER_TEMP/elf-check"
count=0
for lib in "$RUNNER_TEMP/elf-check/"*.so; do
  # Every LOAD segment must support 16 KB pages.
  readelf -lW "$lib" | awk '/ LOAD / { found=1; if ($NF != "0x4000" && $NF != "0x10000") exit 1 } END { if (!found) exit 1 }'
  count=$((count + 1))
done
[[ $count == 5 ]]
[[ -z $(git status --porcelain) ]]
