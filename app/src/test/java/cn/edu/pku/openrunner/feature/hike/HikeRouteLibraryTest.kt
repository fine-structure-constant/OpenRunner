package cn.edu.pku.openrunner.feature.hike

import cn.edu.pku.openrunner.feature.hike.data.ReferenceRouteStore
import cn.edu.pku.openrunner.feature.hike.domain.GeoPoint
import cn.edu.pku.openrunner.feature.hike.domain.HikeRoute
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HikeRouteLibraryTest {
    @get:Rule
    val files = TemporaryFolder()

    @Test
    fun preservesPreviouslyImportedRouteOnUpgrade() {
        val route = route(115.3)
        File(files.root, "hike-reference.json").writeText(Gson().toJson(route), Charsets.UTF_8)
        val (index, active) = ReferenceRouteStore(files.root).snapshot()
        assertEquals(route, active)
        assertEquals(index.entries.single().id, index.activeId)
        assertEquals(index to active, ReferenceRouteStore(files.root).snapshot())
    }

    @Test
    fun keepsSameNameImportsAndRemembersSelectionAcrossRestarts() {
        val store = ReferenceRouteStore(files.root)
        val first = store.add(route(115.3), "route.kml").entries.single().id
        val second = store.add(route(116.2), "route.kml").entries.first().id
        assertNotEquals(first, second)
        assertEquals(2, store.snapshot().first.entries.size)

        store.select(first)
        assertEquals(route(115.3), ReferenceRouteStore(files.root).snapshot().second)
        store.select(null)
        assertNull(ReferenceRouteStore(files.root).snapshot().second)

        store.select(second)
        store.delete(first)
        assertEquals(route(116.2), ReferenceRouteStore(files.root).snapshot().second)
        store.delete(second)
        val (index, active) = ReferenceRouteStore(files.root).snapshot()
        assertTrue(index.entries.isEmpty())
        assertNull(index.activeId)
        assertNull(active)
    }

    @Test
    fun retainsCommittedRouteWhenAnIndexReplacementWasInterrupted() {
        val original = route(115.3)
        val store = ReferenceRouteStore(files.root)
        val saved = store.add(original, "route.kml")
        val index = File(files.root, "hike-routes/index.json")
        assertTrue(index.renameTo(File("${index.path}.bak")))
        File("${index.path}.new").writeText("{")

        val reopened = ReferenceRouteStore(files.root)
        assertEquals(saved to original, reopened.snapshot())
        reopened.select(null)
        assertEquals(saved.copy(activeId = null) to null, ReferenceRouteStore(files.root).snapshot())
    }

    private fun route(longitude: Double) = HikeRoute(
        "Hill route", listOf(listOf(GeoPoint(40.0, longitude), GeoPoint(40.015, longitude + 0.02)))
    )
}
