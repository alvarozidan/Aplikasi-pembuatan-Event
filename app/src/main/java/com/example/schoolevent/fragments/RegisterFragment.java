package com.example.schoolevent.fragments;

import android.util.Log;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.MainActivity;
import com.example.schoolevent.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
public class RegisterFragment extends Fragment {

    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = view.findViewById(R.id.et_name);
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnRegister = view.findViewById(R.id.btn_register);
        tvLogin = view.findViewById(R.id.tv_login);
        progressBar = view.findViewById(R.id.progress_bar);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()){
                Toast.makeText(getContext(),
                        "Semua field harus terisi",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6){
                Toast.makeText(getContext(),
                        "Password minimal 6 karakter",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            registerUser(name, email, password);
        });

        tvLogin.setOnClickListener(v -> {
           requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void registerUser(String name, String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d("REGISTER", "Auth berhasil");
                        String userId = mAuth.getCurrentUser().getUid();
                        Log.d("REGISTER", "UserId: " + userId);

                        User user = new User(userId, name, email, "user");

                        db.collection("users")
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("REGISTER", "Firestore berhasil");
                                    progressBar.setVisibility(View.GONE);
                                    btnRegister.setEnabled(true);

                                    mAuth.signOut();

                                    Toast.makeText(getContext(),
                                            "Registrasi berhasil! Silakan login.",
                                            Toast.LENGTH_SHORT).show();

                                    if (isAdded() && getActivity() != null) {
                                        getParentFragmentManager()
                                                .popBackStack(null,
                                                        androidx.fragment.app.FragmentManager
                                                                .POP_BACK_STACK_INCLUSIVE);
                                        ((MainActivity) getActivity())
                                                .loadFragment(new LoginFragment());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("REGISTER", "Firestore gagal: " + e.getMessage());
                                    progressBar.setVisibility(View.GONE);
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Gagal simpan data: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Log.e("REGISTER", "Auth gagal: " + task.getException());
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registrasi gagal";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
