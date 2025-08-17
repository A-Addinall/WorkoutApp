# WorkoutApp — Technical Map (Canonical)

> Single source of truth for structure, data model, flows, and active work. Updated for **Phase 0 planning model**, **PR pipeline**, and **Room v7 schema**.

---

## 0. Repo & Build
- **Module(s)**: `app`
- **Language**: Kotlin
- **Min/Target SDK**: (as per module gradle)
- **Persistence / DB**: Room **v7**
- **Migrations**: Dev uses destructive migrations (`fallbackToDestructiveMigration()`); proper migrations required before prod.
- **Seeding**: `ExerciseSeed`, `MetconSeed`, `DatabaseSeeder`, `DevPhaseSeed_dev`.

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
├─ ui/
└─ WorkoutApp.kt
```
> **Phase 0** planning tables live in `data/entities` with `PlanDao` in `data/dao`.

---

## 2. Package Details

### 2.1 core/
| File | Responsibility | Key API | Notes |
|---|---|---|---|
| `Enums.kt` | Domain enums | `WorkoutType`, `Equipment`, `Modality`, `MetconType`, `MetconResult` | `MetconResult` persisted via converters. |
| `StrengthFormulas.kt` | 1RM estimates | `estimateOneRepMax(weightKg,reps)`, `repsToPercentage(reps)` | Epley formula (≤12 reps). |
| `PrModels.kt` | PR events | `PrCelebrationEvent` (hard/soft PR info for UI) | Used by ExerciseDetail PR dialog. |

### 2.2 data/dao/
| File | Responsibility | Key API (selected) | Notes |
|---|---|---|---|
| `LibraryDao.kt` | Library queries | `getExercises(type, eq): Flow<List<Exercise>>`, `countExercises()`, `insertAll(...)` | — |
| `ExerciseDao.kt` | Exercise admin/seed helpers | `count()`, `insertAllIgnore(...)`, `insertAll(...)`, `getIdByName(name)`, `firstNIds(limit)` | Seed helpers. |
| `PersonalRecordDao.kt` | PR queries | `upsert`, `bestForExercise(exerciseId)`, `bestEstimated1RM(exId,eq)`, `bestWeightAtReps(exId,eq,reps)`, `upsertEstimated1RM`, `upsertRepMax` | New for PR pipeline. |
| `ProgramDao.kt` | Legacy day selections (strength) | `getProgramForDay(day)`, `upsert`, `remove`, `setRequired`, `setTargetReps`, `exists`, `distinctTypesForDay` | Fallback path. |
| `SessionDao.kt` | Legacy sessions & set logs | `insertSession`, `insertSet`, `lastSets(...)`, `lastMetconSecondsForDay`, `lastMetconForDay` | Back‑compat. |
| `MetconDao.kt` | Metcon library + logs | Plans: `getAllPlans()`, `getPlanWithComponents(planId)`; Day (legacy): `getMetconsForDay(day)`; Select: `upsertSelection/removeSelection/setRequired/setDisplayOrder`; Logs: `insertLog`, `lastForPlan`, `lastForDay`; Seed helpers: `insertPlansIgnore`, `getPlanIdByTitle/key`, `updatePlanByKey`, `insertComponentsIgnore`, `updateComponentText`, `deleteAllComponentsForPlan`, `deleteComponentsNotIn`; Misc: `firstPlanId()` | DTOs: `PlanWithComponents`, `SelectionWithPlanAndComponents`. |
| **`PlanDao.kt`** | **Phase 0 planning model** | Phase: `insertPhase`, `countPhases`, `currentPhaseId`; Week/Day plans: `insertWeekPlans`, `countPlans`, `getPlan(...)`, `getPlanId(...)`, `getPlanById`, `getPlansForPhaseOrdered`, `getNextAfter`; Day items: `insertItems`, `clearItems`, `countItemsForDay`, `flowDayStrengthFor(dayPlanId)`, `flowDayMetconsFor(dayPlanId)`; Calendar: `updatePlanDate` | Preferred source. |
| **`WorkoutSessionDao.kt`** | Phase-aware sessions | `insert`, `getById` | Uses `WorkoutSessionEntity` (phase/week/day indexes). |
| *MetconDao helper note* | | `firstPlanId()` optional | Seed reads from `getAllPlans()`; helper is optional. |

### 2.3 data/db/
| File | Responsibility | Key Settings | Notes |
|---|---|---|---|
| `AppDatabase.kt` | Room DB | `@Database(version = 7, exportSchema=false)`; entities include PR schema and Phase planning; DAOs include `planDao()`; seeds in `onCreate`/`onOpen` | Dev destructive migration on. |
| `Converters.kt` | Type converters | `WorkoutType↔String`, `Equipment↔String`, `MetconType↔String`, `MetconResult↔String` | — |

### 2.4 data/entities/
| File | Table | Fields (key) | Notes |
|---|---|---|---|
| `Exercise.kt` | `exercise` | `id`, `name`, `workoutType`, `primaryEquipment`, `modality`, `isUnilateral` | — |
| `ProgramSelection.kt` | `program_selection` | `id`, `dayIndex`, `exerciseId`, `required`, `preferredEquipment?`, `targetReps?` | Legacy day model. |
| `SetLog.kt` | `set_log` | `id`, `sessionId`, `exerciseId`, `equipment`, `setNumber`, `reps`, `weight?`, `timeSeconds?`, `rpe?`, `success?`, `notes?`, `metconResult?` | `metconResult` persisted. |
| `WorkoutSession.kt` | `workout_session` | `id`, `dayIndex`, `startTs` | Legacy session (still used). |
| **`WorkoutSessionEntity.kt`** | `workout_session` | `id`, `phaseId?`, `weekIndex?`, `dayIndex`, `startTs` | Phase‑aware struct (ensure only one `@Entity` maps the table at a time). |
| `PersonalRecord.kt` | `personal_record` | `id`, `exerciseId`, `recordType`, `value`, `date`, `notes?`, **`equipment`**, **`reps`** | Unique index `(exerciseId, equipment, recordType, reps)`. |
| `UserSettings.kt` | `user_settings` | UX prefs | — |
| `MetconPlan.kt` | `metcon_plan` | `id`, `canonicalKey`, `title`, `type`, `durationMinutes?`, `emomIntervalSec?`, `isArchived` | — |
| `MetconComponent.kt` | `metcon_component` | `id`, `planId(FK)`, `orderInPlan`, `text` | — |
| `ProgramMetconSelection.kt` | `program_metcon_selection` | `id`, `dayIndex`, `planId(FK)`, `required`, `displayOrder` | Legacy day model. |
| `MetconLog.kt` | `metcon_log` | `id`, `dayIndex`, `planId`, `type`, `durationSeconds`, `timeSeconds?`, `rounds?`, `extraReps?`, `intervalsCompleted?`, `result`, `createdAt`, `notes?` | Plan‑scoped results. |
| **`PhaseEntity.kt`** | `phase` | `id`, `name`, `startDateEpochDay`, `weeks` | New. |
| **`WeekDayPlanEntity.kt`** | `week_day_plan` | `id`, `phaseId`, `weekIndex`, `dayIndex`, `displayName?`, `dateEpochDay?` | Unique `(phaseId, weekIndex, dayIndex)`. |
| **`DayItemEntity.kt`** | `day_item` | `id`, `dayPlanId(FK)`, `itemType` (`STRENGTH`/`METCON`), `refId`, `required`, `sortOrder`, `targetReps?`, `prescriptionJson?` | Items attached to a day plan. |

### 2.5 data/repo/
| File | Responsibility | Key API | Notes |
|---|---|---|---|
| `Repos.kt` | Repo factory | `workoutRepository(context)` wires `planDao()` & DAOs | — |
| `WorkoutRepository.kt` | Orchestrates DAOs | **Library**: `getExercises`, `countExercises` • **Program**: `addToDay`, `programForDay` *(prefers Phase model via `PlanDao`, falls back to legacy)*, `setRequired`, `setTargetReps`, `removeFromDay`, `isInProgram`, `selectedTargetReps`, `requiredFor`, `daySummaryLabel` • **Sessions**: `startSession`, `logStrengthSet`, `logTimeOnlySet`, `logMetcon(...)`, `lastMetconSecondsForDay`, `lastMetconForDay`, `lastMetconDisplayForDay` • **Metcon plans**: `metconPlans`, `metconsForDay`, `logMetconForTime`, `logMetconAmrap`, `logMetconEmom`, `lastMetconForPlan`, `planWithComponents`, selection ops • **Phase helpers**: `getPlanFor`, `getNextPlannedDay`, `attachCalendar(startEpochDay)`, `currentPhaseId` resolution • **PRs**: `evaluateAndRecordPrIfAny`, `previewPrEvent`, `bestE1RM`, `bestRMAtReps`, `suggestNextLoadKg`, `getLastSuccessfulWeight` | New PR pipeline. |

### 2.6 data/seed/
| File | Responsibility | Notes |
|---|---|---|
| `ExerciseSeed.kt` | Insert/update default `Exercise` rows. | Called from app scope on startup. |
| `MetconSeed.kt` | Insert/update example metcon plans/components. | Called from app scope on startup and DB `onOpen`. |
| **`DatabaseSeeder.kt`** | Create a default Phase + Week/Day scaffold if empty. | Idempotent; 4 weeks × days 1..5. |
| **`DevPhaseSeed_dev.kt`** | DEV‑only: create a phase/weeks; mirror legacy selections into `day_item`, top‑up missing with defaults, fill empty days with placeholder metcons. | Idempotent; used in `WorkoutApp`. |

### 2.7 ui/
| Screen | Notes |
|---|---|
| `MainActivity.kt` | Day labels still computed from legacy `ProgramDao` types; can be expanded to Phase later. |
| `ExerciseLibraryActivity.kt` | No UI signature changes; repository handles Phase‑vs‑legacy source. |
| `WorkoutActivity.kt` | Renders strength and metcon plan cards; repository resolves via `PlanDao` when present. |
| `MetconActivity.kt`, `MetconAmrapActivity.kt`, `MetconEmomActivity.kt` | No change to inputs; metcon results persisted to `MetconLog`. |
| `ExerciseDetailActivity.kt` | Logs sets; observes last/suggested weight, e1RM, PR events; triggers PR dialog. |
| `SettingsActivity.kt`, `PersonalRecordsActivity.kt` | No change to navigation; PR screen TBD. |

### 2.8 viewmodels/
Public contracts unchanged; VMs consume the repo which now prefers the Phase model and emits PR events for `ExerciseDetailActivity`.

### 2.9 res/* (selected)
| File | Responsibility |
|---|---|
| `activity_workout.xml` | Strength/metcon list. |
| `activity_exercise_detail.xml` | Adds e1RM text + PR hints. |
| `dialog_pr.xml` | PR celebration dialog (hard/soft PR). |
| `item_library_row.xml`, `item_metcon_plan_row.xml` | Library and metcon plan rows. |
| `_main_day_card.xml` | Main day cards. |
| `activity_metcon*.xml` | Metcon screens. |

### 2.10 AndroidManifest & Root
| File | Responsibility | Notes |
|---|---|---|
| `AndroidManifest.xml` | Activities, theme, app class. | No change beyond existing registrations. |
| `WorkoutApp.kt` | Application bootstrap | Seeds: `ExerciseSeed.seedOrUpdate`, `MetconSeed.seedOrUpdate`, and **`DevPhaseSeed_dev.seedFromLegacy`** (SDK ≥ 26). |
| `settings.gradle.kts` | Gradle settings | Includes `:app`. |
| `app/build.gradle.kts` | Module build | Kotlin, Room v7, lifecycle, etc. |

---

## 3. End‑to‑End Flows (updated)

**A. Strength (day plan preferred)**  
`WorkoutRepository.programForDay(day)` → if a `WeekDayPlan` exists for `(currentPhase, week=1, day)`, stream `flowDayStrengthFor(dayPlanId)`; otherwise fallback to legacy `ProgramDao.getProgramForDay(day)`.

**B. Metcon selections (day plan preferred)**  
Same preference using `PlanDao.flowDayMetconsFor(dayPlanId)` → map to `SelectionWithPlanAndComponents` via `MetconDao.getPlanWithComponents(planId)`.

**C. Metcon logging**  
Screens call repo `logMetconForTime/Amrap/Emom` → `MetconDao.insertLog` → `lastMetconForPlan(planId)` feeds UI labels.

**D. Phase scaffold**  
On cold start (`WorkoutApp`), seeds exercise/metcon; dev‑only seed mirrors legacy into `day_item` if empty.

**E. Strength logging + PRs**
1) User enters set → `ExerciseDetailActivity` calls `previewPrEvent`.
2) If PR preview: show `dialog_pr.xml` (strings show hard vs soft PR).
3) Confirm → `WorkoutRepository.logStrengthSet` + `evaluateAndRecordPrIfAny` → `PersonalRecordDao.upsertEstimated1RM/RepMax`.
4) UI observes `prEvent` for celebration.

---

## 4. Change Impact Matrix
| Change | Files | Risks | Status |
|---|---|---|---|
| **Introduce Phase/Week/Day model** | `PhaseEntity`, `WeekDayPlanEntity`, `DayItemEntity`, `PlanDao`, `AppDatabase(v7)` | Schema bump; dev destructive migration | **Done** |
| **Repository bridging to Phase model** | `WorkoutRepository`, `Repos` | Behavior differences if both models populated | **Done** |
| **Plan‑scoped metcon logs** | `MetconDao`, `MetconLog` | None (append‑only table) | **Done** |
| **Dev seed for Phase scaffold** | `DevPhaseSeed_dev`, `DatabaseSeeder`, `WorkoutApp` | Dev‑only side effects | **Done** |
| **PR pipeline** | `StrengthFormulas`, `PrModels`, `PersonalRecordDao`, `WorkoutRepository`, `ExerciseDetailActivity`, `dialog_pr.xml` | New UI flow; ensure UX polish | **Done** |

---

## 5. Open Questions
- When to fully deprecate legacy `ProgramSelection`/`ProgramDao`? Migration plan & UI impacts.
- Migrate `WorkoutSession` fully to `WorkoutSessionEntity` and de‑register the legacy `@Entity`?
- Attach calendar dates automatically when a phase is created (prod behavior vs dev‑only).

---

## 6. Next Actions
1) Decide sunset path for legacy day model (auto‑migrate legacy selections into `day_item`).
2) Expose phase‑aware day labels in `MainActivity` (optional).
3) Write **non‑destructive migrations** for v7 before release.
4) Flesh out `PersonalRecordsActivity` (browse PR history, export).

---
