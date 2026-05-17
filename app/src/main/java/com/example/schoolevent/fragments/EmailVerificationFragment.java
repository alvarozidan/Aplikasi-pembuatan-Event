package com.example.schoolevent.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationFragment extends Fragment {

    private TextView tvDescription, tvVerifyStatus, tvCountdown, tvBackToRegister;
    private MaterialButton btnCheckVerification, btnResendEmail;
    private ProgressBar progressVerify;

    private FirebaseAuth mAuth;
    private CountDownTimer countDownTimer;
    private CountDownTimer autoCheckTimer;

    // Cooldown kirim ulang email (60 detik)
    private boolean canResend = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email_verification,
                container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        tvDescription = view.findViewById(R.id.tv_verify_description);
        tvVerifyStatus = view.findViewById(R.id.tv_verify_status);
        tvCountdown = view.findViewById(R.id.tv_countdown);
        tvBackToRegister = view.findViewById(R.id.tv_back_to_register);
        btnCheckVerification = view.findViewById(R.id.btn_check_verification);
        btnResendEmail = view.findViewById(R.id.btn_resend_email);
        progressVerify = view.findViewById(R.id.progress_verify);

        // Ambil email dari arguments
        String email = "";
        if (getArguments() != null) {
            email = getArguments().getString("email", "");
        }

        // Update deskripsi dengan email
        tvDescription.setText(
                "Kami telah mengirim link verifikasi ke\n"
                        + email
                        + "\n\nSilakan cek inbox atau folder spam kamu");

        // Tombol cek manual
        btnCheckVerification.setOnClickListener(v -> checkEmailVerified());

        // Tombol kirim ulang
        btnResendEmail.setOnClickListener(v -> resendVerificationEmail());

        // Kembali ke register
        tvBackToRegister.setOnClickListener(v -> {
            // Hapus akun yang belum terverifikasi
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.delete(); // hapus akun
            }
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Auto cek setiap 5 detik selama 30 detik
        startAutoCheck();
    }

    private void startAutoCheck() {
        // Progress bar countdown 30 detik
        progressVerify.setMax(30);
        progressVerify.setProgress(0);

        autoCheckTimer = new CountDownTimer(30000, 1000) {
            int seconds = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                seconds++;
                progressVerify.setProgress(seconds);
                tvCountdown.setText("Cek otomatis dalam "
                        + (30 - seconds) + " detik");

                // Cek setiap 5 detik
                if (seconds % 5 == 0) {
                    silentCheckVerification();
                }
            }

            @Override
            public void onFinish() {
                progressVerify.setProgress(30);
                tvCountdown.setText("Klik tombol di atas jika sudah verifikasi");
                // Cek sekali lagi saat timer selesai
                silentCheckVerification();
            }
        }.start();
    }

    // Cek tanpa notifikasi → untuk auto check
    private void silentCheckVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful() && user.isEmailVerified()) {
                // Email sudah terverifikasi!
                onVerificationSuccess();
            }
        });
    }

    // Cek dengan feedback → untuk tombol manual
    private void checkEmailVerified() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            // User belum login → login dulu untuk cek
            // Ini terjadi karena kita logout setelah register
            showLoginThenCheck();
            return;
        }

        btnCheckVerification.setEnabled(false);
        tvVerifyStatus.setText("🔄 Mengecek...");

        user.reload().addOnCompleteListener(task -> {
            btnCheckVerification.setEnabled(true);

            if (task.isSuccessful()) {
                if (user.isEmailVerified()) {
                    onVerificationSuccess();
                } else {
                    tvVerifyStatus.setText("⏳ Email belum diverifikasi");
                    Toast.makeText(getContext(),
                            "Email belum diverifikasi. Cek inbox kamu!",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                tvVerifyStatus.setText("❌ Gagal mengecek");
            }
        });
    }

    private void showLoginThenCheck() {
        // Karena user sudah logout setelah register,
        // kita perlu login ulang untuk cek status verifikasi
        // Tampilkan dialog input password
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Masukkan Password");
        builder.setMessage("Untuk mengecek status verifikasi, masukkan password kamu:");

        android.widget.EditText etPassword = new android.widget.EditText(
                requireContext());
        etPassword.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setHint("Password");
        builder.setView(etPassword);

        String email = "";
        if (getArguments() != null) {
            email = getArguments().getString("email", "");
        }
        final String finalEmail = email;

        builder.setPositiveButton("Cek", (dialog, which) -> {
            String password = etPassword.getText().toString().trim();
            if (password.isEmpty()) return;

            // Login sementara untuk cek
            mAuth.signInWithEmailAndPassword(finalEmail, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                onVerificationSuccess();
                            } else {
                                tvVerifyStatus.setText("⏳ Email belum diverifikasi");
                                Toast.makeText(getContext(),
                                        "Email belum diverifikasi!",
                                        Toast.LENGTH_SHORT).show();
                                // Logout lagi
                                mAuth.signOut();
                            }
                        } else {
                            Toast.makeText(getContext(),
                                    "Password salah",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void resendVerificationEmail() {
        if (!canResend) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(),
                    "Session expired, silakan register ulang",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnResendEmail.setEnabled(false);
        canResend = false;

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "Email verifikasi dikirim ulang!",
                                Toast.LENGTH_SHORT).show();

                        // Cooldown 60 detik sebelum bisa kirim lagi
                        startResendCooldown();
                    } else {
                        btnResendEmail.setEnabled(true);
                        canResend = true;
                        Toast.makeText(getContext(),
                                "Gagal kirim email",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startResendCooldown() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                btnResendEmail.setText(
                        "📨 Kirim Ulang (" + secondsLeft + "s)");
            }

            @Override
            public void onFinish() {
                btnResendEmail.setEnabled(true);
                btnResendEmail.setText("📨 Kirim Ulang Email");
                canResend = true;
            }
        }.start();
    }

    private void onVerificationSuccess() {
        // Hentikan semua timer
        if (autoCheckTimer != null) autoCheckTimer.cancel();
        if (countDownTimer != null) countDownTimer.cancel();

        tvVerifyStatus.setText("✅ Email berhasil diverifikasi!");
        progressVerify.setProgress(30);

        Toast.makeText(getContext(),
                "Email terverifikasi! Silakan login.",
                Toast.LENGTH_LONG).show();

        // Logout → arahkan ke LoginFragment
        mAuth.signOut();

        if (isAdded() && getActivity() != null) {
            requireActivity().getSupportFragmentManager()
                    .popBackStack(null,
                            androidx.fragment.app.FragmentManager
                                    .POP_BACK_STACK_INCLUSIVE);
            ((MainActivity) requireActivity())
                    .loadFragment(new LoginFragment());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hentikan timer saat fragment destroy
        if (autoCheckTimer != null) autoCheckTimer.cancel();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}