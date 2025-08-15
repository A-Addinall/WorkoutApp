Awesome — here’s the **updated Change Plan** with the week/day/date model built in. I’ve kept it practical and sequenced so we can ship in small, testable steps and log each change against your tech\_map + screen\_contracts.

# Phase 0 — Foundations (no UX change)

**Goal:** Switch internal model to Week/Day/Date while keeping current UI working.

1. Schema & Migrations

* Add/modify tables:

    * `phase` (id, name, description, cycleLengthWeeks)
    * `week_day_plan` (id, phaseId, weekNumber, dayNumber, date nullable, isRestDay, title optional)
    * `day_item` (id, dayPlanId, itemType {STRENGTH|METCON}, sortOrder, refId (exerciseId|metconId), prescription JSON)
    * `workout_session` (id, dayPlanId, startedAt, completedAt, status)
* Keep existing entities, but mark old day-1..5 references as deprecated in tech\_map.
* Write destructive migration if safe during dev; convert to proper migrations before beta.

2. Seed Data v2

* Seed a **4–6 week Phase** (e.g., Base 4w) across weeks/days.
* Convert existing “Day1–Day5” content to `(weekNumber, dayNumber)` tuples.
* Keep “Edit Day” working: editing writes to `day_item`.

3. Repository Layer

* New queries:

    * `getPlanFor(phaseId, weekNumber, dayNumber)`
    * `getNextPlannedDay(after dayId)` (skips rest days)
    * `attachCalendar(planStartDate)` to backfill `date` values
* Back-compat adapters so current screens still read from the new repo.

✅ **Acceptance:** App boots, current screens load content via new repo; seeds exist for at least Week1–Week2.

---

# Phase 1 — Start/Edit Flow (UX tidy with new model)

**Goal:** Wire Start Workout and Edit Day to the same source of truth.

1. Start Workout

* “Start” consumes `week_day_plan` → spawns `workout_session`.
* On completion, calls `getNextPlannedDay` and suggests next day.

2. Edit Day

* Open library from the selected `(week, day)`.
* Writes to `day_item` (not filtered by “Day1..5” anymore).

3. Screen Contracts

* Update contracts to pass `(phaseId, weekNumber, dayNumber, dayPlanId)` everywhere.

✅ **Acceptance:** Start and Edit both operate on the same dayPlanId; completing a workout advances by plan order.

---

# Phase 2 — Strength: 1RM engine + rest timer

**Goal:** Intelligent suggestions and better set pacing.

1. Estimated 1RM Engine

* Add table `strength_result` (exerciseId, sessionId, setIndex, reps, weight, isSuccess).
* Implement E1RM with **Epley** for ≥3 reps and **Brzycki** for low reps; keep best of recent N (e.g., 6 sessions) per exercise:

    * Epley: `1RM = w * (1 + reps/30)`
    * Brzycki: `1RM = w * 36 / (37 - reps)`
* Persist rolling best E1RM with decay (last 90 days weighted).

2. Prescriptions & Suggestions

* For a set target like “5 reps @ 75%”, compute `suggestedWeight = roundToPlate( E1RM * 0.75 )`.
* If no history, default % from bodyweight/equipment or seed baseline.

3. Rest Timer

* Add **in-set rest** UI: countdown with 3 preset options (e.g., 60/90/120s) + custom.
* Auto-start timer on set complete; vibrate at 0; “start next set” CTA.

✅ **Acceptance:**

* Logging sets updates E1RM; next sets get updated suggested weight.
* Rest timer works per set with resume on app background/foreground.

---

# Phase 3 — Metcon logging & “for time/AMRAP/EMOM” parity

**Goal:** Add missing log screens so all metcon types are first-class.

1. Types supported

* FOR\_TIME (time capture + split laps)
* AMRAP (total reps + per-round detail optional)
* EMOM/INTERVAL (per-interval reps, failure flag)
* CHIPPER (ordered list progression, partial completion)

2. Data

* `metcon_result` (sessionId, metconId, type, payload JSON for flexible capture)
* Benchmarks flagged (`isBenchmark`) to appear in Performance.

✅ **Acceptance:** You can start any metcon, log it with the appropriate UI, and see the result in history.

---

# Phase 4 — Performance (Key lifts & Benchmarks)

**Goal:** Focused progress view.

1. Strength Progress

* Key exercises list (e.g., Back Squat, Bench, Deadlift, Press, Clean).
* Show latest E1RM, best E1RM (30/90D), and simple trend (↑/↓/→).

2. Benchmark Metcons

* Best time/score with date, last 3 attempts.
* Quick “Retest” entry point that jumps to the benchmark’s day or creates an ad‑hoc session.

✅ **Acceptance:** Performance shows at-a-glance trends and benchmark history.

---

# Phase 5 — Scheduling (date mode under the hood)

**Goal:** Keep UI as Week/Day; let users set a start date and we compute actual dates.

1. Attach Calendar

* “Start Phase” → choose a start date; compute dates across all `week_day_plan` (skip rest).
* Show “today’s workout” chip if today matches a dated day.

2. Missed/Shift logic (light)

* If a dated day passes incomplete, offer “shift remaining forward” or “keep dates”.

✅ **Acceptance:** Users can start a 4–6 week cycle with a real start date; app still shows Week X Day Y.

---

# Phase 6 — Plan Builder (Generate vs Choose)

**Goal:** Add automatic plan option without removing manual selection.

1. Plan Templates

* Phase templates: Base/Build/Peak with strength % progressions + metcon slots.
* Generator fills weeks/days using user profile (experience, equipment).

2. Manual Override

* Library still edits a day; generator writes initial content only.

✅ **Acceptance:** “Build plan for me” creates a full 4–6 week calendar; user can edit any day.

---

# Cross-cutting tasks

* Update **tech\_map.md** and **screen\_contracts.md** after each merged change (you own updates; I’ll provide clear diffs per step).
* Add **feature\_log.md** once we leave dev.
* Add unit tests for: 1RM engine, next-day progression, date attachment, and metcon serializers.
* Accessibility: larger tap targets, timer voice/vibration cues, color contrast.
* Telemetry (local only in dev): time per screen, DNF sessions (to find UX friction).

---

## Rollout Order (small PRs)

1. Phase 0 (schema + seed + repo)
2. Phase 1 (start/edit unified)
3. Phase 2 (1RM + rest timer)
4. Phase 3 (metcon UIs)
5. Phase 4 (performance)
6. Phase 5 (date scheduling)
7. Phase 6 (plan builder)

---

## Minimal acceptance checklist per PR

* Migrations apply cleanly on a fresh install.
* Updated contracts + notes in tech\_map.
* Feature toggled to avoid breaking current flows.
* Basic instrumentation log verifying critical path (start→log→complete).

---

Remove _Dev files -
Here’s a copy‑paste prompt you can give me later to prep the app for production.

---

# Release‑Prep Prompt (Phase 0 → Production)

You are working on **Workout App**. We are moving from **Phase 0 (Foundations)** to **production**. Perform the following, generating exact diffs/patches where applicable:

## 1) Remove dev seeding

* Ensure **no dev seed code ships**.
* If `DevPhaseSeed_dev.kt` exists, **assume it lives under `app/src/debug/...`** and confirm there is **no call** in `AppDatabase.onCreate` unless guarded by:

  ```kotlin
  if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      DevPhaseSeed_dev.seedFromLegacy(db)
  }
  ```
* If any call remains in `main`, remove it. Do **not** add any new dev seed.

## 2) Migrations (no destructive)

* In `AppDatabase.get(...)`, **remove** `fallbackToDestructiveMigration()` for release.
* Add Room **MIGRATION(s)** covering our current version bump that introduced:

  * `phase`
  * `week_day_plan ( + dateEpochDay )`
  * `day_item ( + prescriptionJson )`
* Provide a ready‑to‑paste `addMigrations(MIGRATION_X_Y, …)` snippet and SQL for each missing migration.

## 3) Room schema export (prod hygiene)

* In `build.gradle` (app), apply:

  * `id "androidx.room"`
  * `room { schemaDirectory "$projectDir/schemas" }`
* In `AppDatabase.kt`, set `exportSchema = true`.
* Confirm schemas generate on a clean build.

## 4) Keep legacy compatibility (no UI breakage)

* Verify repo methods that the current UI calls are **unchanged**:

  * `programForDay(day: Int)`
  * `metconsForDay(day: Int)`
  * `startSession(day: Int)`
* Ensure Phase‑0 additions are **additive**:

  * `getPlanFor(phaseId, week, day)`
  * `getNextPlannedDay(afterDayPlanId)`
  * `attachCalendar(phaseId, startEpochDay)`

## 5) Prod data integrity

* Confirm no dev data remains (e.g., “Dev Test Phase”).

  * Provide a one‑time cleanup snippet (guarded with `!BuildConfig.DEBUG`) to delete it if present.

## 6) Acceptance checklist (generate a test plan)

* Fresh install (release build): app boots, no dev seed runs, DB migrations apply cleanly.
* Upgrade path: from previous DB version → current version; data preserved; new tables exist.
* Week/Day plan APIs callable; legacy screens still show content.
* No crashes on `WorkoutActivity`, Library, Metcon screens.
* Logs/telemetry unaffected.

## 7) Output format

* Provide:

  1. **Patch‑style diffs** for each file changed (minimal, focused).
  2. Any **new code blocks** ready to paste (migrations, guards).
  3. A short **runbook** of manual steps (e.g., move `DevPhaseSeed_dev.kt` to `src/debug`).
  4. A **verification script/checklist** I can follow in 5–10 minutes.

Use file paths from our repo structure and keep everything strictly backward‑compatible.
