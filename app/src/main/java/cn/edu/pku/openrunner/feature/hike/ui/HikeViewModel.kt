package cn.edu.pku.openrunner.feature.hike.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.hike.data.*
import cn.edu.pku.openrunner.feature.hike.domain.HikeRoute
import cn.edu.pku.openrunner.feature.hike.domain.MapArea
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HikePage(
    val route: HikeRoute? = null,
    val maps: List<HikeMapPackage> = emptyList(),
    val map: HikeMapData? = null,
    val hike: HikeLog? = null,
    val progress: Pair<Int, Int>? = null,
    val message: Int? = null,
    val importing: Boolean = false,
    val fit: MapArea? = null
)

class HikeViewModel(private val app: Application) : AndroidViewModel(app) {
    private val maps = OfflineHikeMaps(app)
    private val references = ReferenceRouteStore(app)
    private val state = MutableStateFlow(HikePage())
    val page = state.asStateFlow()
    var camera: HikeCamera? = null
    private var currentMapKey: String? = null
    private var requestedKey: String? = null
    private val unreadableMaps = mutableSetOf<String>()
    private var mapLoad: Job? = null
    private var hikeLoad: Job? = null
    private var cache: Job? = null

    init {
        viewModelScope.launch {
            val savedMaps = withContext(Dispatchers.IO) { maps.packages() }
            val route = withContext(Dispatchers.IO) { runCatching { references.read() }.getOrNull() }
            state.update { it.copy(maps = savedMaps, route = it.route ?: route) }
        }
    }

    fun importRoute(uri: Uri) {
        if (state.value.importing) return
        state.update { it.copy(importing = true, message = null) }
        viewModelScope.launch {
            try {
                val route = withContext(Dispatchers.IO) {
                    val resolver = app.contentResolver
                    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else null
                    } ?: app.getString(R.string.hike_kml_reference)
                    val result = resolver.openInputStream(uri)?.use { KmlReader.read(it, name) }
                        ?: throw IOException("Could not open KML")
                    references.save(result)
                    result
                }
                state.update { it.copy(route = route, fit = MapArea.around(route.points(), 0)) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.update { it.copy(message = R.string.hike_import_error) }
            } finally {
                state.update { it.copy(importing = false) }
            }
        }
    }

    fun clearRoute() {
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) { runCatching { references.clear() }.isSuccess }
            state.update { if (removed) it.copy(route = null) else it.copy(message = R.string.hike_import_error) }
        }
    }

    fun chooseMap(camera: HikeCamera) {
        val available = state.value.maps
        val current = state.value.map?.info
        if (current?.area?.contains(camera.x, camera.y) == true) return
        available.firstOrNull { it.key !in unreadableMaps && it.area.contains(camera.x, camera.y) }
            ?.let { loadMap(it, fit = false) }
    }

    fun loadMap(info: HikeMapPackage, fit: Boolean) {
        if (info.key == currentMapKey && state.value.map != null) {
            if (fit) state.update { it.copy(fit = info.area) }
            return
        }
        if (info.key == requestedKey && !fit) return
        mapLoad?.cancel()
        requestedKey = info.key
        mapLoad = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { maps.load(info) }
                unreadableMaps.remove(info.key)
                currentMapKey = info.key
                state.update { it.copy(map = data, fit = if (fit) info.area else it.fit) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                unreadableMaps.add(info.key)
                state.update { it.copy(message = R.string.hike_map_read_error) }
            } finally {
                if (requestedKey == info.key) requestedKey = null
            }
        }
    }

    fun downloadMap(area: MapArea, label: String) {
        if (cache?.isActive == true) return
        state.update { it.copy(progress = 0 to 1, message = null) }
        cache = viewModelScope.launch {
            try {
                val info = maps.download(area, label) { complete, total ->
                    state.update { it.copy(progress = complete to total) }
                }
                state.update { it.copy(maps = listOf(info) + it.maps, message = R.string.hike_map_saved) }
                loadMap(info, fit = true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: HikeMapAreaTooLarge) {
                state.update { it.copy(message = R.string.hike_area_too_large) }
            } catch (_: Exception) {
                state.update { it.copy(message = R.string.hike_download_error) }
            } finally {
                state.update { it.copy(progress = null) }
            }
        }
    }

    fun cancelDownload() {
        cache?.cancel()
    }

    suspend fun hikes(): List<SavedHike> = withContext(Dispatchers.IO) { HikeStore(app).use { it.hikes() } }

    fun loadHike(hike: SavedHike? = null, fit: Boolean = false) {
        hikeLoad?.cancel()
        hikeLoad = viewModelScope.launch {
            try {
                val log = withContext(Dispatchers.IO) {
                    HikeStore(app).use { store ->
                        (hike ?: store.hikes().firstOrNull())?.let(store::read)
                    }
                }
                val area = log?.let { MapArea.around(it.samples.asSequence().map { sample -> sample.point }, 0) }
                state.update { it.copy(hike = log, fit = if (fit) area else it.fit) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.update { it.copy(message = R.string.hike_record_error) }
            }
        }
    }

    fun consumeMessage(message: Int) {
        state.update { if (it.message == message) it.copy(message = null) else it }
    }

    fun consumeFit(fit: MapArea) {
        state.update { if (it.fit === fit) it.copy(fit = null) else it }
    }
}
