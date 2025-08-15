# WorkoutApp — Technical Map (Canonical)

> Single source of truth for structure, data model, flows, and active work. Updated for **Phase 0 planning model** (Phase/Week/Day) and **v6 Room schema**.

---

## 0. Repo & Build
- **Module(s)**: `app`
- **Language**: Kotlin
- **Persistence / DB**: Room **v6** (was v5)
- **Migrations**: Dev uses `fallbackToDestructiveMigration()`; add proper migrations before prod.
- **Seeding**: `ExerciseSeed`, `MetconSeed`, **DevPhaseSeed_dev** (dev-only), **DatabaseSeeder** helpers.

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
> New **Phase 0** planning tables live in `data/entities` with `PlanDao` in `data/dao`.

---

## 2. Package Details

### 2.1 core/
| File | Responsibility | Key API | Notes |
|---|---|---|---|
| `Enums.kt` | Domain enums | `WorkoutType`, `Equipment`, `Modality`, `MetconType`, `MetconResult` | `MetconResult` persisted via converters. |

### 2.2 data/dao/
| File | Responsibility | Key API (selected) | Notes |
|---|---|---|---|
| `LibraryDao.kt` | Library queries | `getExercises(type, eq): Flow<List<Exercise>>`, `countExercises()`, `insertAll(...)` | — |
| `ExerciseDao.kt` | Exercise admin/seed helpers | `count()`, `insertAllIgnore(...)`, `insertAll(...)`, `getIdByName(name)`, `firstNIds(limit)` | New helpers support seeds. |
| `PersonalRecordDao.kt` | PR queries | `upsert`, `bestForExercise(exerciseId)` | — |
| `ProgramDao.kt` | Legacy day selections (strength) | `getProgramForDay(day)`, `upsert`, `remove`, `setRequired`, `setTargetReps`, `exists`, `distinctTypesForDay` | Still used as fallback. |
| `SessionDao.kt` | Legacy sessions & set logs | `insertSession`, `insertSet`, `lastSets(...)`, `lastMetconSecondsForDay`, `lastMetconForDay` | — |
| `MetconDao.kt` | Metcon library + logs | Plans: `getAllPlans()`, `getPlanWithComponents(planId)`; Day (legacy): `getMetconsForDay(day)`; Select: `upsertSelection/removeSelection/setRequired/setDisplayOrder`; Logs: `insertLog`, `lastForPlan`, `lastForDay`; Seed helpers: `insertPlansIgnore`, `getPlanIdByTitle/key`, `updatePlanByKey`, `insertComponentsIgnore`, `updateComponentText`, `deleteAllComponentsForPlan`, `deleteComponentsNotIn`; Misc: `firstPlanId()` | DTOs: `PlanWithComponents`, `SelectionWithPlanAndComponents`. |
| **`PlanDao.kt`** | **Phase 0 planning model** | Phase: `insertPhase`, `countPhases`, `currentPhaseId`; Week/Day plans: `insertWeekPlans`, `countPlans`, `getPlan(...)`, `getPlanId(...)`, `getPlanById`, `getPlansForPhaseOrdered`, `getNextAfter`; Day items: `insertItems`, `clearItems`, `countItemsForDay`, `flowDayStrengthFor(dayPlanId)`, `flowDayMetconsFor(dayPlanId)`; Calendar: `updatePlanDate` | New. |
| **`WorkoutSessionDao.kt`** | Phase-aware sessions (WIP) | `insert`, `getById` | Uses `WorkoutSessionEntity` (phase/week/day indexes). |
### MetconDao helper clarification
*Optional:* `firstPlanId()` is **not required** for the current dev seed. The seed reads from `getAllPlans()`; include `firstPlanId()` only if you prefer a direct ID helper.

### 2.3 data/db/
| File | Responsibility | Key Settings | Notes |
|---|---|---|---|
| `data/db/AppDatabase.kt` | Room DB | `@Database(version = 6, exportSchema=false)`; entities below; DAOs include `planDao()`; seeds in `onCreate`; `MetconSeed.seedOrUpdate` in `onOpen` | Dev destructive migration on. |
| `data/db/Converters.kt` | Type converters | `WorkoutType↔String`, `Equipment↔String`, `MetconType↔String`, `MetconResult↔String` | — |

### 2.4 data/entities/
| File | Table | Fields (key) | Notes |
|---|---|---|---|
| `Exercise.kt` | `exercise` | `id`, `name`, `workoutType`, `primaryEquipment`, `modality`, `isUnilateral` | — |
| `ProgramSelection.kt` | `program_selection` | `id`, `dayIndex`, `exerciseId`, `required`, `preferredEquipment?`, `targetReps?` | Legacy day model. |
| `SetLog.kt` | `set_log` | `id`, `sessionId`, `exerciseId`, `equipment`, `setNumber`, `reps`, `weight?`, `timeSeconds?`, `rpe?`, `success?`, `notes?`, `metconResult?` | `metconResult` now persisted. |
| `WorkoutSession.kt` | `workout_session` | `id`, `dayIndex`, `startTs` | Legacy session (still used). |
| **`WorkoutSessionEntity.kt`** | `workout_session` | `id`, `phaseId?`, `weekIndex?`, `dayIndex`, `startTs` | New phase-aware struct (coexists with legacy usage). |
| `PersonalRecord.kt` | `personal_record` | standard PR | — |
| `UserSettings.kt` | `user_settings` | UX prefs | — |
| `MetconPlan.kt` | `metcon_plan` | `id`, `canonicalKey`, `title`, `type`, `durationMinutes?`, `emomIntervalSec?`, `isArchived` | — |
| `MetconComponent.kt` | `metcon_component` | `id`, `planId(FK)`, `orderInPlan`, `text` | — |
| `ProgramMetconSelection.kt` | `program_metcon_selection` | `id`, `dayIndex`, `planId(FK)`, `required`, `displayOrder` | Legacy day model. |
| `MetconLog.kt` | `metcon_log` | `id`, `dayIndex`, `planId`, `type`, `durationSeconds`, `timeSeconds?`, `rounds?`, `extraReps?`, `intervalsCompleted?`, `result`, `createdAt`, `notes?` | Plan-scoped results. |
| **`PhaseEntity.kt`** | `phase` | `id`, `name`, `startDateEpochDay`, `weeks` | New. |
| **`WeekDayPlanEntity.kt`** | `week_day_plan` | `id`, `phaseId`, `weekIndex`, `dayIndex`, `displayName?`, `dateEpochDay?` | New; unique `(phaseId, weekIndex, dayIndex)`. |
| **`DayItemEntity.kt`** | `day_item` | `id`, `dayPlanId(FK)`, `itemType` (`STRENGTH`/`METCON`), `refId`, `required`, `sortOrder`, `targetReps?`, `prescriptionJson?` | New; items attached to a day plan. |
### Session entity note
Only **one** Room entity should represent the `workout_session` table at a time to avoid duplicate table mappings. Keep the currently used session entity registered in `@Database(entities=[...])` and leave any alternative class as a plain data class (no `@Entity`) until we migrate.


### 2.5 data/repo/
| File | Responsibility | Key API | Notes |
|---|---|---|---|
| `Repos.kt` | Repo factory | `workoutRepository(context)` now wires `planDao()` | — |
| `WorkoutRepository.kt` | Orchestrates DAOs | **Library**: `getExercises`, `countExercises` • **Program**: `addToDay`, `programForDay` *(prefers Phase model via `PlanDao`, falls back to legacy)*, `setRequired`, `setTargetReps`, `removeFromDay`, `isInProgram`, `selectedTargetReps`, `requiredFor`, `daySummaryLabel` • **Sessions**: `startSession`, `logStrengthSet`, `logTimeOnlySet`, `logMetcon(...)`, `lastMetconSecondsForDay`, `lastMetconForDay`, `lastMetconDisplayForDay` • **Metcon plans**: `metconPlans`, `metconsForDay`, `logMetconForTime`, `logMetconAmrap`, `logMetconEmom`, `lastMetconForPlan` *(prefers Phase model)*, `planWithComponents`, `add/remove/setRequired/setOrder` • **Metcon logging**: `logMetconForTime/Amrap/Emom` (writes `MetconLog`) • **Phase helpers**: `getPlanFor`, `getNextPlannedDay`, `attachCalendar(startEpochDay)`, `currentPhaseId` resolution inside `resolveDayPlanIdOrNull(...)` | New Phase 0 bridging logic. |

### 2.6 data/seed/
| File | Responsibility | Notes |
|---|---|---|
| `ExerciseSeed.kt` | Insert/update default `Exercise` rows. | Called from app scope on startup. |
| `MetconSeed.kt` | Insert/update example metcon plans/components. | Called from app scope on startup and DB `onOpen`. |
| **`DatabaseSeeder.kt`** | Create a default Phase + Week/Day scaffold if empty. | Idempotent; 4 weeks × days 1..5. |
| **`DevPhaseSeed_dev.kt`** | DEV‑only: create a phase/weeks; mirror legacy selections into `day_item`, top-up missing with defaults, fill empty days with placeholder metcons. | Idempotent; used in `WorkoutApp`. |

### 2.7 ui/ (unchanged APIs, but data source prefers Phase model)
| Screen | Notes |
|---|---|
| `MainActivity.kt` | Day labels still computed from legacy `ProgramDao` types; can be expanded to Phase later. |
| `ExerciseLibraryActivity.kt` | No UI changes; repository handles Phase-vs-legacy source. |
| `WorkoutActivity.kt` | Renders strength and metcon plan cards; repository now resolves via `PlanDao` when present. |
| `MetconActivity.kt`, `MetconAmrapActivity.kt`, `MetconEmomActivity.kt` | No change to inputs; metcon results persisted to `MetconLog`. |
| `SettingsActivity.kt`, `PersonalRecordsActivity.kt` | No change. |

### 2.8 viewmodels/ (no signature changes)
Unchanged public contracts; VMs consume repo which now prefers Phase model.

### 2.9 res/* (no structural changes in this change set)
No new resources introduced by this change.

### 2.10 AndroidManifest & Root
| File | Responsibility | Notes |
|---|---|---|
| `AndroidManifest.xml` | Activities, theme, app class. | No change beyond existing registrations. |
| `WorkoutApp.kt` | Application bootstrap | Seeds: `ExerciseSeed.seedOrUpdate`, `MetconSeed.seedOrUpdate`, and **`DevPhaseSeed_dev.seedFromLegacy`** (SDK ≥ 26). |

---

## 3. End‑to‑End Flows (updated)

**A. Strength (day plan preferred)**  
`WorkoutRepository.programForDay(day)` → if a `WeekDayPlan` exists for `(currentPhase, week=1, day)`, stream `flowDayStrengthFor(dayPlanId)`; otherwise fallback to legacy `ProgramDao.getProgramForDay(day)`.

**B. Metcon selections (day plan preferred)**  
Same preference using `PlanDao.flowDayMetconsFor(dayPlanId)` → map to `SelectionWithPlanAndComponents` via `MetconDao.getPlanWithComponents(planId)`.

**C. Metcon logging**  
Screens call repo `logMetconForTime/Amrap/Emom` → `MetconDao.insertLog` → `lastMetconForPlan(planId)` feeds UI labels.

**D. Phase scaffold**  
On cold start (`WorkoutApp`), seeds exercise/metcon; dev-only seed mirrors legacy into `day_item` if empty.

---

## 4. Change Impact Matrix (active)
| Change | Files | Risks | Status |
|---|---|---|---|
| **Introduce Phase/Week/Day model** | `PhaseEntity`, `WeekDayPlanEntity`, `DayItemEntity`, `PlanDao`, `AppDatabase(v6)` | Schema bump; dev destructive migration | **Done** |
| **Repository bridging to Phase model** | `WorkoutRepository`, `Repos` | Behavior differences if both models populated | **Done** |
| **Plan-scoped metcon logs** | `MetconDao`, `MetconLog` | None (append-only table) | **Done** |
| **Dev seed for Phase scaffold** | `DevPhaseSeed_dev`, `DatabaseSeeder`, `WorkoutApp` | Dev-only side effects | **Done** |

---

## 5. Open Questions
- When to fully deprecate legacy `ProgramSelection`/`programDao`? Migration plan & UI impacts.
- Should `WorkoutSession` move fully to `WorkoutSessionEntity` (phase/week/day) and deprecate the legacy struct?
- Attach calendar dates automatically when a phase is created (prod behavior vs dev-only).

---

## 6. Next Actions
1) Decide sunset path for legacy day model (auto-migrate legacy selections into `day_item`).
2) Expose phase-aware day labels in `MainActivity` (optional).
3) Write non-destructive migrations for v6 before release.

---