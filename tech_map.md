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
| `ExerciseDao.kt` | Basic persistence for `Exercise`. | `count()`, `insertAll(List<Exercise>)` | Room, `Exercise` | Seeds, repo init checks | Simple CRUD.
| `LibraryDao.kt` | Query/filter exercises by type/equipment; insert and count. | `getExercises(type, eq): Flow<List<Exercise>>`, `countExercises()`, `insertAll(...)` | Room, `Exercise`, `WorkoutType`, `Equipment` | Library UI | NULL params mean no filter.
| `PersonalRecordDao.kt` | Manage `PersonalRecord`; best record lookup. | `upsert(PersonalRecord)`, `bestForExercise(exerciseId)` | Room, `PersonalRecord` | PR tracking | Best by (value desc, date desc).
| `ProgramDao.kt` | Strength program selections + joined reads. | `getProgramForDay(day)` → `ExerciseWithSelection`; `upsert`, `remove`; setters for `required`, `preferredEquipment`, `targetReps`; `exists`; `distinctTypesForDay` | Room, `ProgramSelection`, `Exercise`, `Equipment` | Program/day views | DTO `ExerciseWithSelection(exercise, required, preferredEquipment, targetReps)`.
| `SessionDao.kt` | Sessions + set logs; recent queries; metcon summaries. | `insertSession`, `insertSet`, `lastSets(...)`, `lastMetconSecondsForDay(day)` | Room, `WorkoutSession`, `SetLog`, `Equipment` | Strength logging, metcon last‑time | **Result type (RX/Scaled) not persisted yet**; summary shows time only.
| **`MetconDao.kt`** | Metcon plans & day mapping (**new**). | `getAllPlans(): Flow<List<MetconPlan>>`; `getMetconsForDay(day): Flow<List<SelectionWithPlanAndComponents>>`; `upsertSelection(...)`; `removeSelection(...)`; `setRequired(...)`; `setDisplayOrder(...)` | `MetconPlan`, `MetconComponent`, `ProgramMetconSelection` | Future metcon library UI + day view | Supports multiple metcons/day + ordering.

### 2.3 data/db/

| File | Responsibility | Key API / Settings | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `AppDatabase.kt` | Central Room DB (**version 3**). | `@Database(..., version=3)`, DAOs: Library, Program, Session, PR, **MetconDao**; `@TypeConverters(Converters)`; `onCreate` seeds **exercises + metcon plans** | Room, Seeds | Global persistence; `Repos`, `WorkoutApp` | Dev: `fallbackToDestructiveMigration()` enabled.
| `Converters.kt` | Room enum converters. | `WorkoutType↔String`, `Equipment↔String` (optionally `MetconResult↔String` when persisted) | Core enums | AppDatabase | Keep names in sync with enum constants.

### 2.4 data/entities/

| File | Table | Fields (key) | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `Exercise.kt` | `exercise` | `id`, `name`, `workoutType`, `primaryEquipment`, `modality`, `isUnilateral` | `WorkoutType`, `Equipment`, `Modality` | DAOs, program logic, UI | Seeded on first run.
| `PersonalRecord.kt` | `personal_record` | `id`, `exerciseId(FK)`, `recordType`, `value`, `date`, `notes` | Room, FK → `Exercise` | PR DAO | Cascade delete with exercise.
| `ProgramSelection.kt` | `ProgramSelection` | `id`, `dayIndex`, `exerciseId`, `required`, `preferredEquipment`, `targetReps` | `Equipment` | Program DAO | Nullable `targetReps`.
| `SetLog.kt` | `SetLog` | `id`, `sessionId`, `exerciseId`, `equipment`, `setNumber`, `reps`, `weight`, `timeSeconds`, `rpe`, `success`, `notes` | `Equipment` | Session DAO | **No `metconResult` column yet**; time‑only metcons use `exerciseId=0L`.
| `WorkoutSession.kt` | `WorkoutSession` | `id`, `dayIndex`, `startTs` | — | Session DAO | Indexed by `dayIndex`, `startTs`.
| `UserSettings.kt` | `user_settings` | `id=1`, `darkTheme`, `autoWeightIncrement`, `defaultRestTime`, `units`, `showPersonalRecords` | — | Settings UI (future wiring) | Secondary `@Ignore` ctor with slightly different defaults.
| **`MetconPlan.kt`** | `MetconPlan` (**new**) | `id`, `title`, `type: MetconType`, `durationMinutes?`, `emomIntervalSec?` | Core enums | Metcon DAO/Repo | Defines plan (FOR_TIME / AMRAP / EMOM).
| **`MetconComponent.kt`** | `MetconComponent` (**new**) | `id`, `planId(FK)`, `orderInPlan`, `text` | `MetconPlan` | Metcon DAO | Human‑readable components.
| **`ProgramMetconSelection.kt`** | `ProgramMetconSelection` (**new**) | `id`, `dayIndex`, `planId(FK)`, `required`, `displayOrder` | `MetconPlan` | Metcon DAO/Repo | Multiple metcons per day + ordering.

### 2.5 data/repo/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `Repos.kt` | Factory for repositories backed by DB DAOs. | `workoutRepository(context)` | `AppDatabase`, DAOs | Application/service layer | Now wires **MetconDao** as well.
| `WorkoutRepository.kt` | Orchestrates DAOs; light business logic. | **Library**: `getExercises`, `countExercises`. **Program**: `addToDay`, `programForDay`, `setRequired`, `setTargetReps`, `removeFromDay`, `isInProgram`, `selectedTargetReps`, `requiredFor`, `daySummaryLabel`. **Sessions**: `startSession`, `logStrengthSet`, `logTimeOnlySet`, `logMetcon(day, seconds)`; **overload** `logMetcon(day, seconds, result: MetconResult)` (calls 2‑arg version; **result not persisted yet**). **Queries**: `lastMetconSecondsForDay`, (optional) `lastMetconForDay()`. **Metcon plans**: `metconPlans`, `metconsForDay`, `addMetconToDay`, `removeMetconFromDay`, `setMetconRequired`, `setMetconOrder`. | All DAOs, entities, enums | VM/UI | Suggested weight ≈ last successful ×1.02; metcon equipment placeholder under discussion.

### 2.6 data/seed/

| File | Responsibility | Notes |
|---|---|---|
| `ExerciseSeed.kt` | Seed default `Exercise` rows on first run. | Triggered from DB `onCreate` if empty.
| **`MetconSeed.kt`** | Seed example `MetconPlan` + `MetconComponent`s. | Include FOR_TIME, AMRAP, EMOM examples.

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
| `LibraryViewModel.kt` | Filters + list of `Exercise`. | `exercises: LiveData<List<Exercise>>` | MediatorLiveData over Flow.
| `WorkoutViewModel.kt` | Day state + logging. | `programForDay: LiveData<List<ExerciseWithSelection>>`, `lastMetconSeconds: LiveData<Int>` | `setDay(day)` loads last metcon seconds; provides `logStrengthSet`, `logMetcon`.
| `WorkoutViewModelFactory.kt` | Factory for `WorkoutViewModel`. | — | Standard pattern.

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

## 3. Data Flow Traces (end‑to‑end)

### 3.1 Add exercise to a day (Library → Program)
1) **UI** `ExerciseLibraryActivity` row → **Add**.
2) **Repo** `addToDay(day, exercise, required, preferred, targetReps)`.
3) **DAO** `ProgramDao.upsert(ProgramSelection)`.
4) **VM** `programForDay(day)` Flow re‑emits.
5) **UI** `WorkoutActivity` re‑renders sections.

### 3.2 Remove exercise from a day
1) **UI** row → **Remove**.
2) **Repo** `removeFromDay(day, exerciseId)` → **DAO** delete.
3) **Flow** emits; day view updates.

### 3.3 Toggle “required” flag
1) **UI** star toggle.
2) **Repo** `setRequired(day, exerciseId, newVal)` → **DAO** update.
3) **Flow** emits; card moves Required↔Optional.

### 3.4 Change target reps
1) **UI** reps spinner.
2) **Repo** `setTargetReps(day, exerciseId, reps)` (if already added) → **DAO** update.
3) **Flow** emits; labels update.

### 3.5 Start a workout session (strength)
1) **UI** open `WorkoutActivity` → **Repo** `startSession(day)` → sessionId.
2) **UI** opens `ExerciseDetailActivity` with extras.

### 3.6 Log a strength set
1) **UI** success/fail on a set row.
2) **VM** `logStrengthSet(...)` → **Repo** → **DAO** insert `SetLog`.
3) **UI** refresh header → repo reads `lastSets(...)` → last successful weight; suggest ≈ ×1.02.

### 3.7 Metcon timer (FOR_TIME) — **updated**
1) **UI** `MetconActivity` timer run; user selects **RX** or **Scaled**; taps **Complete**.
2) **VM** `logMetcon(day, seconds, result)`.
3) **Repo** currently delegates to `logMetcon(day, seconds)` (2‑arg); creates session and inserts time‑only `SetLog`.
4) **VM** updates `lastMetconSeconds` via repo’s summary; UI shows last time (no result yet).

### 3.8 Metcon plan flows (library UI pending)
1) **List** `metconPlans()` in future library.
2) **Add to day** → `addMetconToDay(day, planId, required, order)` → `ProgramMetconSelection` write.
3) **Render** `WorkoutActivity` via `metconsForDay(day)`: plan title + ordered components.

### 3.9 Home summary labels
1) **UI** `MainActivity.onResume` → repo `daySummaryLabel(day)`.
2) **DAO** `distinctTypesForDay(day)` → label `Empty` | `Mixed` | single type.

### 3.10 Initial seeding
1) **App start** `WorkoutApp.onCreate` → `AppDatabase.get(...)`.
2) **DB onCreate** seeds **exercises** and **metcon plans** (new).

### 3.11 Settings (current)
1) **UI** renders inputs only.
2) **Persistence** to `UserSettings` table not wired yet.

---

## 4. Change Impact Matrix (active)

| Change | Files touched | Upstream deps | Downstream dependants | Risks | Status |
|---|---|---|---|---|---|
| Introduce Metcon plans (plans/components/selection) | `MetconPlan.kt`, `MetconComponent.kt`, `ProgramMetconSelection.kt`, `MetconDao.kt`, `AppDatabase.kt`, `Converters.kt`, `MetconSeed.kt`, `Repos.kt`, `WorkoutRepository.kt` | Room, enums | VM/UI (future) | DB version bump; destructive migration in dev | **Done** |
| RX/Scaled selection in Metcon UI | `activity_metcon.xml`, `MetconActivity.kt`, `WorkoutViewModel.kt`, `WorkoutRepository.kt` | Enums | SessionDao (read), UI | **Not persisted** → UI shows time only | **Done (UI + API)** |
| Persist `metconResult` on logs | `SetLog.kt`, `Converters.kt`, `AppDatabase.kt` (v4 + migration), `SessionDao` queries, `WorkoutRepository.logMetcon(...)` | Room | VM/UI | Schema change + migration | **Next** |
| Async last‑weight fetch in day cards | `WorkoutActivity.kt` | — | Day UI | Remove `runBlocking` → smoother UI | Planned |
| AMRAP/EMOM scoring inputs | Metcon UIs + VM + Repo | `MetconType` | Day/History views | UX + model changes | Planned |

---

## 5. Open Questions

- **Where to store metcon result?** Extend `SetLog` with nullable `metconResult` vs a dedicated `MetconResultLog` table. (Current plan: extend `SetLog`.)
- **Metcon equipment default?** Use `BODYWEIGHT` vs `BARBELL` to avoid skewing equipment‑based queries.
- **Async in `WorkoutActivity`?** Replace `runBlocking` with coroutine/Flow to avoid UI jank.
- **RepScheme default**: Is R8 the correct fallback for unknown reps? Confirm.

---

## 6. Next Actions (one at a time)

1. **Persist RX/Scaled**: add `metconResult: MetconResult?` to `SetLog`, converters, v4 migration, update insert + summary → UI shows last time **and** result.
2. **Metcon plan library UI**: list `metconPlans()` and allow add/remove + ordering/required.
3. **Make day cards async**: replace `runBlocking` with `lifecycleScope` + suspend repo call.

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
| `ExerciseDao.kt` | Basic persistence for `Exercise`. | `count()`, `insertAll(List<Exercise>)` | Room, `Exercise` | Seeds, repo init checks | Simple CRUD.
| `LibraryDao.kt` | Query/filter exercises by type/equipment; insert and count. | `getExercises(type, eq): Flow<List<Exercise>>`, `countExercises()`, `insertAll(...)` | Room, `Exercise`, `WorkoutType`, `Equipment` | Library UI | NULL params mean no filter.
| `PersonalRecordDao.kt` | Manage `PersonalRecord`; best record lookup. | `upsert(PersonalRecord)`, `bestForExercise(exerciseId)` | Room, `PersonalRecord` | PR tracking | Best by (value desc, date desc).
| `ProgramDao.kt` | Strength program selections + joined reads. | `getProgramForDay(day)` → `ExerciseWithSelection`; `upsert`, `remove`; setters for `required`, `preferredEquipment`, `targetReps`; `exists`; `distinctTypesForDay` | Room, `ProgramSelection`, `Exercise`, `Equipment` | Program/day views | DTO `ExerciseWithSelection(exercise, required, preferredEquipment, targetReps)`.
| `SessionDao.kt` | Sessions + set logs; recent queries; metcon summaries. | `insertSession`, `insertSet`, `lastSets(...)`, `lastMetconSecondsForDay(day)` | Room, `WorkoutSession`, `SetLog`, `Equipment` | Strength logging, metcon last‑time | **Result type (RX/Scaled) not persisted yet**; summary shows time only.
| **`MetconDao.kt`** | Metcon plans & day mapping (**new**). | `getAllPlans(): Flow<List<MetconPlan>>`; `getMetconsForDay(day): Flow<List<SelectionWithPlanAndComponents>>`; `upsertSelection(...)`; `removeSelection(...)`; `setRequired(...)`; `setDisplayOrder(...)` | `MetconPlan`, `MetconComponent`, `ProgramMetconSelection` | Future metcon library UI + day view | Supports multiple metcons/day + ordering.

### 2.3 data/db/

| File | Responsibility | Key API / Settings | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `AppDatabase.kt` | Central Room DB (**version 3**). | `@Database(..., version=3)`, DAOs: Library, Program, Session, PR, **MetconDao**; `@TypeConverters(Converters)`; `onCreate` seeds **exercises + metcon plans** | Room, Seeds | Global persistence; `Repos`, `WorkoutApp` | Dev: `fallbackToDestructiveMigration()` enabled.
| `Converters.kt` | Room enum converters. | `WorkoutType↔String`, `Equipment↔String` (optionally `MetconResult↔String` when persisted) | Core enums | AppDatabase | Keep names in sync with enum constants.

### 2.4 data/entities/

| File | Table | Fields (key) | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `Exercise.kt` | `exercise` | `id`, `name`, `workoutType`, `primaryEquipment`, `modality`, `isUnilateral` | `WorkoutType`, `Equipment`, `Modality` | DAOs, program logic, UI | Seeded on first run.
| `PersonalRecord.kt` | `personal_record` | `id`, `exerciseId(FK)`, `recordType`, `value`, `date`, `notes` | Room, FK → `Exercise` | PR DAO | Cascade delete with exercise.
| `ProgramSelection.kt` | `ProgramSelection` | `id`, `dayIndex`, `exerciseId`, `required`, `preferredEquipment`, `targetReps` | `Equipment` | Program DAO | Nullable `targetReps`.
| `SetLog.kt` | `SetLog` | `id`, `sessionId`, `exerciseId`, `equipment`, `setNumber`, `reps`, `weight`, `timeSeconds`, `rpe`, `success`, `notes` | `Equipment` | Session DAO | **No `metconResult` column yet**; time‑only metcons use `exerciseId=0L`.
| `WorkoutSession.kt` | `WorkoutSession` | `id`, `dayIndex`, `startTs` | — | Session DAO | Indexed by `dayIndex`, `startTs`.
| `UserSettings.kt` | `user_settings` | `id=1`, `darkTheme`, `autoWeightIncrement`, `defaultRestTime`, `units`, `showPersonalRecords` | — | Settings UI (future wiring) | Secondary `@Ignore` ctor with slightly different defaults.
| **`MetconPlan.kt`** | `MetconPlan` (**new**) | `id`, `title`, `type: MetconType`, `durationMinutes?`, `emomIntervalSec?` | Core enums | Metcon DAO/Repo | Defines plan (FOR_TIME / AMRAP / EMOM).
| **`MetconComponent.kt`** | `MetconComponent` (**new**) | `id`, `planId(FK)`, `orderInPlan`, `text` | `MetconPlan` | Metcon DAO | Human‑readable components.
| **`ProgramMetconSelection.kt`** | `ProgramMetconSelection` (**new**) | `id`, `dayIndex`, `planId(FK)`, `required`, `displayOrder` | `MetconPlan` | Metcon DAO/Repo | Multiple metcons per day + ordering.

### 2.5 data/repo/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `Repos.kt` | Factory for repositories backed by DB DAOs. | `workoutRepository(context)` | `AppDatabase`, DAOs | Application/service layer | Now wires **MetconDao** as well.
| `WorkoutRepository.kt` | Orchestrates DAOs; light business logic. | **Library**: `getExercises`, `countExercises`. **Program**: `addToDay`, `programForDay`, `setRequired`, `setTargetReps`, `removeFromDay`, `isInProgram`, `selectedTargetReps`, `requiredFor`, `daySummaryLabel`. **Sessions**: `startSession`, `logStrengthSet`, `logTimeOnlySet`, `logMetcon(day, seconds)`; **overload** `logMetcon(day, seconds, result: MetconResult)` (calls 2‑arg version; **result not persisted yet**). **Queries**: `lastMetconSecondsForDay`, (optional) `lastMetconForDay()`. **Metcon plans**: `metconPlans`, `metconsForDay`, `addMetconToDay`, `removeMetconFromDay`, `setMetconRequired`, `setMetconOrder`. | All DAOs, entities, enums | VM/UI | Suggested weight ≈ last successful ×1.02; metcon equipment placeholder under discussion.

### 2.6 data/seed/

| File | Responsibility | Notes |
|---|---|---|
| `ExerciseSeed.kt` | Seed default `Exercise` rows on first run. | Triggered from DB `onCreate` if empty.
| **`MetconSeed.kt`** | Seed example `MetconPlan` + `MetconComponent`s. | Include FOR_TIME, AMRAP, EMOM examples.

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
| `LibraryViewModel.kt` | Filters + list of `Exercise`. | `exercises: LiveData<List<Exercise>>` | MediatorLiveData over Flow.
| `WorkoutViewModel.kt` | Day state + logging. | `programForDay: LiveData<List<ExerciseWithSelection>>`, `lastMetconSeconds: LiveData<Int>` | `setDay(day)` loads last metcon seconds; provides `logStrengthSet`, `logMetcon`.
| `WorkoutViewModelFactory.kt` | Factory for `WorkoutViewModel`. | — | Standard pattern.

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

## 3. Data Flow Traces (end‑to‑end)

### 3.1 Add exercise to a day (Library → Program)
1) **UI** `ExerciseLibraryActivity` row → **Add**.
2) **Repo** `addToDay(day, exercise, required, preferred, targetReps)`.
3) **DAO** `ProgramDao.upsert(ProgramSelection)`.
4) **VM** `programForDay(day)` Flow re‑emits.
5) **UI** `WorkoutActivity` re‑renders sections.

### 3.2 Remove exercise from a day
1) **UI** row → **Remove**.
2) **Repo** `removeFromDay(day, exerciseId)` → **DAO** delete.
3) **Flow** emits; day view updates.

### 3.3 Toggle “required” flag
1) **UI** star toggle.
2) **Repo** `setRequired(day, exerciseId, newVal)` → **DAO** update.
3) **Flow** emits; card moves Required↔Optional.

### 3.4 Change target reps
1) **UI** reps spinner.
2) **Repo** `setTargetReps(day, exerciseId, reps)` (if already added) → **DAO** update.
3) **Flow** emits; labels update.

### 3.5 Start a workout session (strength)
1) **UI** open `WorkoutActivity` → **Repo** `startSession(day)` → sessionId.
2) **UI** opens `ExerciseDetailActivity` with extras.

### 3.6 Log a strength set
1) **UI** success/fail on a set row.
2) **VM** `logStrengthSet(...)` → **Repo** → **DAO** insert `SetLog`.
3) **UI** refresh header → repo reads `lastSets(...)` → last successful weight; suggest ≈ ×1.02.

### 3.7 Metcon timer (FOR_TIME) — **updated**
1) **UI** `MetconActivity` timer run; user selects **RX** or **Scaled**; taps **Complete**.
2) **VM** `logMetcon(day, seconds, result)`.
3) **Repo** currently delegates to `logMetcon(day, seconds)` (2‑arg); creates session and inserts time‑only `SetLog`.
4) **VM** updates `lastMetconSeconds` via repo’s summary; UI shows last time (no result yet).

### 3.8 Metcon plan flows (library UI pending)
1) **List** `metconPlans()` in future library.
2) **Add to day** → `addMetconToDay(day, planId, required, order)` → `ProgramMetconSelection` write.
3) **Render** `WorkoutActivity` via `metconsForDay(day)`: plan title + ordered components.

### 3.9 Home summary labels
1) **UI** `MainActivity.onResume` → repo `daySummaryLabel(day)`.
2) **DAO** `distinctTypesForDay(day)` → label `Empty` | `Mixed` | single type.

### 3.10 Initial seeding
1) **App start** `WorkoutApp.onCreate` → `AppDatabase.get(...)`.
2) **DB onCreate** seeds **exercises** and **metcon plans** (new).

### 3.11 Settings (current)
1) **UI** renders inputs only.
2) **Persistence** to `UserSettings` table not wired yet.

---

## 4. Change Impact Matrix (active)

| Change | Files touched | Upstream deps | Downstream dependants | Risks | Status |
|---|---|---|---|---|---|
| Introduce Metcon plans (plans/components/selection) | `MetconPlan.kt`, `MetconComponent.kt`, `ProgramMetconSelection.kt`, `MetconDao.kt`, `AppDatabase.kt`, `Converters.kt`, `MetconSeed.kt`, `Repos.kt`, `WorkoutRepository.kt` | Room, enums | VM/UI (future) | DB version bump; destructive migration in dev | **Done** |
| RX/Scaled selection in Metcon UI | `activity_metcon.xml`, `MetconActivity.kt`, `WorkoutViewModel.kt`, `WorkoutRepository.kt` | Enums | SessionDao (read), UI | **Not persisted** → UI shows time only | **Done (UI + API)** |
| Persist `metconResult` on logs | `SetLog.kt`, `Converters.kt`, `AppDatabase.kt` (v4 + migration), `SessionDao` queries, `WorkoutRepository.logMetcon(...)` | Room | VM/UI | Schema change + migration | **Next** |
| Async last‑weight fetch in day cards | `WorkoutActivity.kt` | — | Day UI | Remove `runBlocking` → smoother UI | Planned |
| AMRAP/EMOM scoring inputs | Metcon UIs + VM + Repo | `MetconType` | Day/History views | UX + model changes | Planned |

---

## 5. Open Questions

- **Where to store metcon result?** Extend `SetLog` with nullable `metconResult` vs a dedicated `MetconResultLog` table. (Current plan: extend `SetLog`.)
- **Metcon equipment default?** Use `BODYWEIGHT` vs `BARBELL` to avoid skewing equipment‑based queries.
- **Async in `WorkoutActivity`?** Replace `runBlocking` with coroutine/Flow to avoid UI jank.
- **RepScheme default**: Is R8 the correct fallback for unknown reps? Confirm.

---

## 6. Next Actions (one at a time)

1. **Persist RX/Scaled**: add `metconResult: MetconResult?` to `SetLog`, converters, v4 migration, update insert + summary → UI shows last time **and** result.
2. **Metcon plan library UI**: list `metconPlans()` and allow add/remove + ordering/required.
3. **Make day cards async**: replace `runBlocking` with `lifecycleScope` + suspend repo call.

