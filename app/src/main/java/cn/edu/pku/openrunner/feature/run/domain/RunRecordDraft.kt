package cn.edu.pku.openrunner.feature.run.domain

data class RunRecordDraft(
    val startedAtMillis: Long,
    val completedAtMillis: Long,
    val durationSeconds: Int,
    val track: List<TrackPoint>,
    val steps: Int,
    val metricSamples: List<RunMetricSample>
) {
    val distanceMeters: Int
        get() = TrackDistance.polylineMeters(track).toInt()
}
