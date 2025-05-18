package com.example.njoy

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.njoy.DataClasesApi.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventDeleteActivity : AppCompatActivity() {

    private lateinit var recyclerViewEventos: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyView: TextView
    private lateinit var btnBack: Button
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_delete)

        initViews()
        setupListeners()
        fetchEventos()
    }

    private fun initViews() {
        recyclerViewEventos = findViewById(R.id.recyclerview_eventos)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyView = findViewById(R.id.tvEmptyView)
        btnBack = findViewById(R.id.btnBack)

        // Configurar el RecyclerView con un LinearLayout
        recyclerViewEventos.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun fetchEventos() {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getEventos()

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val eventos = response.body()
                        if (!eventos.isNullOrEmpty()) {
                            setupRecyclerView(eventos)
                        } else {
                            showEmptyView("No se encontraron eventos.")
                        }
                    } else {
                        showError("Error al cargar los eventos: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun setupRecyclerView(eventos: List<Event>) {
        eventAdapter = EventAdapter(eventos, { eventId ->
            // Mostrar un diálogo para confirmar la eliminación
            showDeleteConfirmationDialog(eventId)
        }, { imageView, imageUrl ->
            // Función para cargar imágenes (igual que en MainActivity)
            Glide.with(this)
                .load(imageUrl.takeIf { !it.isNullOrEmpty() }
                    ?: "https://talco-punkchanka.com/wp-content/uploads/2024/06/header.jpg")
                .placeholder(R.drawable.ic_event)
                .error(R.drawable.ic_event)
                .into(imageView)
        })
        recyclerViewEventos.adapter = eventAdapter
        hideEmptyView()
    }

    private fun showDeleteConfirmationDialog(eventId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar evento")
            .setMessage("¿Estás seguro de que deseas eliminar este evento?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteEvent(eventId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteEvent(eventId: Int) {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.deleteEvento(eventId)

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    when (response.code()) {
                        200, 204 -> {
                            Toast.makeText(this@EventDeleteActivity,
                                "Evento eliminado correctamente", Toast.LENGTH_SHORT).show()
                            // Recargar la lista de eventos
                            fetchEventos()
                        }
                        307 -> {
                            // Intentar obtener la URL de redirección
                            val redirectUrl = response.headers().get("Location")
                            showError("Error 307 - Redirección temporal. URL: $redirectUrl")
                            Log.e("EventDeleteActivity", "Redirección 307 a: $redirectUrl")
                        }
                        else -> {
                            showError("Error al eliminar el evento: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerViewEventos.visibility = View.GONE
            tvEmptyView.visibility = View.GONE
        }
    }

    private fun showEmptyView(message: String) {
        tvEmptyView.text = message
        tvEmptyView.visibility = View.VISIBLE
        recyclerViewEventos.visibility = View.GONE
    }

    private fun hideEmptyView() {
        tvEmptyView.visibility = View.GONE
        recyclerViewEventos.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("EventDeleteActivity", message)
    }
}