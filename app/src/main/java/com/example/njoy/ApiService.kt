package com.example.njoy

import com.example.njoy.DataClasesApi.Event
import com.example.njoy.DataClasesApi.EventResponse
import com.example.njoy.DataClasesApi.LoginRequest
import com.example.njoy.DataClasesApi.LoginResponse
import com.example.njoy.DataClasesApi.PaymentRequest
import com.example.njoy.DataClasesApi.PaymentResponse
import com.example.njoy.DataClasesApi.TicketRequest
import com.example.njoy.DataClasesApi.TicketResponse
import com.example.njoy.DataClasesApi.RegisterRequest
import com.example.njoy.DataClasesApi.RegisterResponse

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

object ApiClient {
    private const val BASE_URL = "http://192.168.0.34:8000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

interface ApiService {
    @GET("localidad/")
    suspend fun getLocalidades(): Response<List<DataClasesApi.LocalidadResponse>>
    @GET("localidad/{id}")
    suspend fun getLocalidad(@Path("id") id: Int): Response<DataClasesApi.LocalidadResponse>
    @POST("localidad/")
    suspend fun createLocalidad(@Body ciudad: String): Response<DataClasesApi.LocalidadResponse>
    @GET("genero/")
    suspend fun getGeneros(): Response<List<DataClasesApi.GeneroResponse>>
    @POST("genero/")
    suspend fun createGenero(@Body nombre: String): Response<DataClasesApi.GeneroResponse>
    @GET("evento/")
    suspend fun getEventos(): Response<List<Event>>
    @GET("evento/{id}/")
    suspend fun getEvento(
        @Path("id") id: Int
    ): Response<Event>

    @POST("evento/")
    suspend fun createEvento(@Body evento: DataClasesApi.CreateEventRequest): Response<EventResponse>
    @PUT("evento/{id}")
    suspend fun updateEvento(@Path("id") id: Int, @Body request: DataClasesApi.EventUpdateRequest): Response<Event>

    @DELETE("evento/{id}")
    suspend fun deleteEvento(@Path("id") id: Int): Response<EventResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>


    @POST("pago/")
    suspend fun registerPayment(@Body payment: PaymentRequest): Response<PaymentResponse>
    @GET("pago/")
    suspend fun getPayments(): Response<List<DataClasesApi.PaymentResponse>>
    @GET("ticket/")
    suspend fun getTickets(): Response<List<DataClasesApi.TicketResponse>>
    @POST("ticket/")
    suspend fun createTicket(@Body ticket: TicketRequest): Response<TicketResponse>

    @PUT("ticket/{id}")
    suspend fun updateTicket(@Path("id") id: Int, @Body request: TicketRequest): Response<TicketResponse>

}
