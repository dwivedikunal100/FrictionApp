# 🔒 Friction — App Blocker & Screen Time

> The app that makes opening Instagram annoying.

## What This Is

Friction intercepts app launches using Android's `AccessibilityService` API and forces users through a "friction wall" before they can proceed. Free tier has breathing exercises. Premium unlocks math equations, walk mode, and scheduling.

---

## Project Structure

```
app/src/main/java/com/friction/app/
├── accessibility/
│   └── FrictionAccessibilityService.kt   ← The core engine
├── billing/
│   └── FrictionBillingManager.kt         ← Google Play Billing
├── data/
│   ├── db/FrictionDatabase.kt            ← Room DB + DAOs
│   ├── model/Models.kt                   ← Data classes
│   └── repository/AppRepository.kt       ← Single source of truth
├── ui/
│   ├── screens/
│   │   ├── FrictionWallActivity.kt       ← The overlay screen
│   │   ├── HomeScreen.kt                 ← Dashboard
│   │   ├── PaywallScreen.kt              ← Premium upsell
│   │   └── AddAppScreen.kt              ← App picker
│   └── theme/FrictionTheme.kt           ← Colors + typography
├── utils/
│   └── Utils.kt                         ← Roast messages, schedule checker
└── MainActivity.kt                      ← Nav host
```

---

## Setup Guide

### Step 1: Android Studio Setup

1. Open Android Studio → **New Project from existing source** → select this folder
2. Wait for Gradle sync to complete
3. Add your `google-services.json` if you add Firebase later (optional)

### Step 2: Run on Device (Required — emulators can't test accessibility services properly)

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Enable Accessibility Service

1. On-device: **Settings → Accessibility → Installed Apps → Friction → Enable**
2. The app will guide users through this during onboarding with a dedicated screen

### Step 4: Google Play Console Setup

Before publishing, set up these subscription products in Play Console:

| Product ID | Price | Billing | Free Trial |
|---|---|---|---|
| `friction_premium_monthly` | $4.99 | Monthly | 3 days |
| `friction_premium_annual` | $29.99 | Annual | 3 days |

### Step 5: Play Store Testing

- Create a **closed testing track**
- Add 20 testers (minimum) for 14 days (Google requirement)
- Post on LinkedIn: "Beta testers wanted → lifetime premium in exchange"

---

## How the Interception Works

```
User taps Instagram
       ↓
AccessibilityService fires TYPE_WINDOW_STATE_CHANGED
       ↓
FrictionAccessibilityService.onAccessibilityEvent()
       ↓
Check: is this package in protected_apps DB?
       ↓ YES
Launch FrictionWallActivity with FLAG_ACTIVITY_NEW_TASK
(appears ON TOP of Instagram before it loads)
       ↓
User completes challenge → finish() → Instagram loads
```

### Battery Optimization Note

The `AccessibilityService` runs in the background. To prevent Android from killing it:
- Keep `notificationTimeout` low (100ms) in the config XML
- Avoid heavy work in `onAccessibilityEvent` — offload to coroutines immediately
- Consider showing a persistent notification (foreground service pattern) to reduce kill probability on aggressive OEMs (Samsung, Xiaomi)

---

## Free vs Premium

| Feature | Free | Premium |
|---|---|---|
| Protected apps | 1 | Unlimited |
| Breathing wall | ✓ | ✓ |
| Typing wall | ✓ | ✓ |
| Roast messages | ✓ | ✓ |
| Math Mode | ✗ | ✓ |
| Walk Mode | ✗ | ✓ |
| Strict Mode (no bypass) | ✗ | ✓ |
| Analytics dashboard | ✗ | ✓ |

---

## Marketing Checklist

- [ ] 7-second screen recording: Instagram → Math equation appears
- [ ] Post on TikTok/Reels/Shorts with caption "solve this or no brain rot"
- [ ] Screenshot the funniest roast message and post it
- [ ] ASO keywords: "app blocker", "screen time", "doomscroll", "focus"
- [ ] App title: "Friction: App Blocker & Screen Time"

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Interception**: AccessibilityService API
- **Storage**: Room Database (local only, no cloud)
- **Billing**: Google Play Billing Library 6.x
- **Preferences**: DataStore
- **Async**: Kotlin Coroutines + Flow
