package cn.edu.pku.openrunner.core.orientation

import android.hardware.GeomagneticField
import kotlin.math.roundToInt

internal fun wrapHeading(heading: Float): Float = (heading % 360f + 360f) % 360f

internal fun trueNorthHeading(magneticHeading: Float, declination: Float): Float {
    // Android reports east declination as positive, so add it to the magnetic bearing.
    return wrapHeading(magneticHeading + declination)
}

internal fun mapHeading(magneticHeading: Float?, declination: Float?): Int? {
    if (magneticHeading == null || declination == null) return null
    return trueNorthHeading(magneticHeading, declination).roundToInt() % 360
}

internal fun declinationAt(latitude: Double, longitude: Double, altitude: Double = 0.0,
                           time: Long = System.currentTimeMillis()): Float? =
    GeomagneticField(latitude.toFloat(), longitude.toFloat(), altitude.toFloat(), time)
        .declination.takeIf(Float::isFinite)
