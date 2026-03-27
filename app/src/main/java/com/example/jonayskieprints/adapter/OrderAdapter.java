package com.example.jonayskieprints.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.model.Order;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    // ── Status options for admin spinner ──────────────────────────────────────
    private static final List<String> STATUS_OPTIONS = Arrays.asList(
            "pending", "in progress", "completed", "cancelled"
    );

    public interface Listener {
        void onViewDetails(Order o);
        void onEdit(Order o);
        void onCancel(Order o);

        /** Called only in admin mode when spinner selection changes */
        default void onStatusChanged(Order o, String newStatus) {}

        /** Called only in admin mode when delete button is tapped */
        default void onDelete(Order o) {}
    }

    private List<Order> orders;
    private final Listener listener;

    /**
     * showActions  — true  = customer view (Edit / Cancel buttons for pending orders)
     * isAdminMode  — true  = admin view  (customer name, payment badge, status spinner, delete btn)
     */
    private final boolean showActions;
    private final boolean isAdminMode;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Customer view (no edit/cancel actions) */
    public OrderAdapter(List<Order> orders, Listener listener) {
        this(orders, listener, false, false);
    }

    /** Customer view with optional edit/cancel actions */
    public OrderAdapter(List<Order> orders, Listener listener, boolean showActions) {
        this(orders, listener, showActions, false);
    }

    /** Full constructor */
    public OrderAdapter(List<Order> orders, Listener listener,
                        boolean showActions, boolean isAdminMode) {
        this.orders      = orders;
        this.listener    = listener;
        this.showActions = showActions;
        this.isAdminMode = isAdminMode;
    }

    public void updateOrders(List<Order> newList) {
        this.orders = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.bind(orders.get(pos));
    }

    @Override
    public int getItemCount() { return orders.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    class VH extends RecyclerView.ViewHolder {
        TextView    tvIdService, tvCustomerName, tvSpecsPreview,
                tvQtyAmount, tvOrderAmount, tvPaymentBadge,
                tvPickupTime, tvStatus, tvDate;
        Button      btnView, btnEdit, btnCancel;
        Spinner     spinnerStatus;
        ImageButton btnDelete;

        VH(View v) {
            super(v);
            tvIdService    = v.findViewById(R.id.tv_order_id_service);
            tvCustomerName = v.findViewById(R.id.tv_customer_name);
            tvSpecsPreview = v.findViewById(R.id.tv_specs_preview);
            tvQtyAmount    = v.findViewById(R.id.tv_qty_amount);
            tvOrderAmount  = v.findViewById(R.id.tv_order_amount);
            tvPaymentBadge = v.findViewById(R.id.tv_payment_badge);
            tvPickupTime   = v.findViewById(R.id.tv_pickup_time);
            tvStatus       = v.findViewById(R.id.tv_status_badge);
            tvDate         = v.findViewById(R.id.tv_order_date);
            btnView        = v.findViewById(R.id.btn_view_details);
            btnEdit        = v.findViewById(R.id.btn_edit_order);
            btnCancel      = v.findViewById(R.id.btn_cancel_order);
            spinnerStatus  = v.findViewById(R.id.spinner_status);
            btnDelete      = v.findViewById(R.id.btn_delete_order);
        }

        void bind(Order o) {
            // ── Order ID + Service title ───────────────────────────────────────
            tvIdService.setText("#" + o.getDisplayId() + " — " +
                    (o.service != null ? o.service :
                            (o.serviceType != null ? o.serviceType : "—")));

            // ── Amount (right-side, bold) ──────────────────────────────────────
            tvOrderAmount.setText(String.format("₱%.2f",
                    o.getTotalAmountDouble()));

            // ── Customer name (admin only) ────────────────────────────────────
            if (isAdminMode) {
                // API only returns user_id — show it shortened or hide if empty
                String name = (o.userName != null && !o.userName.isEmpty())
                        ? "ID: ..." + o.userName.substring(Math.max(0, o.userName.length() - 6))
                        : "Unknown";
                tvCustomerName.setText(name);
                tvCustomerName.setVisibility(View.VISIBLE);
            } else {
                tvCustomerName.setVisibility(View.GONE);
            }

            // ── Specs preview ─────────────────────────────────────────────────
            String specs = o.fileName != null ? o.fileName :
                    (o.specifications != null ? o.specifications : "—");
            tvSpecsPreview.setText(specs.length() > 75
                    ? specs.substring(0, 75) + "…" : specs);

            // ── Qty (no amount here — moved to tvOrderAmount) ─────────────────
            tvQtyAmount.setText("Qty: " + o.quantity);

            // ── Payment badge (admin only) ────────────────────────────────────
            if (isAdminMode && o.paymentMethod != null && !o.paymentMethod.isEmpty()) {
                tvPaymentBadge.setText(o.isGcash() ? "GCash" : "Cash");
                tvPaymentBadge.setVisibility(View.VISIBLE);
            } else {
                tvPaymentBadge.setVisibility(View.GONE);
            }

            // ── Pickup time badge ─────────────────────────────────────────────
            if ("pickup".equals(o.deliveryOption)
                    && o.pickupTime != null && !o.pickupTime.isEmpty()) {
                tvPickupTime.setVisibility(View.VISIBLE);
                tvPickupTime.setText("🕐 " + o.getFormattedPickupTime());
            } else {
                tvPickupTime.setVisibility(View.GONE);
            }

            // ── Date ──────────────────────────────────────────────────────────
            tvDate.setText(fmtDate(o.date != null ? o.date : o.createdAt));

            // ── Status badge ──────────────────────────────────────────────────
            applyStatus(o.status);

            // ── Click: view details ───────────────────────────────────────────
            btnView.setOnClickListener(v -> { if (listener != null) listener.onViewDetails(o); });
            itemView.setOnClickListener(v -> { if (listener != null) listener.onViewDetails(o); });

            // ── Customer actions: Edit / Cancel (pending only) ────────────────
            if (showActions && !isAdminMode && o.isPending()) {
                btnEdit.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> { if (listener != null) listener.onEdit(o); });
                btnCancel.setOnClickListener(v -> { if (listener != null) listener.onCancel(o); });
            } else {
                btnEdit.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
            }

            // ── Admin actions: Status spinner + Delete button ─────────────────
            if (isAdminMode) {
                // -- Delete button --
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDelete(o);
                });

                // -- Status spinner --
                spinnerStatus.setVisibility(View.VISIBLE);
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        itemView.getContext(),
                        android.R.layout.simple_spinner_item,
                        STATUS_OPTIONS
                );
                spinnerAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item);
                spinnerStatus.setAdapter(spinnerAdapter);

                // Set current status selection (no callback fire on init)
                String currentStatus = o.status != null
                        ? o.status.toLowerCase() : "pending";
                int idx = STATUS_OPTIONS.indexOf(currentStatus);
                // handle "inprogress" / "in-progress" variants
                if (idx < 0 && (currentStatus.contains("progress") || currentStatus.contains("printing"))) {
                    idx = STATUS_OPTIONS.indexOf("in progress");
                }
                spinnerStatus.setTag("init"); // prevent callback on programmatic set
                spinnerStatus.setSelection(Math.max(idx, 0), false);

                spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        if ("init".equals(spinnerStatus.getTag())) {
                            spinnerStatus.setTag(null); // clear init flag
                            return;
                        }
                        String selected = STATUS_OPTIONS.get(position);
                        if (!selected.equals(o.status) && listener != null) {
                            listener.onStatusChanged(o, selected);
                        }
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

            } else {
                spinnerStatus.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }
        }

        // ── Status badge styling ──────────────────────────────────────────────
        private void applyStatus(String status) {
            String s = status != null ? status.toLowerCase() : "pending";
            tvStatus.setText(s);
            switch (s) {
                case "completed":
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_completed);
                    tvStatus.setTextColor(Color.parseColor("#065F46")); break;
                case "in progress":
                case "inprogress":
                case "printing":
                case "in-progress":
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_inprogress);
                    tvStatus.setTextColor(Color.parseColor("#1E40AF")); break;
                case "cancelled":
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
                    tvStatus.setTextColor(Color.parseColor("#991B1B")); break;
                default:
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
                    tvStatus.setTextColor(Color.parseColor("#92400E")); break;
            }
        }

        // ── Date formatter ────────────────────────────────────────────────────
        private String fmtDate(String iso) {
            if (iso == null || iso.isEmpty()) return "—";
            try {
                SimpleDateFormat in  = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat out = new SimpleDateFormat(
                        "MMM d, yyyy", Locale.getDefault());
                Date d = in.parse(iso);
                return d != null ? out.format(d) : iso;
            } catch (Exception e) { return iso; }
        }
    }
}