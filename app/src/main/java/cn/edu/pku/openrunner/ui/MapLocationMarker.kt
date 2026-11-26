package cn.edu.pku.openrunner.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.core.orientation.wrapHeading
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import kotlin.math.roundToInt

internal class LocationGlyph(context: Context) {
    private val density = context.resources.displayMetrics.density
    private val halfSize = (23f * density).roundToInt()
    private val arrow = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_location_point)).apply {
        setBounds(-halfSize, -halfSize, halfSize, halfSize)
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, x: Float, y: Float, heading: Int?) {
        if (heading == null) {
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, 7f * density, paint)
            paint.color = 0xff0096fa.toInt()
            canvas.drawCircle(x, y, 5.6f * density, paint)
            return
        }
        canvas.withSave {
            translate(x, y)
            rotate(heading.toFloat())
            arrow.draw(this)
        }
    }

    fun bitmap(directional: Boolean): Bitmap = createBitmap(halfSize * 2, halfSize * 2).also {
        val heading = if (directional) 0 else null
        draw(Canvas(it), halfSize.toFloat(), halfSize.toFloat(), heading)
    }
}

internal class MapLocationMarker(context: Context, private val map: AMap) {
    private val glyph = LocationGlyph(context)
    private val dotIcon by lazy { BitmapDescriptorFactory.fromBitmap(glyph.bitmap(false)) }
    private val directionIcon by lazy { BitmapDescriptorFactory.fromBitmap(glyph.bitmap(true)) }
    private var marker: Marker? = null
    private var heading: Int? = null

    fun moveTo(position: LatLng) {
        val current = marker
        if (current != null) {
            current.position = position
        } else {
            marker = map.addMarker(
                MarkerOptions().position(position).anchor(0.5f, 0.5f).setFlat(true)
                    .icon(if (heading == null) dotIcon else directionIcon)
                    .rotateAngle(amapMarkerAngle(heading)).zIndex(4f)
            )
        }
    }

    fun updateHeading(value: Int?) {
        val previous = heading
        if (value == previous) return
        heading = value
        marker?.let {
            if ((previous == null) != (value == null)) it.setIcon(if (value == null) dotIcon else directionIcon)
            it.rotateAngle = amapMarkerAngle(value)
        }
    }

    fun clear() {
        marker?.remove()
        marker = null
    }
}

internal fun amapMarkerAngle(heading: Int?): Float =
    if (heading == null) 0f else wrapHeading(-heading.toFloat()) // AMap measures counterclockwise from north.
