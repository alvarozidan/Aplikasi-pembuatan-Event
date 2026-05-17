package com.example.schoolevent.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.schoolevent.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FormReportFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";

    // Panggil ini untuk buat instance Bug atau Usulan
    public static FormReportFragment newInstance(String category) {
        FormReportFragment fragment = new FormReportFragment();
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
        return inflater.inflate(R.layout.fragment_form_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String category = getArguments() != null
                ? getArguments().getString(ARG_CATEGORY, "bug") : "bug";

        TextView tvDesc = view.findViewById(R.id.tv_report_description);
        TextInputLayout tilTitle = view.findViewById(R.id.til_title);
        TextInputLayout tilDetail = view.findViewById(R.id.til_detail);
        TextInputEditText etTitle = view.findViewById(R.id.et_report_title);
        TextInputEditText etDetail = view.findViewById(R.id.et_report_detail);
        MaterialButton btnSubmit = view.findViewById(R.id.btn_submit_report);

        // Set konten sesuai kategori
        if (category.equals("bug")) {
            tvDesc.setText("Laporkan bug atau masalah pada aplikasi");
            tilTitle.setHint("Judul Laporan");
            tilDetail.setHint("Deskripsikan masalah yang kamu temukan");
            btnSubmit.setText("Kirim Laporan");
        } else {
            tvDesc.setText("Usulkan event yang ingin diselenggarakan di sekolah");
            tilTitle.setHint("Nama Event yang Diusulkan");
            tilDetail.setHint("Deskripsikan event yang ingin kamu usulkan");
            btnSubmit.setText("Kirim Usulan");
        }

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String detail = etDetail.getText().toString().trim();

            if (title.isEmpty()) {
                tilTitle.setError("Judul tidak boleh kosong");
                return;
            }
            if (detail.isEmpty()) {
                tilDetail.setError("Detail tidak boleh kosong");
                return;
            }

            tilTitle.setError(null);
            tilDetail.setError(null);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(),
                        "Login dulu untuk mengirim", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);

            Map<String, Object> report = new HashMap<>();
            report.put("userId", currentUser.getUid());
            report.put("title", title);
            report.put("detail", detail);
            report.put("category", category);
            report.put("status", "pending");
            report.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("reports")
                    .add(report)
                    .addOnSuccessListener(ref -> {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Berhasil dikirim!", Toast.LENGTH_SHORT).show();
                        etTitle.setText(null);
                        etDetail.setText(null);
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Gagal: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }
}