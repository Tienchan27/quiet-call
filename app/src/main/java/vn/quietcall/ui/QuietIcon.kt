package vn.quietcall.ui

import android.content.Context
import android.graphics.*
import android.view.View

enum class Glyph { HOME, BRAND, CLOCK, SETTINGS, SHIELD, PHONE, BLOCK, CHECK, PAUSE, PLAY, PLUS, SEARCH, CHEVRON, MORE, USER, BELL, IMPORT, EXPORT, FLASK, TRASH, LOCK, INFO, EDIT }

/** Small vector pictograms drawn at any density. No bitmap or external icon dependency. */
class QuietIcon(context: Context, private val glyph: Glyph, private val tint: Int) : View(context) {
    constructor(context: Context) : this(context, Glyph.SHIELD, Color.rgb(20, 125, 104))
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint; style = Paint.Style.STROKE; strokeWidth = 1.8f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val paths = mutableListOf<Path>()
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        fun line(x: Float, y: Float, x2: Float, y2: Float) { paths.add(Path().apply { moveTo(x, y); lineTo(x2, y2) }) }
        fun path(vararg points: Float) { paths.add(Path().apply { moveTo(points[0], points[1]); for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1]) }) }
        fun circle(x: Float, y: Float, radius: Float) { paths.add(Path().apply { addCircle(x, y, radius, Path.Direction.CW) }) }
        fun rect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) { paths.add(Path().apply { addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW) }) }
        fun arc(left: Float, top: Float, right: Float, bottom: Float, start: Float, sweep: Float) { paths.add(Path().apply { addArc(RectF(left, top, right, bottom), start, sweep) }) }
        when (glyph) {
            Glyph.HOME -> { path(3f, 10f, 12f, 3f, 21f, 10f); path(5f, 9f, 5f, 21f, 10f, 21f, 10f, 14f, 14f, 14f, 14f, 21f, 19f, 21f, 19f, 9f) }
            Glyph.BRAND -> { for (x in listOf(3f, 14f)) for (y in listOf(3f, 14f)) rect(x, y, x + 7, y + 7, 2f) }
            Glyph.CLOCK -> { circle(12f, 12f, 9f); path(12f, 6f, 12f, 12f, 16f, 14f) }
            Glyph.SETTINGS -> { for ((y, x) in listOf(6f to 8f, 12f to 16f, 18f to 10f)) { line(3f, y, x - 2, y); line(x + 2, y, 21f, y); circle(x, y, 2f) } }
            Glyph.SHIELD -> { paths.add(Path().apply { moveTo(12f, 2f); lineTo(21f, 6f); lineTo(21f, 12f); cubicTo(21f, 17f, 16f, 21f, 12f, 23f); cubicTo(8f, 21f, 3f, 17f, 3f, 12f); lineTo(3f, 6f); close() }); path(7.5f, 12f, 10.5f, 15f, 16.5f, 9f) }
            Glyph.PHONE, Glyph.BLOCK -> { paths.add(Path().apply { moveTo(6f, 3f); lineTo(3f, 5f); cubicTo(3f, 13f, 11f, 21f, 19f, 21f); lineTo(21f, 18f); lineTo(16f, 14f); lineTo(13f, 16f); lineTo(8f, 11f); lineTo(10f, 8f); close() }); if (glyph == Glyph.BLOCK) { line(15f, 3f, 21f, 9f); line(21f, 3f, 15f, 9f) } }
            Glyph.CHECK -> path(4f, 12f, 9f, 17f, 20f, 6f)
            Glyph.PAUSE -> { line(8f, 5f, 8f, 19f); line(16f, 5f, 16f, 19f) }
            Glyph.PLAY -> path(7f, 4f, 20f, 12f, 7f, 20f, 7f, 4f)
            Glyph.PLUS -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
            Glyph.SEARCH -> { circle(10f, 10f, 6.5f); line(15f, 15f, 21f, 21f) }
            Glyph.CHEVRON -> path(9f, 5f, 16f, 12f, 9f, 19f)
            Glyph.MORE -> { for (y in listOf(5f, 12f, 19f)) circle(12f, y, 1f) }
            Glyph.USER -> { circle(12f, 7f, 4f); arc(4f, 13f, 20f, 27f, 180f, 180f) }
            Glyph.BELL -> { path(5f, 17f, 7f, 14f, 7f, 8f); arc(7f, 3f, 17f, 13f, 180f, 180f); path(17f, 8f, 17f, 14f, 19f, 17f, 5f, 17f); arc(10f, 18f, 14f, 22f, 0f, 180f) }
            Glyph.IMPORT, Glyph.EXPORT -> { val down = glyph == Glyph.IMPORT; line(12f, 3f, 12f, 15f); if (down) path(7f, 10f, 12f, 15f, 17f, 10f) else path(7f, 8f, 12f, 3f, 17f, 8f); path(4f, 15f, 4f, 21f, 20f, 21f, 20f, 15f) }
            Glyph.FLASK -> { path(8f, 3f, 16f, 3f); path(9f, 3f, 9f, 10f, 3f, 19f, 4f, 21f, 20f, 21f, 21f, 19f, 15f, 10f, 15f, 3f); line(6f, 16f, 18f, 16f) }
            Glyph.TRASH -> { line(4f, 6f, 20f, 6f); path(8f, 6f, 8f, 3f, 16f, 3f, 16f, 6f); path(6f, 6f, 7f, 21f, 17f, 21f, 18f, 6f); line(10f, 10f, 10f, 17f); line(14f, 10f, 14f, 17f) }
            Glyph.LOCK -> { rect(5f, 10f, 19f, 22f, 3f); arc(8f, 2f, 16f, 14f, 180f, 180f); line(12f, 15f, 12f, 18f) }
            Glyph.INFO -> { circle(12f, 12f, 9f); line(12f, 11f, 12f, 17f); circle(12f, 7f, 0.5f) }
            Glyph.EDIT -> { path(4f, 16f, 4f, 21f, 9f, 21f, 21f, 9f, 16f, 4f, 4f, 16f); line(13f, 7f, 18f, 12f) }
        }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save(); canvas.scale(width / 24f, height / 24f)
        for (i in paths.indices) canvas.drawPath(paths[i], paint)
        canvas.restore()
    }
}

/** Decorative protection illustration; no score or percentage is invented. */
class ShieldScene(context: Context, private val active: Boolean) : View(context) {
    constructor(context: Context) : this(context, true)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shield = Path().apply { moveTo(70f, 32f); lineTo(101f, 44f); lineTo(101f, 70f); cubicTo(101f, 91f, 83f, 104f, 70f, 111f); cubicTo(57f, 104f, 39f, 91f, 39f, 70f); lineTo(39f, 44f); close() }
    private val check = Path().apply { moveTo(56f, 69f); lineTo(66f, 79f); lineTo(85f, 59f) }
    private val gradient = LinearGradient(45f, 35f, 90f, 110f, Color.WHITE, Color.rgb(185, 237, 217), Shader.TileMode.CLAMP)
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(c: Canvas) {
        c.save(); c.scale(width / 140f, height / 140f)
        paint.color = Color.argb(28, 255, 255, 255); paint.style = Paint.Style.FILL
        c.drawCircle(70f, 70f, 65f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f
        c.drawCircle(70f, 70f, 52f, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.argb(40, 255, 255, 255)
        c.drawCircle(70f, 70f, 40f, paint)
        paint.alpha = 255; paint.shader = gradient
        c.drawPath(shield, paint); paint.shader = null
        paint.color = Color.rgb(20, 125, 104); paint.style = Paint.Style.STROKE; paint.strokeWidth = 5f; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        if (active) c.drawPath(check, paint)
        else { c.drawLine(63f, 60f, 63f, 80f, paint); c.drawLine(78f, 60f, 78f, 80f, paint) }
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(161, 224, 195)
        c.drawCircle(116f, 36f, 7f, paint); c.drawCircle(20f, 98f, 4f, paint); c.drawCircle(106f, 118f, 3f, paint)
        c.restore()
    }
}
