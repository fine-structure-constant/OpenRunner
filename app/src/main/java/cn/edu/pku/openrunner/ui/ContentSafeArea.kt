package cn.edu.pku.openrunner.ui

import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

internal fun ComponentActivity.applyContentSafeArea() {
    // Older versions already fit this window inside the system bars.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

    enableEdgeToEdge()
    val content = findViewById<View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(content) { host, insets ->
        val safe = insets.getInsetsIgnoringVisibility(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
        var cornerTop = 0
        var cornerBottom = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val windowInsets = insets.toWindowInsets()
            cornerTop = maxOf(
                windowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0,
                windowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
            )
            cornerBottom = maxOf(
                windowInsets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0,
                windowInsets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
            )
        }
        host.updatePadding(
            left = safe.left,
            top = maxOf(safe.top, cornerTop),
            right = safe.right,
            bottom = maxOf(safe.bottom, cornerBottom, keyboard.bottom)
        )
        // The activity fits its entire content here, including the navigation drawer.
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(content)
}
