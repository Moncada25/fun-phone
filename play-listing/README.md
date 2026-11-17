# Google Play listing package

Reviewed source: [PLAY_STORE_LISTING.md](../PLAY_STORE_LISTING.md). Each locale folder contains
UTF-8 plain text ready to paste into Play Console: `title.txt`, `short-description.txt` and
`full-description.txt`. The manifest uses paths relative to the repository root.

| Language | Play locale / text | Eight phone screenshots and feature graphic |
| --- | --- | --- |
| English | [en-US](en-US/) | [en](../screenshots/store-v3/en/) |
| Español LATAM | [es-419](es-419/) | [es](../screenshots/store-v3/es/) |
| Deutsch | [de-DE](de-DE/) | [de](../screenshots/store-v3/de/) |
| Français | [fr-FR](fr-FR/) | [fr](../screenshots/store-v3/fr/) |
| Português do Brasil | [pt-BR](pt-BR/) | [pt](../screenshots/store-v3/pt/) |

Phone JPEGs are 1080×1920; each `feature-graphic.jpg` is 1024×500. All app screens use fictional
fixtures and localized native UI. Overview sheets are review aids; upload the eight numbered
images rather than the overview sheet. The listing text is a candidate for current app behavior;
the source document includes the current 3.20.5 release notes in all five languages.

Regenerate text with `python3 scripts/export-play-listing.py` from the repository root.
Run `./scripts/verify-play-listing.sh` to detect stale exports, missing locale assets or text over
Play's limits. Recreate graphics with `npm run render:store` from the sibling web checkout.
These commands make no Play Console requests and do not upload or publish a listing.
