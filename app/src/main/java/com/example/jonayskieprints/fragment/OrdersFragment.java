package com.example.jonayskieprints.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.adapter.OrderAdapter;
import com.example.jonayskieprints.api.UserApiClient;
import com.example.jonayskieprints.model.Order;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private static final int POLL_INTERVAL = 15000;

    private RecyclerView  rvOrders;
    private ProgressBar   progressOrders;
    private TextView      tvNoOrders;
    private OrderAdapter  orderAdapter;
    private String        currentFilter = "";

    private final Handler  handler     = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = () -> fetchOrders(currentFilter, true);

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvOrders       = view.findViewById(R.id.rv_orders);
        progressOrders = view.findViewById(R.id.progress_orders);
        tvNoOrders     = view.findViewById(R.id.tv_no_orders);

        if (getArguments() != null) {
            currentFilter = getArguments().getString("filter", "");
        }

        // ── Adapter — isAdminMode = true ─────────────────────────────────────
        orderAdapter = new OrderAdapter(
                new ArrayList<>(),
                new OrderAdapter.Listener() {

                    @Override
                    public void onViewDetails(Order o) {
                        // TODO: open detail screen
                    }

                    @Override
                    public void onEdit(Order o) {
                        // not used in admin view
                    }

                    @Override
                    public void onCancel(Order o) {
                        // not used in admin view
                    }

                    /** Admin: status spinner changed */
                    @Override
                    public void onStatusChanged(Order o, String newStatus) {
                        updateOrderStatus(o, newStatus);
                    }

                    /** Admin: delete (trash) button tapped */
                    @Override
                    public void onDelete(Order o) {
                        confirmDelete(o);
                    }
                },
                false,   // showActions — customer edit/cancel buttons OFF for admin
                true     // isAdminMode — show customer name, payment, spinner, delete
        );

        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(orderAdapter);

        // ── Filter chips ──────────────────────────────────────────────────────
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_filter);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chip_all)        currentFilter = "";
            else if (id == R.id.chip_pending)    currentFilter = "pending";
            else if (id == R.id.chip_inprogress) currentFilter = "in-progress";
            else if (id == R.id.chip_completed)  currentFilter = "completed";
            else if (id == R.id.chip_cancelled)  currentFilter = "cancelled";
            fetchOrders(currentFilter, false);
        });

        if ("pending".equals(currentFilter)) {
            View chip = view.findViewById(R.id.chip_pending);
            if (chip != null) chip.performClick();
        }

        fetchOrders(currentFilter, false);
        handler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    // ── Fetch orders ──────────────────────────────────────────────────────────
    private void fetchOrders(String filter, boolean silent) {
        if (!isAdded()) return;
        if (!silent) {
            progressOrders.setVisibility(View.VISIBLE);
            tvNoOrders.setVisibility(View.GONE);
        }

        UserApiClient.get().fetchOrders(filter, orders -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                progressOrders.setVisibility(View.GONE);
                if (orders == null || orders.isEmpty()) {
                    tvNoOrders.setVisibility(View.VISIBLE);
                    rvOrders.setVisibility(View.GONE);
                } else {
                    tvNoOrders.setVisibility(View.GONE);
                    rvOrders.setVisibility(View.VISIBLE);
                    orderAdapter.updateOrders(orders);
                }
            });
        });
    }

    // ── Status update via UserApiClient ───────────────────────────────────────
    private void updateOrderStatus(Order order, String newStatus) {
        okhttp3.RequestBody body = new okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("action",    "updateOrderStatus")
                .addFormDataPart("order_id",  order.orderId != null ? order.orderId : "")
                .addFormDataPart("status",    newStatus)
                .build();

        UserApiClient.get().updateOrder(body, result -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                boolean ok = result != null && result.optBoolean("success", false);
                if (ok) {
                    Toast.makeText(requireContext(),
                            "Status updated to \"" + newStatus + "\"",
                            Toast.LENGTH_SHORT).show();
                    fetchOrders(currentFilter, true); // silent refresh
                } else {
                    Toast.makeText(requireContext(),
                            "Failed to update status", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ── Delete order with confirmation dialog ─────────────────────────────────
    private void confirmDelete(Order order) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Order")
                .setMessage("Delete order #" + order.getDisplayId() + "? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteOrder(order))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteOrder(Order order) {
        UserApiClient.get().cancelOrder(
                order.orderId != null ? order.orderId : "",
                success -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(requireContext(),
                                    "Order deleted", Toast.LENGTH_SHORT).show();
                            fetchOrders(currentFilter, false);
                        } else {
                            Toast.makeText(requireContext(),
                                    "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(pollRunnable);
    }
}