package com.example.jonayskieprints.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.api.ApiClient;
import com.example.jonayskieprints.model.Pricing;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private TextInputEditText etBw, etColor, etPhotocopy, etPhoto, etLam, etFolder;
    private Button btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etBw        = view.findViewById(R.id.et_price_bw);
        etColor     = view.findViewById(R.id.et_price_color);
        etPhotocopy = view.findViewById(R.id.et_price_photocopy);
        etPhoto     = view.findViewById(R.id.et_price_photo);
        etLam       = view.findViewById(R.id.et_price_lam);
        etFolder    = view.findViewById(R.id.et_price_folder);
        btnSave     = view.findViewById(R.id.btn_save_pricing);

        // Fetch current pricing
        ApiClient.get().fetchPricing(p -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                etBw.setText(String.valueOf(p.printBw));
                etColor.setText(String.valueOf(p.printColor));
                etPhotocopy.setText(String.valueOf(p.photocopying));
                etPhoto.setText(String.valueOf(p.photoDevelopment));
                etLam.setText(String.valueOf(p.laminating));
                etFolder.setText(String.valueOf(p.folder));
            });
        });

        btnSave.setOnClickListener(v -> savePricing());
    }

    private void savePricing() {
        try {
            Pricing p = new Pricing();
            p.printBw          = parseDouble(etBw, 1);
            p.printColor       = parseDouble(etColor, 2);
            p.photocopying     = parseDouble(etPhotocopy, 2);
            p.photoDevelopment = parseDouble(etPhoto, 15);
            p.laminating       = parseDouble(etLam, 20);
            p.folder           = parseDouble(etFolder, 10);

            btnSave.setEnabled(false);
            btnSave.setText("Saving…");

            ApiClient.get().savePricing(p, success -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Pricing");
                    if (success) {
                        Toast.makeText(requireContext(), "Pricing updated!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to save pricing", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

    private double parseDouble(TextInputEditText et, double def) {
        try {
            String s = et.getText() != null ? et.getText().toString().trim() : "";
            return s.isEmpty() ? def : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
