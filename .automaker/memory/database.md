---
tags: [database]
summary: database implementation decisions and patterns
relevantTo: [database]
importance: 0.7
relatedFiles: []
usageStats:
  loaded: 6
  referenced: 1
  successfulFeatures: 1
---
# database

#### [Gotcha] Database schema evolution preceded code implementation; the `direction` column already existed in SQLite (migration 26) but was unmapped in the codebase. (2026-02-14)
- **Situation:** Implementing the 'Habit Direction' feature and updating `HabitRecord`.
- **Root cause:** The column was pre-provisioned or left over from a partial implementation, requiring only code-side mapping updates rather than a new schema migration.
- **How to avoid:** Saved effort on migration scripts but required careful checking of existing schema to avoid duplicate column errors.