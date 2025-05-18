package com.example.njoy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.example.njoy.DataClasesApi.PaymentRequest
import com.example.njoy.DataClasesApi.TicketRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.ImageView
import com.example.njoy.DataClasesApi.Event

class PaymentMethodActivity : BaseActivity() {

    private lateinit var tvTotalAmount: TextView
    private lateinit var cardVisa: CardView
    private lateinit var cardMastercard: CardView
    private lateinit var cardPaypal: CardView
    private lateinit var btnConfirmPayment: Button
    private lateinit var progressBar: ProgressBar

    private var totalAmount: Double = 0.0
    private var eventId: Int = -1
    private var ticketCount: Int = 1
    private var selectedPaymentMethod: String = ""
    private lateinit var currentEvent: Event

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_method)
        setupBottomNavigation(R.id.nav_Main)

        initViews()
        getIntentData()
        setupListeners()
        fetchEventDetails()
    }

    private fun initViews() {
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        cardVisa = findViewById(R.id.cardVisa)
        cardMastercard = findViewById(R.id.cardMastercard)
        cardPaypal = findViewById(R.id.cardPaypal)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun getIntentData() {
        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
        eventId = intent.getIntExtra("EVENT_ID", -1)
        ticketCount = intent.getIntExtra("TICKET_COUNT", 1)

        tvTotalAmount.text = "Total a pagar: ${String.format("%.2f", totalAmount)}€"

        if (eventId < 0) {
            Toast.makeText(this, "Error: Evento no válido", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupListeners() {

        cardVisa.setOnClickListener {
            selectPaymentMethod("VISA", cardVisa, listOf(cardMastercard, cardPaypal))
        }

        cardMastercard.setOnClickListener {
            selectPaymentMethod("MASTERCARD", cardMastercard, listOf(cardVisa, cardPaypal))
        }

        cardPaypal.setOnClickListener {
            selectPaymentMethod("PAYPAL", cardPaypal, listOf(cardVisa, cardMastercard))
        }

        btnConfirmPayment.setOnClickListener {
            if (selectedPaymentMethod.isEmpty()) {
                Toast.makeText(this, "Selecciona un método de pago", Toast.LENGTH_SHORT).show()
            } else {
                processPayment()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun selectPaymentMethod(method: String, selected: CardView, others: List<CardView>) {
        selectedPaymentMethod = method
        selected.setCardBackgroundColor(getColor(R.color.green))
        others.forEach { it.setCardBackgroundColor(getColor(android.R.color.white)) }
        btnConfirmPayment.isEnabled = true
    }

    private fun fetchEventDetails() {
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getEvento(eventId)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful && response.body() != null) {
                        currentEvent = response.body()!!
                        // Ahora tenemos todos los detalles del evento
                    } else {
                        Toast.makeText(this@PaymentMethodActivity, "Error al cargar detalles del evento", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@PaymentMethodActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun processPayment() {
        val userId = SessionManager.getUserId(this)
        if (userId <= 0) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si hay suficientes plazas disponibles
        if (currentEvent.plazas < ticketCount) {
            Toast.makeText(this, "Lo sentimos, solo quedan ${currentEvent.plazas} entradas disponibles", Toast.LENGTH_LONG).show()
            return
        }

        showPaymentProcessingDialog()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Lista para almacenar los IDs de los tickets creados
                val ticketIds = mutableListOf<Int>()

                // Paso 1: Crear los tickets según la cantidad solicitada
                for (i in 1..ticketCount) {
                    val ticketRequest = TicketRequest(
                        evento_id = eventId,
                        usuario_id = userId,
                        activado = true
                    )

                    val ticketResponse = ApiClient.apiService.createTicket(ticketRequest)

                    if (!ticketResponse.isSuccessful || ticketResponse.body() == null) {
                        withContext(Dispatchers.Main) {
                            dismissPaymentDialog()
                            Toast.makeText(this@PaymentMethodActivity,
                                "Error al crear el ticket ${i}: ${ticketResponse.code()}",
                                Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Añadir el ID del ticket creado a la lista
                    ticketResponse.body()?.id?.let { ticketIds.add(it) }
                }

                // Paso 2: Registrar un único pago para todos los tickets
                // Utilizamos el ID del primer ticket para el registro de pago
                if (ticketIds.isNotEmpty()) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val currentDate = dateFormat.format(Date())

                    // Crear un solo pago vinculado al primer ticket
                    val paymentRequest = PaymentRequest(
                        usuario_id = userId,
                        metodo_pago = selectedPaymentMethod,
                        total = totalAmount, // El monto total para todas las entradas
                        fecha = currentDate,
                        ticket_id = ticketIds[0] // Vinculamos con el primer ticket
                    )

                    val paymentResponse = ApiClient.apiService.registerPayment(paymentRequest)

                    if (!paymentResponse.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            dismissPaymentDialog()
                            Toast.makeText(this@PaymentMethodActivity,
                                "Error en el proceso de pago: ${paymentResponse.code()}",
                                Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        dismissPaymentDialog()
                        Toast.makeText(this@PaymentMethodActivity,
                            "Error: No se pudieron crear tickets",
                            Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Paso 3: Actualizar plazas disponibles del evento
                val updatedEvent = DataClasesApi.EventUpdateRequest.fromEvent(currentEvent).copy(
                    plazas = currentEvent.plazas - ticketCount
                )

                val eventUpdateResponse = ApiClient.apiService.updateEvento(eventId, updatedEvent)

                withContext(Dispatchers.Main) {
                    if (eventUpdateResponse.isSuccessful) {
                        // Pago y actualización exitosos
                        updatePaymentDialogSuccess()
                    } else {
                        // El pago fue exitoso pero falló la actualización de plazas
                        Toast.makeText(this@PaymentMethodActivity,
                            "Pago realizado pero hubo un error al actualizar plazas disponibles",
                            Toast.LENGTH_LONG).show()
                        updatePaymentDialogSuccess()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dismissPaymentDialog()
                    Toast.makeText(this@PaymentMethodActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var paymentDialog: AlertDialog? = null

    private fun showPaymentProcessingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_processing, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        paymentDialog = builder.create()
        paymentDialog?.show()
    }

    private fun updatePaymentDialogSuccess() {
        paymentDialog?.let { dialog ->
            val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBarPayment)
            val statusText = dialog.findViewById<TextView>(R.id.tvPaymentStatus)
            val successIcon = dialog.findViewById<ImageView>(R.id.ivPaymentSuccess)
            val btnViewTickets = dialog.findViewById<Button>(R.id.btnViewTickets)

            progressBar?.visibility = View.GONE
            successIcon?.visibility = View.VISIBLE
            statusText?.text = "¡Pago realizado con éxito!"
            btnViewTickets?.visibility = View.VISIBLE

            btnViewTickets?.setOnClickListener {
                val intent = Intent(this, TicketsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }
    }

    private fun dismissPaymentDialog() {
        paymentDialog?.dismiss()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnConfirmPayment.isEnabled = !show && selectedPaymentMethod.isNotEmpty()
    }
}