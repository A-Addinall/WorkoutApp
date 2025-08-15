# screen_contracts.md

Compact “contracts” for each user-facing screen. Each section lists **Inputs**, **State Observed**, **Actions/Calls**, **Side‑effects**, **Navigation**, and **UI Notes**. This file is a *companion* to `tech_map.md` (canonical).

---

## MainActivity
**Inputs:** —  
**Observes:** `WorkoutViewModel.daySummary(day)` per Day 1–5 (subtitle string).  
**Actions/Calls:** Navigates to `WorkoutActivity`, `ExerciseLibraryActivity`, `PersonalRecordsActivity`, `SettingsActivity`.  
**Side‑effects:** —  
**Navigation:** Launcher activity.  
**UI Notes:** Uses `_main_day_card.xml` includes; day cards show “Start” (to `WorkoutActivity`) and “Edit” (to library).

---

## ExerciseLibraryActivity
**Inputs:** `DAY_INDEX` (via internal state; defaults to 1 inside activity).  
**Observes:** `LibraryViewModel.exercises: LiveData<List<Exercise>>`, `LibraryViewModel.metconPlans: LiveData<List<MetconPlan>>`, `LibraryViewModel.metconPlanIdsForDay: LiveData<Set<Long>>`.  
**Actions/Calls:**  
- Strength: `WorkoutRepository.addToDay/removeFromDay/setTargetReps` from row buttons/chips.  
- Metcons: `addMetconToDay/removeMetconFromDay/setMetconRequired/setMetconOrder/setMetconDay`.  
**Side‑effects:** Persists program membership and reps for selected day.  
**Navigation:** Back to previous.  
**UI Notes:** `activity_exercise_library.xml` with mode toggle (Strength/Metcons), filters, `ListView` for Strength rows (`item_library_row.xml`), `RecyclerView` for metcon plans (`item_metcon_plan_row.xml`).

---

## WorkoutActivity
**Inputs:** `DAY_INDEX:Int` (1..5), `WORKOUT_NAME:String` (optional).  
**Observes:**  
- `WorkoutViewModel.programForDay: LiveData<List<ExerciseWithSelection>>`  
- `WorkoutViewModel.metconsForDay: LiveData<List<SelectionWithPlanAndComponents>>`  
**Actions/Calls:**  
- On create: `WorkoutRepository.startSession(day)` (async) → creates `WorkoutSession`.  
- On strength card tap → start `ExerciseDetailActivity`.  
- On metcon card tap → start one of: `MetconActivity` (For Time), `MetconAmrapActivity`, `MetconEmomActivity` (heuristic by title).  
**Side‑effects:** Creates a session record for the day.  
**Navigation:** Back to Main.  
**UI Notes:** `activity_workout.xml`; strength cards styled; metcon plan cards from `item_metcon_plan_card.xml` with last‑result label per plan.

---

## ExerciseDetailActivity
**Inputs:** `SESSION_ID:Long`, `EXERCISE_ID:Long`, `EXERCISE_NAME:String`, `EQUIPMENT:String`, `TARGET_REPS:Int?`.  
**Observes:** (through repo queries triggered by UI lifecycle) last successful weight and suggested weight.  
**Actions/Calls:** `WorkoutViewModel.logStrengthSet(...)` per set; creates multiple `SetLog` rows; supports notes.  
**Side‑effects:** Strength set logs persisted; PRs may be affected downstream.  
**Navigation:** Back to `WorkoutActivity`.  
**UI Notes:** `activity_exercise_detail.xml`; dynamic set rows use `item_set_entry.xml`; “Complete Exercise” finalizes UX only (no explicit session close).

---

## MetconActivity (FOR_TIME)
**Inputs:** `DAY_INDEX:Int`, `WORKOUT_NAME:String?`, `PLAN_ID:Long`.  
**Observes:**  
- `WorkoutViewModel.planWithComponents(planId)` → binds card title/components.  
- `WorkoutViewModel.lastMetconForPlan(planId)` → shows last time + result.  
**Actions/Calls:**  
- Timer (count‑up) with 5s pre‑countdown (`TimerBeeper`).  
- `WorkoutViewModel.logMetconForTime(day, planId, timeSeconds, result)` on complete.  
**Side‑effects:** Inserts `MetconLog` (plan‑scoped).  
**Navigation:** Back to `WorkoutActivity`.  
**UI Notes:** `activity_metcon.xml` with RX/Scaled radio, plan card include, “Complete Metcon”.

---

## MetconAmrapActivity
**Inputs:** `DAY_INDEX:Int`, `PLAN_ID:Long`.  
**Observes:** `WorkoutViewModel.planWithComponents(planId)` (binds title/components).  
**Actions/Calls:**  
- Timer (count‑down with 5s pre‑countdown).  
- Direct inputs for `rounds` and `extraReps`.  
- `WorkoutViewModel.logMetconAmrap(day, planId, durationSeconds, rounds, extraReps, result)`.  
**Side‑effects:** Inserts `MetconLog` (AMRAP).  
**Navigation:** Back to `WorkoutActivity`.  
**UI Notes:** `activity_metcon_amrap.xml`; hides “last” label by spec; RX/Scaled radio centred.

---

## MetconEmomActivity
**Inputs:** `DAY_INDEX:Int`, `PLAN_ID:Long`.  
**Observes:** `WorkoutViewModel.planWithComponents(planId)`; derives `durationSeconds` from plan.  
**Actions/Calls:**  
- Timer (count‑down with 5s pre‑countdown).  
- Minute tick beeps; optional per‑minute cues.  
- `WorkoutViewModel.logMetconEmom(day, planId, durationSeconds, intervalsCompleted, result)`.  
**Side‑effects:** Inserts `MetconLog` (EMOM).  
**Navigation:** Back to `WorkoutActivity`.  
**UI Notes:** `activity_metcon_emom.xml`; plan card include; RX/Scaled required before completion.

---

## SettingsActivity
**Inputs:** —  
**Observes:** —  
**Actions/Calls:** Reads/writes `UserSettings` via Room wiring (available); currently UI elements present for theme, weight increment, rest time, units, PR toggle.  
**Side‑effects:** Updates `user_settings` table when wired (scaffold present).  
**Navigation:** Back to Main.  
**UI Notes:** `activity_settings.xml` with switches, inputs, spinner; save button.

---

## PersonalRecordsActivity
**Inputs:** —  
**Observes:** —  
**Actions/Calls:** — (placeholder)  
**Side‑effects:** —  
**Navigation:** Back to Main.  
**UI Notes:** `activity_placeholder.xml` placeholder.

---

### Common VM/Repo Touchpoints (reference)
- `WorkoutViewModel`: `programForDay`, `metconsForDay`, `planWithComponents`, `lastMetconForPlan`, strength & metcon logging APIs, `daySummary(...)`.  
- `WorkoutRepository`: wraps DAOs; starts sessions; logs sets; plan‑scoped metcon logging; queries last results.

---

**Maintenance rule:** When a screen’s inputs or calls change, update this file *and* the corresponding entries in `tech_map.md`’s UI and repo sections.
