package com.example.njoy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.text.format

class EventActivity : BaseActivity() {

    private lateinit var ivEventImage: ImageView
    private lateinit var tvEventTitle: TextView
    private lateinit var tvEventDate: TextView
    private lateinit var tvEventLocation: TextView
    private lateinit var tvAvailableSeats: TextView
    private lateinit var tvEventDescription: TextView
    private lateinit var tvEventPrice: TextView
    private lateinit var tvTicketCount: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnDecrease: Button
    private lateinit var btnIncrease: Button
    private lateinit var btnBuyTickets: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEventTime: TextView

    private var eventId = 0
    private lateinit var currentEvent: Event
    private var ticketCount = 1
    private var pricePerTicket = 0.0

    companion object {
        private val CATEGORY_PRICE_MAP = mapOf(
            "PREMIUM" to 40.0,
            "STANDARD" to 20.0,
            "ECONOMIC" to 10.0
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        // Obtener el ID del evento del intent
        eventId = intent.getIntExtra("EVENT_ID", 0)
        if (eventId == 0) {
            Toast.makeText(this, "Error: ID de evento no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        setupBottomNavigation(R.id.nav_Main)
        fetchEventDetails()
    }

    private fun initViews() {
        ivEventImage = findViewById(R.id.ivEventImage)
        tvEventTitle = findViewById(R.id.tvEventTitle)
        tvEventDate = findViewById(R.id.tvEventDate)
        tvEventLocation = findViewById(R.id.tvEventLocation)
        tvAvailableSeats = findViewById(R.id.tvAvailableSeats)
        tvEventDescription = findViewById(R.id.tvEventDescription)
        tvEventPrice = findViewById(R.id.tvEventPrice)
        tvTicketCount = findViewById(R.id.tvTicketCount)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        btnDecrease = findViewById(R.id.btnDecrease)
        btnIncrease = findViewById(R.id.btnIncrease)
        btnBuyTickets = findViewById(R.id.btnBuyTickets)
        progressBar = findViewById(R.id.progressBar)
        tvEventTime = findViewById(R.id.tvEventTime)
    }

    private fun setupListeners() {
        btnDecrease.setOnClickListener {
            if (ticketCount > 1) {
                ticketCount--
                updateTicketCountUI()
            }
        }

        btnIncrease.setOnClickListener {
            if (::currentEvent.isInitialized && ticketCount < currentEvent.plazas) {
                ticketCount++
                updateTicketCountUI()
            } else {
                Toast.makeText(this, "No hay más entradas disponibles", Toast.LENGTH_SHORT).show()
            }
        }

        btnBuyTickets.setOnClickListener {
            if (::currentEvent.isInitialized) {
                if (currentEvent.plazas < ticketCount) {
                    Toast.makeText(this, "No hay suficientes entradas disponibles", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val totalPrice = pricePerTicket * ticketCount
                val intent = Intent(this, PaymentMethodActivity::class.java).apply {
                    putExtra("EVENT_ID", eventId)
                    putExtra("TICKET_COUNT", ticketCount)
                    putExtra("TOTAL_AMOUNT", totalPrice)
                    putExtra("EVENT_NAME", currentEvent.nombre)
                }
                startActivity(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchEventDetails() {
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getEvento(eventId)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful && response.body() != null) {
                        currentEvent = response.body()!!
                        displayEventDetails(currentEvent)
                    } else {
                        Toast.makeText(this@EventActivity,
                            "Error al cargar el evento: ${response.code()}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@EventActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayEventDetails(event: Event) {
        // Cargar imagen
        if (event.imagen.isNotEmpty()) {
            Glide.with(this)
                .load(event.imagen)
                .placeholder(R.drawable.ic_event)
                .error(R.drawable.ic_event)
                .into(ivEventImage)
        }

        // Establecer textos
        tvEventTitle.text = event.nombre

        // Formatear fecha
        // Formatear fecha
        val dateTimeOriginal = event.fechayhora // Formato esperado: "2025-07-15T18:00:00"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Usar API moderna para Android 8.0+
                val dateTime = LocalDateTime.parse(dateTimeOriginal)

                // Formato para fecha: día mes año
                val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                val formattedDate = dateTime.format(dateFormatter)

                // Formato para hora: HH:mm
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val formattedTime = dateTime.format(timeFormatter)

                // Actualizar TextViews
                tvEventDate.text = formattedDate
                tvEventTime.text = formattedTime
            } else {
                // Para versiones anteriores, dividir manualmente
                fallbackDateFormatting(dateTimeOriginal)
            }
        } catch (e: Exception) {
            // Si falla el parsing, intentar método alternativo
            fallbackDateFormatting(dateTimeOriginal)
            Log.e("EventActivity", "Error al formatear fecha: ${e.message}")
        }


        tvEventLocation.text = "${event.recinto}, ${fetchLocalidadName(event.localidad_id)}"
        tvAvailableSeats.text = "Entradas disponibles: ${event.plazas}"
        tvEventDescription.text = event.descripcion

        // Establecer precio basado en la categoría
        pricePerTicket = CATEGORY_PRICE_MAP[event.categoria_precio] ?: 15.0

        val df = DecimalFormat("0.00")
        tvEventPrice.text = "Precio por persona: ${df.format(pricePerTicket)}€"

        updateTicketCountUI()

        // Deshabilitar el botón de incremento si no hay suficientes entradas
        if (event.plazas <= 1) {
            btnIncrease.isEnabled = false
        }

        // Deshabilitar botón de compra si no hay entradas disponibles
        btnBuyTickets.isEnabled = event.plazas > 0
    }

    private fun fetchLocalidadName(localidadId: Int): String {
        // Idealmente debería obtener el nombre de la localidad de la API
        // Por ahora solo devolvemos un valor genérico
        return "Localidad $localidadId"
    }

    private fun updateTicketCountUI() {
        tvTicketCount.text = ticketCount.toString()

        val df = DecimalFormat("0.00")
        val totalPrice = pricePerTicket * ticketCount
        tvTotalPrice.text = "Precio total: ${df.format(totalPrice)}€"

        // Verificar si se puede incrementar más
        if (::currentEvent.isInitialized) {
            btnIncrease.isEnabled = ticketCount < currentEvent.plazas
        }

        // Verificar si se puede decrementar más
        btnDecrease.isEnabled = ticketCount > 1
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnBuyTickets.isEnabled = !show && ::currentEvent.isInitialized && currentEvent.plazas > 0
    }
    private fun fallbackDateFormatting(dateTimeStr: String) {
        try {
            // Dividir por T para separar fecha y hora
            val parts = dateTimeStr.split("T")

            if (parts.size == 2) {
                // Extraer hora (los primeros 5 caracteres: HH:mm)
                val time = parts[1].substring(0, 5)
                tvEventTime.text = time

                // Procesar la fecha (YYYY-MM-DD)
                val dateParts = parts[0].split("-")
                if (dateParts.size == 3) {
                    val year = dateParts[0]
                    val monthNum = dateParts[1].toInt()
                    val day = dateParts[2]

                    // Array de nombres de mes abreviados
                    val months = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

                    // Formato: día mes año (ej: "15 Jul 2025")
                    val monthName = if (monthNum in 1..12) months[monthNum-1] else "???"
                    val formattedDate = "$day $monthName $year"
                    tvEventDate.text = formattedDate
                } else {
                    tvEventDate.text = parts[0]
                }
            } else {
                // Si no hay separador T, mostrar el original
                tvEventDate.text = dateTimeStr
                tvEventTime.text = ""
            }
        } catch (e: Exception) {
            // En caso de cualquier error, mostrar el original
            tvEventDate.text = dateTimeStr
            tvEventTime.text = ""
        }
    }
}