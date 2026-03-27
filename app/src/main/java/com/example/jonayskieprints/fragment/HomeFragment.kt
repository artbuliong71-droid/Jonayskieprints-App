package com.example.jonayskieprints.fragment

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jonayskieprints.R
import com.example.jonayskieprints.activity.UserDashboardActivity
import com.example.jonayskieprints.adapter.OrderAdapter
import com.example.jonayskieprints.api.UserApiClient
import com.example.jonayskieprints.databinding.FragmentHomeBinding
import com.example.jonayskieprints.databinding.ItemStatCardBinding
import com.example.jonayskieprints.model.Order

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val api = UserApiClient.get()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStatCards()      // show zeros while loading
        loadUserStats()       // → GET /api/dashboard?action=getDashboardStats
        loadRecentOrders()    // → GET /api/dashboard (latest 5)
        setupPricingCards()   // → GET /api/pricing

        binding.btnViewAll.setOnClickListener {
            (activity as? UserDashboardActivity)?.navigateToMyOrders()
        }
    }

    // ── Stat cards ────────────────────────────────────────────────────────────

    private fun setupStatCards() {
        configureStatCard(binding.statTotal,     R.drawable.ic_cart,         "#DBEAFE", "#2563EB", "0",     "Total Orders")
        configureStatCard(binding.statPending,   R.drawable.ic_clock,        "#FEF3C7", "#D97706", "0",     "Pending Orders")
        configureStatCard(binding.statCompleted, R.drawable.ic_check_circle, "#DCFCE7", "#16A34A", "0",     "Completed")
        configureStatCard(binding.statSpent,     R.drawable.ic_credit_card,  "#EDE9FE", "#7C3AED", "₱0.00", "Total Spent")
    }

    private fun configureStatCard(
        card: ItemStatCardBinding,
        iconRes: Int,
        bgColor: String,
        iconColor: String,
        value: String,
        label: String
    ) {
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(bgColor))
        }
        card.flStatIconBg.background = shape
        card.ivStatIcon.setImageResource(iconRes)
        card.ivStatIcon.setColorFilter(Color.parseColor(iconColor), PorterDuff.Mode.SRC_IN)
        card.tvStatValue.text = value
        card.tvStatLabel.text = label
    }

    /**
     * GET /api/dashboard?action=getDashboardStats
     * Replaces the old Firestore collection scan.
     */
    private fun loadUserStats() {
        api.fetchStats { stats ->
            if (_binding == null) return@fetchStats
            binding.statTotal.tvStatValue.text     = stats.totalOrders.toString()
            binding.statPending.tvStatValue.text   = stats.pendingOrders.toString()
            binding.statCompleted.tvStatValue.text = stats.completedOrders.toString()
            binding.statSpent.tvStatValue.text     = "₱${stats.totalSpent}"
        }
    }

    // ── Pricing cards ─────────────────────────────────────────────────────────

    /**
     * GET /api/pricing — public endpoint, no auth needed.
     * Falls back to hardcoded defaults if the call fails.
     */
    private fun setupPricingCards() {
        data class PricingItem(val iconRes: Int, val name: String, val price: String, val unit: String)

        fun populateGrid(items: List<PricingItem>) {
            if (_binding == null) return
            val grid = binding.gridPricing
            grid.removeAllViews()
            items.forEachIndexed { index, item ->
                val cardView = LayoutInflater.from(context)
                    .inflate(R.layout.item_pricing_card, grid, false)

                val params = android.widget.GridLayout.LayoutParams().apply {
                    width  = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = if (index == items.size - 1)
                        android.widget.GridLayout.spec(
                            android.widget.GridLayout.UNDEFINED, 2,
                            android.widget.GridLayout.FILL, 1f)
                    else
                        android.widget.GridLayout.spec(
                            android.widget.GridLayout.UNDEFINED, 1,
                            android.widget.GridLayout.FILL, 1f)
                    setMargins(6, 6, 6, 6)
                }
                cardView.layoutParams = params

                cardView.findViewById<ImageView>(R.id.iv_service_icon).setImageResource(item.iconRes)
                cardView.findViewById<TextView>(R.id.tv_service_name).text  = item.name
                cardView.findViewById<TextView>(R.id.tv_service_price).text = item.price
                cardView.findViewById<TextView>(R.id.tv_service_unit).text  = item.unit
                cardView.setOnClickListener {
                    (activity as? UserDashboardActivity)?.navigateToNewOrder()
                }
                grid.addView(cardView)
            }
        }

        val defaults = listOf(
            PricingItem(R.drawable.ic_printer,  "Print B&W",   "₱3.00",  "per page"),
            PricingItem(R.drawable.ic_printer,  "Print Color", "₱5.00",  "per page"),
            PricingItem(R.drawable.ic_copy,     "Photocopy",   "₱2.00",  "per page"),
            PricingItem(R.drawable.ic_photo,    "Photo Dev.",  "₱15.00", "per photo"),
            PricingItem(R.drawable.ic_laminate, "Laminating",  "₱20.00", "per item")
        )

        // Show defaults immediately, replace when API responds
        populateGrid(defaults)

        api.fetchPrices { prices ->
            if (prices == null) return@fetchPrices
            populateGrid(listOf(
                PricingItem(R.drawable.ic_printer,  "Print B&W",   "₱%.2f".format(prices.printBw),          "per page"),
                PricingItem(R.drawable.ic_printer,  "Print Color", "₱%.2f".format(prices.printColor),       "per page"),
                PricingItem(R.drawable.ic_copy,     "Photocopy",   "₱%.2f".format(prices.photocopying),     "per page"),
                PricingItem(R.drawable.ic_photo,    "Photo Dev.",  "₱%.2f".format(prices.photoDevelopment), "per photo"),
                PricingItem(R.drawable.ic_laminate, "Laminating",  "₱%.2f".format(prices.laminating),       "per item")
            ))
        }
    }

    // ── Recent orders ─────────────────────────────────────────────────────────

    /**
     * GET /api/dashboard — fetches all orders, then shows the first 5.
     * Replaces the old Firestore query with .limit(5) + .orderBy("createdAt").
     */
    private fun loadRecentOrders() {
        val rv = binding.rvRecentOrders
        rv.layoutManager = LinearLayoutManager(context)

        // Show empty state while loading
        binding.tvNoRecent.visibility = View.GONE
        rv.visibility = View.GONE

        api.fetchOrders(null) { orders ->
            if (_binding == null) return@fetchOrders

            if (orders.isEmpty()) {
                binding.tvNoRecent.visibility = View.VISIBLE
                rv.visibility = View.GONE
                return@fetchOrders
            }

            // API already returns newest-first (sorted by created_at DESC on server)
            val recent = orders.take(5)

            binding.tvNoRecent.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.adapter = OrderAdapter(
                recent.toMutableList(),
                object : OrderAdapter.Listener {
                    override fun onViewDetails(o: Order) {}
                    override fun onEdit(o: Order) {}
                    override fun onCancel(o: Order) {}
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}