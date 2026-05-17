package com.example.schoolevent.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.schoolevent.R;
import com.example.schoolevent.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEventFragment extends Fragment {

    private TextInputEditText etTitle, etDate, etDescription;
    private MaterialButton btnSaveEvent;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private AutoCompleteTextView etCategory;

    String[] categories = {
            "Akademik",
            "Olahraga",
            "Seni & Budaya",
            "Ekstrakurikuler",
            "OSIS",
            "Kesehatan",
            "Festival",
            "Lainnya"
    };

    private FirebaseFirestore db;
    private String selectedDateForDb = ""; // format yyyy-MM-dd untuk Firestore & kalender
    private Event eventToEdit = null;      // null = mode create, ada isi = mode edit

    // Static factory method → untuk mode edit, membawa data event lama
    public static AddEventFragment newInstance(Event event) {
        AddEventFragment fragment = new AddEventFragment();
        Bundle args = new Bundle();
        args.putString("event_id", event.getEventId());
        args.putString("event_title", event.getTitle());
        args.putString("event_date", event.getDate());
        args.putString("event_date_display", event.getDateDisplay());
        args.putString("event_category", event.getCategory());
        args.putString("event_description", event.getDescription());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        toolbar = view.findViewById(R.id.toolbar_add);
        etTitle = view.findViewById(R.id.et_title);
        etDate = view.findViewById(R.id.et_date);
        etDescription = view.findViewById(R.id.et_description);
        btnSaveEvent = view.findViewById(R.id.btn_save_event);
        progressBar = view.findViewById(R.id.progress_bar);
        etCategory = view.findViewById(R.id.et_category);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        etCategory.setAdapter(categoryAdapter);

        // Setup tombol back di toolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // Cek apakah mode edit
        if (getArguments() != null) {
            eventToEdit = new Event();
            eventToEdit.setEventId(getArguments().getString("event_id"));
            eventToEdit.setTitle(getArguments().getString("event_title"));
            eventToEdit.setDate(getArguments().getString("event_date"));
            eventToEdit.setDateDisplay(getArguments().getString("event_date_display"));
            eventToEdit.setCategory(getArguments().getString("event_category"));
            eventToEdit.setDescription(getArguments().getString("event_description"));

            if(eventToEdit != null){
                etCategory.setText(eventToEdit.getCategory(), false);
            }

            // Isi form dengan data lama
            toolbar.setTitle("Edit Event");
            btnSaveEvent.setText("Update Event");
            etTitle.setText(eventToEdit.getTitle());
            etDate.setText(eventToEdit.getDateDisplay());
            etCategory.setText(eventToEdit.getCategory());
            etDescription.setText(eventToEdit.getDescription());
            selectedDateForDb = eventToEdit.getDate() != null
                    ? eventToEdit.getDate() : "";
        }

        // Klik field tanggal → buka DatePickerDialog
        etDate.setOnClickListener(v -> showDatePicker());

        btnSaveEvent.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String dateDisplay = etDate.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (title.isEmpty() || dateDisplay.isEmpty()
                    || category.isEmpty() || description.isEmpty()) {
                Toast.makeText(getContext(),
                        "Semua field harus diisi",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedDateForDb.isEmpty()) {
                Toast.makeText(getContext(),
                        "Pilih tanggal terlebih dahulu",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            saveEvent(title, dateDisplay, category, description);
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Jika mode edit → set kalender ke tanggal event lama
        if (!selectedDateForDb.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "yyyy-MM-dd", Locale.getDefault());
                calendar.setTime(sdf.parse(selectedDateForDb));
            } catch (Exception ignored) {}
        }

        new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, day) -> {
                    // Simpan format yyyy-MM-dd untuk filter kalender
                    selectedDateForDb = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, day);

                    // Tampilkan format Indonesia di field
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day);
                    SimpleDateFormat displayFormat = new SimpleDateFormat(
                            "dd MMMM yyyy", new Locale("id", "ID"));
                    etDate.setText(displayFormat.format(selected.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveEvent(String title, String dateDisplay,
                           String category, String description) {
        progressBar.setVisibility(View.VISIBLE);
        btnSaveEvent.setEnabled(false);

        // imageUrl dikosongkan karena fitur gambar dihapus
        Event event = new Event("", title, selectedDateForDb,
                dateDisplay, category, description, "");

        if (eventToEdit != null) {
            // Mode edit → update dokumen yang sudah ada
            db.collection("events")
                    .document(eventToEdit.getEventId())
                    .set(event)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Event berhasil diupdate!",
                                Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnSaveEvent.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Gagal update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Mode create → tambah dokumen baru
            db.collection("events")
                    .add(event)
                    .addOnSuccessListener(documentReference -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Event berhasil ditambahkan!",
                                Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnSaveEvent.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Gagal menyimpan: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }
}