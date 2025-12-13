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

    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
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
        etFirstName = findViewById(R.id.etUsername)  // Reutilizamos este ID para nombre
        etLastName = findViewById(R.id.etFullName)  // Reutilizamos para apellidos
        etEmail = findViewById(R.id.etEmail)
        etBirthDate = findViewById(R.id.etBirthDate)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val birthDate = etBirthDate.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInputs(firstName, lastName, email, birthDate, password, confirmPassword)) {
                registerUser(firstName, lastName, email, birthDate, password)
            }
        }

        // Date picker
        etBirthDate.setOnClickListener {
            showDatePickerDialog()
        }
        etBirthDate.isFocusable = false
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val dateString = etBirthDate.text.toString()
        if (dateString.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(dateString)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // Usar fecha actual
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                etBirthDate.setText(formattedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun validateInputs(
        firstName: String,
        lastName: String,
        email: String,
        birthDate: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (firstName.isEmpty()) {
            etFirstName.error = "El nombre es requerido"
            isValid = false
        }

        if (lastName.isEmpty()) {
            etLastName.error = "Los apellidos son requeridos"
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

    private fun registerUser(
        nombre: String,
        apellidos: String,
        email: String,
        fechaNacimiento: String,
        password: String
    ) {
        showLoading(true)

        val registerRequest = RegisterRequest(
            nombre = nombre,
            apellidos = apellidos,
            email = email,
            fecha_nacimiento = fechaNacimiento,
            pais = null,  // Campo opcional, enviamos null
            contrasena = password,
            role = "user"  // Default role
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(this@RegisterActivity)
                val response = apiService.register(registerRequest)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        response.body()?.let { userData ->
                            Toast.makeText(this@RegisterActivity, "Registro exitoso. Por favor, inicia sesión.", Toast.LENGTH_SHORT).show()
                            // Redirigir al login
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } ?: showError("Error: Respuesta inválida del servidor")
                    } else {
                        when (response.code()) {
                            409 -> showError("Este email ya está registrado")
                            422 -> showError("Datos inválidos. Verifica los campos")
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
}