# Releasing Fun Phone

Every release ships on three channels from **one tagged commit on `main`**:

| Channel | Artifact | Package | Signed with | Who publishes |
|---|---|---|---|---|
| Google Play | `fun-phone-play-X.Y.Z.aab` (`bundlePlayRelease`) | `com.bookverse.contacts` | Play upload key (`RELEASE_*`) | Maintainer, in Play Console |
| GitHub Releases / Obtainium | `fun-phone-community-X.Y.Z.apk` (`assembleCommunityRelease`) | `com.bookverse.contacts.community` | Community key (`COMMUNITY_RELEASE_*`) | Maintainer, with `gh release create` |
| F-Droid | Built by F-Droid from the tag | `com.bookverse.contacts.community` | Same community signature, copied from GitHub | Automatic |

F-Droid's recipe (`Binaries` + `AllowedAPKSigningKeys`) rebuilds the tag, downloads the GitHub APK and
publishes it only if both are identical apart from the signature. So the GitHub APK **must** be built
from a clean checkout of the exact tagged commit, after the squash merge, never from a feature branch
or a dirty tree. `scripts/release.sh` does that for you.

Community certificate SHA-256:

```
caf15843b5efc692ff6dec15e989d2fb32946dfc089453dfc6d31edf242b7151
```

## Prerequisites

- JDK 25, the Android SDK (`sdk.dir` in `local.properties` or `ANDROID_HOME`) and `gh` logged in as
  the maintainer account.
- Signing configuration kept **outside Git**, in `gradle.properties.private` (ignored) or in
  environment variables:
  - `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`:
    the Play upload key.
  - `COMMUNITY_RELEASE_STORE_FILE`, `COMMUNITY_RELEASE_STORE_PASSWORD`,
    `COMMUNITY_RELEASE_KEY_ALIAS`, `COMMUNITY_RELEASE_KEY_PASSWORD`: the community key.
  - Use absolute keystore paths; the release script builds in a temporary clone.
- Never commit keystores, passwords or the private properties file, and never paste them in issues,
  PRs or logs.

## 1. Prepare the release PR

On a branch from an up-to-date `main` (for example `release/X.Y.Z`):

1. **Release notes.** In `PLAY_STORE_LISTING.md`, replace the `## What's new — <previous>` section with
   `## What's new — X.Y.Z` and write the notes in all five languages (`### English`, `### Español`,
   `### Deutsch`, `### Français`, `### Português`). Each language must fit in 500 bytes. Update the
   `**Release notes candidate below:**` line at the top.
2. **Version.** In `release.properties`, set `VERSION_NAME=X.Y.Z` and increase `VERSION_CODE` by one.
3. **Export the listing.** Run:

   ```bash
   python3 scripts/export-play-listing.py
   ./scripts/verify-play-listing.sh
   ```

   This writes `fastlane/metadata/android/<locale>/changelogs/<VERSION_CODE>.txt` (read by F-Droid)
   and the Play text in `play-listing/`. Commit the generated files.
4. **Changelog.** In `CHANGELOG.md`, move the `[Unreleased]` entries into a new
   `## [X.Y.Z] - YYYY-MM-DD` section and leave `## [Unreleased]` empty.
5. **Checks.** Run the usual local checks (`./gradlew ktlintCheck`, `./scripts/verify-localization.sh`,
   and the build/test/lint commands from [CONTRIBUTING.md](CONTRIBUTING.md) when code changed).
6. Open the PR, wait for the required `validate` and `gitleaks` checks, and **squash merge** it.
   `main` is protected: there are no direct pushes.

## 2. Tag the merged commit

Tag the squash commit that the merge created on `main`, not a branch commit:

```bash
git switch main
git pull --ff-only
git log -1 --oneline            # must be the release PR's squash commit
git tag -a vX.Y.Z -m "Fun Phone X.Y.Z"
git push origin vX.Y.Z
```

Once the tag is pushed, F-Droid will build it. Do not move or recreate a published tag. If something
is wrong, fix it in a new patch release.

## 3. Build and verify both artifacts

```bash
./scripts/release.sh vX.Y.Z
```

The script never pushes, tags or uploads anything. It:

1. checks that the tag exists on `origin`, points to a commit on `origin/main`, and that
   `release.properties`, `CHANGELOG.md` and the fastlane changelog at that commit match the version;
2. clones that commit into a temporary directory (clean tree, fresh build directories) and copies
   only the ignored signing/SDK files into it;
3. runs `validateCommunityReleaseSigning`, `assembleCommunityRelease` and `preparePlayStoreArtifacts`
   (which runs `bundlePlayRelease` and collects the mapping file and native symbols);
4. verifies the APK: APK Signature Scheme v2, the community certificate above, package, `versionCode`
   and `versionName`, and that `META-INF/version-control-info.textproto` embeds the tagged commit;
5. verifies the AAB signature and prints its upload certificate. Set `PLAY_UPLOAD_CERT_SHA256` to
   make the script fail if the AAB is signed with a different upload key;
6. writes everything to `build/release/vX.Y.Z/`: the APK, the AAB, `mapping.txt`, native symbols,
   `SHA256SUMS` and `github-release-notes.md`, and prints the next steps.

To check the embedded commit yourself:

```bash
unzip -p build/release/vX.Y.Z/fun-phone-community-X.Y.Z.apk META-INF/version-control-info.textproto
apksigner verify --print-certs build/release/vX.Y.Z/fun-phone-community-X.Y.Z.apk
```

## 4. Publish

1. **GitHub.** Review `build/release/vX.Y.Z/github-release-notes.md`, then:

   ```bash
   gh release create vX.Y.Z build/release/vX.Y.Z/fun-phone-community-X.Y.Z.apk \
     -R Moncada25/fun-phone --verify-tag --title "Fun Phone X.Y.Z" \
     --notes-file build/release/vX.Y.Z/github-release-notes.md
   ```

   The asset name must stay `fun-phone-community-X.Y.Z.apk`: F-Droid's `Binaries` URL and
   Obtainium depend on it.
2. **Google Play.** In Play Console, create a release, upload
   `build/release/vX.Y.Z/fun-phone-play-X.Y.Z.aab` and paste the notes from
   `fastlane/metadata/android/<locale>/changelogs/<VERSION_CODE>.txt`. Upload `mapping.txt` and the
   native symbols if Play Console asks for them.
3. **F-Droid.** Nothing to do. Its build server picks up the new tag and publishes the GitHub
   signature once its own build matches.

## If something goes wrong

- **The script says the tag is not on `origin/main`:** you tagged a branch commit. Delete the tag
  (only if it was never published), then tag the squash commit.
- **The APK certificate does not match:** `COMMUNITY_RELEASE_*` is missing or points to the wrong
  keystore. Without it, Gradle builds an unsigned APK (that is what F-Droid builds).
- **The embedded commit does not match:** the APK was not built from the tag. Run the script again
  instead of building by hand.
- **F-Droid reports a mismatch:** check that the GitHub asset is the one the script built from the
  tag, and compare it with `diffoscope` against F-Droid's unsigned build.
