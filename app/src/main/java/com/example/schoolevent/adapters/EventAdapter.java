package com.example.schoolevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private OnEventClickListener listener;

    public EventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.tvTitle.setText(event.getTitle());
        holder.tvTitleBanner.setText(event.getTitle());
        holder.tvDate.setText(event.getDate());
        holder.tvCategory.setText(event.getCategory());
        holder.tvDescription.setText(event.getDescription());
        holder.tvCommentCount.setText(String.valueOf(event.getCommentCount()));

        //Hitung jumlah like
        int likeCount = event.getLikes() != null ? event.getLikes().size() : 0;
        holder.tvLikeCount.setText(String.valueOf(likeCount));

        //Cek udah like apa blom
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        boolean isLiked = currentUid != null
                && event.getLikes() != null
                && event.getLikes().contains(currentUid);

        // PENTING: Reset listener dulu sebelum set yang baru
        // Mencegah listener lama menempel saat ViewHolder di-recycle
        holder.layoutLike.setOnLongClickListener(null);
        holder.layoutComment.setOnLongClickListener(null);
        holder.itemView.setOnLongClickListener(null);

        //Ubah ikon status
        holder.tvLikeIcon.setText(isLiked ? "❤" : "🤍");

        // Simpan uid ke final variable agar bisa dipakai di lambda
        final String uid = currentUid;
        final Event currentEvent = event;

        //Listener klik like
        holder.layoutLike.setOnClickListener(v -> {
            if(uid == null){
                //Belum login = gak bisa like
                Toast.makeText(v.getContext(),
                        "Login dulu buat like",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            toggleLike(event, uid, holder);
        });

        //Listener klik komentar (ini buka detail event)
        holder.layoutComment.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(currentEvent);
        });

        //Listener klik kartu (ini juga buka detail event)

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(currentEvent);
            }
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

    private void toggleLike(Event event, String uid, EventViewHolder holder){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        boolean isLiked = event.getLikes() != null && event.getLikes().contains(uid);

        if (isLiked){
            // Sudah like → unlike
            // FieldValue.arrayRemove → hapus uid dari array likes di Firestore
            db.collection("events").document(event.getEventId())
                    .update("likes", FieldValue.arrayRemove(uid))
                    .addOnSuccessListener(aVoid -> {
                        event.getLikes().remove(uid);
                        holder.tvLikeIcon.setText("🤍");
                        holder.tvLikeCount.setText(
                                String.valueOf(event.getLikes().size())
                        );
                    });
        } else{
            // Belum like → like
            // FieldValue.arrayUnion → tambahkan uid ke array likes di Firestore
            db.collection("events").document(event.getEventId())
                    .update("likes", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener(aVoid -> {
                        event.getLikes().remove(uid);
                        holder.tvLikeIcon.setText("🤍");
                        holder.tvLikeCount.setText(
                                String.valueOf(event.getLikes().size())
                        );
                    });
        }
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle,tvTitleBanner, tvDate, tvCategory;
        TextView tvDescription, tvLikeCount, tvLikeIcon, tvCommentCount;
        View layoutLike, layoutComment;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTitleBanner = itemView.findViewById(R.id.tv_title_banner);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvLikeIcon = itemView.findViewById(R.id.tv_like_icon);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
            layoutLike = itemView.findViewById(R.id.layout_like);
            layoutComment = itemView.findViewById(R.id.layout_comment);
        }
    }
}