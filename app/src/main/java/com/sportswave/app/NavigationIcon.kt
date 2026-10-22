package com.sportswave.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Density-independent artwork: no font or emoji substitution on different phones. */
class NavigationIcon(context: Context, private val name: String, color: Int) : View(context) {
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save(); canvas.scale(width / 24f, height / 24f)
        fun path(vararg points: Float) {
            val p = Path(); p.moveTo(points[0], points[1])
            for (i in 2 until points.size step 2) p.lineTo(points[i], points[i + 1])
            canvas.drawPath(p, pen)
        }
        when (name) {
            "Fullscreen" -> { path(9f,3f,3f,3f,3f,9f); path(15f,3f,21f,3f,21f,9f); path(3f,15f,3f,21f,9f,21f); path(21f,15f,21f,21f,15f,21f) }
            "Collapse" -> { path(3f,9f,9f,9f,9f,3f); path(15f,3f,15f,9f,21f,9f); path(3f,15f,9f,15f,9f,21f); path(15f,21f,15f,15f,21f,15f) }
            "Back" -> { path(14f,5f,7f,12f,14f,19f); path(7f,12f,21f,12f) }
            "Add" -> { path(12f,5f,12f,19f); path(5f,12f,19f,12f) }
            "Discover" -> { path(3f, 10f, 12f, 3f, 21f, 10f); path(5f, 9f, 5f, 21f, 19f, 21f, 19f, 9f); path(10f, 21f, 10f, 15f, 14f, 15f, 14f, 21f) }
            "Schedule" -> { canvas.drawRoundRect(3f, 5f, 21f, 21f, 2f, 2f, pen); path(3f, 10f, 21f, 10f); path(8f, 3f, 8f, 7f); path(16f, 3f, 16f, 7f); for (x in listOf(8f, 12f, 16f)) canvas.drawPoint(x, 15f, pen) }
            "Live" -> { path(3f, 12f, 7f, 12f, 10f, 5f, 14f, 19f, 17f, 12f, 21f, 12f) }
            "Channels" -> { canvas.drawRoundRect(2f, 5f, 22f, 19f, 3f, 3f, pen); path(10f, 9f, 16f, 12f, 10f, 15f, 10f, 9f); path(8f, 22f, 16f, 22f) }
            "Saved" -> { val p = Path(); p.moveTo(12f,21f); p.cubicTo(9f,18f,2f,13f,2f,8f); p.cubicTo(2f,2f,9f,1f,12f,6f); p.cubicTo(15f,1f,22f,2f,22f,8f); p.cubicTo(22f,13f,15f,18f,12f,21f); canvas.drawPath(p,pen) }
        }
        canvas.restore()
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}
