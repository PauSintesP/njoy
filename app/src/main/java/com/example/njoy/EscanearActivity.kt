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
import com.google.mlkit.vision.barcode.BarcodeScanning
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

    private lateinit var eventSelectionLayout: View
    private lateinit var scannerLayout: View
    private lateinit var rvEvents: RecyclerView
    private lateinit var progressBarEvents: ProgressBar
    private lateinit var tvNoEvents: TextView
    private lateinit var btnBack: Button
    private lateinit var previewView: PreviewView
    private lateinit var tvEventName: TextView
    private lateinit var overlayFeedback: View
    private lateinit var ivOverlayIcon: ImageView
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayMessage: TextView
    private lateinit var btnChangeEvent: Button
    
    // Variables de control
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var isScanning = false
    private var selectedEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escanear)

        // Keep screen on while scanning to avoid interruptions
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
        tvEventName = findViewById(R.id.tvEventName)
        
        // Nuevo Overlay Feedback
        overlayFeedback = findViewById(R.id.overlayFeedback)
        ivOverlayIcon = findViewById(R.id.ivOverlayIcon)
        tvOverlayTitle = findViewById(R.id.tvOverlayTitle)
        tvOverlayMessage = findViewById(R.id.tvOverlayMessage)
        
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
        
        overlayFeedback.setOnClickListener {
            // Permitir cerrar el overlay manualmente al tocar
            resetScanResults()
        }
    }

    // --- MÉTODOS DE VALIDACIÓN RECUPERADOS ---

    private fun extractTicketId(barcodeValue: String): Int {
        return try {
            if (barcodeValue.startsWith("NJOY-TICKET-")) {
                barcodeValue.substringAfter("NJOY-TICKET-").toInt()
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting ID", e)
            -1
        }
    }

    private fun validateTicket(ticketId: Int) {
        Log.d(TAG, "Validating by ID: $ticketId")
        // Enviar el ID directamente al servidor para activar
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Llamar al endpoint de ACTIVACIÓN (Valida y marca como usado)
                val response = ApiClient.getApiService(this@EscanearActivity).activateTicket(ticketId)
                Log.d(TAG, "ActivateTicket Response Code: ${response.code()}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val scanResult = response.body()!!
                        Log.d(TAG, "ActivateTicket Success Body: $scanResult")

                        if (scanResult.success) {
                            // Ticket Válido (Verde)
                            showValidResult(
                                scanResult.message,
                                "Asistente: ${scanResult.user_name ?: "Desconocido"}",
                                "Evento: ${scanResult.event_name ?: "Desconocido"}"
                            )
                        } else {
                            // Ticket Inválido o Ya Usado (Rojo)
                            showInvalidResult(
                                scanResult.message,
                                "ID: $ticketId",
                                if (scanResult.user_name != null) "Asistente: ${scanResult.user_name}" else ""
                            )
                        }
                    } else {
                        // Error del servidor (403 Forbidden si no es scanner, 404 si no existe)
                        Log.e(TAG, "ActivateTicket Error Body: ${response.errorBody()?.string()}")
                        val errorMsg = when (response.code()) {
                            403 -> "No tienes permisos de Scanner"
                            404 -> "Ticket no encontrado en BD (ID: $ticketId)"
                            else -> "Error del servidor: ${response.code()}"
                        }
                        showInvalidResult(
                            "Error",
                            errorMsg,
                            "ID: $ticketId"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ActivateTicket Exception", e)
                withContext(Dispatchers.Main) {
                    showInvalidResult(
                        "Error de conexión",
                        "Error: ${e.message}",
                        ""
                    )
                }
            }
        }
    }

    private fun validateByCode(code: String) {
        Log.d(TAG, "Validating by CODE: '$code'")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Remove potential whitespace/newlines
                var cleanCode = code.trim()
                
                // Intentar parsear JSON si parece un objeto JSON
                if (cleanCode.startsWith("{") && cleanCode.endsWith("}")) {
                    try {
                         val jsonObject = org.json.JSONObject(cleanCode)
                         if (jsonObject.has("codigo")) {
                             cleanCode = jsonObject.getString("codigo")
                             Log.d(TAG, "Extracted code from JSON: '$cleanCode'")
                         }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing JSON QR", e)
                        // Fallback: usar el código original (tal vez no era JSON válido)
                    }
                }

                Log.d(TAG, "Final Code to Send: '$cleanCode'")
                
                // Primero obtenemos información del ticket por código
                val response = ApiClient.getApiService(this@EscanearActivity).scanTicket(cleanCode)
                Log.d(TAG, "ScanTicket Response Code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val scanResult = response.body()!!
                    Log.d(TAG, "ScanTicket Success Body: $scanResult")
                    
                    val ticketId = scanResult.ticket?.id ?: scanResult.ticket_id
                    Log.d(TAG, "Resolved Ticket ID: $ticketId")

                    if (scanResult.success) {
                         // Ticket Válido y YA ACTIVADO por el backend en este paso.
                         // NO llamar a validateTicket(id) porque daría error de "ya usado".
                         withContext(Dispatchers.Main) {
                            showValidResult(
                                scanResult.message,
                                "Asistente: ${scanResult.user_name ?: "Desconocido"}",
                                "Evento: ${scanResult.event_name ?: "Desconocido"}"
                            )
                         }
                    } else {
                        // Si ya es inválido o no activo, mostramos el error directamente
                         withContext(Dispatchers.Main) {
                            showInvalidResult(
                                scanResult.message,
                                "Code: $cleanCode",
                                "Evento/User: ${scanResult.event_name} / ${scanResult.user_name}"
                            )
                         }
                    }
                } else {
                    Log.e(TAG, "ScanTicket Error Body: ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        showInvalidResult("Ticket no encontrado", "Código desconocido: $cleanCode", "Error: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                 Log.e(TAG, "ScanTicket Exception", e)
                 withContext(Dispatchers.Main) {
                    showInvalidResult("Error Conexión", e.message ?: "Unknown", "")
                 }
            }
        }
    }

    private fun processBarcode(barcodeValue: String) {
        Log.d(TAG, "Código escaneado: $barcodeValue")

        // 1. Intentar extraer ID del string "NJOY-TICKET-{ID}"
        val id = extractTicketId(barcodeValue)

        if (id != -1) {
            validateTicket(id)
        } else {
             // 2. Si no es formato ID, intentar validar como código alfanumérico (ej. TQ4XMS...)
             validateByCode(barcodeValue)
        }
    }

    // ...

    private fun showValidResult(title: String, message: String, details: String) {
        runOnUiThread {
            overlayFeedback.visibility = View.VISIBLE
            overlayFeedback.setBackgroundColor(android.graphics.Color.parseColor("#CC00C853")) // Green semi-transparent
            
            ivOverlayIcon.setImageResource(R.drawable.ic_check_circle)
            tvOverlayTitle.text = "ENTRADA VÁLIDA"
            tvOverlayMessage.text = "$message\n$details"
            
            // Auto hide after 2.5 seconds
            previewView.postDelayed({
                resetScanResults()
            }, 2500)
        }
    }

    private fun showInvalidResult(title: String, message: String, details: String) {
        runOnUiThread {
            overlayFeedback.visibility = View.VISIBLE
            overlayFeedback.setBackgroundColor(android.graphics.Color.parseColor("#CCD50000")) // Red semi-transparent
            
            ivOverlayIcon.setImageResource(android.R.drawable.ic_delete)
            tvOverlayTitle.text = "ENTRADA INVÁLIDA"
            tvOverlayMessage.text = "$message\n$details"

            // Auto hide after 3 seconds
            previewView.postDelayed({
                resetScanResults()
            }, 3000)
        }
    }

    private fun resetScanResults() {
        overlayFeedback.visibility = View.GONE
        isScanning = true
    }

    private fun loadEvents() {
        progressBarEvents.visibility = View.VISIBLE
        rvEvents.visibility = View.GONE
        tvNoEvents.visibility = View.GONE

    CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch both own events (as creator) and team events (as scanner member)
                val api = ApiClient.getApiService(this@EscanearActivity)
                
                // 1. Get My Events (Creator)
                val myEventsResponse = try {
                    api.getEventsMine()
                } catch (e: Exception) {
                    null
                }
                
                // 2. Get Team Events (Member)
                val teamEventsResponse = try {
                    api.getTeamEvents()
                } catch (e: Exception) {
                    null
                }

                withContext(Dispatchers.Main) {
                    progressBarEvents.visibility = View.GONE

                    val finalEvents = mutableListOf<Event>()
                    
                    if (myEventsResponse?.isSuccessful == true && myEventsResponse.body() != null) {
                         finalEvents.addAll(myEventsResponse.body()!!)
                    }
                    
                    if (teamEventsResponse?.isSuccessful == true && teamEventsResponse.body() != null) {
                        // Avoid duplicates
                        val teamEvents = teamEventsResponse.body()!!
                        val existingIds = finalEvents.map { it.id }.toSet()
                        finalEvents.addAll(teamEvents.filter { it.id !in existingIds })
                    }

                    if (finalEvents.isNotEmpty()) {
                        setupEventAdapter(finalEvents)
                    } else {
                        showNoEvents("No tienes eventos asignados para escanear")
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

                // Cargar imagen con Glide - usar safe call para nullable
                if (!event.imagen.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(event.imagen)
                        .placeholder(R.drawable.ic_event)
                        .error(R.drawable.ic_event)
                        .into(ivEventImage)
                } else {
                    ivEventImage.setImageResource(R.drawable.ic_event)
                }
            }
        }
    }

}