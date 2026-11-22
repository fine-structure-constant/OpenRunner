package cn.edu.pku.openrunner.feature.hike.domain

import kotlin.math.*

private const val MAX_MERCATOR_LATITUDE = 85.05112878

fun worldX(longitude: Double): Double = (longitude + 180.0) / 360.0

fun worldY(latitude: Double): Double {
    val radians = Math.toRadians(latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE))
    return (1 - asinh(tan(radians)) / PI) / 2
}

fun latitudeAt(y: Double): Double = Math.toDegrees(atan(sinh(PI * (1 - 2 * y))))
fun longitudeAt(x: Double): Double = (x - floor(x)) * 360 - 180
fun nearestWorldX(x: Double, reference: Double): Double = x + round(reference - x)

data class MapArea(val south: Double, val west: Double, val north: Double, val east: Double) {
    val x: Double get() = worldX((west + east) / 2)
    val y: Double get() = (worldY(north) + worldY(south)) / 2

    fun contains(x: Double, y: Double): Boolean =
        nearestWorldX(x, this.x) in worldX(west)..worldX(east) && y in worldY(north)..worldY(south)

    fun tiles(zoom: Int): List<TerrainId> {
        val scale = 1 shl zoom
        val startX = floor(worldX(west) * scale).toInt()
        val endX = floor(worldX(east) * scale).toInt()
        val startY = floor(worldY(north) * scale).toInt().coerceAtLeast(0)
        val endY = floor(worldY(south) * scale).toInt().coerceAtMost(scale - 1)
        if ((endX - startX + 1L) * (endY - startY + 1L) > MAX_TERRAIN_TILES) return emptyList()
        return buildList {
            for (x in startX..endX) for (y in startY..endY) add(TerrainId(zoom, x, y))
        }
    }

    /** Overpass requires longitudes inside [-180, 180] in each bounding box. */
    fun normalizedBoxes(): List<MapArea> {
        val normalizedWest = longitudeAt(worldX(west))
        val normalizedEast = normalizedWest + east - west
        val first = copy(west = normalizedWest, east = min(180.0, normalizedEast))
        return if (normalizedEast <= 180.0) listOf(first)
        else listOf(first, copy(west = -180.0, east = normalizedEast - 360.0))
    }

    companion object {
        private const val MAX_TERRAIN_TILES = 64

        fun around(points: Sequence<GeoPoint>, kilometers: Int): MapArea? {
            val iterator = points.iterator()
            if (!iterator.hasNext()) return null
            val first = iterator.next()
            val reference = worldX(first.longitude)
            var west = reference
            var east = reference
            var south = first.latitude
            var north = first.latitude
            while (iterator.hasNext()) {
                val point = iterator.next()
                val x = nearestWorldX(worldX(point.longitude), reference)
                west = min(west, x)
                east = max(east, x)
                south = min(south, point.latitude)
                north = max(north, point.latitude)
            }
            val padding = max(kilometers / 111.0, 0.0002)
            val southBound = (south - padding).coerceAtLeast(-MAX_MERCATOR_LATITUDE)
            val northBound = (north + padding).coerceAtMost(MAX_MERCATOR_LATITUDE)
            if (southBound >= northBound) return null
            val longitudePadding = padding / cos(Math.toRadians(max(abs(southBound), abs(northBound))))
            val left = west * 360 - 180 - longitudePadding
            val right = east * 360 - 180 + longitudePadding
            if (right - left > 180) return null
            return MapArea(southBound, left, northBound, right)
        }
    }
}

data class TerrainId(val zoom: Int, val x: Int, val y: Int) {
    val scale: Int get() = 1 shl zoom
    val wrappedX: Int get() = (x % scale + scale) % scale
    val filename: String get() = "${zoom}_${x}_${y}.png"
}

enum class MapFeatureType { ROAD, LOCAL_ROAD, PATH, WATERWAY, WATER, PEAK }
data class MapFeature(val type: MapFeatureType, val points: List<GeoPoint>, val name: String?, val elevation: String?)
