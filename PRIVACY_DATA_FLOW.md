# Privacy and data-flow audit

**Audited target:** working tree based on 3.19.6 (code 259); updated 2026-11-03, not a Play publication claim
**Model:** local-first dialer with two user-initiated third-party network flows

## Distribution boundary

| Control | Community | Play |
|---|---:|---:|
| Internet permission | Yes | Yes |
| Firebase / Google Services | No | No |
| Analytics / crash reporting / ads | No | No |
| Cloud photo upload | No | No |
| Play In-App Review client | No | Yes |

## Data flows

| ID | Trigger | Data and destination | Retention / control |
|---|---|---|---|
| LOCAL-001 | Contacts and dialer use | Contacts, call log and role state are read through Android providers on-device | Controlled by Android permissions and source providers |
| LOCAL-002 | Notes, passwords, settings, reminders and favorites | Stored only in app databases/preferences on-device | Clear app data, delete content, or uninstall |
| LOCAL-003 | QR/barcode scan and generation | Camera frames and generated content are processed locally | Temporary frames are not stored unless the user saves/shares output |
| LOCAL-004 | User assigns an incoming-call background to a contact | The original image is copied to app-private storage and displayed only when that contact calls | Replace/remove it from the contact form, delete the contact, clear app data, or uninstall; it is excluded from contact exports and cloud backup |
| LOCAL-005 | Modern chrome draws a translucent bar | Haze processes the visible background in transient on-device graphics layers | No disk output or network flow; layers leave composition with the screen. Notes, vault, expenses, cycle, calls, camera, voice and games use solid surfaces instead |
| SHARE-001 | User shares a contact/photo/export | A local `content://` URI is granted to the target selected in Android Sharesheet | Target app receives the selected file under its own policy |
| EXTERNAL-001 | User opens a web/social/WhatsApp link | Android hands the URL/phone number to the selected external app | Fun Phone makes no direct web request |
| SYSTEM-001 | User activates speech recognition or synthesis | Android sends audio/text to the speech provider configured by the user | Provider policy applies; Fun Phone keeps only the returned text or generated local file |
| SYSTEM-002 | User connects and selects calendars to view events | Calendar data is read from Android Calendar Provider; synchronization remains handled by the account provider configured on the device | Controlled by READ_CALENDAR and the selected calendars; Fun Phone makes no direct calendar network request and never copies contact birthdays into calendar events |
| PLAY-001 | Eligible user reaches the post-call review moment | Google Play may display its native review card | Fun Phone stores only local date/version of the request; it cannot see rating, text, or completion |
| TRANSFER-001 | User starts local device transfer | Data is exchanged only through Android's user-authorized nearby/system transport | Explicit action; receiving/sending device controls the session |
| NETWORK-001 | User accepts the disclosure and opens/refreshes Public Holidays | HTTPS request to `date.nager.at` with selected country/year; the service necessarily receives IP and request metadata | In-memory response cache for 7–30 days; Nager.Date policy controls server-side handling |
| NETWORK-002 | User opens the social preview, selects a profile, or enters an HTTPS website | HTTPS URL is loaded in the embedded WebView; the selected site receives the URL, IP, cookies and request metadata under its own policy | User-triggered only; no contacts, calls or app database content is added to the request |

## Retired network paths

The 3.18.0 build retains the removal of Firebase Auth, Storage, Analytics and App Check, and never
fetches hosted contact-photo fields. Website and social URLs are prepared locally; the user-requested
preview is the explicit WebView network exception, while the public-holidays lookup remains isolated
to `date.nager.at` after its disclosure. Legacy remote-photo fields can still be parsed for
compatibility but are never fetched.

## Acceptance evidence

- Inspect `processPlayReleaseMainManifest` / merged manifest for required Internet/network-state
  permissions and absence of AD_ID.
- Inspect `playReleaseRuntimeClasspath` for absence of Firebase and ad/analytics artifacts.
- Run network inspection on Android 10, 14 and 16; verify that only the accepted holidays flow
  reaches `date.nager.at` and that social/web traffic occurs only after the user selects the preview.
- Re-audit on any permission, dependency, speech-provider, review-library, sharing, or backup change.

## Protected-session boundary (upgrade work package)

- Authentication unlocks a session; background/screen off locks it immediately. Vault observers
  and decrypted lists stop at lock, and row decryption runs off the main thread.
- PIN verifiers are versioned PBKDF2-HMAC-SHA256 with 600,000 iterations and random salt; legacy
  SHA-256 verifiers migrate only after a successful PIN. Five failures impose a durable 30s wait.
- Password and protected-note editor drafts put ciphertext, not clear text, in saved state.
  Protected windows prevent capture/recents exposure. Password copies are marked sensitive;
  cleanup after 30s is best effort while the process is alive and Android permits clipboard access.
- Modern Notes, Password list and Expenses chrome renders a transient, local GPU backdrop of
  visible content. Authenticated content stays inside `ProtectedScreenHost` and its secure window;
  locking removes that content and its optical layer. Backgrounding drops the backdrop state.
  This render layer is not saved, exported, logged or transmitted; protected editors keep the
  solid fallback.
- Room stays at 30; encryption keys, vault ciphertext and backup formats are unchanged.
- Returning from the vault's document picker requires a new authenticated session before its URI
  is processed. Pending picker state contains metadata only; a locked vault cannot export an empty
  list as if it were a valid backup. A subsequent lock cancels the active transfer.
