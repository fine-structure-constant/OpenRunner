package cn.edu.pku.openrunner.feature.hike

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class HikeIntentFilterTest {
    @Test
    fun candidatesCanBeQueriedByMimeTypeBeforeTheSenderGrantsAUri() {
        val manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))
        val activities = manifest.documentElement.elements("activity")
        val main = activities.single { it.getAttribute("android:name") == ".MainActivity" }
        val mimeOnly = main.elements("intent-filter").filter { filter ->
            filter.elements("data").let { data ->
                data.isNotEmpty() && data.all {
                    it.attributes.length == 1 && it.hasAttribute("android:mimeType")
                }
            } && filter.names("category").contains("android.intent.category.DEFAULT")
        }
        for (action in listOf("android.intent.action.VIEW", "android.intent.action.SEND")) {
            val types = mimeOnly.filter { action in it.names("action") }.flatMap { filter ->
                filter.elements("data").map { it.getAttribute("android:mimeType") }
            }
            assertTrue("$action must accept KML", "application/vnd.google-earth.kml+xml" in types)
            assertTrue("$action must accept generic file attachments", "application/octet-stream" in types)
            assertTrue("$action must accept XML attachments", types.containsAll(listOf("application/xml", "text/xml")))
            assertTrue("$action must not claim every file without a path restriction", "*/*" !in types)
        }
    }

    private fun Element.names(tag: String): List<String> = elements(tag).map { it.getAttribute("android:name") }

    private fun Element.elements(tag: String): List<Element> = getElementsByTagName(tag).let { nodes ->
        (0 until nodes.length).map { nodes.item(it) as Element }
    }
}
