package cn.edu.pku.openrunner.feature.run.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Converts GPS WGS-84 coordinates to the GCJ-02 coordinates expected by AMap. */
object Wgs84Gcj02 {
    fun convert(longitude: Double, latitude: Double): Pair<Double, Double> {
        if (outOfChina(longitude, latitude)) return longitude to latitude
        val dLat = transformLatitude(longitude - 105.0, latitude - 35.0)
        val dLon = transformLongitude(longitude - 105.0, latitude - 35.0)
        val radLat = latitude / 180.0 * Math.PI
        val magic = 1 - 0.006693421622965943 * sin(radLat).pow(2)
        val sqrtMagic = sqrt(magic)
        val mgLat = latitude + (dLat * 180.0) /
            ((6335552.717000426 / (magic * sqrtMagic)) * Math.PI)
        val mgLon = longitude + (dLon * 180.0) /
            ((6378245.0 / sqrtMagic * cos(radLat)) * Math.PI)
        return mgLon to mgLat
    }

    private fun outOfChina(longitude: Double, latitude: Double): Boolean =
        longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271

    private fun transformLatitude(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y +
            0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * Math.PI) + 20.0 * sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * Math.PI) + 40.0 * sin(y / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * Math.PI) + 320 * sin(y * Math.PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLongitude(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x +
            0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * Math.PI) + 20.0 * sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * Math.PI) + 40.0 * sin(x / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * Math.PI) + 300.0 * sin(x / 30.0 * Math.PI)) * 2.0 / 3.0
        return ret
    }
}
