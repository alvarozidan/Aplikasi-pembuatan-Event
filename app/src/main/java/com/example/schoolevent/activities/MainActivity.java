package com.example.schoolevent.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;
import  androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.os.Build;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.example.schoolevent.R;
import com.example.schoolevent.fragments.AddEventFragment;
import com.example.schoolevent.fragments.CalendarFragment;
import com.example.schoolevent.fragments.CategoryDetailFragment;
import com.example.schoolevent.fragments.CategoryFragment;
import com.example.schoolevent.fragments.HomeFragment;
import com.example.schoolevent.fragments.LikedEventsFragment;
import com.example.schoolevent.fragments.LoginFragment;
import com.example.schoolevent.fragments.ManageEventFragment;
import com.example.schoolevent.fragments.MyReportsFragment;
import com.example.schoolevent.fragments.ProfileFragment;
import com.example.schoolevent.fragments.RegisterFragment;
import com.example.schoolevent.fragments.ReportDetailFragment;
import com.example.schoolevent.fragments.ReportFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderScriptBlur;


public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load tema dulu sebelum layout dibuat
        // agar tidak ada flicker/kedip saat app dibuka
        loadThemePreference();
        setContentView(R.layout.activity_main);
        setupBlurNavbar();
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Listener backstack → update navbar saat tekan back
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (current == null) return;

            updateNavbarSelection(current);
            updateNavbarVisibility(current);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;

            } else if (id == R.id.nav_calendar) {
                loadFragment(new CalendarFragment());
                return true;

            } else if (id == R.id.nav_category) {
                loadFragment(new CategoryFragment());
                return true;

            } else if (id == R.id.nav_report) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    loadFragment(new ReportFragment());
                    return true;
                } else {
                    Toast.makeText(this,
                            "Login dulu untuk mengirim laporan",
                            Toast.LENGTH_SHORT).show();
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    return false;
                }
            }
            return false;
        });

        // Jika savedInstanceState != null → activity di-recreate (dark mode toggle)
        // Fragment sudah ada, tidak perlu load ulang
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        checkLoginState();
    }

    public void loadFragment( Fragment fragment ){

        // Cek apakah fragment yang aktif sama dengan yang akan di-load
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        // Jika sama → skip, tidak perlu load ulang
        if (currentFragment != null
                && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        // Update navbar visibility sesuai fragment
        updateNavbarVisibility(fragment);
    }

    // Update highlight navbar sesuai fragment aktif
    private void updateNavbarSelection(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (fragment instanceof CalendarFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
        } else if (fragment instanceof CategoryFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_category);
        } else if (fragment instanceof ReportFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_report);
        }
    }

    // Sembunyikan navbar untuk halaman tertentu
    private void updateNavbarVisibility(Fragment fragment) {
        View navbarWrapper = findViewById(R.id.navbar_wrapper);
        if (navbarWrapper == null) return;

        // Sembunyikan navbar di halaman ini
        boolean hideNavbar = fragment instanceof ReportDetailFragment
                || fragment instanceof ProfileFragment
                || fragment instanceof LoginFragment
                || fragment instanceof RegisterFragment
                || fragment instanceof AddEventFragment
                || fragment instanceof ManageEventFragment
                || fragment instanceof CategoryDetailFragment
                || fragment instanceof LikedEventsFragment
                || fragment instanceof MyReportsFragment;

        navbarWrapper.setVisibility(hideNavbar ? View.GONE : View.VISIBLE);
    }

    public void updateNavbar() {
        // Refresh fragment yang sedang aktif
        // agar tombol profil di toolbar terupdate sesuai status login
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (current != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .detach(current)
                    .attach(current)
                    .commit();
        }
    }

    public void onThemeChanged(){
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        loadFragment(new HomeFragment());
    }
    public void setNavbarSelected(int itemId){
        bottomNavigationView.setSelectedItemId(itemId);
    }


//    Dipanggil saat app pertama dibuka → load preferensi tema
    private void loadThemePreference(){
        // SharedPreferences → penyimpanan data kecil secara lokal di HP
        // Seperti "catatan kecil" yang diingat app meski ditutup
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        // getBoolean("dark_mode", false) → ambil nilai dark_mode
        // false = nilai default jika belum pernah disimpan
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        if(isDarkMode){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    private void checkLoginState(){
        FirebaseUser currentuser = mAuth.getCurrentUser();
        if(currentuser != null){
            updateNavbar();
        }
    }

    @Override
    public void onBackPressed() {
        // Jika ada fragment di backstack → pop dulu
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            // Backstack kosong → keluar app
            super.onBackPressed();
        }
    }

    private void setupBlurNavbar() {
        BlurView blurNavbar = findViewById(R.id.blur_navbar);
        View decorView = getWindow().getDecorView();
        ViewGroup rootView = decorView.findViewById(android.R.id.content);

        blurNavbar.setupWith(rootView, new RenderScriptBlur(this))
                .setBlurRadius(15f)
                .setBlurAutoUpdate(true);
    }

    public void hideNavbar() {
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
    }

    public void showNavbar() {
        findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
    }

}