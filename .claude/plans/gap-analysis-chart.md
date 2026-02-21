# Gap Analysis Chart — Implementation Plan

## Overview

Add a new "Gap Analysis" card to the habit statistics (ShowHabit) view. The chart displays vertical bars representing **stint durations** — the number of days between "events" — with a 10-stint Simple Moving Average (SMA) trend line overlay.

**Direction determines what counts as an "event":**

| Direction | Habit example | Event | Stint = | What each bar shows |
|-----------|--------------|-------|---------|---------------------|
| NEGATIVE | Sober | Failure (relapse) | Days between failures | Length of each sobriety streak |
| POSITIVE | Exercise | Success (workout) | Days between successes | Rest gap between workouts |

**Missing data (UNKNOWN)** is treated as neutral — it does not break stints or count as an event.

**Visual design:**
- Vertical bar chart (one bar per stint), chronological left-to-right
- Y-axis: number of days; X-axis: event dates (date of the failure/success that ended each stint)
- Completed stints: lighter shade of habit color
- Active (current) stint: darker/deeper shade, grows daily
- SMA trend line (10-stint window) drawn on top of bars
- Scrollable horizontally when many stints exist (via `DataView`/`AndroidDataView` pattern)
- Only shown for boolean (Yes/No) habits

---

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Chart rendering | Core `DataView` (like `BarChart`) | Gives horizontal scrolling via `AndroidDataView`; platform-abstract |
| Data model location | `uhabits-core` | Follows `Streak`/`StreakList` pattern |
| SMA period | Hardcoded 10 | Configurable later (backlog item `configurable-sma-period`) |
| Interactive controls | None (no spinner) | Matches `StreakCard` simplicity; can add later |
| Card placement | After StreakCard, before FrequencyCard | Groups streak-related analysis together |

---

## Phase 1: Stint Data Model (`uhabits-core`)

### 1.1 Create `Stint.kt`

**File:** `uhabits-core/src/jvmMain/java/org/isoron/uhabits/core/models/Stint.kt`

```kotlin
data class Stint(
    val start: Timestamp,  // first day of the stint
    val end: Timestamp,    // last day of the stint (day before the event)
    val isActive: Boolean = false  // true if this is the current/ongoing stint
) {
    val length: Int
        get() = start.daysUntil(end) + 1  // inclusive day count
}
```

Mirrors `Streak(start, end)` with an added `isActive` flag.

### 1.2 Create `StintList.kt`

**File:** `uhabits-core/src/jvmMain/java/org/isoron/uhabits/core/models/StintList.kt`

```kotlin
class StintList {
    private val list = ArrayList<Stint>()

    fun getAll(): List<Stint>  // returns all stints chronologically

    fun recompute(
        computedEntries: EntryList,
        from: Timestamp,
        to: Timestamp,    // today
        direction: HabitDirection
    )
}
```

**Algorithm for `recompute()`:**

1. Get all entries from `from` to `to` via `computedEntries.getByInterval(from, to)` (returns newest-first).
2. Reverse to get chronological order (oldest-first).
3. Determine what constitutes an "event":
   - `NEGATIVE`: event = entry with `value == Entry.NO` (explicit failure)
   - `POSITIVE`: event = entry with `value == Entry.YES_MANUAL || value == Entry.YES_AUTO` (explicit success)
4. Walk through entries chronologically:
   - Track `stintStart` (timestamp of first non-event day after an event)
   - When an event is encountered:
     - If `stintStart != null` and stint length > 0, record `Stint(stintStart, previousDay, isActive=false)`
     - Reset `stintStart` to null
   - When a non-event is encountered:
     - If `stintStart == null`, set `stintStart = currentTimestamp`
5. After walking all entries, if `stintStart != null`:
   - Record `Stint(stintStart, to, isActive=true)` — the current active stint
6. Filter out stints with `length == 0`.

### 1.3 Add `stints` to `Habit.kt`

Add field to `Habit` data class:
```kotlin
val stints: StintList
```

### 1.4 Wire into `Habit.recompute()`

In `Habit.recompute()`, after streaks recompute, add:
```kotlin
stints.recompute(computedEntries, from, to, direction)
```

### 1.5 Wire `StintList` creation

In `HabitList` / wherever `Habit` instances are created (likely `SQLiteHabitList` or the Habit factory), instantiate `StintList` alongside `StreakList`.

---

## Phase 2: SMA Calculator (`uhabits-core`)

### 2.1 Create `SmaCalculator.kt`

**File:** `uhabits-core/src/jvmMain/java/org/isoron/uhabits/core/utils/SmaCalculator.kt`

```kotlin
object SmaCalculator {
    /**
     * Computes Simple Moving Average for a list of values.
     * @param values list of values (chronological order)
     * @param window SMA window size (e.g., 10)
     * @return list of SMA values, same length as input.
     *         For positions with fewer than [window] prior values,
     *         uses all available values.
     */
    fun compute(values: List<Double>, window: Int): List<Double>
}
```

Algorithm: For each position `i`, average `values[max(0, i-window+1)..i]`.

---

## Phase 3: Gap Chart View (`uhabits-core`)

### 3.1 Create `GapChart.kt`

**File:** `uhabits-core/src/jvmMain/java/org/isoron/uhabits/core/ui/views/GapChart.kt`

Implements `DataView` (same as `BarChart`). This gives horizontal scrolling support via `AndroidDataView`.

```kotlin
class GapChart(
    var theme: Theme,
    var dateFormatter: LocalDateFormatter
) : DataView {
    var stints: List<Stint> = emptyList()
    var smaValues: List<Double> = emptyList()
    var color: Color = Color(0.0, 0.0, 0.0)   // habit color (lighter shade for completed)
    var activeColor: Color = Color(0.0, 0.0, 0.0)  // deeper shade for active stint

    override var dataOffset = 0
    override val dataColumnWidth: Double  // bar width + margins

    override fun draw(canvas: Canvas) { ... }
}
```

**Drawing logic (`draw()`):**

1. Compute layout: `nColumns` that fit in width, bar width, margins.
2. Draw horizontal grid lines (like `BarChart`: percentage of max stint length).
3. Draw bars right-to-left (newest = rightmost, matches `BarChart` convention):
   - For each visible stint (adjusted by `dataOffset`):
     - Active stint → `activeColor` (deeper shade)
     - Completed stint → `color` (lighter shade)
     - Bar height proportional to `stint.length / maxLength`
     - Rounded top corners (radius = `barWidth * 0.15`, matching `BarChart`)
     - Duration label above bar (number of days)
4. Draw SMA line:
   - For each visible stint, plot a point at `(barCenterX, smaValue)` scaled to chart height
   - Connect points with lines (use `canvas.drawLine()`)
   - Use a contrasting color (e.g., theme's `cardTextColor` or a red accent)
5. Draw X-axis labels:
   - Show the event date that ended each stint (formatted via `dateFormatter`)
   - For active stint, show "Now" or today's date

**Color shade calculation:**
- Completed bars: habit color at ~60% alpha (lighter)
- Active bar: habit color at 100% (deeper/full saturation)
- Use `Color(r, g, b, alpha)` or blend with background

---

## Phase 4: Gap Card Presenter (`uhabits-core`)

### 4.1 Create `GapCard.kt`

**File:** `uhabits-core/src/jvmMain/java/org/isoron/uhabits/core/ui/screens/habits/show/views/GapCard.kt`

```kotlin
data class GapCardState(
    val color: PaletteColor,
    val stints: List<Stint>,
    val smaValues: List<Double>,
    val theme: Theme
)

class GapCardPresenter {
    companion object {
        fun buildState(habit: Habit, theme: Theme): GapCardState {
            val stints = habit.stints.getAll()
            val durations = stints.map { it.length.toDouble() }
            val smaValues = SmaCalculator.compute(durations, window = 10)
            return GapCardState(
                color = habit.color,
                stints = stints,
                smaValues = smaValues,
                theme = theme
            )
        }
    }
}
```

Follows `StreakCartPresenter` pattern (stateless companion, no interactive controls).

---

## Phase 5: Gap Card View (`uhabits-android`)

### 5.1 Create `show_habit_gap.xml`

**File:** `uhabits-android/src/main/res/layout/show_habit_gap.xml`

Layout structure (matching other cards):
```xml
<merge>
    <TextView android:id="@+id/title"
        android:text="@string/gap_analysis"
        style="@style/CardHeader" />
    <org.isoron.platform.gui.AndroidDataView
        android:id="@+id/gapChart"
        style="@style/Chart" />
</merge>
```

Uses `AndroidDataView` to host the core `GapChart` (provides scroll/touch handling).

### 5.2 Create `GapCardView.kt`

**File:** `uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/show/views/GapCardView.kt`

```kotlin
class GapCardView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val binding = ShowHabitGapBinding.inflate(LayoutInflater.from(context), this)

    fun setState(state: GapCardState) {
        val androidColor = state.theme.color(state.color).toInt()
        binding.title.setTextColor(androidColor)
        val chart = GapChart(state.theme, AndroidDateFormatter(context))
        chart.stints = state.stints
        chart.smaValues = state.smaValues
        chart.color = state.theme.color(state.color).withAlpha(0.6)   // lighter
        chart.activeColor = state.theme.color(state.color)             // full
        binding.gapChart.setDataView(chart)
        postInvalidate()
    }
}
```

### 5.3 Add string resource

**File:** `uhabits-android/src/main/res/values/strings.xml`

```xml
<string name="gap_analysis">Gap Analysis</string>
```

---

## Phase 6: Integration into ShowHabit

### 6.1 Update `ShowHabitState`

**File:** `uhabits-core/.../show/ShowHabit.kt`

Add field:
```kotlin
data class ShowHabitState(
    // ... existing fields ...
    val gaps: GapCardState,
    // ...
)
```

Add to `ShowHabitPresenter.buildState()`:
```kotlin
gaps = GapCardPresenter.buildState(habit, theme),
```

### 6.2 Update `ShowHabitView.kt`

Add:
```kotlin
binding.gapCard.setState(data.gaps)
```

Hide for numerical habits (same visibility logic as other boolean-only cards).

### 6.3 Update `show_habit.xml`

Add `GapCardView` after `streakCard`, before `frequencyCard`:
```xml
<org.isoron.uhabits.activities.habits.show.views.GapCardView
    android:id="@+id/gapCard"
    style="@style/Card" />
```

---

## Phase 7: Unit Tests

### 7.1 `StintListTest.kt`

**File:** `uhabits-core/src/jvmTest/java/org/isoron/uhabits/core/models/StintListTest.kt`

Test cases:
- **Basic NEGATIVE:** failures at known dates → correct stint durations between them
- **Basic POSITIVE:** successes at known dates → correct gap durations between them
- **Missing data neutral:** UNKNOWN entries don't break stints
- **Consecutive events:** two failures in a row → no 0-length stint generated
- **Active stint:** last stint extends to `to` date with `isActive=true`
- **No events:** all successes (NEGATIVE) → single active stint spanning entire range
- **All events:** all failures (NEGATIVE) → no stints (or only 0-length ones filtered out)
- **Edge: single entry**
- **Edge: empty entries**

### 7.2 `SmaCalculatorTest.kt`

**File:** `uhabits-core/src/jvmTest/java/org/isoron/uhabits/core/utils/SmaCalculatorTest.kt`

Test cases:
- Window larger than data → averages all available
- Exact window size → correct moving average
- Window = 1 → returns original values
- Empty input → empty output
- Single value → returns that value

### 7.3 `GapCardPresenterTest.kt`

Test that `buildState()` correctly computes stints and SMA from a habit with known entries.

---

## File Summary

| File | Action | Layer |
|------|--------|-------|
| `core/models/Stint.kt` | **Create** | Core |
| `core/models/StintList.kt` | **Create** | Core |
| `core/models/Habit.kt` | **Edit** — add `stints: StintList` field | Core |
| `core/models/Habit.kt` | **Edit** — add `stints.recompute()` call | Core |
| `core/utils/SmaCalculator.kt` | **Create** | Core |
| `core/ui/views/GapChart.kt` | **Create** | Core |
| `core/ui/screens/.../views/GapCard.kt` | **Create** | Core |
| `core/ui/screens/.../ShowHabit.kt` | **Edit** — add `gaps` to state + presenter | Core |
| `android/.../show/views/GapCardView.kt` | **Create** | Android |
| `android/res/layout/show_habit_gap.xml` | **Create** | Android |
| `android/res/layout/show_habit.xml` | **Edit** — add gap card | Android |
| `android/res/values/strings.xml` | **Edit** — add string | Android |
| `android/.../show/ShowHabitView.kt` | **Edit** — wire gap card | Android |
| Habit creation sites (SQLiteHabitList etc.) | **Edit** — instantiate `StintList` | Core/Android |
| `core/models/StintListTest.kt` | **Create** | Test |
| `core/utils/SmaCalculatorTest.kt` | **Create** | Test |
| `core/ui/.../GapCardPresenterTest.kt` | **Create** | Test |

---

## Implementation Order

1. **Phase 1** (Stint data model) — foundation, can be unit tested independently
2. **Phase 2** (SMA calculator) — standalone utility, unit tested independently
3. **Phase 7.1-7.2** (unit tests for Phase 1 & 2) — TDD: write tests alongside or right after model code
4. **Phase 3** (GapChart view) — core rendering, can be visually tested via existing screenshot test infrastructure
5. **Phase 4** (GapCardPresenter) — wires data to chart
6. **Phase 5** (GapCardView + layout) — Android UI layer
7. **Phase 6** (ShowHabit integration) — plug everything together
8. **Phase 7.3** (presenter test) — verify integration logic
9. **Verify** — build, unit tests, ktlint, UI testing via android-tester skill

---

## Out of Scope (Future Backlog)

These remain in the backlog for later implementation:
- `configurable-sma-period` — spinner to change SMA window
- `inverted-gap-statistics` — (already handled by direction-aware logic, but could add UI toggle)
- `gap-comparison-view` — multi-habit gap comparison
- `gap-notification-alerts` — threshold-based notifications
- Numerical habit support
