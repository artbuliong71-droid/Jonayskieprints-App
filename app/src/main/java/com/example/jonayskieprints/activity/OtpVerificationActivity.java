package com.example.jonayskieprints.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.api.UserApiClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class OtpVerificationActivity extends AppCompatActivity {

    private TextInputLayout    tilOtp;
    private TextInputEditText  etOtp;
    private Button             btnVerify;
    private TextView           tvResend, tvEmail, tvTimer;

    private String email;
    private String mode; // "register" or "forgot"
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        email = getIntent().getStringExtra("email");
        mode  = getIntent().getStringExtra("mode");

        tilOtp    = findViewById(R.id.til_otp);
        etOtp     = findViewById(R.id.et_otp);
        btnVerify = findViewById(R.id.btn_verify_otp);
        tvResend  = findViewById(R.id.tv_resend_otp);
        tvEmail   = findViewById(R.id.tv_otp_email);
        tvTimer   = findViewById(R.id.tv_otp_timer);

        tvEmail.setText("We sent a code to\n" + email);

        btnVerify.setOnClickListener(v -> handleVerify());
        tvResend.setOnClickListener(v -> handleResend());

        startTimer();
    }

    private void handleVerify() {
        String otp = etOtp.getText().toString().trim();

        if (TextUtils.isEmpty(otp) || otp.length() != 6) {
            tilOtp.setError("Enter the 6-digit code");
            return;
        }
        tilOtp.setError(null);

        btnVerify.setEnabled(false);
        btnVerify.setText("Verifying...");

        if ("register".equals(mode)) {
            UserApiClient.get().verifyRegisterOtp(email, otp, ok -> {
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify");

                if (ok) {
                    Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_SHORT).show();
                    // Go back to Login
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Incorrect or expired OTP.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Existing forgot-password OTP flow
            UserApiClient.get().verifyOtp(email, otp, ok -> {
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify");

                if (ok) {
                    // Check if ResetPasswordActivity exists.
                    // For now, I'll assume it exists or will be handled.
                    // The user's code mentions it.
                    try {
                        Class<?> resetClass = Class.forName("com.example.jonayskieprints.activity.ResetPasswordActivity");
                        Intent intent = new Intent(this, resetClass);
                        intent.putExtra("email", email);
                        intent.putExtra("otp", otp);
                        startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        Toast.makeText(this, "ResetPasswordActivity not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Incorrect or expired OTP.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleResend() {
        tvResend.setEnabled(false);

        if ("register".equals(mode)) {
            // Re-send registration OTP — user data already stored server-side
            // Just re-hit send-otp with email only, or navigate back
            Toast.makeText(this, "Please go back and submit the form again.", Toast.LENGTH_SHORT).show();
            tvResend.setEnabled(true);
        } else {
            UserApiClient.get().forgotPassword(email, ok -> {
                if (ok) {
                    Toast.makeText(this, "New OTP sent!", Toast.LENGTH_SHORT).show();
                    startTimer();
                } else {
                    Toast.makeText(this, "Failed to resend OTP.", Toast.LENGTH_SHORT).show();
                    tvResend.setEnabled(true);
                }
            });
        }
    }

    private void startTimer() {
        tvResend.setEnabled(false);
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Resend in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("");
                tvResend.setEnabled(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
