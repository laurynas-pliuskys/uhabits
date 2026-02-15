---
tags: [architecture]
summary: architecture implementation decisions and patterns
relevantTo: [architecture]
importance: 0.7
relatedFiles: []
usageStats:
  loaded: 0
  referenced: 0
  successfulFeatures: 0
---
# architecture

#### [Pattern] Explicit `copyFrom`/`copyTo` methods for Domain-to-Record mapping (2026-02-14)
- **Problem solved:** Propagating the new `parentId` field between `Habit` (Domain) and `HabitRecord` (Database Layer)
- **Why this works:** Decouples the domain model from the underlying SQLite record structure, allowing independent evolution.
- **Trade-offs:** Increases boilerplate; requiring manual updates in multiple methods (`constructor`, `copyFrom`, `copyTo`) for every new field.

#### [Pattern] Explicit manual mapping between Domain Models (`Habit`) and Persistence Records (`HabitRecord`) via `copyFrom`/`copyTo` methods. (2026-02-14)
- **Problem solved:** Persisting domain objects to SQLite.
- **Why this works:** Decouples the rich domain model (behaviors, complex types) from the flat database representation.
- **Trade-offs:** Increases boilerplate (manual field copying) but prevents database schema details from leaking into domain logic.

### Circular dependency validation enforced at the Repository/List level, not the Domain Model. (2026-02-14)
- **Context:** Preventing cycles (A->B->A) or self-parenting in the habit hierarchy.
- **Why:** Validation requires access to the entire collection to trace ancestry, which individual `Habit` entities (anemic models) do not possess.
- **Rejected:** Database constraints (insufficient for recursive checks) or Domain Service (added unnecessary indirection for this scope).
- **Trade-offs:** Couples validation logic to the specific list implementation.