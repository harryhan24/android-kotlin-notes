/*
    Copyright 2018 Adrian Hall

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
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