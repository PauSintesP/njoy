package com.example.njoy

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.njoy.DataClasesApi.Event
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventAdapter(
    private val eventos: List<Event>,
    private val onEventClick: (Int) -> Unit,
    private val imageLoader: (ImageView, String?) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun getItemCount() = eventos.size

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventos[position]
        holder.bind(event)
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.event_image)
        private val titleView: TextView = itemView.findViewById(R.id.event_title)
        private val dateView: TextView = itemView.findViewById(R.id.event_date)
        private val locationView: TextView = itemView.findViewById(R.id.event_location)
        private val priceView: TextView = itemView.findViewById(R.id.event_price)
        private val badgeView: TextView = itemView.findViewById(R.id.event_badge)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(event: Event) {
            titleView.text = event.nombre

            // Formatear fecha y hora al estilo web: "VIE 26 DIC, 19:47"
            try {
                val dateTime = LocalDateTime.parse(event.fechayhora)
                val dayOfWeek = dateTime.format(DateTimeFormatter.ofPattern("EEE", Locale("es", "ES"))).uppercase()
                val day = dateTime.format(DateTimeFormatter.ofPattern("dd"))
                val month = dateTime.format(DateTimeFormatter.ofPattern("MMM", Locale("es", "ES"))).uppercase()
                val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))

                dateView.text = "$dayOfWeek $day $month, $time"
            } catch (e: Exception) {
                val parts = event.fechayhora.split("T")
                val time = if (parts.size > 1) parts[1].substring(0, 5) else "??:??"

                try {
                    val dateParts = parts[0].split("-")
                    if (dateParts.size >= 3) {
                        val day = dateParts[2]
                        val monthNum = dateParts[1].toInt()
                        val months = arrayOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
                            "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
                        val month = if (monthNum in 1..12) months[monthNum-1] else "???"
                        dateView.text = "$day $month, $time"
                    } else {
                        dateView.text = event.fechayhora
                    }
                } catch (e: Exception) {
                    dateView.text = event.fechayhora
                }
            }

            // Ubicación con emoji de pin
            locationView.text = "📍 ${event.recinto}"

            // Mostrar precio
            priceView.text = getPriceLabel(event.precio)

            // Mostrar badge si es un tipo especial de evento
            if (event.tipo.equals("festival", ignoreCase = true)) {
                badgeView.visibility = View.VISIBLE
                badgeView.text = "Festival"
            } else {
                badgeView.visibility = View.GONE
            }

            // Cargar imagen
            imageLoader(imageView, event.imagen)

            itemView.setOnClickListener {
                onEventClick(event.id)
            }
        }

        private fun getPriceLabel(precio: Double?): String {
            val p = precio ?: 0.0
            return if (p > 0) {
                if (p % 1.0 == 0.0) "${p.toInt()}€" else "$p€"
            } else {
                "Consultar"
            }
        }
    }
}