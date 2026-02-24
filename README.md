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


---
### PRD

The "Friction" App: Monetizing Dopamine Detox

The concept is simple: An app that makes opening addictive apps (like Instagram, TikTok, Twitter) annoying.

When a user taps Instagram, your app intercepts it and puts up a "Wall". To get past the wall, the user has to do something slightly frustrating, like wait 10 seconds while doing a breathing exercise, or type out the sentence "I am choosing to waste my time." This type of app (look at "One Sec" or "Opal" on iOS) makes millions, goes viral easily because of the funny/aggressive prompts, and is very easy to build on Android.

1. Feature Breakdown (The Funnel)

The Free Tier (The Hook - Optimized for Virality)

1 App Limit: They can only "protect" one app (usually Instagram or TikTok).

Standard Friction: A 5-second breathing animation before the app opens.

The "Roast" Screen: If they open the app more than 5 times in an hour, the app shows a passive-aggressive message (e.g., "Don't you have code to write?"). This is the highly shareable feature users will screenshot and post online.

The Premium Tier ($4.99/mo or $29.99/year - The Money Maker)

Unlimited Apps: Block/add friction to as many apps as they want.

Hardcore Modes: * Math Mode: Solve a medium-difficulty math equation to open the app.

Walk Mode: Walk 50 steps to unlock the app.

Scheduling: "Strict Mode" during work hours (9 AM - 5 PM) where bypassing is impossible.

Guilt Trip Analytics: A dashboard showing exactly how many hours of their life your app gave back to them this week.

2. Generating UI Mocks

Do not reinvent the wheel here. The app needs to look premium and minimal so people trust it enough to pay.

Steal the UX Flow: Go to Mobbin.com. It’s a library of real-world app designs. Search for apps like Headspace, Opal, or Forest. Look at how they design their paywalls and settings screens.

Mock it up: Use Figma (free).

Use a UI Kit: Inside Figma, search the Community tab for the "Material 3 Design Kit". Just drag and drop the toggles, cards, and buttons.

The Vibe: Keep it dark mode by default. Use minimal text, lots of negative space, and a single accent color (like a bright neon green or stark white) so it looks sleek and modern.

3. The End-to-End Build Process

Here is the exact technical and launch path for a solo dev.

Step 1: Core Tech & Architecture (Week 1)

The Magic API: The core of this app relies on Android's AccessibilityService API and UsageStatsManager. This allows your app to detect when the user launches Instagram and instantly draw a screen over it.

UI Stack: Use Kotlin + Jetpack Compose. It's the fastest way to build beautiful Android UIs right now.

Storage: Room Database (local). Save their app preferences, bypass history, and screen time locally. No cloud backend needed. No AWS bills.

Step 2: Build the MVP (Week 2-3)

Build the onboarding flow (requesting Accessibility permissions is the biggest hurdle, make it clear why you need it).

Implement the interception logic.

Build the "Breathing" bypass screen.

Crucial: Implement the Google Play Billing Library for the Premium tier immediately. Put the paywall right after they see their first "Time Saved" stat.

Step 3: Closed Testing & App Store Optimization (Week 4)

Google requires 20 testers for 14 days. You have 12k LinkedIn followers. Make a post asking for testers in exchange for lifetime premium. You'll get 100 signups in ten minutes.

ASO (App Store Optimization): * Title: "Friction: App Blocker & Screen Time"

Screenshots: Use Figma to create clean screenshots showing the exact pain point. "Stop Doomscrolling." "Reclaim 3 hours a day."

Step 4: Marketing for Virality (The Launch)

You don't run paid ads for this. You use short-form video.

Make a 7-second screen recording showing someone trying to open TikTok, and your app popping up saying: "Solve 14 x 8 to rot your brain." * Post it on TikTok, Instagram Reels, and YouTube Shorts. The sheer relatable frustration of it makes people tag their friends.

4. Your LinkedIn & Substack Content Strategy

Just because this app isn't about SDE interviews doesn't mean you can't use it for your toinfinitescale audience. SDE-2s and SDE-3s are obsessed with productivity, side-hustles, and building products.

Use the "Build in Public" angle to grow your newsletter while building the app.

Week 1: The Product Mindset

Saturday Substack: Title: "Why great SDEs build products, not just features (My side-hustle blueprint)."

Angle: Talk about how moving from SDE-2 to SDE-3 requires "Product Sense." To build this sense, you are building an Android app from scratch that optimizes for revenue. Break down why you chose a productivity app over a complex SaaS.

Sunday LinkedIn: * Hook: "Coding fast won't get you to SDE-3. Understanding what users actually pay for will."

Body: Summarize the Substack. Explain the concept of the app (monetizing friction). Link the newsletter for the full technical breakdown.

Wednesday LinkedIn: * Hook: "I just paid $25 for a Google Play account. Here is the exact tech stack I'm using to build an app in 3 weeks."

Body: List Kotlin, Compose, Room DB, and the AccessibilityService API. Ask your audience if they have ever built a mobile app to scratch their own itch.

Week 4: The Launch & Marketing

Saturday Substack: Title: "The system design of a viral app (and how I launched it)."

Angle: Break down the architecture (Accessibility Service limits, keeping battery usage low) and how you handled the Google Play testing requirements.

Sunday LinkedIn: * Hook: "I just launched my first Android app. Here is how I got 20 beta testers in 10 minutes without spending a dime."

Body: Talk about leveraging your audience. SDEs need to learn distribution, not just code.

Wednesday LinkedIn:

Hook: "The hardest part of building my app wasn't the code. It was the paywall."

Body: A quick tactical tip on why engineers suck at monetization and how you designed a simple free vs. premium model.