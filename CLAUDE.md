# Claude Code - Project Context

## Project Vision

Transforming Loop Habit Tracker into a **hierarchical Routine Manager** with gap analysis:
1. **Hierarchical structure** — parent Routines contain child habit Items
2. **Expandable accordion UI** — grouped, collapsible RecyclerView with 7-day grid
3. **Trinary states** — Success / Failure / Unknown (extends binary Yes/No)
4. **Gap analysis engine** — calculates intervals between state transitions (stints)
5. **Gap visualization** — bar charts with SMA trend lines in statistics view

Full spec: `.automaker/app_spec.txt`

## What's Implemented

| Feature | Notes |
|---------|-------|
| Hierarchical DB schema | Migration 26.sql — parent_id + direction columns on habits |
| Habit model parentId | Habit.kt + HabitRecord.kt extended |
| Direction toggle | POSITIVE/NEGATIVE enum on Habit |
| Hierarchical query methods | SQLiteHabitList: getChildren(), getTopLevel() |
| Parent habit aggregation | Routine completion count from child entries |
| Parent habit creation UI | Inline parent picker in EditHabitActivity |
| Routine card view | Expandable accordion card (waiting_approval) |
| Hierarchical RecyclerView adapter | Multi-type adapter for routine headers vs child items |
| Hierarchical CSV export | parent_id + direction in export |
| Flexible Habit Label Position | Settings option to move habit/routine labels to the right side |
| Trinary State System & Rendering | Red/Green indicators for pass/fail when missing data enabled. Success/Failure/Unknown states already exist |
| Gap Analysis Chart | `Stint`/`StintList` data model, `SmaCalculator`, `GapChart` DataView, `GapCardPresenter`, `GapCardView` — shown in ShowHabit for boolean habits only |

## Backlog Features

**Note:** Ignore `.automaker` files. This file (`CLAUDE.md`) is the single source of truth for both Claude Code and Gemini CLI.

| ID | Title | Description | Complexity | Use Case |
|----|-------|-------------|------------|----------|
| inverted-gap-statistics | Direction-Aware Gap Statistics | - | - | - |
| gap-comparison-view | Multi-Habit Gap Comparison Chart | - | - | - |
| gap-notification-alerts | Gap Threshold Notifications | - | - | - |
| hierarchy-csv-import | CSV Import with Hierarchy | - | - | - |
| widget-hierarchy-support | Widget Support for Hierarchical Habits | - | - | - |
| configurable-sma-period | Configurable SMA Period Setting | - | - | - |
| multi-parent-habits | Multi-Parent Habits | Allow a single habit to belong to multiple parent routines (e.g., "Face Wash" in both "Morning" and "Night"). | High (Schema) / Medium (Alias) | **Solution:** likely implement via "Alias" wrapper to avoid M:N schema refactor. |

## Workflow

Use **Claude Code plan mode** to design implementation, then implement:
1. Explore codebase to understand patterns
2. Write implementation plan (Claude Code plan mode)
3. Implement, then verify (build → unit tests → ktlint → UI if needed)

## Commands

| Command | Description |
|---------|-------------|
| `./gradlew assembleDebug` | Build debug APK → `uhabits-android/build/outputs/apk/debug/` |
| `./gradlew test` | Run unit tests (JVM, no emulator needed) |
| `./gradlew ktlintCheck` | Check Kotlin code style |
| `./gradlew connectedCheck` | Run instrumented tests (requires emulator) |
| `./build.sh build` | Full CI build (core + android) |
| `./build.sh android-tests <API>` | Run instrumented tests on API level |

## Architecture

```
uhabits-android/   # Android UI layer (activities, widgets, notifications)
uhabits-core/      # Kotlin Multiplatform business logic (models, algorithms)
docs/              # BUILD.md, GUIDELINES.md, TEST.md
build.sh           # CI/CD build script
```

- **Language:** Kotlin (new code must be Kotlin, legacy Java exists)
- **DI:** Dagger 2 with KSP
- **Min SDK:** 28 | **Target SDK:** 36 | **Compile SDK:** 36
- **Java toolchain:** JDK 17

## Code Style

- ktlint enforced — run `./gradlew ktlintCheck` before PRs
- Git-flow: feature branches → `dev` → `main`
- Keep PRs small and independent (see docs/GUIDELINES.md)

## Testing

- **Unit tests:** `./gradlew test` — JVM-only, no emulator
- **Instrumented tests:** require emulator, use `./build.sh android-tests <API>`
- View tests compare against prerendered images in `uhabits-android/src/androidTest/`
- All animations must be disabled on test emulator (0 duration scale)

## Verification Requirements

Before marking a feature complete, verify in order:

1. **Build** — `./gradlew assembleDebug` must succeed
2. **Unit tests** — `./gradlew test` must pass
3. **Code style** — `./gradlew ktlintCheck` must pass
4. **UI verification** — for UI changes, use the **android-tester** skill to install and push to emulator, then take a screenshot to verify

## Autonomous UI Testing

**Always install and push to emulator after any UI code change, then take a screenshot to verify visually.**

Use the **android-tester** skill for UI-level verification:

```bash
ACT=~/.claude/skills/android-tester/scripts/android-ctl

$ACT ensure          # start emulator if needed
./gradlew assembleDebug --quiet
$ACT install uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk
$ACT launch
sleep 3
$ACT screenshot /tmp/screen.png
$ACT view-xml /tmp/view.xml
$ACT tap 540 1200
```

## Gotchas

- Debug APK is unsigned; release requires `LOOP_KEY_*` env vars
- `lint-baseline.xml` (350KB) exists — lint abortOnError is disabled
- `build.sh` manages emulator snapshots for fresh-install test state
- **NEVER run `adb shell pm clear`** to fix rendering/black screen issues — it wipes ALL app data including backups, causing irreversible data loss. Use `adb shell am force-stop <pkg>` to kill the process (preserves data), or cold boot the emulator from Android Studio to reset GPU/display state.

## WSL2 to Android Emulator Setup

- **Dev environment:** WSL2
- **Emulator:** Windows host (Android Studio, Pixel 9 Pro XL AVD)
- **Connection:** ADB bridge over TCP (handled by android-tester skill)

### Daily Startup

1. **Launch emulator** in Android Studio Device Manager
2. **Open ADB bridge** in PowerShell: `adb kill-server; adb -a nodaemon server` (keep window open)
3. **In WSL2**, set ADB socket:
   ```bash
   export ADB_SERVER_SOCKET=tcp:$(ip route show | grep default | awk '{print $3}'):5037
   adb devices  # should show emulator-5554
   ```

### Troubleshooting

| Issue | Solution |
|:------|:---------|
| Connection Refused | Check Windows Firewall rule is active |
| Empty Device List | Cold Boot the emulator from Android Studio |
| IP Mismatch | Re-check host IP; it may have changed |
| Black screen / stuck splash | Cold boot emulator (HWUI EGL state corruption); check `adb shell dumpsys SurfaceFlinger --list` for stuck layers |
| Screenshots show wrong tap coords | Pixel 9 Pro XL screenshots display at ~898x2000 but device is 1344x2992 — multiply displayed coords by 1.5 |
