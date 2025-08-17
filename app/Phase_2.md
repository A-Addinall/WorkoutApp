### Phase 2 — Strength: 1RM engine + rest timer + PR celebration — **⏳ In Progress**

| Area               | Task                                                                             | Status |
| ------------------ | -------------------------------------------------------------------------------- | ------ |
| **1RM Engine**     | Implement `estimateOneRepMax(weight, reps)` (Epley; reps ≤ 12)                   | ✅      |
| ✅                  | Add `WorkoutRepository.suggestNextLoad(...)` (prefer 1RM; fallback last-success) | ✅      |
| ✅                  | Extend `PersonalRecordDao` with `bestEstimated1RM`, `bestWeightAtReps`           | ✅      |
| ✅                  | Expose e1RM + suggested load in `ExerciseDetailViewModel`                        | ✅      |
| ✅                  | Display e1RM + suggested weight in strength detail UI                            | ✅      |
| ✅                  | Unit tests for e1RM + suggested load                                             | ✅      |
| **Rest Timer**     | Decide trigger point (auto after `logStrengthSet` vs. manual start)              | ⏳      |
|                    | Add timer start API to repository                                                | ⏳      |
|                    | Add timer chip UI (countdown, pause/resume, “+30s”)                              | ⏳      |
|                    | Persist timer across background/process death                                    | ⏳      |
|                    | PR auto-rest extension (+30s)                                                    | ⏳      |
|                    | Timer tests (navigation, process death, PR extension)                            | ⏳      |
| **PR Celebration** | Schema: extend `personal_record` with `equipment` + `reps`                       | ✅      |
|                    | DAO: add `upsertEstimated1RM`, `upsertRepMax`                                    | ✅      |
|                    | Repo: check Hard/Soft PRs + thresholds; prioritise Hard PR                       | ✅      |
|                    | Repo: emit PR event + add +30s earned rest                                       | ✅      |
|                    | UI: PR modal dialog (centered, dismiss-to-close, no confetti)                    | ✅      |
|                    | UI: PR badge on set row                                                          | ⏳      |
|                    | VM: surface PR events for UI                                                     | ✅      |
|                    | Tests: PR detection, thresholds, equipment separation, guardrails                | ⏳      |
| **Cross-Cutting**  | Update `tech_map.md` and `screen_contracts.md` with new APIs                     | ⏳      |
|                    | Update `Change_plan.md` with migration + completion notes                        | ⏳      |
|                    | Keep destructive migration + dev seed until pre-release                          | 🔄     |

---

### Next Steps

1. **Docs sync**

   * Update `Change_plan.md`, `tech_map.md`, `screen_contracts.md` with new APIs (e1RM, `suggestNextLoadKg`, PR events) and modal UI decision.

2. **Rest Timer implementation**

   * Add `RestTimerEntity` + DAO.
   * Extend `WorkoutRepository` with start/pause/resume/clear.
   * Wire ViewModel with `restRemaining: StateFlow<Long?>`.
   * Add UI chip in Exercise Detail with countdown + pause/resume + "+30s".
   * Auto-start after successful set; extend by +30s on hard PR.

3. **PR UI polish**

   * Add optional small “🏆 PR” badge next to the set row that triggered the PR.

4. **Testing**

   * Unit tests for:

      * 1RM function edge cases.
      * Suggestion stepping logic.
      * Hard vs. soft PR detection.
      * Rest timer start/pause/resume/extend.

5. **Release readiness**

   * Verify destructive migration + dev seed approach remains safe until pre-release.
