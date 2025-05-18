package com.example.njoy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.njoy.DataClasesApi.Event
import com.example.njoy.DataClasesApi.TicketRequest
import com.example.njoy.DataClasesApi.TicketResponse
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EscanearActivity : AppCompatActivity() {
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "EscanearActivity"
    }

    // Vistas
    private lateinit var eventSelectionLayout: View
    private lateinit var scannerLayout: View
    private lateinit var rvEvents: RecyclerView
    private lateinit var progressBarEvents: ProgressBar
    private lateinit var tvNoEvents: TextView
    private lateinit var btnBack: Button
    private lateinit var previewView: PreviewView
    private lateinit var tvEventName: TextView
    private lateinit var resultPanel: View
    private lateinit var ivResultIcon: ImageView
    private lateinit var tvResultTitle: TextView
    private lateinit var tvResultMessage: TextView
    private lateinit var tvTicketDetails: TextView
    private lateinit var btnScanAgain: Button
    private lateinit var btnChangeEvent: Button
    private lateinit var tvScannerTitle: TextView

    // Variables para la cámara
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    // Variables de estado
    private var selectedEvent: Event? = null
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escanear)

        initViews()
        setupListeners()
        loadEvents()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initViews() {
        // Vistas para selección de evento
        eventSelectionLayout = findViewById(R.id.eventSelectionLayout)
        rvEvents = findViewById(R.id.rvEvents)
        progressBarEvents = findViewById(R.id.progressBarEvents)
        tvNoEvents = findViewById(R.id.tvNoEvents)
        btnBack = findViewById(R.id.btnBack)

        // Vistas para escáner
        scannerLayout = findViewById(R.id.scannerLayout)
        previewView = findViewById(R.id.previewView)
        tvScannerTitle = findViewById(R.id.tvScannerTitle)
        tvEventName = findViewById(R.id.tvEventName)
        resultPanel = findViewById(R.id.resultPanel)
        ivResultIcon = findViewById(R.id.ivResultIcon)
        tvResultTitle = findViewById(R.id.tvResultTitle)
        tvResultMessage = findViewById(R.id.tvResultMessage)
        tvTicketDetails = findViewById(R.id.tvTicketDetails)
        btnScanAgain = findViewById(R.id.btnScanAgain)
        btnChangeEvent = findViewById(R.id.btnChangeEvent)

        // Configurar RecyclerView
        rvEvents.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnChangeEvent.setOnClickListener {
            stopCamera()
            showEventSelection()
        }

        btnScanAgain.setOnClickListener {
            resetScanResults()
        }
    }

    private fun loadEvents() {
        progressBarEvents.visibility = View.VISIBLE
        rvEvents.visibility = View.GONE
        tvNoEvents.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getEventos()

                withContext(Dispatchers.Main) {
                    progressBarEvents.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val eventos = response.body()!!
                        if (eventos.isNotEmpty()) {
                            setupEventAdapter(eventos)
                        } else {
                            showNoEvents("No hay eventos disponibles")
                        }
                    } else {
                        showNoEvents("Error al cargar eventos: ${response.code()}")
                        Log.e(TAG, "Error: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarEvents.visibility = View.GONE
                    showNoEvents("Error de conexión: ${e.message}")
                    Log.e(TAG, "Excepción: ${e.message}", e)
                }
            }
        }
    }

    private fun setupEventAdapter(eventos: List<Event>) {
        val adapter = EventScanAdapter(eventos) { event ->
            selectedEvent = event
            if (checkCameraPermission()) {
                showScanner()
            }
        }

        rvEvents.adapter = adapter
        rvEvents.visibility = View.VISIBLE
    }

    private fun showNoEvents(message: String) {
        tvNoEvents.text = message
        tvNoEvents.visibility = View.VISIBLE
        rvEvents.visibility = View.GONE
    }

    private fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showScanner()
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara para escanear tickets", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEventSelection() {
        eventSelectionLayout.visibility = View.VISIBLE
        scannerLayout.visibility = View.GONE
    }

    private fun showScanner() {
        // Configurar el escáner
        eventSelectionLayout.visibility = View.GONE
        scannerLayout.visibility = View.VISIBLE

        // Mostrar nombre del evento seleccionado
        tvEventName.text = selectedEvent?.nombre ?: "Evento no seleccionado"

        // Configurar mensaje inicial
        resetScanResults()

        // Iniciar la cámara
        startCamera()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Configuración del preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Configuración del analizador de imágenes
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(imageProxy)
                        }
                    }

                // Seleccionar cámara trasera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Vincular los casos de uso a la cámara
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                isScanning = true
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar cámara", e)
                Toast.makeText(this, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanning) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Obtener un escáner de códigos de barras
            val scanner = BarcodeScanning.getClient()

            // Procesar la imagen buscando códigos
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { barcodeValue ->
                            if (isScanning) {
                                isScanning = false
                                processBarcode(barcodeValue)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al procesar código", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processBarcode(barcodeValue: String) {
        Log.d(TAG, "Código escaneado: $barcodeValue")

        runOnUiThread {
            // Mostrar loading
            resultPanel.visibility = View.VISIBLE
            ivResultIcon.setImageResource(android.R.drawable.ic_popup_sync)
            tvResultTitle.text = "Procesando entrada"
            tvResultMessage.text = "Verificando validez del ticket..."
            btnScanAgain.visibility = View.GONE
        }

        // Intentar extraer un ID de ticket
        try {
            val ticketId = extractTicketId(barcodeValue)

            if (ticketId > 0) {
                validateTicket(ticketId)
            } else {
                showInvalidResult(
                    "Formato inválido",
                    "El código escaneado no corresponde a un ticket válido.",
                    "Formato detectado: $barcodeValue"
                )
            }
        } catch (e: Exception) {
            showInvalidResult(
                "Error de procesamiento",
                "No se pudo procesar el código: ${e.message}",
                "Código: $barcodeValue"
            )
        }
    }

    private fun extractTicketId(barcodeValue: String): Int {
        // Patrón esperado: "ticket:123" o similar
        val pattern = Regex("NJOY-TICKET-(\\d+)")
        val matchResult = pattern.find(barcodeValue)

        return matchResult?.groupValues?.getOrNull(1)?.toIntOrNull() ?: -1
    }

    private fun validateTicket(ticketId: Int) {
        val eventoId = selectedEvent?.id ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Obtener el ticket de la API
                val ticketResponse = ApiClient.apiService.getTickets()

                if (!ticketResponse.isSuccessful || ticketResponse.body() == null) {
                    showInvalidResult(
                        "Error de verificación",
                        "No se pudo verificar el ticket: ${ticketResponse.code()}",
                        "ID: $ticketId"
                    )
                    return@launch
                }

                val tickets = ticketResponse.body()!!
                val ticket = tickets.find { it.id == ticketId }

                if (ticket == null) {
                    showInvalidResult(
                        "Ticket no encontrado",
                        "No existe un ticket con ID $ticketId",
                        ""
                    )
                    return@launch
                }

                // 2. Verificar que el ticket corresponde al evento
                if (ticket.evento_id != eventoId) {
                    showInvalidResult(
                        "Ticket no válido",
                        "Este ticket no corresponde a este evento",
                        "ID Ticket: ${ticket.id}, Evento: ${selectedEvent?.nombre}"
                    )
                    return@launch
                }

                // 3. Verificar si el ticket ya fue utilizado
                if (!ticket.activado) {
                    showInvalidResult(
                        "Ticket ya utilizado",
                        "Este ticket ya ha sido escaneado anteriormente",
                        "ID Ticket: ${ticket.id}"
                    )
                    return@launch
                }

                // 4. Activar el ticket
                val ticketRequest = TicketRequest(
                    evento_id = ticket.evento_id,
                    usuario_id = ticket.usuario_id,
                    activado = false
                )

                val updateResponse = ApiClient.apiService.updateTicket(ticketId, ticketRequest)

                if (updateResponse.isSuccessful) {
                    showValidResult(
                        "Ticket válido",
                        "Entrada verificada correctamente",
                        "ID Ticket: ${ticket.id}, Evento: ${selectedEvent?.nombre}"
                    )
                } else {
                    showInvalidResult(
                        "Error de actualización",
                        "No se pudo actualizar el estado del ticket: ${updateResponse.code()}",
                        "ID Ticket: ${ticket.id}"
                    )
                }

            } catch (e: Exception) {
                showInvalidResult(
                    "Error de conexión",
                    "No se pudo verificar el ticket: ${e.message}",
                    ""
                )
                Log.e(TAG, "Error validando ticket", e)
            }
        }
    }

    private fun showValidResult(title: String, message: String, details: String) {
        runOnUiThread {
            resultPanel.visibility = View.VISIBLE
            ivResultIcon.setImageResource(android.R.drawable.ic_dialog_info)
            ivResultIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            tvResultTitle.text = title
            tvResultMessage.text = message
            tvTicketDetails.text = details
            tvTicketDetails.visibility = if (details.isNotEmpty()) View.VISIBLE else View.GONE
            btnScanAgain.visibility = View.GONE

            // Esperar 3 segundos y reiniciar el escaneo automáticamente
            previewView.postDelayed({
                resetScanResults()
            }, 3000)
        }
    }

    private fun showInvalidResult(title: String, message: String, details: String) {
        runOnUiThread {
            resultPanel.visibility = View.VISIBLE
            ivResultIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            ivResultIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            tvResultTitle.text = title
            tvResultMessage.text = message
            tvTicketDetails.text = details
            tvTicketDetails.visibility = if (details.isNotEmpty()) View.VISIBLE else View.GONE
            btnScanAgain.visibility = View.GONE

            // Esperar 3 segundos y reiniciar el escaneo automáticamente
            previewView.postDelayed({
                resetScanResults()
            }, 3000)
        }
    }

    private fun resetScanResults() {
        resultPanel.visibility = View.VISIBLE
        ivResultIcon.setImageResource(android.R.drawable.ic_menu_camera)
        ivResultIcon.colorFilter = null
        tvResultTitle.text = "Listo para escanear"
        tvResultMessage.text = "Acerca un código QR de entrada al visor de la cámara"
        tvTicketDetails.text = ""
        tvTicketDetails.visibility = View.GONE
        btnScanAgain.visibility = View.GONE // Siempre oculto
        isScanning = true
    }

    private fun stopCamera() {
        try {
            isScanning = false
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener la cámara", e)
        }
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // Adaptador para la lista de eventos
    class EventScanAdapter(
        private val events: List<Event>,
        private val onEventClick: (Event) -> Unit
    ) : RecyclerView.Adapter<EventScanAdapter.EventViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event_scan, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = events[position]
            holder.bind(event)
            holder.itemView.setOnClickListener { onEventClick(event) }
        }

        override fun getItemCount() = events.size

        class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
            private val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
            private val ivEventImage: ImageView = itemView.findViewById(R.id.ivEventImage)

            fun bind(event: Event) {
                tvEventName.text = event.nombre

                // Formatear fecha
                val dateTime = event.fechayhora.split("T")
                tvEventDate.text = if (dateTime.size == 2) {
                    "Fecha: ${dateTime[0]} - Hora: ${dateTime[1].substring(0, 5)}"
                } else {
                    event.fechayhora
                }

                // Cargar imagen con Glide
                if (!event.imagen.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(event.imagen)
                        .placeholder(R.drawable.ic_event)
                        .error(R.drawable.ic_event)
                        .into(ivEventImage)
                }
            }
        }
    }
}