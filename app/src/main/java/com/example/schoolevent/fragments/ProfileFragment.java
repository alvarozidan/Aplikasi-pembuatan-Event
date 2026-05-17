package com.example.schoolevent.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.DetailActivity;
import com.example.schoolevent.activities.MainActivity;
import com.example.schoolevent.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole, tvLikedCount;
    private MaterialButton btnLogout, btnAddEventAdmin, btnManageEventAdmin, btnDeleteAccount;
    private SwitchMaterial switchDarkMode;
    private LinearLayout layoutAdminSection, containerLikedPreview, containerMyReportsPreview;
    private TextView tvReportCount, tvSuggestionCount, tvMyReportsSeeAll, tvMyReportsEmpty;
    private androidx.cardview.widget.CardView cardReports, cardSuggestions, cardLikedEvents, cardMyReports;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private String currentUserRole;
    private android.widget.ImageButton btnBack;

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
        layoutAdminSection = view.findViewById(R.id.layout_admin_section);
        tvReportCount = view.findViewById(R.id.tv_report_count);
        tvSuggestionCount = view.findViewById(R.id.tv_suggestion_count);
        btnBack = view.findViewById(R.id.btn_back);
        cardReports = view.findViewById(R.id.card_reports);
        cardSuggestions = view.findViewById(R.id.card_suggestions);
        cardLikedEvents = view.findViewById(R.id.card_liked_events);
        tvLikedCount = view.findViewById(R.id.tv_liked_count);
        containerLikedPreview = view.findViewById(R.id.container_liked_preview);
        cardMyReports = view.findViewById(R.id.card_my_reports);
        containerMyReportsPreview = view.findViewById(R.id.container_my_reports_preview);
        tvMyReportsSeeAll = view.findViewById(R.id.tv_my_reports_see_all);
        tvMyReportsEmpty = view.findViewById(R.id.tv_my_reports_empty);
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account);
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
        btnAddEventAdmin = view.findViewById(R.id.btn_add_event_admin);
        btnManageEventAdmin = view.findViewById(R.id.btn_manage_event_admin);
        btnAddEventAdmin.setOnClickListener(v ->
                ((MainActivity) requireActivity())
                        .loadFragment(new AddEventFragment()));

        btnManageEventAdmin.setOnClickListener(v ->
                ((MainActivity) requireActivity())
                        .loadFragment(new ManageEventFragment()));

        // Klik "Lihat Semua →" → buka MyReportsFragment
        tvMyReportsSeeAll.setOnClickListener(v ->
                ((MainActivity) requireActivity()).loadFragment(new MyReportsFragment()));

        swipeRefresh = view.findViewById(R.id.swipe_refresh_profile);
        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light
        );
        swipeRefresh.setOnRefreshListener(() -> {
            loadUserProfile();
            if ("admin".equals(currentUserRole)) {
                loadAdminDashboard();
            }
            swipeRefresh.setRefreshing(false);
        });

        btnBack.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).loadFragment(new HomeFragment());
            ((MainActivity) requireActivity()).setNavbarSelected(R.id.nav_home);
        });

        loadUserProfile();

        tvLikedCount.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).loadFragment(new LikedEventsFragment());
        });

        int currentMode = AppCompatDelegate.getDefaultNightMode();
        switchDarkMode.setChecked(
                currentMode == AppCompatDelegate.MODE_NIGHT_YES
        );

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked)-> {
            //Simpan preferensi ke SharedPreference
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("settings", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
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
            if(isAdded() && getActivity() instanceof MainActivity){
                ((MainActivity)requireActivity()).onThemeChanged();
            }
            //Activty akan recreate otomatis saat mode berubah
        });
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(),
                    "Berhasil LogOut",
                    Toast.LENGTH_SHORT).show();
            ((MainActivity)requireActivity()).updateNavbar();

            ((MainActivity) requireActivity()).setNavbarSelected(R.id.nav_home);
            ((MainActivity) requireActivity()).loadFragment(new HomeFragment());

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
                        currentUserRole = role;

                        tvName.setText(name);
                        tvName.setText(email);

                        if("admin".equals(role)){
                            tvRole.setText("👑 Admin");
                            tvRole.setVisibility(View.VISIBLE);
                            //Tampilkan dashboard admin
                            layoutAdminSection.setVisibility(View.VISIBLE);
                            loadAdminDashboard();
                        }else{
                            tvRole.setText("🙍‍♂️ User");
                            tvRole.setVisibility(View.VISIBLE);
                            layoutAdminSection.setVisibility(View.GONE);
                        }
                        loadLikedEventsPreview();
                        loadMyReportsPreview();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Gagal memuat Profil",
                            Toast.LENGTH_SHORT).show();
                });

    }
    private void loadAdminDashboard() {
        // Hitung laporan bug
        db.collection("reports")
                .whereEqualTo("category", "bug")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    tvReportCount.setText(String.valueOf(snap.size()));
                });

        // Hitung usulan event
        db.collection("reports")
                .whereEqualTo("category", "event_suggestion")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    tvSuggestionCount.setText(String.valueOf(snap.size()));
                });

        // Klik card laporan → buka halaman detail laporan
        cardReports.setOnClickListener(v -> {
            ReportDetailFragment fragment = ReportDetailFragment.newInstance("bug");
            ((MainActivity) requireActivity()).loadFragment(fragment);
        });

        // Klik card usulan → buka halaman detail usulan
        cardSuggestions.setOnClickListener(v -> {
            ReportDetailFragment fragment =
                    ReportDetailFragment.newInstance("event_suggestion");
            ((MainActivity) requireActivity()).loadFragment(fragment);
        });
    }

    private void loadLikedEventsPreview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        db.collection("events")
                .whereArrayContains("likes", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();

                    if (count == 0) {
                        cardLikedEvents.setVisibility(View.GONE);
                        return;
                    }

                    cardLikedEvents.setVisibility(View.VISIBLE);
                    tvLikedCount.setText(count + " event →");

                    // Tampilkan preview maksimal 3 event
                    containerLikedPreview.removeAllViews();
                    int previewCount = Math.min(count, 3);
                    int i = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (i >= previewCount) break;

                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());

                        // Buat view preview sederhana
                        View previewView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_liked_preview,
                                        containerLikedPreview, false);

                        TextView tvTitle = previewView.findViewById(
                                R.id.tv_liked_title);
                        TextView tvCategory = previewView.findViewById(
                                R.id.tv_liked_category);

                        tvTitle.setText(event.getTitle());
                        tvCategory.setText(event.getCategory());

                        final Event currentEvent = event;
                        previewView.setOnClickListener(v -> {
                            Intent intent = new Intent(getContext(),
                                    DetailActivity.class);
                            intent.putExtra("event_id", currentEvent.getEventId());
                            intent.putExtra("event_title", currentEvent.getTitle());
                            intent.putExtra("event_date", currentEvent.getDateDisplay());
                            intent.putExtra("event_category", currentEvent.getCategory());
                            intent.putExtra("event_description",
                                    currentEvent.getDescription());
                            startActivity(intent);
                        });

                        containerLikedPreview.addView(previewView);
                        i++;
                    }

                    // Jika lebih dari 3 → tampilkan "dan X lainnya"
                    if (count > 3) {
                        TextView tvMore = new TextView(getContext());
                        tvMore.setText("dan " + (count - 3) + " event lainnya...");
                        tvMore.setTextSize(12);
                        tvMore.setTextColor(
                                getContext().getColor(android.R.color.darker_gray));
                        tvMore.setPadding(0, 8, 0, 0);
                        containerLikedPreview.addView(tvMore);
                    }
                });
    }

    private void loadMyReportsPreview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        db.collection("reports")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        // Tetap tampilkan card tapi isi teks kosong
                        cardMyReports.setVisibility(View.VISIBLE);
                        tvMyReportsEmpty.setVisibility(View.VISIBLE);
                        containerMyReportsPreview.setVisibility(View.GONE);
                        return;
                    }

                    cardMyReports.setVisibility(View.VISIBLE);
                    tvMyReportsEmpty.setVisibility(View.GONE);
                    containerMyReportsPreview.setVisibility(View.VISIBLE);
                    containerMyReportsPreview.removeAllViews();

                    // Urutkan terbaru dulu
                    List<QueryDocumentSnapshot> docs =
                            new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                        docs.add(doc);
                    }
                    docs.sort((a, b) -> {
                        long tsA = a.getLong("timestamp") != null ? a.getLong("timestamp") : 0;
                        long tsB = b.getLong("timestamp") != null ? b.getLong("timestamp") : 0;
                        return Long.compare(tsB, tsA);
                    });

                    // Tampilkan maksimal 2 preview
                    int previewCount = Math.min(docs.size(), 2);
                    for (int i = 0; i < previewCount; i++) {
                        com.google.firebase.firestore.QueryDocumentSnapshot doc = docs.get(i);
                        String title    = doc.getString("title") != null ? doc.getString("title") : "-";
                        String status   = doc.getString("status") != null ? doc.getString("status") : "pending";
                        String category = doc.getString("category") != null ? doc.getString("category") : "";

                        // Inflate item preview sederhana (bisa pakai item_my_report atau layout kustom)
                        View previewView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_my_report, containerMyReportsPreview, false);

                        TextView tvTitle    = previewView.findViewById(R.id.tv_my_report_title);
                        TextView tvCat      = previewView.findViewById(R.id.tv_my_report_category);
                        TextView tvStatus   = previewView.findViewById(R.id.tv_my_report_status);
                        TextView tvTime     = previewView.findViewById(R.id.tv_my_report_time);

                        tvTitle.setText(title);
                        tvCat.setText("bug".equals(category) ? "🐛 Bug / Masalah" : "💡 Usulan Event");

                        long ts = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                        tvTime.setText(new java.text.SimpleDateFormat(
                                "dd MMM yyyy", java.util.Locale.getDefault())
                                .format(new java.util.Date(ts)));

                        switch (status) {
                            case "pending":
                                tvStatus.setText("⏳ Menunggu");
                                tvStatus.setBackgroundColor(0xFFFF9800);
                                break;
                            case "reviewed":
                                tvStatus.setText("👁 Ditinjau");
                                tvStatus.setBackgroundColor(0xFF2196F3);
                                break;
                            case "resolved":
                                tvStatus.setText("✅ Selesai");
                                tvStatus.setBackgroundColor(0xFF4CAF50);
                                break;
                        }

                        containerMyReportsPreview.addView(previewView);
                    }

                    // Jika lebih dari 2, tampilkan hint
                    if (docs.size() > 2) {
                        TextView tvMore = new TextView(getContext());
                        tvMore.setText("+" + (docs.size() - 2) + " laporan lainnya → Lihat Semua");
                        tvMore.setTextSize(12);
                        tvMore.setTextColor(getContext().getColor(android.R.color.darker_gray));
                        tvMore.setPadding(0, 8, 0, 0);
                        tvMore.setOnClickListener(v ->
                                ((MainActivity) requireActivity()).loadFragment(new MyReportsFragment()));
                        containerMyReportsPreview.addView(tvMore);
                    }
                });
    }

    private void confirmDeleteAccount() {
        // Dialog konfirmasi pertama
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Hapus Akun")
                .setMessage("Akun kamu akan dihapus permanen beserta semua data terkait.\n\nAksi ini tidak dapat dibatalkan!")
                .setPositiveButton("Lanjutkan", (dialog, which) -> {
                    // Minta password untuk konfirmasi
                    showPasswordConfirmation();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showPasswordConfirmation() {
        // Input password untuk re-authentikasi
        // Firebase wajib re-auth sebelum hapus akun
        android.widget.EditText etPassword = new android.widget.EditText(requireContext());
        etPassword.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setHint("Masukkan password kamu");

        // Padding untuk EditText di dalam dialog
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 0);
        container.addView(etPassword);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Password")
                .setMessage("Masukkan password untuk mengkonfirmasi penghapusan akun:")
                .setView(container)
                .setPositiveButton("Hapus Akun", (dialog, which) -> {
                    String password = etPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Password tidak boleh kosong",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    deleteAccount(password);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteAccount(String password) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String email = currentUser.getEmail();

        // Tampilkan loading
        btnDeleteAccount.setEnabled(false);
        btnDeleteAccount.setText("Menghapus...");

        // Step 1: Re-authentikasi dulu (wajib Firebase sebelum hapus akun)
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider
                        .getCredential(email, password);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(authTask -> {
                    if (!authTask.isSuccessful()) {
                        // Re-auth gagal → password salah
                        btnDeleteAccount.setEnabled(true);
                        btnDeleteAccount.setText("Hapus Akun");
                        Toast.makeText(getContext(),
                                "Password salah! Coba lagi.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Step 2: Re-auth berhasil → hapus data Firestore dulu
                    String uid = currentUser.getUid();

                    deleteUserData(uid, currentUser);
                });
    }

    private void deleteUserData(String uid, FirebaseUser currentUser) {
        // Hapus semua data user dari Firestore secara berurutan
        // 1. Hapus dokumen user dari collection "users"
        db.collection("users")
                .document(uid)
                .delete()
                .addOnCompleteListener(task -> {
                    // 2. Hapus semua komentar milik user
                    db.collection("comments")
                            .whereEqualTo("userId", uid)
                            .get()
                            .addOnSuccessListener(commentSnapshots -> {
                                com.google.firebase.firestore.WriteBatch batch =
                                        db.batch();
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc
                                        : commentSnapshots) {
                                    batch.delete(doc.getReference());
                                }

                                // 3. Hapus semua laporan milik user
                                db.collection("reports")
                                        .whereEqualTo("userId", uid)
                                        .get()
                                        .addOnSuccessListener(reportSnapshots -> {
                                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc
                                                    : reportSnapshots) {
                                                batch.delete(doc.getReference());
                                            }

                                            // Eksekusi semua penghapusan sekaligus
                                            batch.commit()
                                                    .addOnCompleteListener(batchTask -> {
                                                        // 4. Hapus akun dari Firebase Auth
                                                        currentUser.delete()
                                                                .addOnCompleteListener(deleteTask -> {
                                                                    if (deleteTask.isSuccessful()) {
                                                                        onAccountDeleted();
                                                                    } else {
                                                                        btnDeleteAccount.setEnabled(true);
                                                                        btnDeleteAccount.setText("Hapus Akun");
                                                                        Toast.makeText(getContext(),
                                                                                "Gagal hapus akun: "
                                                                                        + deleteTask.getException()
                                                                                        .getMessage(),
                                                                                Toast.LENGTH_LONG).show();
                                                                    }
                                                                });
                                                    });
                                        });
                            });
                });
    }

    private void onAccountDeleted() {
        Toast.makeText(getContext(),
                "Akun berhasil dihapus",
                Toast.LENGTH_LONG).show();

        // Pindah ke HomeFragment sebagai guest
        if (isAdded() && getActivity() != null) {
            ((MainActivity) requireActivity()).updateNavbar();
            ((MainActivity) requireActivity())
                    .setNavbarSelected(R.id.nav_home);
            ((MainActivity) requireActivity())
                    .loadFragment(new HomeFragment());
        }
    }
}
