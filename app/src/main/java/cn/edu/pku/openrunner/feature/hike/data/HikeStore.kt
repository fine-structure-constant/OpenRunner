package cn.edu.pku.openrunner.feature.hike.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.AtomicFile
import cn.edu.pku.openrunner.feature.hike.domain.GeoPoint
import cn.edu.pku.openrunner.feature.hike.domain.HikeRoute
import cn.edu.pku.openrunner.feature.hike.domain.HikeSample
import cn.edu.pku.openrunner.feature.hike.domain.distanceMeters
import com.google.gson.Gson
import java.io.File

data class SavedHike(val id: Long, val started: Long, val finished: Long?)
data class RecordedFix(
    val point: GeoPoint,
    val time: Long,
    val horizontalAccuracy: Float,
    val verticalAccuracy: Float?,
    val monotonicMs: Long
)
data class HikeLog(
    val hike: SavedHike,
    val samples: List<HikeSample>,
    val distance: Double,
    val ascent: Double
)

class HikeStore(context: Context) : SQLiteOpenHelper(context, "hike.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE hike (id INTEGER PRIMARY KEY, started INTEGER NOT NULL, finished INTEGER)")
        db.execSQL(
            "CREATE TABLE sample (hike INTEGER NOT NULL, seq INTEGER NOT NULL, time INTEGER NOT NULL, " +
                "lat REAL NOT NULL, lon REAL NOT NULL, altitude REAL, section INTEGER NOT NULL, gain REAL, " +
                "verticalAccuracy REAL, PRIMARY KEY (hike, seq))"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun start(time: Long): Long = writableDatabase.insertOrThrow(
        "hike", null, ContentValues().apply { put("started", time) }
    )

    fun finish(id: Long, time: Long) {
        writableDatabase.update("hike", ContentValues().apply { put("finished", time) }, "id=?", arrayOf(id.toString()))
    }

    fun append(id: Long, sequence: Int, sample: HikeSample) {
        writableDatabase.insertOrThrow("sample", null, ContentValues().apply {
            put("hike", id)
            put("seq", sequence)
            put("time", sample.timestamp)
            put("lat", sample.point.latitude)
            put("lon", sample.point.longitude)
            put("altitude", sample.point.altitude)
            put("section", sample.section)
            put("gain", sample.segmentAscent)
            put("verticalAccuracy", sample.verticalAccuracy)
        })
    }

    fun hikes(): List<SavedHike> = readableDatabase.rawQuery(
        "SELECT id, started, finished FROM hike ORDER BY id DESC", null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(SavedHike(cursor.getLong(0), cursor.getLong(1), if (cursor.isNull(2)) null else cursor.getLong(2)))
            }
        }
    }

    fun read(hike: SavedHike): HikeLog {
        val samples = readableDatabase.rawQuery(
            "SELECT time, lat, lon, altitude, section, gain, verticalAccuracy FROM sample WHERE hike=? ORDER BY seq",
            arrayOf(hike.id.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(HikeSample(
                        GeoPoint(cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3).takeUnless { cursor.isNull(3) }),
                        cursor.getLong(0),
                        cursor.getInt(4),
                        cursor.getDouble(5).takeUnless { cursor.isNull(5) },
                        cursor.getFloat(6).takeUnless { cursor.isNull(6) }
                    ))
                }
            }
        }
        val distance = samples.zipWithNext { a, b ->
            if (a.section == b.section) distanceMeters(a.point, b.point) else 0.0
        }.sum()
        return HikeLog(hike, samples, distance, samples.sumOf { it.segmentAscent ?: 0.0 })
    }
}

class ReferenceRouteStore(context: Context) {
    private val gson = Gson()
    private val file = AtomicFile(File(context.filesDir, "hike-reference.json"))

    fun clear() = file.delete()

    fun read(): HikeRoute? {
        if (!file.baseFile.exists()) return null
        return file.openRead().bufferedReader().use { gson.fromJson(it, HikeRoute::class.java) }
    }

    @Synchronized
    fun save(route: HikeRoute) {
        val output = file.startWrite()
        try {
            output.write(gson.toJson(route).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
