package com.example.njoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeamsAdapter(
    private val teams: List<DataClasesApi.Team>,
    private val onTeamClick: (DataClasesApi.Team) -> Unit,
    private val onDeleteClick: (DataClasesApi.Team) -> Unit
) : RecyclerView.Adapter<TeamsAdapter.TeamViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team, parent, false)
        return TeamViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(teams[position])
    }
    
    override fun getItemCount() = teams.size
    
    inner class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTeamName: TextView = itemView.findViewById(R.id.tvTeamName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvMemberCount: TextView = itemView.findViewById(R.id.tvMemberCount)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        fun bind(team: DataClasesApi.Team) {
            tvTeamName.text = team.nombre_equipo
            
            if (team.descripcion.isNullOrBlank()) {
                tvDescription.visibility = View.GONE
            } else {
                tvDescription.visibility = View.VISIBLE
                tvDescription.text = team.descripcion
            }
            
            val memberCount = team.num_miembros ?: 0
            tvMemberCount.text = "$memberCount miembros"
            
            itemView.setOnClickListener {
                onTeamClick(team)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(team)
            }
        }
    }
}
