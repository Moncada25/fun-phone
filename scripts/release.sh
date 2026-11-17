#!/usr/bin/env bash
# Builds and verifies the Play bundle and the signed community APK for a release tag.
# Usage: scripts/release.sh [vX.Y.Z]   (defaults to v$VERSION_NAME from release.properties)
# See RELEASING.md. The script never pushes, tags, uploads or prints signing secrets.
set -euo pipefail

readonly COMMUNITY_CERT_SHA256="caf15843b5efc692ff6dec15e989d2fb32946dfc089453dfc6d31edf242b7151"
readonly COMMUNITY_PACKAGE="com.bookverse.contacts.community"
readonly PLAY_PACKAGE="com.bookverse.contacts"
readonly REPO_SLUG="Moncada25/fun-phone"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

fail() {
  echo "error: $*" >&2
  exit 1
}

step() {
  printf '\n==> %s\n' "$*"
}

require() {
  command -v "$1" >/dev/null 2>&1 || fail "missing required tool: $1"
}

read_property() {
  sed -n "s/^$1=//p" "$2" | tr -d '\r'
}

sdk_dir() {
  local dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -z "$dir" && -f local.properties ]]; then
    dir="$(read_property sdk.dir local.properties)"
  fi
  [[ -n "$dir" && -d "$dir" ]] || fail "Android SDK not found; set ANDROID_HOME or sdk.dir in local.properties"
  printf '%s\n' "$dir"
}

for tool in git java jarsigner keytool unzip shasum; do
  require "$tool"
done

sdk="$(sdk_dir)"
build_tools="$(find "$sdk/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | awk 'END {print}')"
apksigner="$build_tools/apksigner"
aapt2="$build_tools/aapt2"
[[ -x "$apksigner" && -x "$aapt2" ]] || fail "apksigner/aapt2 not found in $build_tools"

tag="${1:-v$(read_property VERSION_NAME release.properties)}"
[[ "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "tag must look like vX.Y.Z (got '$tag')"
version="${tag#v}"

step "Checking tag $tag"
git fetch --quiet origin main "refs/tags/$tag:refs/tags/$tag" \
  || fail "tag $tag is not on origin; create and push it on the merged release commit first"
commit="$(git rev-parse "$tag^{commit}")"
remote_tag="$(git ls-remote origin "refs/tags/$tag" "refs/tags/$tag^{}" | sort -k2 | awk 'END {print $1}')"
[[ "$remote_tag" == "$commit" ]] || fail "local and remote $tag point to different commits"
git merge-base --is-ancestor "$commit" origin/main || fail "$tag ($commit) is not on origin/main"

tagged_properties="$(git show "$commit:release.properties")"
tagged_version="$(sed -n 's/^VERSION_NAME=//p' <<<"$tagged_properties")"
version_code="$(sed -n 's/^VERSION_CODE=//p' <<<"$tagged_properties")"
[[ "$tagged_version" == "$version" ]] || fail "release.properties at $tag says $tagged_version, expected $version"
git cat-file -e "$commit:fastlane/metadata/android/en-US/changelogs/$version_code.txt" 2>/dev/null \
  || fail "fastlane changelog $version_code.txt is missing at $tag; run scripts/export-play-listing.py"
grep -q "^## \[$version\]" <(git show "$commit:CHANGELOG.md") || fail "CHANGELOG.md has no [$version] section at $tag"
echo "$tag -> $commit (versionCode $version_code)"

[[ -f gradle.properties.private || -n "${COMMUNITY_RELEASE_STORE_FILE:-}" ]] \
  || fail "no signing configuration: add gradle.properties.private or export RELEASE_* and COMMUNITY_RELEASE_*"

step "Creating a clean checkout of $tag"
work="$(mktemp -d "${TMPDIR:-/tmp}/fun-phone-release.XXXXXX")"
checkout="$work/fun-phone"
trap 'rm -rf "$work"' EXIT
git clone --quiet --no-checkout "$repo_root" "$checkout"
git -C "$checkout" checkout --quiet --detach "$commit"
for private_file in gradle.properties.private local.properties keystore.properties; do
  if [[ -f "$private_file" ]]; then
    cp "$private_file" "$checkout/$private_file"
  fi
done
[[ "$(git -C "$checkout" rev-parse HEAD)" == "$commit" ]] || fail "checkout is not on $commit"
[[ -z "$(git -C "$checkout" status --porcelain)" ]] || fail "the release checkout is not clean"

step "Building assembleCommunityRelease and bundlePlayRelease"
(
  cd "$checkout"
  ./gradlew --no-configuration-cache --console=plain \
    :app:validateCommunityReleaseSigning :app:assembleCommunityRelease :app:preparePlayStoreArtifacts
)
[[ -z "$(git -C "$checkout" status --porcelain)" ]] || fail "the build modified tracked files"

apk_source="$(find "$checkout/app/build/outputs/apk/community/release" -name '*.apk' | awk 'NR == 1')"
aab_source="$(find "$checkout/app/build/playStoreArtifacts/playRelease" -name '*.aab' | awk 'NR == 1')"
[[ -f "$apk_source" ]] || fail "community release APK not found"
[[ -f "$aab_source" ]] || fail "Play bundle not found"

out="$repo_root/build/release/$tag"
rm -rf "$out"
mkdir -p "$out"
apk="$out/fun-phone-community-$version.apk"
aab="$out/fun-phone-play-$version.aab"
cp "$apk_source" "$apk"
cp "$aab_source" "$aab"
find "$checkout/app/build/playStoreArtifacts/playRelease" -type f ! -name '*.aab' -exec cp {} "$out/" \;

step "Verifying the community APK"
signature_report="$("$apksigner" verify --verbose --print-certs "$apk" 2>/dev/null)" \
  || fail "apksigner could not verify the APK"
grep -Eq '^Verified using v2 scheme.*: true$' <<<"$signature_report" \
  || fail "APK is not signed with APK Signature Scheme v2"
apk_cert="$(sed -n 's/^.*[Ss]igner.*certificate SHA-256 digest: //p' <<<"$signature_report" | awk 'NR == 1')"
[[ "$apk_cert" == "$COMMUNITY_CERT_SHA256" ]] \
  || fail "APK certificate $apk_cert does not match the community key $COMMUNITY_CERT_SHA256"
badging="$("$aapt2" dump badging "$apk" 2>/dev/null | awk 'NR == 1')"
grep -q "name='$COMMUNITY_PACKAGE' versionCode='$version_code' versionName='$version'" <<<"$badging" \
  || fail "unexpected APK identity: $badging"
vcs="$(unzip -p "$apk" META-INF/version-control-info.textproto 2>/dev/null || true)"
grep -q "revision: \"$commit\"" <<<"$vcs" || fail "APK does not embed commit $commit in META-INF/version-control-info.textproto"
echo "signature, package, version and embedded commit OK"

step "Verifying the Play bundle"
jarsigner_output="$(jarsigner -verify "$aab" 2>&1 || true)"
grep -q "jar verified" <<<"$jarsigner_output" || fail "AAB signature does not verify"
aab_cert="$(keytool -printcert -jarfile "$aab" 2>/dev/null | sed -n 's/^[[:space:]]*SHA256: //p' | awk 'NR == 1' | tr -d ':' | tr 'A-F' 'a-f')"
[[ "$aab_cert" != "$COMMUNITY_CERT_SHA256" ]] || fail "the AAB is signed with the community key"
if [[ -n "${PLAY_UPLOAD_CERT_SHA256:-}" ]]; then
  expected_upload="$(tr -d ':' <<<"$PLAY_UPLOAD_CERT_SHA256" | tr 'A-F' 'a-f')"
  [[ "$aab_cert" == "$expected_upload" ]] || fail "AAB certificate $aab_cert does not match PLAY_UPLOAD_CERT_SHA256"
fi
unzip -l "$aab" "base/manifest/AndroidManifest.xml" >/dev/null || fail "AAB has no base manifest"
aab_vcs="$(unzip -p "$aab" 'BUNDLE-METADATA/*version-control-info.textproto' 2>/dev/null || true)"
if [[ -n "$aab_vcs" ]]; then
  grep -q "revision: \"$commit\"" <<<"$aab_vcs" || fail "AAB embeds a different commit"
fi
echo "AAB signed with upload certificate SHA-256 $aab_cert"

step "Writing checksums and GitHub release notes"
(
  cd "$out"
  shasum -a 256 "$(basename "$apk")" "$(basename "$aab")" > SHA256SUMS
)
changelog="$(git show "$commit:fastlane/metadata/android/en-US/changelogs/$version_code.txt" | sed 's/^• /- /')"
notes="$out/github-release-notes.md"
cat > "$notes" <<EOF
**Fun Phone** is a private Android dialer and personal hub (T9 dialer, contacts, notes, encrypted password vault, QR tools, expenses and mini games). No ads, no analytics, no developer cloud.

## Download

- **\`fun-phone-community-$version.apk\`**: the \`community\` build (\`$COMMUNITY_PACKAGE\`). It contains no Google Play Services, ML Kit, Firebase or Play Core libraries; QR codes are scanned with ZXing.
- It installs **next to** the Google Play version (different package), and the two do not share data. Use the in-app backup to move your data between them.
- Requires Android 10 (API 29) or newer.

**Automatic updates:** add \`https://github.com/$REPO_SLUG\` to [Obtainium](https://github.com/ImranR98/Obtainium).

**Verify the signature** (for example with AppVerifier or \`apksigner verify --print-certs\`):

\`\`\`
$COMMUNITY_CERT_SHA256
\`\`\`

This APK is reproducible: building tag \`$tag\` with F-Droid's tooling produces the same APK, byte for byte, once this signature is applied.

## What's new in $version

$changelog

Full details in [CHANGELOG.md](https://github.com/$REPO_SLUG/blob/$tag/CHANGELOG.md).
EOF

step "Done"
cat <<EOF
Artifacts in $out:
$(cd "$out" && ls -1)

Next manual steps:
1. Review $notes, then publish the GitHub release:
   gh release create $tag "$apk" -R $REPO_SLUG --verify-tag --title "Fun Phone $version" --notes-file "$notes"
2. Upload $aab to Play Console (Production > Create new release) and paste the notes from
   fastlane/metadata/android/*/changelogs/$version_code.txt. Upload mapping.txt and the native
   symbols from $out if Play asks for them.
3. F-Droid builds $tag on its own; nothing to upload there.
EOF
