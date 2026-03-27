package com.example.jonayskieprints.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.api.UserApiClient;
import com.example.jonayskieprints.model.Prices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewOrderFragment extends Fragment {

    private static final int REQ_FILES   = 101;
    private static final int REQ_RECEIPT = 102;

    private LinearLayout llStep0, llStep1, llStep2, llPickupTime, tilAddress,
            llPriceBreakdown, llCashInfo, llFileList;
    private MaterialCardView llGcashPanel;
    private TextView tvStep1, tvStep2, tvStep3;
    private MaterialButton btnPayCash, btnPayGcash, btnPickFiles;
    private Button btnPickReceipt, btnPlaceOrder;
    private RadioGroup rgDelivery, rgGcashAmount, rgPrintType;
    private RadioButton rbFull, rbDown;
    private FrameLayout flFullPayment, flDownpayment;
    private EditText etQuantity, etSpecifications;
    private TextInputLayout tilSpecifications;
    private CheckBox cbLamination, cbFolder;

    // Summary TextViews
    private TextView tvGcashAmount, tvFullAmtBox,
            tvSummaryService, tvSummaryDelivery, tvSummaryPickupTime,
            tvSummaryPayment, tvTotalAmount,
            tvSummaryPaperSize, tvSummaryPaperType; // ← NEW

    private ImageView ivReceiptPreview;

    private Spinner spinnerService, spinnerPickupTime, spinnerPaperSize, spinnerPaperType,
            spinnerPhotoSize, spinnerFolderSize, spinnerFolderColor;

    private double total         = 0.00;
    private String paymentMethod = "gcash";
    private List<Uri> selectedFiles = new ArrayList<>();
    private Uri gcashReceiptUri  = null;
    private int totalPages       = 0;

    private Prices dynamicPricing = new Prices();

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_order_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Steps
        llStep0 = v.findViewById(R.id.ll_step0);
        llStep1 = v.findViewById(R.id.ll_step1);
        llStep2 = v.findViewById(R.id.ll_step2);
        tvStep1 = v.findViewById(R.id.tv_step1);
        tvStep2 = v.findViewById(R.id.tv_step2);
        tvStep3 = v.findViewById(R.id.tv_step3);

        // Controls
        spinnerService     = v.findViewById(R.id.spinner_service);
        spinnerPaperSize   = v.findViewById(R.id.spinner_paper_size);
        spinnerPaperType   = v.findViewById(R.id.spinner_paper_type);
        spinnerPhotoSize   = v.findViewById(R.id.spinner_photo_size);
        spinnerFolderSize  = v.findViewById(R.id.spinner_folder_size);
        spinnerFolderColor = v.findViewById(R.id.spinner_folder_color);
        spinnerPickupTime  = v.findViewById(R.id.spinner_pickup_time);

        etQuantity        = v.findViewById(R.id.et_quantity);
        etSpecifications  = v.findViewById(R.id.et_specifications);
        tilSpecifications = v.findViewById(R.id.til_specifications);
        rgDelivery        = v.findViewById(R.id.rg_delivery);
        rgPrintType       = v.findViewById(R.id.rg_print_type);
        llPickupTime      = v.findViewById(R.id.ll_pickup_time);
        tilAddress        = v.findViewById(R.id.til_address);
        cbLamination      = v.findViewById(R.id.cb_lamination);
        cbFolder          = v.findViewById(R.id.cb_folder);

        // Summary & breakdown
        llPriceBreakdown    = v.findViewById(R.id.ll_price_breakdown);
        tvSummaryService    = v.findViewById(R.id.tv_summary_service);
        tvSummaryDelivery   = v.findViewById(R.id.tv_summary_delivery);
        tvSummaryPickupTime = v.findViewById(R.id.tv_summary_pickup_time);
        tvSummaryPayment    = v.findViewById(R.id.tv_summary_payment);
        tvTotalAmount       = v.findViewById(R.id.tv_total_amount);
        tvSummaryPaperSize  = v.findViewById(R.id.tv_summary_paper_size); // ← NEW
        tvSummaryPaperType  = v.findViewById(R.id.tv_summary_paper_type); // ← NEW

        // Payment UI
        btnPayCash       = v.findViewById(R.id.btn_pay_cash);
        btnPayGcash      = v.findViewById(R.id.btn_pay_gcash);
        llCashInfo       = v.findViewById(R.id.ll_cash_info);
        llGcashPanel     = v.findViewById(R.id.ll_gcash_panel);
        flFullPayment    = v.findViewById(R.id.fl_full_payment);
        flDownpayment    = v.findViewById(R.id.fl_downpayment);
        tvFullAmtBox     = v.findViewById(R.id.tv_full_amt_box);
        rgGcashAmount    = v.findViewById(R.id.rg_gcash_amount);
        rbFull           = v.findViewById(R.id.rb_full);
        rbDown           = v.findViewById(R.id.rb_down);
        tvGcashAmount    = v.findViewById(R.id.tv_gcash_amount);
        btnPickReceipt   = v.findViewById(R.id.btn_pick_receipt);
        ivReceiptPreview = v.findViewById(R.id.iv_receipt_preview);
        btnPickFiles     = v.findViewById(R.id.btn_pick_files);
        llFileList       = v.findViewById(R.id.ll_file_list);
        btnPlaceOrder    = v.findViewById(R.id.btn_place_order);

        // IMPORTANT: navigation & listeners must be registered BEFORE setupSpinners().
        // setAdapter() fires onItemSelected immediately, which calls refreshSummary().
        // All views must already be bound at that point or we get a NullPointerException.
        setupNavigation(v);
        setupListeners();
        setupSpinners();   // ← intentionally last

        setPayment("gcash");
        fetchPricingFromDb();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data
    // ─────────────────────────────────────────────────────────────────────────

    private void fetchPricingFromDb() {
        UserApiClient.get().fetchPrices(prices -> {
            if (prices != null) {
                this.dynamicPricing = prices;
                refreshSummary();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listeners
    // ─────────────────────────────────────────────────────────────────────────

    private void setupListeners() {
        etQuantity.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { refreshSummary(); }
        });

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { refreshSummary(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // ← NEW: refresh summary when paper options change
        spinnerPaperSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { refreshSummary(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerPaperType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { refreshSummary(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        rgPrintType.setOnCheckedChangeListener((g, id) -> refreshSummary());
        cbLamination.setOnCheckedChangeListener((btn, checked) -> refreshSummary());
        cbFolder.setOnCheckedChangeListener((btn, checked) -> refreshSummary());

        rgDelivery.setOnCheckedChangeListener((g, id) -> {
            boolean isPickup = (id == R.id.rb_pickup);
            llPickupTime.setVisibility(isPickup ? View.VISIBLE : View.GONE);
            tilAddress.setVisibility(isPickup ? View.GONE : View.VISIBLE);
            refreshSummary();
        });

        btnPayCash.setOnClickListener(v -> setPayment("cash"));
        btnPayGcash.setOnClickListener(v -> setPayment("gcash"));

        flFullPayment.setOnClickListener(v -> {
            rbFull.setChecked(true);
            updateGcashSelectionUI();
            updateGcashAmount();
        });

        flDownpayment.setOnClickListener(v -> {
            if (total >= 500) {
                rbDown.setChecked(true);
                updateGcashSelectionUI();
                updateGcashAmount();
            } else {
                Toast.makeText(getContext(),
                        "Downpayment only available for orders ₱500 and above",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnPickFiles.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Files"), REQ_FILES);
        });

        btnPickReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_RECEIPT);
        });

        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Summary
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshSummary() {
        String service = spinnerService.getSelectedItem() != null
                ? spinnerService.getSelectedItem().toString() : "Print";

        int qty = 1;
        try { qty = Integer.parseInt(etQuantity.getText().toString()); } catch (Exception ignored) {}

        double unitPrice = 0.0;
        if (service.equals("Print")) {
            boolean isColor = (rgPrintType.getCheckedRadioButtonId() == R.id.rb_color);
            unitPrice = isColor ? dynamicPricing.printColor : dynamicPricing.printBw;
            if (unitPrice <= 0) unitPrice = isColor ? 5.0 : 3.0;
        } else if (service.equals("Photocopying")) {
            unitPrice = dynamicPricing.photocopying > 0 ? dynamicPricing.photocopying : 1.5;
        } else if (service.equals("Photo Development")) {
            unitPrice = dynamicPricing.photoDevelopment > 0 ? dynamicPricing.photoDevelopment : 15.0;
        } else if (service.equals("Laminating")) {
            unitPrice = dynamicPricing.laminating > 0 ? dynamicPricing.laminating : 20.0;
        }

        double calculatedQty = (service.equals("Print") || service.equals("Photocopying")) && totalPages > 0
                ? (qty * totalPages) : qty;

        double baseTotal       = unitPrice * calculatedQty;
        double laminationPrice = cbLamination.isChecked()
                ? (dynamicPricing.laminating > 0 ? dynamicPricing.laminating : 15.0) * calculatedQty : 0;
        double folderPrice     = cbFolder.isChecked()
                ? (dynamicPricing.folder > 0 ? dynamicPricing.folder : 10.0) : 0;

        total = baseTotal + laminationPrice + folderPrice;

        // Existing summary fields
        tvSummaryService.setText(service);
        tvSummaryDelivery.setText(
                rgDelivery.getCheckedRadioButtonId() == R.id.rb_pickup ? "Pickup" : "Delivery");
        tvSummaryPickupTime.setText(
                spinnerPickupTime.getSelectedItem() != null
                        ? spinnerPickupTime.getSelectedItem().toString() : "Not set");
        tvTotalAmount.setText(String.format(Locale.getDefault(), "₱%.2f", total));
        tvFullAmtBox.setText(String.format(Locale.getDefault(), "₱%.2f", total));

        // NEW: Paper Size & Paper Type — null-safe guard prevents crash if
        // refreshSummary() is somehow called before onViewCreated finishes binding.
        if (tvSummaryPaperSize != null) {
            tvSummaryPaperSize.setText(
                    spinnerPaperSize.getSelectedItem() != null
                            ? spinnerPaperSize.getSelectedItem().toString() : "Not set");
        }
        if (tvSummaryPaperType != null) {
            tvSummaryPaperType.setText(
                    spinnerPaperType.getSelectedItem() != null
                            ? spinnerPaperType.getSelectedItem().toString() : "Not set");
        }

        // Price breakdown
        llPriceBreakdown.removeAllViews();
        String qtyLabel = totalPages > 0
                ? " (x" + qty + " copies, " + totalPages + " pages)"
                : " (x" + qty + ")";
        addBreakdownRow(service + qtyLabel,
                String.format(Locale.getDefault(), "₱%.2f", baseTotal));
        if (cbLamination.isChecked())
            addBreakdownRow("Lamination",
                    String.format(Locale.getDefault(), "₱%.2f", laminationPrice));
        if (cbFolder.isChecked())
            addBreakdownRow("Folder",
                    String.format(Locale.getDefault(), "₱%.2f", folderPrice));

        if (total < 500 && rbDown.isChecked()) {
            rbFull.setChecked(true);
            updateGcashSelectionUI();
        }

        updateGcashAmount();
    }

    private void addBreakdownRow(String label, String value) {
        View row = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        TextView t1 = row.findViewById(android.R.id.text1);
        TextView t2 = row.findViewById(android.R.id.text2);
        row.setPadding(0, 0, 0, 0);
        t1.setText(label); t1.setTextSize(13);
        t2.setText(value);
        t2.setTextColor(getResources().getColor(R.color.accent));
        t2.setTextSize(13);
        llPriceBreakdown.addView(row);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void setupNavigation(View v) {
        v.findViewById(R.id.btn_step0_next).setOnClickListener(view -> {
            llStep0.setVisibility(View.GONE);
            llStep1.setVisibility(View.VISIBLE);
            updateStepIndicator(1);
        });
        v.findViewById(R.id.btn_step1_next).setOnClickListener(view -> {
            if (validateStep1()) {
                llStep1.setVisibility(View.GONE);
                llStep2.setVisibility(View.VISIBLE);
                updateStepIndicator(2);
                refreshSummary();
            }
        });
        v.findViewById(R.id.btn_step1_back).setOnClickListener(view -> {
            llStep1.setVisibility(View.GONE);
            llStep0.setVisibility(View.VISIBLE);
            updateStepIndicator(0);
        });
        v.findViewById(R.id.btn_step2_back).setOnClickListener(view -> {
            llStep2.setVisibility(View.GONE);
            llStep1.setVisibility(View.VISIBLE);
            updateStepIndicator(1);
        });
    }

    private boolean validateStep1() {
        String specs = etSpecifications.getText().toString().trim();
        if (TextUtils.isEmpty(specs)) {
            tilSpecifications.setError("Please provide specifications and completion date");
            etSpecifications.requestFocus();
            return false;
        } else {
            tilSpecifications.setError(null);
            return true;
        }
    }

    private void updateStepIndicator(int step) {
        tvStep1.setBackgroundResource(step >= 0 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive);
        tvStep2.setBackgroundResource(step >= 1 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive);
        tvStep3.setBackgroundResource(step >= 2 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spinners
    // ─────────────────────────────────────────────────────────────────────────

    private void setupSpinners() {
        String[] services = {"Print", "Photocopying", "Photo Development", "Laminating"};
        spinnerService.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, services));

        ArrayAdapter<CharSequence> paperSizeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.paper_sizes, android.R.layout.simple_spinner_item);
        paperSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaperSize.setAdapter(paperSizeAdapter);

        ArrayAdapter<CharSequence> paperTypeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.paper_types, android.R.layout.simple_spinner_item);
        paperTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaperType.setAdapter(paperTypeAdapter);

        ArrayAdapter<CharSequence> photoSizeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.photo_sizes, android.R.layout.simple_spinner_item);
        photoSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhotoSize.setAdapter(photoSizeAdapter);

        ArrayAdapter<CharSequence> folderSizeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.folder_sizes, android.R.layout.simple_spinner_item);
        folderSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFolderSize.setAdapter(folderSizeAdapter);

        ArrayAdapter<CharSequence> folderColorAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.folder_colors, android.R.layout.simple_spinner_item);
        folderColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFolderColor.setAdapter(folderColorAdapter);

        List<String> times = new ArrayList<>();
        for (int h = 8; h <= 17; h++) {
            times.add((h > 12 ? h - 12 : h) + ":00 " + (h < 12 ? "AM" : "PM"));
            times.add((h > 12 ? h - 12 : h) + ":30 " + (h < 12 ? "AM" : "PM"));
        }
        spinnerPickupTime.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, times));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payment
    // ─────────────────────────────────────────────────────────────────────────

    private void setPayment(String method) {
        paymentMethod = method;
        boolean isGcash = method.equals("gcash");

        llGcashPanel.setVisibility(isGcash ? View.VISIBLE : View.GONE);
        llCashInfo.setVisibility(isGcash ? View.GONE : View.VISIBLE);

        if (isGcash) {
            btnPayGcash.setStrokeColor(null);
            btnPayGcash.setBackgroundTintList(getResources().getColorStateList(R.color.accent));
            btnPayGcash.setTextColor(getResources().getColor(R.color.white));
            btnPayGcash.setIconTintResource(R.color.white);

            btnPayCash.setStrokeColor(getResources().getColorStateList(R.color.border));
            btnPayCash.setBackgroundTintList(getResources().getColorStateList(R.color.white));
            btnPayCash.setTextColor(getResources().getColor(R.color.muted));
            btnPayCash.setIconTintResource(R.color.muted);
        } else {
            btnPayCash.setStrokeColor(null);
            btnPayCash.setBackgroundTintList(getResources().getColorStateList(R.color.accent));
            btnPayCash.setTextColor(getResources().getColor(R.color.white));
            btnPayCash.setIconTintResource(R.color.white);

            btnPayGcash.setStrokeColor(getResources().getColorStateList(R.color.border));
            btnPayGcash.setBackgroundTintList(getResources().getColorStateList(R.color.white));
            btnPayGcash.setTextColor(getResources().getColor(R.color.muted));
            btnPayGcash.setIconTintResource(R.color.muted);
        }

        tvSummaryPayment.setText(isGcash ? "GCash" : "Cash on Pickup");
    }

    private void updateGcashSelectionUI() {
        boolean isFull = rbFull.isChecked();
        flFullPayment.setSelected(isFull);
        flDownpayment.setSelected(!isFull);

        TextView tvFullLabel = (TextView) ((LinearLayout) flFullPayment.getChildAt(0)).getChildAt(0);
        TextView tvFullAmt   = (TextView) ((LinearLayout) flFullPayment.getChildAt(0)).getChildAt(1);
        TextView tvDownLabel = (TextView) ((LinearLayout) flDownpayment.getChildAt(0)).getChildAt(0);
        TextView tvDownSub   = (TextView) ((LinearLayout) flDownpayment.getChildAt(0)).getChildAt(1);

        int activeColor   = getResources().getColor(R.color.accent);
        int inactiveColor = getResources().getColor(R.color.muted);

        tvFullLabel.setTextColor(isFull  ? activeColor : inactiveColor);
        tvFullAmt.setTextColor(isFull    ? activeColor : inactiveColor);
        tvDownLabel.setTextColor(!isFull ? activeColor : inactiveColor);
        tvDownSub.setTextColor(!isFull   ? activeColor : inactiveColor);
    }

    private void updateGcashAmount() {
        double amt = rbFull.isChecked() ? total : (total / 2);
        tvGcashAmount.setText(String.format(Locale.getDefault(), "₱%.2f", amt));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Order placement
    // ─────────────────────────────────────────────────────────────────────────

    private void placeOrder() {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(getContext(), "Please upload your files first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (paymentMethod.equals("gcash") && gcashReceiptUri == null) {
            Toast.makeText(getContext(), "Please upload your GCash receipt screenshot", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getContext(), "Placing your order...", Toast.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File handling
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;

        if (requestCode == REQ_FILES) {
            selectedFiles.clear();
            llFileList.removeAllViews();
            totalPages = 0;

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    selectedFiles.add(uri);
                    processFile(uri);
                }
            } else if (data.getData() != null) {
                selectedFiles.add(data.getData());
                processFile(data.getData());
            }
        } else if (requestCode == REQ_RECEIPT) {
            gcashReceiptUri = data.getData();
            ivReceiptPreview.setImageURI(gcashReceiptUri);
            ivReceiptPreview.setVisibility(View.VISIBLE);
            btnPickReceipt.setText("Change Receipt");
        }
    }

    private void processFile(Uri uri) {
        String fileName = uri.getLastPathSegment();
        int pages = countPages(uri);
        totalPages += pages;

        TextView tv = new TextView(getContext());
        String displayText = "📄 " + fileName;
        if (pages > 0) displayText += " (" + pages + " pages)";
        tv.setText(displayText);
        tv.setPadding(0, 8, 0, 8);
        llFileList.addView(tv);

        refreshSummary();
    }

    private int countPages(Uri uri) {
        try {
            String type = getContext().getContentResolver().getType(uri);
            if (type != null && type.equals("application/pdf")) {
                InputStream is = getContext().getContentResolver().openInputStream(uri);
                PdfReader reader = new PdfReader(is);
                PdfDocument pdfDoc = new PdfDocument(reader);
                int n = pdfDoc.getNumberOfPages();
                pdfDoc.close();
                return n;
            }
        } catch (Exception e) {
            Log.e("NewOrderFragment", "Error counting pages", e);
        }
        return 0;
    }
}