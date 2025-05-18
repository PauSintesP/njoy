package com.example.njoy

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AdminMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
    }

    private fun setupListeners() {

        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            showProfileOptions()
        }

        findViewById<CardView>(R.id.cardCreateEvent).setOnClickListener {
            startActivity(Intent(this, EventCreateActivity::class.java))
        }

        findViewById<CardView>(R.id.cardModifyEvent).setOnClickListener {
            startActivity(Intent(this, SelectModifyEventActivity::class.java))
        }

        findViewById<CardView>(R.id.cardDeleteEvent).setOnClickListener {
            startActivity(Intent(this, EventDeleteActivity::class.java))
        }

        findViewById<CardView>(R.id.cardScanTickets).setOnClickListener {
            startActivity(Intent(this, EscanearActivity::class.java))
        }
    }

    private fun showProfileOptions() {
        val options = arrayOf("Cerrar sesión")

        AlertDialog.Builder(this)
            .setTitle("Opciones de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> logOut()
                }
            }
            .show()
    }

    private fun logOut() {
        // Limpiar datos de sesión
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // Redirigir al login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}