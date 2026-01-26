package com.lightningstudio.watchrss.debug

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import com.lightningstudio.watchrss.BuildConfig
import java.util.Collections
import java.util.WeakHashMap

object PerformanceMonitor {
    private const val TAG = "perf"
    private val statsMap = Collections.synchronizedMap(WeakHashMap<ComponentActivity, FrameStats>())

    fun attach(activity: ComponentActivity) {
        if (!BuildConfig.DEBUG) return
        val window = activity.window
        val stateHolder = PerformanceMetricsState.getHolderForHierarchy(window.decorView).state
        stateHolder?.putState("screen", activity.javaClass.simpleName)

        val stats = FrameStats(activity.javaClass.simpleName)
        statsMap[activity] = stats
        val jankStats = JankStats.createAndTrack(window) { frameData ->
            stats.onFrame(frameData)
        }

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                jankStats.isTrackingEnabled = true
            }

            override fun onPause(owner: LifecycleOwner) {
                jankStats.isTrackingEnabled = false
                logAndReset(stats)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                jankStats.isTrackingEnabled = false
                logAndReset(stats)
            }
        })
    }

    fun setScenario(activity: ComponentActivity, scenario: String) {
        if (!BuildConfig.DEBUG) return
        val stateHolder = PerformanceMetricsState.getHolderForHierarchy(activity.window.decorView).state
        stateHolder?.putState("scenario", scenario)
        statsMap[activity]?.updateScenario(scenario)
    }

    private fun logAndReset(stats: FrameStats) {
        val snapshot = stats.snapshotAndReset()
        if (snapshot.totalFrames == 0L) return
        val avgMs = snapshot.totalDurationNanos / snapshot.totalFrames / 1_000_000f
        val maxMs = snapshot.maxDurationNanos / 1_000_000f
        val jankPct = snapshot.jankFrames * 100f / snapshot.totalFrames
        val message = buildString {
            append("screen=${snapshot.screenName} ")
            snapshot.scenario?.let { append("scenario=$it ") }
            append("frames=${snapshot.totalFrames} ")
            append("jank=${snapshot.jankFrames} ")
            append("jankPct=${"%.2f".format(jankPct)} ")
            append("avgMs=${"%.2f".format(avgMs)} ")
            append("maxMs=${"%.2f".format(maxMs)} ")
        }
        DebugLogBuffer.log(TAG, message)
        Log.d(TAG, message)
    }
}

private class FrameStats(private val screenName: String) {
    private var totalFrames: Long = 0
    private var jankFrames: Long = 0
    private var totalDurationNanos: Long = 0
    private var maxDurationNanos: Long = 0
    private var scenario: String? = null

    fun onFrame(frameData: FrameData) {
        totalFrames += 1
        if (frameData.isJank) {
            jankFrames += 1
        }
        totalDurationNanos += frameData.frameDurationUiNanos
        if (frameData.frameDurationUiNanos > maxDurationNanos) {
            maxDurationNanos = frameData.frameDurationUiNanos
        }
    }

    fun updateScenario(value: String) {
        scenario = value
    }

    fun snapshotAndReset(): FrameStatsSnapshot {
        val snapshot = FrameStatsSnapshot(
            screenName = screenName,
            scenario = scenario,
            totalFrames = totalFrames,
            jankFrames = jankFrames,
            totalDurationNanos = totalDurationNanos,
            maxDurationNanos = maxDurationNanos
        )
        totalFrames = 0
        jankFrames = 0
        totalDurationNanos = 0
        maxDurationNanos = 0
        scenario = null
        return snapshot
    }
}

private data class FrameStatsSnapshot(
    val screenName: String,
    val scenario: String?,
    val totalFrames: Long,
    val jankFrames: Long,
    val totalDurationNanos: Long,
    val maxDurationNanos: Long
)
