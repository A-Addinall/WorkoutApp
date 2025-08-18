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
