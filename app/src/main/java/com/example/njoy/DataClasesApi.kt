package com.example.njoy

class DataClasesApi {

    // User model matching the new API
    data class User(
        val id: Int,
        val nombre: String,
        val apellidos: String,
        val email: String,
        val fecha_nacimiento: String,
        val pais: String?,
        val role: String,  // "user", "promotor", "owner", "admin"
        val is_active: Boolean,
        val is_banned: Boolean,
        val created_at: String
    )

    // Login request
    data class LoginRequest(
        val email: String,
        val contrasena: String
    )

    // Login response with JWT tokens
    data class LoginResponse(
        val access_token: String,
        val refresh_token: String,
        val token_type: String
    )

    // Refresh token request
    data class RefreshTokenRequest(
        val refresh_token: String
    )

    // Change password request
    data class ChangePasswordRequest(
        val current_password: String,
        val new_password: String
    )

    // Register request with correct field names
    data class RegisterRequest(
        val nombre: String,
        val apellidos: String,
        val email: String,
        val fecha_nacimiento: String,  // Format: "YYYY-MM-DD"
        val pais: String?,
        val contrasena: String,
        val role: String = "user"
    )

    // Register response (returns the created user)
    data class RegisterResponse(
        val id: Int,
        val nombre: String,
        val apellidos: String,
        val email: String,
        val fecha_nacimiento: String,
        val pais: String?,
        val role: String,
        val is_active: Boolean,
        val is_banned: Boolean,
        val created_at: String
    )

    // Payment models
    data class PaymentResponse(
        val id: Int,
        val usuario_id: Int,
        val metodo_pago: String,
        val total: Double,
        val fecha: String,
        val ticket_id: Int
    )

    data class PaymentRequest(
        val usuario_id: Int,
        val metodo_pago: String,
        val total: Double,
        val fecha: String,
        val ticket_id: Int
    )

    // Genre models
    data class GeneroResponse(
        val id: Int,
        val nombre: String,
        val cantidad: Int
    )

    // TEAMS MODELS
    data class Team(
        val id: Int,
        val nombre_equipo: String,
        val descripcion: String?,
        val leader_id: Int,
        val created_at: String,
        val num_miembros: Int?
    )

    data class CreateTeamRequest(
        val nombre_equipo: String,
        val descripcion: String?
    )

    data class TeamMember(
        val id: Int,
        val equipo_id: Int,
        val usuario_id: Int,
        val estado: String,  // "pending", "accepted", "rejected"
        val usuario: User?
    )

    // CREATE EVENT REQUEST - removed duplicate, see line 249

    // Location models
    data class LocalidadResponse(
        val ciudad: String,
        val id: Int
    )
    
    // Organizer response
    data class OrganizadorResponse(
        val dni: String,
        val ncompleto: String,
        val email: String,
        val telefono: String,
        val web: String?
    )

    // Ticket models
    data class TicketResponse(
        val id: Int,
        val evento_id: Int,
        val usuario_id: Int,
        val activado: Boolean
    )

    data class TicketRequest(
        val evento_id: Int,
        val usuario_id: Int,
        val activado: Boolean
    )
    
    // Ticket Purchase
    data class TicketPurchaseRequest(
        val evento_id: Int,
        val cantidad: Int = 1,
        val nombres_asistentes: List<String>? = null
    )

    data class TicketInfo(
        val id: Int,
        val codigo: String,
        val nombre: String?,
        val evento_id: Int
    )

    data class TicketPurchaseResponse(
        val message: String,
        val cantidad: Int,
        val total: Double,
        val tickets: List<TicketInfo>
    )

    // Evento detail helper for tickets
    data class EventoDetail(
        val id: Int,
        val nombre: String,
        val descripcion: String,
        val fechayhora: String?,
        val recinto: String,
        val imagen: String?,
        val precio: Double?,
        val tipo: String
    )
    
    data class UserSlim(
        val id: Int,
        val nombre: String,
        val apellidos: String,
        val email: String
    )

    // Detail of a specific ticket
    data class TicketDetailResponse(
        val ticket_id: Int,
        val codigo_ticket: String?, 
        val nombre_asistente: String?,
        val activado: Boolean,
        val usuario: UserSlim?,
        val evento: EventoDetail?
    )

    // My tickets list response item
    data class MyTicketResponse(
        val ticket_id: Int,
        val codigo: String?, // Match backend "codigo" field
        val codigo_ticket: String?, // Optional legacy/alt field
        val nombre_asistente: String?,
        val activado: Boolean,
        val evento: EventoDetail?
    )

    // Scanner models
    data class TicketScanRequest(
        val ticket_id: Int
    )

    data class TicketScanResponse(
        val success: Boolean,
        val message: String,
        val ticket: TicketResponse?,
        val event_name: String?,
        val user_name: String?,
        val ticket_id: Int? = null // Added for fallback access
    )

    // Event models
    data class Event(
        val id: Int,
        val nombre: String,
        val descripcion: String,
        val localidad_id: Int?,
        val recinto: String,
        val plazas: Int,
        val fechayhora: String,
        val tipo: String,
        val precio: Double?,
        val organizador_dni: String?,
        val genero_id: Int?,
        val imagen: String?,
        val creador_id: Int?,
        val tickets_vendidos: Int?
    )

    data class CreateEventRequest(
        val nombre: String,
        val descripcion: String,
        val localidad_id: Int?,
        val recinto: String?,
        val plazas: Int?,
        val fechayhora: String?,
        val tipo: String?,
        val precio: Double?,
        val organizador_dni: String?,
        val genero_id: Int?,
        val imagen: String?
    )

    data class EventResponse(
        val id: Int,
        val nombre: String,
        val descripcion: String,
        val localidad_id: Int?,
        val recinto: String,
        val plazas: Int,
        val fechayhora: String,
        val tipo: String,
        val precio: Double?,
        val organizador_dni: String?,
        val genero_id: Int?,
        val imagen: String?,
        val creador_id: Int?
    )

    data class EventUpdateRequest(
        val nombre: String,
        val descripcion: String,
        val localidad_id: Int?,
        val recinto: String?,
        val plazas: Int?,
        val fechayhora: String?,
        val tipo: String?,
        val precio: Double?,
        val organizador_dni: String?,
        val genero_id: Int?,
        val imagen: String?
    ) {
        companion object {
            fun fromEvent(event: Event): EventUpdateRequest {
                return EventUpdateRequest(
                    nombre = event.nombre,
                    descripcion = event.descripcion,
                    localidad_id = event.localidad_id,
                    recinto = event.recinto,
                    plazas = event.plazas,
                    fechayhora = event.fechayhora,
                    tipo = event.tipo,
                    precio = event.precio,
                    organizador_dni = event.organizador_dni,
                    genero_id = event.genero_id,
                    imagen = event.imagen
                )
            }
        }
    }
}
