package com.example.njoy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.njoy.DataClasesApi.Event
import com.example.njoy.DataClasesApi.EventUpdateRequest
import com.example.njoy.DataClasesApi.GeneroResponse
import com.example.njoy.DataClasesApi.LocalidadResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventModifyActivity : AppCompatActivity() {

    private lateinit var etNombreEvento: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var spinnerLocalidad: Spinner
    private lateinit var etRecinto: EditText
    private lateinit var etPlazas: EditText
    private lateinit var spinnerTipoEvento: Spinner
    private lateinit var tvFechaHoraSelected: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var spinnerPrecio: Spinner
    private lateinit var spinnerGenero: Spinner
    private lateinit var etDniOrganizador: EditText
    private lateinit var etImagenUrl: EditText
    private lateinit var btnUpdateEvent: Button
    private lateinit var progressBar: ProgressBar

    private var eventId: Int = 0
    private lateinit var currentEvent: Event

    private val localidades = mutableListOf<LocalidadResponse>()
    private val generos = mutableListOf<GeneroResponse>()

    private val calendar = Calendar.getInstance()
    private var selectedDateTimeString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_create) // Reutilizamos el layout de creación

        eventId = intent.getIntExtra("EVENT_ID", 0)
        if (eventId == 0) {
            Toast.makeText(this, "Error: ID de evento no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupSpinners()
        setupDateTimePickers()
        setupListeners()
        loadEventData()
    }

    private fun initViews() {
        etNombreEvento = findViewById(R.id.etNombreEvento)
        etDescripcion = findViewById(R.id.etDescripcion)
        spinnerLocalidad = findViewById(R.id.spinnerLocalidad)
        etRecinto = findViewById(R.id.etRecinto)
        etPlazas = findViewById(R.id.etPlazas)
        spinnerTipoEvento = findViewById(R.id.spinnerTipoEvento)
        tvFechaHoraSelected = findViewById(R.id.tvFechaHoraSelected)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        spinnerPrecio = findViewById(R.id.spinnerPrecio)
        spinnerGenero = findViewById(R.id.spinnerGenero)
        etDniOrganizador = findViewById(R.id.etDniOrganizador)
        etImagenUrl = findViewById(R.id.etImagenUrl)
        progressBar = findViewById(R.id.progressBar)

        // Cambiar el botón para actualizar en lugar de crear
        btnUpdateEvent = findViewById(R.id.btnCreateEvent)
        btnUpdateEvent.text = "ACTUALIZAR EVENTO"
    }

    private fun setupSpinners() {
        // Configuración del spinner de tipos de evento
        val tiposEvento = arrayOf("Concierto", "Festival", "Teatro", "Charla", "Deportivo", "Exposición", "Otro")
        val adapterTipos = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposEvento)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoEvento.adapter = adapterTipos

        // Configuración del spinner de precios
        val precioCategoriaAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.categorias_precio,
            android.R.layout.simple_spinner_item
        )
        precioCategoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrecio.adapter = precioCategoriaAdapter

        // Cargar localidades y géneros de la API
        fetchLocalidades()
        fetchGeneros()
    }

    private fun fetchLocalidades() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localidadesList = ApiClient.getApiService(this@EventModifyActivity).getLocalidades()
                localidades.clear()
                localidades.addAll(localidadesList)

                withContext(Dispatchers.Main) {
                    val localidadesNombres = localidades.map { it.ciudad }
                    val adapter = ArrayAdapter(this@EventModifyActivity,
                        android.R.layout.simple_spinner_item, localidadesNombres)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerLocalidad.adapter = adapter

                    // Si ya tenemos el evento, seleccionamos la localidad
                    if (::currentEvent.isInitialized) {
                        selectLocalidadInSpinner(currentEvent.localidad_id)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EventModifyActivity,
                        "Error al cargar localidades: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchGeneros() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val generosList = ApiClient.getApiService(this@EventModifyActivity).getGeneros()
                generos.clear()
                generos.addAll(generosList)

                withContext(Dispatchers.Main) {
                    val generosNombres = generos.map { it.nombre }
                    val adapter = ArrayAdapter(this@EventModifyActivity,
                        android.R.layout.simple_spinner_item, generosNombres)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerGenero.adapter = adapter

                    // Si ya tenemos el evento, seleccionamos el género
                    if (::currentEvent.isInitialized) {
                        selectGeneroInSpinner(currentEvent.genero_id)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EventModifyActivity,
                        "Error al cargar géneros: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupDateTimePickers() {
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnSelectTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateSelectedDateTime()
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            updateSelectedDateTime()
        },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true)

        timePickerDialog.show()
    }

    private fun updateSelectedDateTime() {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00", Locale.getDefault())
        selectedDateTimeString = sdf.format(calendar.time)

        val displaySdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        tvFechaHoraSelected.text = displaySdf.format(calendar.time)
    }

    private fun setupListeners() {
        btnUpdateEvent.setOnClickListener {
            if (validateInputs()) {
                updateEvent()
            }
        }
    }

    private fun loadEventData() {
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getApiService(this@EventModifyActivity).getEvento(eventId)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful && response.body() != null) {
                        currentEvent = response.body()!!
                        populateFormWithEventData(currentEvent)
                    } else {
                        Toast.makeText(this@EventModifyActivity,
                            "Error al cargar el evento: ${response.code()}",
                            Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@EventModifyActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun populateFormWithEventData(event: Event) {
        etNombreEvento.setText(event.nombre)
        etDescripcion.setText(event.descripcion)
        etRecinto.setText(event.recinto)
        etPlazas.setText(event.plazas.toString())
        etDniOrganizador.setText(event.organizador_dni ?: "")
        etImagenUrl.setText(event.imagen ?: "")

        // Seleccionar tipo de evento en spinner
        val tiposEventoAdapter = spinnerTipoEvento.adapter as ArrayAdapter<String>
        val tipoEventoPosition = (0 until tiposEventoAdapter.count)
            .firstOrNull { tiposEventoAdapter.getItem(it) == event.tipo }
            ?: 0
        spinnerTipoEvento.setSelection(tipoEventoPosition)

        // Seleccionar categoría de precio
        val categoriasPrecioAdapter = spinnerPrecio.adapter as ArrayAdapter<String>
        val targetPrice = event.precio ?: 0.0
        val categoriaPrecioPosition = (0 until categoriasPrecioAdapter.count)
            .firstOrNull { index ->
                val item = categoriasPrecioAdapter.getItem(index) ?: ""
                val itemPrice = item.replace("€", "").trim().toDoubleOrNull() ?: -1.0
                itemPrice == targetPrice
            } ?: 0
        spinnerPrecio.setSelection(categoriaPrecioPosition)

        // Intentar seleccionar localidad y género
        event.localidad_id?.let { selectLocalidadInSpinner(it) }
        event.genero_id?.let { selectGeneroInSpinner(it) }

        // Configurar fecha y hora
        try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00", Locale.getDefault())
            val date = dateTimeFormat.parse(event.fechayhora)
            if (date != null) {
                calendar.time = date
                updateSelectedDateTime()
            } else {
                tvFechaHoraSelected.text = event.fechayhora
                selectedDateTimeString = event.fechayhora
            }
        } catch (e: Exception) {
            tvFechaHoraSelected.text = event.fechayhora
            selectedDateTimeString = event.fechayhora
        }
    }

    private fun selectLocalidadInSpinner(localidadId: Int?) {
        if (localidades.isNotEmpty() && localidadId != null) {
            val position = localidades.indexOfFirst { it.id == localidadId }
            if (position >= 0) {
                spinnerLocalidad.setSelection(position)
            }
        }
    }

    private fun selectGeneroInSpinner(generoId: Int?) {
        if (generos.isNotEmpty() && generoId != null) {
            val position = generos.indexOfFirst { it.id == generoId }
            if (position >= 0) {
                spinnerGenero.setSelection(position)
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (etNombreEvento.text.isBlank()) {
            etNombreEvento.error = "El nombre es obligatorio"
            return false
        }

        if (etDescripcion.text.isBlank()) {
            etDescripcion.error = "La descripción es obligatoria"
            return false
        }

        if (etRecinto.text.isBlank()) {
            etRecinto.error = "El recinto es obligatorio"
            return false
        }

        if (etPlazas.text.isBlank()) {
            etPlazas.error = "El número de plazas es obligatorio"
            return false
        }

        if (selectedDateTimeString.isBlank()) {
            Toast.makeText(this, "Debe seleccionar fecha y hora", Toast.LENGTH_SHORT).show()
            return false
        }

        if (etDniOrganizador.text.isBlank()) {
            etDniOrganizador.error = "El DNI del organizador es obligatorio"
            return false
        }

        return true
    }

    private fun updateEvent() {
        try {
            val selectedLocalidadPosition = spinnerLocalidad.selectedItemPosition
            val selectedGeneroPosition = spinnerGenero.selectedItemPosition

            if (selectedLocalidadPosition < 0 || selectedLocalidadPosition >= localidades.size) {
                Toast.makeText(this, "Debe seleccionar una localidad", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedGeneroPosition < 0 || selectedGeneroPosition >= generos.size) {
                Toast.makeText(this, "Debe seleccionar un género", Toast.LENGTH_SHORT).show()
                return
            }

            val localidadId = localidades[selectedLocalidadPosition].id
            val generoId = generos[selectedGeneroPosition].id

            // Obtener precio seleccionado y eliminar el símbolo €
            val precioTexto = spinnerPrecio.selectedItem.toString()
            val precioVal = precioTexto.replace("€", "").trim().toDoubleOrNull() ?: 0.0

            val updateRequest = EventUpdateRequest(
                nombre = etNombreEvento.text.toString(),
                descripcion = etDescripcion.text.toString(),
                localidad_id = localidadId,
                recinto = etRecinto.text.toString(),
                plazas = etPlazas.text.toString().toInt(),
                fechayhora = selectedDateTimeString,
                tipo = spinnerTipoEvento.selectedItem.toString(),
                precio = precioVal,
                organizador_dni = etDniOrganizador.text.toString(),
                genero_id = generoId,
                imagen = etImagenUrl.text.toString()
            )

            showLoading(true)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.getApiService(this@EventModifyActivity).updateEvento(eventId, updateRequest)

                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        if (response.isSuccessful) {
                            Toast.makeText(this@EventModifyActivity,
                                "Evento actualizado correctamente",
                                Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@EventModifyActivity,
                                "Error al actualizar el evento: ${response.code()}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(this@EventModifyActivity,
                            "Error de conexión: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error al preparar la actualización: ${e.message}",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnUpdateEvent.isEnabled = !show
    }
}