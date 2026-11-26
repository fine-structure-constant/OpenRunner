package cn.edu.pku.openrunner.feature.hike.ui

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class HikePage(
    val basemap: HikeBasemap = HikeBasemap.AMAP,
    val routes: HikeRouteIndex = HikeRouteIndex(),
    val route: HikeRoute? = null,
    val maps: List<HikeMapPackage> = emptyList(),
    val map: HikeMapData? = null,
    val hike: HikeLog? = null,
    val progress: Pair<Int, Int>? = null,
    val message: Int? = null,
    val importing: Boolean = false,
    val fit: MapArea? = null
)

enum class HikeBasemap { AMAP, CONTOURS }

class HikeViewModel(private val app: Application) : AndroidViewModel(app) {
    private val maps = OfflineHikeMaps(app)
    private val references = ReferenceRouteStore(app.filesDir)
    private val referenceLock = Mutex()
    private val preferences = app.getSharedPreferences("hike-map-settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(HikePage(basemap =
        if (preferences.getBoolean("contours", false)) HikeBasemap.CONTOURS else HikeBasemap.AMAP))
    val page = state.asStateFlow()
    var camera: HikeCamera? = null
    private var requestedKey: String? = null
    private var fitMapOnLoad = false
    private val unreadableMaps = mutableSetOf<String>()
    private val deletingMaps = mutableSetOf<String>()
    private var mapLoad: Job? = null
    private var hikeLoad: Job? = null
    private var cache: Job? = null

    init {
        viewModelScope.launch {
            val savedMaps = withContext(Dispatchers.IO) { maps.packages() }
            state.update { it.copy(maps = (it.maps + savedMaps).distinctBy(HikeMapPackage::key)) }
            referenceLock.withLock {
                try {
                    val (index, route) = withContext(Dispatchers.IO) { references.snapshot() }
                    state.update { it.copy(routes = index, route = route) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    state.update { it.copy(message = R.string.hike_import_error) }
                }
            }
        }
    }

    fun setBasemap(basemap: HikeBasemap) {
        state.update { it.copy(basemap = basemap) }
        preferences.edit().putBoolean("contours", basemap == HikeBasemap.CONTOURS).apply()
    }

    fun importRoute(uri: Uri) {
        viewModelScope.launch {
            referenceLock.withLock {
                state.update { it.copy(importing = true, message = null) }
                try {
                    val (index, route) = withContext(Dispatchers.IO) {
                        val resolver = app.contentResolver
                        val name = runCatching {
                            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                                if (it.moveToFirst()) it.getString(0) else null
                            }
                        }.getOrNull() ?: app.getString(R.string.hike_kml_reference)
                        val result = resolver.openInputStream(uri)?.use { KmlReader.read(it, name) }
                            ?: throw IOException("Could not open KML")
                        references.add(result, name) to result
                    }
                    state.update { it.copy(routes = index, route = route, fit = MapArea.around(route.points(), 0)) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    state.update { it.copy(message = R.string.hike_import_error) }
                } finally {
                    state.update { it.copy(importing = false) }
                }
            }
        }
    }

    fun selectRoute(id: String?) {
        viewModelScope.launch {
            referenceLock.withLock {
                try {
                    val (index, route) = withContext(Dispatchers.IO) { references.select(id) }
                    state.update { it.copy(routes = index, route = route, fit = route?.let { MapArea.around(it.points(), 0) }) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    state.update { it.copy(message = R.string.hike_import_error) }
                }
            }
        }
    }

    fun deleteRoute(id: String) {
        viewModelScope.launch {
            referenceLock.withLock {
                try {
                    val index = withContext(Dispatchers.IO) { references.delete(id) }
                    state.update { page -> page.copy(
                        routes = index,
                        route = page.route.takeIf { page.routes.activeId == index.activeId }
                    ) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    state.update { it.copy(message = R.string.hike_delete_error) }
                }
            }
        }
    }

    fun chooseMap(camera: HikeCamera) {
        val available = state.value.maps
        val current = state.value.map?.info
        if (requestedKey != null && fitMapOnLoad) return
        if (current?.area?.contains(camera.x, camera.y) == true) return
        available.firstOrNull { it.key !in unreadableMaps && it.key !in deletingMaps && it.area.contains(camera.x, camera.y) }
            ?.let { loadMap(it, fit = false) }
    }

    fun openOfflineMap(info: HikeMapPackage) {
        setBasemap(HikeBasemap.CONTOURS)
        loadMap(info, fit = true)
    }

    fun loadMap(info: HikeMapPackage, fit: Boolean) {
        if (info.key in deletingMaps || state.value.maps.none { it.key == info.key }) return
        if (info.key == state.value.map?.info?.key) {
            if (fit) state.update { it.copy(fit = info.area) }
            return
        }
        if (info.key == requestedKey) {
            fitMapOnLoad = fitMapOnLoad || fit
            return
        }
        mapLoad?.cancel()
        requestedKey = info.key
        fitMapOnLoad = fit
        mapLoad = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { maps.load(info) }
                unreadableMaps.remove(info.key)
                state.update { it.copy(map = data, fit = if (fitMapOnLoad) info.area else it.fit) }
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

    fun deleteMap(info: HikeMapPackage) {
        if (!deletingMaps.add(info.key)) return
        if (requestedKey == info.key) mapLoad?.cancel()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { maps.delete(info) }
                state.update { page -> page.copy(
                    maps = page.maps.filterNot { it.key == info.key },
                    map = page.map?.takeUnless { it.info.key == info.key },
                    fit = page.fit.takeUnless { it === info.area }
                ) }
                unreadableMaps.remove(info.key)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.update { it.copy(message = R.string.hike_delete_error) }
            } finally {
                deletingMaps.remove(info.key)
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
                if (state.value.basemap == HikeBasemap.CONTOURS) loadMap(info, fit = true)
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
