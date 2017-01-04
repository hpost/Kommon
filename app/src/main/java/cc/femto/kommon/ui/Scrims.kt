package cc.femto.kommon.ui

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.Gravity

/**
 * Utility for creating pretty gradient scrims.
 */
object Scrims {

    /** 40% opacity black to transparent */
    val TOOLBAR_SCRIM = makeCubicGradientScrimDrawable(
            Color.argb(102, 0, 0, 0), 9, Gravity.TOP)

    /** transparent to 50% opacity black */
    val PHOTO_SCRIM = makeCubicGradientScrimDrawable(
            Color.argb(128, 0, 0, 0), 9, Gravity.BOTTOM)

    /** 80% opacity white to transparent */
    val PHOTO_SCRIM_INVERSE = makeCubicGradientScrimDrawable(
            Color.argb(204, 255, 255, 255), 9, Gravity.TOP)

    /**
     * Creates an approximated cubic gradient using a multi-stop linear gradient.
     * See [this post](https://plus.google.com/+RomanNurik/posts/2QvHVFWrHZf) for more details.
     */
    fun makeCubicGradientScrimDrawable(baseColor: Int, numStops: Int,
                                       gravity: Int): Drawable {
        var numStops = numStops
        numStops = Math.max(numStops, 2)

        val paintDrawable = PaintDrawable()
        paintDrawable.shape = RectShape()

        val stopColors = IntArray(numStops)

        val red = Color.red(baseColor)
        val green = Color.green(baseColor)
        val blue = Color.blue(baseColor)
        val alpha = Color.alpha(baseColor)

        for (i in 0 until numStops) {
            val x = i * 1f / (numStops - 1)
            val opacity = Math.pow(x.toDouble(), 3.0)
            stopColors[i] = Color.argb((alpha * opacity).toInt(), red, green, blue)
        }

        val x0: Float
        val x1: Float
        val y0: Float
        val y1: Float
        when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.LEFT -> {
                x0 = 1f
                x1 = 0f
            }
            Gravity.RIGHT -> {
                x0 = 0f
                x1 = 1f
            }
            else -> {
                x0 = 0f
                x1 = 0f
            }
        }
        when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.TOP -> {
                y0 = 1f
                y1 = 0f
            }
            Gravity.BOTTOM -> {
                y0 = 0f
                y1 = 1f
            }
            else -> {
                y0 = 0f
                y1 = 0f
            }
        }

        paintDrawable.shaderFactory = object : ShapeDrawable.ShaderFactory() {
            override fun resize(width: Int, height: Int): Shader {
                val linearGradient = LinearGradient(
                        width * x0,
                        height * y0,
                        width * x1,
                        height * y1,
                        stopColors, null,
                        Shader.TileMode.CLAMP)
                return linearGradient
            }
        }

        return paintDrawable
    }
}
