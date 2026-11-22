package cn.edu.pku.openrunner.feature.hike.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Path
import cn.edu.pku.openrunner.feature.hike.domain.TerrainId
import java.io.File
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ContourLabel(val x: Float, val y: Float, val meters: Int)
data class TerrainDrawing(
    val id: TerrainId,
    val shade: Bitmap,
    val minor: Path,
    val major: Path,
    val labels: List<ContourLabel>
)

object Terrarium {
    private const val SIDE = 256
    private const val STEP = 4
    private const val INTERVAL = 20
    private const val MAJOR_INTERVAL = 100

    fun read(file: File): FloatArray {
        val bitmap = BitmapFactory.decodeFile(file.path) ?: throw IOException("Unreadable terrain tile")
        try {
            if (bitmap.width != SIDE || bitmap.height != SIDE) throw IOException("Invalid terrain dimensions")
            val rgb = IntArray(SIDE * SIDE)
            bitmap.getPixels(rgb, 0, SIDE, 0, 0, SIDE, SIDE)
            return FloatArray(rgb.size) { index ->
                val color = rgb[index]
                ((color shr 16 and 255) * 256 + (color shr 8 and 255) - 32768) + (color and 255) / 256f
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun draw(id: TerrainId, elevations: Map<TerrainId, FloatArray>): TerrainDrawing {
        val center = elevations.getValue(id)
        val east = elevations[id.copy(x = id.x + 1)]
        val south = elevations[id.copy(y = id.y + 1)]
        val southeast = elevations[id.copy(x = id.x + 1, y = id.y + 1)]
        fun elevation(x: Int, y: Int): Float = when {
            x == SIDE && y == SIDE -> southeast?.get(0) ?: center.last()
            x == SIDE -> east?.get(y * SIDE) ?: center[y * SIDE + SIDE - 1]
            y == SIDE -> south?.get(x) ?: center[(SIDE - 1) * SIDE + x]
            else -> center[y * SIDE + x]
        }

        val minor = Path()
        val major = Path()
        val labels = mutableListOf<ContourLabel>()
        val labeled = mutableSetOf<Pair<Int, Int>>()
        val xCrossings = FloatArray(4)
        val yCrossings = FloatArray(4)
        val shadePixels = IntArray(SIDE / STEP * SIDE / STEP)
        for (y in 0 until SIDE step STEP) {
            for (x in 0 until SIDE step STEP) {
                val a = elevation(x, y)
                val b = elevation(x + STEP, y)
                val c = elevation(x + STEP, y + STEP)
                val d = elevation(x, y + STEP)
                val shadow = ((c - a) * 0.38f).roundToInt().coerceIn(-19, 23)
                shadePixels[y / STEP * (SIDE / STEP) + x / STEP] =
                    if (shadow < 0) (-shadow shl 24) or 0xFFFFFF else (shadow shl 24) or 0x383023

                val low = min(min(a, b), min(c, d))
                val high = max(max(a, b), max(c, d))
                var level = ceil(low / INTERVAL).toInt() * INTERVAL
                while (level < high) {
                    var count = 0
                    fun cross(ax: Int, ay: Int, az: Float, bx: Int, by: Int, bz: Float) {
                        if ((az > level) == (bz > level)) return
                        val fraction = (level - az) / (bz - az)
                        xCrossings[count] = ax + fraction * (bx - ax)
                        yCrossings[count] = ay + fraction * (by - ay)
                        count++
                    }
                    cross(x, y, a, x + STEP, y, b)
                    cross(x + STEP, y, b, x + STEP, y + STEP, c)
                    cross(x + STEP, y + STEP, c, x, y + STEP, d)
                    cross(x, y + STEP, d, x, y, a)
                    val isMajor = level % MAJOR_INTERVAL == 0
                    val path = if (isMajor) major else minor
                    fun line(first: Int, second: Int) {
                        path.moveTo(xCrossings[first], yCrossings[first])
                        path.lineTo(xCrossings[second], yCrossings[second])
                    }
                    if (count == 2) line(0, 1)
                    if (count == 4) {
                        val centerAbove = (a + b + c + d) / 4 > level
                        if (centerAbove == (a > level)) {
                            line(0, 1)
                            line(2, 3)
                        } else {
                            line(0, 3)
                            line(1, 2)
                        }
                    }
                    if (isMajor && count >= 2 && x in 20..236 && y in 20..236 &&
                        labeled.add(x / 70 to y / 70)) {
                        labels.add(ContourLabel(xCrossings[0], yCrossings[0], level))
                    }
                    level += INTERVAL
                }
            }
        }
        val shade = Bitmap.createBitmap(shadePixels, SIDE / STEP, SIDE / STEP, Bitmap.Config.ARGB_8888)
        return TerrainDrawing(id, shade, minor, major, labels)
    }
}
