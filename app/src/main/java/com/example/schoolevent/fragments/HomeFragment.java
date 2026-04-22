package com.example.schoolevent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.DetailActivity;
import com.example.schoolevent.activities.MainActivity;
import com.example.schoolevent.adapters.EventAdapter;
import com.example.schoolevent.models.Event;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
public class HomeFragment extends Fragment {

    private RecyclerView rvEvents;
    private TextView tvEmpty;
    private FloatingActionButton fabAddEvent;

    private EventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rvEvents = view.findViewById(R.id.rv_events);
        tvEmpty = view.findViewById(R.id.tv_empty);
        fabAddEvent = view.findViewById(R.id.fab_add_event);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new EventAdapter(eventList, event -> {
            Intent intent = new Intent(getContext(), DetailActivity.class);
            intent.putExtra("event_id", event.getEventId());
            intent.putExtra("event_title", event.getTitle());
            intent.putExtra("event_date", event.getDate());
            intent.putExtra("event_category", event.getCategory());
            intent.putExtra("event_description", event.getDescription());
            startActivity(intent);
        });

        rvEvents.setAdapter(adapter);

        checkAdminStatus();
        loadEvents();

        fabAddEvent.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).loadFragment(new AddEventFragment());
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        loadEvents();
    }

    private void checkAdminStatus(){
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){
            Log.d("ADMIN CHECK", "User tidak login");
            return;
        }

        Log.d("ADMIN CHECK", "UID: " + currentUser.getUid());

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        String role = documentSnapshot.getString("role");
                        Log.d("ADMIN_CHECK", "Role: " + role);

                        if("admin".equals(role)){
                            Log.d("ADMIN_CHECK", "FAB muncul");
                            fabAddEvent.setVisibility(View.VISIBLE);
                        }else{
                            Log.d("ADMIN_CHECK", "Role bukan admin: " + role);
                        }
                    }else{
                        Log.d("ADMIN_CHECK", "Document tidak ditemukan");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ADMIN_CHECK", "Error: " + e.getMessage());
                });
    }

    private void loadEvents(){
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();

                    for(QueryDocumentSnapshot doc: queryDocumentSnapshots){
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        eventList.add(event);
                    }

                    adapter.updateData(eventList);

                    if(eventList.isEmpty()){
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvEvents.setVisibility(View.GONE);
                    }else{
                        tvEmpty.setVisibility(View.GONE);
                        rvEvents.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Gagal memuat event: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }


}
