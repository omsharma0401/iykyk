package com.omsharma.iykyk.utils.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.omsharma.iykyk.constants.CollageSpec
import com.omsharma.iykyk.data.model.PersonResult

// Draws the 1080x1920 story collage
object CollageRenderer {

    fun render(people: List<PersonResult>): Bitmap {
        require(people.isNotEmpty()) { "Nothing to render" }
        val shown = people
        val bitmap = Bitmap.createBitmap(CollageSpec.WIDTH, CollageSpec.HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBackground(canvas)
        drawHeader(canvas, people)
        val cells = CollageLayout.cells(shown.size)
        shown.forEachIndexed { index, person -> drawTile(canvas, person, cells[index]) }
        return bitmap
    }

    private fun drawBackground(canvas: Canvas) {
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, CollageSpec.HEIGHT.toFloat(), CollageSpec.BACKGROUND_TOP, CollageSpec.BACKGROUND_BOTTOM, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, CollageSpec.WIDTH.toFloat(), CollageSpec.HEIGHT.toFloat(), paint)
    }

    private fun drawHeader(canvas: Canvas, people: List<PersonResult>) {
        val appearances = people.sumOf { it.appearanceCount }
        val title = if (people.size == 1) "1 person" else "${people.size} people"
        canvas.drawText(title, CollageSpec.MARGIN, 96f, titlePaint)
        canvas.drawText("$appearances appearances in this video", CollageSpec.MARGIN, 142f, subtitlePaint)
    }

    private fun drawTile(canvas: Canvas, person: PersonResult, cell: RectF) {
        val clip = Path().apply { addRoundRect(cell, CollageSpec.CORNER_RADIUS, CollageSpec.CORNER_RADIUS, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)

        val source = CollageLayout.fitCrop(person.photo.width, person.photo.height, person.faceBox, cell.width() / cell.height())
        canvas.drawBitmap(person.photo, source, cell, photoPaint)

        val scrimTop = cell.bottom - CollageSpec.SCRIM_HEIGHT
        val scrim = Paint().apply {
            shader = LinearGradient(0f, scrimTop, 0f, cell.bottom, Color.TRANSPARENT, Color.argb(190, 0, 0, 0), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(cell.left, scrimTop, cell.right, cell.bottom, scrim)

        val baseline = cell.bottom - CollageSpec.CAPTION_INSET
        canvas.drawText(person.label, cell.left + CollageSpec.CAPTION_INSET, baseline, labelPaint)
        drawCountPill(canvas, person.appearanceCount, cell.right - CollageSpec.CAPTION_INSET, baseline)

        canvas.restore()
    }

    private fun drawCountPill(canvas: Canvas, count: Int, right: Float, baseline: Float) {
        val text = "×$count"
        val textWidth = pillTextPaint.measureText(text)
        val pill = RectF(right - textWidth - 2 * CollageSpec.PILL_PADDING, baseline - CollageSpec.PILL_HEIGHT + 12f, right, baseline + 12f)
        canvas.drawRoundRect(pill, CollageSpec.PILL_HEIGHT / 2f, CollageSpec.PILL_HEIGHT / 2f, pillPaint)
        val textBaseline = pill.centerY() - (pillTextPaint.descent() + pillTextPaint.ascent()) / 2f
        canvas.drawText(text, pill.centerX(), textBaseline, pillTextPaint)
    }

    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 60f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB8BCC8.toInt(); textSize = 32f; typeface = Typeface.SANS_SERIF
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 34f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF15181F.toInt(); textSize = 28f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
}
