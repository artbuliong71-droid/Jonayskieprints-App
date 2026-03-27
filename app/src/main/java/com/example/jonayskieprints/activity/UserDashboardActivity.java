package com.example.jonayskieprints.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.api.UserApiClient;
import com.example.jonayskieprints.fragment.MyOrdersFragment;
import com.example.jonayskieprints.fragment.NewOrderFragment;
import com.example.jonayskieprints.fragment.UserProfileFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.example.jonayskieprints.fragment.HomeFragment;

public class UserDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TOS_PREF_KEY = "jonayskie_tos_accepted";
    public static final String PREFS_NAME   = "jonayskie_prefs";

    private DrawerLayout drawerLayout;
    private TextView     tvPageTitle;
    private TextView     tvWelcome;
    private TextView     tvAvatarInitials;

    // ✅ Removed FirebaseFirestore — user data lives in MongoDB, not Firestore.
    //    Keeping FirebaseAuth only because auth.signOut() is called on logout.
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        auth = FirebaseAuth.getInstance();
        // ✅ Removed: db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        drawerLayout     = findViewById(R.id.drawer_layout);
        tvPageTitle      = findViewById(R.id.tv_page_title);
        tvWelcome        = findViewById(R.id.tv_welcome);
        tvAvatarInitials = findViewById(R.id.tv_avatar_initials);

        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Avatar → profile
        findViewById(R.id.btn_avatar).setOnClickListener(v -> {
            loadFragment(new UserProfileFragment(), "Profile");
            navView.setCheckedItem(R.id.nav_profile);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // Delay TOS dialog so the window is fully attached first.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(TOS_PREF_KEY, false)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    showTosDialog();
                }
            }, 300);
        }

        // ✅ Replaced fetchUserFromFirestore() — user is in MongoDB, not Firestore.
        //    No null-check redirect here; the session cookie determines auth state.
        fetchUserFromMongoDB();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Dashboard");
            navView.setCheckedItem(R.id.nav_dashboard);
        }
    }

    // ── Fetch user from MongoDB via UserApiClient ─────────────────────────────
    private void fetchUserFromMongoDB() {
        UserApiClient.get().fetchUser(user -> {
            // Already on main thread (UserApiClient posts to mainHandler)
            if (user == null || (user.firstName == null && user.lastName == null)) {
                // Session may have expired or network failed — show safe fallback,
                // do NOT redirect back to LoginActivity here.
                if (tvWelcome != null)        tvWelcome.setText("Welcome!");
                if (tvAvatarInitials != null) tvAvatarInitials.setText("U");
                return;
            }

            // Build display name from first + last name
            String name;
            if (user.firstName != null && !user.firstName.isEmpty()) {
                name = (user.lastName != null && !user.lastName.isEmpty())
                        ? user.firstName + " " + user.lastName
                        : user.firstName;
            } else if (user.email != null && !user.email.isEmpty()) {
                name = user.email.split("@")[0];
            } else {
                name = "User";
            }

            String initials = getInitials(name);

            if (tvWelcome != null)        tvWelcome.setText("Welcome, " + name);
            if (tvAvatarInitials != null) tvAvatarInitials.setText(initials);
        });
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (String.valueOf(parts[0].charAt(0))
                    + String.valueOf(parts[1].charAt(0)))
                    .toUpperCase();
        }
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    // ── Navigation ────────────────────────────────────────
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String title = "Dashboard";

        if (id == R.id.nav_dashboard) {
            fragment = new HomeFragment();        title = "Dashboard";
        } else if (id == R.id.nav_new_order) {
            fragment = new NewOrderFragment();    title = "New Order";
        } else if (id == R.id.nav_my_orders) {
            fragment = new MyOrdersFragment();    title = "My Orders";
        } else if (id == R.id.nav_profile) {
            fragment = new UserProfileFragment(); title = "Profile";
        } else if (id == R.id.nav_logout) {
            handleLogout();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (fragment != null) loadFragment(fragment, title);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void loadFragment(Fragment fragment, String title) {
        if (tvPageTitle != null) tvPageTitle.setText(title);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void navigateToNewOrder() {
        loadFragment(new NewOrderFragment(), "New Order");
        NavigationView nav = findViewById(R.id.nav_view);
        if (nav != null) nav.setCheckedItem(R.id.nav_new_order);
    }

    public void navigateToMyOrders() {
        loadFragment(new MyOrdersFragment(), "My Orders");
        NavigationView nav = findViewById(R.id.nav_view);
        if (nav != null) nav.setCheckedItem(R.id.nav_my_orders);
    }

    // ── TOS Dialog ────────────────────────────────────────
    private void showTosDialog() {
        android.app.Dialog dialog = new android.app.Dialog(
                this, R.style.Theme_JonayskiePrints);
        dialog.setContentView(R.layout.dialog_tos);
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                (int)(getResources().getDisplayMetrics().heightPixels * 0.88));
        dialog.getWindow().setGravity(android.view.Gravity.BOTTOM);
        dialog.getWindow().getDecorView()
                .setBackgroundResource(android.R.color.transparent);
        dialog.setCancelable(false);

        android.widget.ScrollView sv = dialog.findViewById(R.id.sv_tos_body);
        TextView tvHint              = dialog.findViewById(R.id.tv_scroll_hint);
        View btnAccept               = dialog.findViewById(R.id.btn_tos_accept);
        View btnDecline              = dialog.findViewById(R.id.btn_tos_decline);

        final boolean[] scrolled = {false};

        if (sv != null) {
            sv.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
                if (!scrolled[0]) {
                    int bottom = sv.getChildAt(0).getMeasuredHeight()
                            - sv.getMeasuredHeight();
                    if (scrollY >= bottom - 40) {
                        scrolled[0] = true;
                        if (tvHint != null) tvHint.setVisibility(View.GONE);
                        if (btnAccept instanceof android.widget.Button) {
                            android.widget.Button b = (android.widget.Button) btnAccept;
                            b.setEnabled(true);
                            b.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            Color.parseColor("#7C3AED")));
                            b.setText("✓ I Accept These Terms");
                            b.setTextColor(Color.WHITE);
                        }
                    }
                }
            });
        }

        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> {
                if (!scrolled[0]) {
                    Toast.makeText(this,
                            "Please scroll and read all terms first.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putBoolean(TOS_PREF_KEY, true).apply();
                dialog.dismiss();
                showToast("Welcome! You have agreed to our Terms of Service.");
            });
        }

        if (btnDecline != null) {
            btnDecline.setOnClickListener(v -> {
                dialog.dismiss();
                handleLogout();
            });
        }

        dialog.show();
    }

    // ── Logout ────────────────────────────────────────────
    private void handleLogout() {
        // Sign out of Firebase (Google Sign-In token), clear MongoDB session cookie
        auth.signOut();
        UserApiClient.get().logout(null);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK          |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void updateHeaderUser(String firstName, String initials) {
        if (tvWelcome != null)
            tvWelcome.setText("Welcome, " + firstName);
        if (tvAvatarInitials != null)
            tvAvatarInitials.setText(initials.isEmpty() ? "U" : initials);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}