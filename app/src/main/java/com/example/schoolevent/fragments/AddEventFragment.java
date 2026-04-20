package com.example.schoolevent.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.MainActivity;
import com.example.schoolevent.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddEventFragment extends Fragment {

    private TextInputEditText etTitle, etDate, etCategory, etDescription;
    private MaterialButton btnSaveEvent, btnCancel;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_add_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanseState){
        super.onViewCreated(view, savedInstanseState);

        db = FirebaseFirestore.getInstance();

        etTitle = view.findViewById(R.id.et_title);
        etDate = view.findViewById(R.id.et_date);
        etCategory = view.findViewById(R.id.et_category);
        etDescription = view.findViewById(R.id.et_description);
        btnSaveEvent = view.findViewById(R.id.btn_save_event);
        btnCancel = view.findViewById(R.id.btn_cancel);
        progressBar = view.findViewById(R.id.progress_bar);

        btnSaveEvent.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String date = etTitle.getText().toString().trim();
            String category = etTitle.getText().toString().trim();
            String description = etTitle.getText().toString().trim();

            if(title.isEmpty() || date.isEmpty() ||
            category.isEmpty() || description.isEmpty()){
                Toast.makeText(getContext(),
                        "Semua field harus diisi",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            saveEvent(title, date, category, description);
        });

        btnCancel.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void saveEvent(String title, String date,
                           String category, String description ){
        progressBar.setVisibility(View.VISIBLE);
        btnSaveEvent.setEnabled(false);

        // Buat objek Event dengan ID kosong dulu
        // ID akan diisi otomatis oleh Firestore
        Event event = new Event("", title, date, category, description);

        // add() → simpan ke Firestore dengan ID otomatis
        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveEvent.setEnabled(true);

                    Toast.makeText(getContext(),
                            "Event berhasil ditambahkan",
                            Toast.LENGTH_SHORT).show();

                    //Kembali ke Home fragment
                    if(isAdded() && getActivity() != null){
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveEvent.setEnabled(true);
                    Toast.makeText(getContext(),
                            "Event gagal ditambahkan: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
