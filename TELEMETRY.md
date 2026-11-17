# Telemetry policy

Fun Phone 3.18.0+ sends no analytics or telemetry in either Play or Community builds.

The code keeps a small `Telemetry` interface so product events remain decoupled from UI code, but
both release containers bind it to `NoOpTelemetry`. No event, identifier, contact value, phone
number, call metadata, free-form text, or device information leaves the app.

Release verification:

1. Confirm the merged release manifest contains `INTERNET` and `ACCESS_NETWORK_STATE` for the
   disclosed Nager.Date holiday lookup and the user-requested HTTPS social/web preview.
2. Confirm the Play runtime dependency graph has no Firebase, Crashlytics, Analytics, Auth,
   Storage, App Check, Google Services, or advertising SDK.
3. Exercise onboarding, calls, contact sharing, QR generation, settings, review eligibility, and
   social/web preview while inspecting traffic; only the holidays and user-requested preview flows
   may access the network.
4. Treat any additional host, telemetry sink or background network access as a privacy-model change
   requiring a new review of this file, `PRIVACY.md`, and Play Data Safety before release.
