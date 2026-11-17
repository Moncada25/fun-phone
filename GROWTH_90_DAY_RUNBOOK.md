# Fun Phone — 90-day organic growth runbook

**Start:** 2026-08-01

**Day 90:** 2026-10-30

**North star:** organic Play Store acquisitions

## Baseline and targets

| Metric (rolling 28 days) | Baseline | Day 30 | Day 90 |
|---|---:|---:|---:|
| Store listing visitors | 202 | 240 | 300+ |
| Install-click CTR | 20% | 25%+ | 28%+ |
| Listing acquisitions | 33–37 | 50+ | 75+ |
| Google Play Search share | Not measurable | Establish baseline | 15%+ |
| New ratings | 0 | 5+ | 20+ |
| Rating | 5.0 (24 ratings) | 4.6+ | 4.6+ |
| Known crashes in active release | 1 | 0 | 0 |

Compare complete, non-overlapping 28-day windows. Do not call a result from partial-day or
partial-window data.

## Release and listing gates

1. Keep 3.18.0 at 10% for 24–48 hours after approval. Check crash-free users, ANRs, incoming-call
   failures and negative reviews before moving to 50% and then 100%.
2. Do not submit the saved EN/ES listing changes while the 3.18.0 review is in progress; Play
   Console warned that doing so would restart the release review.
3. Once 3.18.0 is serving eligible users, submit the saved listing, then update Data Safety using
   `PLAY_DATA_SAFETY.md` only when the previous Firebase-enabled version is no longer served.
4. Upload the EN/ES preview videos to public or unlisted YouTube URLs and attach them to their
   listings. Keep them understandable without audio.
5. Create custom listings only after the public, licensed source repository exists:
   - Privacy/FOSS EN: GitHub, F-Droid and privacy communities.
   - T9/Personalization ES: LATAM Android communities and intent-led links.

## Weekly scorecard

Record this every Monday in a new row. Preserve both totals and deltas.

| Week ending | Release rollout | Visitors | Install clicks | CTR | Acquisitions | Explore % | Search % | First opens | Rating / count | Crashes / ANRs | Notes/actions |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---|
| 2026-08-02 | Review | — | — | — | — | — | — | — | — | — | Hotfix and listing prepared |

Classify review and support feedback into: calling, stability, accessibility, privacy and
personalization. Reply to actionable reviews without requesting a higher score.

## Acquisition attribution

Owned landing pages generate encoded Play referrers with:

- `utm_source=website`
- `utm_medium=community`
- `utm_campaign=funphone_oss_launch_2026q3`
- `utm_content=<locale>_<placement>`

Use distinct `utm_source` values for external distribution (`github`, `fdroid`, `reddit_androidapps`,
`reddit_fossdroid`, and the specific LATAM community). Do not reuse one URL across channels.

## Review prompt rules

The in-app prompt is eligible only after Fun Phone is the default dialer and the user completes
three connected calls on three distinct days. It is requested after the call screen closes, stores
the requested version/date locally and applies a 180-day cooldown. There is no sentiment gate,
incentive or pre-review survey.

## Experiment threshold

Do not start a Play Store listing experiment below 500 visitors per rolling 28 days. Above that
threshold, run one variable at a time in this order:

1. Existing icon versus the new multicolor dialer icon.
2. Dialer-first screenshot block versus privacy-first screenshot block.
3. Privacy-first short description versus customization-first short description.

Pre-register the primary metric, minimum sample, start date and stop rule before launching each
experiment.
