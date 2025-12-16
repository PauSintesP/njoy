package com.example.njoy.model

data class EventStats(
    val ingreso_total: Double,
    val ingreso_promedio_ticket: Double,
    val capacidad_total: Int,
    val tickets_vendidos: Int,
    val tickets_disponibles: Int,
    val tickets_escaneados: Int,
    val tasa_asistencia: Double
)

data class StatsAccessToken(
    val access_token: String,
    val expires_in: Int,
    val token_type: String
)

data class PasswordVerificationRequest(
    val password: String
)
