# WorkoutApp — Technical Map (Working Draft)

> Living document mapping files → responsibilities → references. Updated incrementally as files are reviewed.

---

## 0. Repo & Build

- **Repo**: A-Addinall/WorkoutApp (URL pending)
- **Default branch**: master (to confirm)
- **Module(s)**: `app`
- **Language**: Kotlin
- **Min/Target/Compile SDK**: (fill)
- **DI**: (Hilt/Koin/manual — to confirm)
- **Persistence**: Room database v1, with type converters for enums.

---

## 1. App Structure (high-level)

```
com.example.safitness
├─ core/
├─ data/
│  ├─ dao/
│  ├─ db/
│  ├─ entities/
│  └─ repo/
├─ seed/
├─ domain/
├─ ui/
├─ viewmodel/
└─ WorkoutApp.kt
```

---

## 2. Package Details

### 2.1 core/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `Enums.kt` | Central domain enums for workouts, equipment, modality, and rep schemes. Provides helper to map raw reps → enum. | `enum class WorkoutType`, `enum class Equipment`, `enum class Modality`, `enum class RepScheme(val reps:Int)` with `fromReps` helper. | Kotlin stdlib only. | Likely used by entities, DAOs, repos, and UI for type-safety. | `RepScheme.fromReps` returns `R8` as fallback; ensure default is semantically correct.

### 2.2 data/dao/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `ExerciseDao.kt` | Basic persistence for `Exercise` entity. | `count()`, `insertAll(List<Exercise>)`. | Room, `Exercise` entity. | Seeding, repository initialisation checks. | Simple CRUD, no filtering.
| `LibraryDao.kt` | Query/filter exercises by type and equipment; insert and count. | `getExercises(type, eq): Flow<List<Exercise>>`, `countExercises()`, `insertAll`. | Room, `Exercise`, `WorkoutType`, `Equipment`. | Library UI, filters, initial load. | NULL parameters allow broad queries.
| `PersonalRecordDao.kt` | Manage `PersonalRecord` entries and query best record. | `upsert(PersonalRecord)`, `bestForExercise(exerciseId)`. | Room, `PersonalRecord`. | PR tracking features. | Returns best record sorted by value and date.
| `ProgramDao.kt` | Manage and query `ProgramSelection` linked with `Exercise` for a given day. | `getProgramForDay(day)`, `upsert`, `remove`, setters for required/equipment/reps, existence checks, type summaries. | Room, `Exercise`, `ProgramSelection`, `WorkoutType`, `Equipment`. | Program building screens, daily view. | `ExerciseWithSelection` DTO used for joined queries.
| `SessionDao.kt` | Manage `WorkoutSession` and `SetLog` entries; query last sets and metcon times. | `insertSession`, `insertSet`, `lastSets(...)`, `lastMetconSecondsForDay(day)`. | Room, `WorkoutSession`, `SetLog`, `Equipment`. | Session logging, history, metcon summary UI. | `lastSets` can filter by reps or return last N entries.

### 2.3 data/db/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `AppDatabase.kt` | Central Room database definition. Manages DAOs, entity list, version, type converters, and initial seeding. | `get(context)`, `seedInitialExercises()`. | Room, all DAO interfaces, all entities, `ExerciseSeed`. | Global persistence access, invoked on app startup. | Seeds default exercises on first creation if table empty. Uses `fallbackToDestructiveMigration` (dev-friendly but destructive on schema change).
| `Converters.kt` | Room type converters for enum persistence. | `toWorkoutType`, `fromWorkoutType`, `toEquipment`, `fromEquipment`. | Room, `WorkoutType`, `Equipment`. | Referenced by `AppDatabase` via `@TypeConverters`. | Stores enums as string names; must match enum constant names exactly.

### 2.4 data/entities/

| File | Responsibility | Fields | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `Exercise.kt` | Core exercise definition. | `id`, `name`, `workoutType`, `primaryEquipment`, `modality`, `isUnilateral`. | Room, `WorkoutType`, `Equipment`, `Modality`. | DAOs, program logic, UI. | Indexed table name `exercise`.
| `PersonalRecord.kt` | Personal record log for exercises. | `id`, `exerciseId`, `recordType`, `value`, `date`, `notes`. | Room, foreign key to `Exercise`. | `PersonalRecordDao`, PR displays. | Cascades delete with exercise.
| `ProgramSelection.kt` | Selection of exercises for program days. | `id`, `dayIndex`, `exerciseId`, `required`, `preferredEquipment`, `targetReps`. | Room, `Equipment`. | `ProgramDao`. | Nullable target reps for flexibility.
| `SetLog.kt` | Logged details for each set in a session. | `id`, `sessionId`, `exerciseId`, `equipment`, `setNumber`, `reps`, `weight`, `timeSeconds`, `rpe`, `success`, `notes`. | Room, `Equipment`. | `SessionDao`. | Supports weight-based and time-based sets.
| `UserSettings.kt` | Persistent user configuration. | `id`, `darkTheme`, `autoWeightIncrement`, `defaultRestTime`, `units`, `showPersonalRecords`. | Room. | Settings UI, app startup config. | Has `@Ignore` secondary constructor with slightly different defaults.
| `WorkoutSession.kt` | Summary of a workout session. | `id`, `dayIndex`, `startTs`. | Room. | `SessionDao`. | Indexed by `dayIndex` and `startTs` for query performance.

### 2.5 data/repo/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `Repos.kt` | Factory for creating repositories with DB-backed DAOs. | `workoutRepository(context)`. | `AppDatabase`, `WorkoutRepository`. | Application/service layer. | Encapsulates DAO wiring.
| `WorkoutRepository.kt` | Main repository handling library, program, session, and PR features. Orchestrates DAO calls. | Library ops: `getExercises`, `countExercises`; Program ops: `addToDay`, `programForDay`, `setRequired`, `setTargetReps`, `removeFromDay`, `isInProgram`, `selectedTargetReps`, `requiredFor`, `daySummaryLabel`; Session ops: `startSession`, `logStrengthSet`, `logTimeOnlySet`, `logMetcon`, `lastMetconSecondsForDay`; PR ops: `bestPR`, `getLastSuccessfulWeight`, `getSuggestedWeight`. | All DAOs, domain entities, `Equipment`, `WorkoutType`. | ViewModels, UI controllers. | Implements light business logic (e.g., day summary naming, suggested weight calculation). Some constants (e.g., metcon equipment choice) are hardcoded.

### 2.6 data/seed/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `ExerciseSeed.kt` | Holds canonical default list of exercises; seeds DB on first run if empty. | `DEFAULT_EXERCISES` list, `seedIfEmpty(db)` suspending function. | `Exercise`, `WorkoutType`, `Equipment`, `Modality`, `AppDatabase`. | `AppDatabase` creation callback, `seedInitialExercises`. | Uses named args for clarity; covers Push/Pull/Legs_Core/Metcon categories with representative exercises.

### 2.7 ui/

| File | Responsibility | Key API | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `ExerciseLibraryActivity.kt` | UI for browsing and filtering exercises; allows adding/removing to program, setting reps, marking required. | Spinner filters, adapter with `LibraryAdapter`, onClick for add/remove/required toggle. | Android UI widgets, `LibraryViewModel`, `WorkoutRepository`, `Repos`, `Equipment`, `WorkoutType`. | Accessed from `MainActivity` and likely program setup flows. | Maintains internal state maps for added, reps, required; fetches from repo on list update.
| `LibraryViewModel.kt` | Holds filters and exposes `exercises` LiveData based on repo query. | `setTypeFilter`, `setEqFilter`. | `WorkoutRepository`, Kotlin Flow → LiveData. | `ExerciseLibraryActivity`. | MediatorLiveData used to trigger reloads on filter change.
| `LibraryViewModelFactory.kt` | Factory for creating `LibraryViewModel` with repo. | `create(modelClass)`. | `WorkoutRepository`. | Passed to `viewModels` in `ExerciseLibraryActivity`. | Standard ViewModel factory pattern.
| `MainActivity.kt` | App dashboard; navigates to days, exercise library, PRs, settings; updates day labels from repo. | `openDay`, `refreshDayLabels`. | `Repos`, `WorkoutActivity`, `ExerciseLibraryActivity`, `PersonalRecordsActivity`, `SettingsActivity`. | Launcher activity. | Uses `MainScope` to fetch labels in background.
| `MetconActivity.kt` | Manages Metcon workout execution with timer; logs completion time via repo. | `startTimer`, `stopTimer`, `resetTimer`, `completeMetcon`. | `WorkoutViewModel`, `WorkoutViewModelFactory`, `Repos`, `Modality`. | Launched from `WorkoutActivity` metcon card. | Uses `CountDownTimer`; filters exercises by Modality.METCON.
| `PersonalRecordsActivity.kt` | Placeholder for PR view. | None beyond layout binding. | Android UI. | Launched from `MainActivity`. | Currently minimal.
| `WorkoutActivity.kt` | Displays program for a day; sections: required, optional, metcon; opens detail screens; fetches last weight from repo. | `render`, `addSection`, `addExerciseCard`, `addMetconCard`. | `WorkoutViewModel`, `WorkoutViewModelFactory`, `Repos`, `ExerciseDetailActivity`, `MetconActivity`. | From `MainActivity`. | Uses `runBlocking` for last weight fetch—potential UI thread block.
| `WorkoutViewModel.kt` | Manages program list and set logging for a day; exposes LiveData for program and last metcon time. | `setDay`, `logStrengthSet`, `logMetcon`, `getLastSuccessfulWeight`, `getSuggestedWeight`. | `WorkoutRepository`, `ExerciseWithSelection`, `Equipment`. | `WorkoutActivity`, `MetconActivity`. | Updates last metcon seconds after log.
| `WorkoutViewModelFactory.kt` | Factory for `WorkoutViewModel`. | `create(modelClass)`. | `WorkoutRepository`. | `WorkoutActivity`, `MetconActivity`. | Standard ViewModel factory.

### 2.8 root/

| File            | Responsibility                                                                                        | Key API                                            | Depends on                            | Referenced by                                                  | Notes                                                                                                                               |
| --------------- | ----------------------------------------------------------------------------------------------------- | -------------------------------------------------- | ------------------------------------- | -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `WorkoutApp.kt` | Application subclass providing global access to `AppDatabase` instance. Initialises DB on app launch. | `onCreate()` assigns `db = AppDatabase.get(this)`. | Android `Application`, `AppDatabase`. | Any component needing DB instance; indirectly through `Repos`. | No explicit repository or ViewModel initialisation; relies on on-demand creation. No manual seeding — handled inside `AppDatabase`. |

### 2.9 res/drawable/

| File                         | Responsibility | Key Elements | Depends on | Referenced by | Notes |
|---|---|---|---|---|---|
| `ic_arrow_back.xml` | Vector drawable for back navigation arrow. | Path data for left arrow; tinted via `?attr/colorOnSurface`; white fill. | Android vector drawable API. | Likely used in toolbar/action bar navigation. | Standard Material-style arrow; size 24dp. 【174†source】 |
| `ic_launcher_background.xml` | Vector background for launcher icon. | Green base (#3DDC84) with white grid overlay lines. | Android vector drawable API. | Combined with `ic_launcher_foreground.xml` for adaptive icon. | Generated by Android Studio default template. 【175†source】 |
| `ic_launcher_foreground.xml` | Vector foreground for launcher icon. | Gradient overlay path + white dumbbell/weight-like icon. | Android vector drawable API. | Launcher icon. | Standard template with slight theming; layered over background. 【176†source】 |

### 2.10 res/layout/

| File                                 | Responsibility                                                            | Key Elements                                                                                   | Depends on                           | Referenced by                                   | Notes |
| ------------------------------------ | -------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- | ------------------------------------ | ------------------------------------------------ | ----- |
| `_main_day_card.xml`                 | Card layout representing a single day in the program list.                 | `CardView` with rounded corners; horizontal `LinearLayout` containing day title and arrow.     | AndroidX CardView, LinearLayout, TextView | `MainActivity` day list adapter                  | Displays `tvDayTitle`; arrow as static symbol. |
| `activity_exercise_detail.xml`       | UI for detailed view of a single exercise.                                 | Header with back button and title, last/suggested weight, sets container, notes, complete btn. | ConstraintLayout, LinearLayout, ScrollView | `ExerciseDetailActivity`                         | Focused on exercise tracking and notes. |
| `activity_exercise_library.xml`      | Library browsing UI with filters.                                          | Filters via Spinners, clear filters button, ListView, empty state message.                     | LinearLayout, Spinner, ListView        | `ExerciseLibraryActivity`                        | Includes type, equipment, and day filters. |
| `activity_main.xml`                  | Main app dashboard UI.                                                     | Title, workout day cards, sections for library, progress, and settings.                        | ConstraintLayout, CardView, include     | `MainActivity`                                   | Serves as navigation hub. |
| `activity_metcon.xml`                | UI for Metcon timer workout mode.                                          | Header, timer display, last time, start/stop/reset buttons, exercise list, complete btn.       | ConstraintLayout, LinearLayout, Button  | `MetconActivity`                                 | For timed workouts with results tracking. |
| `activity_placeholder.xml`           | Placeholder UI for future features.                                        | Header with back button, centred "Coming Soon" message.                                        | ConstraintLayout, LinearLayout, ImageView | Placeholder screens                              | Used for unimplemented sections. |
| `activity_settings.xml`              | UI for adjusting app settings.                                             | Dark theme toggle, weight increment, rest time, units spinner, show PR toggle, save btn.       | ScrollView, LinearLayout, Switch, EditText | `SettingsActivity`                              | Covers app preferences and units. |
| `activity_workout.xml`               | UI for an active workout session.                                          | Header, scrollable list of exercises.                                                          | ConstraintLayout, LinearLayout, ScrollView | `WorkoutActivity`                                | Displays workout content dynamically. |
| `item_enhanced_progress_card.xml`    | Card showing extended progress stats for an exercise.                      | Exercise name, current max, trend, success rate, total volume.                                 | CardView, LinearLayout, TextView        | Progress UI components                           | For enhanced analytics display. |
| `item_exercise_card.xml`             | Card view for listing exercises.                                           | Exercise name, rep range, last weight.                                                          | CardView, LinearLayout, TextView        | Library or workout lists                         | Compact exercise summary. |
| `item_library_row.xml`               | Row layout for an exercise in the library with controls.                   | Exercise title, metadata, equipment, rep spinner, required toggle, add/remove button.          | LinearLayout, Spinner, Button, ImageButton | `ExerciseLibraryActivity` list adapter           | Supports selection, rep adjustment, marking as required. 【230†item_library_row.xml】 |
| `item_metcon_card.xml`               | Card for displaying a metcon workout summary.                              | Title, dynamic exercise list container, last recorded time.                                    | CardView, LinearLayout, TextView         | `WorkoutActivity` metcon section                 | Displays summary and access to detailed view. 【231†item_metcon_card.xml】 |
| `item_metcon_exercise.xml`           | Card for individual exercises in a metcon workout.                         | Bullet marker, exercise name, rep range.                                                       | CardView, LinearLayout, TextView         | Inside `item_metcon_card` dynamic list            | Compact listing for metcon components. 【232†item_metcon_exercise.xml】 |
| `item_progress_card.xml`             | Compact card for progress tracking of an exercise.                         | Exercise name, current max, trend, success rate.                                                | CardView, LinearLayout, TextView         | Progress overview UI                             | Lighter-weight version of enhanced progress card. 【233†item_progress_card.xml】 |
| `item_set_entry.xml`                 | Card for entering results for a workout set.                               | Set number, editable weight, fixed reps, success/fail buttons.                                 | CardView, LinearLayout, EditText, Button | `ExerciseDetailActivity` set logging             | Supports marking set outcome and weight entry. 【234†item_set_entry.xml】 |

### 2.11 res/values/

| File          | Responsibility                                      | Key Elements / Styles / Values                                                                                                         | Depends on                    | Referenced by                                   | Notes |
| ------------- | --------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------- | ------------------------------------------------ | ----- |
| `colors.xml`  | Defines app-wide colour palette, including brand and status colours. | Purples (`purple_200`, `purple_500`, `purple_700`), teals (`teal_200`, `teal_700`), black, white. Custom: `button_default` (grey), `button_success` (green), `button_fail` (red). 【246†colors.xml】 | Android colour resource system | All UI components referencing `@color/...`                  | Supports Material theming and custom button states. |
| `strings.xml` | Centralised string values for app text.              | App name, navigation labels (`back`, `start_workout`, `exercise_library`, `personal_records`, `settings`), action labels (`complete_exercise`, `add_set`). 【247†strings.xml】 | Android string resource system | Activities, layouts, adapters                             | Improves localisation and consistency. |
| `styles.xml`  | Defines reusable text appearance styles.             | `Title20.Bold`, `Body16`, `Caption12.Muted`, `Title20`, `Caption12`. Sizes from 12sp–20sp; purple or black text; some bold. 【248†styles.xml】 | Android style system           | Layout XML text elements via `style` attribute             | Encourages design consistency; some duplication (`Title20.Bold` and `Title20`). |
| `themes.xml`  | Declares app theme inheritance.                      | `Theme.SAFitness` extends `Theme.Material3.Light.NoActionBar`. 【249†themes.xml】                                                        | Material3 theming              | Declared in `AndroidManifest.xml` as app theme | Minimal; inherits Material 3 defaults. |

### 2.12 AndroidManifest.xml

| Element        | Responsibility                                           | Key Attributes / Values                                                               | Depends on                      | Referenced by         | Notes |
| -------------- | -------------------------------------------------------- | -------------------------------------------------------------------------------------- | -------------------------------- | --------------------- | ----- |
| `<manifest>`   | Declares app package and manifest root.                   | `package="com.example.safitness"`                                                     | Android app build system         | Gradle build config   | Package name defines app ID. |
| `<application>`| Declares global application settings and components.      | `android:name=".WorkoutApp"`, `allowBackup=true`, `label="SA Fitness"`, `icon=@mipmap/ic_launcher`, `theme=@style/Theme.SAFitness` | `WorkoutApp` Application class   | All activities        | Theme defined in `themes.xml`. |
| `<activity>`   | Registers activities with their launch/export settings.   | **ExerciseDetailActivity** (`exported=false`), **WorkoutActivity** (`false`), **SettingsActivity** (`false`), **PersonalRecordsActivity** (`false`), **ExerciseLibraryActivity** (`false`), **MetconActivity** (`false`), **MainActivity** (`exported=true`, launcher intent-filter) | Android Activity lifecycle       | Navigation flows      | `MainActivity` is launcher with `MAIN`/`LAUNCHER` intent filter; others internal only. |


## 4. Data Flow Traces (end‑to‑end)

### 4.1 Add exercise to a day (Library → Program)
1) **UI** `ExerciseLibraryActivity` row click → `btnPrimary` → coroutine.
2) **Repo** `WorkoutRepository.addToDay(day, exercise, required, preferred, targetReps)`.
3) **DAO** `ProgramDao.upsert(ProgramSelection)` → Room writes to `ProgramSelection` table.
4) **VM refresh** `WorkoutViewModel.setDay(day)` already observing → `programForDay` Flow emits via `ProgramDao.getProgramForDay(day)` join with `Exercise`.
5) **UI** `WorkoutActivity` observer renders new Required/Optional sections.

### 4.2 Remove exercise from a day
1) **UI** `ExerciseLibraryActivity` → `btnPrimary` when already added.
2) **Repo** `WorkoutRepository.removeFromDay(day, exerciseId)`.
3) **DAO** `ProgramDao.remove(day, exerciseId)` delete.
4) **Flow** `getProgramForDay(day)` re‑emits → `WorkoutActivity` re‑renders.

### 4.3 Toggle “required” flag
1) **UI** `ExerciseLibraryActivity` → `btnRequired`.
2) **Repo** `WorkoutRepository.setRequired(day, exerciseId, newVal)`.
3) **DAO** `ProgramDao.setRequired(...)` update.
4) **Flow** emits updated `ExerciseWithSelection.required` → affected card moves between Required/Optional.

### 4.4 Change target reps for a selected exercise
1) **UI** library row `spinnerReps` selection.
2) If already added → **Repo** `setTargetReps(day, exerciseId, chosen)`.
3) **DAO** `ProgramDao.setTargetReps(...)`.
4) **Flow** updates → `WorkoutActivity` shows new rep label; detail screen pre‑fills reps.

### 4.5 Start a workout session (strength days)
1) **UI** `WorkoutActivity.onCreate` launches IO → **Repo** `startSession(day)`.
2) **DAO** `SessionDao.insertSession(WorkoutSession)` returns `sessionId`.
3) **UI** opens `ExerciseDetailActivity` for a card with extras (`SESSION_ID`, `EXERCISE_ID`, `TARGET_REPS`, `EQUIPMENT`).

### 4.6 Log a strength set
1) **UI** `ExerciseDetailActivity` → set row success/fail.
2) **VM** `WorkoutViewModel.logStrengthSet(...)`.
3) **Repo** `logStrengthSet(...)` builds `SetLog(weight, reps, rpe, success, notes)`.
4) **DAO** `SessionDao.insertSet(SetLog)` persists.
5) **Suggestion refresh** `ExerciseDetailActivity.refreshHeader()` calls `vm.getLastSuccessfulWeight(...)` and `vm.getSuggestedWeight(...)`.
6) **Repo** reads recent sets via `SessionDao.lastSets(...)` and returns last successful weight; computes suggested ≈ last × 1.02.
7) **UI** updates "Last" and "Suggested" labels.

## 4.7 Metcon timer flow (updated)
1) **UI** `WorkoutActivity` builds Metcon card; tap → `MetconActivity`.
2) **VM** `setDay(day)` → fetch `lastMetconSecondsForDay(day)` via Repo.
3) **User** runs timer → chooses result type ('RX' or 'Scaled') → complete → **VM** `logMetcon(day, seconds, resultType)`.
4) **Repo** `startSession(day)` then `logTimeOnlySet(sessionId, exerciseId=0L, timeSeconds=seconds, equipment=BARBELL, metconResult=resultType)`.
5) **DAO** `SessionDao.insertSession`, `insertSet` (now with `metconResult` column/field); `lastMetconSecondsForDay(day)` joins `WorkoutSession`↔`SetLog` to read most recent time and type.
6) **VM** updates `lastMetconSeconds` and `lastMetconResult` LiveData → UI shows "Last time" and result type.

### 4.8 Day summary labels on Home
1) **UI** `MainActivity.onResume` → for day 1..5 call **Repo** `daySummaryLabel(day)`.
2) **Repo** `ProgramDao.distinctTypesForDay(day)` → infer label: `Empty` | `Mixed` | capitalised single type.
3) **UI** writes text into each included `_main_day_card` (`tvDayTitle`).

### 4.9 Initial data seeding
1) **App start** `WorkoutApp.onCreate` → `AppDatabase.get(context)`.
2) **DB callback** `onCreate` launches IO: if `LibraryDao.countExercises()==0` then `insertAll(ExerciseSeed.DEFAULT_EXERCISES)`.
3) **Library** screens then stream from populated `Exercise` table via `LibraryDao.getExercises(...)`.

### 4.10 Settings screen (current)
1) **UI** `SettingsActivity` renders layout only.
2) **Data** No persistence wired yet; future: add `UserSettings` DAO + Repo functions; bind fields; save → upsert.

---

## 5. Change Impact Matrix (template)

| Proposed change | Files touched | Upstream deps | Downstream dependants | Risks | Tests |
|---|---|---|---|---|---|
| Log Metcons with 'RX' or 'Scaled' | `WorkoutViewModel`, `WorkoutRepository`, `SessionDao`, `SetLog` entity, `MetconActivity` UI | None new; reuses metcon logging flow | Progress views, PR records, analytics functions reading SetLog | DB schema change risk if stored as new field; UI logic changes; need to handle null/legacy entries | Unit test Repo logMetcon; instrument UI to log RX/Scaled and verify persistence |

---

## 6. Open Questions (delta)
- Should metcon placeholder `equipment` be `BODYWEIGHT` instead of `BARBELL` to avoid skewing queries by equipment?
- Replace `runBlocking` in `WorkoutActivity` with async fetch to avoid UI jank?
- Do we want PRs to influence suggested weight or keep simple 1.02× logic?

