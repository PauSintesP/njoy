package com.example.njoy

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.njoy.DataClasesApi.RegisterRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etBirthDate: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etBirthDate = findViewById(R.id.etBirthDate)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        etBirthDate.setOnClickListener {
            showDatePickerDialog()
        }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val birthDate = etBirthDate.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInputs(username, fullName, email, birthDate, password)) {
                registerUser(username, fullName, email, birthDate, password)
            }
        }
    }

    private fun showDatePickerDialog() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateBirthDateField()
        }

        DatePickerDialog(
            this, dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateBirthDateField() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Formato esperado por el servidor
        etBirthDate.setText(sdf.format(calendar.time))
    }

    private fun validateInputs(
        username: String,
        fullName: String,
        email: String,
        birthDate: String,
        password: String
    ): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            etUsername.error = "Nombre de usuario requerido"
            isValid = false
        }

        if (fullName.isEmpty()) {
            etFullName.error = "Nombre completo requerido"
            isValid = false
        }

        if (email.isEmpty()) {
            etEmail.error = "Email requerido"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email inválido"
            isValid = false
        }

        if (birthDate.isEmpty()) {
            etBirthDate.error = "Fecha de nacimiento requerida"
            isValid = false
        }

        if (password.isEmpty()) {
            etPassword.error = "Contraseña requerida"
            isValid = false
        } else if (password.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        }

        return isValid
    }

    private fun registerUser(
        username: String,
        fullName: String,
        email: String,
        birthDate: String,
        contrasena: String
    ) {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = RegisterRequest(username, fullName, email, birthDate, contrasena)
                val response = ApiClient.apiService.register(request)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        response.body()?.let {
                            // Guardar la sesión del usuario recién registrado
                            val userId = it.user_id ?: 0
                            if (userId > 0) {
                                SessionManager.login(this@RegisterActivity, email, userId, fullName)

                                // Redirigir directamente a MainActivity
                                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                showError("No se pudo obtener ID de usuario")
                            }
                        } ?: showError("Respuesta inválida del servidor")
                    } else {
                        showError("Error ${response.code()}: ${response.message()}")
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

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}