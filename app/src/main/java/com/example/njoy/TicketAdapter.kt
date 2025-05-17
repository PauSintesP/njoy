package com.example.njoy

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.njoy.DataClasesApi.TicketResponse

class TicketAdapter(
    private val tickets: List<TicketResponse>,
    private val eventNames: Map<Int, String>
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = tickets[position]
        val nombreEvento = eventNames[ticket.evento_id] ?: "Evento #${ticket.evento_id}"
        holder.bind(ticket, nombreEvento)
    }

    override fun getItemCount(): Int = tickets.size

    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTicketId: TextView = itemView.findViewById(R.id.tvTicketId)
        private val tvEventName: TextView = itemView.findViewById(R.id.tvEventId) // reutilizamos esta vista
        private val tvActivated: TextView = itemView.findViewById(R.id.tvActivated)

        @RequiresApi(Build.VERSION_CODES.M)
        fun bind(ticket: TicketResponse, nombreEvento: String) {
            tvTicketId.text = "Ticket ID: ${ticket.id}"
            tvEventName.text = nombreEvento
            tvActivated.text = if (ticket.activado) "Activado" else "Desactivado"
            tvActivated.setTextColor(
                if (ticket.activado) itemView.context.getColor(R.color.green)
                else itemView.context.getColor(R.color.black)
            )
        }
    }
}