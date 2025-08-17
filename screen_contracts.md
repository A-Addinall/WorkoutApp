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
**Observes:** `programForDay`, `metconsForDay`, `lastMetconDisplayForDay` (repo prefers Phase model if available).  
**Actions/Calls:** `startSession(day)`; open `ExerciseDetailActivity` or `Metcon*` screens.  
**Side‑effects:** Creates `WorkoutSession`.  
**Navigation:** Back to Main.  
**UI Notes:** `activity_workout.xml`; uses metcon plan cards.

### WorkoutActivity (data source bridge)
The ViewModel/Repo **prefers** the Phase model (Phase → Week/Day → `day_item`) via `PlanDao` **when items exist** and **falls back** to legacy (`program_selection` + `program_metcon_selection`) otherwise. No UI changes required; adapters are in the repository layer.

---

## ExerciseDetailActivity
**Inputs:** `SESSION_ID:Long`, `EXERCISE_ID:Long`, `EXERCISE_NAME:String`, `EQUIPMENT:String`, `TARGET_REPS:Int?`.  
**Observes:** last/suggested weight via repo queries; **e1RM (estimated 1RM)**; **`prEvent: LiveData<PrCelebrationEvent?>`**.  
**Actions/Calls:** `previewPrEvent(...)` (pre‑confirm PR check), `logStrengthSet(...)` (persists set and PR evaluation).  
**Side‑effects:** Writes `SetLog`; may upsert `PersonalRecord` (estimated 1RM or rep‑max); may trigger PR modal.  
**Navigation:** Back to WorkoutActivity.  
**UI Notes:** `activity_exercise_detail.xml` updated with e1RM/PR hints; PR dialog: `dialog_pr.xml` (strings include `pr_title_hard`, etc.).

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
**Inputs:** —  
**Observes:** Best e1RM and rep‑max per exercise/equipment as available via repo.  
**Actions/Calls:** (Placeholder) future: filter by exercise, export PRs.  
**Side‑effects:** —  
**UI Notes:** Placeholder list UI.

---

### Common VM/Repo Touchpoints
- `WorkoutViewModel`: screens unchanged; repo under the hood may source from Phase model via `PlanDao` or fallback.
- `WorkoutRepository`: bridges **Phase 0** (`PlanDao`) with legacy (`ProgramDao`); handles PR preview/eval notifications.

---
