package com.example.njoy

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.njoy.DataClasesApi.EventoDetail
import com.example.njoy.DataClasesApi.MyTicketResponse
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TicketAdapter(
    private val tickets: List<MyTicketResponse>
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = tickets[position]
        holder.bind(ticket) { ticketResponse, event ->
            // Implementación para descargar PDF
            Toast.makeText(holder.itemView.context,
                "Descargando entrada para ${event?.nombre ?: "Evento #${ticketResponse.ticket_id}"}",
                Toast.LENGTH_SHORT).show()
            // Aquí iría el código real para generar y descargar el PDF
        }
    }

    override fun getItemCount(): Int = tickets.size

    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a las vistas
        private val tvTicketCode: TextView = itemView.findViewById(R.id.tvTicketCode)
        private val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvVenue: TextView = itemView.findViewById(R.id.tvVenue)
        private val tvTicketType: TextView = itemView.findViewById(R.id.tvTicketType)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val ivEventImage: ImageView = itemView.findViewById(R.id.ivEventImage)
        private val ivQrCode: ImageView = itemView.findViewById(R.id.ivQrCode)
        private val btnDownloadPdf: Button? = itemView.findViewById(R.id.btnDownloadPdf)
        private val btnAddToWallet: Button? = itemView.findViewById(R.id.btnAddToWallet)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(ticket: MyTicketResponse, onPdfDownloadClick: (MyTicketResponse, EventoDetail?) -> Unit) {
            val evento = ticket.evento

            // Código del ticket: Preferir "codigo" (alfanumérico), fallback a ID
            val displayCode = ticket.codigo ?: "NJ-${ticket.ticket_id}"
            tvTicketCode.text = displayCode

            // Nombre del evento
            tvEventName.text = evento?.nombre ?: "Evento desconocido"

            // Estado del ticket
            tvTicketType.text = if (ticket.activado) "Entrada Activada" else "Entrada Desactivada"

            // Lugar del evento
            tvVenue.text = evento?.recinto ?: "Lugar por confirmar"

            val p = evento?.precio ?: 0.0
            val precio = if (p > 0) {
                 if (p % 1.0 == 0.0) {
                     "${p.toInt()} €"
                 } else {
                     "$p €"
                 }
            } else {
                "Consultar"
            }
            tvPrice.text = precio

            // Fecha y hora del evento
            if (evento?.fechayhora != null) {
                formatDateTime(evento.fechayhora, tvDate, tvTime)
            } else {
                tvDate.text = "Fecha pendiente"
                tvTime.text = "-- : --"
            }

            // Imagen del evento
            if (!evento?.imagen.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(evento?.imagen)
                    .placeholder(R.drawable.ic_event)
                    .error(R.drawable.ic_event)
                    .into(ivEventImage)
            } else {
                ivEventImage.setImageResource(R.drawable.ic_event)
            }

            // Generar código QR
            // Si tenemos código alfanumérico, usarlo. Si no, usar ID.
            val qrContent = ticket.codigo ?: "NJOY-TICKET-${ticket.ticket_id}"
            generateQRCode(qrContent)

            // Configurar el botón de descarga si existe
            btnDownloadPdf?.setOnClickListener {
                onPdfDownloadClick(ticket, evento)
            }
            
            // Configurar el botón de Google Wallet (placeholder por ahora)
            btnAddToWallet?.setOnClickListener {
                Toast.makeText(itemView.context, 
                    "Función Google Wallet en desarrollo", 
                    Toast.LENGTH_SHORT).show()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun formatDateTime(dateTimeStr: String, dateView: TextView, timeView: TextView) {
            try {
                val dateTime = LocalDateTime.parse(dateTimeStr)
                val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

                dateView.text = dateTime.format(dateFormatter)
                timeView.text = dateTime.format(timeFormatter)
            } catch (e: Exception) {
                // Fallback para versiones anteriores o formato incorrecto
                val parts = dateTimeStr.split("T")
                if (parts.size == 2) {
                    timeView.text = parts[1].substring(0, 5)

                    val dateParts = parts[0].split("-")
                    if (dateParts.size == 3) {
                        val year = dateParts[0]
                        val monthNum = dateParts[1].toInt()
                        val day = dateParts[2]

                        val months = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                        val monthName = if (monthNum in 1..12) months[monthNum-1] else "???"
                        dateView.text = "$day $monthName $year"
                    } else {
                        dateView.text = parts[0]
                    }
                } else {
                    dateView.text = "Fecha pendiente"
                    timeView.text = "-- : --"
                }
            }
        }

        private fun generateQRCode(content: String) {
            try {
                // OPTIMIZED: Simple QR code containing only the ticket code
                val multiFormatWriter = MultiFormatWriter()
                val bitMatrix = multiFormatWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    512,
                    512
                )
                val barcodeEncoder = BarcodeEncoder()
                val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
                ivQrCode.setImageBitmap(bitmap)

                // Configurar el clic en el QR para mostrar ampliado
                ivQrCode.setOnClickListener {
                    showExpandedQRCode(bitmap, itemView.context)
                }
            } catch (e: Exception) {
                // En caso de error, mostrar icono básico
                ivQrCode.setImageResource(R.drawable.ic_tickets)
            }
        }

        // Método para mostrar el QR en grande
        private fun showExpandedQRCode(bitmap: Bitmap, context: Context) {
            // Crear vista para el diálogo
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_qr_expanded, null)
            val imageView = dialogView.findViewById<ImageView>(R.id.expanded_qr_image)

            // Asignar el bitmap al ImageView
            imageView.setImageBitmap(bitmap)

            // Crear y mostrar el diálogo
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            // Cerrar el diálogo al tocar la imagen
            imageView.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}