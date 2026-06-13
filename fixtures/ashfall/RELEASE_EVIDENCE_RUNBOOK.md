# Ashfall Release Evidence Runbook

This runbook records the remaining non-synthetic evidence needed by the Release
Index readiness gate. Do not set booleans to `true` or clear screenshot gaps
until the referenced files exist and prove the claim.

## Phase 7 Native Public Beta

1. Build the release candidate package and record its SHA-256.
2. Copy `fixtures/ashfall/native-public-beta/manual-evidence.template.json` to
   `fixtures/ashfall/native-public-beta/manual-evidence.json`.
3. Replace every placeholder with real data from three clean internal Native
   Loader sessions.
4. Each session must cite a real `latest.log`, tester notes, support bundle,
   current start/end timestamps, a positive duration, the release candidate
   build id, the tested artifact SHA-256, and no-crash evidence.
5. Add crash review notes proving no crash signal in the latest log and zero
   crash reports, with a current `reviewedAt` timestamp.
6. Add the tester package, support runbook, rollback plan, and known
   limitations documents under `fixtures/ashfall/native-public-beta/evidence/`.
7. Run `node scripts/generate-ashfall-native-public-beta-evidence.mjs`.

## Phase 8 Gameplay QA

1. Ensure upstream Native tester playability, Minecraft baseline playability,
   and crash intake reports are PASS or PASS_WITH_WARNINGS and not dry-run.
2. Copy `fixtures/ashfall/gameplay-qa/manual-evidence.template.json` to
   `fixtures/ashfall/gameplay-qa/manual-evidence.json`.
3. Add real evidence for first-hour client play, fresh world creation,
   save/reload, route verification, dedicated server, server/client export,
   ending verification, and no-crash review.
4. Include at least four supporting notes, two screenshots, two server/export
   logs, and two save snapshots. The screenshots must include first-launch or
   first-hour proof plus server/client export proof; the logs must include
   dedicated-server and client-export proof; the save snapshots must include
   fresh-world and reloaded-world proof.
5. Run `node scripts/generate-ashfall-gameplay-qa-evidence.mjs`.

## Phase 9 Player-Facing Polish

Add real first-launch and server/client export screenshots to the Ashfall Native
Edition metadata assets, update `metadata/official_packs/ashfall.json`, and
clear `screenshotsNeeded` only after those files are present and accurate.

## Phase 10 Release Candidate

The GitHub release for `v0.1.0-ashfall-native-edition` must be a draft with the
release manifest, pack manifest, checksums, and `ashfall-native-edition-*.zip`
assets attached. Then run the draft download smoke and the Release Index
readiness verifier. Keep JEI out of the default Ashfall release path.
