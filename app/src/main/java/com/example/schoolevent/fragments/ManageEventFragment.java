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

import com.example.schoolevent.R;
import com.example.schoolevent.activities.MainActivity;
import com.example.schoolevent.adapters.ManageEventAdapter;
import com.example.schoolevent.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageEventFragment extends Fragment {

    private RecyclerView rvManageEvents;
    private FirebaseFirestore db;
    private List<Event> eventList = new ArrayList<>();
    private ManageEventAdapter manageAdapter;
    private ListenerRegistration listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = view.findViewById(R.id.toolbar_manage);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        rvManageEvents = view.findViewById(R.id.rv_manage_events);
        rvManageEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        manageAdapter = new ManageEventAdapter(eventList,
                // Listener Edit
                event -> {
                    AddEventFragment editFragment = AddEventFragment.newInstance(event);
                    ((MainActivity) requireActivity()).loadFragment(editFragment);
                },
                // Listener Hapus
                event -> confirmDelete(event)
        );

        rvManageEvents.setAdapter(manageAdapter);
        loadEvents();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }

    private void loadEvents() {
        listener = db.collection("events")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        eventList.add(event);
                    }
                    manageAdapter.updateData(eventList);
                });
    }

    private void confirmDelete(Event event) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hapus Event")
                .setMessage("Yakin ingin menghapus \"" + event.getTitle() + "\"?")
                .setPositiveButton("Ya, Hapus", (dialog, which) ->
                        db.collection("events").document(event.getEventId())
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(getContext(),
                                                "Event dihapus", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Gagal: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Batal", null)
                .show();
    }
}