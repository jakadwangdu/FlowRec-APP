# Compose (Kotlin) reference

Add theme, then components. Keep every composable stateless: pass state and callbacks in from the existing ViewModel.

## Theme

```kotlin
@Immutable
data class FlowColors(
    val bg: Color, val fg: Color, val muted: Color, val fill: Color,
    val separator: Color, val glass: Color, val sheet: Color,
    val record: Color, val onFg: Color
)

val DarkFlow = FlowColors(
    bg = Color(0xFF0A0A0A), fg = Color(0xFFF5F5F3), muted = Color(0xFFF5F5F3).copy(alpha = .47f),
    fill = Color.White.copy(alpha = .07f), separator = Color.White.copy(alpha = .08f),
    glass = Color(0xFF161616).copy(alpha = .72f), sheet = Color(0xFF121212).copy(alpha = .95f),
    record = Color(0xFFFF4533), onFg = Color(0xFF0A0A0A)
)
val LightFlow = FlowColors(
    bg = Color(0xFFF4F4F1), fg = Color(0xFF111111), muted = Color(0xFF111111).copy(alpha = .47f),
    fill = Color(0xFF111111).copy(alpha = .05f), separator = Color(0xFF111111).copy(alpha = .09f),
    glass = Color.White.copy(alpha = .80f), sheet = Color(0xFFF4F4F1).copy(alpha = .95f),
    record = Color(0xFFFF4533), onFg = Color(0xFFF4F4F1)
)

val LocalFlow = staticCompositionLocalOf { DarkFlow }
enum class ThemeMode { Auto, Light, Dark }

@Composable
fun FlowTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.Auto -> isSystemInDarkTheme(); ThemeMode.Dark -> true; ThemeMode.Light -> false }
    val c = if (dark) DarkFlow else LightFlow
    val animated = c.copy(
        bg = animateColorAsState(c.bg, tween(250)).value,
        fg = animateColorAsState(c.fg, tween(250)).value
    )
    val view = LocalView.current
    SideEffect {
        val w = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(w, false)
        WindowCompat.getInsetsController(w, view).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalFlow provides animated) {
        MaterialTheme(colorScheme = (if (dark) darkColorScheme() else lightColorScheme())
            .copy(background = animated.bg, surface = animated.bg, onBackground = animated.fg, onSurface = animated.fg),
            content = content)
    }
}
val flow: FlowColors @Composable get() = LocalFlow.current
```

Wrap the existing root content in `FlowTheme(mode = vm.themeMode)`. Read and write `themeMode` through the existing preferences layer.

## Shutter

```kotlin
@Composable
fun Shutter(recording: Boolean, size: Dp = 84.dp, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val inner by animateDpAsState(if (recording) size * .36f else size * .76f,
        spring(dampingRatio = .55f, stiffness = 400f), label = "inner")
    val radius by animateDpAsState(if (recording) 10.dp else size, tween(400), label = "r")
    Box(
        Modifier.size(size).border(4.dp, flow.fg, CircleShape).clip(CircleShape)
            .semantics { role = Role.Button; contentDescription = if (recording) "Stop recording" else "Start recording" }
            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        contentAlignment = Alignment.Center
    ) { Box(Modifier.size(inner).clip(RoundedCornerShape(radius)).background(flow.record)) }
}
```

## Segmented control

```kotlin
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(modifier.clip(CircleShape).background(flow.fill).padding(3.dp)) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, tween(400, easing = FastOutSlowInEasing), label = "seg")
        Box(Modifier.offset(x).width(w).height(34.dp).clip(CircleShape).background(flow.glass))
        Row {
            options.forEachIndexed { i, label ->
                Box(Modifier.width(w).height(34.dp).clip(CircleShape)
                    .clickable(role = Role.Tab) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(i) },
                    contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        color = if (i == selected) flow.fg else flow.muted)
                }
            }
        }
    }
}
```

## Switch (51 x 31)

```kotlin
@Composable
fun FlowSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val knob by animateDpAsState(if (checked) 20.dp else 0.dp, spring(.6f, 500f), label = "knob")
    Box(Modifier.size(51.dp, 31.dp).clip(CircleShape)
        .background(if (checked) flow.fg else flow.fill)
        .toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(2.dp)) {
        Box(Modifier.offset(knob).size(27.dp).clip(CircleShape).background(if (checked) flow.onFg else Color.White))
    }
}
```

## Floating dock

```kotlin
enum class Tab(val label: String, val icon: Int) { Home("Home", R.drawable.ic_home), Capture("Capture", R.drawable.ic_capture), Library("Library", R.drawable.ic_grid), Settings("Settings", R.drawable.ic_sliders) }

@Composable
fun BoxScope.Dock(current: Tab, onSelect: (Tab) -> Unit) {
    Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp)
        .shadow(12.dp, CircleShape).clip(CircleShape).background(flow.glass).padding(5.dp)) {
        Tab.values().forEach { t ->
            val on = t == current
            Box(Modifier.size(64.dp, 48.dp).clip(CircleShape).background(if (on) flow.fill else Color.Transparent)
                .clickable(role = Role.Tab) { onSelect(t) }, contentAlignment = Alignment.Center) {
                Icon(painterResource(t.icon), t.label, tint = if (on) flow.fg else flow.muted, modifier = Modifier.size(24.dp))
            }
        }
    }
}
```

Use `Haze` on the content behind for real blur: `Modifier.haze(state)` on the screen and `Modifier.hazeChild(state)` on the dock. Add `contentPadding(bottom = 110.dp)` to scrolling lists.

## Page transitions

```kotlin
AnimatedContent(targetState = tab, transitionSpec = {
    (fadeIn(tween(550, easing = LinearOutSlowInEasing)) + slideInVertically(tween(550)) { 60 })
        .togetherWith(fadeOut(tween(220)) + slideOutVertically(tween(220)) { -30 })
}, label = "tabs") { t -> when (t) { Tab.Home -> HomeScreen(...); Tab.Capture -> CaptureScreen(...); Tab.Library -> LibraryScreen(...); Tab.Settings -> SettingsScreen(...) } }
```

## Background rings (Capture and Home)

```kotlin
@Composable
fun Rings(recording: Boolean, reduceMotion: Boolean) {
    val t by rememberInfiniteTransition(label = "rings").animateFloat(0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "t")
    val tint = if (recording) flow.record else flow.fg
    Canvas(Modifier.fillMaxSize()) {
        listOf(1f, 1.35f, 1.75f).forEachIndexed { i, m ->
            val wobble = if (reduceMotion) 0f else sin(t * 2 - i) * if (recording) .05f else .02f
            drawCircle(tint.copy(alpha = .22f - i * .07f), size.width * .2f * m * (1 + wobble),
                Offset(size.width / 2, size.height * .4f), style = Stroke(1.dp.toPx()))
        }
    }
}
```

Reduced motion check: `Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f`.

## Quality sheet

Use `ModalBottomSheet` with `containerColor = flow.sheet`, `shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`, and three `Segmented` rows bound to the existing resolution, fps and audio settings. Update the settings on each change.

## Editor

Show with `AnimatedVisibility(visible, enter = slideInVertically(tween(600)) { it }, exit = slideOutVertically(tween(450)) { it })` over the whole screen. Timeline: a `Box` with `pointerInput(detectTapGestures)` for seek and `detectDragGestures` on each 22 dp zoom point; read and write zoom positions through the existing timeline state. Scale the preview with `animateFloatAsState(if (nearZoom) 1.35f else 1f, tween(600))`.

## Settings rows

A `Column` on `flow.fill` with `RoundedCornerShape(16.dp)`, rows 48 dp minimum, hairline `HorizontalDivider(color = flow.separator)` between rows. Value rows end with muted value text and a chevron. Add an **Appearance** row that cycles Auto, Light, Dark.
