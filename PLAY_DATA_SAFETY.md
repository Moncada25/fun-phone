# Google Play Data Safety — release worksheet

**Target:** Play 3.18.0 (code 244)
**Answer model:** no developer collection; two user-initiated third-party network flows to review

This worksheet supports the Play Console declaration; the submitted form must be checked against
the signed AAB and current Google Play definitions before rollout.

## Proposed answers

- Does the app collect or share required contacts, calls, messages, files, financial data or precise
  location? **No.**
- Is all transmitted data encrypted in transit? **Yes:** the optional holidays lookup and social/web
  preview use HTTPS.
- Can users request data deletion? **Yes, locally:** delete individual content, clear app data, or
  uninstall. The developer has no server-side user record to delete.
- Does the app follow the Families policy? **Not targeted to children; verify the target-audience
  declaration separately.**

## Important boundaries

- Android Sharesheet recipients, external browsers/apps, the configured Android speech provider,
  Android backup, and Google Play's native review flow act only after user/system action and under
  their own policies. Verify their treatment against the current Data Safety help text during form
  submission.
- Play In-App Review does not expose rating, review text, or completion status to Fun Phone. The app
  stores only local eligibility and cooldown state.
- After explicit in-app approval, the holidays tool sends the selected country and year to
  `date.nager.at`; the service necessarily observes IP address and request metadata. Before
  submitting Data Safety, confirm Nager.Date retention/processing and classify this flow under the
  current Google definitions. Do not rely on the previous blanket “no network traffic” answer.
- The user-requested social/web preview loads the selected HTTPS URL in an embedded WebView. The
  selected site may observe the URL, IP address and request metadata; classify this flow under the
  current Google definitions before submission.
- Before form publication, upload the exact 3.18.0 AAB and confirm production no longer serves a
  Firebase-enabled version to eligible devices. Do not rely on this worksheet for an older build.

## Release gate

```bash
./gradlew :app:processPlayReleaseMainManifest
./gradlew :app:dependencies --configuration playReleaseRuntimeClasspath
./scripts/verify-release-metadata.sh
```

The gate requires Internet/network-state permissions for the approved holidays and social/web preview
flows, rejects AD_ID, and fails if the runtime graph contains Firebase, Analytics, Auth, Storage, App
Check, Crashlytics, an advertising SDK, or an HTTP logging interceptor.
