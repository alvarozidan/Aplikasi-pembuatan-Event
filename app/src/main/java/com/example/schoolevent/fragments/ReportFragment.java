package com.example.schoolevent.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.FragmentActivity;

import com.example.schoolevent.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ReportFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager);

        // Adapter — 2 halaman: bug dan event_suggestion
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) return FormReportFragment.newInstance("bug");
                else return FormReportFragment.newInstance("event_suggestion");
            }

            @Override
            public int getItemCount() { return 2; }
        });

        // Percepat animasi swipe ViewPager2
        viewPager.setPageTransformer((page, position) -> {
            page.setAlpha(1 - Math.abs(position) * 1.0f);
        });

        // Hubungkan TabLayout ↔ ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Bug / Masalah");
                tab.setIcon(R.drawable.ic_bug_report);
            } else {
                tab.setText("Usulan Event");
                tab.setIcon(R.drawable.ic_lightbulb);
            }
        }).attach();
        tabLayout.setTabIconTint(
                getResources().getColorStateList(R.color.tab_icon_tint, null)
        );
    }
}