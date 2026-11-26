package cn.edu.pku.openrunner.ui

import android.content.Context
import cn.edu.pku.openrunner.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal fun Context.openRunnerDialog() = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_OpenRunner_Dialog)
