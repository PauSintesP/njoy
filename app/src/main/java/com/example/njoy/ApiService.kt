package com.example.njoy

import com.example.njoy.DataClasesApi.EventUpdateRequest
import com.example.njoy.DataClasesApi.LoginRequest
import com.example.njoy.DataClasesApi.LoginResponse
import com.example.njoy.DataClasesApi.PaymentRequest
import com.example.njoy.DataClasesApi.PaymentResponse
import com.example.njoy.DataClasesApi.RegisterRequest
import com.example.njoy.DataClasesApi.TicketRequest
import com.example.njoy.DataClasesApi.TicketResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

interface ApiService {
    @GET("evento/")
    suspend fun getEventos(): Response<List<Event>>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("ticket/")
    suspend fun createTicket(@Body ticket: TicketRequest): Response<TicketResponse>

    @POST("pago/")
    suspend fun registerPayment(@Body payment: PaymentRequest): Response<PaymentResponse>
    @GET("ticket/")
    suspend fun getTickets(): Response<List<DataClasesApi.TicketResponse>>

    @GET("evento/{id}/")
    suspend fun getEvento(
        @Path("id") id: Int
    ): Response<Event>


    @PUT("evento/{id}")
    suspend fun updateEvento(@Path("id") id: Int, @Body request: DataClasesApi.EventUpdateRequest): Response<Event>
}
