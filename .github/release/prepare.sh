#!/usr/bin/env bash
set -euo pipefail
file=app/src/main/java/app/buddy/assistant/BuildInfo.java
fail() { echo "$*" >&2; exit 1; }
[[ -z $(git status --porcelain) ]] || fail 'Release requires a clean tree'
current=$(sed -n 's/.*VERSION = "\([^"]*\)";.*/\1/p' "$file")
code=$(sed -n 's/.*VERSION_CODE = \([0-9]*\);.*/\1/p' "$file")
pattern='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-zees\.([1-9][0-9]*))?$'
[[ $current =~ $pattern ]] || fail 'Unsupported source version'
base=${current%%-*}
sequence=0
while IFS= read -r version; do
  version=${version#v}
  [[ $version =~ $pattern ]] || continue
  candidate=${version%%-*}
  highest=$(printf '%s\n%s\n' "$base" "$candidate" | sort -V | tail -1)
  if [[ $highest != "$base" ]]; then base=$highest; sequence=0; fi
  if [[ $candidate == "$base" && $version == *-zees.* ]]; then
    n=${version##*.}
    if (( n > sequence )); then sequence=$n; fi
  fi
done < <(printf '%s\n' "$current"; git tag --list 'v*')
version=${RELEASE_VERSION:-}
version=${version#v}
if [[ -z $version ]]; then version="$base-zees.$((sequence + 1))"; fi
[[ $version =~ $pattern ]] || fail 'Use X.Y.Z or X.Y.Z-zees.N (optional v prefix)'
# Buddy's updater ranks a bare base below that base's fork suffixes.
newbase=${version%%-*}
[[ $(printf '%s\n%s\n' "$base" "$newbase" | sort -V | tail -1) == "$newbase" ]] || fail 'Version would downgrade the release base'
if [[ $newbase == "$base" ]]; then
  [[ $version == *-zees.* ]] || fail 'A bare version must advance the release base'
  (( ${version##*.} > sequence )) || fail 'Fork release sequence must increase'
fi
tag=v$version
git show-ref --verify --quiet "refs/tags/$tag" && fail 'Tag already exists'
# Upstream tags may carry a newer Android code than the maintained branch.
while IFS= read -r ref; do
  value=$(git show "$ref:$file" 2>/dev/null | sed -n 's/.*VERSION_CODE = \([0-9]*\);.*/\1/p' || true)
  if [[ $value =~ ^[0-9]+$ ]] && (( value > code )); then code=$value; fi
done < <(git tag --list 'v*')
code=$((code + 1))
(( code <= 2100000000 )) || fail 'Android version code exhausted'
sed -i.bak "s/VERSION = \"[^\"]*\"/VERSION = \"$version\"/; s/VERSION_CODE = [0-9]*/VERSION_CODE = $code/" "$file"
rm "$file.bak"
git add "$file"
git -c user.name='github-actions[bot]' -c user.email='41898282+github-actions[bot]@users.noreply.github.com' commit -m "Release $tag"
{
  echo "RELEASE_TAG=$tag"
  echo "RELEASE_NAME=$version"
  echo "RELEASE_CODE=$code"
  echo "RELEASE_COMMIT=$(git rev-parse HEAD)"
} >> "$GITHUB_ENV"
