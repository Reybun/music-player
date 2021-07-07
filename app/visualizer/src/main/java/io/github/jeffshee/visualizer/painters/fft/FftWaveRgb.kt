package io.github.jeffshee.visualizer.painters.fft

import android.graphics.*
import io.github.jeffshee.visualizer.painters.Painter
import io.github.jeffshee.visualizer.utils.VisualizerHelper

class FftWaveRgb(
    flags: Int = Paint.ANTI_ALIAS_FLAG,
    var color: List<Int> = listOf(Color.RED, Color.GREEN, Color.BLUE),
    //
    startHz: Int = 0,
    endHz: Int = 2000,
    //
    num: Int = 128,
    interpolator: String = "sp",
    //
    var side: String = "a",
    mirror: Boolean = false,
    power: Boolean = false,
    //
    ampR: Float = 1f
) : Painter() {

    private val wave = FftWave(Paint(flags).apply {
        style = Paint.Style.FILL;xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }, startHz, endHz, num, interpolator, side, mirror, power, ampR)

    override fun calc(helper: VisualizerHelper) {
        wave.calc(helper)
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        canvas.save()
        canvas.scale(1.2f, 1f, canvas.width / 2f, canvas.height.toFloat())
        drawHelper(canvas, side, -.03f, 0f) {
            wave.paint.color = color[0]
            wave.draw(canvas, helper)
        }
        wave.paint.color = color[1]
        wave.draw(canvas, helper)
        drawHelper(canvas, side, .03f, 0f) {
            wave.paint.color = color[2]
            wave.draw(canvas, helper)
        }
        canvas.restore()
    }
}