package com.example.jonayskieprints.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.api.UserApiClient;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilOtp, tilNewPassword;
    private TextInputEditText etEmail, etOtp, etNewPassword;
    private Button btnAction;
    private TextView tvTitle, tvSubtitle, tvBackToLogin;

    private enum State { SEND_OTP, VERIFY_OTP, RESET_PASSWORD }
    private State currentState = State.SEND_OTP;
    private String userEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        tilEmail = findViewById(R.id.til_email);
        tilOtp = findViewById(R.id.til_otp);
        tilNewPassword = findViewById(R.id.til_new_password);
        etEmail = findViewById(R.id.et_email);
        etOtp = findViewById(R.id.et_otp);
        etNewPassword = findViewById(R.id.et_new_password);
        btnAction = findViewById(R.id.btn_action);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);
    }

    private void setupListeners() {
        btnAction.setOnClickListener(v -> handleAction());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void handleAction() {
        switch (currentState) {
            case SEND_OTP:
                sendOtp();
                break;
            case VERIFY_OTP:
                verifyOtp();
                break;
            case RESET_PASSWORD:
                resetPassword();
                break;
        }
    }

    private void sendOtp() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return;
        }
        tilEmail.setError(null);
        userEmail = email;

        btnAction.setEnabled(false);
        btnAction.setText("Sending...");

        UserApiClient.get().forgotPassword(email, ok -> {
            btnAction.setEnabled(true);
            if (ok) {
                showNotification("OTP sent to your email", true);
                currentState = State.VERIFY_OTP;
                updateUI();
            } else {
                showNotification("Failed to send OTP. Check email or try again.", false);
                btnAction.setText("Send OTP");
            }
        });
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();
        if (otp.length() != 6) {
            tilOtp.setError("Enter 6-digit OTP");
            return;
        }
        tilOtp.setError(null);

        btnAction.setEnabled(false);
        btnAction.setText("Verifying...");

        UserApiClient.get().verifyOtp(userEmail, otp, ok -> {
            btnAction.setEnabled(true);
            if (ok) {
                showNotification("OTP Verified", true);
                currentState = State.RESET_PASSWORD;
                updateUI();
            } else {
                showNotification("Invalid OTP. Please try again.", false);
                btnAction.setText("Verify OTP");
            }
        });
    }

    private void resetPassword() {
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
            tilNewPassword.setError("Password must be at least 6 characters");
            return;
        }
        tilNewPassword.setError(null);

        btnAction.setEnabled(false);
        btnAction.setText("Resetting...");

        UserApiClient.get().resetPassword(userEmail, otp, newPass, ok -> {
            btnAction.setEnabled(true);
            if (ok) {
                showNotification("Password reset successfully!", true);
                findViewById(android.R.id.content).postDelayed(this::finish, 1500);
            } else {
                showNotification("Failed to reset password.", false);
                btnAction.setText("Reset Password");
            }
        });
    }

    private void updateUI() {
        switch (currentState) {
            case VERIFY_OTP:
                tvTitle.setText("Verify OTP");
                tvSubtitle.setText("Enter the 6-digit code sent to " + userEmail);
                tilEmail.setVisibility(View.GONE);
                tilOtp.setVisibility(View.VISIBLE);
                btnAction.setText("Verify OTP");
                break;
            case RESET_PASSWORD:
                tvTitle.setText("New Password");
                tvSubtitle.setText("Create a new strong password for your account");
                tilOtp.setVisibility(View.GONE);
                tilNewPassword.setVisibility(View.VISIBLE);
                btnAction.setText("Reset Password");
                break;
        }
    }

    private void showNotification(String message, boolean isSuccess) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snack = Snackbar.make(rootView, message, 4000);
        View snackView = snack.getView();
        snackView.setBackgroundColor(isSuccess ? Color.parseColor("#22C55E") : Color.parseColor("#EF4444"));
        TextView tv = snackView.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        snack.show();
    }
}