package com.example.jonayskieprints.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.api.ApiClient;                          // ← ADDED
import com.example.jonayskieprints.fragment.AdminHomeFragment;
import com.example.jonayskieprints.fragment.CustomersFragment;
import com.example.jonayskieprints.fragment.DeletedOrdersFragment;
import com.example.jonayskieprints.fragment.OrdersFragment;
import com.example.jonayskieprints.fragment.ReportsFragment;
import com.example.jonayskieprints.fragment.SettingsFragment;
import com.google.android.material.navigation.NavigationView;

public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView tvToolbarTitle;
    private TextView tvNotifBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // ── Set API base URL ──────────────────────────────────── ← ADDED
        ApiClient.get().setBaseUrl("https://jonayskieprints.vercel.app");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        tvNotifBadge = findViewById(R.id.tv_notif_badge);

        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Notification bell
        findViewById(R.id.btn_notifications).setOnClickListener(v ->
                showPendingOrdersNotification());

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new AdminHomeFragment(), "Dashboard");
            navView.setCheckedItem(R.id.nav_dashboard);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        String title = "Dashboard";

        int id = item.getItemId();
        if (id == R.id.nav_dashboard) {
            fragment = new AdminHomeFragment();
            title = "Dashboard";
        } else if (id == R.id.nav_orders) {
            fragment = new OrdersFragment();
            title = "Manage Orders";
        } else if (id == R.id.nav_customers) {
            fragment = new CustomersFragment();
            title = "Customers";
        } else if (id == R.id.nav_reports) {
            fragment = new ReportsFragment();
            title = "Reports";
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
            title = "Settings";
        } else if (id == R.id.nav_deleted) {
            fragment = new DeletedOrdersFragment();
            title = "Deleted Transactions";
        } else if (id == R.id.nav_logout) {
            handleLogout();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (fragment != null) {
            loadFragment(fragment, title);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment, String title) {
        tvToolbarTitle.setText(title);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void updateNotificationBadge(int count) {
        if (tvNotifBadge == null) return;
        if (count > 0) {
            tvNotifBadge.setVisibility(android.view.View.VISIBLE);
            tvNotifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            tvNotifBadge.setVisibility(android.view.View.GONE);
        }
    }

    private void showPendingOrdersNotification() {
        OrdersFragment fragment = new OrdersFragment();
        Bundle args = new Bundle();
        args.putString("filter", "pending");
        fragment.setArguments(args);
        loadFragment(fragment, "Manage Orders");
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setCheckedItem(R.id.nav_orders);
    }

    private void handleLogout() {
        getSharedPreferences("auth", MODE_PRIVATE).edit().clear().apply();
        startActivity(new android.content.Intent(this, LoginActivity.class));
        finish();
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