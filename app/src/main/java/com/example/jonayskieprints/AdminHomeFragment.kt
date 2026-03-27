package com.example.jonayskieprints

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.jonayskieprints.databinding.FragmentAdminHomeBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("AdminHome", "onCreateView called")
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AdminHome", "onViewCreated called")

        // ── Labels ────────────────────────────────────────────
        binding.layoutTotalOrders.tvStatLabel.text    = "Total Orders"
        binding.layoutPendingOrders.tvStatLabel.text  = "Pending"
        binding.layoutTotalCustomers.tvStatLabel.text = "Customers"
        binding.layoutTotalSales.tvStatLabel.text     = "Total Sales"

        // ── Icons ─────────────────────────────────────────────
        binding.layoutTotalOrders.ivStatIcon.setImageResource(R.drawable.ic_cart)
        binding.layoutPendingOrders.ivStatIcon.setImageResource(R.drawable.ic_clock)
        binding.layoutTotalCustomers.ivStatIcon.setImageResource(R.drawable.ic_users)
        binding.layoutTotalSales.ivStatIcon.setImageResource(R.drawable.ic_credit_card)

        // ── Tint all icons white explicitly ───────────────────
        val white = android.graphics.Color.WHITE
        binding.layoutTotalOrders.ivStatIcon
            .setColorFilter(white, android.graphics.PorterDuff.Mode.SRC_IN)
        binding.layoutPendingOrders.ivStatIcon
            .setColorFilter(white, android.graphics.PorterDuff.Mode.SRC_IN)
        binding.layoutTotalCustomers.ivStatIcon
            .setColorFilter(white, android.graphics.PorterDuff.Mode.SRC_IN)
        binding.layoutTotalSales.ivStatIcon
            .setColorFilter(white, android.graphics.PorterDuff.Mode.SRC_IN)

        loadStatsFromFirestore()
    }

    private fun loadStatsFromFirestore() {
        Log.d("AdminHome", "loadStatsFromFirestore() called")

        // ── Fetch all orders ──────────────────────────────────
        db.collection("orders").get()
            .addOnSuccessListener { orders ->
                Log.d("AdminHome", "Orders SUCCESS - count: ${orders.size()}")
                if (_binding == null) {
                    Log.d("AdminHome", "Binding is null, skipping update")
                    return@addOnSuccessListener
                }

                var totalOrders   = 0
                var pending       = 0
                var inProgress    = 0
                var completed     = 0
                var cancelled     = 0
                var totalSales    = 0.0

                for (doc in orders) {
                    totalOrders++
                    val status = doc.getString("status")?.lowercase() ?: ""
                    val amount = doc.getDouble("totalAmount") ?: 0.0
                    Log.d("AdminHome", "Order doc: status='$status', amount=$amount, allFields=${doc.data?.keys}")

                    when (status) {
                        "pending"     -> pending++
                        "in progress",
                        "inprogress"  -> inProgress++
                        "completed"   -> { completed++; totalSales += amount }
                        "cancelled"   -> cancelled++
                    }
                }

                Log.d("AdminHome", "Results => total=$totalOrders, pending=$pending, inProgress=$inProgress, completed=$completed, cancelled=$cancelled, sales=$totalSales")

                // ── Update stat cards ─────────────────────────
                binding.layoutTotalOrders.tvStatValue.text   = totalOrders.toString()
                binding.layoutPendingOrders.tvStatValue.text = pending.toString()
                binding.layoutTotalSales.tvStatValue.text    = "₱%.2f".format(totalSales)

                // ── Update timestamp ──────────────────────────
                val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                binding.tvLastUpdated.text = "Updated $time"

                // ── Draw pie chart ────────────────────────────
                setupPieChart(pending, inProgress, completed, cancelled, totalOrders)
            }
            .addOnFailureListener { e ->
                Log.e("AdminHome", "Orders FAILED: ${e.message}", e)
                if (_binding == null) return@addOnFailureListener
                binding.layoutTotalOrders.tvStatValue.text    = "—"
                binding.layoutPendingOrders.tvStatValue.text  = "—"
                binding.layoutTotalCustomers.tvStatValue.text = "—"
                binding.layoutTotalSales.tvStatValue.text     = "—"
            }

        // ── Fetch customer count separately ───────────────────
        db.collection("users")
            .whereEqualTo("role", "user")
            .get()
            .addOnSuccessListener { users ->
                Log.d("AdminHome", "Users SUCCESS - count: ${users.size()}")
                if (_binding == null) return@addOnSuccessListener
                binding.layoutTotalCustomers.tvStatValue.text = users.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("AdminHome", "Users FAILED: ${e.message}", e)
            }
    }

    private fun setupPieChart(
        pending: Int, inProgress: Int,
        completed: Int, cancelled: Int,
        total: Int
    ) {
        if (_binding == null) return

        val chart = binding.pieChart

        if (total == 0) {
            chart.setNoDataText("No chart data available.")
            chart.setNoDataTextColor(android.graphics.Color.parseColor("#F59E0B"))
            chart.invalidate()
            return
        }

        val entries = mutableListOf<PieEntry>()
        if (pending    > 0) entries.add(PieEntry(pending.toFloat(),    "Pending"))
        if (inProgress > 0) entries.add(PieEntry(inProgress.toFloat(), "In Progress"))
        if (completed  > 0) entries.add(PieEntry(completed.toFloat(),  "Completed"))
        if (cancelled  > 0) entries.add(PieEntry(cancelled.toFloat(),  "Cancelled"))

        val colors = listOf(
            android.graphics.Color.parseColor("#F59E0B"),
            android.graphics.Color.parseColor("#3B82F6"),
            android.graphics.Color.parseColor("#22C55E"),
            android.graphics.Color.parseColor("#EF4444")
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace      = 2f
            selectionShift  = 4f
            setDrawValues(false)
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = "${value.toInt()}"
            })
        }

        chart.apply {
            this.data = data
            isDrawHoleEnabled       = true
            holeRadius              = 58f
            transparentCircleRadius = 62f
            setHoleColor(android.graphics.Color.WHITE)
            description.isEnabled   = false
            legend.isEnabled        = false
            setDrawEntryLabels(false)
            isRotationEnabled       = true
            animateY(800)
            setDrawCenterText(true)
            centerText       = "$total\nTotal Orders"
            setCenterTextSize(14f)
            setCenterTextColor(android.graphics.Color.parseColor("#111827"))
            invalidate()
        }

        updateLegend(pending, inProgress, completed, cancelled)
    }

    private fun updateLegend(
        pending: Int, inProgress: Int,
        completed: Int, cancelled: Int
    ) {
        val legendLayout = binding.llChartLegend
        legendLayout.removeAllViews()

        val items = listOf(
            Triple("#F59E0B", "Pending",     pending),
            Triple("#3B82F6", "In Progress", inProgress),
            Triple("#22C55E", "Completed",   completed),
            Triple("#EF4444", "Cancelled",   cancelled)
        )

        for ((color, label, count) in items) {
            val row = LayoutInflater.from(context)
                .inflate(R.layout.item_chart_legend_dot, legendLayout, false)

            val dot = row.findViewById<View>(R.id.view_legend_dot)
            val bg = android.graphics.drawable.GradientDrawable()
            bg.shape = android.graphics.drawable.GradientDrawable.OVAL
            bg.setColor(android.graphics.Color.parseColor(color))
            dot.background = bg

            row.findViewById<android.widget.TextView>(R.id.tv_legend_label)
                .text = "$label ($count)"

            legendLayout.addView(row)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}