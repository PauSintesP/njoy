package com.example.njoy

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.njoy.DataClasesApi.PaymentResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryPaymentsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_history_payments)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation(R.id.nav_history)
        initViews()
        setupListeners()
        loadPayments()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewPayments)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyView = findViewById(R.id.tvEmptyPayments)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        swipeRefreshLayout.setOnRefreshListener {
            loadPayments()
        }
    }

    private fun loadPayments() {
        showLoading(true)

        // Obtener ID del usuario actual
        val userId = SessionManager.getUserId(this)
        if (userId <= 0) {
            showEmptyView("Debes iniciar sesión para ver tu historial de pagos")
            swipeRefreshLayout.isRefreshing = false
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener todos los pagos de la API
                val response = ApiClient.getApiService(this@HistoryPaymentsActivity).getPayments()

                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false
                    showLoading(false)

                    if (response.isSuccessful) {
                        val allPayments = response.body() ?: emptyList()

                        // Filtrar por ID de usuario
                        val userPayments = allPayments.filter { payment -> payment.usuario_id == userId }

                        if (userPayments.isEmpty()) {
                            showEmptyView("No tienes pagos registrados")
                        } else {
                            hideEmptyView()
                            displayPayments(userPayments)
                        }
                    } else {
                        showEmptyView("Error al cargar los pagos: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false
                    showLoading(false)
                    showEmptyView("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun displayPayments(payments: List<PaymentResponse>) {
        // Ordenar pagos por fecha (más recientes primero)
        val sortedPayments = payments.sortedByDescending { it.fecha }
        val adapter = PaymentAdapter(sortedPayments)
        recyclerView.adapter = adapter
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            tvEmptyView.visibility = View.GONE
        }
    }

    private fun showEmptyView(message: String) {
        tvEmptyView.text = message
        tvEmptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyView() {
        tvEmptyView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}