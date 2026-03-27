package com.example.jonayskieprints

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class AdminOrdersAdapter(
    private var orders: List<AdminOrder>,
    private val onStatusUpdate: (AdminOrder, String) -> Unit,
    private val onViewReceipt: (AdminOrder) -> Unit
) : RecyclerView.Adapter<AdminOrdersAdapter.AdminViewHolder>() {

    class AdminViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tv_order_id)
        val tvOrderAmount: TextView = view.findViewById(R.id.tv_order_amount)
        val tvCustomerName: TextView = view.findViewById(R.id.tv_customer_name)
        val tvCustomerEmail: TextView = view.findViewById(R.id.tv_customer_email)
        val tvService: TextView = view.findViewById(R.id.tv_service)
        val tvQty: TextView = view.findViewById(R.id.tv_qty)
        val tvStatusBadge: TextView = view.findViewById(R.id.tv_status_badge)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val spinnerStatus: Spinner = view.findViewById(R.id.spinner_status)
        val btnViewDetails: Button = view.findViewById(R.id.btn_view_details)
        val btnViewFiles: Button = view.findViewById(R.id.btn_view_files)
        val btnDeleteOrder: ImageButton = view.findViewById(R.id.btn_delete_order)
        val btnGcashReceipt: Button = view.findViewById(R.id.btn_gcash_receipt)
        val tvPaymentBadge: TextView = view.findViewById(R.id.tv_payment_badge)
        val tvPickupBadge: TextView = view.findViewById(R.id.tv_pickup_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_admin, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val order = orders[position]
        
        holder.tvOrderId.text = "#${order.order_id ?: order._id ?: ""}"
        val amount = order.total_amount?.toDoubleOrNull() ?: 0.0
        holder.tvOrderAmount.text = String.format(Locale.getDefault(), "₱%.2f", amount)
        holder.tvCustomerName.text = "Customer: ${order.user_name ?: "Unknown"}"
        holder.tvCustomerEmail.text = order.user_email ?: "customer@example.com"
        holder.tvService.text = order.service ?: ""
        holder.tvQty.text = "Qty: ${order.quantity}"
        holder.tvDate.text = order.created_at ?: ""

        // Apply status badge styling
        applyStatusStyle(holder.tvStatusBadge, order.status ?: "pending")

        // Setup status spinner
        val statusOptions = arrayOf("pending", "in-progress", "completed", "cancelled")
        val statusAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerStatus.adapter = statusAdapter
        
        // Find current status index, mapping "printing" to "in-progress" if needed
        val currentStatus = when(order.status?.lowercase()) {
            "printing" -> "in-progress"
            else -> order.status?.lowercase() ?: "pending"
        }
        val currentPos = statusOptions.indexOf(currentStatus)
        if (currentPos >= 0) {
            holder.spinnerStatus.setSelection(currentPos, false)
        }

        // Flag to avoid triggering update on initial load
        var isInitialBinding = true
        holder.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (!isInitialBinding) {
                    val newStatus = statusOptions[pos]
                    if (newStatus != currentStatus) {
                        onStatusUpdate(order, newStatus)
                    }
                }
                isInitialBinding = false
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Logic for GCash badge and receipt button
        val isGcash = order.payment_method?.contains("gcash", true) == true
        holder.tvPaymentBadge.text = if (isGcash) "GCash" else "Cash"
        holder.tvPaymentBadge.setBackgroundResource(if (isGcash) R.drawable.bg_badge_gcash else R.drawable.bg_badge_completed)
        holder.tvPaymentBadge.setTextColor(if (isGcash) Color.parseColor("#7C3AED") else Color.parseColor("#065F46"))
        
        holder.btnGcashReceipt.visibility = if (isGcash) View.VISIBLE else View.GONE
        holder.btnGcashReceipt.setOnClickListener { onViewReceipt(order) }

        // Action button placeholders
        holder.btnDeleteOrder.setOnClickListener { /* Handle delete if needed */ }
        holder.btnViewDetails.setOnClickListener { /* Handle detail view if needed */ }
        holder.btnViewFiles.setOnClickListener { /* Handle file view if needed */ }
    }

    private fun applyStatusStyle(textView: TextView, status: String) {
        val s = status.lowercase()
        textView.text = s
        when (s) {
            "pending" -> {
                textView.setBackgroundResource(R.drawable.bg_badge_pending)
                textView.setTextColor(Color.parseColor("#92400E"))
            }
            "in-progress", "printing" -> {
                textView.setBackgroundResource(R.drawable.bg_badge_inprogress)
                textView.setTextColor(Color.parseColor("#1E40AF"))
            }
            "completed" -> {
                textView.setBackgroundResource(R.drawable.bg_badge_completed)
                textView.setTextColor(Color.parseColor("#065F46"))
            }
            "cancelled" -> {
                textView.setBackgroundResource(R.drawable.bg_badge_cancelled)
                textView.setTextColor(Color.parseColor("#991B1B"))
            }
            else -> {
                textView.setBackgroundResource(R.drawable.bg_badge_pending)
                textView.setTextColor(Color.BLACK)
            }
        }
    }

    override fun getItemCount() = orders.size

    fun updateData(newOrders: List<AdminOrder>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
