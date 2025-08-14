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
| `Enums.kt` | Domain enums for workouts, equipment, modality, rep schemes, and metcon results. | `WorkoutType`, `Equipment`, `Modality`, `RepScheme(val reps:Int)`, **`MetconResult`** | Kotlin stdlib | Entities, DAOs, Repo, UI | `RepScheme.fromReps` defaults to R8 (verify); `MetconResult` used by UI/VM; **not yet persisted** in DB (see SetLog note).

### 2.2 data/dao/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `data/dao/ExerciseDao.kt` | Exercise CRUD | `count()`, `insertAll(List<Exercise>)` | Used by seed. |
| `data/dao/LibraryDao.kt` | Library queries | `getExercises(type, eq): Flow<List<Exercise>>`, `countExercises()`, `insertAll(...)` | Null = no filter. |
| `data/dao/PersonalRecordDao.kt` | PR queries | `upsert(PersonalRecord)`, `bestForExercise(exerciseId)` | — |
| `data/dao/ProgramDao.kt` | Program selections | `getProgramForDay(day): Flow<List<ExerciseWithSelection>>`, `upsert`, `remove`, `setRequired`, `setTargetReps`, `exists`, `distinctTypesForDay` | DTO `ExerciseWithSelection`. |
| `data/dao/SessionDao.kt` | Sessions & logs | `insertSession`, `insertSet`, `lastSets(...)`, `lastMetconSecondsForDay(day)` | Metcon result not persisted yet. |
| `data/dao/MetconDao.kt` | **Metcon plans** | `getAllPlans(): Flow<List<MetconPlan>>`, `getMetconsForDay(day): Flow<List<SelectionWithPlanAndComponents>>`, `upsertSelection`, `removeSelection`, `setRequired`, `setDisplayOrder` | DTOs `PlanWithComponents`, `SelectionWithPlanAndComponents` are **top-level** types. |

### 2.3 data/db/

| File | Responsibility | Key API / Settings | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `data/db/AppDatabase.kt` | Room DB | `@Database(version = 3, entities = [..., MetconPlan, MetconComponent, ProgramMetconSelection])`; DAOs include `metconDao()`; seeds **exercises** and **metcon plans** in `onCreate` | Dev: destructive migration enabled. |
| `data/db/Converters.kt` | Type converters | `WorkoutType↔String`, `Equipment↔String`, `MetconType↔String` | Add `MetconResult` converter when persisted. |

### 2.4 data/entities/

| File | Table | Fields (key) | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `data/entities/Exercise.kt` | `exercise` | `id`, `name`, `workoutType`, `primaryEquipment`, `modality`, `isUnilateral` | Seeded. |
| `data/entities/ProgramSelection.kt` | `program_selection` | `id`, `dayIndex`, `exerciseId`, `required`, `preferredEquipment?`, `targetReps?` | — |
| `data/entities/SetLog.kt` | `set_log` | `id`, `sessionId`, `exerciseId`, `equipment`, `setNumber`, `reps`, `weight?`, `timeSeconds?`, `rpe?`, `success?`, `notes?` | Metcon time uses `exerciseId = 0L`. |
| `data/entities/WorkoutSession.kt` | `workout_session` | `id`, `dayIndex`, `startTs` | — |
| `data/entities/PersonalRecord.kt` | `personal_record` | Standard PR fields | — |
| `data/entities/UserSettings.kt` | `user_settings` | UX prefs | Wiring pending. |
| **`data/entities/MetconPlan.kt`** | `metcon_plan` | `id`, `title`, `type: MetconType`, `durationMinutes?`, `emomIntervalSec?` | **New**. |
| **`data/entities/MetconComponent.kt`** | `metcon_component` | `id`, `planId(FK)`, `orderInPlan`, `text` | **New**. |
| **`data/entities/ProgramMetconSelection.kt`** | `program_metcon_selection` | `id`, `dayIndex`, `planId(FK)`, `required`, `displayOrder` | **New**; multiple metcons per day + ordering. |

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
| `MainActivity.kt` | App dashboard; navigation to Days 1–5, Library, PRs, Settings. | Refreshes day labels via `repo.daySummaryLabel(day)`. | Strength‑only label for now.
| `ExerciseLibraryActivity.kt` | Browse/filter exercises; add/remove to program; set reps; toggle required. | Uses `LibraryViewModel` + `WorkoutRepository`. | Maintains `addedState`, `currentReps`, `requiredState` per row.
| `WorkoutActivity.kt` | Render day: Required / Optional (strength) + Metcon card. | Observes `programForDay`; shows last metcon time via VM. | Uses `runBlocking` for last weight (future: async).
| `ExerciseDetailActivity.kt` | Log strength sets; show last/suggested weight. | Calls VM `logStrengthSet(...)`; refreshes header via repo queries. | Unchanged by metcon work.
| `MetconActivity.kt` | Timer UI for FOR_TIME; **RX/Scaled selection in UI**. | On complete → `vm.logMetcon(day, seconds, result)` (result currently not persisted). | Last‑metcon label shows time only.
| `PersonalRecordsActivity.kt` | Placeholder for PR view. | — | Present; minimal.
| `SettingsActivity.kt` | Theme & preferences UI. | — | Persistence wiring to `UserSettings` pending.

### 2.8 viewmodels/

| File | Responsibility | Exposed LiveData | Notes |
|---|---|---|---|
| `viewmodel/WorkoutViewModel.kt` | Day state + logging | `programForDay: LiveData<List<ExerciseWithSelection>>`, **`metconsForDay: LiveData<List<SelectionWithPlanAndComponents>>`**, **`lastMetconSeconds: LiveData<Int>`**; `setDay(day)`; `logMetcon(...)` | Add-only wiring. |
| `ui/LibraryViewModel.kt` | Library filters & lists | `exercises: LiveData<List<Exercise>>`, **`metconPlans: LiveData<List<MetconPlan>>`**; helpers: `addMetconToDay`, `removeMetconFromDay`, `setMetconRequired`, `setMetconOrder`, `setMetconDay(day)` | Add-only wiring. |
| `ui/WorkoutViewModelFactory.kt` | Factory | Standard factory using `Repos.workoutRepository(context)` | — |

### 2.9 res/drawable/

Unchanged: `ic_arrow_back.xml`, launcher background/foreground vectors.

### 2.10 res/layout/

Unchanged core layouts; notable ones: `_main_day_card.xml`, `activity_main.xml`, `activity_workout.xml`, `item_exercise_card.xml`, `item_set_entry.xml`, `activity_metcon.xml` (timer + last time label), `item_metcon_card.xml` / `item_metcon_exercise.xml` for metcon sections.

### 2.11 res/values/

Unchanged: `colors.xml` (brand + success/fail), `strings.xml` (labels), `styles.xml` (typography), `themes.xml` (Material3 Light NoActionBar).

### 2.12 AndroidManifest.xml & root

| Element | Responsibility | Key details | Notes |
|---|---|---|---|
| `AndroidManifest.xml` | Registers activities; theme; app class. | `MainActivity` exported launcher; others internal; app theme `Theme.SAFitness`. | No nav graph; manual intents.
| `WorkoutApp.kt` | Application class; DB bootstrap. | `AppDatabase.get(this)`; `onCreate` seeds exercises (+ metcons). | Central entry point.

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

- Persist **metcon result** in `SetLog` (nullable) vs a separate table? *(Plan: extend `SetLog`.)*
- Default metcon equipment for time-only logs? *(Prefer `BODYWEIGHT` to avoid skewing equipment queries.)*
- Replace any `runBlocking` in day UI with coroutine/Flow to avoid jank.
- Confirm `RepScheme` default if used implicitly.

---

## 6. Next Actions (one at a time)

1) **Persist RX/Scaled**: add `metconResult: MetconResult?` to `SetLog` + converters; bump DB to v4; update insert & summary; surface in UI.
2) **Make day cards async**: audit and remove any blocking calls.
3) **AMRAP/EMOM inputs**: extend UI and repo for rounds+reps (AMRAP) and minute marks (EMOM).
