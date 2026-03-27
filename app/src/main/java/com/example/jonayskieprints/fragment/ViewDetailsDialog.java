package com.example.jonayskieprints.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.api.UserApiClient;
import com.example.jonayskieprints.model.Order;              // ← Order na
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewDetailsDialog extends DialogFragment {

    public interface ActionListener {
        void onEdit(Order order);                             // ← Order na
        void onCancel(Order order);
    }

    private static final String KEY_ORDER_ID    = "order_id";
    private static final String KEY_SERVICE     = "service";
    private static final String KEY_QUANTITY    = "quantity";
    private static final String KEY_STATUS      = "status";
    private static final String KEY_SPECS       = "specs";
    private static final String KEY_DELIVERY    = "delivery";
    private static final String KEY_PICKUP_TIME = "pickup_time";
    private static final String KEY_TOTAL       = "total";
    private static final String KEY_PAYMENT     = "payment";
    private static final String KEY_CREATED_AT  = "created_at";

    private Order order;                                      // ← Order na
    private ActionListener actionListener;

    public static ViewDetailsDialog newInstance(Order o) {   // ← Order na
        ViewDetailsDialog d = new ViewDetailsDialog();
        Bundle args = new Bundle();
        args.putString(KEY_ORDER_ID,    o.orderId);
        args.putString(KEY_SERVICE,     o.service);
        args.putInt   (KEY_QUANTITY,    o.quantity);
        args.putString(KEY_STATUS,      o.status);
        args.putString(KEY_SPECS,       o.specifications);
        args.putString(KEY_DELIVERY,    o.deliveryOption);
        args.putString(KEY_PICKUP_TIME, o.pickupTime);
        args.putDouble(KEY_TOTAL,       o.totalAmount);       // ← double na
        args.putString(KEY_PAYMENT,     o.paymentMethod);
        args.putString(KEY_CREATED_AT,  o.createdAt);
        d.setArguments(args);
        d.order = o;
        return d;
    }

    public void setOnActionListener(ActionListener l) { this.actionListener = l; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_JonayskiePrints);
        if (order == null && getArguments() != null) {
            order = new Order();
            order.orderId        = getArguments().getString(KEY_ORDER_ID);
            order.service        = getArguments().getString(KEY_SERVICE);
            order.quantity       = getArguments().getInt(KEY_QUANTITY, 1);
            order.status         = getArguments().getString(KEY_STATUS, "pending");
            order.specifications = getArguments().getString(KEY_SPECS);
            order.deliveryOption = getArguments().getString(KEY_DELIVERY);
            order.pickupTime     = getArguments().getString(KEY_PICKUP_TIME);
            order.totalAmount    = getArguments().getDouble(KEY_TOTAL, 0); // ← double na
            order.paymentMethod  = getArguments().getString(KEY_PAYMENT);
            order.createdAt      = getArguments().getString(KEY_CREATED_AT);
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_view_order_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (order == null) { dismiss(); return; }

        ((TextView) view.findViewById(R.id.tv_detail_order_id))
                .setText("Order #" + order.getDisplayId());
        ((TextView) view.findViewById(R.id.tv_detail_date))
                .setText(fmtDate(order.createdAt));
        view.findViewById(R.id.tv_detail_close).setOnClickListener(v -> dismiss());

        applyStatusBanner(view, order.status);

        LinearLayout llRows = view.findViewById(R.id.ll_detail_rows);
        addRow(llRows, "Service",  order.service);
        addRow(llRows, "Quantity", String.valueOf(order.quantity));
        addRow(llRows, "Delivery", order.deliveryOption != null
                ? order.deliveryOption.substring(0,1).toUpperCase()
                + order.deliveryOption.substring(1) : "");
        if (order.pickupTime != null && !order.pickupTime.isEmpty())
            addRow(llRows, "Pickup Time", order.getFormattedPickupTime());

        ((TextView) view.findViewById(R.id.tv_detail_specs)).setText(
                order.specifications != null
                        ? order.specifications : "No specifications provided.");

        LinearLayout llPrice = view.findViewById(R.id.ll_price_breakdown);
        double total = order.getTotalAmountDouble();
        addRow(llPrice, order.service,
                "₱" + String.format(Locale.getDefault(), "%.2f", total));

        ((TextView) view.findViewById(R.id.tv_detail_payment)).setText(
                order.isGcash() ? "GCash"
                        : (order.paymentMethod != null ? order.paymentMethod : "Cash"));

        fetchFilesAndReceipt(view);

        View btnEdit   = view.findViewById(R.id.btn_detail_edit);
        View btnCancel = view.findViewById(R.id.btn_detail_cancel);
        View btnClose  = view.findViewById(R.id.btn_detail_close);
        btnClose.setOnClickListener(v -> dismiss());

        if (order.isPending() && actionListener != null) {
            btnEdit.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
            btnClose.setVisibility(View.GONE);
            btnEdit.setOnClickListener(v -> { dismiss(); actionListener.onEdit(order); });
            btnCancel.setOnClickListener(v -> { dismiss(); actionListener.onCancel(order); });
        }
    }

    private void fetchFilesAndReceipt(View view) {
        if (order.orderId == null) return;
        UserApiClient.get().fetchOrderFiles(order.orderId, result -> {
            if (!isAdded() || result == null) return;
            try {
                org.json.JSONArray files = result.optJSONObject("data") != null
                        ? result.getJSONObject("data").optJSONArray("files") : null;
                if (files != null && files.length() > 0) {
                    view.findViewById(R.id.tv_files_header).setVisibility(View.VISIBLE);
                    LinearLayout llFiles = view.findViewById(R.id.ll_detail_files);
                    llFiles.setVisibility(View.VISIBLE);
                    for (int i = 0; i < files.length(); i++) {
                        org.json.JSONObject f = files.getJSONObject(i);
                        addFileRow(llFiles,
                                f.optString("name", "File " + (i+1)),
                                f.optString("url"));
                    }
                }
                org.json.JSONObject receipt = result.optJSONObject("data") != null
                        ? result.getJSONObject("data").optJSONObject("gcash_receipt") : null;
                if (receipt != null) {
                    view.findViewById(R.id.tv_receipt_header).setVisibility(View.VISIBLE);
                    ImageView iv = view.findViewById(R.id.iv_detail_receipt);
                    iv.setVisibility(View.VISIBLE);
                    Glide.with(this).load(receipt.optString("url")).into(iv);
                }
            } catch (Exception ignored) {}
        });
    }

    private void addRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#6B7280"));
        tvLabel.setTextSize(12);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvVal = new TextView(requireContext());
        tvVal.setText(value);
        tvVal.setTextColor(Color.parseColor("#111827"));
        tvVal.setTextSize(13);
        tvVal.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(tvLabel);
        row.addView(tvVal);

        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#F3F4F6"));

        parent.addView(row);
        parent.addView(divider);
    }

    private void addFileRow(LinearLayout parent, String name, String url) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
        row.setBackgroundResource(R.drawable.bg_spinner);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(6));
        row.setLayoutParams(lp);

        TextView tv = new TextView(requireContext());
        tv.setText("📄 " + name);
        tv.setTextSize(12);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(tv);

        if (!url.isEmpty()) {
            Button btn = new Button(requireContext());
            btn.setText("View");
            btn.setTextSize(11);
            btn.setOnClickListener(v -> startActivity(
                    new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))));
            row.addView(btn);
        }
        parent.addView(row);
    }

    private void applyStatusBanner(View root, String status) {
        LinearLayout banner = root.findViewById(R.id.ll_status_banner);
        TextView badge = root.findViewById(R.id.tv_detail_status_badge);
        if (banner == null || badge == null) return;
        badge.setText(status);
        switch (status != null ? status : "pending") {
            case "pending":
                banner.setBackgroundResource(R.drawable.bg_badge_pending);
                badge.setTextColor(Color.parseColor("#92400E")); break;
            case "in-progress":
                banner.setBackgroundResource(R.drawable.bg_badge_inprogress);
                badge.setTextColor(Color.parseColor("#1E40AF")); break;
            case "completed":
                banner.setBackgroundResource(R.drawable.bg_badge_completed);
                badge.setTextColor(Color.parseColor("#065F46")); break;
            case "cancelled":
                banner.setBackgroundResource(R.drawable.bg_badge_cancelled);
                badge.setTextColor(Color.parseColor("#991B1B")); break;
        }
    }

    private String fmtDate(String iso) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            Date d = in.parse(iso);
            return d != null ? out.format(d) : iso;
        } catch (Exception e) { return iso != null ? iso : ""; }
    }

    private int dpToPx(int dp) {
        return (int)(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    (int)(requireContext().getResources().getDisplayMetrics().heightPixels * 0.92));
            dialog.getWindow().setGravity(android.view.Gravity.BOTTOM);
            dialog.getWindow().getDecorView()
                    .setBackgroundResource(android.R.color.transparent);
        }
    }
}