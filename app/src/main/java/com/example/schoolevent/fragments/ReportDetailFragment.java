package com.example.schoolevent.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.schoolevent.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportDetailFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";

    private RecyclerView rvReports;
    private TextView tvCount;
    private View layoutEmpty;
    private ChipGroup chipGroup;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvClearAll; // tombol clear all

    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private ReportAdapter reportAdapter;

    // Menyimpan SEMUA data dari Firestore
    private List<Map<String, Object>> allReports = new ArrayList<>();
    private List<String> allReportIds = new ArrayList<>();

    // Data yang sedang ditampilkan (hasil filter)
    private List<Map<String, Object>> filteredReports = new ArrayList<>();
    private List<String> filteredReportIds = new ArrayList<>();

    private String category;
    private String currentFilter = "all";

    interface OnStatusChangeListener {
        void onStatusChange(String reportId, String newStatus);
    }

    public static ReportDetailFragment newInstance(String category) {
        ReportDetailFragment fragment = new ReportDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
        }

        db = FirebaseFirestore.getInstance();

        // Setup Toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar_report_detail);
        toolbar.setTitle("bug".equals(category) ? "Laporan Bug" : "Usulan Event");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        // Set warna judul toolbar putih
        toolbar.setTitleTextColor(android.graphics.Color.WHITE);

        tvCount = view.findViewById(R.id.tv_count);
        rvReports = view.findViewById(R.id.rv_reports);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        chipGroup = view.findViewById(R.id.chip_group);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_report_detail);
        tvClearAll = view.findViewById(R.id.tv_clear_all);

        // Setup SwipeRefresh
        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light);
        swipeRefresh.setOnRefreshListener(() -> loadAllReports());

        // Setup RecyclerView
        rvReports.setLayoutManager(new LinearLayoutManager(getContext()));
        reportAdapter = new ReportAdapter(filteredReports, filteredReportIds,
                (reportId, newStatus) -> updateReportStatus(reportId, newStatus));
        rvReports.setAdapter(reportAdapter);

        // Setup chip filter
        setupChips();

        // Setup tombol Clear All
        setupClearAll();

        // Load semua data sekali dari Firestore
        loadAllReports();
    }

    private void setupChips() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_all) {
                currentFilter = "all";
            } else if (checkedId == R.id.chip_pending) {
                currentFilter = "pending";
            } else if (checkedId == R.id.chip_reviewed) {
                currentFilter = "reviewed";
            } else if (checkedId == R.id.chip_resolved) {
                currentFilter = "resolved";
            }

            // Filter data lokal → tidak perlu request Firestore lagi
            applyFilter();

            // Tampilkan/sembunyikan tombol Clear All
            // hanya muncul saat filter "resolved"
            if (tvClearAll != null) {
                tvClearAll.setVisibility(
                        "resolved".equals(currentFilter)
                                ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setupClearAll() {
        if (tvClearAll == null) return;

        // Sembunyikan dulu
        tvClearAll.setVisibility(View.GONE);

        tvClearAll.setOnClickListener(v -> {
            // Konfirmasi sebelum hapus semua yang resolved
            new AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Semua")
                    .setMessage("Yakin ingin menghapus semua laporan yang sudah selesai?")
                    .setPositiveButton("Hapus Semua", (dialog, which) -> {
                        clearAllResolved();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    private void clearAllResolved() {
        // Ambil semua ID yang resolved
        List<String> resolvedIds = new ArrayList<>();
        for (int i = 0; i < allReports.size(); i++) {
            Map<String, Object> data = allReports.get(i);
            String status = data.get("status") != null
                    ? data.get("status").toString() : "";
            if ("resolved".equals(status)) {
                resolvedIds.add(allReportIds.get(i));
            }
        }

        if (resolvedIds.isEmpty()) {
            Toast.makeText(getContext(),
                    "Tidak ada laporan selesai",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Hapus satu per satu menggunakan batch
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (String id : resolvedIds) {
            batch.delete(db.collection("reports").document(id));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            resolvedIds.size() + " laporan dihapus",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Gagal: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllReports() {
        if (listener != null) listener.remove();

        // Ambil semua laporan berdasarkan category saja
        // Filter status dilakukan secara lokal
        // Ini menghindari kebutuhan Firestore composite index
        listener = db.collection("reports")
                .whereEqualTo("category", category)
                .addSnapshotListener((snapshots, error) -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                    if (error != null) {
                        Toast.makeText(getContext(),
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    allReports.clear();
                    allReportIds.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        allReports.add(doc.getData());
                        allReportIds.add(doc.getId());
                    }

                    // Urutkan berdasarkan timestamp descending (terbaru dulu)
                    // Dilakukan lokal karena kita tidak pakai orderBy di query
                    sortByTimestamp();

                    // Terapkan filter yang sedang aktif
                    applyFilter();
                });
    }

    private void sortByTimestamp() {
        // Zip allReports dan allReportIds agar tetap sinkron saat diurutkan
        List<Map.Entry<String, Map<String, Object>>> combined = new ArrayList<>();
        for (int i = 0; i < allReports.size(); i++) {
            combined.add(new java.util.AbstractMap.SimpleEntry<>(
                    allReportIds.get(i), allReports.get(i)));
        }

        // Urutkan descending berdasarkan timestamp
        combined.sort((a, b) -> {
            long tsA = a.getValue().get("timestamp") != null
                    ? (long) a.getValue().get("timestamp") : 0;
            long tsB = b.getValue().get("timestamp") != null
                    ? (long) b.getValue().get("timestamp") : 0;
            return Long.compare(tsB, tsA); // descending
        });

        // Kembalikan ke list terpisah
        allReports.clear();
        allReportIds.clear();
        for (Map.Entry<String, Map<String, Object>> entry : combined) {
            allReportIds.add(entry.getKey());
            allReports.add(entry.getValue());
        }
    }

    private void applyFilter() {
        // Filter data lokal berdasarkan currentFilter
        filteredReports.clear();
        filteredReportIds.clear();

        for (int i = 0; i < allReports.size(); i++) {
            Map<String, Object> data = allReports.get(i);
            String status = data.get("status") != null
                    ? data.get("status").toString() : "pending";

            // "all" → tampilkan semua
            // filter lain → hanya tampilkan yang statusnya cocok
            if ("all".equals(currentFilter) || currentFilter.equals(status)) {
                filteredReports.add(data);
                filteredReportIds.add(allReportIds.get(i));
            }
        }

        // Update UI
        reportAdapter.notifyDataSetChanged();

        String label = "bug".equals(category) ? "laporan" : "usulan";
        tvCount.setText(filteredReports.size() + " " + label);

        if (filteredReports.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvReports.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvReports.setVisibility(View.VISIBLE);
        }
    }

    private void updateReportStatus(String reportId, String newStatus) {
        db.collection("reports")
                .document(reportId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(),
                                "Status diperbarui ke: " + newStatus,
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Gagal: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }

    // =================== ADAPTER ===================
    class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

        private List<Map<String, Object>> list;
        private List<String> ids;
        private OnStatusChangeListener statusListener;

        ReportAdapter(List<Map<String, Object>> list, List<String> ids,
                      OnStatusChangeListener statusListener) {
            this.list = list;
            this.ids = ids;
            this.statusListener = statusListener;
        }

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int pos) {
            Map<String, Object> data = list.get(pos);
            String id = ids.get(pos);

            String title = data.get("title") != null
                    ? data.get("title").toString() : "";
            String detail = data.get("detail") != null
                    ? data.get("detail").toString() : "";
            String status = data.get("status") != null
                    ? data.get("status").toString() : "pending";
            long timestamp = data.get("timestamp") != null
                    ? (long) data.get("timestamp") : 0;

            holder.tvTitle.setText(title);
            holder.tvDetail.setText(detail);

            // Ambil field tambahan
            String type       = data.get("type") != null ? data.get("type").toString() : "";
            String eventTitle = data.get("eventTitle") != null ? data.get("eventTitle").toString() : "";

            // Tampilkan info event hanya jika ini laporan komentar
            if ("comment_report".equals(type) && !eventTitle.isEmpty()) {
                holder.tvEventRef.setVisibility(View.VISIBLE);
                holder.tvEventRef.setText("📌 Dari event: " + eventTitle);
            } else {
                holder.tvEventRef.setVisibility(View.GONE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy, HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(timestamp)));

            // Status badge dengan rounded background
            switch (status) {
                case "pending":
                    holder.tvStatus.setText("⏳ Menunggu");
                    holder.tvStatus.setBackgroundColor(0xFFFF9800);
                    break;
                case "reviewed":
                    holder.tvStatus.setText("👁 Ditinjau");
                    holder.tvStatus.setBackgroundColor(0xFF2196F3);
                    break;
                case "resolved":
                    holder.tvStatus.setText("✅ Selesai");
                    holder.tvStatus.setBackgroundColor(0xFF4CAF50);
                    break;
            }

            // Reset listener dulu
            holder.btnReviewed.setOnClickListener(null);
            holder.btnResolved.setOnClickListener(null);

            holder.btnReviewed.setOnClickListener(v ->
                    statusListener.onStatusChange(id, "reviewed"));
            holder.btnResolved.setOnClickListener(v ->
                    statusListener.onStatusChange(id, "resolved"));

            // Sembunyikan tombol jika sudah resolved
            if ("resolved".equals(status)) {
                holder.btnReviewed.setVisibility(View.GONE);
                holder.btnResolved.setVisibility(View.GONE);
            } else if ("reviewed".equals(status)) {
                // Sudah ditinjau → hanya tampilkan tombol Selesai
                holder.btnReviewed.setVisibility(View.GONE);
                holder.btnResolved.setVisibility(View.VISIBLE);
            } else {
                // Pending → tampilkan kedua tombol
                holder.btnReviewed.setVisibility(View.VISIBLE);
                holder.btnResolved.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ReportViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDetail, tvStatus, tvTime, tvEventRef;
            com.google.android.material.button.MaterialButton btnReviewed, btnResolved;

            ReportViewHolder(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_report_title);
                tvDetail = v.findViewById(R.id.tv_report_detail);
                tvStatus = v.findViewById(R.id.tv_status_badge);
                tvTime = v.findViewById(R.id.tv_report_time);
                tvEventRef = v.findViewById(R.id.tv_event_ref);
                btnReviewed = v.findViewById(R.id.btn_mark_reviewed);
                btnResolved = v.findViewById(R.id.btn_mark_resolved);
            }
        }
    }
}