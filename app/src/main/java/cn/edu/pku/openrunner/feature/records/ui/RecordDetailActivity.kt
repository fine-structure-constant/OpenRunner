package cn.edu.pku.openrunner.feature.records.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecord
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecordStore
import cn.edu.pku.openrunner.feature.run.domain.RunChartData
import cn.edu.pku.openrunner.feature.run.domain.RunMetrics
import com.google.android.material.appbar.MaterialToolbar
import java.text.DateFormat
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordDetailActivity : AppCompatActivity(R.layout.activity_record_detail) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<MaterialToolbar>(R.id.record_detail_toolbar)
            .setNavigationOnClickListener { finish() }
        val localId = intent.getStringExtra(EXTRA_LOCAL_ID)
        if (localId.isNullOrBlank()) {
            finishMissingRecord()
            return
        }
        lifecycleScope.launch {
            val record = withContext(Dispatchers.IO) {
                LocalRunRecordStore(this@RecordDetailActivity).find(localId)
            }
            if (record == null || record.metricSamples.orEmpty().size < 2) {
                finishMissingRecord()
            } else {
                render(record)
            }
        }
    }

    private fun render(record: LocalRunRecord) {
        findViewById<View>(R.id.record_detail_progress).visibility = View.GONE
        findViewById<View>(R.id.record_detail_content).visibility = View.VISIBLE
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
        val samples = record.metricSamples.orEmpty()
        val paceSeries = RunChartData.paceMinutesPerKm(samples)
        val fastestPace = RunChartData.fastestPaceMinutesPerKm(samples)

        findViewById<TextView>(R.id.record_detail_start).text = getString(
            R.string.record_detail_start,
            dateFormat.format(record.startedAtMillis)
        )
        findViewById<TextView>(R.id.record_detail_end).text = getString(
            R.string.record_detail_end,
            dateFormat.format(record.completedAtMillis)
        )
        findViewById<TextView>(R.id.record_detail_distance).text = getString(
            R.string.record_detail_distance,
            record.distanceMeters / 1_000.0
        )
        findViewById<TextView>(R.id.record_detail_duration).text = getString(
            R.string.record_detail_duration,
            RunMetrics.formatDuration(record.durationSeconds)
        )
        findViewById<TextView>(R.id.record_detail_steps).text = getString(
            R.string.record_detail_steps,
            record.steps
        )
        findViewById<TextView>(R.id.record_detail_fastest_pace).text = getString(
            R.string.record_detail_fastest_pace,
            RunMetrics.formatPace(fastestPace?.times(60)?.roundToInt())
        )
        findViewById<TextView>(R.id.record_detail_samples).text = getString(
            R.string.record_detail_samples,
            samples.size
        )
        findViewById<RunLineChartView>(R.id.record_distance_chart).setData(
            RunChartData.distanceKilometres(samples),
            RunChartType.DISTANCE
        )
        findViewById<RunLineChartView>(R.id.record_pace_chart).setData(
            paceSeries,
            RunChartType.PACE
        )
    }

    private fun finishMissingRecord() {
        Toast.makeText(this, R.string.record_detail_missing, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private const val EXTRA_LOCAL_ID = "record_local_id"

        fun intent(context: Context, localId: String): Intent =
            Intent(context, RecordDetailActivity::class.java)
                .putExtra(EXTRA_LOCAL_ID, localId)
    }
}
