package com.example.schoolevent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.DetailActivity;
import com.example.schoolevent.activities.MainActivity;
import com.example.schoolevent.adapters.EventAdapter;
import com.example.schoolevent.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LikedEventsFragment extends Fragment {

    private RecyclerView rvLikedEvents;
    private View layoutEmpty;
    private EventAdapter adapter;
    private List<Event> likedEvents = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_liked_events,
                container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = view.findViewById(R.id.toolbar_liked);
        toolbar.setNavigationOnClickListener(v -> {
            ((MainActivity) requireActivity()).showNavbar();
            ((MainActivity) requireActivity()).loadFragment(new ProfileFragment());
        });

        rvLikedEvents = view.findViewById(R.id.rv_liked_events);
        layoutEmpty = view.findViewById(R.id.layout_empty_liked);

        rvLikedEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventAdapter(likedEvents, event -> {
            Intent intent = new Intent(getContext(), DetailActivity.class);
            intent.putExtra("event_id", event.getEventId());
            intent.putExtra("event_title", event.getTitle());
            intent.putExtra("event_date", event.getDateDisplay());
            intent.putExtra("event_category", event.getCategory());
            intent.putExtra("event_description", event.getDescription());
            startActivity(intent);
        });
        rvLikedEvents.setAdapter(adapter);

        loadLikedEvents();
    }

    private void loadLikedEvents() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        // Ambil semua event yang likes array-nya mengandung uid user ini
        db.collection("events")
                .whereArrayContains("likes", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    likedEvents.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        likedEvents.add(event);
                    }

                    adapter.updateData(likedEvents);

                    if (likedEvents.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvLikedEvents.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvLikedEvents.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showNavbar();
        }
    }
}