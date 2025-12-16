package com.example.njoy

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.njoy.DataClasesApi.LoginRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            etEmail.error = "El email es requerido"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email inválido"
            isValid = false
        }

        if (password.isEmpty()) {
            etPassword.error = "La contraseña es requerida"
            isValid = false
        }

        return isValid
    }

    private fun loginUser(email: String, contrasena: String) {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = LoginRequest(email, contrasena)
                val apiService = ApiClient.getApiService(this@LoginActivity)
                val response = apiService.login(request)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        response.body()?.let { loginData ->
                            // We have the tokens, now get user data
                            fetchUserData(loginData.access_token, loginData.refresh_token)
                        } ?: showError("Error: Respuesta inválida del servidor")
                    } else {
                        val errorMsg = when (response.code()) {
                            401 -> "Credenciales incorrectas"
                            403 -> "Usuario baneado o inactivo"
                            else -> "Error ${response.code()}: ${response.message()}"
                        }
                        showError(errorMsg)
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

    private fun fetchUserData(accessToken: String, refreshToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Temporarily store tokens to make authenticated request
                SessionManager.login(
                    this@LoginActivity,
                    accessToken,
                    refreshToken,
                    DataClasesApi.User(
                        id = 0,
                        nombre = "",
                        apellidos = "",
                        email = "",
                        fecha_nacimiento = "",
                        pais = null,
                        role = "user",
                        is_active = true,
                        is_banned = false,
                        created_at = ""
                    )
                )

                val apiService = ApiClient.getApiService(this@LoginActivity)
                val userResponse = apiService.getCurrentUser()

                withContext(Dispatchers.Main) {
                    if (userResponse.isSuccessful) {
                        userResponse.body()?.let { user ->
                            // Check if user is banned
                            if (user.is_banned) {
                                SessionManager.logout(this@LoginActivity)
                                showError("Tu cuenta ha sido suspendida. Contacta con soporte.")
                                return@withContext
                            }

                            // Store complete user data with tokens
                            SessionManager.login(
                                this@LoginActivity,
                                accessToken,
                                refreshToken,
                                user
                            )

                            Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                            // Navigate based on user role
                            navigateByRole(user.role)
                        } ?: showError("Error obteniendo datos del usuario")
                    } else {
                        SessionManager.logout(this@LoginActivity)
                        showError("Error obteniendo perfil de usuario")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    SessionManager.logout(this@LoginActivity)
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun navigateByRole(role: String) {
        // All users go directly to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnRegister.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}