package com.example.jonayskieprints

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.jonayskieprints.databinding.FragmentReportsBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.util.*

class AdminReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private var selectedPeriod = "daily"
    private var selectedDate = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set today's date as default
        val cal = Calendar.getInstance()
        selectedDate = String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        binding.tvSelectedDate.text = selectedDate

        // Stat card labels
        binding.layoutRstatOrders.tvRstatLabel.text = "Total Orders"
        binding.layoutRstatSales.tvRstatLabel.text = "Total Sales"
        binding.layoutRstatCustomers.tvRstatLabel.text = "Customers"
        binding.layoutRstatCompletion.tvRstatLabel.text = "Completion Rate"

        // Period spinner
        val periods = listOf("daily", "weekly", "monthly")
        val periodAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periods
        )
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriod.adapter = periodAdapter
        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedPeriod = periods[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Date picker
        binding.btnPickDate.setOnClickListener {
            val cal2 = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                    binding.tvSelectedDate.text = selectedDate
                },
                cal2.get(Calendar.YEAR),
                cal2.get(Calendar.MONTH),
                cal2.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Generate button
        binding.btnGenerateReport.setOnClickListener {
            generateReport()
        }

        // Load base stats on open
        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAdminStats()
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!.data
                    updateStatCards(stats)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateReport() {
        binding.btnGenerateReport.isEnabled = false
        binding.btnGenerateReport.text = "Generating..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAdminStats(
                    period = selectedPeriod,
                    date = selectedDate
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    updateStatCards(body.data)

                    val chartData = body.chartData
                    if (!chartData.isNullOrEmpty()) {
                        renderChart(chartData)
                        binding.lineChart.visibility = View.VISIBLE
                        binding.llChartEmpty.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnGenerateReport.isEnabled = true
                binding.btnGenerateReport.text = "Generate Report"
            }
        }
    }

    private fun updateStatCards(stats: AdminStats) {
        binding.layoutRstatOrders.tvRstatValue.text = stats.totalOrders.toString()
        binding.layoutRstatSales.tvRstatValue.text = "₱${stats.totalSales}"
        binding.layoutRstatCustomers.tvRstatValue.text = stats.totalCustomers.toString()
        val rate = if (stats.totalOrders > 0)
            (stats.completedOrders * 100) / stats.totalOrders else 0
        binding.layoutRstatCompletion.tvRstatValue.text = "$rate%"
    }

    private fun renderChart(chartData: List<ChartPoint>) {
        val labels = chartData.map { it.label }
        val revenueEntries = chartData.mapIndexed { i, d -> Entry(i.toFloat(), d.Revenue.toFloat()) }
        val orderEntries = chartData.mapIndexed { i, d -> Entry(i.toFloat(), d.Orders.toFloat()) }

        val revenueSet = LineDataSet(revenueEntries, "Revenue (₱)").apply {
            color = android.graphics.Color.parseColor("#7C3AED")
            setCircleColor(android.graphics.Color.parseColor("#7C3AED"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }

        val orderSet = LineDataSet(orderEntries, "Orders").apply {
            color = android.graphics.Color.parseColor("#10B981")
            setCircleColor(android.graphics.Color.parseColor("#10B981"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }

        binding.lineChart.apply {
            data = LineData(revenueSet, orderSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.granularity = 1f
            xAxis.labelRotationAngle = -45f
            description.isEnabled = false
            legend.isEnabled = true
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}