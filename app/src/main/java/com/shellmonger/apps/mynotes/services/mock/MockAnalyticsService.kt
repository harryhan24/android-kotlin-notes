package com.shellmonger.apps.mynotes.services.mock

import android.util.Log
import com.shellmonger.apps.mynotes.services.AnalyticsService

class MockAnalyticsService : AnalyticsService {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    override fun startSession() {
        Log.i(TAG, "Analytics --> start session")
    }

    override fun stopSession() {
        Log.i(TAG, "Analytics --> stop session")
    }

    override fun recordEvent(eventName: String, parameters: Map<String, String>?, metrics: Map<String, Double>?) {
        val event = StringBuilder()
        event.append("Analytics --> record event $eventName")
        parameters?.let {
            for ((paramKey, paramValue) in parameters) {
                event.append("\n\tparam[$paramKey] = $paramValue")
            }
        }
        metrics?.let {
            for ((metricKey, metricValue) in metrics) {
                event.append("\n\tmetric[$metricKey] = $metricValue")
            }
        }
        Log.i(TAG, event.toString())
    }

    override fun pendingEvents(): Long = 0L
}