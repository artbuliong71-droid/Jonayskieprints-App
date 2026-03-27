package com.example.jonayskieprints.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.activity.AdminDashboardActivity;
import com.example.jonayskieprints.adapter.OrderAdapter;
import com.example.jonayskieprints.api.UserApiClient;
import com.example.jonayskieprints.model.Order;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdminHomeFragment extends Fragment {

    private static final int POLL_INTERVAL = 15_000;

    private TextView     tvLastUpdated;
    private PieChart     pieChart;
    private RecyclerView rvRecentOrders;
    private TextView     tvNoOrders;

    private TextView tvTotalOrders, tvPendingOrders, tvTotalCustomers, tvTotalSales;
    private TextView tvLabelOrders, tvLabelPending, tvLabelCustomers, tvLabelSales;

    private OrderAdapter recentAdapter;

    private final Handler  handler     = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = this::fetchData;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvLastUpdated  = view.findViewById(R.id.tv_last_updated);
        pieChart       = view.findViewById(R.id.pie_chart);
        rvRecentOrders = view.findViewById(R.id.rv_recent_orders);
        tvNoOrders     = view.findViewById(R.id.tv_no_orders);

        // ── Stat cards ────────────────────────────────────────────────────────
        View cardOrders    = view.findViewById(R.id.layout_total_orders);
        View cardPending   = view.findViewById(R.id.layout_pending_orders);
        View cardCustomers = view.findViewById(R.id.layout_total_customers);
        View cardSales     = view.findViewById(R.id.layout_total_sales);

        tvTotalOrders    = cardOrders.findViewById(R.id.tv_stat_value);
        tvPendingOrders  = cardPending.findViewById(R.id.tv_stat_value);
        tvTotalCustomers = cardCustomers.findViewById(R.id.tv_stat_value);
        tvTotalSales     = cardSales.findViewById(R.id.tv_stat_value);

        tvLabelOrders    = cardOrders.findViewById(R.id.tv_stat_label);
        tvLabelPending   = cardPending.findViewById(R.id.tv_stat_label);
        tvLabelCustomers = cardCustomers.findViewById(R.id.tv_stat_label);
        tvLabelSales     = cardSales.findViewById(R.id.tv_stat_label);

        tvLabelOrders.setText("Total Orders");
        tvLabelPending.setText("Pending");
        tvLabelCustomers.setText("Customers");
        tvLabelSales.setText("Total Sales");

        setIcon(cardOrders,    R.drawable.ic_cart);
        setIcon(cardPending,   R.drawable.ic_clock);
        setIcon(cardCustomers, R.drawable.ic_users);
        setIcon(cardSales,     R.drawable.ic_credit_card);

        // ── Clickable stat cards ──────────────────────────────────────────────
        cardOrders.setOnClickListener(v    -> goToOrders(""));
        cardPending.setOnClickListener(v   -> goToOrders("pending"));
        cardSales.setOnClickListener(v     -> goToOrders("completed"));
        cardCustomers.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CustomersFragment())
                        .addToBackStack(null)
                        .commit());

        // ── View All button ───────────────────────────────────────────────────
        view.findViewById(R.id.btn_view_all_orders).setOnClickListener(v -> goToOrders(""));

        // ── Recent orders RecyclerView ────────────────────────────────────────
        recentAdapter = new OrderAdapter(
                new ArrayList<>(),
                new OrderAdapter.Listener() {
                    @Override public void onViewDetails(Order o) { /* TODO */ }
                    @Override public void onEdit(Order o) {}
                    @Override public void onCancel(Order o) {}
                },
                false,  // showActions
                false   // isAdminMode — recent list is display-only
        );
        rvRecentOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentOrders.setNestedScrollingEnabled(false);
        rvRecentOrders.setAdapter(recentAdapter);

        setupPieChart();
        fetchData();
        handler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    private void setIcon(View card, int drawableRes) {
        android.widget.ImageView iv = card.findViewById(R.id.iv_stat_icon);
        if (iv != null) {
            iv.setImageResource(drawableRes);
            iv.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    private void setupPieChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setRotationEnabled(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#111827"));
    }

    // ── Fetch all data ────────────────────────────────────────────────────────
    private void fetchData() {
        if (!isAdded()) return;

        // 1. Stats API → totalOrders, pendingOrders, totalSpent
        UserApiClient.get().fetchStats(stats -> {
            if (!isAdded()) return;

            tvTotalOrders.setText(String.valueOf(stats.totalOrders));
            tvPendingOrders.setText(String.valueOf(stats.pendingOrders));
            tvTotalSales.setText(String.format("₱%.2f", stats.totalSpent));

            String time = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                    .format(new Date());
            tvLastUpdated.setText("Updated " + time);

            if (requireActivity() instanceof AdminDashboardActivity) {
                ((AdminDashboardActivity) requireActivity())
                        .updateNotificationBadge(stats.pendingOrders);
            }
        });

        // 2. All orders → pie chart + unique customer count + recent list
        UserApiClient.get().fetchOrders("", orders -> {
            if (!isAdded()) return;

            int pending = 0, inProgress = 0, completed = 0, cancelled = 0;
            Set<String> uniqueCustomers = new HashSet<>();

            for (Order o : orders) {
                String s = o.status != null ? o.status.toLowerCase() : "";
                switch (s) {
                    case "pending":     pending++;    break;
                    case "in progress":
                    case "inprogress":
                    case "in-progress":
                    case "printing":    inProgress++; break;
                    case "completed":   completed++;  break;
                    case "cancelled":   cancelled++;  break;
                }

                // user_id is stored in userName field (mapped in parseOrder)
                if (o.userName != null && !o.userName.isEmpty()) {
                    uniqueCustomers.add(o.userName);
                }
            }

            // Customers card
            tvTotalCustomers.setText(String.valueOf(uniqueCustomers.size()));

            // Pie chart
            updatePieChart(pending, inProgress, completed, cancelled, orders.size());

            // Recent orders (top 5)
            List<Order> recent = orders.size() > 5
                    ? orders.subList(0, 5) : orders;

            if (recent.isEmpty()) {
                tvNoOrders.setVisibility(View.VISIBLE);
                rvRecentOrders.setVisibility(View.GONE);
            } else {
                tvNoOrders.setVisibility(View.GONE);
                rvRecentOrders.setVisibility(View.VISIBLE);
                recentAdapter.updateOrders(new ArrayList<>(recent));
            }
        });
    }

    // ── Pie chart ─────────────────────────────────────────────────────────────
    private void updatePieChart(int pending, int inProgress,
                                int completed, int cancelled, int total) {
        if (total == 0) {
            pieChart.clear();
            pieChart.setCenterText("No Data");
            pieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        if (pending    > 0) entries.add(new PieEntry(pending,    "Pending"));
        if (inProgress > 0) entries.add(new PieEntry(inProgress, "In Progress"));
        if (completed  > 0) entries.add(new PieEntry(completed,  "Completed"));
        if (cancelled  > 0) entries.add(new PieEntry(cancelled,  "Cancelled"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#F59E0B"),  // pending     — amber
                Color.parseColor("#3B82F6"),  // in progress — blue
                Color.parseColor("#22C55E"),  // completed   — green
                Color.parseColor("#EF4444")   // cancelled   — red
        );
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);

        pieChart.setData(new PieData(dataSet));
        pieChart.setCenterText(total + "\nTotal Orders");
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    // ── Navigation helper ────────────────────────────────────────────────────
    private void goToOrders(String filter) {
        Bundle args = new Bundle();
        args.putString("filter", filter);
        OrdersFragment frag = new OrdersFragment();
        frag.setArguments(args);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, frag)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(pollRunnable);
    }
}