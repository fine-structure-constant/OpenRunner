package cn.edu.pku.openrunner.feature.records.domain

import cn.edu.pku.openrunner.core.network.UserStatusDto

internal class RunGoalProgress(status: UserStatusDto) {
    val distanceMeters = (status.current.toLong() + status.bonus).coerceAtLeast(0L)
    val percent: Double? = if (status.target > 0) {
        distanceMeters * 100.0 / status.target
    } else {
        null
    }
}
