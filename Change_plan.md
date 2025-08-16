# Change Plan — Workout App
**Revision:** 2025-08-15

This plan is status-first. Every bullet is a checkbox with a status indicator:
- ✅ = Done · 🔄 = In Progress · x = Not Started

---

## 0) Snapshot

- ✅  **Phase 0 — Foundations** (new Phase/Week/Day model preferred; legacy fallback)
- ✅  **Phase 1 — Metcon unification & edits** (reads, logs, and edits aligned; legacy fallback intact)
- [ ] **Phase 2 — Strength: 1RM engine + rest timer + PR celebration** (next up)

---

## 1) Definition of Done per phase

### Phase 0 — Foundations — **✅ Done**
- ✅ Repo **prefers** Phase model (`day_item`) for reads and **falls back** to legacy when empty.
- ✅  DB includes `PhaseEntity`, `WeekDayPlanEntity`, `DayItemEntity` and `planDao()`.
- ✅  Dev seed creates Week 1 Day 1–5 if empty; idempotent; top-ups ensure metcon variety.
- ✅ App builds & runs from fresh install with pre-populated days.

### Phase 1 — Metcon unification & edits — **✅ Done**
- ✅  **Display**: `lastMetconDisplay` prefers `metcon_log`; legacy summary is fallback.
- ✅  **Logging**: plan-scoped `FOR_TIME`, `AMRAP`, `EMOM` stored in `metcon_log` (RX/Scaled kept).
- ✅  **Editing**: add/remove/reorder/toggle metcons write to **`day_item`** when a plan exists; legacy when not.
- ✅  **Verification**: all three metcon types tested; edits reflect immediately; persistence verified.

### Phase 2 — Strength: 1RM engine + rest timer + PR celebration — **⏳ Not Started**
- [ ] **1RM engine** (estimate from recent successful sets; show suggested loads).
- [ ] **Rest timer** (auto-start after strength set; survives navigation/background).
- [ ] **PR celebration** (hard rep-max PRs and soft e1RM PRs; banner + confetti; one-shot event).

---

## 2) What shipped (by component)

- ✅  **DB/Schema**: Registered Phase tables; version bumped; destructive migration (dev only).
- ✅  **DAOs**: `PlanDao` reads/writes + metcon edit helpers; legacy DAOs unchanged.
- ✅  **Repository**:
- ✅  Read adapters for strength/metcon (prefer new model; fallback legacy).
- ✅  Unified `lastMetconDisplay` (new first; legacy fallback).
- ✅  **Dual-path metcon edits** (day_item when plan exists; legacy otherwise).
- ✅  **ViewModel**: `lastMetconDisplay` exposed; legacy fields retained.
- ✅  **Dev Seed**: Creates phase/plans; mirrors legacy or defaults; tops up metcon variety.

---

## 3) Test evidence (executed)

- ✅ Fresh install shows Week 1 Day 1–5 with items; no duplication on relaunch.
- ✅  Logging: FOR-TIME (mm:ss), AMRAP (rounds + reps), EMOM (intervals) with RX/Scaled.
- ✅ Preference rule: metcon_log result **overrides** legacy when both exist.
- ✅  Editing metcons: add/remove/reorder/toggle reflected immediately; data persists.
- ✅  Legacy flows (strength logs, suggestions) unaffected.

---

## 4) Phase 2 scope — *detailed tracker*

### 4.1 1RM engine (add-only) — **⏳**
- [ ] Implement e1RM estimate (default Epley; cap reps used at ≤12).
- [ ] Expose `estimateOneRepMax(weight, reps)` in repo (pure function).
- [ ] Expose `suggestNextLoad(exerciseId, equipment, reps)` — prefer 1RM; fallback to last-success.
- [ ] Show e1RM & suggested weight in the strength detail UI.

### 4.2 Rest timer (add-only) — **⏳**
- [ ] Start timer automatically after `logStrengthSet` (configurable default, e.g., 120s).
- [ ] Timer chip: countdown, pause/resume, “+30s” nudge.
- [ ] Persist timer across background/process death (saved-state + local notification).

### 4.3 PR celebration (concept locked) — **⏳**
- [ ] **What to count as a PR**
- [ ] **Hard PR — Rep-Max PR**: more **weight at the same reps** than ever for that exercise + equipment.
- [ ] **Soft PR — Estimated-1RM PR**: today’s e1RM > best historical e1RM.

**Priority (avoid double count)**
- [ ] If both trigger, **fire Hard PR only**; include e1RM in the banner body.

**Guardrails**
- [ ] Only on `success == true` sets.
- [ ] Per **equipment** (barbell/dumbbell/kettlebell tracked separately).
- [ ] e1RM uses reps ≤ 12.
- [ ] Thresholds: Hard PR ≥ equipment plate step (e.g., barbell +2.5 kg); Soft PR ≥ max(1%, 1.0 kg).
- [ ] One PR per set; optional RPE ≥ 6 if/when RPE is logged.

**UI celebration**
- [ ] **Hard PR banner** (bigger confetti): “New **5RM**: **102.5 kg** 🎉 (prev 100.0) — e1RM 117.1 kg”
- [ ] **Soft PR banner** (subtle confetti): “New **e1RM**: **117.1 kg** ▲ +1.8 kg”
- [ ] Add PR badge on the set row; emit as one-shot VM event.
- [ ] **Earned rest**: when PR fires, auto-extend rest +30s.

**Storage plan (compatible with current schema)**
_Current table: `personal_record(exerciseId, recordType, value, date, notes)` — no `equipment` or `reps` fields yet._
- [ ] **Option A — No schema change (fastest)**: encode type in `recordType` (e.g., `E1RM_KG`, `RM_5_KG`); optionally encode equipment in `notes` (e.g., `equipment=BARBELL`). *Limitation:* not query-friendly for equipment.
- [ ] **Option B — Tiny migration (preferred)**: extend PR storage to capture both dimensions cleanly:
- [ ] Add columns `equipment TEXT` and `reps INTEGER?` (null for E1RM). Key by `(exerciseId, equipment, recordType, reps)`.
- [ ] Add DAOs: `bestEstimated1RM(...)`, `bestWeightAtReps(...)`, `upsertEstimated1RM(...)`, `upsertRepMax(...)`.

---
### Phase 3 — Engine (Run/Row/Bike) & Skills — **⏳ Not Started**
**Decision: where to log cardio & skills**  
**Recommendation:** keep them as **separate item types** (`ENGINE`, `SKILL`) with their **own logs**, while still allowing them to be **components inside Metcons**. This keeps standalone sessions clean, preserves analytics/PRs, and avoids overloading `metcon_log`. Scaled variants live on the plan component (e.g., “DU → SU”; “MU → banded MU”).

- [ ] Add new day item types: `ENGINE`, `SKILL` (no removal of existing types).
- [ ] Allow ENGINE/SKILL to appear as components inside metcons (for mixed workouts).
- [ ] Add `engine_log` (cardio) and `skill_log` (skills) tables *(or a unified `performance_log` with a `category` field)*.

**ENGINE (Cardio) — v1 scope**
- [ ] Modes: `RUN`, `ROW`, `BIKE`.
- [ ] Intents: `FOR_TIME` (fixed distance), `FOR_DISTANCE` (fixed time), `FOR_CALORIES` (fixed time).
- [ ] Program fields (per item): `distanceMeters?` / `durationSeconds?` / `targetCalories?` *(exactly one)*.
- [ ] Result fields (per log): `timeSeconds` / `distanceMeters` / `calories` + optional `pace`.
- [ ] Scaling: choose different distance/duration/calories; record `result` tag as `RX` or `Scaled`.
- [ ] PRs: best 2k row time, best 5k run, most calories in 10 min, most meters in 10 min (per mode).

**SKILL — v1 scope**
- [ ] Skills: `DOUBLE_UNDERS`, `HANDSTAND_HOLD`, `MUSCLE_UP` *(extensible)*.
- [ ] Test types: `MAX_REPS_UNBROKEN`, `FOR_TIME_REPS`, `MAX_HOLD_SECONDS`, `ATTEMPTS`.
- [ ] Program fields: `targetReps?` / `targetDuration?` / `progressionLevel?` / `scaledVariant?`.
- [ ] Result fields: `reps` / `timeSeconds` / `maxHoldSeconds` / `attempts`, plus `RX`|`Scaled`.
- [ ] PRs: max unbroken DUs, longest handstand hold, time-to-30 DUs, “first MU” milestone.

**UI**
- [ ] ENGINE card: show programmed target + pace/result line; simple logging picker.
- [ ] SKILL card: show target + scaled variant picker; log unbroken reps/hold time.
- [ ] Both also render inside metcon details (no extra UI beyond component display).

**Storage notes (align with current `PersonalRecord`)**
_Current table: `personal_record(exerciseId, recordType, value, date, notes)`._
- [ ] **Option A — No migration**: encode PR type in `recordType` (`RUN_5K_TIME`, `ROW_2K_TIME`, `DU_MAX_REPS`); encode mode/scale in `notes` (e.g., `mode=ROW;scaled=true`). *Fast, but less queryable.*
- [ ] **Option B — Tiny migration (preferred)**: add `mode/equipment` and (for skills) `reps`/`durationType` columns so queries are first-class. Key on `(exerciseId, mode/equipment, recordType, reps?)`.

**Acceptance**
- [ ] Users can log standalone engine or skill sessions and see them in history.
- [ ] PRs computed correctly per mode/skill; banners fire with clear labels.
- [ ] Engine/Skill components inside metcons continue to work unchanged.

---

## 6) Backlog (kept; not removed)

- [ ] Plan Editor UI (day-level add/remove/reorder for strength & metcons)
- [ ] Calendar Attach (map Week/Day to dates; simple “today” picker)
- [ ] Strength edits parity (dual-path writes for strength items)
- [ ] Debug actions (Reseed / Top-up metcons)
- [ ] Unit tests (read preference, dual-path edits, lastMetconDisplay preference)
- [ ] Pre-release prep (disable dev seed; add Room migrations; enable schema export)

---

## 7) Notes on dev posture

- [ ] 🔄 Keep **destructive migration** and **dev seed** enabled while in active development.
- [ ] When preparing for release, flip posture: disable dev seed, add Room migrations, enable schema export.

---

## 8) Changelog

- **2025-08-15** — Added **PR celebration** to Phase 2 tracker with hard/soft PR definitions, guardrails, UI plan, and storage options.
