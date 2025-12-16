package com.example.njoy

import android.content.Context
import com.example.njoy.DataClasesApi.ChangePasswordRequest
import com.example.njoy.DataClasesApi.CreateEventRequest
import com.example.njoy.DataClasesApi.Event
import com.example.njoy.DataClasesApi.EventResponse
import com.example.njoy.DataClasesApi.EventUpdateRequest
import com.example.njoy.DataClasesApi.GeneroResponse
import com.example.njoy.DataClasesApi.LocalidadResponse
import com.example.njoy.DataClasesApi.LoginRequest
import com.example.njoy.DataClasesApi.LoginResponse
import com.example.njoy.DataClasesApi.MyTicketResponse
import com.example.njoy.DataClasesApi.PaymentRequest
import com.example.njoy.DataClasesApi.PaymentResponse
import com.example.njoy.DataClasesApi.RefreshTokenRequest
import com.example.njoy.DataClasesApi.RegisterRequest
import com.example.njoy.DataClasesApi.RegisterResponse
import com.example.njoy.DataClasesApi.TicketDetailResponse
import com.example.njoy.DataClasesApi.TicketPurchaseRequest
import com.example.njoy.DataClasesApi.TicketPurchaseResponse
import com.example.njoy.DataClasesApi.TicketRequest
import com.example.njoy.DataClasesApi.TicketResponse
import com.example.njoy.DataClasesApi.TicketScanResponse
import com.example.njoy.DataClasesApi.TicketScanRequest
import com.example.njoy.DataClasesApi.User

import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

object ApiClient {
    // API URL - Production
    //private const val BASE_URL = "http://192.168.18.52:8000/"
    private const val BASE_URL = "https://projecte-n-joy.vercel.app/"

    private fun getAuthInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val token = SessionManager.getAccessToken(context)
            val request = if (token.isNotEmpty()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
    }

    private fun getTokenAuthenticator(context: Context): Authenticator {
        return Authenticator { _, response ->
            // Si ya fallamos demasiadas veces, nos rendimos
            if (responseCount(response) >= 3) {
                return@Authenticator null
            }

            // Sincronizar para evitar múltiples refrescos simultáneos
            synchronized(this) {
                val currentToken = SessionManager.getAccessToken(context)
                val requestToken = response.request().header("Authorization")?.substringAfter("Bearer ")

                // Si el token cambió mientras esperábamos (otro hilo lo refrescó), reintentar con el nuevo
                if (currentToken != requestToken && currentToken.isNotEmpty()) {
                    return@Authenticator response.request().newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // Intentar refrescar
                val refreshToken = SessionManager.getRefreshToken(context)
                if (refreshToken.isEmpty()) {
                    return@Authenticator null
                }

                val refreshResponse = refreshCall(refreshToken)
                if (refreshResponse != null) {
                    SessionManager.saveAccessToken(context, refreshResponse.access_token)
                    if (refreshResponse.refresh_token.isNotEmpty()) {
                        SessionManager.saveRefreshToken(context, refreshResponse.refresh_token)
                    }

                    return@Authenticator response.request().newBuilder()
                        .header("Authorization", "Bearer ${refreshResponse.access_token}")
                        .build()
                } else {
                    // Refresco fallido, cerrar sesión
                    SessionManager.logout(context)
                    return@Authenticator null
                }
            }
        }
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var result = 1
        var prior = response.priorResponse()
        while (prior != null) {
            result++
            prior = prior.priorResponse()
        }
        return result
    }

    private fun refreshCall(refreshToken: String): LoginResponse? {
        // Crear un cliente limpio sin interceptores para el refresh
        val client = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)

        return try {
            val call = api.refreshTokenSync(RefreshTokenRequest(refreshToken))
            call.execute().body()
        } catch (e: Exception) {
            null
        }
    }

    fun getApiService(context: Context): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(getAuthInterceptor(context))
            .authenticator(getTokenAuthenticator(context))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

interface ApiService {
    // Authentication endpoints
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("token/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    // Synchronous call for Authenticator
    @POST("token/refresh")
    fun refreshTokenSync(@Body request: RefreshTokenRequest): Call<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("me")
    suspend fun getCurrentUser(): Response<User>

    @PUT("usuario/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: User): Response<User>

    @POST("usuario/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Void>

    // Location endpoints
    @GET("localidad/")
    suspend fun getLocalidades(): Response<List<LocalidadResponse>>

    @GET("localidad/{id}")
    suspend fun getLocalidad(@Path("id") id: Int): Response<LocalidadResponse>

    @POST("localidad/")
    suspend fun createLocalidad(@Body ciudad: String): Response<LocalidadResponse>

    // Genre endpoints
    @GET("genero/")
    suspend fun getGeneros(): Response<List<GeneroResponse>>

    @POST("genero/")
    suspend fun createGenero(@Body nombre: String): Response<GeneroResponse>

    // Event endpoints
    @GET("evento/")
    suspend fun getEventos(): Response<List<Event>>
    
    @GET("eventos/mis-eventos")
    suspend fun getEventsMine(): Response<List<Event>>

    @GET("teams/events")
    suspend fun getTeamEvents(): Response<List<Event>> 


    @GET("evento/{id}/")
    suspend fun getEvento(@Path("id") id: Int): Response<Event>

    @POST("evento/")
    suspend fun createEvento(@Body evento: CreateEventRequest): Response<EventResponse>

    @PUT("evento/{id}")
    suspend fun updateEvento(@Path("id") id: Int, @Body request: EventUpdateRequest): Response<Event>

    @DELETE("evento/{id}")
    suspend fun deleteEvento(@Path("id") id: Int): Response<EventResponse>

    // Payment endpoints
    @POST("pago/")
    suspend fun registerPayment(@Body payment: PaymentRequest): Response<PaymentResponse>

    @GET("pago/")
    suspend fun getPayments(): Response<List<PaymentResponse>>

    // Ticket endpoints
    @GET("ticket/")
    suspend fun getTickets(): Response<List<TicketResponse>> // Old endpoint, keeps compatibility
    
    @GET("tickets/my-tickets")
    suspend fun getMyTickets(): Response<List<MyTicketResponse>>

    @POST("tickets/purchase")
    suspend fun purchaseTickets(@Query("evento_id") eventId: Int, @Query("cantidad") quantity: Int): Response<TicketPurchaseResponse>

    @GET("tickets/{ticket_id}")
    suspend fun getTicketDetail(@Path("ticket_id") ticketId: Int): Response<TicketDetailResponse>

    @POST("tickets/scan/{codigo_ticket}")
    suspend fun scanTicket(@Path("codigo_ticket") codigo: String): Response<TicketScanResponse>

    @POST("scanner/validate-ticket")
    suspend fun validateTicket(@Body request: TicketScanRequest): Response<TicketScanResponse>

    @POST("scanner/activate-ticket/{ticket_id}")
    suspend fun activateTicket(@Path("ticket_id") ticketId: Int): Response<TicketScanResponse>

    @POST("ticket/")
    suspend fun createTicket(@Body ticket: TicketRequest): Response<TicketResponse>

    @PUT("ticket/{id}")
    suspend fun updateTicket(@Path("id") id: Int, @Body request: TicketRequest): Response<TicketResponse>

    // Stats endpoints
    @POST("evento/{evento_id}/verificar-acceso-estadisticas")
    suspend fun verifyStatsAccess(
        @Path("evento_id") eventoId: Int,
        @Body request: com.example.njoy.model.PasswordVerificationRequest
    ): Response<com.example.njoy.model.StatsAccessToken>

    @GET("evento/{evento_id}/estadisticas")
    suspend fun getEventStats(@Path("evento_id") eventoId: Int): Response<com.example.njoy.model.EventStats>
}
