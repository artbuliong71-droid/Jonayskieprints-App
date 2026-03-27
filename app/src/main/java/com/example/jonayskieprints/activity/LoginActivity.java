package com.example.jonayskieprints.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.RegisterActivity;
import com.example.jonayskieprints.api.UserApiClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    // Views
    private TextInputLayout   tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button            btnSignIn, btnGoogle, btnRegister;
    private TextView          tvForgotPassword;
    private FrameLayout       flCheckbox;
    private ImageView         ivCheck;

    // State
    private boolean rememberMe = false;
    private boolean isLoading  = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Google Sign-In (gets the ID token, then sent to Next.js backend)
    private GoogleSignInClient googleSignInClient;

    // Prefs
    private static final String PREFS_NAME      = "jonayskie_prefs";
    private static final String KEY_SAVED_EMAIL = "saved_email";

    private final UserApiClient api = UserApiClient.get();

    // Google Sign-In launcher
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            handleGoogleToken(account.getIdToken(), account.getEmail());
                        } catch (ApiException e) {
                            setLoading(false);
                            showNotification("Google Sign-In failed: " + e.getMessage(), false);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ Handle the splash screen transition
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupGoogleSignIn();
        bindViews();
        restoreSavedEmail();
        setupListeners();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("395472208925-bh109a696c72fb1rgaodd562q3gg8kpo.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void bindViews() {
        tilEmail         = findViewById(R.id.til_email);
        tilPassword      = findViewById(R.id.til_password);
        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        btnSignIn        = findViewById(R.id.btn_sign_in);
        btnGoogle        = findViewById(R.id.btn_google);
        btnRegister      = findViewById(R.id.btn_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        flCheckbox       = findViewById(R.id.fl_checkbox);
        ivCheck          = findViewById(R.id.iv_check);
    }

    private void restoreSavedEmail() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = prefs.getString(KEY_SAVED_EMAIL, "");
        if (!saved.isEmpty() && etEmail != null) {
            etEmail.setText(saved);
            rememberMe = true;
            updateCheckboxUI();
        }
    }

    private void setupListeners() {
        if (flCheckbox != null) {
            View llRemember = (View) flCheckbox.getParent();
            if (llRemember != null) {
                llRemember.setOnClickListener(v -> {
                    rememberMe = !rememberMe;
                    updateCheckboxUI();
                });
            }
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v ->
                    startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))
            );
        }

        if (btnSignIn   != null) btnSignIn.setOnClickListener(v -> handleSignIn());
        if (btnGoogle   != null) btnGoogle.setOnClickListener(v -> handleGoogleSignIn());
        if (btnRegister != null) btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        if (etEmail != null) {
            etEmail.addTextChangedListener(new SimpleTextWatcher(() -> {
                if (tilEmail != null) tilEmail.setError(null);
            }));
        }
        if (etPassword != null) {
            etPassword.addTextChangedListener(new SimpleTextWatcher(() -> {
                if (tilPassword != null) tilPassword.setError(null);
            }));
        }
    }

    private void updateCheckboxUI() {
        if (flCheckbox == null || ivCheck == null) return;
        if (rememberMe) {
            flCheckbox.setBackgroundResource(R.drawable.bg_checkbox_checked);
            ivCheck.setVisibility(View.VISIBLE);
        } else {
            flCheckbox.setBackgroundResource(R.drawable.bg_checkbox_unchecked);
            ivCheck.setVisibility(View.GONE);
        }
    }

    // ── Email / Password Sign In ──────────────────────────────────────────────

    private void handleSignIn() {
        if (isLoading) return;
        if (!validate()) return;

        String email    = getText(etEmail);
        String password = getText(etPassword);

        // Save / clear remembered email
        SharedPreferences.Editor editor =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        if (rememberMe) editor.putString(KEY_SAVED_EMAIL, email);
        else            editor.remove(KEY_SAVED_EMAIL);
        editor.apply();

        setLoading(true);

        api.login(email, password, ok -> {
            if (ok) {
                api.fetchUser(user -> {
                    setLoading(false);
                    showNotification("Signed in successfully!", true);
                    mainHandler.postDelayed(
                            () -> redirectByRole(user != null ? user.role : "customer"),
                            1200
                    );
                });
            } else {
                setLoading(false);
                showNotification("Invalid email or password.", false);
            }
        });
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    private void handleGoogleSignIn() {
        if (isLoading) return;
        setLoading(true);
        // Sign out first so user can pick account each time
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleToken(String idToken, String email) {
        if (idToken == null) {
            setLoading(false);
            showNotification("Google Sign-In failed: no ID token", false);
            return;
        }

        api.loginWithGoogle(idToken, ok -> {
            if (ok) {
                api.fetchUser(user -> {
                    setLoading(false);
                    showNotification("Signed in successfully!", true);
                    mainHandler.postDelayed(
                            () -> redirectByRole(user != null ? user.role : "customer"),
                            1200
                    );
                });
            } else {
                setLoading(false);
                showNotification("Google Sign-In failed. Please try again.", false);
            }
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void redirectByRole(String role) {
        Class<?> dest = "admin".equals(role)
                ? AdminDashboardActivity.class
                : UserDashboardActivity.class;
        startActivity(new Intent(this, dest));
        finish();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validate() {
        boolean valid = true;
        String email    = getText(etEmail);
        String password = getText(etPassword);

        if (TextUtils.isEmpty(email)) {
            if (tilEmail != null) tilEmail.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (tilEmail != null) tilEmail.setError("Invalid email format");
            valid = false;
        } else {
            if (tilEmail != null) tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            if (tilPassword != null) tilPassword.setError("Password is required");
            valid = false;
        } else {
            if (tilPassword != null) tilPassword.setError(null);
        }

        return valid;
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        if (btnSignIn == null) return;
        runOnUiThread(() -> {
            btnSignIn.setEnabled(!loading);
            btnSignIn.setText(loading ? "Signing in…" : "Sign In");
            btnSignIn.setAlpha(loading ? 0.7f : 1f);
            if (btnGoogle != null) btnGoogle.setEnabled(!loading);
        });
    }

    // ── Snackbar notification ─────────────────────────────────────────────────

    private void showNotification(String message, boolean isSuccess) {
        runOnUiThread(() -> {
            View rootView = findViewById(android.R.id.content);
            Snackbar snack = Snackbar.make(rootView, message, 4000);
            View snackView = snack.getView();
            snackView.setBackgroundColor(isSuccess
                    ? Color.parseColor("#22C55E")
                    : Color.parseColor("#EF4444"));
            TextView tv = snackView.findViewById(
                    com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(13f);
            tv.setMaxLines(3);
            snack.show();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getText(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable action;
        SimpleTextWatcher(Runnable action) { this.action = action; }
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) { action.run(); }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}
