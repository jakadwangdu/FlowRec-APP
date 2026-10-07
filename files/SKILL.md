---
name: flowrec-android-ui
description: Restyle a native Android app (Kotlin or Java, Jetpack Compose or XML Views) into the FlowRec minimalist, monochrome, immersive UI with dark and light themes, without changing any backend or working logic. Use when the user asks to redesign, re-theme or modernize the screens of an Android screen recorder or video editor app, mentions FlowRec, UI.md, a floating dock, shutter button, bottom sheet, dual theme or Apple-style minimal design on Android, or wants Compose or XML code for these components.
---

# FlowRec Android UI

Apply the FlowRec design to a real Android app. The design is minimal and monochrome: ink on paper, with red only while recording. Both dark and light themes are first-class.

## Hard rules (read first)

1. **UI only.** Never edit recording (MediaProjection, MediaRecorder, AudioPlaybackCapture), encoding, storage, Room or database code, export, AI processing, services, permissions, notifications, the Quick Settings tile, navigation route names or arguments.
2. Keep every existing feature and setting. Move or restyle, never delete.
3. Connect new UI to existing ViewModels, repositories and callbacks. Do not rewrite state logic to suit the UI. If an element has no data source, hide it or reuse an existing value.
4. Work one screen at a time. The app must compile and run after each screen. Run `./gradlew assembleDebug` after each step.
5. No hard-coded colors, sizes or strings in screens. Everything comes from theme tokens, dimens and string resources.
6. Use only free, open-source libraries.

## Step 1: Inspect the project

Before writing code, find out and state in one short message:
- Language: Kotlin or Java (or both)
- UI toolkit: Jetpack Compose, XML Views with ViewBinding, or mixed
- Navigation: Navigation Component, Compose Navigation, or manual
- Existing theme file, `minSdk`, `targetSdk`
- How screens read state (ViewModel with StateFlow, LiveData, callbacks)

Pick the matching reference:
- Compose (Kotlin): read `references/compose.md`
- XML Views (Kotlin or Java): read `references/views.md`
- Mixed: use Compose for new screens, Views reference for legacy screens, and share the same color tokens through `res/values` and `res/values-night`.

## Step 2: Design tokens

| Token | Dark | Light |
|---|---|---|
| bg | #0A0A0A | #F4F4F1 |
| fg | #F5F5F3 | #111111 |
| muted | fg at 47% | fg at 47% |
| fill | #FFFFFF at 7% | #111111 at 5% |
| separator | #FFFFFF at 8% | #111111 at 9% |
| glass | #161616 at 72% | #FFFFFF at 80% |
| sheet | #121212 at 95% | #F4F4F1 at 95% |
| record | #FF4533 | #FF4533 |
| onFg | #0A0A0A | #F4F4F1 |

Shapes: cards 20 dp, groups 16 dp, sheet top 28 dp, pills fully round. Side padding 20 dp. Minimum touch target 48 dp.
Type: system font (Roboto or Google Sans). Home headline 56 sp w600 with -5% tracking, clock 76 sp w200 with tabular figures, screen title 34 sp w700, body 15 sp, caption 13 sp. Sentence case only.

The full per-screen spec is in `UI.md` if the user has it in the repo. Follow it when it exists.

## Step 3: Build order

1. Theme tokens, dark and light, plus an **Appearance** setting (Auto, Light, Dark) stored with the app's existing preferences (DataStore or SharedPreferences). Apply it with `AppCompatDelegate.setDefaultNightMode` for Views or a theme parameter for Compose.
2. Edge-to-edge window with transparent system bars and correct icon contrast per theme.
3. Floating dock navigation with four tabs: Home, Capture, Library, Settings. Editor is not a tab; it opens full-screen from Library, Home or Capture.
4. Home, Capture, Library, Editor, Settings, Quality bottom sheet, in that order.
5. Motion, haptics, reduced-motion handling.
6. Accessibility pass and theme test.

## Step 4: Screen map (old to new)

| Old screen | New | Notes |
|---|---|---|
| Home | Home | Greeting, big record button, settings chip, recent row, Make it Flow row, storage line |
| Record | Capture | Mode selector, clock, waveform, shutter, thumbnail, countdown |
| Projects | Library | Search, filters, 2-column grid |
| Editor | Editor sheet | Preview, timeline with zoom points, AI tools, clip chips |
| Settings | Settings | Grouped rows, switches, Appearance row |
| Inline resolution, fps, audio | Quality sheet | Three segmented controls |

Mode Game must call the existing Game recording preset. Mode Voice must set the existing audio source to microphone only.

## Step 5: Motion and feel

- Page enter 550 ms decelerate, exit 220 ms. Never show two pages at once.
- Segmented indicator slides in 400 ms. Shutter circle morphs to a rounded square in 400 ms with slight overshoot.
- Sheet open 550 ms strong decelerate, close 400 ms. Drag down over 90 dp to dismiss.
- Haptics (light) on: mode change, segmented change, switch toggle, shutter press, zoom point release, AI action complete. Use `HapticFeedbackConstants` (`CONFIRM`, `CLOCK_TICK`, `CONTEXT_CLICK`) with fallbacks for older APIs.
- Reduced motion: if `Settings.Global.ANIMATOR_DURATION_SCALE` is 0, remove idle animations (button breathing, background rings, card tilt) and use fades.
- Backdrop blur (glass): Android 12+ can use `RenderEffect`. For Compose, the open-source Haze library (`dev.chrisbanes.haze`) gives true backdrop blur. On older devices raise glass opacity to 92%.

## Step 6: Accessibility and QA

- Contrast at least 4.5:1 in both themes. Muted text only for secondary text, at least 14 sp.
- Every icon-only button has `contentDescription`. Switches expose state with `Role.Switch` or `android:accessibilityLiveRegion` where needed.
- Recording state is shown by text ("REC") and shape (square shutter), not color alone.
- Test: dark, light, Auto; font scale 130%; display size large; TalkBack on; animations off; landscape and portrait; Android 10, 12 and latest.

## Libraries (all free and open source)

- Icons: Material Symbols (Outlined, weight 300) or Lucide through vector drawables
- Compose animation: built-in `androidx.compose.animation`
- Views animation: `ObjectAnimator`, `SpringAnimation` (`androidx.dynamicanimation`), `MaterialContainerTransform`
- Components for Views: Material Components (`com.google.android.material`)
- Blur: Haze (Compose), `RenderEffect` (API 31+)
- Optional: Lottie for a single hero animation if needed

## Output expectations

When finishing a screen, report: files changed, which existing callbacks or state it is wired to, and what was verified. If anything functional looks different from before, stop and say so. Do not guess at backend changes.

## Acceptance checklist

- [ ] No backend, recording, storage or AI file modified (check `git diff --stat`)
- [ ] All existing features and settings reachable and working
- [ ] No hard-coded colors; both themes match the token table
- [ ] Appearance Auto follows the system and updates live
- [ ] Red appears only while recording
- [ ] Only one page visible at a time; all four tabs open correctly
- [ ] Sheets and editor open, close and drag as specified
- [ ] Touch targets 48 dp or larger; TalkBack labels present
- [ ] Reduced motion removes idle animation
