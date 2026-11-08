#!/bin/sh
set -eu

# npm's arm64 package includes the musl binary and its adjacent resources. Verify before unpacking.
version=0.167.0
sha512=e5cbfeb326d15aaa5124be8a0afdd84a7231d461d19a37a4f7dec2dfc9f565f9e8e07a262832e43528d62c338e736f884a554d491467c925aae332a6ce5635da
check() { sha256sum -c /root/bin/codex.sha256 >/dev/null 2>&1; }
if [ "${1:-}" = --check ]; then check; exit; fi
mkdir -p /opt /usr/local/bin
# Serialize migration and verify existing bytes, not npm presence, version output or a saved marker.
exec 9>/opt/buddy-codex.lock
busybox flock -x 9
if [ "${1:-}" = --ensure ] && check; then exit; fi
stage=$(mktemp -d /opt/buddy-codex.XXXXXX)
trap 'rm -rf "$stage"' EXIT
curl -fsSL "https://registry.npmjs.org/@openai/codex/-/codex-$version-linux-arm64.tgz" -o "$stage/codex.tgz"
printf '%s  %s\n' "$sha512" "$stage/codex.tgz" | sha512sum -c -
tar xzf "$stage/codex.tgz" -C "$stage"
rm "$stage/codex.tgz"
binary=package/vendor/aarch64-unknown-linux-musl/bin/codex
test -x "$stage/$binary"
dest=/opt/buddy-codex-$version
if [ -e "$dest" ]; then mv "$dest" "$stage/previous"; fi
if ! mv "$stage/package" "$dest"; then
    [ ! -e "$stage/previous" ] || mv "$stage/previous" "$dest"
    exit 1
fi
ln -sfn "$dest/vendor/aarch64-unknown-linux-musl/bin/codex" /usr/local/bin/codex
check
"$dest/vendor/aarch64-unknown-linux-musl/bin/codex" --version
