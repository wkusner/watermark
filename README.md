# Watermark v1

Need-based water nudges for Pixel Watch. Nudges only when you're behind pace *and* haven't
drunk in a while; never in quiet hours or Bedtime mode. Rising Tide makes ignored nudges firmer.

## What's in v1
- **Tile**: progress ring, today's total, **+1 glass**, undo.
- **Complication** ("Water"): ring or short text on your watch face; tap = log a glass.
- **Nudges** every ~15 min check (`Pace.kt` is the rule). Actions: **Drank**, **Snooze 30m**.
- **Rising Tide**: gentle buzz → firm buzz (20 min later) → full-screen "Water break" (20 min after that).
- **Settings** in the app: goal, glass size, wake time, quiet-after time, wait-after-a-drink, Rising Tide.

Defaults: 2000 ml goal, 250 ml glass, awake 6:30–20:00, wait 90 min after a drink.

## Build and install
1. Open this folder in Android Studio (File → Open → `Watermark`). Accept any prompts to
   sync Gradle or upgrade the Gradle/AGP versions.
2. On the watch: Settings → System → About → Versions → tap **Build number** 7×, then
   Settings → Developer options → **Wireless debugging** on (same Wi-Fi as the laptop).
3. In Android Studio: Device Manager → **Pair devices using Wi-Fi** → enter the code from the watch.
4. Pick the watch in the device menu and press ▶ Run.
5. On the watch: allow notifications, then add the **Watermark** tile and the **Water** complication.

## Tuning
- `Pace.kt` – the "is a nudge due?" rule. Unit tests in `app/src/test/.../PaceTest.kt`.
- `Nudger.kt` – escalation timing (`ESCALATE_AFTER_MIN`, `REPEAT_AFTER_MIN`) and vibration patterns.

Not yet: Health Connect sync, Slack Tide (calendar-aware), Bootstrap bottle scale.
