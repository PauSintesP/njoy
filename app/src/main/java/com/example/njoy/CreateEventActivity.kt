package com.example.njoy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : AppCompatActivity() {
    
    private lateinit var etNombre: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etLocalidad: TextInputEditText
    private lateinit var etRecinto: TextInputEditText
    private lateinit var etPlazas: TextInputEditText
    private lateinit var etPrecio: TextInputEditText
    private lateinit var etTipo: TextInputEditText
    private lateinit var etGenero: TextInputEditText
    private lateinit var etImagen: TextInputEditText
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var btnSelectTime: MaterialButton
    private lateinit var spinnerGenero: Spinner
    private lateinit var spinnerLocalidad: Spinner
    private lateinit var llTeamsContainer: LinearLayout
    private lateinit var btnCreateEvent: MaterialButton
    private lateinit var loadingOverlay: View
    
    private val calendar = Calendar.getInstance()
    private var selectedDateTime: String? = null
    
    private val teams = mutableListOf<DataClasesApi.Team>()
    private val selectedTeams = mutableListOf<Int>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)
        
        setupToolbar()
        initViews()
        setupListeners()
        loadData()
    }
    
    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Crear Evento"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun initViews() {
        etNombre = findViewById(R.id.etNombre)
        etDescripcion = findViewById(R.id.etDescripcion)
        etLocalidad = findViewById(R.id.etLocalidad)
        etRecinto = findViewById(R.id.etRecinto)
        etPlazas = findViewById(R.id.etPlazas)
        etPrecio = findViewById(R.id.etPrecio)
        etTipo = findViewById(R.id.etTipo)
        etGenero = findViewById(R.id.etGenero)
        etImagen = findViewById(R.id.etImagen)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        llTeamsContainer = findViewById(R.id.llTeamsContainer)
        btnCreateEvent = findViewById(R.id.btnCreateEvent)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        
        // Ensure loading is hidden initially
        loadingOverlay.visibility = View.GONE
    }
    
    private fun setupListeners() {
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }
        
        btnSelectTime.setOnClickListener {
            showTimePicker()
        }
        
        btnCreateEvent.setOnClickListener {
            createEvent()
        }
    }
    
    private fun loadData() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@CreateEventActivity)
                
                // Load teams only (no longer need to load genres/locations)
                val teamsResponse = apiService.getMyTeams()
                
                teams.clear()
                teams.addAll(teamsResponse)
                
                populateTeamsSelector()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@CreateEventActivity,
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    
    private fun populateTeamsSelector() {
        llTeamsContainer.removeAllViews()
        
        if (teams.isEmpty()) {
            val tvNoTeams = TextView(this)
            tvNoTeams.text = "No tienes equipos. Crea uno primero."
            tvNoTeams.setTextColor(getColor(R.color.text_muted))
            tvNoTeams.textSize = 14f
            llTeamsContainer.addView(tvNoTeams)
            return
        }
        
        teams.forEach { team ->
            val checkBox = CheckBox(this)
            checkBox.text = "${team.nombre_equipo} (${team.num_miembros ?: 0} miembros)"
            checkBox.setTextColor(getColor(R.color.text_main))
            checkBox.isChecked = selectedTeams.contains(team.id)
            
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (!selectedTeams.contains(team.id)) {
                        selectedTeams.add(team.id)
                    }
                } else {
                    selectedTeams.remove(team.id)
                }
            }
            
            llTeamsContainer.addView(checkBox)
        }
    }
    
    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTime()
                btnSelectDate.text = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                updateDateTime()
                btnSelectTime.text = String.format("%02d:%02d", hourOfDay, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun updateDateTime() {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        selectedDateTime = sdf.format(calendar.time)
    }
    
    private fun createEvent() {
        val nombre = etNombre.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        
        if (nombre.isBlank() || descripcion.isBlank()) {
            Toast.makeText(this, "Nombre y descripción son requeridos", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@CreateEventActivity)
                
                // Auto-create or get location if specified
                var localidadId: Int? = null
                val localidadText = etLocalidad.text.toString().trim()
                if (localidadText.isNotBlank()) {
                    try {
                        val localidad = apiService.createOrGetLocalidad(localidadText)
                        localidadId = localidad.id
                    } catch (e: Exception) {
                        Log.e("CreateEvent", "Error creating/getting location", e)
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
                        Log.e("CreateEvent", "Error creating/getting genre", e)
                    }
                }
                
                // Build event data
                val eventData = DataClasesApi.CreateEventRequest(
                    nombre = nombre,
                    descripcion = descripcion,
                    recinto = etRecinto.text.toString().takeIf { it.isNotBlank() },
                    plazas = etPlazas.text.toString().toIntOrNull(),
                    fechayhora = selectedDateTime,
                    tipo = etTipo.text.toString().takeIf { it.isNotBlank() },
                    precio = etPrecio.text.toString().toDoubleOrNull(),
                    genero_id = generoId,
                    localidad_id = localidadId,
                    imagen = etImagen.text.toString().takeIf { it.isNotBlank() },
                    organizador_dni = null
                )
                
                // Create event with selected teams
                val event = apiService.createEventWithTeams(eventData, selectedTeams)
                
                Toast.makeText(
                    this@CreateEventActivity,
                    "Evento creado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                
                finish()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CreateEvent", "Error creating event: ${e.message}", e)
                Toast.makeText(
                    this@CreateEventActivity,
                    "Error al crear evento: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
