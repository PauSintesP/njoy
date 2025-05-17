package com.example.njoy

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private lateinit var recyclerViewPopulares: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerViewPopulares = findViewById(R.id.recyclerview_populares)
        progressBar = findViewById(R.id.progressBar)
        setupBottomNavigation(R.id.nav_Main)
        setupUserProfileClick()
        fetchEventos()
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
                            setupRecyclerView(eventos)
                        } else {
                            showError("No se encontraron eventos.")
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
            val intent = Intent(this, EventActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            startActivity(intent)
        }, { imageView, imageUrl ->
            // Función para cargar imágenes con Glide
            loadImageWithGlide(imageView, imageUrl)
        })
        recyclerViewPopulares.layoutManager = GridLayoutManager(this, 2)
        recyclerViewPopulares.adapter = eventAdapter
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