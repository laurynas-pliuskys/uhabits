---
tags: [performance]
summary: performance implementation decisions and patterns
relevantTo: [performance]
importance: 0.7
relatedFiles: []
usageStats:
  loaded: 0
  referenced: 0
  successfulFeatures: 0
---
# performance

#### [Pattern] In-memory adjacency list cache (`childrenCache`) implemented within the repository class to optimize hierarchical reads. (2026-02-14)
- **Problem solved:** Need to frequently access children of habits for hierarchical display without hitting the DB or scanning the full list every time.
- **Why this works:** Reduces child lookup complexity from O(N_total) to O(1) (map lookup), critical for recursive hierarchy rendering.
- **Trade-offs:** Increases complexity of mutator methods (`add`, `update`, `remove`) which must now maintain cache consistency alongside data persistence.