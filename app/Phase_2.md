Absolutely—here’s a phased roadmap that lets us ship step‑by‑step, verify the UI after each step, and end with ML + a selection UI. Every phase is **additive** (no breaking renames), and we keep your existing queries/UI paths working (e.g., your strength day list uses `flowDayStrengthFor` joining `day_item → exercise` and we won’t touch that path until we’re ready to cut over) .

---

# Phase 1 — Strength foundations (additive, no UI change)

**Goal:** Give strength days structure (sets, rep ranges, intensity) without touching the current day list UI.

**DB/entities (new):**

* `WorkoutBlock(id, dayPlanId, blockType, orderInDay, rounds?, intervalSec?, timeCapSec?, restBetweenExercisesSec?)`
* `BlockExercise(id, blockId, exerciseId, orderInBlock, targetSets, repsMin, repsMax, intensityType, rpeTarget?, percent1Rm?, loadKg?, tempo?, restSec?)`

**Keep current UI working:** Continue reading strength items via `flowDayStrengthFor(...)` (which selects `e.*, di.required, di.targetReps, di.sortOrder` from `day_item`) while we start **writing** new sessions to `WorkoutBlock/BlockExercise` in parallel.&#x20;

**Acceptance checks:**

* App builds; existing day screens render unchanged.
* New tables exist; a small “dev only” command populates one day with a block + two exercises.
* Library filters still work (they currently filter by `WorkoutType` and `primaryEquipment`, and we didn’t touch them).&#x20;

---

# Phase 2 — Metcon structure (still no UI change)

**Goal:** Make metcon components queryable (push/pull focus, equipment) while preserving current displays.

**What you already have:**

* Metcon plan/selection relations used by the UI (`PlanWithComponents`, `getMetconsForDay`)—we keep these intact. &#x20;
* CRUD for metcon plans/components/logs.&#x20;

**DB/entity changes (add fields):**

* In `MetconComponent`: add `blockType`, `rounds?`, `durationSec?`, `emomIntervalSec?`, `movement?`, `primaryMuscles`, `secondaryMuscles`, `equipment`, `reps?`, and an intensity pair (`intensityType`, `value`) while **keeping `text`** as a render hint (UI keeps reading it).
* Optional junctions: `metcon_component_equipment`, `metcon_component_muscle`.

**Acceptance checks:**

* Existing metcon list/detail still render (same DAO methods).
* We can now filter components by focus/equipment on the backend (even if UI doesn’t use it yet).

---

# Phase 3 — Real dates (and “show 5 upcoming days”)

**Goal:** Move from “Day 1/2/3…” to calendar dates and only show the **next 5**. Your `week_day_plan` already has a `dateEpochDay` setter we can use.&#x20;

**Changes:**

* Backfill `dateEpochDay` for existing plans (e.g., pick a start date per phase and map week/day).
* New DAO helpers:

    * `fun upcomingFive(today: Long): Flow<List<WeekDayPlanEntity>>` → `SELECT * FROM week_day_plan WHERE dateEpochDay >= :today ORDER BY dateEpochDay ASC LIMIT 5`
    * `fun byDate(date: Long): WeekDayPlanEntity?` (for tapping a specific date).
* Keep existing navigations (`getPlansForPhaseOrdered`, `getNextAfter`) so nothing breaks while we switch screens to date‑driven lists. &#x20;

**Acceptance checks:**

* New “Calendar/Upcoming” screen shows 5 days based on `dateEpochDay`.
* Legacy “Week/Day” navigation still functions until we fully migrate.

---

# Phase 4 — User intake: goals & equipment (prep for ML)

**Goal:** Capture the inputs the planner will need, without changing plan screens.

**DB/entities (new):**

* `UserProfile(userId, trainingGoal, experience, availableMinutesPerSession, preferredDays, splitPreference, constraints, stylePrefs)`
* `UserEquipment(userId, equipment)` (many‑to‑many)

**UI:**

* A simple **Settings → Training Setup** screen to enter goal/equipment.
* No impact on existing workout displays.

**Acceptance checks:**

* Persisted profile + equipment.
* Seed exercises still list via `LibraryDao` unchanged.&#x20;

---

# Phase 5 — ML v1 (planner service + suggestions)

**Goal:** Generate a candidate plan per day using your existing strength formulas and PRs, but **only as suggestions** (non‑destructive).

**What you already have to leverage:**

* Epley e1RM + reps→%1RM helpers for load targets. &#x20;
* PR DAO to read best e1RM / best weight\@reps. &#x20;

**Planner outline:**

* Inputs: `UserProfile`, `UserEquipment`, target **focus** (push/pull/legs or full‑body), time cap, recent PRs.
* Candidate selection: pull exercises from library by `WorkoutType` and `primaryEquipment` for now (we’ll add richer metadata later); compute suggested load using `repsToPercentage` + e1RM fallback. &#x20;
* Output: A proposed set of `WorkoutBlock/BlockExercise` rows (Phase 1 tables).
* Integration: show a **“Generate Suggestions”** button on the edit screen; users can accept to copy suggestions into the actual plan.

**Acceptance checks:**

* Suggestions generate without errors for at least one phase.
* Accepting suggestions populates Phase 1 tables; legacy day list still renders (since we keep writing `day_item` until full cutover).

---

# Phase 6 — “Select your workout” UI (final pass)

**Goal:** A user‑facing picker that uses the planner + metcon library to present recommendations per day.

**UI behavior:**

* For each upcoming day (from Phase 3), show:

    * **Recommended strength** (from ML v1) with load targets.
    * **Recommended metcon** filtered by available equipment/focus (Phase 2 structure).
* Allow swapping from the library; selections create/replace blocks for that date.

**Acceptance checks:**

* Selecting a workout updates the date’s plan; the list of 5 upcoming days reflects it.
* Metcon detail still shows text (we kept `text` on components), but the backend is powered by structured fields (Phase 2).&#x20;

---

## Cutover notes (how we avoid UI regressions)

* **Keep PlanDao APIs stable** while we add new tables/fields. Your strength day UI depends on its current query; we won’t remove or change it until the new block‑based UI is ready.&#x20;
* **Metcon:** keep using `PlanWithComponents`/`getMetconsForDay` while we add structure—the UI won’t notice. &#x20;
* **Feature flags:** gate the new date‑driven list and the ML suggestion button so you can flip them separately.

## Small DAO additions you’ll likely want (copy‑paste ready)

```kotlin
// PlanDao additions (Phase 3)
@Query("""
  SELECT * FROM week_day_plan
  WHERE dateEpochDay >= :todayEpochDay
  ORDER BY dateEpochDay ASC
  LIMIT 5
""")
fun upcomingFive(todayEpochDay: Long): Flow<List<WeekDayPlanEntity>>
```

```kotlin
// Convenience to set today (Phase 3)
suspend fun setPlanDateToToday(dayPlanId: Long, todayEpochDay: Long) =
    updatePlanDate(dayPlanId, todayEpochDay) // already exists
```

(Uses your existing `updatePlanDate` method.)&#x20;

---

## Optional “Phase 0” (now): expand enums (non‑breaking)

Right now `WorkoutType` is `PUSH, PULL, LEGS_CORE`. We can leave it for back‑compat and **add** a new enum `WorkoutFocus` (`PUSH, PULL, LEGS, UPPER, LOWER, FULL_BODY, CORE, CONDITIONING`) for planner logic, plus `MovementPattern`/`MuscleGroup` types we’ll use later—without touching the UI.&#x20;

---

If you want, I can draft the Kotlin/Room stubs for `WorkoutBlock`/`BlockExercise`, the `upcomingFive` DAO, and a short migration that backfills `dateEpochDay` so you can start Phase 1+3 immediately.
