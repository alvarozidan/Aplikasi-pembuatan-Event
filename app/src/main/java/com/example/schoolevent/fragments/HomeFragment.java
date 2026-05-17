package com.example.schoolevent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.widget.Toolbar;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.DetailActivity;
import com.example.schoolevent.activities.MainActivity;
import com.example.schoolevent.activities.AboutActivity;
import com.example.schoolevent.adapters.EventAdapter;
import com.example.schoolevent.models.Event;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class HomeFragment extends Fragment {

    private RecyclerView rvEvents;
    private View tvEmpty, viewNotificationDot;
    private ListenerRegistration notificationListener;
    private SwipeRefreshLayout swipeRefresh;
    private Toolbar toolbar;
    private boolean isFirstLoad = true;
    private boolean isRefreshing = false;
    private CardView btnProfile;
    private TextView tvProfileAvatar, tvAppTitle;

    private String categoryFilter = null; // null = tampilkan semua

    private EventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private List<Event> allEventList = new ArrayList<>(); // simpan semua data asli
    private TextInputEditText etSearch;
    private ListenerRegistration eventsListener;

    private ImageView btnSearch;
    private LinearLayout layoutSearch;
    private boolean isSearchOpen = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public static HomeFragment newInstanceWithFilter(String category){
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("category_filter", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inisialisasi komponen
        toolbar = view.findViewById(R.id.toolbar);
        rvEvents = view.findViewById(R.id.rv_events);
        tvEmpty = view.findViewById(R.id.tv_empty);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        btnProfile = view.findViewById(R.id.btn_profile);
        tvProfileAvatar = view.findViewById(R.id.tv_profile_avatar);
        tvAppTitle = view.findViewById(R.id.tv_app_title);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        layoutSearch = view.findViewById(R.id.layout_search);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch.setOnClickListener(v -> toggleSearch());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewNotificationDot = view.findViewById(R.id.view_notification_dot);

        checkNotifications();

        tvAppTitle.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AboutActivity.class);
            startActivity(intent);
        });

        if(getArguments() != null){
            categoryFilter = getArguments().getString("category_filter");
        }

        // Set avatar dari nama user atau ikon guest
        updateProfileButton();

        btnProfile.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if(currentUser != null){
                ((MainActivity) requireActivity()).loadFragment(new ProfileFragment());
            }else{
                ((MainActivity)requireActivity()).loadFragment(new LoginFragment());
            }
            ((MainActivity) requireActivity()).setNavbarSelected(-1);
        });

        // Setup RecyclerView
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEvents.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        adapter = new EventAdapter(eventList, event -> {
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
        swipeRefresh.setOnRefreshListener(() -> {
            isRefreshing = true;  // ← tandai bahwa ini refresh manual
            loadEvents();
        });
        loadEvents();
    }



    @Override
    public void onResume() {
        super.onResume();
    }

    private void loadEvents() {
        if (eventsListener != null) eventsListener.remove();

        com.google.firebase.firestore.Query query = db.collection("events");

        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            query = query.whereEqualTo("category", categoryFilter);
        }

        eventsListener = query.addSnapshotListener((snapshots, error) -> {
            swipeRefresh.setRefreshing(false);
            if (error != null || snapshots == null) return;

            eventList.clear();
            allEventList.clear();

            for (QueryDocumentSnapshot doc : snapshots) {
                Event event = doc.toObject(Event.class);
                event.setEventId(doc.getId());
                eventList.add(event);
                allEventList.add(event);
            }

            String currentQuery = etSearch.getText() != null
                    ? etSearch.getText().toString() : "";

            if (isFirstLoad || isRefreshing) {
                // ✅ post() → tunggu RecyclerView siap, baru jalankan animasi
                rvEvents.post(() -> {
                    runLayoutAnimation(rvEvents);
                    if (!currentQuery.isEmpty()) {
                        filterEvents(currentQuery);
                    } else {
                        adapter.updateData(eventList);
                    }
                });
                isFirstLoad = false;
                isRefreshing = false;
            } else {
                if (!currentQuery.isEmpty()) {
                    filterEvents(currentQuery);
                } else {
                    adapter.updateData(eventList);
                }
            }

            tvEmpty.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
            rvEvents.setVisibility(eventList.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void runLayoutAnimation(RecyclerView recyclerView) {
        android.view.animation.LayoutAnimationController controller =
                android.view.animation.AnimationUtils.loadLayoutAnimation(
                        requireContext(), R.anim.layout_animation_fall_down);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.scheduleLayoutAnimation();
    }

    private void updateProfileButton(){
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null && tvProfileAvatar != null){
            //Ambil huruf pertama username di Firebase
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("name");
                        if(name != null && !name.isEmpty()){
                            tvProfileAvatar.setText(
                                    String.valueOf(name.charAt(0)).toUpperCase()
                            );
                        }else{
                            tvProfileAvatar.setText("U");
                        }
                    });
        }else if(tvProfileAvatar != null){
            //Belum login -> tampilkan icon
            tvProfileAvatar.setText("?");
        }

    }
    private void checkNotifications() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Hanya cek jika sudah login
        if (currentUser == null) {
            if (viewNotificationDot != null) {
                viewNotificationDot.setVisibility(View.GONE);
            }
            return;
        }

        // Cek apakah user adalah admin
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String role = doc.getString("role");

                    if ("admin".equals(role)) {
                        // Admin → cek laporan/usulan yang status pending
                        listenToPendingReports();
                    } else {
                        // User biasa → sembunyikan dot
                        if (viewNotificationDot != null) {
                            viewNotificationDot.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void listenToPendingReports() {
        // Stop listener lama jika ada
        if (notificationListener != null) notificationListener.remove();

        // Realtime listener → update dot setiap ada laporan baru
        notificationListener = db.collection("reports")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    if (viewNotificationDot == null) return;

                    if (snapshots.size() > 0) {
                        // Ada laporan pending → tampilkan dot
                        viewNotificationDot.setVisibility(View.VISIBLE);

                        // Animasi pulse agar lebih menarik perhatian
                        viewNotificationDot.animate()
                                .scaleX(1.3f)
                                .scaleY(1.3f)
                                .setDuration(500)
                                .withEndAction(() ->
                                        viewNotificationDot.animate()
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .setDuration(500)
                                                .start())
                                .start();
                    } else {
                        // Tidak ada laporan pending → sembunyikan dot
                        viewNotificationDot.setVisibility(View.GONE);
                    }
                });
    }

    private void filterEvents(String query) {
        if (query.isEmpty()) {
            // Kosong → tampilkan semua
            eventList.clear();
            eventList.addAll(allEventList);
        } else {
            // Filter berdasarkan judul (case-insensitive)
            String lowerQuery = query.toLowerCase().trim();
            eventList.clear();
            for (Event event : allEventList) {
                if (event.getTitle() != null
                        && event.getTitle().toLowerCase().contains(lowerQuery)) {
                    eventList.add(event);
                }
            }
        }

        adapter.updateData(eventList);

        // Tampilkan empty state jika hasil kosong
        tvEmpty.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void toggleSearch() {
        if (isSearchOpen) {
            // Tutup search bar
            layoutSearch.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        layoutSearch.setVisibility(View.GONE);
                        etSearch.setText("");
                        filterEvents(""); // reset list

                        // Sembunyikan keyboard
                        android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager)
                                        requireContext().getSystemService(
                                                android.content.Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                    })
                    .start();
            btnSearch.setImageResource(R.drawable.ic_search); // icon kembali ke search
        } else {
            // Buka search bar
            layoutSearch.setAlpha(0f);
            layoutSearch.setVisibility(View.VISIBLE);
            layoutSearch.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
            etSearch.requestFocus(); // langsung fokus ke input
            btnSearch.setImageResource(R.drawable.ic_close); // icon berubah jadi X
        }
        isSearchOpen = !isSearchOpen;
    }

    // Hentikan listener saat fragment destroy
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventsListener != null) eventsListener.remove();
        if (notificationListener != null) notificationListener.remove();
    }
}