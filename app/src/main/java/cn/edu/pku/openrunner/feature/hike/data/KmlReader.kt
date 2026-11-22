package cn.edu.pku.openrunner.feature.hike.data

import cn.edu.pku.openrunner.feature.hike.domain.GeoPoint
import cn.edu.pku.openrunner.feature.hike.domain.HikeRoute
import cn.edu.pku.openrunner.feature.hike.domain.HikeWaypoint
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.ext.DefaultHandler2

object KmlReader {
    private const val MAX_BYTES = 16 * 1024 * 1024
    private const val MAX_POINTS = 150_000
    private const val GX_NAMESPACE = "http://www.google.com/kml/ext/2.2"
    private val whitespace = Regex("\\s+")

    fun read(stream: InputStream, fallbackName: String): HikeRoute {
        val handler = Handler(fallbackName)
        val reader = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
            .newSAXParser().xmlReader
        reader.contentHandler = handler
        reader.entityResolver = handler
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler)
        reader.parse(InputSource(LimitedStream(stream)))
        return handler.route()
    }

    private class Handler(private val fallbackName: String) : DefaultHandler2() {
        private val stack = mutableListOf<String>()
        private val text = StringBuilder()
        private val lines = mutableListOf<List<GeoPoint>>()
        private val waypoints = mutableListOf<HikeWaypoint>()
        private var track: MutableList<GeoPoint>? = null
        private val placemarkPoints = mutableListOf<GeoPoint>()
        private var linesBeforePlacemark = 0
        private var placemarkName = ""
        private var documentName = ""
        private var firstLineName = ""
        private var pointCount = 0

        override fun startDTD(name: String?, publicId: String?, systemId: String?) {
            throw SAXException("KML document types are unsupported")
        }

        override fun startElement(uri: String, name: String, qName: String, attributes: Attributes) {
            if (stack.size >= 64 || stack.isEmpty() && name != "kml") throw SAXException("Invalid KML")
            stack.add(name)
            text.setLength(0)
            if (name == "Placemark") {
                placemarkPoints.clear()
                linesBeforePlacemark = lines.size
                placemarkName = ""
            }
            if (uri == GX_NAMESPACE && name == "Track") track = mutableListOf()
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            when (stack.lastOrNull()) {
                "name", "coordinates", "coord" -> text.append(ch, start, length)
            }
        }

        override fun endElement(uri: String, name: String, qName: String) {
            stack.removeAt(stack.lastIndex)
            val parent = stack.lastOrNull()
            when {
                name == "name" && parent == "Document" && documentName.isEmpty() ->
                    documentName = text.toString().trim().take(160)
                name == "name" && parent == "Placemark" -> placemarkName = text.toString().trim().take(160)
                name == "coordinates" && parent == "LineString" -> {
                    val points = whitespace.splitToSequence(text.toString().trim())
                        .mapNotNull { point(it.split(',')) }.toList()
                    if (points.size >= 2) lines.add(points)
                }
                name == "coordinates" && parent == "Point" ->
                    whitespace.splitToSequence(text.toString().trim()).firstOrNull()
                        ?.let { point(it.split(',')) }?.let(placemarkPoints::add)
                uri == GX_NAMESPACE && name == "coord" ->
                    point(whitespace.split(text.toString().trim()))?.let { track?.add(it) }
                uri == GX_NAMESPACE && name == "Track" -> {
                    track?.takeIf { it.size >= 2 }?.let(lines::add)
                    track = null
                }
                name == "Placemark" -> {
                    if (firstLineName.isEmpty() && lines.size > linesBeforePlacemark) firstLineName = placemarkName
                    waypoints.addAll(placemarkPoints.map { HikeWaypoint(it, placemarkName) })
                }
            }
            text.setLength(0)
        }

        private fun point(values: List<String>): GeoPoint? {
            val longitude = values.getOrNull(0)?.toDoubleOrNull() ?: return null
            val latitude = values.getOrNull(1)?.toDoubleOrNull() ?: return null
            val altitude = values.getOrNull(2)?.toDoubleOrNull()?.takeIf(Double::isFinite)
            val point = GeoPoint(latitude, longitude, altitude).takeIf { it.valid } ?: return null
            if (++pointCount > MAX_POINTS) throw SAXException("Too many KML points")
            return point
        }

        fun route(): HikeRoute {
            if (lines.isEmpty() && waypoints.isEmpty()) throw SAXException("No usable KML geometry")
            return HikeRoute(documentName.ifBlank { firstLineName.ifBlank { fallbackName } }, lines, waypoints)
        }
    }

    private class LimitedStream(input: InputStream) : FilterInputStream(input) {
        private var remaining = MAX_BYTES

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) consume(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val count = super.read(buffer, offset, minOf(length, remaining + 1))
            if (count > 0) consume(count)
            return count
        }

        private fun consume(count: Int) {
            remaining -= count
            if (remaining < 0) throw IOException("KML is too large")
        }
    }
}
