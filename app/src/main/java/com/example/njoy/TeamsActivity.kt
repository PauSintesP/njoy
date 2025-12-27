package com.example.njoy

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class TeamsActivity : AppCompatActivity() {
    
    private lateinit var rvTeams: RecyclerView
    private lateinit var btnCreateTeam: MaterialButton
    private lateinit var loadingOverlay: View
    private lateinit var emptyState: View
    
    private lateinit var teamsAdapter: TeamsAdapter
    private val teams = mutableListOf<DataClasesApi.Team>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teams)
        
        setupToolbar()
        initViews()
        setupRecyclerView()
        setupListeners()
        loadTeams()
    }
    
    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Mis Equipos"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun initViews() {
        rvTeams = findViewById(R.id.rvTeams)
        btnCreateTeam = findViewById(R.id.btnCreateTeam)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        emptyState = findViewById(R.id.emptyState)
    }
    
    private fun setupRecyclerView() {
        teamsAdapter = TeamsAdapter(
            teams = teams,
            onTeamClick = { team -> navigateToTeamDetails(team) },
            onDeleteClick = { team -> confirmDeleteTeam(team) }
        )
        
        rvTeams.apply {
            layoutManager = LinearLayoutManager(this@TeamsActivity)
            adapter = teamsAdapter
        }
    }
    
    private fun setupListeners() {
        btnCreateTeam.setOnClickListener {
            showCreateTeamDialog()
        }
    }
    
    private fun loadTeams() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@TeamsActivity)
                val response = apiService.getMyTeams()
                
                teams.clear()
                teams.addAll(response)
                teamsAdapter.notifyDataSetChanged()
                
                updateEmptyState()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@TeamsActivity,
                    "Error al cargar equipos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showCreateTeamDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_team, null)
        val etTeamName = dialogView.findViewById<TextInputEditText>(R.id.etTeamName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Crear Equipo")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val teamName = etTeamName.text.toString().trim()
                val description = etDescription.text.toString().trim()
                
                if (teamName.isNotBlank()) {
                    createTeam(teamName, description)
                } else {
                    Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun createTeam(name: String, description: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@TeamsActivity)
                val request = DataClasesApi.CreateTeamRequest(
                    nombre_equipo = name,
                    descripcion = description.ifBlank { null }
                )
                
                val newTeam = apiService.createTeam(request)
                
                teams.add(0, newTeam)
                teamsAdapter.notifyItemInserted(0)
                rvTeams.scrollToPosition(0)
                
                updateEmptyState()
                
                Toast.makeText(
                    this@TeamsActivity,
                    "Equipo creado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@TeamsActivity,
                    "Error al crear equipo: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun confirmDeleteTeam(team: DataClasesApi.Team) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Equipo")
            .setMessage("¿Estás seguro de que deseas eliminar el equipo \"${team.nombre_equipo}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteTeam(team)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun deleteTeam(team: DataClasesApi.Team) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@TeamsActivity)
                apiService.deleteTeam(team.id)
                
                val position = teams.indexOf(team)
                if (position != -1) {
                    teams.removeAt(position)
                    teamsAdapter.notifyItemRemoved(position)
                }
                
                updateEmptyState()
                
                Toast.makeText(
                    this@TeamsActivity,
                    "Equipo eliminado",
                    Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@TeamsActivity,
                    "Error al eliminar equipo: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun navigateToTeamDetails(team: DataClasesApi.Team) {
        // TODO: Implementar TeamDetailsActivity
        Toast.makeText(this, "Detalles de ${team.nombre_equipo}", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateEmptyState() {
        if (teams.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvTeams.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvTeams.visibility = View.VISIBLE
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
