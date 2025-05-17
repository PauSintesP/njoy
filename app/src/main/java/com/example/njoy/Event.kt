package com.example.njoy

data class Event(
    val id: Int,
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
)