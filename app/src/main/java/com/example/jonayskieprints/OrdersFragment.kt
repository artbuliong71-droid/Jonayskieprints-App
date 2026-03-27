package com.example.jonayskieprints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jonayskieprints.adapter.OrderAdapter
import com.example.jonayskieprints.databinding.FragmentOrdersBinding
import com.example.jonayskieprints.model.Order              // ← Order na
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvOrders.layoutManager = LinearLayoutManager(context)
        binding.swipeRefresh.setOnRefreshListener { loadOrders() }
        loadOrders()
    }

    private fun loadOrders() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val token = auth.currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token

                if (token == null) {
                    binding.tvEmptyState.text = "Not logged in"
                    binding.tvEmptyState.visibility = View.VISIBLE
                    return@launch
                }

                val response = RetrofitClient.instance
                    .getUserOrders("Bearer $token")

                if (response.isSuccessful && response.body()?.success == true) {
                    val mobileOrders = response.body()!!.data ?: emptyList()

                    val orders = mobileOrders.map { o ->
                        Order().apply {
                            orderId        = o.order_id
                            service        = o.service
                            serviceType    = o.serviceType
                            quantity       = o.quantity
                            specifications = o.specifications
                            deliveryOption = o.deliveryOption
                            pickupTime     = o.pickupTime
                            status         = o.status
                            paymentMethod  = o.paymentMethod
                            totalAmount    = try { o.totalAmount?.toDouble() ?: 0.0 } catch (e: Exception) { 0.0 }
                            price          = o.price
                            createdAt      = o.createdAt
                            date           = o.date
                            fileName       = o.fileName
                        }
                    }.toMutableList()

                    binding.rvOrders.adapter = OrderAdapter(
                        orders,
                        object : OrderAdapter.Listener {
                            override fun onViewDetails(o: Order) {}
                            override fun onEdit(o: Order) {}
                            override fun onCancel(o: Order) {}
                        },
                        true
                    )

                    binding.tvEmptyState.visibility =
                        if (orders.isEmpty()) View.VISIBLE else View.GONE

                } else {
                    binding.tvEmptyState.text = "Failed to load orders"
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.tvEmptyState.text = "Network Error: ${e.message}"
                binding.tvEmptyState.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}