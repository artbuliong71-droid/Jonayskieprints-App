package com.example.jonayskieprints.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.activity.UserDashboardActivity;
import com.example.jonayskieprints.api.UserApiClient;
import com.example.jonayskieprints.model.UserProfile;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import okhttp3.MultipartBody;

public class UserProfileFragment extends Fragment {

    private static final int REQ_AVATAR = 201;

    // Views
    private TextView tvProfileName, tvProfileEmail, tvAvatarInitials;
    private ImageView ivAvatarImage;
    private TabLayout tabLayout;
    private LinearLayout llTabInfo, llTabPassword;

    // Info tab
    private TextInputEditText etFirstName, etLastName, etEmail, etPhone;
    private Button btnSaveInfo;

    // Password tab
    private TextInputEditText etCurrentPw, etNewPw, etConfirmPw;
    private LinearLayout llPwStrength;
    private View vBar1, vBar2, vBar3, vBar4;
    private TextView tvPwStrengthLabel, tvPwMatch;
    private Button btnClearPw, btnUpdatePw;

    private UserProfile currentUser;
    private boolean submitting = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupTabs();
        setupListeners();
        fetchUser();
    }

    private void bindViews(View v) {
        tvProfileName    = v.findViewById(R.id.tv_profile_name);
        tvProfileEmail   = v.findViewById(R.id.tv_profile_email);
        tvAvatarInitials = v.findViewById(R.id.tv_avatar_initials);
        ivAvatarImage    = v.findViewById(R.id.iv_avatar_image);
        tabLayout        = v.findViewById(R.id.tab_layout);
        llTabInfo        = v.findViewById(R.id.ll_tab_info);
        llTabPassword    = v.findViewById(R.id.ll_tab_password);

        etFirstName = v.findViewById(R.id.et_first_name);
        etLastName  = v.findViewById(R.id.et_last_name);
        etEmail     = v.findViewById(R.id.et_email);
        etPhone     = v.findViewById(R.id.et_phone);
        btnSaveInfo = v.findViewById(R.id.btn_save_info);

        etCurrentPw      = v.findViewById(R.id.et_current_pw);
        etNewPw          = v.findViewById(R.id.et_new_pw);
        etConfirmPw      = v.findViewById(R.id.et_confirm_pw);
        llPwStrength     = v.findViewById(R.id.ll_pw_strength);
        vBar1 = v.findViewById(R.id.v_bar1); vBar2 = v.findViewById(R.id.v_bar2);
        vBar3 = v.findViewById(R.id.v_bar3); vBar4 = v.findViewById(R.id.v_bar4);
        tvPwStrengthLabel= v.findViewById(R.id.tv_pw_strength_label);
        tvPwMatch        = v.findViewById(R.id.tv_pw_match);
        btnClearPw       = v.findViewById(R.id.btn_clear_pw);
        btnUpdatePw      = v.findViewById(R.id.btn_update_password);

        // Avatar click
        View flAvatar = v.findViewById(R.id.fl_avatar);
        if (flAvatar != null) flAvatar.setOnClickListener(x -> pickAvatar());
    }

    private void setupTabs() {
        if (tabLayout == null) return;
        tabLayout.addTab(tabLayout.newTab().setText("Personal Info"));
        tabLayout.addTab(tabLayout.newTab().setText("Change Password"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                boolean isInfo = tab.getPosition() == 0;
                if (llTabInfo != null) llTabInfo.setVisibility(isInfo ? View.VISIBLE : View.GONE);
                if (llTabPassword != null) llTabPassword.setVisibility(isInfo ? View.GONE : View.VISIBLE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        if (btnSaveInfo != null) btnSaveInfo.setOnClickListener(v -> saveInfo());
        if (btnClearPw != null) btnClearPw.setOnClickListener(v -> {
            if (etCurrentPw != null) etCurrentPw.setText("");
            if (etNewPw     != null) etNewPw.setText("");
            if (etConfirmPw != null) etConfirmPw.setText("");
        });
        if (btnUpdatePw != null) btnUpdatePw.setOnClickListener(v -> savePassword());

        // Password strength watcher
        if (etNewPw != null) etNewPw.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                updateStrength(s.toString());
                updateMatch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        if (etConfirmPw != null) etConfirmPw.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { updateMatch(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchUser() {
        UserApiClient.get().fetchUser(user -> {
            if (!isAdded() || user == null) return;
            currentUser = user;
            if (tvProfileName != null) tvProfileName.setText(user.getFullName().isEmpty() ? "User" : user.getFullName());
            if (tvProfileEmail != null) tvProfileEmail.setText(user.email != null ? user.email : "");
            String initials = user.getInitials();
            if (tvAvatarInitials != null) tvAvatarInitials.setText(initials.isEmpty() ? "U" : initials);

            if (etFirstName != null) etFirstName.setText(user.firstName);
            if (etLastName  != null) etLastName.setText(user.lastName);
            if (etEmail     != null) etEmail.setText(user.email);
            if (etPhone     != null) etPhone.setText(user.phone);
        });
    }

    private void saveInfo() {
        String fn = txt(etFirstName), ln = txt(etLastName), em = txt(etEmail), ph = txt(etPhone);
        if (fn.isEmpty() || ln.isEmpty()) { toast("First and last name are required."); return; }
        if (!em.contains("@"))            { toast("Valid email is required."); return; }
        if (submitting) return;
        submitting = true;
        btnSaveInfo.setText("Saving…"); btnSaveInfo.setEnabled(false);

        MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("update_profile", "1")
                .addFormDataPart("first_name", fn)
                .addFormDataPart("last_name",  ln)
                .addFormDataPart("email",      em)
                .addFormDataPart("phone",      ph)
                .build();

        UserApiClient.get().updateProfile(body, ok -> {
            if (!isAdded()) return;
            submitting = false;
            btnSaveInfo.setText("✓  Save Changes"); btnSaveInfo.setEnabled(true);
            if (ok) {
                toast("Profile updated!");
                if (tvProfileName != null) tvProfileName.setText(fn + " " + ln);
                if (getActivity() instanceof UserDashboardActivity)
                    ((UserDashboardActivity) getActivity()).updateHeaderUser(fn, (fn.charAt(0) + "" + ln.charAt(0)).toUpperCase());
            } else { toast("Update failed."); }
        });
    }

    private void savePassword() {
        String curr = txt(etCurrentPw), newPw = txt(etNewPw), conf = txt(etConfirmPw);
        if (curr.isEmpty() && newPw.isEmpty()) { toast("No password changes made."); return; }
        if (curr.isEmpty()) { toast("Current password is required."); return; }
        if (newPw.length() < 8) { toast("New password must be at least 8 characters."); return; }
        if (!newPw.equals(conf)) { toast("Passwords don't match."); return; }
        if (curr.equals(newPw)) { toast("New password must differ from current."); return; }
        if (submitting) return;
        submitting = true;
        btnUpdatePw.setText("Saving…"); btnUpdatePw.setEnabled(false);

        MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("update_profile",    "1")
                .addFormDataPart("first_name",        currentUser != null && currentUser.firstName != null ? currentUser.firstName : "")
                .addFormDataPart("last_name",         currentUser != null && currentUser.lastName  != null ? currentUser.lastName  : "")
                .addFormDataPart("email",             currentUser != null && currentUser.email     != null ? currentUser.email     : "")
                .addFormDataPart("current_password",  curr)
                .addFormDataPart("new_password",      newPw)
                .addFormDataPart("confirm_password",  conf)
                .build();

        UserApiClient.get().updateProfile(body, ok -> {
            if (!isAdded()) return;
            submitting = false;
            btnUpdatePw.setText("🔒  Update Password"); btnUpdatePw.setEnabled(true);
            if (ok) {
                toast("Password updated!");
                if (etCurrentPw != null) etCurrentPw.setText("");
                if (etNewPw     != null) etNewPw.setText("");
                if (etConfirmPw != null) etConfirmPw.setText("");
            } else { toast("Update failed. Check your current password."); }
        });
    }

    private void updateStrength(String pw) {
        if (pw.isEmpty()) { if (llPwStrength != null) llPwStrength.setVisibility(View.GONE); return; }
        if (llPwStrength != null) llPwStrength.setVisibility(View.VISIBLE);
        int s = 0;
        if (pw.length() >= 8) s++;
        if (pw.matches(".*[A-Z].*")) s++;
        if (pw.matches(".*[0-9].*")) s++;
        if (pw.matches(".*[^a-zA-Z0-9].*")) s++;
        int[] colors = {0xFFEF4444, 0xFFEF4444, 0xFFF59E0B, 0xFF3B82F6, 0xFF22C55E};
        String[] labels = {"", "Weak", "Weak", "Fair", "Strong"};
        int col = colors[s];
        View[] bars = {vBar1, vBar2, vBar3, vBar4};
        for (int i = 0; i < 4; i++) { if (bars[i] != null) bars[i].setBackgroundColor(i < s ? col : 0xFFE5E7EB); }
        if (tvPwStrengthLabel != null) { tvPwStrengthLabel.setText(labels[s]); tvPwStrengthLabel.setTextColor(col); }
    }

    private void updateMatch() {
        if (etNewPw == null || etConfirmPw == null || tvPwMatch == null) return;
        String np = txt(etNewPw), cp = txt(etConfirmPw);
        if (np.isEmpty() || cp.isEmpty()) { tvPwMatch.setVisibility(View.GONE); return; }
        tvPwMatch.setVisibility(View.VISIBLE);
        if (np.equals(cp)) { tvPwMatch.setText("✓ Passwords match");  tvPwMatch.setTextColor(Color.parseColor("#22C55E")); }
        else               { tvPwMatch.setText("✕ Passwords don't match"); tvPwMatch.setTextColor(Color.parseColor("#EF4444")); }
    }

    private void pickAvatar() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i, REQ_AVATAR);
    }

    @Override
    public void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_AVATAR && res == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (tvAvatarInitials != null) tvAvatarInitials.setVisibility(View.GONE);
            if (ivAvatarImage != null) {
                ivAvatarImage.setVisibility(View.VISIBLE);
                ivAvatarImage.setImageURI(uri);
            }
        }
    }

    private String txt(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }
    private void toast(String msg) { if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); }
}