package com.example.jonayskieprints.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.activity.UserDashboardActivity;
import com.example.jonayskieprints.adapter.OrderAdapter;
import com.example.jonayskieprints.api.UserApiClient;
import com.example.jonayskieprints.model.Order;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class MyOrdersFragment extends Fragment {

    private OrderAdapter  adapter;
    private String        currentFilter = "";  // "" = all, "pending", "completed", etc.
    private ProgressBar   progress;
    private TextView      tvEmpty;
    private RecyclerView  rv;

    private final UserApiClient api = UserApiClient.get();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv       = view.findViewById(R.id.rv_orders);
        progress = view.findViewById(R.id.progress_orders);
        tvEmpty  = view.findViewById(R.id.tv_empty_orders);

        adapter = new OrderAdapter(new ArrayList<>(), new OrderAdapter.Listener() {
            @Override public void onViewDetails(Order o) { openViewDetails(o); }
            @Override public void onEdit(Order o)        { openEdit(o); }
            @Override public void onCancel(Order o)      { promptCancel(o); }
        });
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        ChipGroup cg = view.findViewById(R.id.chip_group_filter);
        if (cg != null) {
            cg.setOnCheckedStateChangeListener((group, ids) -> {
                if (ids.isEmpty()) return;
                int id = ids.get(0);
                if      (id == R.id.chip_all)        currentFilter = "";
                else if (id == R.id.chip_pending)    currentFilter = "pending";
                else if (id == R.id.chip_done)       currentFilter = "completed";
                else if (id == R.id.chip_inprogress) currentFilter = "in-progress";
                else if (id == R.id.chip_cancelled)  currentFilter = "cancelled";
                fetchOrders();
            });
        }

        fetchOrders();
    }

    // ── Fetch via API (replaces Firestore query) ──────────────────────────────

    /**
     * Calls GET /api/dashboard?status=xxx (or no status param for "all").
     *
     * The server already filters by the logged-in user's session, so we don't
     * need to pass a userId — the jp_session cookie handles that.
     */
    private void fetchOrders() {
        if (progress == null || tvEmpty == null || rv == null) return;

        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.GONE);

        // Pass null for "all", or the status string for filtered views
        String filter = currentFilter.isEmpty() ? null : currentFilter;

        api.fetchOrders(filter, orders -> {
            if (!isAdded()) return;

            progress.setVisibility(View.GONE);

            if (orders == null || orders.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(R.string.no_orders); // ← fixed: was no_orders_found
                rv.setVisibility(View.GONE);
                return;
            }

            rv.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            adapter.updateOrders(orders);
        });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void openViewDetails(Order o) {
        ViewDetailsDialog dlg = ViewDetailsDialog.newInstance(o);
        dlg.setOnActionListener(new ViewDetailsDialog.ActionListener() {
            @Override public void onEdit(Order order)   { openEdit(order); }
            @Override public void onCancel(Order order) { promptCancel(order); }
        });
        dlg.show(getParentFragmentManager(), "view_details");
    }

    private void openEdit(Order o) {
        EditOrderDialog dlg = EditOrderDialog.newInstance(o);
        dlg.setOnSavedListener(this::fetchOrders);
        dlg.show(getParentFragmentManager(), "edit_order");
    }

    private void promptCancel(Order o) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Cancel Order")
                .setMessage("Cancel order #" + o.getDisplayId() + "? This cannot be undone.")
                .setPositiveButton("Yes, Cancel", (d, w) -> doCancel(o))
                .setNegativeButton("Keep Order", null)
                .show();
    }

    /**
     * POST /api/dashboard  { action: "cancelOrder", order_id: "..." }
     * Replaces the old Firestore .update("status", "cancelled") call.
     */
    private void doCancel(Order o) {
        api.cancelOrder(o.orderId, ok -> {
            if (!isAdded()) return;
            UserDashboardActivity host = (UserDashboardActivity) requireActivity();
            if (ok) {
                host.showToast("Order cancelled.");
                fetchOrders();
            } else {
                host.showToast("Could not cancel order.");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchOrders();
    }
}