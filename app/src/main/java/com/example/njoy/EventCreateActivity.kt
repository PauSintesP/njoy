package com.example.njoy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.njoy.DataClasesApi.CreateEventRequest
import com.example.njoy.DataClasesApi.GeneroResponse
import com.example.njoy.DataClasesApi.LocalidadResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EventCreateActivity : AppCompatActivity() {

    // UI components
    private lateinit var etNombreEvento: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var spinnerTipoEvento: Spinner
    private lateinit var spinnerLocalidad: Spinner
    private lateinit var etRecinto: EditText
    private lateinit var etPlazas: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var tvFechaHoraSelected: TextView
    private lateinit var spinnerPrecio: Spinner
    private lateinit var spinnerGenero: Spinner
    private lateinit var etDniOrganizador: EditText
    private lateinit var etImagenUrl: EditText
    private lateinit var btnCreateEvent: Button
    private lateinit var btnBack: Button
    private lateinit var progressBar: ProgressBar

    // Data for spinners
    private val localidades = mutableListOf<LocalidadResponse>()
    private val generos = mutableListOf<GeneroResponse>()

    // Selected date and time
    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var selectedHour = 0
    private var selectedMinute = 0
    private var isDateSelected = false
    private var isTimeSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_create)

        initUI()
        setupSpinners()
        fetchLocalidades()
        fetchGeneros()
        setupListeners()
    }

    private fun initUI() {
        etNombreEvento = findViewById(R.id.etNombreEvento)
        etDescripcion = findViewById(R.id.etDescripcion)
        spinnerTipoEvento = findViewById(R.id.spinnerTipoEvento)
        spinnerLocalidad = findViewById(R.id.spinnerLocalidad)
        etRecinto = findViewById(R.id.etRecinto)
        etPlazas = findViewById(R.id.etPlazas)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        tvFechaHoraSelected = findViewById(R.id.tvFechaHoraSelected)
        spinnerPrecio = findViewById(R.id.spinnerPrecio)
        spinnerGenero = findViewById(R.id.spinnerGenero)
        etDniOrganizador = findViewById(R.id.etDniOrganizador)
        etImagenUrl = findViewById(R.id.etImagenUrl)
        btnCreateEvent = findViewById(R.id.btnCreateEvent)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinners() {
        // Setup tipo evento spinner
        val tipoEventoAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.tipos_evento,
            android.R.layout.simple_spinner_item
        )
        tipoEventoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoEvento.adapter = tipoEventoAdapter

        // Setup precio spinner
        val precioCategoriaAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.categorias_precio,
            android.R.layout.simple_spinner_item
        )
        precioCategoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrecio.adapter = precioCategoriaAdapter

        // Los spinners de localidad y género se configurarán después de obtener los datos
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        btnCreateEvent.setOnClickListener {
            if (validateFields()) {
                createEvent()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            isDateSelected = true
            updateDateTimeText()
        }, currentYear, currentMonth, currentDay).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
            isTimeSelected = true
            updateDateTimeText()
        }, currentHour, currentMinute, true).show()
    }

    private fun updateDateTimeText() {
        if (isDateSelected && isTimeSelected) {
            val dateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            val timeStr = String.format("%02d:%02d:00", selectedHour, selectedMinute)
            tvFechaHoraSelected.text = "$dateStr $timeStr"
        } else if (isDateSelected) {
            val dateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            tvFechaHoraSelected.text = "$dateStr (Hora no seleccionada)"
        } else if (isTimeSelected) {
            val timeStr = String.format("%02d:%02d:00", selectedHour, selectedMinute)
            tvFechaHoraSelected.text = "(Fecha no seleccionada) $timeStr"
        }
    }

    private fun fetchLocalidades() {
        setLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getLocalidades()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        localidades.clear()
                        localidades.addAll(response.body()!!)
                        setupLocalidadesSpinner()
                    } else {
                        showToast("Error al cargar localidades: ${response.code()}")
                    }
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error de conexión: ${e.message}")
                    setLoading(false)
                }
            }
        }
    }

    private fun setupLocalidadesSpinner() {
        val localidadAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            localidades.map { "${it.ciudad} (ID: ${it.id})" }
        )
        localidadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocalidad.adapter = localidadAdapter
    }

    private fun fetchGeneros() {
        setLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getGeneros()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        generos.clear()
                        generos.addAll(response.body()!!)
                        setupGenerosSpinner()
                    } else {
                        showToast("Error al cargar géneros: ${response.code()}")
                    }
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error de conexión: ${e.message}")
                    setLoading(false)
                }
            }
        }
    }

    private fun setupGenerosSpinner() {
        val generoAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            generos.map { "${it.nombre} (ID: ${it.id})" }
        )
        generoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGenero.adapter = generoAdapter
    }

    private fun validateFields(): Boolean {
        var isValid = true

        // Validar que los campos no estén vacíos
        if (etNombreEvento.text.toString().trim().isEmpty()) {
            etNombreEvento.error = "El nombre es obligatorio"
            isValid = false
        }

        if (etDescripcion.text.toString().trim().isEmpty()) {
            etDescripcion.error = "La descripción es obligatoria"
            isValid = false
        }

        if (etRecinto.text.toString().trim().isEmpty()) {
            etRecinto.error = "El recinto es obligatorio"
            isValid = false
        }

        if (etPlazas.text.toString().trim().isEmpty()) {
            etPlazas.error = "Las plazas son obligatorias"
            isValid = false
        }

        if (etDniOrganizador.text.toString().trim().isEmpty()) {
            etDniOrganizador.error = "El DNI es obligatorio"
            isValid = false
        }

        if (!isDateSelected || !isTimeSelected) {
            showToast("Debe seleccionar fecha y hora")
            isValid = false
        }

        return isValid
    }

    private fun createEvent() {
        setLoading(true)

        // Obtener IDs seleccionados
        val localidadId = if (localidades.isNotEmpty()) {
            localidades[spinnerLocalidad.selectedItemPosition].id
        } else {
            1 // Valor por defecto
        }

        val generoId = if (generos.isNotEmpty()) {
            generos[spinnerGenero.selectedItemPosition].id
        } else {
            1 // Valor por defecto
        }

        // Obtener tipo de evento seleccionado
        val tipoEvento = spinnerTipoEvento.selectedItem.toString()

        // Obtener precio seleccionado (quitar el símbolo €)
        val precioTexto = spinnerPrecio.selectedItem.toString()
        val categoriaPrecio = precioTexto.replace("€", "").trim()

        // Crear fecha y hora en formato ISO
        val fechaHora = String.format(
            "%04d-%02d-%02dT%02d:%02d:00",
            selectedYear,
            selectedMonth + 1,
            selectedDay,
            selectedHour,
            selectedMinute
        )

        // Crear objeto de solicitud
        val eventRequest = CreateEventRequest(
            nombre = etNombreEvento.text.toString().trim(),
            descripcion = etDescripcion.text.toString().trim(),
            localidad_id = localidadId,
            recinto = etRecinto.text.toString().trim(),
            plazas = etPlazas.text.toString().toInt(),
            fechayhora = fechaHora,
            tipo = tipoEvento,
            categoria_precio = categoriaPrecio,
            organizador_dni = etDniOrganizador.text.toString().trim(),
            genero_id = generoId,
            imagen = etImagenUrl.text.toString().trim()
        )

        // Enviar solicitud a la API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.createEvento(eventRequest)

                withContext(Dispatchers.Main) {
                    setLoading(false)

                    if (response.isSuccessful) {
                        showToast("¡Evento creado correctamente!")
                        finish() // Volver a la actividad anterior
                    } else {
                        showToast("Error al crear evento: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    showToast("Error: ${e.message}")
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnCreateEvent.isEnabled = !loading
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}