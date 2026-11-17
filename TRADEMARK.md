# Política de Marca / Trademark Policy

## Español

### Resumen

- El código de este repositorio se licencia bajo la **GPL-3.0-or-later** (ver [LICENSE](LICENSE)).
  Cualquiera puede usarlo, modificarlo, redistribuirlo e incluso venderlo, siempre que cumpla la GPL.
- La GPL es una licencia de derechos de autor y **no concede derechos sobre el nombre ni el
  logotipo** del proyecto. La propia GPL v3 permite reservarlos (sección 7(e)).
- "**Fun Phone**", su logotipo e ícono y el nombre de desarrollador "**Bookverse**" identifican el
  proyecto oficial. Se usan públicamente desde abril de 2025 en Google Play. Esta política no afirma
  que exista un registro formal de marca; Bookverse se reserva los derechos que le reconozca la ley.

### Signos y archivos cubiertos

- El nombre "Fun Phone" y nombres que puedan confundirse con él (por ejemplo "FunPhone",
  "Fun Phone Pro" o "Fun Phone+").
- El logotipo y el ícono de la app, en cualquier tamaño, color o formato.
- Los archivos de marca de este repositorio:
  - `app/src/main/ic_launcher-playstore.png`
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png` e `ic_launcher_foreground_adaptive.png`
  - `fastlane/metadata/android/*/images/icon.png` y `featureGraphic.jpg`
- El material de ficha de tienda, que muestra la marca oficial: `screenshots/`,
  `fastlane/metadata/android/*/images/phoneScreenshots/`, los textos de `fastlane/metadata/` y
  `play-listing/`.

### Uso permitido sin pedir autorización

- Redistribuir **sin modificar** las versiones oficiales o builds compilados desde el código oficial
  sin cambios (por ejemplo F-Droid, IzzyOnDroid, Obtainium o mirrors), con el nombre y el ícono
  originales. Firmar con otra clave o aplicar los ajustes mínimos que exija la tienda para publicar,
  sin cambiar funciones, no cuenta como modificación.
- Mencionar "Fun Phone" para referirse al proyecto en artículos, reseñas, tutoriales o comparativas.
- Decir que un fork está "basado en Fun Phone" o es un "fork de Fun Phone", siempre que el fork
  tenga otro nombre y otro ícono.
- Enlazar al repositorio y a la ficha oficial de Google Play.

### Uso NO permitido sin autorización escrita de Bookverse

- Distribuir una versión modificada con el nombre "Fun Phone", un nombre confundible, o con el
  logotipo o el ícono oficiales.
- Crear fichas de tienda, cuentas de desarrollador, dominios o cuentas de redes sociales con
  "Fun Phone" o el ícono para distribuir o promocionar versiones no oficiales.
- Sugerir que una versión, producto o servicio es oficial o está respaldado por Bookverse.
- Publicar apps con identificadores de paquete bajo `com.bookverse` (incluidos
  `com.bookverse.contacts` y `com.bookverse.contacts.community`).

### Si distribuyes un fork

Debes:

1. Cambiar el nombre visible de la app: `app_name` en `app/src/main/res/values/strings.xml` y las
   demás menciones de "Fun Phone" en `app/src/main/res/values*/strings.xml`.
2. Reemplazar el logotipo y los íconos listados arriba.
3. Cambiar el `applicationId` en `app/build.gradle.kts` (sabores `community` y `play`) por uno que
   no empiece por `com.bookverse`.
4. Reemplazar los textos y capturas de ficha de `fastlane/`, `play-listing/` y `screenshots/`.
5. Indicar de forma visible que tu versión es un fork independiente, no afiliado a Bookverse.
6. Cumplir la GPL-3.0-or-later: publicar el código fuente correspondiente y conservar los avisos de
   copyright y de licencia.

Puedes conservar una atribución como "Basado en Fun Phone, © Bookverse, GPL-3.0-or-later" en la
pantalla "Acerca de" y en tu README.

### Licencia de los assets gráficos

Todos los archivos de este repositorio, incluidos íconos, avatares, ilustraciones de juegos y
material de ficha, se distribuyen bajo la GPL-3.0-or-later, salvo que un archivo indique otra cosa.

Esa licencia de derechos de autor no permite usar el nombre "Fun Phone" ni el logotipo e ícono para
identificar otra app o servicio: ese uso lo regula esta política de marca. En la práctica, un fork
puede reutilizar los avatares y las ilustraciones de juegos, pero debe reemplazar los archivos de
marca y el material de ficha.

---

## English

### Summary

- The code in this repository is licensed under **GPL-3.0-or-later** (see [LICENSE](LICENSE)).
  Anyone may use, modify, redistribute and even sell it, as long as they comply with the GPL.
- The GPL is a copyright license and **grants no rights to the project's name or logo**. GPL v3
  itself allows reserving them (section 7(e)).
- "**Fun Phone**", its logo and icon, and the developer name "**Bookverse**" identify the official
  project. They have been used publicly on Google Play since April 2025. This policy does not claim
  that a formal trademark registration exists; Bookverse reserves the rights the law grants it.

### Covered marks and files

- The name "Fun Phone" and names likely to be confused with it (for example "FunPhone",
  "Fun Phone Pro" or "Fun Phone+").
- The app logo and icon, in any size, color or format.
- The brand files in this repository:
  - `app/src/main/ic_launcher-playstore.png`
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png` and `ic_launcher_foreground_adaptive.png`
  - `fastlane/metadata/android/*/images/icon.png` and `featureGraphic.jpg`
- Store listing material, which shows the official brand: `screenshots/`,
  `fastlane/metadata/android/*/images/phoneScreenshots/`, and the texts in `fastlane/metadata/` and
  `play-listing/`.

### Permitted without asking

- Redistributing **unmodified** official releases, or builds made from the unmodified official
  source (for example F-Droid, IzzyOnDroid, Obtainium or mirrors), under the original name and
  icon. Signing with a different key, or the minimal changes a store requires to publish without
  changing features, does not count as a modification.
- Using "Fun Phone" to refer to the project in articles, reviews, tutorials or comparisons.
- Saying that a fork is "based on Fun Phone" or a "fork of Fun Phone", as long as the fork has its
  own name and icon.
- Linking to the repository and to the official Google Play listing.

### NOT permitted without written permission from Bookverse

- Distributing a modified version under the "Fun Phone" name, a confusingly similar name, or the
  official logo or icon.
- Creating store listings, developer accounts, domains or social media accounts that use
  "Fun Phone" or the icon to distribute or promote unofficial versions.
- Suggesting that a version, product or service is official or endorsed by Bookverse.
- Publishing apps with package names under `com.bookverse` (including `com.bookverse.contacts` and
  `com.bookverse.contacts.community`).

### If you distribute a fork

You must:

1. Change the visible app name: `app_name` in `app/src/main/res/values/strings.xml` and the other
   mentions of "Fun Phone" in `app/src/main/res/values*/strings.xml`.
2. Replace the logo and icons listed above.
3. Change the `applicationId` in `app/build.gradle.kts` (`community` and `play` flavors) to one that
   does not start with `com.bookverse`.
4. Replace the store listing texts and screenshots in `fastlane/`, `play-listing/` and
   `screenshots/`.
5. State visibly that your version is an independent fork, not affiliated with Bookverse.
6. Comply with GPL-3.0-or-later: publish the corresponding source code and keep the copyright and
   license notices.

You may keep an attribution such as "Based on Fun Phone, © Bookverse, GPL-3.0-or-later" on the
About screen and in your README.

### Graphic asset license

All files in this repository, including icons, avatars, game illustrations and store listing
material, are distributed under GPL-3.0-or-later unless a file states otherwise.

That copyright license does not allow using the "Fun Phone" name or the logo and icon to identify
another app or service: that use is governed by this trademark policy. In practice, a fork may
reuse the avatars and game illustrations, but must replace the brand files and the store listing
material.

---

## Contacto / Contact

Para solicitudes de uso de marca: **santiago.moncada.dev@gmail.com**
For trademark usage requests: **santiago.moncada.dev@gmail.com**
