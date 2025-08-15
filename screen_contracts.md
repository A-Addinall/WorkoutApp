# screen_contracts.md

Companion to `tech_map.md`. Contracts list **Inputs**, **Observed State**, **Actions**, **Side‑effects**, **Navigation**, **UI Notes**.  
(Underlying data now prefers **Phase/Week/Day** via `PlanDao`; repo falls back to legacy where needed.)

---

## MainActivity
**Inputs:** —  
**Observes:** day summaries (legacy-based for now).  
**Actions/Calls:** Navigate to `WorkoutActivity`, `ExerciseLibraryActivity`, `PersonalRecordsActivity`, `SettingsActivity`.  
**Side‑effects:** —  
**Navigation:** Launcher activity.  
**UI Notes:** `_main_day_card.xml` for day cards.

---

## ExerciseLibraryActivity
**Inputs:** `DAY_INDEX:Int` (1..5).  
**Observes:** `LibraryViewModel.exercises`, `metconPlans`, `metconPlanIdsForDay`.  
**Actions/Calls:** Strength: `addToDay/removeFromDay/setTargetReps`; Metcons: `addMetconToDay/removeMetconFromDay/setMetconRequired/setMetconOrder/setMetconDay`.  
**Side‑effects:** Persists program membership/reps (legacy); Phase model population handled by seeds/dev sync.  
**Navigation:** Back.  
**UI Notes:** `activity_exercise_library.xml`; rows `item_library_row.xml`, plan rows `item_metcon_plan_row.xml`.

---

## WorkoutActivity
**Inputs:** `DAY_INDEX:Int` (1..5), `WORKOUT_NAME:String?`.  
**Observes:** `programForDay`, `metconsForDay` (repo prefers Phase model if available).  
**Actions/Calls:** `startSession(day)`; open `ExerciseDetailActivity` or `Metcon*` screens.  
**Side‑effects:** Creates `WorkoutSession`.  
**Navigation:** Back to Main.  
**UI Notes:** `activity_workout.xml`; uses metcon plan cards.

---

## ExerciseDetailActivity
**Inputs:** `SESSION_ID:Long`, `EXERCISE_ID:Long`, `EXERCISE_NAME:String`, `EQUIPMENT:String`, `TARGET_REPS:Int?`.  
**Observes:** last/suggested weight via repo queries.  
**Actions/Calls:** `logStrengthSet(...)` per set.  
**Side‑effects:** Writes `SetLog`.  
**UI Notes:** `activity_exercise_detail.xml` + `item_set_entry.xml`.

---

## MetconActivity (FOR_TIME)
**Inputs:** `DAY_INDEX:Int`, `WORKOUT_NAME:String?`, `PLAN_ID:Long`.  
**Observes:** `planWithComponents(planId)`, `lastMetconForPlan(planId)`.  
**Actions/Calls:** `logMetconForTime(...)`.  
**Side‑effects:** Writes `MetconLog`.  
**UI Notes:** `activity_metcon.xml`.

---

## MetconAmrapActivity
**Inputs:** `DAY_INDEX:Int`, `PLAN_ID:Long`.  
**Observes:** `planWithComponents(planId)`.  
**Actions/Calls:** `logMetconAmrap(...)`.  
**Side‑effects:** Writes `MetconLog`.  
**UI Notes:** `activity_metcon_amrap.xml`.

---

## MetconEmomActivity
**Inputs:** `DAY_INDEX:Int`, `PLAN_ID:Long`.  
**Observes:** `planWithComponents(planId)`.  
**Actions/Calls:** `logMetconEmom(...)`.  
**Side‑effects:** Writes `MetconLog`.  
**UI Notes:** `activity_metcon_emom.xml`.

---

## SettingsActivity
**Inputs:** —  
**Actions/Calls:** planned wiring to `UserSettings`.  
**Side‑effects:** update settings when wired.  
**UI Notes:** `activity_settings.xml`.

---

## PersonalRecordsActivity
Placeholder; no changes.

---

### Common VM/Repo Touchpoints
- `WorkoutViewModel`: screens unchanged; repo under the hood may source from Phase model via `PlanDao` or fallback.
- `WorkoutRepository`: bridges **Phase 0** (`PlanDao`) with legacy (`ProgramDao`).

