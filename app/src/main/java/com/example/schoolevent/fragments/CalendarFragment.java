package com.example.schoolevent.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.adapters.CalendarEventAdapter;
import com.example.schoolevent.models.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// @RequiresApi → memberitahu compiler bahwa seluruh class ini
// sudah kita tangani sendiri untuk API di bawah 26
@RequiresApi(api = Build.VERSION_CODES.O)
public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView rvCalendarEvents;
    private View tvNoEventDate;

    private FirebaseFirestore db;
    private List<Event> allEvents = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();
    private CalendarEventAdapter calendarAdapter;

    private Set<String> eventDates = new HashSet<>();
    private String selectedDateStr = "";
    private LocalDate selectedLocalDate = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle("Smakanza Event");

        // Cek API level → kalender hanya jalan di API 26+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Tampilkan pesan jika HP tidak support
            tvNoEventDate = view.findViewById(R.id.tv_no_event_date);
            tvNoEventDate.setVisibility(View.VISIBLE);
            TextView msg = view.findViewById(R.id.tv_no_event_date)
                    .findViewById(android.R.id.text1);
            return;
        }

        db = FirebaseFirestore.getInstance();

        calendarView = view.findViewById(R.id.calendar_view);
        rvCalendarEvents = view.findViewById(R.id.rv_calendar_events);
        tvNoEventDate = view.findViewById(R.id.tv_no_event_date);

        rvCalendarEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        calendarAdapter = new CalendarEventAdapter(filteredEvents, event -> {
            Intent intent = new Intent(getContext(),
                    com.example.schoolevent.activities.DetailActivity.class);
            intent.putExtra("event_id", event.getEventId());
            intent.putExtra("event_title", event.getTitle());
            intent.putExtra("event_date", event.getDateDisplay());
            intent.putExtra("event_category", event.getCategory());
            intent.putExtra("event_description", event.getDescription());
            startActivity(intent);
        });
        rvCalendarEvents.setAdapter(calendarAdapter);

        // Set tanggal hari ini
        Calendar today = Calendar.getInstance();
        selectedDateStr = formatDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH)
        );

        setupCalendar();
        loadAllEvents();
    }

    private void setupCalendar() {

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {

            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container,
                             @NonNull CalendarDay calendarDay) {

                LocalDate date = calendarDay.getDate();

                // Ambil nilai tanggal dari LocalDate
                // Aman karena sudah dicek Build.VERSION di onViewCreated
                int day = date.getDayOfMonth();
                int month = date.getMonthValue();
                int year = date.getYear();

                container.tvDay.setText(String.valueOf(day));
                String dateStr = formatDate(year, month, day);

                if (calendarDay.getPosition() == DayPosition.MonthDate) {
                    container.tvDay.setAlpha(1f);

                    container.viewDot.setVisibility(
                            eventDates.contains(dateStr) ? View.VISIBLE : View.GONE
                    );

                    if (dateStr.equals(selectedDateStr)) {
                        container.tvDay.setBackgroundResource(
                                R.drawable.bg_selected_date);
                        container.tvDay.setTextColor(Color.WHITE);
                    } else if (isToday(year, month, day)) {
                        container.tvDay.setBackgroundResource(0);
                        container.tvDay.setTextColor(
                                ContextCompat.getColor(requireContext(),
                                        R.color.primary));
                    } else {
                        container.tvDay.setBackgroundResource(0);
                        container.tvDay.setTextColor(
                                ContextCompat.getColor(requireContext(),
                                        android.R.color.tab_indicator_text));
                    }

                    container.tvDay.setOnClickListener(v -> {
                        String previousDate = selectedDateStr;
                        selectedDateStr = dateStr;
                        selectedLocalDate = date;

                        // Refresh tanggal sebelumnya
                        if (!previousDate.isEmpty()) {
                            try {
                                String[] parts = previousDate.split("-");
                                LocalDate prevLocal = LocalDate.of(
                                        Integer.parseInt(parts[0]),
                                        Integer.parseInt(parts[1]),
                                        Integer.parseInt(parts[2])
                                );
                                calendarView.notifyDateChanged(prevLocal);
                            } catch (Exception ignored) {}
                        }

                        calendarView.notifyDateChanged(date);
                        filterEventsByDate(dateStr);
                    });

                } else {
                    container.tvDay.setAlpha(0.3f);
                    container.viewDot.setVisibility(View.GONE);
                    container.tvDay.setBackgroundResource(0);
                    container.tvDay.setOnClickListener(null);
                }
            }
        });

        calendarView.setMonthHeaderBinder(
                new MonthHeaderFooterBinder<MonthViewContainer>() {

                    @NonNull
                    @Override
                    public MonthViewContainer create(@NonNull View view) {
                        return new MonthViewContainer(view);
                    }

                    @Override
                    public void bind(@NonNull MonthViewContainer container,
                                     @NonNull CalendarMonth calendarMonth) {

                        // Ambil tahun & bulan dari YearMonth
                        // Aman karena sudah dicek Build.VERSION
                        int year = calendarMonth.getYearMonth().getYear();
                        int month = calendarMonth.getYearMonth().getMonthValue();

                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month - 1, 1);

                        SimpleDateFormat sdf = new SimpleDateFormat(
                                "MMMM yyyy", new Locale("id", "ID"));
                        container.tvMonthYear.setText(sdf.format(cal.getTime()));

                        container.btnPrev.setOnClickListener(v -> {
                            YearMonth current =
                                    calendarView.findFirstVisibleMonth() != null
                                            ? calendarView.findFirstVisibleMonth()
                                            .getYearMonth()
                                            : YearMonth.now();
                            calendarView.smoothScrollToMonth(
                                    current.minusMonths(1));
                        });

                        container.btnNext.setOnClickListener(v -> {
                            YearMonth current =
                                    calendarView.findFirstVisibleMonth() != null
                                            ? calendarView.findFirstVisibleMonth()
                                            .getYearMonth()
                                            : YearMonth.now();
                            calendarView.smoothScrollToMonth(
                                    current.plusMonths(1));
                        });
                    }
                });

        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(12);
        YearMonth endMonth = currentMonth.plusMonths(12);
        calendarView.setup(startMonth, endMonth, DayOfWeek.SUNDAY);
        calendarView.scrollToMonth(currentMonth);

        filterEventsByDate(selectedDateStr);
    }

    private void loadAllEvents() {
        db.collection("events")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    allEvents.clear();
                    eventDates.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        allEvents.add(event);

                        if (event.getDate() != null && !event.getDate().isEmpty()) {
                            eventDates.add(event.getDate());
                        }
                    }

                    calendarView.notifyCalendarChanged();
                    filterEventsByDate(selectedDateStr);
                });
    }

    private void filterEventsByDate(String date) {
        filteredEvents.clear();

        for (Event event : allEvents) {
            if (date.equals(event.getDate())) {
                filteredEvents.add(event);
            }
        }

        calendarAdapter.updateData(filteredEvents);

        if (filteredEvents.isEmpty()) {
            tvNoEventDate.setVisibility(View.VISIBLE);
            rvCalendarEvents.setVisibility(View.GONE);
        } else {
            tvNoEventDate.setVisibility(View.GONE);
            rvCalendarEvents.setVisibility(View.VISIBLE);
        }
    }

    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(),
                "%04d-%02d-%02d", year, month, day);
    }

    private boolean isToday(int year, int month, int day) {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == year
                && today.get(Calendar.MONTH) + 1 == month
                && today.get(Calendar.DAY_OF_MONTH) == day;
    }

    class DayViewContainer extends ViewContainer {
        TextView tvDay;
        View viewDot;

        public DayViewContainer(@NonNull View view) {
            super(view);
            tvDay = view.findViewById(R.id.tv_day);
            viewDot = view.findViewById(R.id.view_dot);
        }
    }

    class MonthViewContainer extends ViewContainer {
        TextView tvMonthYear;
        ImageButton btnPrev, btnNext;

        public MonthViewContainer(@NonNull View view) {
            super(view);
            tvMonthYear = view.findViewById(R.id.tv_month_year);
            btnPrev = view.findViewById(R.id.btn_prev_month);
            btnNext = view.findViewById(R.id.btn_next_month);
        }
    }
}