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
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar

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
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val birthDate = etBirthDate.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInputs(username, fullName, email, birthDate, password, confirmPassword)) {
                registerUser(username, fullName, email, birthDate, password)
            }
        }

        // Añadir listener para mostrar el DatePicker al hacer clic en el campo
        etBirthDate.setOnClickListener {
            showDatePickerDialog()
        }

        // También es buena idea hacer el campo no editable directamente
        etBirthDate.isFocusable = false
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        // Si ya hay una fecha en el campo, intentar usarla
        val dateString = etBirthDate.text.toString()
        if (dateString.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(dateString)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // Si hay error de formato, usar fecha actual
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Formatear la fecha seleccionada como YYYY-MM-DD
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                // Actualizar el campo con la fecha formateada
                etBirthDate.setText(formattedDate)
            },
            year,
            month,
            day
        )

        // Establecer fecha máxima como hoy (para no permitir fechas futuras)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun validateInputs(
        username: String,
        fullName: String,
        email: String,
        birthDate: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            etUsername.error = "El nombre de usuario es requerido"
            isValid = false
        } else if (username.length < 3) {
            etUsername.error = "El nombre de usuario debe tener al menos 3 caracteres"
            isValid = false
        }

        if (fullName.isEmpty()) {
            etFullName.error = "El nombre completo es requerido"
            isValid = false
        }

        if (email.isEmpty()) {
            etEmail.error = "El email es requerido"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email inválido"
            isValid = false
        }

        if (birthDate.isEmpty()) {
            etBirthDate.error = "La fecha de nacimiento es requerida"
            isValid = false
        } else if (!isValidDateFormat(birthDate)) {
            etBirthDate.error = "Formato inválido. Usar: YYYY-MM-DD"
            isValid = false
        }

        if (password.isEmpty()) {
            etPassword.error = "La contraseña es requerida"
            isValid = false
        } else if (password.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Confirme su contraseña"
            isValid = false
        } else if (password != confirmPassword) {
            etConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        }

        return isValid
    }

    private fun isValidDateFormat(dateStr: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.isLenient = false
            format.parse(dateStr)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun registerUser(username: String, fullName: String, email: String, birthDate: String, password: String) {
        showLoading(true)

        val registerRequest = RegisterRequest(
            user = username,
            ncompleto = fullName,
            email = email,
            fnacimiento = birthDate,
            contrasena = password
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.register(registerRequest)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        response.body()?.let { userData ->

                            SessionManager.login(
                                this@RegisterActivity,
                                userData.email,
                                userData.id,
                                userData.user
                            )
                            Toast.makeText(this@RegisterActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            startMainActivity()
                        } ?: showError("Error: Respuesta inválida del servidor")
                    } else {
                        when (response.code()) {
                            409 -> showError("Este usuario o email ya está registrado")
                            else -> showError("Error ${response.code()}: ${response.message()}")
                        }
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

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}