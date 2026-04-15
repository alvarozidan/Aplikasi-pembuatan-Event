package com.example.schoolevent.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

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
}