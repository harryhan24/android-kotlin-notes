package com.shellmonger.apps.mynotes.services

interface AnalyticsService {
    /**
     * Record a startSession event
     */
    fun startSession()

    /**
     * Record a stopSession event
     */
    fun stopSession()

    /**
     * Record an analytics event
     */
    fun recordEvent(eventName: String, parameters: Map<String,String>? = null, metrics: Map<String,Double>? = null)

    /**
     * Get the number of pending events
     */
    fun pendingEvents(): Long
}