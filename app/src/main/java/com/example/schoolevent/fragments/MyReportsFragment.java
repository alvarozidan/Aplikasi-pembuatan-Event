package com.example.schoolevent.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyReportsFragment extends Fragment {

    private MyReportAdapter adapter;
    private RecyclerView rvMyReports;
    private View layoutEmpty;
    private TextView tvDeleteAll, tvCount;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Map<String, Object>> reportList = new ArrayList<>();
    private List<String> reportIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Kamu perlu buat layout fragment_my_reports.xml (lihat bagian 3)
        return inflater.inflate(R.layout.fragment_my_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = view.findViewById(R.id.toolbar_my_reports);
        toolbar.setTitle("Laporan Saya");
        toolbar.setTitleTextColor(android.graphics.Color.WHITE);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        rvMyReports = view.findViewById(R.id.rv_my_reports);
        layoutEmpty = view.findViewById(R.id.layout_empty_my_reports);
        tvDeleteAll = view.findViewById(R.id.tv_delete_all_my_reports);
        tvCount = view.findViewById(R.id.tv_my_report_count);

        tvDeleteAll.setOnClickListener(v -> showDeleteAllDialog());

        rvMyReports.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyReportAdapter(reportList);
        rvMyReports.setAdapter(adapter);

        loadMyReports(adapter);
    }

    private void loadMyReports(MyReportAdapter adapter) {
        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("reports")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    reportList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        reportList.add(doc.getData());
                    }


                    // Urutkan terbaru dulu berdasarkan timestamp
                    reportList.sort((a, b) -> {
                        long tsA = a.get("timestamp") != null ? (long) a.get("timestamp") : 0;
                        long tsB = b.get("timestamp") != null ? (long) b.get("timestamp") : 0;
                        return Long.compare(tsB, tsA);
                    });

                    adapter.notifyDataSetChanged();

                    // Update count
                    tvCount.setText(reportList.size() + " laporan");

                    if (reportList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvMyReports.setVisibility(View.GONE);
                        tvDeleteAll.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvMyReports.setVisibility(View.VISIBLE);
                        tvDeleteAll.setVisibility(View.VISIBLE);
                    }
                });
    }

    // ===== ADAPTER =====
    class MyReportAdapter extends RecyclerView.Adapter<MyReportAdapter.VH> {

        private List<Map<String, Object>> list;

        MyReportAdapter(List<Map<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_report, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int pos) {
            Map<String, Object> data = list.get(pos);

            String title = data.get("title") != null ? data.get("title").toString() : "-";
            String category = data.get("category") != null ? data.get("category").toString() : "-";
            String status = data.get("status") != null ? data.get("status").toString() : "pending";
            long timestamp = data.get("timestamp") != null ? (long) data.get("timestamp") : 0;

            holder.tvTitle.setText(title);

            // Label kategori yang lebih ramah
            String categoryLabel = "bug".equals(category) ? "🐛 Bug / Masalah" : "💡 Usulan Event";
            holder.tvCategory.setText(categoryLabel);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(timestamp)));

            // Status badge — read only untuk user
            switch (status) {
                case "pending":
                    holder.tvStatus.setText("⏳ Menunggu");
                    holder.tvStatus.setBackgroundColor(0xFFFF9800);
                    break;
                case "reviewed":
                    holder.tvStatus.setText("👁 Sedang Ditinjau");
                    holder.tvStatus.setBackgroundColor(0xFF2196F3);
                    break;
                case "resolved":
                    holder.tvStatus.setText("✅ Selesai");
                    holder.tvStatus.setBackgroundColor(0xFF4CAF50);
                    break;
                default:
                    holder.tvStatus.setText(status);
                    holder.tvStatus.setBackgroundColor(0xFF9E9E9E);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCategory, tvStatus, tvTime;

            VH(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_my_report_title);
                tvCategory = v.findViewById(R.id.tv_my_report_category);
                tvStatus = v.findViewById(R.id.tv_my_report_status);
                tvTime = v.findViewById(R.id.tv_my_report_time);
            }
        }
    }
    private void showDeleteAllDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Hapus Semua Laporan")
                .setMessage("Yakin ingin menghapus semua laporanmu? Tindakan ini tidak bisa dibatalkan.")
                .setPositiveButton("Hapus Semua", (dialog, which) -> deleteAllMyReports())
                .setNegativeButton("Batal", null)
                .show();
    }
    private void deleteAllMyReports() {
        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("reports")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Tidak ada laporan untuk dihapus",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        batch.delete(db.collection("reports").document(doc.getId()));
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(),
                                        snapshots.size() + " laporan dihapus",
                                        Toast.LENGTH_SHORT).show();
                                reportList.clear();
                                adapter.notifyDataSetChanged();
                                tvCount.setText("0 laporan");
                                tvDeleteAll.setVisibility(View.GONE);
                                layoutEmpty.setVisibility(View.VISIBLE);
                                rvMyReports.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Gagal menghapus: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                });
    }
}