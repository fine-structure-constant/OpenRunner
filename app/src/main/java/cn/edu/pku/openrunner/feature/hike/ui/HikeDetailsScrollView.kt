package cn.edu.pku.openrunner.feature.hike.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import kotlin.math.roundToInt

internal class HikeDetailsScrollView(context: Context, attrs: AttributeSet) : ScrollView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.AT_MOST) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        // In the stacked layout, long route names and scaled text must leave room for the map.
        val limit = (resources.configuration.screenHeightDp * resources.displayMetrics.density * 0.4f).roundToInt()
        val height = minOf(MeasureSpec.getSize(heightMeasureSpec), limit)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
    }
}
