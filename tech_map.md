# WorkoutApp — Technical Map (Working Draft)

> Merged from **Old_tech_map** (baseline) with updates from **tech_map**. We preserved all prior content and layered in the new metcon items (plans, RX/Scaled UI), repo/API deltas, and version bumps.

---

## 0. Repo & Build

- **Repo**: A-Addinall/WorkoutApp
- **Default branch**: master (to confirm)
- **Module(s)**: `app`
- **Language**: Kotlin
- **Min/Target/Compile SDK**: (fill)
- **DI**: (Hilt/Koin/manual — to confirm)
- **Persistence / DB**: Room **v3** (was v1)
- **Migrations**: Dev mode uses `fallbackToDestructiveMigration()`; add proper migrations before prod. (Migration stubs may exist — confirm.)

---

## 1. App Structure (high‑level)

```
com.example.safitness
├─ core/
├─ data/
│  ├─ dao/
│  ├─ db/
│  ├─ entities/
│  └─ repo/
├─ data/seed/
├─ domain/
├─ ui/
├─ viewmodel/
└─ WorkoutApp.kt
```

> Note: `data/seed/` is explicitly called out now to include seeding for both **exercises** and (new) **metcon plans/components**.

---

## 2. Package Details

### 2.1 core/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `Enums.kt` | Domain enums for workouts, equipment, modality, metcon types, and metcon results. | `WorkoutType`, `Equipment`, `Modality`, `MetconType`, `MetconResult` | Kotlin stdlib | Entities, DAOs, Repo, UI | `MetconResult` used by UI/VM; **not yet persisted** in DB (see SetLog note).

### 2.2 data/dao/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `data/dao/ExerciseDao.kt` | Exercise CRUD | `count()`, `insertAll(List<Exercise>)`, `insertAllIgnore(List<Exercise>)` | Used by seed. |
| `data/dao/LibraryDao.kt` | Library queries | `getExercises(type, eq): Flow<List<Exercise>>`, `countExercises()`, `insertAll(...)` | Null = no filter. |
| `data/dao/PersonalRecordDao.kt` | PR queries | `upsert(PersonalRecord)`, `bestForExercise(exerciseId)` | — |
| `data/dao/ProgramDao.kt` | Program selections | `getProgramForDay(day): Flow<List<ExerciseWithSelection>>`, `upsert`, `remove`, `setRequired`, `setTargetReps`, `exists`, `distinctTypesForDay` | DTO `ExerciseWithSelection`. |
| `data/dao/SessionDao.kt` | Sessions & logs | `insertSession`, `insertSet`, `lastSets(...)`, `lastMetconSecondsForDay(day)`, `lastMetconForDay(day)` | Metcon result not persisted yet. |
| `data/dao/MetconDao.kt` | **Metcon plans & logs** | `getAllPlans(): Flow<List<MetconPlan>>`, `getMetconsForDay(day): Flow<List<SelectionWithPlanAndComponents>>`, `upsertSelection`, `removeSelection`, `setRequired`, `setDisplayOrder`, `insertLog`, `lastForPlan`, `lastForDay`, `updatePlanByKey`, `updateComponentText`, `deleteAllComponentsForPlan`, `deleteComponentsNotIn` | DTOs `PlanWithComponents`, `SelectionWithPlanAndComponents` are **top-level** types. |

### 2.3 data/db/

| File | Responsibility | Key API / Settings | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `data/db/AppDatabase.kt` | Room DB | `@Database(version = 5, entities = [..., MetconPlan, MetconComponent, ProgramMetconSelection, MetconLog])`; DAOs include `metconDao()`; seeds **exercises** and **metcon plans** in `onCreate`; calls `MetconSeed.seedOrUpdate` on open | Dev: destructive migration enabled. |
| `data/db/Converters.kt` | Type converters | `WorkoutType↔String`, `Equipment↔String`, `MetconType↔String`, `MetconResult↔String` |

### 2.4 data/entities/

| File | Table | Fields (key) | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `data/entities/Exercise.kt` | `exercise` | `id`, `name`, `workoutType`, `primaryEquipment`, `modality`, `isUnilateral` | Seeded. |
| `data/entities/ProgramSelection.kt` | `program_selection` | `id`, `dayIndex`, `exerciseId`, `required`, `preferredEquipment?`, `targetReps?` | — |
| `data/entities/SetLog.kt` | `set_log` | `id`, `sessionId`, `exerciseId`, `equipment`, `setNumber`, `reps`, `weight?`, `timeSeconds?`, `rpe?`, `success?`, `notes?`, `metconResult?` | Stores RX/Scaled for metcon attempts; metcon time uses `exerciseId = 0L`. |
| `data/entities/WorkoutSession.kt` | `workout_session` | `id`, `dayIndex`, `startTs` | — |
| `data/entities/PersonalRecord.kt` | `personal_record` | `id`, `exerciseId(FK)`, `recordType`, `value`, `date`, `notes?` | — |
| `data/entities/UserSettings.kt` | `user_settings` | `id`, `darkTheme`, `autoWeightIncrement`, `defaultRestTime`, `units`, `showPersonalRecords` | Wired via SettingsActivity; default rest time differs between primary and @Ignore ctor. |
| **`data/entities/MetconPlan.kt`** | `metcon_plan` | `id`, `canonicalKey`, `title`, `type: MetconType`, `durationMinutes?`, `emomIntervalSec?`, `isArchived` | Stable-identity metcon plan with canonical key; supports archiving. |
| **`data/entities/MetconComponent.kt`** | `metcon_component` | `id`, `planId(FK)`, `orderInPlan`, `text` | FK to `MetconPlan`; cascade delete on plan removal; unique by (planId, orderInPlan). |
| **`data/entities/MetconLog.kt`** | `metcon_log` | `id`, `dayIndex`, `planId`, `type`, `durationSeconds`, `timeSeconds?`, `rounds?`, `extraReps?`, `intervalsCompleted?`, `result`, `createdAt`, `notes?` | Stores plan-scoped metcon attempts with mode-specific scoring and RX/Scaled. |

| **`data/entities/ProgramMetconSelection.kt`** | `program_metcon_selection` | `id`, `dayIndex`, `planId(FK)`, `required`, `displayOrder` | Multiple metcons per day + ordering; FK to MetconPlan. |

### 2.5 data/repo/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `data/repo/Repos.kt` | Repo factory | `workoutRepository(context)` wires Library/Program/Session/PR/**Metcon** DAOs | — |
| `data/repo/WorkoutRepository.kt` | Orchestrates DAOs | **Library**: `getExercises`, `countExercises`. **Program**: `addToDay`, `programForDay`, `setRequired`, `setTargetReps`, `removeFromDay`, `isInProgram`, `selectedTargetReps`, `requiredFor`, `daySummaryLabel`. **Sessions**: `startSession`, `logStrengthSet`, `logTimeOnlySet`, `logMetcon(day, seconds)`; *(optional overload present)* `logMetcon(day, seconds, result: MetconResult)` (currently ignores `result`). **Queries**: `lastMetconSecondsForDay`. **Metcon plans**: `metconPlans()`, `metconsForDay(day)`, `addMetconToDay`, `removeMetconFromDay`, `setMetconRequired`, `setMetconOrder`. | Uses top-level `SelectionWithPlanAndComponents`. |

### 2.6 data/seed/

| File | Responsibility | Notes |
|---|---|---|
| `data/seed/ExerciseSeed.kt` | Inserts default `Exercise` rows if empty. | Triggered in DB `onCreate`. |
| **`data/seed/MetconSeed.kt`** | Inserts example `MetconPlan` + `MetconComponent`s if empty. | FOR_TIME / AMRAP / EMOM examples. |

### 2.7 ui/

| File | Responsibility | Key interactions | Notes |
|---|---|---|---|
| `MetconAmrapActivity.kt` | Timer UI for AMRAP with RX/Scaled selection and direct rounds/reps input; logs via `logMetconAmrap`. | Includes pre-start countdown and validation. |
| `MetconEmomActivity.kt` | Timer UI for EMOM with RX/Scaled selection; beeps each minute; logs via `logMetconEmom`. | Includes pre-start countdown and validation. |
| `MetconUiHelpers.kt` | Helper object to bind a plan card UI with title and components from VM. | Used in metcon screens. |
| `TimerBeeper.kt` | Utility for playing pips, minute ticks, and final buzz sounds. | Used by all metcon timer UIs. |
| `MainActivity.kt` | App dashboard; navigation to Days 1–5, Library, PRs, Settings. | Refreshes day labels via combined strength + metcon selections. | Shows "Metcon" label if any metcon present.
| `ExerciseLibraryActivity.kt` | Browse/filter exercises & metcons; toggle between modes; filter by type/equipment or metcon type/duration; manage membership and reps. | Uses `LibraryViewModel` + `WorkoutRepository`. | Maintains `addedState`, `currentReps` for strength; membership for metcons.
| `WorkoutActivity.kt` | Render day: Required / Optional (strength) + Metcon plan cards. | Observes `programForDay` and `metconsForDay`; shows last results for each plan via VM. | Starts appropriate metcon activity based on plan title.
| `ExerciseDetailActivity.kt` | Log strength sets; show last/suggested weight. | Calls VM `logStrengthSet(...)`; refreshes header via repo queries. | Unchanged by metcon work.
| `MetconActivity.kt` | Timer UI for FOR_TIME; **RX/Scaled selection in UI**. | On complete → `vm.logMetcon(day, seconds, result)` (result currently not persisted). | Last‑metcon label shows time only.
| `PersonalRecordsActivity.kt` | Placeholder for PR view. | Simple back button. | Present; minimal.
| `SettingsActivity.kt` | Theme & preferences UI. | Binds back button; interacts with `UserSettings`. | Persistence now possible via Room.

### 2.8 viewmodels/

| File | Responsibility | Exposed LiveData | Notes |
|---|---|---|---|
| `ui/LibraryViewModelFactory.kt` | Factory | Standard factory for `LibraryViewModel` using `WorkoutRepository`. |
| `viewmodel/WorkoutViewModel.kt` | Day state + logging | `programForDay`, `metconsForDay`, `lastMetconSeconds`, `lastMetconForPlan`; logging methods for strength and all metcon types (for time, amrap, emom). | Computes day summary including metcons. |
| `ui/LibraryViewModel.kt` | Library filters & lists | `exercises: LiveData<List<Exercise>>` (filtered by type/equipment), `metconPlans: LiveData<List<MetconPlan>>` (sorted by title), `metconPlanIdsForDay: LiveData<Set<Long>>`; helpers: `addMetconToDay`, `removeMetconFromDay`, `setMetconRequired`, `setMetconOrder`, `setMetconDay(day)` | Uses repo flows; wraps writes in `viewModelScope`. |
| `ui/WorkoutViewModelFactory.kt` | Factory | Standard factory using `Repos.workoutRepository(context)` | — |

### 2.9 res/drawable/
| File | Responsibility | Exposed LiveData | Notes |
|---|---|---|---|
|Unchanged: `ic_arrow_back.xml`| launcher background/foreground vectors.|

### 2.10 res/layout/

Unchanged core layouts; notable ones: `_main_day_card.xml`, `activity_main.xml`, `activity_workout.xml`, `item_exercise_card.xml`, `item_set_entry.xml`, `activity_metcon.xml` (timer + last time label), `item_metcon_card.xml` / `item_metcon_exercise.xml` for metcon sections.

### 2.11 res/values/
| `colors.xml` | App color palette including purple, teal, black/white, and button state colors. |
| `strings.xml` | Centralized strings for UI labels, validation messages, and accessibility. |
| `styles.xml` | Text styles for titles, body text, and captions. |
| `themes.xml` | App theme definition based on Material3 Light NoActionBar. |

Unchanged: `colors.xml` (brand + success/fail), `strings.xml` (labels), `styles.xml` (typography), `themes.xml` (Material3 Light NoActionBar).
| `activity_metcon_emom.xml` | EMOM metcon layout with timer, RX/Scaled toggle, plan card, and complete button. |
| `activity_placeholder.xml` | Simple placeholder screen layout with back button and title. |
| `activity_settings.xml` | Settings screen layout with toggles, inputs, and save button. |

| `item_enhanced_progress_card.xml` | Card layout showing progress metrics for an exercise. |
### 2.12 AndroidManifest.xml & root
| `item_library_row.xml` | Row layout for exercise library with name, reps chip group, and add/remove button. |

| Element | Responsibility | Key details | Notes |
|---|---|---|---|
| `AndroidManifest.xml` | Registers activities; theme; app class. | `MainActivity` exported launcher; all feature activities registered; `WorkoutApp` as application class; explicit exported flags for all activities. | No nav graph; manual intents.
| `item_progress_card.xml` | Card layout showing basic progress stats for an exercise. |
| `WorkoutApp.kt` | Application class; DB bootstrap. | `AppDatabase.get(this)`; `onCreate` seeds exercises (+ metcons) via `seedOrUpdate` in appScope. | Central entry point.

---
## 3) Data Flows (End-to-End)

**A. Add exercise to a day**
1. Library row → **Add**
2. Repo `addToDay(...)` → Program DAO upsert
3. `programForDay(day)` Flow emits → Workout screen rebuilds

**B. Remove exercise from a day**  
Same path via `removeFromDay(...)`.

**C. Toggle “required”**  
Library star → Repo `setRequired(...)` → Flow re-emits → Sections update.

**D. Change target reps**  
Library spinner → Repo `setTargetReps(...)` (when added) → Flow re-emits.

**E. Start/Log strength**  
Open `WorkoutActivity` → `startSession(day)` → sets logged from `ExerciseDetailActivity`.

**F. Metcon timer (FOR_TIME) — with RX/Scaled in UI**  
Run in `MetconActivity` → `vm.logMetcon(day, seconds, result)` → Repo inserts time-only log → VM refreshes `lastMetconSeconds` → Workout shows last time.

**G. Metcon Plans (new)**  
Library **Metcons** tab → list `metconPlans()` → **Add to Day** via `addMetconToDay(...)` → Workout observes `metconsForDay(day)` and renders plan(s): title + ordered components + last time.

**H. Home labels**  
`daySummaryLabel(day)` via `distinctTypesForDay(day)` → “Empty”/“Mixed”/single type.

**I. Seeding**  
DB `onCreate` → `ExerciseSeed` + `MetconSeed` (idempotent on empty).


---

## 4. Change Impact Matrix (active)

| Change | Files touched | Upstream deps | Downstream dependants | Risks | Status |
|---|---|---|---|---|---|
| Metcon plans (plans/components/selection) | `MetconPlan.kt`, `MetconComponent.kt`, `ProgramMetconSelection.kt`, `MetconDao.kt`, `AppDatabase.kt`, `Converters.kt`, `MetconSeed.kt`, `Repos.kt`, `WorkoutRepository.kt` | DB version bump; dev destructive migration | **Done** |
| Library Metcon tab | `ExerciseLibraryActivity.kt`, `MetconPlanAdapter.kt`, `activity_exercise_library.xml`, `item_metcon_plan_row.xml` | Low UI risk | **Done** |
| Workout Metcon card (pretty) | `WorkoutActivity.kt`, `item_metcon_plan_card.xml`, `bg_metcon_card.xml` | Low UI risk | **Done** |
| RX/Scaled selection UI | `MetconActivity.kt`, `activity_metcon.xml`, `WorkoutViewModel.kt`, `WorkoutRepository.kt` | Not persisted yet | **Done (UI/API)** |
| Persist metcon result | `SetLog.kt`, `Converters.kt`, `AppDatabase.kt` (v4 + migration), `SessionDao`, `WorkoutRepository.logMetcon(...)` | Schema/migration | **Next** |
| Async day card lookups | `WorkoutActivity.kt` | Replace any blocking calls | Planned |
| AMRAP/EMOM scoring inputs | Metcon UI + VM + Repo | Model + UX changes | Planned |

---

## 5. Open Questions


---

## 6. Next Actions (one at a time)

