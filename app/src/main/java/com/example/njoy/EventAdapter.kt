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
        private val dayView: TextView = itemView.findViewById(R.id.event_day)
        private val monthView: TextView = itemView.findViewById(R.id.event_month)
        private val timeView: TextView = itemView.findViewById(R.id.event_time)
        private val locationView: TextView = itemView.findViewById(R.id.event_location)
        private val availableSeatsView: TextView = itemView.findViewById(R.id.event_available_seats)
        private val priceView: TextView = itemView.findViewById(R.id.event_price)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(event: Event) {
            titleView.text = event.nombre

            // Formatear fecha y hora
            try {
                val dateTime = LocalDateTime.parse(event.fechayhora)
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val dayFormatter = DateTimeFormatter.ofPattern("dd")
                val monthFormatter = DateTimeFormatter.ofPattern("MMM")

                // Solo mostrar la hora
                timeView.text = dateTime.format(timeFormatter)

                dayView.text = dateTime.format(dayFormatter)
                monthView.text = dateTime.format(monthFormatter).uppercase()

            } catch (e: Exception) {
                val parts = event.fechayhora.split("T")
                timeView.text = if (parts.size > 1) parts[1].substring(0, 5) else "??:??"

                // Intentar extraer día y mes del formato YYYY-MM-DD
                try {
                    val dateParts = parts[0].split("-")
                    if (dateParts.size >= 3) {
                        dayView.text = dateParts[2] // Día

                        // Convertir número de mes a abreviatura
                        val monthNum = dateParts[1].toInt()
                        val months = arrayOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
                            "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
                        monthView.text = if (monthNum in 1..12) months[monthNum-1] else "???"
                    } else {
                        dayView.text = "??"
                        monthView.text = "???"
                    }
                } catch (e: Exception) {
                    dayView.text = "??"
                    monthView.text = "???"
                }
            }

            locationView.text = "Ubicación: ${event.recinto}"
            availableSeatsView.text = "Entradas Disponibles: ${event.plazas}"

            // Mostrar precio real, analizando si es un número o categoría
            priceView.text = getPriceLabel(event.categoria_precio)

            // Cargar imagen
            imageLoader(imageView, event.imagen)

            itemView.setOnClickListener {
                onEventClick(event.id)
            }
        }

        private fun getPriceLabel(categoriaPrecio: String): String {
            return try {
                // Intentar convertir directamente a número
                val precio = categoriaPrecio.toDouble()
                "${precio.toInt()}€"
            } catch (e: NumberFormatException) {
                // Si no es un número, usar la lógica anterior de categorías
                when (categoriaPrecio.lowercase()) {
                    "bajo" -> "10€"
                    "medio" -> "20€"
                    "alto" -> "30€"
                    else -> "Consultar"
                }
            }
        }
    }
}