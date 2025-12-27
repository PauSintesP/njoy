package com.example.njoy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProfileActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var btnBack: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var cvRoleBadge: CardView
    private lateinit var tvBirthDate: TextView
    private lateinit var tvCountry: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private lateinit var apiService: ApiService

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        apiService = ApiClient.getApiService(this)

        initViews()
        setupListeners()
        loadUserData()
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        loadingOverlay = findViewById(R.id.loading_overlay)
        btnBack = findViewById(R.id.btn_back)
        tvUserName = findViewById(R.id.tv_user_name)
        tvUserEmail = findViewById(R.id.tv_user_email)
        tvUserRole = findViewById(R.id.tv_user_role)
        cvRoleBadge = findViewById(R.id.cv_role_badge)
        tvBirthDate = findViewById(R.id.tv_birth_date)
        tvCountry = findViewById(R.id.tv_country)
        tvMemberSince = findViewById(R.id.tv_member_since)
        btnEditProfile = findViewById(R.id.btn_edit_profile)
        btnLogout = findViewById(R.id.btn_logout)

        // Configure SwipeRefresh colors
        swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.accent,
            R.color.success
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

        swipeRefresh.setOnRefreshListener {
            loadUserData(isRefresh = true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUserData(isRefresh: Boolean = false) {
        lifecycleScope.launch {
            try {
                // Show loading indicator
                if (!isRefresh) {
                    showLoading(true)
                }

                // Fetch fresh user data from API
                val response = apiService.getCurrentUser()

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!

                    // Update SessionManager with fresh data
                    SessionManager.updateUser(this@ProfileActivity, user)

                    // Display user data
                    displayUserData(user)

                } else {
                    // API call failed, try to load cached data
                    val cachedUser = SessionManager.getUser(this@ProfileActivity)
                    if (cachedUser != null) {
                        displayUserData(cachedUser)
                        Toast.makeText(
                            this@ProfileActivity,
                            "Mostrando datos guardados (sin conexión)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // No cached data, go to login
                        showError("Error al cargar perfil. Por favor, inicia sesión nuevamente.")
                        navigateToLogin()
                    }
                }

            } catch (e: Exception) {
                // Network error, try to load cached data
                val cachedUser = SessionManager.getUser(this@ProfileActivity)
                if (cachedUser != null) {
                    displayUserData(cachedUser)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Sin conexión. Mostrando datos guardados.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showError("Error de conexión. Verifica tu internet.")
                }

            } finally {
                // Hide loading indicators
                showLoading(false)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayUserData(user: DataClasesApi.User) {
        // Nombre completo
        tvUserName.text = "${user.nombre} ${user.apellidos}"

        // Email
        tvUserEmail.text = user.email

        // Rol (en mayúsculas para el badge)
        tvUserRole.text = user.role.uppercase()

        // Configurar color del badge según el rol
        val badgeColor = when (user.role.lowercase()) {
            "admin" -> getColor(R.color.warning) // Dorado/Amarillo
            "promotor" -> getColor(R.color.accent) // Púrpura
            else -> getColor(R.color.primary) // Azul por defecto
        }
        cvRoleBadge.setCardBackgroundColor(badgeColor)

        // Fecha de nacimiento
        tvBirthDate.text = formatDate(user.fecha_nacimiento)

        // País
        tvCountry.text = user.pais ?: "No especificado"

        // Miembro desde
        tvMemberSince.text = formatMemberSince(user.created_at)
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

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
