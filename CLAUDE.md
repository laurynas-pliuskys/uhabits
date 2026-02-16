# Claude Code - Project Context

## How This Project Uses Claude Code

This project is developed through **automaker-app**, a GUI (Kanban interface) that manages prompts, context, and task orchestration for Claude Code. This is NOT a standard Claude Code setup:
- Automaker generates prompts/context automatically and invokes Claude Code
- Claude Code sessions here are primarily for **high-level overviews and exploration**, not direct feature implementation
- **DO NOT** create config files or documentation unprompted — only create functional scripts that automaker can call
- Use the **android-tester** skill when an implemented feature needs UI-level verification on the emulator

## Automaker Tickets

Feature tickets: `.automaker/features/<feature-id>/feature.json` (id, title, description, status, priority, complexity, dependencies)
- **OK to update:** `description` — refine implementation approach, clarify requirements, add design decisions
- **DO NOT touch:** `status`, `priority`, `complexity`, `dependencies`, `category` — automaker manages these
- `.automaker/app_spec.txt` — overall vision + list of already-implemented features
- Everything in `.automaker/features/` is planned/backlog work, not existing features

## Gotchas

- Debug APK is unsigned; release requires `LOOP_KEY_*` env vars
- `lint-baseline.xml` (350KB) exists — lint abortOnError is disabled
- `build.sh` manages emulator snapshots for fresh-install test state

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

**IMPORTANT:** All features have `skipTests: true` and MUST pass these checks before approval:

### Required Verification Steps (in order):
1. ✅ **Build Check** - `./gradlew assembleDebug` must succeed
2. ✅ **Unit Tests** - `./gradlew test` must pass
3. ✅ **Code Style** - `./gradlew ktlintCheck` must pass
4. ✅ **UI Verification** - **MANDATORY** for all UI category tickets:
   - Use android-tester skill to verify feature on emulator
   - Create test data through the UI (not backend) to exercise the feature
   - Take screenshots demonstrating the feature works as specified
   - If UI verification reveals missing dependencies (e.g., no UI to create required test data), create a new ticket for the blocker and document the limitation in the current ticket's description

**DO NOT** mark a feature as complete/verified or move to waiting_approval until ALL applicable steps pass. For UI tickets, step 4 is NOT optional.

## Autonomous Verification

Use the **android-tester** skill for UI-level verification:

```bash
ACT=~/.claude/skills/android-tester/scripts/android-ctl

# 1. Ensure emulator is running
$ACT ensure

# 2. Build and install
./gradlew assembleDebug --quiet
$ACT install uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk
$ACT launch
sleep 3

# 3. Take screenshot and view hierarchy
$ACT screenshot /tmp/screen.png
$ACT view-xml /tmp/view.xml

# 4. Interact (tap, scroll, type, etc.)
$ACT tap 540 1200
sleep 2

# 5. Verify result
$ACT screenshot /tmp/after.png
```

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
