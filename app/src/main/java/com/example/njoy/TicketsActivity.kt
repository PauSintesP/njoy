package com.example.njoy

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.njoy.DataClasesApi.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnAllTickets: Button
    private lateinit var btnActiveTickets: Button

    private var allTickets = listOf<DataClasesApi.MyTicketResponse>()
    // Event details are now embedded in MyTicketResponse, so map is not needed
    private var currentFilter: String = "ALL" // Filtro actual: ALL o ACTIVE

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tickets)

        initViews()
        setupListeners()
        setupBottomNavigation(R.id.nav_tickets)
        loadTickets()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewTickets)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyView = findViewById(R.id.tvEmptyTickets)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        btnAllTickets = findViewById(R.id.btnAllTickets)
        btnActiveTickets = findViewById(R.id.btnActiveTickets)

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupListeners() {
        swipeRefreshLayout.setOnRefreshListener {
            loadTickets()
        }

        btnAllTickets.setOnClickListener {
            currentFilter = "ALL"
            updateButtonState()
            filterAndDisplayTickets()
        }

        btnActiveTickets.setOnClickListener {
            currentFilter = "ACTIVE"
            updateButtonState()
            filterAndDisplayTickets()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateButtonState() {
        if (currentFilter == "ALL") {
            btnAllTickets.backgroundTintList = getColorStateList(R.color.blue_dark)
            btnActiveTickets.backgroundTintList = getColorStateList(R.color.green_dark)
        } else {
            btnAllTickets.backgroundTintList = getColorStateList(R.color.green_dark)
            btnActiveTickets.backgroundTintList = getColorStateList(R.color.blue_dark)
        }
    }

    private fun loadTickets() {
        showLoading(true)

        val userId = SessionManager.getUserId(this)
        if (userId <= 0) {
            showEmptyView("Debes iniciar sesión para ver tus entradas")
            swipeRefreshLayout.isRefreshing = false
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use getMyTickets to get tickets with embedded event info and CODES
                val ticketsResponse = ApiClient.getApiService(this@TicketsActivity).getMyTickets()

                if (ticketsResponse.isSuccessful) {
                    val loadedTickets = ticketsResponse.body() ?: emptyList()
                    // Filter logic is now server-side implicit (MyTickets returns user tickets)
                    // We just store them
                    this@TicketsActivity.allTickets = loadedTickets

                    withContext(Dispatchers.Main) {
                        swipeRefreshLayout.isRefreshing = false
                        showLoading(false)
                        filterAndDisplayTickets()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        swipeRefreshLayout.isRefreshing = false
                        showLoading(false)
                        showEmptyView("Error al cargar tus entradas: ${ticketsResponse.code()}")
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

    private fun filterAndDisplayTickets() {
        // Aplicar filtro (todos o solo activos)
        val filteredTickets = if (currentFilter == "ACTIVE") {
            allTickets.filter { it.activado }
        } else {
            allTickets
        }

        if (filteredTickets.isEmpty()) {
            val message = when {
                allTickets.isEmpty() -> "No tienes entradas todavía"
                currentFilter == "ACTIVE" -> "No tienes entradas activas"
                else -> "No se encontraron entradas"
            }
            showEmptyView(message)
        } else {
            hideEmptyView()
            // Support simplified adapter (MyTicketResponse list, no map)
            val adapter = TicketAdapter(filteredTickets)
            recyclerView.adapter = adapter
        }
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

    override fun onResume() {
        super.onResume()
        // Actualizar los tickets cuando se vuelve a la actividad
        if (!swipeRefreshLayout.isRefreshing) {
            loadTickets()
        }
    }
}