package cc.femto.kommon.util

import android.graphics.Color

/**
 * This is a color animator that has a correct interpolation in HSV 3D space.
 * The algorithm makes a standard 3D math interpolation between
 * two 3D points and thus it allows to make a visually perfect color shift.
 * The usual direct interpolation of HSV values makes stranger behavior,
 * in example you can see red color while you're interpolating from
 * blue to white.
 *
 * https://github.com/konmik/animated-color
 */
class AnimatedColor(private val start: Int, private val end: Int) {
    private val vector0: FloatArray
    private val vector1: FloatArray

    init {
        this.vector0 = toVector(toHSV(start))
        this.vector1 = toVector(toHSV(end))
    }

    fun with(delta: Float): Int {
        if (delta <= 0)
            return start
        if (delta >= 1)
            return end
        return Color.HSVToColor(toHSV(move(vector0, vector1, delta)))
    }

    companion object {

        private val ERROR = 0.001f

        fun move(vector0: FloatArray, vector1: FloatArray, delta: Float): FloatArray {
            val vector = FloatArray(3)
            vector[0] = (vector1[0] - vector0[0]) * delta + vector0[0]
            vector[1] = (vector1[1] - vector0[1]) * delta + vector0[1]
            vector[2] = (vector1[2] - vector0[2]) * delta + vector0[2]
            return vector
        }

        fun toHSV(color: Int): FloatArray {
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            return hsv
        }

        fun toVector(hsv: FloatArray): FloatArray {
            val vector = FloatArray(3)
            val rad = Math.PI * hsv[0] / 180
            vector[0] = Math.cos(rad).toFloat() * hsv[1]
            vector[1] = Math.sin(rad).toFloat() * hsv[1]
            vector[2] = hsv[2]
            return vector
        }

        fun toHSV(vector: FloatArray): FloatArray {
            val hsv = FloatArray(3)
            hsv[1] = Math.sqrt((vector[0] * vector[0] + vector[1] * vector[1]).toDouble()).toFloat()
            hsv[0] = if (hsv[1] < ERROR)
                0f
            else
                (Math.atan2((vector[1] / hsv[1]).toDouble(), (vector[0] / hsv[1]).toDouble()) * 180 / Math.PI).toFloat()
            if (hsv[0] < 0)
                hsv[0] += 360f
            hsv[2] = vector[2]
            return hsv
        }
    }
}