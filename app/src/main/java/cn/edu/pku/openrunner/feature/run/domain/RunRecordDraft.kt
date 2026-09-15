package cn.edu.pku.openrunner.feature.run.domain

data class RunRecordDraft(
    val startedAtMillis: Long,
    val durationSeconds: Int,
    val track: List<TrackPoint>,
    val steps: Int
) {
    val distanceMeters: Int
        get() = TrackDistance.polylineMeters(track).toInt()
}
