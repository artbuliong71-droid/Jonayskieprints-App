package com.example.jonayskieprints;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jonayskieprints.activity.LoginActivity;
import com.example.jonayskieprints.activity.OtpVerificationActivity;
import com.example.jonayskieprints.api.UserApiClient;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextInputLayout tilFirstName, tilLastName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private CheckBox cbTerms;
    private Button btnRegister;
    private TextView tvTerms, tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email_reg);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password_reg);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        
        tilFirstName = findViewById(R.id.til_first_name);
        tilLastName = findViewById(R.id.til_last_name);
        tilEmail = findViewById(R.id.til_email_reg);
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password_reg);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        cbTerms = findViewById(R.id.cb_terms);
        btnRegister = findViewById(R.id.btn_register_submit);
        tvTerms = findViewById(R.id.tv_terms);
        tvLoginLink = findViewById(R.id.tv_login_link);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());

        tvTerms.setOnClickListener(v -> showTermsDialog());

        tvLoginLink.setOnClickListener(v -> finish());
        
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            tilPassword.setHelperText("");
        } else if (password.length() < 6) {
            tilPassword.setHelperText("Weak password");
        } else if (password.length() < 10) {
            tilPassword.setHelperText("Medium strength");
        } else {
            tilPassword.setHelperText("Strong password ✓");
        }
    }

    private void showTermsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Terms of Service")
                .setMessage("By registering and using Jonayskie Prints, you agree to our terms. For orders above ₱500, a 50% downpayment via GCash is required before processing...")
                .setPositiveButton("Accept", (dialog, which) -> cbTerms.setChecked(true))
                .setNegativeButton("Close", null)
                .show();
    }

    private void handleRegister() {
        String firstName       = etFirstName.getText().toString().trim();
        String lastName        = etLastName.getText().toString().trim();
        String email           = etEmail.getText().toString().trim();
        String phone           = etPhone.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(firstName)) {
            tilFirstName.setError("Required"); isValid = false;
        } else tilFirstName.setError(null);

        if (TextUtils.isEmpty(lastName)) {
            tilLastName.setError("Required"); isValid = false;
        } else tilLastName.setError(null);

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Valid email required"); isValid = false;
        } else tilEmail.setError(null);

        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Required"); isValid = false;
        } else tilPhone.setError(null);

        if (password.length() < 8) {
            tilPassword.setError("Min. 8 characters"); isValid = false;
        } else tilPassword.setError(null);

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match"); isValid = false;
        } else tilConfirmPassword.setError(null);

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to Terms of Service", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid) {
            btnRegister.setEnabled(false);
            btnRegister.setText("Sending OTP...");

            UserApiClient.get().sendRegisterOtp(firstName, lastName, email, phone, password, ok -> {
                btnRegister.setEnabled(true);
                btnRegister.setText("Create Account");

                if (ok) {
                    // Go to OTP screen, pass email so it knows where to verify
                    Intent intent = new Intent(RegisterActivity.this, OtpVerificationActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("mode", "register"); // distinguish from forgot-password OTP
                    startActivity(intent);
                } else {
                    Toast.makeText(this,
                        "Failed to send OTP. Email may already be in use.",
                        Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
