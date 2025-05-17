package com.example.njoy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class TicketsActivity : BaseActivity() {

    private lateinit var rvTickets: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoTickets: TextView
    private val TAG = "TicketsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tickets)
        setupBottomNavigation(R.id.nav_tickets)
        rvTickets = findViewById(R.id.rv_tickets)
        progressBar = findViewById(R.id.progressBar_tickets)
        tvNoTickets = findViewById(R.id.tv_no_tickets)
        setupBottomNavigation(R.id.nav_tickets)
        fetchTickets()
    }

    private fun fetchTickets() {
        showLoading(true)

        // Obtener ID del usuario actual
        val userId = SessionManager.getUserId(this)
        Log.d(TAG, "Buscando tickets para usuario ID: $userId")

        if (userId <= 0) {
            showLoading(false)
            showNoTickets("No hay sesión de usuario activa")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ticketsResp = ApiClient.apiService.getTickets()
                val eventosResp = ApiClient.apiService.getEventos()

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (ticketsResp.isSuccessful && eventosResp.isSuccessful) {
                        val allTickets = ticketsResp.body().orEmpty()
                        Log.d(TAG, "Total de tickets recibidos: ${allTickets.size}")

                        // Filtrar tickets por ID de usuario
                        val userTickets = allTickets.filter { it.usuario_id == userId }
                        Log.d(TAG, "Tickets del usuario actual: ${userTickets.size}")

                        val eventos = eventosResp.body().orEmpty()
                        val mapEventos = eventos.associate { it.id to it.nombre }

                        if (userTickets.isEmpty()) {
                            showNoTickets("No tienes entradas todavía")
                        } else {
                            showTickets(userTickets, mapEventos)
                        }
                    } else {
                        Toast.makeText(this@TicketsActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                        showNoTickets("Error al cargar los datos")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "Error al cargar tickets: ${e.message}", e)
                    Toast.makeText(this@TicketsActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    showNoTickets("Error de conexión")
                }
            }
        }
    }

    private fun showTickets(tickets: List<DataClasesApi.TicketResponse>, eventMap: Map<Int, String>) {
        tvNoTickets.visibility = View.GONE
        rvTickets.visibility = View.VISIBLE
        rvTickets.layoutManager = LinearLayoutManager(this)
        rvTickets.adapter = TicketAdapter(tickets, eventMap)
    }

    private fun showNoTickets(message: String) {
        tvNoTickets.visibility = View.VISIBLE
        rvTickets.visibility = View.GONE
        tvNoTickets.text = message
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            rvTickets.visibility = View.GONE
            tvNoTickets.visibility = View.GONE
        }
    }
}