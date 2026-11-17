# Public-source release checklist

This checklist reflects the 3.18.0 privacy release. It deliberately does not
publish the current Git history: the existing history contains a Firebase key
and a signing keystore and must be treated as compromised until those
credentials have been rotated.

## Current status

| Check | Status | Required action |
|---|---|---|
| GPL-3.0 license and community files | Ready | None |
| Play flavor contains Firebase | Ready | Not present in 3.18.0 |
| Network permission and scope | Ready | Required for the opt-in Nager.Date holidays tool and the user-requested HTTPS social/web preview; verify no other direct network path |
| Reproducible build instructions | Ready | See `SETUP.md` |
| Privacy and data-flow documentation | Ready | See `PRIVACY.md`, `PRIVACY_DATA_FLOW.md`, and `TELEMETRY.md` |
| Automated privacy-boundary verification | Ready | Run `scripts/verify-zero-collection.sh` |
| Secret-free Git history | Blocked | Rotate credentials and publish a history-free snapshot |
| Public Git repository | Blocked | The configured URL is not publicly reachable |
| Public release | Blocked | Complete the two items above first |

## Safe publication procedure

1. Rotate the Firebase key that appears in the old history, even though Firebase
   is no longer part of the Play build.
2. Rotate the Android upload/signing credential found in the old history. Follow
   the Play App Signing recovery process if the exposed file is an upload key.
3. Run the release checks:

   ```bash
   ./gradlew testPlayDebugUnitTest lintPlayRelease bundlePlayRelease
   scripts/verify-release-metadata.sh
   scripts/verify-zero-collection.sh
   scripts/verify-play-listing.sh
   ```

4. Create a new, empty public repository. Export the reviewed working tree into
   that repository without copying `.git`, local properties, signing material,
   build outputs, or emulator data.
5. Commit the clean snapshot as the initial public release, enable secret
   scanning and push protection, and protect `main` before inviting contributors.
6. Tag the exact source used for the public binary and attach checksums and build
   instructions to the release.
7. Only after the repository and license are publicly reachable, use the terms
   “open source” or “FOSS” in store listings and community announcements.

## Do not do this

- Do not make the current repository public.
- Do not force-push a rewritten history until every credential has been rotated.
- Do not attach a private signing key, `local.properties`, or generated secrets
  to a GitHub release.
- Do not claim reproducibility until an independent clean checkout produces the
  same artifact or the documented reproducibility limitations are disclosed.

## Post-publication configuration

- Description: `Private, customizable and accessible Android dialer. GPL-3.0.`
- Website: `https://moncada25.github.io/fun-phone-web/en/`
- Topics: `android`, `kotlin`, `jetpack-compose`, `dialer`, `contacts`,
  `accessibility`, `privacy`, `t9`, `gpl-3`
- Enable dependency alerts, secret scanning, push protection, protected branches,
  required CI checks, issue templates and security reporting.
