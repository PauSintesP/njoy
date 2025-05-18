package com.example.njoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.njoy.DataClasesApi.PaymentResponse
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentAdapter(
    private val payments: List<PaymentResponse>
) : RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment, parent, false)
        return PaymentViewHolder(view)
    }

    override fun getItemCount() = payments.size

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payments[position]
        holder.bind(payment)
    }

    class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPaymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        private val tvPaymentTotal: TextView = itemView.findViewById(R.id.tvPaymentTotal)
        private val tvPaymentDate: TextView = itemView.findViewById(R.id.tvPaymentDate)

        fun bind(payment: PaymentResponse) {
            // Configurar método de pago
            tvPaymentMethod.text = payment.metodo_pago

            // Formatear el total con dos decimales
            val df = DecimalFormat("0.00")
            tvPaymentTotal.text = "${df.format(payment.total)}€"

            // Formatear la fecha si es posible
            formatDate(payment.fecha)
        }

        private fun formatDate(dateStr: String) {
            try {
                // Intentar formatear la fecha si tiene formato ISO
                if (dateStr.contains("T")) {
                    val parts = dateStr.split("T")
                    val datePart = parts[0].split("-")

                    if (datePart.size == 3) {
                        val year = datePart[0]
                        val month = datePart[1]
                        val day = datePart[2]

                        tvPaymentDate.text = "$day/$month/$year"
                    } else {
                        tvPaymentDate.text = dateStr
                    }
                } else {
                    tvPaymentDate.text = dateStr
                }
            } catch (e: Exception) {
                // En caso de error mostrar la fecha original
                tvPaymentDate.text = dateStr
            }
        }
    }
}