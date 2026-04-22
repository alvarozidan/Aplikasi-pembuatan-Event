package com.example.schoolevent.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole;
    private MaterialButton btnLogout;
    private SwitchMaterial switchDarkMode;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvName = view.findViewById(R.id.tv_name);
        tvEmail = view.findViewById(R.id.tv_email);
        tvRole = view.findViewById(R.id.tv_role);
        btnLogout = view.findViewById(R.id.btn_logout);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        loadUserProfile();

        int currentMode = AppCompatDelegate.getDefaultNightMode();
        switchDarkMode.setChecked(
                currentMode == AppCompatDelegate.MODE_NIGHT_YES
        );

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked)-> {
            //Simpan preferensi ke SharedPreference
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("setting", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("dark+mode", isChecked).apply();
            // .edit() → buka mode edit
            // .putBoolean() → simpan nilai boolean
            // .apply() → simpan secara async (tidak blocking UI)

            if(isChecked){
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                );
            }else{
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                );
            }
            //Activty akan recreate otomatis saat mode berubah
        });
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(),
                    "Berhasil LogOut",
                    Toast.LENGTH_SHORT).show();
            ((MainActivity)requireActivity()).updateNavbar();

            ((MainActivity) requireActivity()).loadFragment(new HomeFragment());
            ((MainActivity) requireActivity()).setNavbarSelected(R.id.nav_home);

            Toast.makeText(getContext(),
                    "Berhasil LogOut",
                    Toast.LENGTH_SHORT).show();

            //Ubah navbar Profile => login
            ((MainActivity) requireActivity()).updateNavbar();
            //Kembali ke Home
            ((MainActivity) requireActivity()).loadFragment(new HomeFragment());
        });
    }

    private void loadUserProfile(){
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null) return;

        //Pengambilan data
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String role = documentSnapshot.getString("role");

                        tvName.setText(name);
                        tvName.setText(email);

                        if("admin".equals(role)){
                            tvRole.setText("👑 Admin");
                            tvRole.setVisibility(View.VISIBLE);
                        }else{
                            tvRole.setText("🙍‍♂️ User");
                            tvRole.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Gagal memuat Profil",
                            Toast.LENGTH_SHORT).show();
                });
    }
}
