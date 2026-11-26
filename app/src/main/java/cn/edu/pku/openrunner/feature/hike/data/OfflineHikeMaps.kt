package cn.edu.pku.openrunner.feature.hike.data

import android.content.Context
import cn.edu.pku.openrunner.feature.hike.domain.GeoPoint
import cn.edu.pku.openrunner.feature.hike.domain.MapArea
import cn.edu.pku.openrunner.feature.hike.domain.MapFeature
import cn.edu.pku.openrunner.feature.hike.domain.MapFeatureType
import cn.edu.pku.openrunner.feature.hike.domain.TerrainId
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

data class HikeMapPackage(val key: String, val label: String, val area: MapArea, val terrain: List<TerrainId>)
data class HikeMapData(val info: HikeMapPackage, val features: List<MapFeature>, val terrain: List<TerrainDrawing>)
class HikeMapAreaTooLarge : IOException()

class OfflineHikeMaps(context: Context) {
    private val root = File(context.filesDir, "hike-maps")
    private val gson = Gson()

    fun packages(): List<HikeMapPackage> = root.listFiles().orEmpty()
        .filter { it.isDirectory && !it.name.startsWith(".") }
        .sortedByDescending { it.lastModified() }
        .mapNotNull { directory ->
            runCatching {
                File(directory, "area.json").reader(Charsets.UTF_8).use {
                    gson.fromJson(it, HikeMapPackage::class.java)
                }
            }.getOrNull()
        }

    fun load(info: HikeMapPackage): HikeMapData {
        val directory = File(root, info.key)
        val features = File(directory, "features.json").reader(Charsets.UTF_8).use {
            gson.fromJson(it, Array<MapFeature>::class.java).toList()
        }
        val elevations = info.terrain.associateWith { tile -> Terrarium.read(File(directory, tile.filename)) }
        return HikeMapData(info, features, info.terrain.map { Terrarium.draw(it, elevations) })
    }

    fun delete(info: HikeMapPackage) {
        val key = UUID.fromString(info.key).toString()
        val directory = File(root, key)
        if (!directory.exists()) return
        val discarded = File(root, ".delete-${UUID.randomUUID()}")
        if (!directory.renameTo(discarded)) throw IOException("Unable to delete offline map")
        discarded.deleteRecursively()
    }

    suspend fun download(area: MapArea, label: String, progress: (Int, Int) -> Unit): HikeMapPackage =
        withContext(Dispatchers.IO) {
            val tiles = (14 downTo 12).firstNotNullOfOrNull { zoom ->
                area.tiles(zoom).takeIf { it.isNotEmpty() }
            } ?: throw HikeMapAreaTooLarge()
            val key = UUID.randomUUID().toString()
            val packageInfo = HikeMapPackage(key, label, area, tiles)
            val staging = File(root, ".$key")
            if (!staging.mkdirs()) throw IOException("Unable to open map storage")
            try {
                val total = tiles.size + 1
                progress(0, total)
                val features = area.normalizedBoxes().flatMap { loadFeatures(it) }
                File(staging, "features.json").writer(Charsets.UTF_8).use { gson.toJson(features, it) }
                progress(1, total)
                for ((i, tile) in tiles.withIndex()) {
                    ensureActive()
                    val url = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/${tile.zoom}/${tile.wrappedX}/${tile.y}.png"
                    val request = Request.Builder().url(url).build()
                    val bytes = requestBytes(request, 1_500_000)
                    val file = File(staging, tile.filename)
                    file.writeBytes(bytes)
                    Terrarium.read(file)
                    progress(i + 2, total)
                }
                // Prepare all expected content before publishing the package to offline readers.
                ensureActive()
                File(staging, "area.json").writer(Charsets.UTF_8).use { gson.toJson(packageInfo, it) }
                if (!staging.renameTo(File(root, key))) throw IOException("Unable to save map package")
                packageInfo
            } finally {
                if (staging.exists()) staging.deleteRecursively()
            }
        }

    private suspend fun loadFeatures(area: MapArea): List<MapFeature> {
        val bbox = "${area.south},${area.west},${area.north},${area.east}"
        val query = """
            [out:json][timeout:45];
            (
              way["highway"~"^(motorway|trunk|primary|secondary|tertiary|unclassified|residential|living_street|service|track|path|footway|bridleway|steps|pedestrian|cycleway|motorway_link|trunk_link|primary_link|secondary_link|tertiary_link)$"]($bbox);
              way["waterway"~"^(river|stream|canal|ditch|drain)$"]($bbox);
              way["natural"~"^(water|coastline)$"]($bbox);
              relation["type"="multipolygon"]["natural"="water"]($bbox);
              node["natural"~"^(peak|volcano|saddle)$"]($bbox);
            );
            out geom($bbox);
        """.trimIndent().replace('\n', ' ')
        val hosts = if (area.west >= 73 && area.east <= 135 && area.south >= 18 && area.north <= 54) {
            listOf(OVERPASS.first(), ASIA_OVERPASS) + OVERPASS.drop(1)
        } else OVERPASS
        var failure: IOException? = null
        for (host in hosts) {
            val url = requireNotNull(HttpUrl.parse(host)).newBuilder().addQueryParameter("data", query).build()
            val response = try {
                val bytes = requestBytes(Request.Builder().url(url).build(), MAX_OSM_BYTES)
                gson.fromJson(bytes.toString(Charsets.UTF_8), JsonObject::class.java)
            } catch (error: IOException) {
                failure = error
                continue
            } catch (error: JsonParseException) {
                failure = IOException("Invalid map response", error)
                continue
            }
            val elements = response?.get("elements")?.takeIf { it.isJsonArray }?.asJsonArray
            if (elements == null || response.has("remark")) {
                failure = IOException("Incomplete map data")
                continue
            }
            return buildList {
                for (entry in elements) {
                    val element = entry.asJsonObject
                    val tags = element.getAsJsonObject("tags") ?: continue
                    val kind = featureType(tags) ?: continue
                    val name = (tags.string("name:zh") ?: tags.string("name"))?.take(80)
                    when (element.string("type")) {
                        "node" -> coordinate(element)?.let {
                            add(MapFeature(kind, listOf(it), name, tags.string("ele")?.take(16)))
                        }
                        "way" -> addAll(geometry(element, kind, name))
                        "relation" -> for (member in element.getAsJsonArray("members") ?: emptyList()) {
                            if (member.asJsonObject.string("type") == "way") {
                                addAll(geometry(member.asJsonObject, MapFeatureType.WATERWAY, null))
                            }
                        }
                    }
                }
            }
        }
        throw failure ?: IOException("Map download failed")
    }

    private fun featureType(tags: JsonObject): MapFeatureType? {
        return when {
            tags.string("natural") in setOf("peak", "volcano", "saddle") -> MapFeatureType.PEAK
            tags.string("natural") == "water" -> MapFeatureType.WATER
            tags.has("waterway") || tags.string("natural") == "coastline" -> MapFeatureType.WATERWAY
            tags.string("highway") in TRAILS -> MapFeatureType.PATH
            tags.string("highway") in LOCAL_ROADS -> MapFeatureType.LOCAL_ROAD
            tags.has("highway") -> MapFeatureType.ROAD
            else -> null
        }
    }

    private fun geometry(element: JsonObject, type: MapFeatureType, name: String?): List<MapFeature> {
        val geometry = element.getAsJsonArray("geometry") ?: return emptyList()
        val lines = mutableListOf<List<GeoPoint>>()
        var points = mutableListOf<GeoPoint>()
        for (entry in geometry) {
            val position = if (entry.isJsonObject) coordinate(entry.asJsonObject) else null
            if (position != null) {
                points.add(position)
            } else if (points.isNotEmpty()) {
                lines.add(points)
                points = mutableListOf()
            }
        }
        if (points.isNotEmpty()) lines.add(points)
        return lines.filter { it.size >= 2 }.map { MapFeature(type, it, name, null) }
    }

    private fun coordinate(json: JsonObject): GeoPoint? {
        if (!json.has("lat") || !json.has("lon")) return null
        return GeoPoint(json.get("lat").asDouble, json.get("lon").asDouble).takeIf { it.valid }
    }

    private fun JsonObject.string(name: String): String? = get(name)?.takeUnless { it.isJsonNull }?.asString

    private suspend fun requestBytes(request: Request, maxBytes: Int): ByteArray =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request.newBuilder().header("User-Agent", AGENT).build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val bytes = response.use {
                            if (!it.isSuccessful) throw IOException("Map provider HTTP ${it.code()}")
                            val body = it.body() ?: throw IOException("Empty map response")
                            val source = body.source()
                            if (source.request(maxBytes + 1L)) throw IOException("Map response is too large")
                            source.readByteArray()
                        }
                        continuation.resume(bytes)
                    } catch (error: IOException) {
                        continuation.resumeWithException(error)
                    }
                }
            })
        }

    companion object {
        private const val MAX_OSM_BYTES = 20_000_000
        private const val AGENT = "OpenRunner-Hike (https://github.com/fine-structure-constant/OpenRunner)"
        private val TRAILS = setOf("track", "path", "footway", "bridleway", "steps", "cycleway")
        private val LOCAL_ROADS = setOf("residential", "service", "unclassified", "living_street", "pedestrian")
        private const val ASIA_OVERPASS = "https://ovp.dresel.systems/api-as/interpreter"
        private val OVERPASS = listOf(
            "https://lz4.overpass-api.de/api/interpreter",
            "https://z.overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
        )
        private val client = OkHttpClient.Builder().callTimeout(65, TimeUnit.SECONDS)
            .readTimeout(55, TimeUnit.SECONDS).build()
    }
}
