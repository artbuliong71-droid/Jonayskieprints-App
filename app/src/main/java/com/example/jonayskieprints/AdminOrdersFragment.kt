package com.example.jonayskieprints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jonayskieprints.databinding.FragmentOrdersBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class AdminOrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = "Manage Orders"
        binding.rvOrders.layoutManager = LinearLayoutManager(context)

        loadOrders()

        binding.swipeRefresh.setOnRefreshListener {
            loadOrders()
        }
    }

    private fun loadOrders() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAdminOrders()
                if (response.isSuccessful && response.body() != null) {
                    val orders = response.body()!!.data ?: emptyList()
                    val adapter = AdminOrdersAdapter(
                        orders,
                        onStatusUpdate = { order, newStatus ->
                            updateStatus(order, newStatus)
                        },
                        onViewReceipt = { order ->
                            // TODO: Show GCash receipt
                        }
                    )
                    binding.rvOrders.adapter = adapter
                    binding.tvEmptyState.visibility =
                        if (orders.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(context, "Failed to load orders", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateStatus(order: AdminOrder, status: String) {
        lifecycleScope.launch {
            try {
                val orderId = (order.order_id ?: order._id ?: return@launch)
                    .toRequestBody("text/plain".toMediaTypeOrNull())
                val statusBody = status
                    .toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.instance.updateOrderStatus(orderId, statusBody)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                    loadOrders()
                } else {
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}