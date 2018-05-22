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
package com.shellmonger.apps.mynotes.services.aws

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager
import com.shellmonger.apps.mynotes.extensions.getConnectivityManager
import com.shellmonger.apps.mynotes.services.AnalyticsService

class AWSAnalyticsService(context: Context) : AnalyticsService {
    private var manager: PinpointManager? = null
    private var isConnected = false

    init {
        val awsConfig = AWSConfiguration(context)
        val creds = CognitoCachingCredentialsProvider(context, awsConfig)
        manager = PinpointManager(PinpointConfiguration(context, creds, awsConfig))

        val networkStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                isConnected = ctx?.getConnectivityManager()?.activeNetworkInfo != null
                if (isConnected) manager?.analyticsClient?.submitEvents()
            }
        }
        context.registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        isConnected = context.getConnectivityManager().activeNetworkInfo != null
    }

    private fun submitEventsIfConnected() {
        if (isConnected) manager?.analyticsClient?.submitEvents()
    }

    override fun startSession() {
        manager?.sessionClient?.startSession()
        submitEventsIfConnected()
    }

    override fun stopSession() {
        manager?.sessionClient?.stopSession()
        submitEventsIfConnected()
    }

    override fun recordEvent(eventName: String, parameters: Map<String, String>?, metrics: Map<String, Double>?) {
        manager?.let {
            val event = it.analyticsClient.createEvent(eventName)
            parameters?.let {
                for ((k, v) in it) event.addAttribute(k, v)
            }
            metrics?.let {
                for ((k, v) in it) event.addMetric(k, v)
            }
            it.analyticsClient.recordEvent(event)
            submitEventsIfConnected()
        }
    }

    override fun pendingEvents(): Long = (manager?.analyticsClient?.allEvents?.size ?: 0).toLong()
}