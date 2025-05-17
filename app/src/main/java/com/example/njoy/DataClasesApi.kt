package com.example.njoy

class DataClasesApi {


    data class PaymentResponse(
        val id: Int,
        val usuario_id: Int,
        val metodo_pago: String,
        val total: Double,
        val fecha: String,
        val ticket_id: Int
    )

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

    data class PaymentRequest(
        val usuario_id: Int,
        val metodo_pago: String,
        val total: Double,
        val fecha: String,
        val ticket_id: Int
    )

    data class RegisterRequest(
        val user: String,
        val ncompleto: String,
        val email: String,
        val fnacimiento: String,
        val contrasena: String
    )

    data class LoginRequest(
        val email: String,
        val contrasena: String
    )

    data class LoginResponse(
        val user: String,
        val ncompleto: String,
        val email: String,
        val fnacimiento: String,
        val contrasena: String,
        val id: Int
    )
    data class EventUpdateRequest(
        val nombre: String,
        val descripcion: String,
        val localidad_id: Int,
        val recinto: String,
        val plazas: Int,
        val fechayhora: String,
        val tipo: String,
        val categoria_precio: String,
        val organizador_dni: String,
        val genero_id: Int,
        val imagen: String
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
                    categoria_precio = event.categoria_precio,
                    organizador_dni = event.organizador_dni,
                    genero_id = event.genero_id,
                    imagen = event.imagen
                )
            }
        }
    }
}