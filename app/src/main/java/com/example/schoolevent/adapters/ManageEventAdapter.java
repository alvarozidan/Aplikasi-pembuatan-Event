package com.example.schoolevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.models.Event;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ManageEventAdapter extends RecyclerView.Adapter<ManageEventAdapter.ManageViewHolder> {

    private List<Event> eventList;

    public interface OnEditClickListener { void onEditClick(Event event); }
    public interface OnDeleteClickListener { void onDeleteClick(Event event); }

    private OnEditClickListener editListener;
    private OnDeleteClickListener deleteListener;

    public ManageEventAdapter(List<Event> eventList,
                              OnEditClickListener editListener,
                              OnDeleteClickListener deleteListener) {
        this.eventList = eventList;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ManageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_manage, parent, false);
        return new ManageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvTitle.setText(event.getTitle());
        holder.tvDate.setText(event.getDateDisplay());
        holder.tvCategory.setText(event.getCategory());

        // Reset listener dulu
        holder.btnEdit.setOnClickListener(null);
        holder.btnDelete.setOnClickListener(null);

        final Event currentEvent = event;
        holder.btnEdit.setOnClickListener(v -> {
            if (editListener != null) editListener.onEditClick(currentEvent);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(currentEvent);
        });
    }

    @Override
    public int getItemCount() { return eventList.size(); }

    public void updateData(List<Event> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    public static class ManageViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvCategory;
        MaterialButton btnEdit, btnDelete;

        public ManageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_manage_title);
            tvDate = itemView.findViewById(R.id.tv_manage_date);
            tvCategory = itemView.findViewById(R.id.tv_manage_category);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}