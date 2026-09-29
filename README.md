# Urgent Ring (Android MVP)

[![License: MIT](https://img.shields.io/github/license/AmmarSAA/urgentring-dndbypass)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/AmmarSAA/urgentring-dndbypass?style=social)](https://github.com/AmmarSAA/urgentring-dndbypass/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/AmmarSAA/urgentring-dndbypass?style=social)](https://github.com/AmmarSAA/urgentring-dndbypass/fork)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ffdd00?style=flat&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/ammarsaa)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-F16061?style=flat&logo=ko-fi&logoColor=white)](https://ko-fi.com/ammarsaa)

Lets specific callers — or anyone who calls repeatedly — ring through even when the
phone is on silent or Do Not Disturb, by counting calls per caller in a rolling time
window and forcing the ringer on once a threshold is crossed.

### Like this project?

If Urgent Ring is useful to you, the easiest ways to support it:

- ⭐ **Star** the repo — it's free, and it's the single biggest thing that helps
  other people find this project.
- 🍴 **Fork** it — adapt it, build on it, use it as a base for your own idea.
- ☕ **Donate** — [Buy Me a Coffee](https://buymeacoffee.com/ammarsaa) or
  [Ko-fi](https://ko-fi.com/ammarsaa) if you'd like to support development directly.
- 📣 **Share** it with anyone who's ever missed an urgent call because their phone
  was on silent.

See the platform feasibility notes below before assuming this behavior is portable to
iOS — it is **not**, by design of the platform, and this repo is Android-only for that
reason.

## Why Android-only

- Android's `CallScreeningService` (API 29+, granted via `RoleManager.ROLE_CALL_SCREENING`)
  gives an app the real caller number for every incoming call *before* it rings, and an
  app can play a loud alert over the top for that call via `AudioManager`/`MediaPlayer`
  on the alarm stream. This is the Play-Store-sanctioned mechanism spam/caller-ID apps
  (Truecaller, Hiya) already use.
- iOS sandboxes the Phone app: there is no public API to read a real cellular caller's
  number in real time, silence/un-silence a real call, or override the hardware
  ring/silent switch. iOS does ship its own native "Allow Repeated Calls" DND exception
  and per-contact "Emergency Bypass," but neither is configurable or triggerable by a
  third-party app. A genuinely equivalent iOS app isn't possible without either Apple's
  Critical Alerts entitlement (health/safety-gated) or building an entirely separate
  VoIP product on CallKit/PushKit.

## Project layout

```
app/src/main/java/com/dndbypass/urgentring/
├── data/            Room entities/DAOs, DataStore settings, phone-number normalizer, debug log
├── screening/        CallScreeningService, per-caller CallCounter, RingerOverrideManager
├── permissions/       Helpers for the call-screening role and the battery-optimization exemption
└── ui/                Compose screens: dashboard, rules list/edit, activity log, permissions, debug log
```

### Core logic

- `screening/UrgentCallScreeningService.kt` — bound by the system for every incoming
  call. Never rejects a call; below-threshold calls stay silent only because the
  device's own ringer/DND state is already silent.
- `screening/CallCounter.kt` — resolves the matching rule (specific number > "all
  callers" default) and counts that caller's calls in a rolling window, pruning
  expired ones so they stop counting.
- `screening/RingerOverrideManager.kt` — plays a loud alert on the alarm audio stream
  (`AudioAttributes.USAGE_ALARM`) when the threshold is crossed, then stops it and
  restores alarm volume afterward. Deliberately does **not** touch ringer mode or Do
  Not Disturb — those exist so the alert plays *through* Silent/DND without visibly
  flipping either off, matching how Google's "Find My Device" ring works. The one
  device state this can't defeat is Android's strictest DND level ("Total Silence" /
  `ZEN_MODE_NO_INTERRUPTIONS`), which mutes the alarm stream too, by design, with no
  app-level exception.

## Known MVP limitations (intentional, called out for the next pass)

- **Ringer restore is timer-based** (45s fixed window in
  `UrgentCallScreeningService.RING_OVERRIDE_DURATION_MS`), not tied to actual call end.
  A production build should restore on `TelephonyCallback.CALL_STATE_IDLE` instead.
- **Phone number normalization is a US/NANP heuristic** (`data/PhoneNumberNormalizer.kt`).
  Swap in Google's libphonenumber for real international support.
- **Unknown/blocked caller ID** falls back to a single `"__unknown__"` bucket if you
  create a default rule that applies to it — spoofed caller ID can defeat a blanket
  "all callers" rule, so the UI should keep steering users toward per-contact rules.
- No handling yet for VoIP calls placed through other apps (WhatsApp, Signal, etc.) —
  `CallScreeningService` only sees native telephony calls.

## Building

This scaffold doesn't include the Gradle wrapper jar (binary, not written by hand).
Open the folder in **Android Studio** (Jellyfish or newer) and let it regenerate the
wrapper on first sync, or generate it yourself if you have Gradle installed:

```bash
gradle wrapper --gradle-version 8.7
```

Minimum SDK is 29 (Android 10), required for `RoleManager.ROLE_CALL_SCREENING`.

## Testing without real repeat calls

The Android emulator can simulate incoming calls from a fixed number, which is enough
to exercise the counting logic end-to-end:

```bash
adb emu gsm call 5551234567
adb emu gsm call 5551234567
adb emu gsm call 5551234567
```

Grant both permissions from the in-app **Permissions** screen first (call screening
role, battery optimization exemption), enable the feature on the dashboard, and set a
low threshold (e.g. 2 calls / 5 minutes) on a test rule to see the ringer-override
behavior quickly. Watch the **Activity** screen to confirm counts and which calls
triggered a ring-through, or export the **Debug logs** screen's copy-to-clipboard trace
for the full per-call decision path.

Real-device testing matters too: OEM battery managers (MIUI, Samsung, etc.) are the
most likely source of production reliability issues, since they can kill background
processes despite the call-screening role's system binding.

## License

MIT — see [LICENSE](LICENSE). Reusing this code requires keeping the copyright notice
and license text, which credits the original author.

## Author

Built by **Ammar S.** ([@AmmarSAA](https://github.com/AmmarSAA)).

Questions, feature requests, or feedback: **contact@ammarsaa.com**, or use the app's
own Help & Feedback / Donate screens.

If you build something on top of this, I'd genuinely like to hear about it — open an
issue, or just email me.
