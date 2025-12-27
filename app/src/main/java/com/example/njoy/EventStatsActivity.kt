package com.example.njoy

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EventStatsActivity : AppCompatActivity() {
    
    private lateinit var tvTotalTickets: TextView
    private lateinit var tvTicketsSold: TextView
    private lateinit var tvTicketsAvailable: TextView
    private lateinit var tvRevenue: TextView
    private lateinit var tvAttendees: TextView
    private lateinit var pieChart: PieChart
    private lateinit var loadingOverlay: View
    private lateinit var statsContainer: View
    
    private var eventoId: Int = -1
    private var verified = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_stats)
        
        eventoId = intent.getIntExtra("evento_id", -1)
        
        if (eventoId == -1) {
            Toast.makeText(this, "Error: ID de evento inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupToolbar()
        initViews()
        showPasswordDialog()
    }
    
    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Estadísticas del Evento"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun initViews() {
        tvTotalTickets = findViewById(R.id.tvTotalTickets)
        tvTicketsSold = findViewById(R.id.tvTicketsSold)
        tvTicketsAvailable = findViewById(R.id.tvTicketsAvailable)
        tvRevenue = findViewById(R.id.tvRevenue)
        tvAttendees = findViewById(R.id.tvAttendees)
        pieChart = findViewById(R.id.pieChart)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        statsContainer = findViewById(R.id.statsContainer)
        
        statsContainer.visibility = View.GONE
    }
    
    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_verification, null)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.etPassword)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Verificación Requerida")
            .setMessage("Introduce la contraseña del evento para ver las estadísticas")
            .setView(dialogView)
            .setPositiveButton("Verificar") { _, _ ->
                val password = etPassword.text.toString()
                if (password.isNotBlank()) {
                    verifyPassword(password)
                } else {
                    Toast.makeText(this, "La contraseña es requerida", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun verifyPassword(password: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@EventStatsActivity)
                val request = com.example.njoy.model.PasswordVerificationRequest(password)
                val response = apiService.verifyStatsAccess(eventoId, request)
                
                if (response.isSuccessful && response.body()?.access == true) {
                    verified = true
                    loadStats()
                } else {
                    Toast.makeText(
                        this@EventStatsActivity,
                        "Contraseña incorrecta",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@EventStatsActivity,
                    "Error al verificar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun loadStats() {
        if (!verified) return
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@EventStatsActivity)
                val response = apiService.getEventStats(eventoId)
                
                if (response.isSuccessful) {
                    val stats = response.body()
                    stats?.let { displayStats(it) }
                } else {
                    Toast.makeText(
                        this@EventStatsActivity,
                        "Error al cargar estadísticas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@EventStatsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun displayStats(stats: com.example.njoy.model.EventStats) {
        statsContainer.visibility = View.VISIBLE
        
        // Display stats in TextViews
        tvTotalTickets.text = stats.total_tickets.toString()
        tvTicketsSold.text = stats.tickets_sold.toString()
        tvTicketsAvailable.text = stats.tickets_available.toString()
        tvRevenue.text = String.format("%.2f €", stats.total_revenue)
        tvAttendees.text = stats.attendees.toString()
        
        // Setup pie chart
        setupPieChart(stats)
    }
    
    private fun setupPieChart(stats: com.example.njoy.model.EventStats) {
        val entries = ArrayList<PieEntry>()
        
        entries.add(PieEntry(stats.tickets_sold.toFloat(), "Vendidos"))
        entries.add(PieEntry(stats.tickets_available.toFloat(), "Disponibles"))
        
        val dataSet = PieDataSet(entries, "Tickets")
        dataSet.colors = listOf(
            Color.parseColor("#8b5cf6"), // Primary
            Color.parseColor("#ec4899")  // Accent
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f
        
        val data = PieData(dataSet)
        
        pieChart.apply {
            setData(data)
            description.isEnabled = false
            legend.textColor = Color.WHITE
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            animateY(1000)
            invalidate()
        }
    }
    
    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
