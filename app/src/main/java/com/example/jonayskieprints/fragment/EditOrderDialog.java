package com.example.jonayskieprints.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.activity.UserDashboardActivity;
import com.example.jonayskieprints.api.UserApiClient;
import com.example.jonayskieprints.model.Order;              // ← Order na
import com.google.android.material.textfield.TextInputEditText;

import okhttp3.MultipartBody;

public class EditOrderDialog extends DialogFragment {

    private Order order;                                      // ← Order na
    private Runnable onSavedListener;

    public static EditOrderDialog newInstance(Order o) {     // ← Order na
        EditOrderDialog d = new EditOrderDialog();
        d.order = o;
        return d;
    }

    public void setOnSavedListener(Runnable r) { this.onSavedListener = r; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_JonayskiePrints);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_order_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (order == null) { dismiss(); return; }

        Spinner spinnerService = view.findViewById(R.id.spinner_service);
        if (spinnerService != null && spinnerService.getAdapter() != null) {
            for (int i = 0; i < spinnerService.getAdapter().getCount(); i++) {
                if (order.service != null && order.service.equals(
                        spinnerService.getAdapter().getItem(i).toString())) {
                    spinnerService.setSelection(i);
                    break;
                }
            }
        }

        EditText etQty = view.findViewById(R.id.et_quantity);
        if (etQty != null) etQty.setText(String.valueOf(order.quantity));

        RadioButton rbPickup   = view.findViewById(R.id.rb_pickup);
        RadioButton rbDelivery = view.findViewById(R.id.rb_delivery);
        if ("pickup".equals(order.deliveryOption) && rbPickup != null)
            rbPickup.setChecked(true);
        if ("delivery".equals(order.deliveryOption) && rbDelivery != null)
            rbDelivery.setChecked(true);

        TextInputEditText etSpecs = view.findViewById(R.id.et_specifications);
        if (etSpecs != null && order.specifications != null)
            etSpecs.setText(order.specifications);

        Button btnPlace = view.findViewById(R.id.btn_place_order);
        if (btnPlace != null) {
            btnPlace.setText("Save Changes");
            btnPlace.setOnClickListener(v -> saveChanges(view));
        }
    }

    private void saveChanges(View view) {
        EditText etQty            = view.findViewById(R.id.et_quantity);
        TextInputEditText etSpecs = view.findViewById(R.id.et_specifications);
        RadioButton rbPickup      = view.findViewById(R.id.rb_pickup);
        Spinner spinnerService    = view.findViewById(R.id.spinner_service);

        String service = spinnerService != null && spinnerService.getSelectedItem() != null
                ? spinnerService.getSelectedItem().toString() : order.service;
        String specsText = etSpecs != null && etSpecs.getText() != null
                ? etSpecs.getText().toString().trim() : "";
        int qty = 1;
        try {
            if (etQty != null && etQty.getText() != null)
                qty = Integer.parseInt(etQty.getText().toString());
        } catch (Exception ignored) {}
        String delivery = (rbPickup != null && rbPickup.isChecked()) ? "pickup" : "delivery";

        if (specsText.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Specifications are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Button btnSave = view.findViewById(R.id.btn_place_order);
        if (btnSave != null) { btnSave.setEnabled(false); btnSave.setText("Saving…"); }

        MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("action",          "updateOrder")
                .addFormDataPart("order_id",        order.orderId != null ? order.orderId : "")
                .addFormDataPart("service",         service)
                .addFormDataPart("quantity",        String.valueOf(qty))
                .addFormDataPart("specifications",  specsText)
                .addFormDataPart("delivery_option", delivery)
                .build();

        UserApiClient.get().updateOrder(body, result -> {
            if (!isAdded()) return;
            if (btnSave != null) { btnSave.setEnabled(true); btnSave.setText("Save Changes"); }
            if (result != null && result.optBoolean("success")) {
                if (getActivity() instanceof UserDashboardActivity)
                    ((UserDashboardActivity) getActivity()).showToast("Order updated successfully!");
                if (onSavedListener != null) onSavedListener.run();
                dismiss();
            } else {
                String msg = result != null
                        ? result.optString("message", "Error updating order")
                        : "Network error";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
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