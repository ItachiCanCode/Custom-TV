package com.example.tvlauncher

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.*

class MainActivity : Activity() {

    data class AppEntry(val label: String, val pkg: String, val info: ResolveInfo)

    private lateinit var content: LinearLayout
    private val apps = mutableListOf<AppEntry>()
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }
    private var dp = 1f
    private var firstTile: View? = null
    private var pendingFocusPkg: String? = null
    private val allTiles = mutableMapOf<String, View>()
    private val ease = PathInterpolator(0.2f, 0f, 0f, 1f)   // fast start, long soft landing
    private var firstRender = true
    private var crashShown = false

    // Scroll view that doesn't jump on focus; we animate the scroll ourselves.
    class SmoothScroll(c: android.content.Context) : HorizontalScrollView(c) {
        override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { prefs.edit().putString("crash", Log.getStackTraceString(e)).commit() } catch (_: Throwable) {}
            old?.uncaughtException(t, e)
        }
        val saved = prefs.getString("crash", null)
        if (saved != null) { showCrash(saved); return }
        try { buildUi() } catch (e: Throwable) { showCrash(Log.getStackTraceString(e)) }
    }

    private fun showCrash(text: String) {
        crashShown = true
        val tv = TextView(this).apply {
            this.text = "Launcher error. Photo this screen, then press OK to retry.\n\n" + text
            textSize = 13f; setTextColor(Color.WHITE); setPadding(48, 32, 48, 32)
        }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#400000")); addView(tv) })
    }

    private fun buildUi() {
        dp = resources.displayMetrics.density
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding((56 * dp).toInt(), (32 * dp).toInt(), (56 * dp).toInt(), (24 * dp).toInt())
        }
        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#26365C"), Color.parseColor("#0A0D17"))
            )
            clipChildren = false
            addView(ScrollView(this@MainActivity).apply {
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                clipChildren = false; clipToPadding = false
                isFillViewport = true
                addView(content)
            })
        }
        // Soft drifting color blobs: they give the glass something to "frost".
        root.addView(blob("#553A7BFF", 520, Gravity.TOP or Gravity.END, -120, -160, -60f, 40f, 22000), 0)
        root.addView(blob("#55A64DFF", 560, Gravity.BOTTOM or Gravity.START, -160, -200, 70f, -30f, 26000), 0)
        root.addView(blob("#3319C7B5", 420, Gravity.CENTER, 0, 120, 50f, 50f, 30000), 0)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (crashShown) return
        try { loadApps(); render() } catch (e: Throwable) { showCrash(Log.getStackTraceString(e)) }
    }

    private fun blob(color: String, sizeDp: Int, gravity: Int, mx: Int, my: Int,
                     dx: Float, dy: Float, ms: Long): View {
        val v = View(this)
        v.background = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = sizeDp / 2f * dp
            colors = intArrayOf(Color.parseColor(color), Color.TRANSPARENT)
        }
        val lp = FrameLayout.LayoutParams((sizeDp * dp).toInt(), (sizeDp * dp).toInt(), gravity)
        lp.setMargins((mx * dp).toInt(), (my * dp).toInt(), (mx * dp).toInt(), (my * dp).toInt())
        v.layoutParams = lp
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null)   // composite on the GPU instead of redrawing
        val lowRam = (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
        if (!lowRam) for ((prop, d) in listOf("translationX" to dx, "translationY" to dy)) {
            ObjectAnimator.ofFloat(v, prop, 0f, d * dp).apply {
                duration = ms; repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator(); start()
            }
        }
        return v
    }

    private fun loadApps() {
        val pm = packageManager
        fun query(cat: String) = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
        apps.clear()
        apps += (query(Intent.CATEGORY_LEANBACK_LAUNCHER) + query(Intent.CATEGORY_LAUNCHER))
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName, it) }
            .sortedBy { it.label.lowercase() }
    }

    private fun favs(): Set<String> = prefs.getStringSet("favs", emptySet()) ?: emptySet()

    /** Focusable glass pill in the header that opens a system settings screen. */
    private fun headerButton(label: String, action: String): View {
        val glass = GlassDrawable(24 * dp, 1.5f * dp, true)
        return TextView(this).apply {
            text = label; textSize = 20f; setTextColor(Color.WHITE)
            background = glass
            isFocusable = true; isClickable = true
            setPadding((22 * dp).toInt(), (8 * dp).toInt(), (22 * dp).toInt(), (8 * dp).toInt())
            setOnFocusChangeListener { _, f -> glass.intensity = if (f) 1f else 0f }
            setOnClickListener {
                try { startActivity(Intent(action)) } catch (_: Throwable) {}
            }
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = (16 * dp).toInt() }
        }
    }

    private fun render() {
        content.removeAllViews()
        allTiles.clear()
        firstTile = null

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "Home"; textSize = 30f; setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(headerButton("Wi-Fi", android.provider.Settings.ACTION_WIFI_SETTINGS))
        header.addView(headerButton("Settings", android.provider.Settings.ACTION_SETTINGS))
        header.addView(TextClock(this).apply {
            format12Hour = "EEE, MMM d"; format24Hour = "EEE, MMM d"
            textSize = 20f; setTextColor(Color.WHITE)
            background = GlassDrawable(24 * dp, 1.5f * dp, true)
            setPadding((22 * dp).toInt(), (8 * dp).toInt(), (22 * dp).toInt(), (8 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = (16 * dp).toInt() }
        })
        header.addView(TextClock(this).apply {
            format12Hour = "h:mm a"; format24Hour = "HH:mm"
            textSize = 22f; setTextColor(Color.WHITE)
            background = GlassDrawable(24 * dp, 1.5f * dp, true)
            setPadding((22 * dp).toInt(), (8 * dp).toInt(), (22 * dp).toInt(), (8 * dp).toInt())
        })
        content.addView(header)

        val favSet = favs()
        val favApps = apps.filter { it.pkg in favSet }
        if (favApps.isNotEmpty()) section("Favorites", favApps, 300, 169)
        else content.addView(TextView(this).apply {
            text = "Long-press any app to pin it here"
            textSize = 20f; setTextColor(Color.parseColor("#CCFFFFFF"))
            setPadding(0, (28 * dp).toInt(), 0, 0)
        })
        gridSection("All Apps", apps, 200, 113)

        content.post {
            val target = pendingFocusPkg?.let { allTiles[it] } ?: firstTile
            pendingFocusPkg = null
            target?.requestFocus()
        }
        if (firstRender) {
            firstRender = false
            content.alpha = 0f
            content.translationY = 24 * dp
            content.animate().alpha(1f).translationY(0f).setDuration(450).setInterpolator(ease).start()
        }
    }

    private fun centerInRow(v: View) {
        val hsv = (v.parent as? View)?.parent as? HorizontalScrollView ?: return
        val target = (v.left + v.width / 2 - hsv.width / 2).coerceAtLeast(0)
        (hsv.tag as? Animator)?.cancel()
        hsv.tag = ObjectAnimator.ofInt(hsv, "scrollX", hsv.scrollX, target).apply {
            duration = 340; interpolator = ease; start()
        }
    }

    private fun sectionTitle(title: String) = TextView(this).apply {
        text = title; textSize = 22f; setTextColor(Color.parseColor("#E6FFFFFF"))
        setPadding(0, (28 * dp).toInt(), 0, 0)
    }

    /** All apps as a wrapping grid so any app is a few D-pad presses away. */
    private fun gridSection(title: String, list: List<AppEntry>, w: Int, h: Int) {
        content.addView(sectionTitle(title))
        val availDp = resources.displayMetrics.widthPixels / dp - 112f
        val cols = ((availDp + 20f) / (w + 20f)).toInt().coerceAtLeast(1)
        val grid = GridLayout(this).apply {
            columnCount = cols
            clipChildren = false; clipToPadding = false
            setPadding(0, (22 * dp).toInt(), 0, (22 * dp).toInt())
        }
        list.forEach { e ->
            val t = tile(e, w, h)
            if (firstTile == null) firstTile = t
            allTiles[e.pkg] = t
            grid.addView(t, GridLayout.LayoutParams().apply {
                setMargins(0, 0, (20 * dp).toInt(), (14 * dp).toInt())
            })
        }
        content.addView(grid, LinearLayout.LayoutParams(-1, -2))
    }

    private fun section(title: String, list: List<AppEntry>, w: Int, h: Int, trackAll: Boolean = false) {
        content.addView(sectionTitle(title))
        val row = LinearLayout(this).apply { clipChildren = false; clipToPadding = false }
        list.forEach { e ->
            val tile = tile(e, w, h)
            if (firstTile == null) firstTile = tile
            if (trackAll) allTiles[e.pkg] = tile
            row.addView(tile, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = (20 * dp).toInt() })
        }
        content.addView(SmoothScroll(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false; clipToPadding = false
            setPadding(0, (22 * dp).toInt(), 0, (22 * dp).toInt())
            addView(row)
        }, LinearLayout.LayoutParams(-1, -2))
    }

    private fun tile(e: AppEntry, w: Int, h: Int): View {
        val pm = packageManager
        val wPx = (w * dp).toInt(); val hPx = (h * dp).toInt()

        val glassBg = GlassDrawable(20 * dp, 1.5f * dp, true)
        val glassFg = GlassDrawable(20 * dp, 1.5f * dp, false)  // edge highlight over banners
        val card = FrameLayout(this).apply {
            background = glassBg
            clipToOutline = true
            elevation = 4 * dp
        }
        val banner = e.info.activityInfo.loadBanner(pm)
        if (banner != null) {
            card.foreground = glassFg
            card.addView(ImageView(this).apply {
                setImageDrawable(banner); scaleType = ImageView.ScaleType.CENTER_CROP
            }, FrameLayout.LayoutParams(-1, -1))
        } else {
            val s = (hPx * 0.55f).toInt()
            card.addView(ImageView(this).apply { setImageDrawable(e.info.loadIcon(pm)) },
                FrameLayout.LayoutParams(s, s, Gravity.CENTER))
        }

        val label = TextView(this).apply {
            text = e.label; textSize = 18f; setTextColor(Color.WHITE)
            setShadowLayer(4 * dp, 0f, 1 * dp, Color.parseColor("#99000000"))
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER; alpha = 0.8f
            setPadding(0, (10 * dp).toInt(), 0, 0)
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isFocusable = true; isClickable = true
            clipChildren = false
            addView(card, LinearLayout.LayoutParams(wPx, hPx))
            addView(label, LinearLayout.LayoutParams(wPx, -2))

            setOnFocusChangeListener { v, focused ->
                val s = if (focused) 1.1f else 1f
                animate().scaleX(s).scaleY(s).setDuration(260).setInterpolator(ease).start()
                card.animate().translationZ(if (focused) 10 * dp else 0f)
                    .setDuration(260).setInterpolator(ease).start()
                ValueAnimator.ofFloat(glassBg.intensity, if (focused) 1f else 0f).apply {
                    duration = 260; interpolator = ease
                    addUpdateListener {
                        val f = it.animatedValue as Float
                        glassBg.intensity = f; glassFg.intensity = f
                    }
                    start()
                }
                label.animate().alpha(if (focused) 1f else 0.8f)
                    .setDuration(260).setInterpolator(ease).start()
                if (focused) centerInRow(v)
            }
            setOnClickListener { v ->
                (pm.getLeanbackLaunchIntentForPackage(e.pkg) ?: pm.getLaunchIntentForPackage(e.pkg))
                    ?.let {
                        val o = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.width, v.height)
                        startActivity(it, o.toBundle())
                    }
            }
            setOnLongClickListener {
                val set = favs().toMutableSet()
                val pinned = set.add(e.pkg)
                if (!pinned) set.remove(e.pkg)
                prefs.edit().putStringSet("favs", set).apply()
                Toast.makeText(context, if (pinned) "Pinned to Favorites" else "Removed from Favorites",
                    Toast.LENGTH_SHORT).show()
                pendingFocusPkg = e.pkg
                render()
                true
            }
        }
    }

    // Home screen shouldn't close on Back.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (crashShown && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
            getSharedPreferences("launcher", MODE_PRIVATE).edit().remove("crash").apply()
            recreate(); return true
        }
        return if (keyCode == KeyEvent.KEYCODE_BACK) true else super.onKeyDown(keyCode, event)
    }
}


/** Frosted-glass look: translucent fill, top sheen, and a bright gradient edge. */
class GlassDrawable(
    private val radius: Float,
    private val strokePx: Float,
    private val drawFill: Boolean
) : Drawable() {

    var intensity = 0f
        set(v) { field = v; invalidateSelf() }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = strokePx
    }
    private val rect = RectF()

    override fun onBoundsChange(b: Rect) {
        sheenPaint.shader = LinearGradient(
            0f, b.top.toFloat(), 0f, b.bottom.toFloat(),
            intArrayOf(0x55FFFFFF, 0x00FFFFFF, 0x10FFFFFF), floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        strokePaint.shader = LinearGradient(
            b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(),
            intArrayOf(-0x1, 0x30FFFFFF, 0x90FFFFFF.toInt()), floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(c: Canvas) {
        val h = strokePx / 2f
        rect.set(bounds.left + h, bounds.top + h, bounds.right - h, bounds.bottom - h)
        if (drawFill) {
            fillPaint.alpha = (34 + 46 * intensity).toInt()
            c.drawRoundRect(rect, radius, radius, fillPaint)
            sheenPaint.alpha = (255 * (0.7f + 0.3f * intensity)).toInt()
            c.drawRoundRect(rect, radius, radius, sheenPaint)
        }
        strokePaint.alpha = (255 * (0.5f + 0.5f * intensity)).toInt()
        c.drawRoundRect(rect, radius, radius, strokePaint)
    }

    override fun getOutline(outline: Outline) {
        outline.setRoundRect(bounds, radius)
        outline.alpha = 0.3f
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
