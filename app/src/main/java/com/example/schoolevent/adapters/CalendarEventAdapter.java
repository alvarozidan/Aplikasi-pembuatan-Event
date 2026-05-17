package com.example.schoolevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.models.Event;

import java.util.List;

public class CalendarEventAdapter extends
        RecyclerView.Adapter<CalendarEventAdapter.CalendarEventViewHolder> {

    private List<Event> eventList;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private OnEventClickListener listener;

    public CalendarEventAdapter(List<Event> eventList,
                                OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_calendar, parent, false);
        return new CalendarEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarEventViewHolder holder,
                                 int position) {
        Event event = eventList.get(position);

        holder.tvTitle.setText(event.getTitle());
        holder.tvCategory.setText(event.getCategory());
        holder.tvDescription.setText(event.getDescription());

        holder.itemView.setOnClickListener(null);
        final Event currentEvent = event;
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(currentEvent);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public void updateData(List<Event> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    public static class CalendarEventViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvCategory, tvDescription;

        public CalendarEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_cal_title);
            tvCategory = itemView.findViewById(R.id.tv_cal_category);
            tvDescription = itemView.findViewById(R.id.tv_cal_description);
        }
    }
}