package cn.edu.pku.openrunner.feature.hike.data

import androidx.core.util.AtomicFile
import cn.edu.pku.openrunner.feature.hike.domain.HikeRoute
import com.google.gson.Gson
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

data class SavedHikeRoute(val id: String, val title: String, val fileName: String, val importedAt: Long)
data class HikeRouteIndex(val entries: List<SavedHikeRoute> = emptyList(), val activeId: String? = null)

class ReferenceRouteStore(filesDir: File) {
    private val gson = Gson()
    private val root = File(filesDir, "hike-routes")
    private val index = File(root, "index.json")
    private val legacy = AtomicFile(File(filesDir, "hike-reference.json"))

    fun snapshot(): Pair<HikeRouteIndex, HikeRoute?> = synchronized(lock) {
        val current = readIndex()
        val route = current.activeId?.let { runCatching { readRoute(it) }.getOrNull() }
        current to route
    }

    fun add(route: HikeRoute, fileName: String): HikeRouteIndex = synchronized(lock) {
        add(readIndex(), route, fileName, System.currentTimeMillis())
    }

    fun select(id: String?): Pair<HikeRouteIndex, HikeRoute?> = synchronized(lock) {
        val current = readIndex()
        val route = id?.let {
            if (current.entries.none { entry -> entry.id == id }) throw IOException("Unknown route")
            readRoute(it)
        }
        val next = current.copy(activeId = id)
        write(index, next)
        next to route
    }

    fun delete(id: String): HikeRouteIndex = synchronized(lock) {
        val current = readIndex()
        val next = HikeRouteIndex(current.entries.filterNot { it.id == id }, current.activeId.takeUnless { it == id })
        write(index, next)
        routeFile(id).delete()
        next
    }

    private fun readIndex(): HikeRouteIndex {
        read(index, HikeRouteIndex::class.java)?.let { return it }
        val input = try {
            legacy.openRead()
        } catch (error: FileNotFoundException) {
            if (legacy.baseFile.exists()) throw error
            return HikeRouteIndex()
        }
        val route = input.bufferedReader(Charsets.UTF_8).use { gson.fromJson(it, HikeRoute::class.java) }
            ?: throw IOException("Empty legacy route")
        val migrated = add(HikeRouteIndex(), route, route.name, legacy.baseFile.lastModified())
        legacy.delete()
        return migrated
    }

    private fun add(current: HikeRouteIndex, route: HikeRoute, fileName: String, time: Long): HikeRouteIndex {
        val item = SavedHikeRoute(UUID.randomUUID().toString(), route.name, fileName, time)
        val file = routeFile(item.id)
        write(file, route)
        val next = HikeRouteIndex(listOf(item) + current.entries, item.id)
        try {
            write(index, next)
            return next
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    private fun routeFile(id: String): File = File(root, "${UUID.fromString(id)}.json")

    private fun readRoute(id: String): HikeRoute = read(routeFile(id), HikeRoute::class.java)
        ?: throw IOException("Saved route is missing")

    private fun <T> read(file: File, type: Class<T>): T? {
        val available = file.takeIf(File::exists) ?: File("${file.path}.bak").takeIf(File::exists) ?: return null
        return available.bufferedReader(Charsets.UTF_8).use { gson.fromJson(it, type) }
            ?: throw IOException("Empty route data")
    }

    private fun write(file: File, value: Any) {
        if (!root.isDirectory && !root.mkdirs()) throw IOException("Unable to create route library")
        val backup = File("${file.path}.bak")
        val pending = File("${file.path}.new")
        // Recover a previous version before preparing a new replacement alongside it.
        if (backup.exists()) {
            val recovered = if (file.exists()) backup.delete() else backup.renameTo(file)
            if (!recovered) throw IOException("Unable to recover route file")
        }
        try {
            pending.outputStream().use { output ->
                val writer = output.bufferedWriter(Charsets.UTF_8)
                gson.toJson(value, writer)
                writer.flush()
                output.fd.sync()
            }
            if (file.exists() && !file.renameTo(backup)) throw IOException("Unable to preserve route file")
            // Readers can recover the backup if the app is stopped between the renames.
            if (!pending.renameTo(file)) throw IOException("Unable to save route file")
            backup.delete()
        } finally {
            pending.delete()
        }
    }

    private companion object {
        val lock = Any()
    }
}
