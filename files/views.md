# XML Views reference (Kotlin or Java)

## Themes and tokens

`res/values/colors.xml` (light) and `res/values-night/colors.xml` (dark):

```xml
<!-- values/colors.xml (light) -->
<resources>
  <color name="flow_bg">#F4F4F1</color>
  <color name="flow_fg">#111111</color>
  <color name="flow_muted">#78111111</color>
  <color name="flow_fill">#0D111111</color>
  <color name="flow_separator">#17111111</color>
  <color name="flow_glass">#CCFFFFFF</color>
  <color name="flow_sheet">#F2F4F4F1</color>
  <color name="flow_record">#FF4533</color>
  <color name="flow_on_fg">#F4F4F1</color>
</resources>
<!-- values-night/colors.xml (dark) -->
<resources>
  <color name="flow_bg">#0A0A0A</color>
  <color name="flow_fg">#F5F5F3</color>
  <color name="flow_muted">#78F5F5F3</color>
  <color name="flow_fill">#12FFFFFF</color>
  <color name="flow_separator">#14FFFFFF</color>
  <color name="flow_glass">#B8161616</color>
  <color name="flow_sheet">#F2121212</color>
  <color name="flow_record">#FF4533</color>
  <color name="flow_on_fg">#0A0A0A</color>
</resources>
```

`res/values/themes.xml`:

```xml
<style name="Theme.FlowRec" parent="Theme.Material3.DayNight.NoActionBar">
  <item name="android:windowBackground">@color/flow_bg</item>
  <item name="android:statusBarColor">@android:color/transparent</item>
  <item name="android:navigationBarColor">@android:color/transparent</item>
  <item name="android:textColorPrimary">@color/flow_fg</item>
  <item name="android:textColorSecondary">@color/flow_muted</item>
  <item name="colorSurface">@color/flow_bg</item>
  <item name="colorOnSurface">@color/flow_fg</item>
  <item name="colorPrimary">@color/flow_fg</item>
  <item name="colorOnPrimary">@color/flow_on_fg</item>
</style>
```

Reference tokens only through `@color/flow_*`. Never use literal hex in layouts.

## Appearance setting (Kotlin)

```kotlin
fun applyTheme(mode: String) = AppCompatDelegate.setDefaultNightMode(when (mode) {
    "Light" -> AppCompatDelegate.MODE_NIGHT_NO
    "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM })
```

Call it in `Application.onCreate` with the stored value, and from the Appearance row.

## Edge-to-edge

```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
    val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    v.updatePadding(top = b.top, bottom = b.bottom); insets }
```

## Shapes and surfaces

`res/drawable/bg_fill_card.xml`:

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
  <solid android:color="@color/flow_fill"/><corners android:radius="20dp"/>
</shape>
```

Make similar drawables: `bg_glass_pill` (radius 99dp, `flow_glass`), `bg_sheet_top` (top radii 28dp, `flow_sheet`).

## Floating dock

Use a `LinearLayout` (horizontal, `bg_glass_pill`, padding 5dp, `elevation` 12dp) anchored with `layout_gravity="bottom|center_horizontal"` and a bottom margin of 18dp plus the nav bar inset. Each tab is an `ImageButton` 64 x 48 dp with a `selector` background: `flow_fill` pill when selected. Add `contentDescription`. Give scrolling content `android:paddingBottom="110dp"` and `clipToPadding="false"`.

## Shutter (custom View, Java)

```java
public class ShutterView extends View {
  private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG), dot = new Paint(Paint.ANTI_ALIAS_FLAG);
  private float t = 0f; // 0 idle (circle), 1 recording (rounded square)
  private ValueAnimator anim;
  public ShutterView(Context c, AttributeSet a) { super(c, a);
    ring.setStyle(Paint.Style.STROKE); ring.setStrokeWidth(dp(4)); ring.setColor(ContextCompat.getColor(c, R.color.flow_fg));
    dot.setColor(ContextCompat.getColor(c, R.color.flow_record)); setContentDescription("Start recording"); }
  public void setRecording(boolean rec) {
    if (anim != null) anim.cancel();
    anim = ValueAnimator.ofFloat(t, rec ? 1f : 0f); anim.setDuration(400);
    anim.setInterpolator(new OvershootInterpolator(1.6f));
    anim.addUpdateListener(v -> { t = (float) v.getAnimatedValue(); invalidate(); }); anim.start();
    setContentDescription(rec ? "Stop recording" : "Start recording"); }
  @Override protected void onDraw(Canvas c) {
    float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - dp(2);
    c.drawCircle(cx, cy, r, ring);
    float half = r * (0.76f - 0.4f * t), rad = half * (1f - 0.8f * t);
    c.drawRoundRect(cx - half, cy - half, cx + half, cy + half, rad, rad, dot); }
  private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
```

Call `performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)` in the click listener.

## Segmented control

Use `MaterialButtonToggleGroup` with `app:singleSelection="true"` and `app:selectionRequired="true"`, buttons styled as text buttons with a `flow_fill` track and `flow_glass` selected state. For the sliding indicator, add a `View` behind the group and animate its `translationX` and width with `ObjectAnimator` (400 ms, `DecelerateInterpolator`).

## Switch

Use `MaterialSwitch` (Material 1.8+). Track tint: checked `flow_fg`, unchecked `flow_fill`. Thumb tint: checked `flow_on_fg`, unchecked white. Size via the default style (about 52 x 32 dp).

## Bottom sheet (Quality)

Extend `BottomSheetDialogFragment`. Set `background = bg_sheet_top`, draw a 38 x 5 dp grab handle, add three segmented groups bound to the existing settings. Material's behavior handles drag-to-dismiss. Show with `QualitySheet().show(supportFragmentManager, "quality")`.

## Editor

Show as a full-screen `DialogFragment` or a destination with a vertical slide animation (`ObjectAnimator.ofFloat(root, "translationY", height, 0f)`, 600 ms, `DecelerateInterpolator(2f)`). Timeline is a custom `View` or `FrameLayout`; use `setOnTouchListener` for seek and drag of the 22 dp zoom point views. Scale the preview with `animate().scaleX(1.35f).scaleY(1.35f).setDuration(600)` when the playhead is within 9% of a zoom point.

## Page transitions

For Navigation Component, set `enterAnim` (fade in plus translationY 24dp, 550 ms), `exitAnim` (fade out plus translationY -10dp, 220 ms), `popEnterAnim` and `popExitAnim` in the nav graph. Use `res/anim` XML or `Fragment` transitions. Ensure the old fragment is fully removed or hidden.

## Reduced motion

```kotlin
fun reduceMotion(c: Context) = Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
```

Skip idle animations (breathing button, rings, card tilt) when true.

## Backdrop blur

API 31+: `view.setRenderEffect(RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP))` blurs the view's own content. For glass over other content, use a blurred snapshot or raise glass alpha to about 0.92 on older devices.
