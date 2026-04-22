package com.example.schoolevent.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;

import android.os.Bundle;
import android.content.SharedPreferences;

import com.example.schoolevent.R;
import com.example.schoolevent.fragments.HomeFragment;
import com.example.schoolevent.fragments.LoginFragment;
import com.example.schoolevent.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


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

        mAuth = FirebaseAuth.getInstance();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        loadFragment(new HomeFragment());
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if( id == R.id.nav_home){
                loadFragment(new HomeFragment());
                return true;
            } else if ( id == R.id.nav_login) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null){
                    loadFragment(new ProfileFragment());
                }else {
                    loadFragment(new LoginFragment());
                }
                return true;
            }
            return false;
        });
    }

    public void loadFragment( Fragment fragment){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void updateNavbar(){
        FirebaseUser currentUser = mAuth.getCurrentUser();
        bottomNavigationView.getMenu().findItem(R.id.nav_login)
                .setTitle(currentUser != null ? "Profle" : "Login");
    }

//    Dipanggil saat app pertama dibuka → load preferensi tema
    private void loadThemePreference(){
        // SharedPreferences → penyimpanan data kecil secara lokal di HP
        // Seperti "catatan kecil" yang diingat app meski ditutup
        SharedPreferences prefs = getSharedPreferences("setting", MODE_PRIVATE);
        // getBoolean("dark_mode", false) → ambil nilai dark_mode
        // false = nilai default jika belum pernah disimpan
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        if(isDarkMode){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public void setNavbarSelected(int itemId){
        bottomNavigationView.setSelectedItemId(itemId);
    }
}