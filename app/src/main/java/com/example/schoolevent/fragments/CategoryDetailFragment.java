package com.example.schoolevent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.DetailActivity;
import com.example.schoolevent.adapters.EventAdapter;
import com.example.schoolevent.models.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryDetailFragment extends Fragment {

    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CATEGORY_ICON = "category_icon";

    private RecyclerView rvEvents;
    private View layoutEmpty;
    private SwipeRefreshLayout swipeRefresh;

    private EventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private ListenerRegistration eventsListener;
    private FirebaseFirestore db;

    private String categoryName;
    private String categoryIcon;

    // Static factory method
    public static CategoryDetailFragment newInstance(String categoryName,
                                                     String categoryIcon) {
        CategoryDetailFragment fragment = new CategoryDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_NAME, categoryName);
        args.putString(ARG_CATEGORY_ICON, categoryIcon);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_detail,
                container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            categoryName = getArguments().getString(ARG_CATEGORY_NAME);
            categoryIcon = getArguments().getString(ARG_CATEGORY_ICON);
        }

        db = FirebaseFirestore.getInstance();

        // Setup toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar_category_detail);
        // Tampilkan icon + nama kategori di judul
        toolbar.setTitle(categoryIcon + " " + categoryName);
        toolbar.setTitleTextColor(android.graphics.Color.WHITE);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        rvEvents = view.findViewById(R.id.rv_category_events);
        layoutEmpty = view.findViewById(R.id.layout_empty_category);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_category);

        // Setup RecyclerView
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventAdapter(eventList, event -> {
            // Klik event → buka DetailActivity
            Intent intent = new Intent(getContext(), DetailActivity.class);
            intent.putExtra("event_id", event.getEventId());
            intent.putExtra("event_title", event.getTitle());
            intent.putExtra("event_date", event.getDateDisplay());
            intent.putExtra("event_category", event.getCategory());
            intent.putExtra("event_description", event.getDescription());
            startActivity(intent);
        });
        rvEvents.setAdapter(adapter);

        // Setup SwipeRefresh
        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light
        );
        swipeRefresh.setOnRefreshListener(this::loadEvents);

        loadEvents();
    }

    private void loadEvents() {
        if (eventsListener != null) eventsListener.remove();

        // Query event berdasarkan kategori
        eventsListener = db.collection("events")
                .whereEqualTo("category", categoryName)
                .addSnapshotListener((snapshots, error) -> {
                    swipeRefresh.setRefreshing(false);

                    if (error != null) {
                        Toast.makeText(getContext(),
                                "Gagal memuat: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    eventList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        eventList.add(event);
                    }

                    adapter.updateData(eventList);

                    if (eventList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvEvents.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvEvents.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventsListener != null) eventsListener.remove();
    }
}