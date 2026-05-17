package com.example.schoolevent.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.activities.MainActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryFragment extends Fragment {

    private RecyclerView rvCategories;
    private FirebaseFirestore db;

    // Data kategori dengan icon
    private final String[][] categoryData = {
            {"Akademik", "📚"},
            {"Olahraga", "⚽"},
            {"Seni & Budaya", "🎨"},
            {"Ekstrakurikuler", "🌟"},
            {"OSIS", "🏛️"},
            {"Kesehatan", "🏥"},
            {"Festival", "🎉"},
            {"Lainnya", "📌"},
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        rvCategories = view.findViewById(R.id.rv_categories);

        // Grid 2 kolom
        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));

        loadCategoryCount();
    }

    private void loadCategoryCount() {
        db.collection("events")
                .get()
                .addOnSuccessListener(snapshots -> {
                    // Hitung jumlah event per kategori
                    Map<String, Integer> countMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String cat = doc.getString("category");
                        if (cat != null) {
                            countMap.put(cat,
                                    countMap.getOrDefault(cat, 0) + 1);
                        }
                    }

                    // Buat list data kategori
                    List<String[]> categories = new ArrayList<>();
                    for (String[] cat : categoryData) {
                        int count = countMap.getOrDefault(cat[0], 0);
                        categories.add(new String[]{
                                cat[0],           // [0] nama kategori
                                cat[1],           // [1] icon emoji
                                count + " event"  // [2] jumlah event
                        });
                    }

                    // Setup adapter dengan listener yang membuka
                    // CategoryDetailFragment
                    CategoryAdapter adapter = new CategoryAdapter(
                            categories,
                            (categoryName, categoryIcon) -> {
                                CategoryDetailFragment detailFragment =
                                        CategoryDetailFragment.newInstance(
                                                categoryName, categoryIcon);
                                ((MainActivity) requireActivity())
                                        .loadFragment(detailFragment);
                            }
                    );
                    rvCategories.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    // Jika gagal load count → tetap tampilkan kategori
                    // dengan count 0
                    List<String[]> categories = new ArrayList<>();
                    for (String[] cat : categoryData) {
                        categories.add(new String[]{
                                cat[0],
                                cat[1],
                                "0 event"
                        });
                    }

                    CategoryAdapter adapter = new CategoryAdapter(
                            categories,
                            (categoryName, categoryIcon) -> {
                                CategoryDetailFragment detailFragment =
                                        CategoryDetailFragment.newInstance(
                                                categoryName, categoryIcon);
                                ((MainActivity) requireActivity())
                                        .loadFragment(detailFragment);
                            }
                    );
                    rvCategories.setAdapter(adapter);
                });
    }

    // ============= INTERFACE =============
    interface OnCategoryClickListener {
        // Menerima nama kategori DAN icon emoji
        void onCategoryClick(String categoryName, String categoryIcon);
    }

    // ============= ADAPTER =============
    static class CategoryAdapter extends
            RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

        private List<String[]> categories;
        private OnCategoryClickListener listener;

        CategoryAdapter(List<String[]> categories,
                        OnCategoryClickListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                     int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder,
                                     int position) {
            String[] cat = categories.get(position);

            // cat[0] = nama, cat[1] = icon, cat[2] = jumlah event
            holder.tvIcon.setText(cat[1]);
            holder.tvName.setText(cat[0]);
            holder.tvCount.setText(cat[2]);

            // Highlight jika tidak ada event → tampilkan warna berbeda
            if ("0 event".equals(cat[2])) {
                holder.tvCount.setAlpha(0.5f);
            } else {
                holder.tvCount.setAlpha(1f);
            }

            // Reset listener dulu
            holder.itemView.setOnClickListener(null);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    // Kirim nama [0] dan icon [1]
                    listener.onCategoryClick(cat[0], cat[1]);
                }
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvCount;

            CategoryViewHolder(@NonNull View v) {
                super(v);
                tvIcon = v.findViewById(R.id.tv_category_icon);
                tvName = v.findViewById(R.id.tv_category_name);
                tvCount = v.findViewById(R.id.tv_category_count);
            }
        }
    }
}