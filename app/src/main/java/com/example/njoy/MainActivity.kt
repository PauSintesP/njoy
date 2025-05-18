package com.example.njoy

import android.app.AlertDialog
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.njoy.DataClasesApi.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private lateinit var recyclerViewEventos: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyView: TextView
    private lateinit var btnEventos: Button
    private lateinit var btnConciertos: Button
    private lateinit var tvCategoryTitle: TextView
    private lateinit var eventAdapter: EventAdapter

    private var allEvents: List<Event> = listOf()
    private var currentCategory: String = "EVENTOS" // Por defecto mostramos todos los eventos

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupBottomNavigation(R.id.nav_Main)
        setupUserProfileClick()
        fetchEventos()
    }

    private fun initViews() {
        recyclerViewEventos = findViewById(R.id.recyclerview_eventos)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyView = findViewById(R.id.tv_empty_view)
        btnEventos = findViewById(R.id.btn_eventos)
        btnConciertos = findViewById(R.id.btn_conciertos)
        tvCategoryTitle = findViewById(R.id.tv_category_title)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupListeners() {
        btnEventos.setOnClickListener {
            currentCategory = "EVENTOS"
            btnEventos.backgroundTintList = getColorStateList(R.color.blue_dark)
            btnConciertos.backgroundTintList = getColorStateList(R.color.green_dark)
            tvCategoryTitle.text = "Eventos"
            filterAndDisplayEvents()
        }

        btnConciertos.setOnClickListener {
            currentCategory = "CONCIERTOS"
            btnConciertos.backgroundTintList = getColorStateList(R.color.blue_dark)
            btnEventos.backgroundTintList = getColorStateList(R.color.green_dark)
            tvCategoryTitle.text = "Conciertos"
            filterAndDisplayEvents()
        }
    }

    private fun fetchEventos() {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getEventos()
                Log.d("MainActivity", "API Response: ${response.body()}")

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val eventos = response.body()
                        if (!eventos.isNullOrEmpty()) {
                            allEvents = eventos
                            filterAndDisplayEvents()
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

    private fun filterAndDisplayEvents() {
        if (allEvents.isEmpty()) {
            showEmptyView("No hay eventos disponibles")
            return
        }

        // Filtrar según la categoría seleccionada
        val filteredEvents = if (currentCategory == "CONCIERTOS") {
            allEvents.filter { it.tipo.equals("concierto", ignoreCase = true) }
        } else {
            allEvents.filter { !it.tipo.equals("concierto", ignoreCase = true) }
        }

        if (filteredEvents.isEmpty()) {
            showEmptyView("No hay ${currentCategory.lowercase()} disponibles")
        } else {
            hideEmptyView()
            setupRecyclerView(filteredEvents)
        }
    }

    private fun setupRecyclerView(eventos: List<Event>) {
        eventAdapter = EventAdapter(eventos, { eventId ->
            val intent = Intent(this, EventActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            startActivity(intent)
        }, { imageView, imageUrl ->
            // Función para cargar imágenes con Glide
            loadImageWithGlide(imageView, imageUrl)
        })
        recyclerViewEventos.layoutManager = GridLayoutManager(this, 2)
        recyclerViewEventos.adapter = eventAdapter
    }

    // Método para cargar imágenes con Glide
    fun loadImageWithGlide(imageView: ImageView, imageUrl: String?) {
        val url = imageUrl.takeIf { !it.isNullOrEmpty() }
            ?: "https://talco-punkchanka.com/wp-content/uploads/2024/06/header.jpg" // Imagen por defecto

        Glide.with(this)
            .load(url)
            .apply(RequestOptions()
                .placeholder(R.drawable.ic_event) // Imagen de carga
                .error(R.drawable.ic_event)) // Imagen de error
            .into(imageView)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
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
    }

    private fun setupUserProfileClick() {
        val userProfileImage = findViewById<ImageView>(R.id.iv_user_profile)
        userProfileImage.setOnClickListener {
            showUserProfileDialog()
        }
    }

    private fun showUserProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_profile, null)

        // Obtener referencias a las vistas del diálogo
        val tvUsername = dialogView.findViewById<TextView>(R.id.tv_dialog_username)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tv_dialog_email)
        val btnLogout = dialogView.findViewById<Button>(R.id.btn_logout)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close)

        // Establecer los datos del usuario
        val username = SessionManager.getUsername(this)
        val email = SessionManager.getEmail(this)
        tvUsername.text = "Usuario: $username"
        tvEmail.text = "Email: $email"

        // Crear el diálogo
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Configurar botón de cierre
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Configurar botón de cerrar sesión
        btnLogout.setOnClickListener {
            // Cerrar sesión
            SessionManager.logout(this)

            // Redireccionar al inicio de sesión
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Mostrar el diálogo
        dialog.show()
    }
}