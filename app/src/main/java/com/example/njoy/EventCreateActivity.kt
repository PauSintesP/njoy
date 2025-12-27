package com.example.njoy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.njoy.DataClasesApi.CreateEventRequest
import com.example.njoy.DataClasesApi.GeneroResponse
import com.example.njoy.DataClasesApi.LocalidadResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventCreateActivity : AppCompatActivity() {

    // Views
    private lateinit var etNombreEvento: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etRecinto: TextInputEditText
    private lateinit var etPlazas: TextInputEditText
    private lateinit var etImagenUrl: TextInputEditText
    private lateinit var etDniOrganizador: android.widget.EditText // Is it EditText or TextInputEditText? XML says EditText (line 218 in Step 112)
    
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var tvFechaHoraSelected: TextView
    
    private lateinit var spinnerTipoEvento: Spinner
    private lateinit var etLocalidad: TextInputEditText
    private lateinit var etGenero: TextInputEditText
    private lateinit var etPrecio: TextInputEditText
    
    private lateinit var btnCreateEvent: Button
    private lateinit var btnBack: Button
    private lateinit var progressBar: ProgressBar

    // No longer need these lists

    // Date and time selection variables
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
        setupListeners()
    }

    private fun initUI() {
        etNombreEvento = findViewById(R.id.etNombreEvento)
        etDescripcion = findViewById(R.id.etDescripcion)
        spinnerTipoEvento = findViewById(R.id.spinnerTipoEvento)
        etLocalidad = findViewById(R.id.etLocalidad)
        etRecinto = findViewById(R.id.etRecinto)
        etPlazas = findViewById(R.id.etPlazas)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        tvFechaHoraSelected = findViewById(R.id.tvFechaHoraSelected)
        etPrecio = findViewById(R.id.etPrecio)
        etGenero = findViewById(R.id.etGenero)
        etDniOrganizador = findViewById(R.id.etDniOrganizador)
        etImagenUrl = findViewById(R.id.etImagenUrl)
        btnCreateEvent = findViewById(R.id.btnCreateEvent)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinners() {
        val tipoEventoAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.tipos_evento,
            android.R.layout.simple_spinner_item
        )
        tipoEventoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoEvento.adapter = tipoEventoAdapter
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

    private fun validateFields(): Boolean {
        var isValid = true

        if (etNombreEvento.text.toString().trim().isEmpty()) {
            etNombreEvento.error = "El nombre es obligatorio"
            isValid = false
        }

        if (etDescripcion.text.toString().trim().isEmpty()) {
            etDescripcion.error = "La descripción es obligatoria"
            isValid = false
        }

        // All other fields are now optional
        return isValid
    }

    private fun createEvent() {
        setLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(this@EventCreateActivity)
                
                // Auto-create or get location if specified
                var localidadId: Int? = null
                val localidadText = etLocalidad.text.toString().trim()
                if (localidadText.isNotBlank()) {
                    try {
                        val localidad = apiService.createOrGetLocalidad(localidadText)
                        localidadId = localidad.id
                    } catch (e: Exception) {
                        Log.e("EventCreate", "Error creating/getting location", e)
                    }
                }
                
                // Auto-create or get genre if specified
                var generoId: Int? = null
                val generoText = etGenero.text.toString().trim()
                if (generoText.isNotBlank()) {
                    try {
                        val genero = apiService.createOrGetGenero(generoText)
                        generoId = genero.id
                    } catch (e: Exception) {
                        Log.e("EventCreate", "Error creating/getting genre", e)
                    }
                }

                // Get tipo de evento
                val tipoEvento = spinnerTipoEvento.selectedItem.toString()
                
                // Get precio from text field
                val precioVal = etPrecio.text.toString().trim().toDoubleOrNull()

                // Build fecha y hora if selected
                val fechaHora = if (isDateSelected && isTimeSelected) {
                    String.format(
                        "%04d-%02d-%02dT%02d:%02d:00",
                        selectedYear,
                        selectedMonth + 1,
                        selectedDay,
                        selectedHour,
                        selectedMinute
                    )
                } else {
                    null
                }

                val eventRequest = CreateEventRequest(
                    nombre = etNombreEvento.text.toString().trim(),
                    descripcion = etDescripcion.text.toString().trim(),
                    localidad_id = localidadId,
                    recinto = etRecinto.text.toString().trim().takeIf { it.isNotBlank() },
                    plazas = etPlazas.text.toString().toIntOrNull(),
                    fechayhora = fechaHora,
                    tipo = tipoEvento,
                    precio = precioVal,
                    organizador_dni = etDniOrganizador.text.toString().trim().takeIf { it.isNotBlank() },
                    genero_id = generoId,
                    imagen = etImagenUrl.text.toString().trim().takeIf { it.isNotBlank() }
                )

                val response = apiService.createEvento(eventRequest)

                withContext(Dispatchers.Main) {
                    setLoading(false)

                    if (response.isSuccessful) {
                        showToast("¡Evento creado correctamente!")
                        finish()
                    } else {
                        showToast("Error al crear evento: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Log.e("EventCreate", "Error creating event: ${e.message}", e)
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