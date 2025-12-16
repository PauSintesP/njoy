package com.example.njoy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var tvCountry: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        loadUserData()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvUserName = findViewById(R.id.tv_user_name)
        tvUserEmail = findViewById(R.id.tv_user_email)
        tvUserRole = findViewById(R.id.tv_user_role)
        tvBirthDate = findViewById(R.id.tv_birth_date)
        tvCountry = findViewById(R.id.tv_country)
        tvMemberSince = findViewById(R.id.tv_member_since)
        btnEditProfile = findViewById(R.id.btn_edit_profile)
        btnLogout = findViewById(R.id.btn_logout)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUserData() {
        val user = SessionManager.getUser(this)
        
        if (user != null) {
            // Nombre completo
            tvUserName.text = "${user.nombre} ${user.apellidos}"
            
            // Email
            tvUserEmail.text = user.email
            
            // Rol (en mayúsculas para el badge)
            tvUserRole.text = user.role.uppercase()
            
            // Fecha de nacimiento
            tvBirthDate.text = formatDate(user.fecha_nacimiento)
            
            // País
            tvCountry.text = user.pais ?: "No especificado"
            
            // Miembro desde
            tvMemberSince.text = formatMemberSince(user.created_at)
        } else {
            // Si no hay usuario, volver al login
            navigateToLogin()
        }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "No especificado"
        
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                "${parts[2]}/${parts[1]}/${parts[0]}"
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatMemberSince(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrEmpty()) return "No disponible"
        
        return try {
            val dateTime = LocalDateTime.parse(dateTimeStr)
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
            dateTime.format(formatter).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            // Fallback: intentar extraer año y mes
            try {
                val parts = dateTimeStr.split("-")
                if (parts.size >= 2) {
                    val year = parts[0]
                    val monthNum = parts[1].toInt()
                    val months = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
                    val monthName = if (monthNum in 1..12) months[monthNum-1] else "???"
                    "$monthName $year"
                } else {
                    dateTimeStr
                }
            } catch (e2: Exception) {
                dateTimeStr
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnEditProfile.setOnClickListener {
            // TODO: Implementar edición de perfil
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        SessionManager.logout(this)
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
