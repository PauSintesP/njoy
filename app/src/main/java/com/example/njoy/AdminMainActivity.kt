package com.example.njoy

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat

class AdminMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user has permission to access this activity
        if (!SessionManager.canManageEvents(this)) {
            // User doesn't have permission, redirect to main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
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

        // Event management - accessible to admin, owner, promotor
        findViewById<CardView>(R.id.cardCreateEvent).setOnClickListener {
            startActivity(Intent(this, EventCreateActivity::class.java))
        }

        findViewById<CardView>(R.id.cardModifyEvent).setOnClickListener {
            startActivity(Intent(this, SelectModifyEventActivity::class.java))
        }

        findViewById<CardView>(R.id.cardDeleteEvent).setOnClickListener {
            startActivity(Intent(this, EventDeleteActivity::class.java))
        }

        // Ticket scanning - accessible to all event managers
        findViewById<CardView>(R.id.cardScanTickets).setOnClickListener {
            startActivity(Intent(this, EscanearActivity::class.java))
        }
    }

    private fun showProfileOptions() {
        val user = SessionManager.getUser(this)
        val userName = user?.let { "${it.nombre} ${it.apellidos}" } ?: "Usuario"
        val userRole = SessionManager.getUserRole(this)
        
        val options = arrayOf("Ver perfil", "Cerrar sesión")

        AlertDialog.Builder(this)
            .setTitle("$userName ($userRole)")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showProfileInfo()
                    1 -> logOut()
                }
            }
            .show()
    }
    
    private fun showProfileInfo() {
        val user = SessionManager.getUser(this)
        user?.let {
            val message = """
                Nombre: ${it.nombre} ${it.apellidos}
                Email: ${it.email}
                Rol: ${it.role}
                Estado: ${if (it.is_active) "Activo" else "Inactivo"}
            """.trimIndent()
            
            AlertDialog.Builder(this)
                .setTitle("Mi Perfil")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun logOut() {
        // Clear session data
        SessionManager.logout(this)

        // Redirect to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}