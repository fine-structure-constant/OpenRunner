package cn.edu.pku.openrunner.feature.hike.domain

internal fun trueNorthHeading(magneticHeading: Float, declination: Float): Float {
    // Android reports east declination as positive, so add it to the magnetic bearing.
    return (magneticHeading + declination + 360f) % 360f
}
