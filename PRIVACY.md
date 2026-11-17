# Fun Phone Privacy Policy

**Effective date:** 2026-11-03
**Applies to:** Fun Phone Play and Community distributions, version 3.18.0 and later

## Español

Fun Phone es un marcador Android privado. La app no contiene Firebase, publicidad, analítica,
telemetría, autenticación remota ni almacenamiento en la nube. Fun Phone no recopila, vende ni
comparte contactos, llamadas ni contenido personal con el desarrollador.

Los contactos, favoritos, historial de llamadas, configuraciones, notas, contraseñas y demás
contenido permanecen en el dispositivo y se procesan localmente. Los fondos personalizados de
llamadas también se guardan en el almacenamiento privado local y no se incluyen en las exportaciones
de contactos. Las fotos se comparten como archivos locales `content://` mediante Android Sharesheet;
los JSON y códigos QR nunca publican una foto en un servidor.

Fun Phone solicita únicamente permisos relacionados con las funciones que activa el usuario:
contactos, teléfono, registro de llamadas, cámara, micrófono, calendario, notificaciones y alarmas.
El backup cloud está desactivado mediante las reglas incluidas. Se conserva la transferencia local
entre dispositivos autorizada por Android. El vault se bloquea al salir o apagar la pantalla; los
borradores del vault y de notas protegidas usan cifrado y se restauran solo tras autenticar.

La herramienta opcional de festivos solicita autorización dentro de la app antes de usar la red.
Al aceptarla, Fun Phone realiza una solicitud HTTPS directa a `date.nager.at` con el país y año
seleccionados. No incluye contactos, llamadas ni eventos del calendario. Como en cualquier conexión
web, el servicio recibe necesariamente la dirección IP y metadatos técnicos de la solicitud y los
trata según su propia política. La consulta solo ocurre al abrir o actualizar esta herramienta.

La vista previa social/web es una excepción de red iniciada por el usuario: al abrir el preview, elegir una red social
o introducir una URL HTTPS, Fun Phone carga esa dirección en un WebView integrado. El sitio elegido
puede recibir la URL, la dirección IP, cookies y metadatos técnicos según su propia política. Fun Phone no
envía contactos, llamadas ni otros datos de la app a esa vista.

Otras acciones salen de Fun Phone: abrir un enlace o perfil social fuera de la vista previa entrega
la URL a la aplicación que elija el usuario; reconocimiento y síntesis de voz son prestados por el
proveedor configurado en Android; y Play In-App Review es mostrado por Google Play. Esos proveedores
procesan la acción bajo sus propias políticas. Fun Phone no recibe el audio, la reseña ni el resultado
de la reseña.

El usuario puede borrar datos desde Ajustes de Android, desinstalar la app, eliminar contactos o
revocar permisos en cualquier momento. Para preguntas de privacidad, abre un issue en el
repositorio público del proyecto una vez disponible o usa el canal de soporte indicado en Google
Play.

## English

Fun Phone is a private Android dialer. The app contains no Firebase, ads, analytics, telemetry,
remote authentication, or cloud storage. Fun Phone does not collect, sell, or share contacts, calls,
or personal content with the developer.

Contacts, favorites, call history, settings, notes, passwords, and other content remain on the
device and are processed locally. Custom incoming-call backgrounds also remain in private local
storage and are not included in contact exports. Photos are shared as local `content://` files
through Android Sharesheet; JSON and QR exports never publish a photo to a server.

Fun Phone only requests permissions tied to features the user enables: contacts, phone, call log,
camera, microphone, calendar, notifications, and alarms. Cloud backup is disabled by the app rules; Android-authorized local device transfer is retained.
Protected surfaces lock on background/screen off. Vault and protected-note drafts use encryption
and are restored only after authentication.

The optional public-holidays tool requests in-app approval before it uses the network. After
approval, Fun Phone sends a direct HTTPS request to `date.nager.at` containing the selected country
and year. It never includes contacts, calls, or calendar entries. As with any web connection, the
service necessarily receives the IP address and technical request metadata and processes them under
its own policy. Requests occur only while opening or refreshing this tool.

The social/web preview is a user-initiated network exception: after the user opens the preview, chooses a social network
or enters an HTTPS URL, Fun Phone loads that address in an embedded WebView. The selected site may
receive the URL, IP address, cookies, and technical request metadata under its own policy. Fun Phone does not
send contacts, calls, or other app data to the preview.

Some other user actions leave Fun Phone: opening a link or social profile outside the preview hands
the URL to the app chosen by the user; speech recognition and synthesis are provided by the provider
configured in Android; and Google Play renders the In-App Review flow. Those providers process the
action under their own policies. Fun Phone does not receive the audio, the review, or whether a review
was submitted.

Users can clear app data in Android Settings, uninstall the app, delete contacts, or revoke
permissions at any time. For privacy questions, open an issue in the public project repository once
available or use the support channel shown on Google Play.

## Verifiable implementation

- Both distributions declare `INTERNET` and `ACCESS_NETWORK_STATE` for the disclosed, opt-in
  Nager.Date holiday lookup and the user-requested HTTPS social/web preview.
- Both distributions use local/no-op adapters for telemetry, integrity, and photo sharing.
- The Play distribution contains only Google Play's In-App Review client; it contains no Firebase
  runtime or Google Services configuration.
- The detailed audit and release checks live in `PRIVACY_DATA_FLOW.md` and
  `PLAY_DATA_SAFETY.md`.
