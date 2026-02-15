---
tags: [testing]
summary: testing implementation decisions and patterns
relevantTo: [testing]
importance: 0.7
relatedFiles: []
usageStats:
  loaded: 0
  referenced: 0
  successfulFeatures: 0
---
# testing

#### [Gotcha] Domain object equality checks require manual UUID synchronization when using ModelFactory (2026-02-14)
- **Situation:** Verifying `Habit.equals` behavior after adding `parentId` field
- **Root cause:** `ModelFactory.buildHabit()` generates unique UUIDs by default. Since `Habit.equals` compares all fields (including UUID), assertions checking specifically for `parentId` differences will fail (or pass deceptively) if UUIDs are not manually synchronized between test instances.
- **How to avoid:** Test setup is more verbose (requires manual ID copying) but ensures the test isolates the specific field being validated.

#### [Pattern] Business logic is isolated in a pure Kotlin `uhabits-core` module to utilize fast JVM-based unit testing (`:uhabits-core:jvmTest`). (2026-02-14)
- **Problem solved:** Verifying new model logic without launching Android emulators.
- **Why this works:** Provides a significantly faster feedback loop than Android Instrumentation tests.
- **Trade-offs:** Requires strict separation of logic from Android context objects.

#### [Gotcha] Inherited test fixtures typed to interfaces require explicit casting to verify implementation-specific extensions. (2026-02-14)
- **Situation:** `BaseUnitTest` defines `habitList` as the interface `HabitList`, but `SQLiteHabitListVerificationTest` needed to test `getTopLevelHabits` (specific to `SQLiteHabitList`).
- **Root cause:** Avoids duplicating complex setup logic from `BaseUnitTest` while still allowing access to the concrete class's new API.
- **How to avoid:** Reduces type safety in the test suite by relying on runtime casts (`as SQLiteHabitList`).