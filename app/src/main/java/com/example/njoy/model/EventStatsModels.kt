package com.example.njoy.model

data class PasswordVerificationRequest(
    val password: String
)

data class StatsAccessToken(
    val access: Boolean,
    val message: String
)

data class EventStats(
    val total_tickets: Int,
    val tickets_sold: Int,
    val tickets_available: Int,
    val total_revenue: Double,
    val attendees: Int,
    val attendance_by_hour: Map<String, Int>?,
    val tickets_by_type: Map<String, Int>?
)
