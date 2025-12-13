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

    private lateinit var radioVisa: android.widget.RadioButton
    private lateinit var radioMastercard: android.widget.RadioButton
    private lateinit var radioPaypal: android.widget.RadioButton

    private fun initViews() {
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        cardVisa = findViewById(R.id.cardVisa)
        cardMastercard = findViewById(R.id.cardMastercard)
        cardPaypal = findViewById(R.id.cardPaypal)
        
        radioVisa = findViewById(R.id.radioVisa)
        radioMastercard = findViewById(R.id.radioMastercard)
        radioPaypal = findViewById(R.id.radioPaypal)
        
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
            selectPaymentMethod("VISA", cardVisa, radioVisa, 
                listOf(Pair(cardMastercard, radioMastercard), Pair(cardPaypal, radioPaypal)))
        }

        cardMastercard.setOnClickListener {
            selectPaymentMethod("MASTERCARD", cardMastercard, radioMastercard,
                 listOf(Pair(cardVisa, radioVisa), Pair(cardPaypal, radioPaypal)))
        }

        cardPaypal.setOnClickListener {
            selectPaymentMethod("PAYPAL", cardPaypal, radioPaypal,
                listOf(Pair(cardVisa, radioVisa), Pair(cardMastercard, radioMastercard)))
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
    private fun selectPaymentMethod(
        method: String, 
        selectedCard: CardView, 
        selectedRadio: android.widget.RadioButton,
        others: List<Pair<CardView, android.widget.RadioButton>>
    ) {
        selectedPaymentMethod = method
        
        // Select logic using state_selected in drawable
        selectedCard.isSelected = true
        selectedRadio.isChecked = true
        
        others.forEach { (card, radio) -> 
            card.isSelected = false 
            radio.isChecked = false
        }
        
        btnConfirmPayment.isEnabled = true
    }

    private fun fetchEventDetails() {
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getApiService(this@PaymentMethodActivity).getEvento(eventId)
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

        showPaymentProcessingDialog()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Paso 1: Usar el endpoint de compra (purchaseTickets) que genera códigos y gestiona plazas
                val purchaseResponse = ApiClient.getApiService(this@PaymentMethodActivity).purchaseTickets(
                    eventId = eventId,
                    quantity = ticketCount
                )

                if (!purchaseResponse.isSuccessful || purchaseResponse.body() == null) {
                    withContext(Dispatchers.Main) {
                        dismissPaymentDialog()
                        // Mostrar error específico si es 400 (Plazas agotadas)
                        val errorMsg = if (purchaseResponse.code() == 400) {
                            "No hay suficientes plazas disponibles"
                        } else {
                            "Error al realizar la compra: ${purchaseResponse.code()}"
                        }
                        Toast.makeText(this@PaymentMethodActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val purchaseResult = purchaseResponse.body()!!
                val createdTickets = purchaseResult.tickets

                if (createdTickets.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        dismissPaymentDialog()
                        Toast.makeText(this@PaymentMethodActivity, "Error: La compra no retornó tickets", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Paso 2: Registrar el pago utilizando el ID del primer ticket
                val firstTicketId = createdTickets[0].id
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                val paymentRequest = PaymentRequest(
                    usuario_id = userId,
                    metodo_pago = selectedPaymentMethod,
                    total = totalAmount,
                    fecha = currentDate,
                    ticket_id = firstTicketId
                )

                val paymentResponse = ApiClient.getApiService(this@PaymentMethodActivity).registerPayment(paymentRequest)

                withContext(Dispatchers.Main) {
                    if (paymentResponse.isSuccessful) {
                        // Pago registrado con éxito
                        updatePaymentDialogSuccess()
                    } else {
                        // Compra exitosa pero falló registro de pago (caso borde)
                        Toast.makeText(this@PaymentMethodActivity, 
                            "Compra realizada, pero error al registrar pago: ${paymentResponse.code()}", 
                            Toast.LENGTH_LONG).show()
                        updatePaymentDialogSuccess() // Permitimos ver los tickets igual
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