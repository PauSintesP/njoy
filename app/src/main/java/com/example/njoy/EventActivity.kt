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
import com.bumptech.glide.Glide
import com.example.njoy.DataClasesApi.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
            val sold = if (::currentEvent.isInitialized) currentEvent.tickets_vendidos ?: 0 else 0
            val remaining = if (::currentEvent.isInitialized) currentEvent.plazas - sold else 0
            
            if (::currentEvent.isInitialized && ticketCount < remaining) {
                ticketCount++
                updateTicketCountUI()
            } else {
                Toast.makeText(this, "No hay más entradas disponibles", Toast.LENGTH_SHORT).show()
            }
        }

        btnBuyTickets.setOnClickListener {
            if (::currentEvent.isInitialized) {
                val sold = currentEvent.tickets_vendidos ?: 0
                val remaining = currentEvent.plazas - sold
                
                if (remaining < ticketCount) {
                    Toast.makeText(this, "No hay suficientes entradas disponibles", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                showPurchaseConfirmation()
            }
        }
    }

    private fun showPurchaseConfirmation() {
        val totalPrice = pricePerTicket * ticketCount
        val dformat = DecimalFormat("0.00")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Confirmar Compra")
            .setMessage("¿Deseas comprar $ticketCount entradas por ${dformat.format(totalPrice)}€?")
            .setPositiveButton("Comprar") { _, _ ->
                purchaseTickets()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun purchaseTickets() {
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getApiService(this@EventActivity).purchaseTickets(eventId, ticketCount)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@EventActivity, "¡Compra realizada con éxito!", Toast.LENGTH_LONG).show()
                        // Navigate to tickets or finish
                        val intent = Intent(this@EventActivity, TicketsActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                         val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@EventActivity, "Error en la compra: ${response.code()} $errorBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@EventActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchEventDetails() {
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getApiService(this@EventActivity).getEvento(eventId)
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
        if (!event.imagen.isNullOrEmpty()) {
            Glide.with(this)
                .load(event.imagen)
                .placeholder(R.drawable.ic_event)
                .error(R.drawable.ic_event)
                .into(ivEventImage)
        }

        // Establecer textos
        tvEventTitle.text = event.nombre

        // Formatear fecha
        val dateTimeOriginal = event.fechayhora

        try {
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
        } catch (e: Exception) {
            fallbackDateFormatting(dateTimeOriginal)
            Log.e("EventActivity", "Error al formatear fecha: ${e.message}")
        }

        CoroutineScope(Dispatchers.IO).launch {
            val locationId = event.localidad_id
            if (locationId != null) {
                val localidadName = fetchLocalidadName(locationId)
                withContext(Dispatchers.Main) {
                    tvEventLocation.text = getString(R.string.event_location_format, event.recinto, localidadName)
                }
            } else {
                withContext(Dispatchers.Main) {
                    tvEventLocation.text = event.recinto
                }
            }
        }

        val ticketsSold = event.tickets_vendidos ?: 0
        val remaining = event.plazas - ticketsSold
        
        tvAvailableSeats.text = "Quedan $remaining entradas"
        // tvAvailableSeats.text = getString(R.string.available_seats_format, remaining) // Use simple string for now

        tvEventDescription.text = event.descripcion

            // Usar directamente el precio del evento
            pricePerTicket = event.precio ?: 0.0
            Log.d("EventActivity", "Precio obtenido: $pricePerTicket€")

        val dformat = DecimalFormat("0.00")
        tvEventPrice.text = getString(R.string.price_per_person_format, dformat.format(pricePerTicket))

        // Actualizar precio total
        updateTicketCountUI()

        if (remaining <= 1) {
            btnIncrease.isEnabled = false
        }

        if (remaining <= 0) {
             btnBuyTickets.isEnabled = false
             btnBuyTickets.text = "SOLD OUT"
             btnBuyTickets.alpha = 0.5f
        } else {
             btnBuyTickets.isEnabled = true
        }
    }

    private suspend fun fetchLocalidadName(localidadId: Int): String {
        try {
            val response = ApiClient.getApiService(this@EventActivity).getLocalidad(localidadId)
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!.ciudad
            } else {
                Log.e("EventActivity", "Error al obtener localidad: ${response.code()}")
                return "Localidad desconocida"
            }
        } catch (e: Exception) {
            Log.e("EventActivity", "Error en fetchLocalidadName: ${e.message}")
            return "Localidad desconocida"
        }
    }

    private fun updateTicketCountUI() {
        tvTicketCount.text = ticketCount.toString()

        val df = DecimalFormat("0.00")
        val totalPrice = pricePerTicket * ticketCount
        // tvTotalPrice.text = getString(R.string.total_price_format, df.format(totalPrice)) // Old way
        
        // New way: Update button text
        btnBuyTickets.text = "Comprar Entradas - ${df.format(totalPrice)}€"

        if (::currentEvent.isInitialized) {
            btnIncrease.isEnabled = ticketCount < currentEvent.plazas
        }

        btnDecrease.isEnabled = ticketCount > 1
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnBuyTickets.isEnabled = !show && ::currentEvent.isInitialized && currentEvent.plazas > 0
    }
    private fun fallbackDateFormatting(dateTimeStr: String) {
        try {
            val parts = dateTimeStr.split("T")

            if (parts.size == 2) {
                val time = parts[1].substring(0, 5)
                tvEventTime.text = time

                val dateParts = parts[0].split("-")
                if (dateParts.size == 3) {
                    val year = dateParts[0]
                    val monthNum = dateParts[1].toInt()
                    val day = dateParts[2]

                    val months = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

                    val monthName = if (monthNum in 1..12) months[monthNum-1] else "???"
                    val formattedDate = "$day $monthName $year"
                    tvEventDate.text = formattedDate
                } else {
                    tvEventDate.text = parts[0]
                }
            } else {
                tvEventDate.text = dateTimeStr
                tvEventTime.text = ""
            }
        } catch (_: Exception) {
            tvEventDate.text = dateTimeStr
            tvEventTime.text = ""
        }
    }
}