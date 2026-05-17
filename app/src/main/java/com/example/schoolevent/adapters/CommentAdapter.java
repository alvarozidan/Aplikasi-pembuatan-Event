package com.example.schoolevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.models.Comment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    // Hanya komentar UTAMA (bukan reply) yang ada di list ini
    private List<Comment> commentList;

    public interface OnDeleteCommentListener {
        void onDeleteClick(Comment comment, int position);
    }

    public interface OnReplyClickListener {
        void onReplyClick(Comment comment);
    }

    private OnDeleteCommentListener deleteListener;
    private OnReplyClickListener replyListener;

    private String currentUserId = "";
    private String currentUserRole = "";
    private String currentEventId = "";
    private String currentEventTitle = "";

    private FirebaseFirestore db;

    public CommentAdapter(List<Comment> commentList,
                          OnDeleteCommentListener deleteListener) {
        this.commentList = commentList;
        this.deleteListener = deleteListener;
        this.db = FirebaseFirestore.getInstance();
    }

    public void setReplyListener(OnReplyClickListener replyListener) {
        this.replyListener = replyListener;
    }

    public void setCurrentUserInfo(String userId, String role) {
        this.currentUserId = userId;
        this.currentUserRole = role;
        notifyDataSetChanged();
    }

    public void setEventInfo(String eventId, String eventTitle) {
        this.currentEventId = eventId;
        this.currentEventTitle = eventTitle;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        android.util.Log.d("TS_DEBUG",
                "timestamp raw = " + comment.getTimestamp() +
                        " | username = " + comment.getUsername());

        String username = comment.getUsername() != null
                ? comment.getUsername() : "Anonim";

        // Set avatar (huruf pertama username)
        String firstLetter = !username.isEmpty()
                ? String.valueOf(comment.getUsername().charAt(0)).toUpperCase()
                : "?";
        holder.tvAvatar.setText(firstLetter);

        holder.tvUsername.setText(username);
        holder.tvCommentText.setText(comment.getText());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm",
                new Locale("id", "ID"));
        holder.tvTimestamp.setText(sdf.format(new Date(comment.getTimestamp())));

        // Tombol Balas → hanya jika sudah login
        if (!currentUserId.isEmpty()) {
            holder.tvReplyBtn.setVisibility(View.VISIBLE);
            holder.tvReplyBtn.setOnClickListener(v -> {
                if (replyListener != null) replyListener.onReplyClick(comment);
            });
        } else {
            holder.tvReplyBtn.setVisibility(View.GONE);
        }

        // Reset state replies
        holder.containerReplies.setVisibility(View.GONE);
        holder.containerReplies.removeAllViews();
        holder.tvShowReplies.setVisibility(View.GONE);
        holder.isRepliesExpanded = false;

        // Load jumlah reply dari Firestore
        loadReplyCount(comment, holder);

        // Long press → menu hapus & laporkan
        holder.itemView.setOnLongClickListener(v -> {
            showCommentOptions(v, comment, position);
            return true;
        });
    }

    private void loadReplyCount(Comment comment, CommentViewHolder holder) {
        db.collection("comments")
                .whereEqualTo("eventId", comment.getEventId())
                .whereEqualTo("parentCommentId", comment.getCommentId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    int replyCount = snapshots.size();
                    if (replyCount > 0) {
                        holder.tvShowReplies.setVisibility(View.VISIBLE);
                        holder.tvShowReplies.setText("Lihat " + replyCount + " Balasan ▼");

                        // Simpan data reply untuk ditampilkan nanti
                        List<Comment> replies = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Comment reply = doc.toObject(Comment.class);
                            reply.setCommentId(doc.getId());
                            replies.add(reply);
                        }

                        // Listener toggle show/hide reply
                        holder.tvShowReplies.setOnClickListener(v -> {
                            if (holder.isRepliesExpanded) {
                                // Sembunyikan balasan
                                holder.containerReplies.setVisibility(View.GONE);
                                holder.containerReplies.removeAllViews();
                                holder.tvShowReplies.setText(
                                        "Lihat " + replyCount + " Balasan ▼");
                                holder.isRepliesExpanded = false;
                            } else {
                                // Tampilkan balasan
                                holder.containerReplies.setVisibility(View.VISIBLE);
                                holder.containerReplies.removeAllViews();
                                inflateReplies(replies, holder, comment);
                                holder.tvShowReplies.setText(
                                        "Sembunyikan Balasan ▲");
                                holder.isRepliesExpanded = true;
                            }
                        });
                    } else {
                        holder.tvShowReplies.setVisibility(View.GONE);
                    }
                });
    }

    private void inflateReplies(List<Comment> replies, CommentViewHolder holder,
                                Comment parentComment) {
        // Urutkan berdasarkan timestamp
        replies.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (Comment reply : replies) {
            View replyView = inflater.inflate(
                    R.layout.item_comment_reply,
                    holder.containerReplies, false);

            // Avatar
            TextView tvReplyAvatar = replyView.findViewById(R.id.tv_reply_avatar);
            String firstLetter = reply.getUsername() != null
                    && !reply.getUsername().isEmpty()
                    ? String.valueOf(reply.getUsername().charAt(0)).toUpperCase()
                    : "?";
            tvReplyAvatar.setText(firstLetter);

            // Username & waktu
            TextView tvReplyUsername = replyView.findViewById(R.id.tv_reply_username);
            TextView tvReplyTimestamp = replyView.findViewById(R.id.tv_reply_timestamp);
            tvReplyUsername.setText(reply.getUsername());
            tvReplyTimestamp.setText(sdf.format(new Date(reply.getTimestamp())));

            // Tag @username jika ada
            TextView tvReplyToTag = replyView.findViewById(R.id.tv_reply_to_tag);
            if (reply.getReplyToUsername() != null
                    && !reply.getReplyToUsername().isEmpty()) {
                tvReplyToTag.setVisibility(View.VISIBLE);
                tvReplyToTag.setText("@" + reply.getReplyToUsername());
            } else {
                tvReplyToTag.setVisibility(View.GONE);
            }

            // Isi reply
            TextView tvReplyText = replyView.findViewById(R.id.tv_reply_text);
            tvReplyText.setText(reply.getText());

            // Tombol balas reply
            TextView tvReplyReplyBtn = replyView.findViewById(R.id.tv_reply_reply_btn);
            if (!currentUserId.isEmpty()) {
                tvReplyReplyBtn.setVisibility(View.VISIBLE);
                tvReplyReplyBtn.setOnClickListener(v -> {
                    // Saat balas reply → set target ke komentar utama
                    // tapi dengan mention username reply
                    if (replyListener != null) {
                        // Buat comment sementara agar username target benar
                        Comment replyTarget = new Comment();
                        replyTarget.setCommentId(parentComment.getCommentId());
                        replyTarget.setUsername(reply.getUsername());
                        replyListener.onReplyClick(replyTarget);
                    }
                });
            } else {
                tvReplyReplyBtn.setVisibility(View.GONE);
            }

            // Long press reply → hapus/laporkan
            final Comment currentReply = reply;
            replyView.setOnLongClickListener(v -> {
                showCommentOptions(v, currentReply, -1);
                return true;
            });

            holder.containerReplies.addView(replyView);
        }
    }

    private void showCommentOptions(View anchor, Comment comment, int position) {
        android.util.Log.d("DELETE_DEBUG",
                "commentId = " + comment.getCommentId() +
                        " | username = " + comment.getUsername());

        boolean isAdmin = "admin".equals(currentUserRole);
        boolean isOwner = comment.getUserId() != null
                && comment.getUserId().equals(currentUserId);

        android.widget.PopupMenu popup = new android.widget.PopupMenu(
                anchor.getContext(), anchor);

        if ((isAdmin || isOwner) && !currentUserId.isEmpty()) {
            popup.getMenu().add(0, 1, 0, "🗑️ Hapus Komentar");
        }

        if (!currentUserId.isEmpty()) {
            popup.getMenu().add(0, 2, 1, "🚩 Laporkan Komentar");
        }

        if (popup.getMenu().size() == 0) return;

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                if (deleteListener != null && position >= 0) {
                    deleteListener.onDeleteClick(comment, position);
                } else if (position < 0) {
                    // Hapus reply langsung
                    FirebaseFirestore.getInstance()
                            .collection("comments")
                            .document(comment.getCommentId())
                            .delete();
                }
            } else if (item.getItemId() == 2) {
                reportComment(anchor, comment);
            }
            return true;
        });

        popup.show();
    }

    private void reportComment(View anchor, Comment comment) {
        String[] reasons = {
                "Komentar tidak sopan",
                "Spam",
                "Konten menyesatkan",
                "Lainnya"
        };

        new androidx.appcompat.app.AlertDialog.Builder(anchor.getContext())
                .setTitle("Alasan Laporan")
                .setItems(reasons, (dialog, which) -> {
                    String reason = reasons[which];

                    java.util.Map<String, Object> report = new java.util.HashMap<>();
                    report.put("type", "comment_report");
                    report.put("category", "bug");
                    report.put("title", "Laporan Komentar: " + reason);
                    report.put("detail", "Komentar dari @"
                            + comment.getUsername()
                            + ": \"" + comment.getText()
                            + "\"\nAlasan: " + reason);
                    report.put("commentId", comment.getCommentId());
                    report.put("reportedUserId", comment.getUserId());
                    report.put("eventId", currentEventId);
                    report.put("eventTitle", currentEventTitle);
                    report.put("status", "pending");
                    report.put("timestamp", System.currentTimeMillis());

                    FirebaseFirestore.getInstance()
                            .collection("reports")
                            .add(report)
                            .addOnSuccessListener(ref ->
                                    android.widget.Toast.makeText(
                                            anchor.getContext(),
                                            "Komentar dilaporkan",
                                            android.widget.Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public int getItemCount() { return commentList.size(); }

    public void updateData(List<Comment> newList) {
        // Filter hanya komentar utama (bukan reply)
        this.commentList = new ArrayList<>();
        for (Comment c : newList) {
            if (c.getParentCommentId() == null
                    || c.getParentCommentId().isEmpty()) {
                this.commentList.add(c);
            }
        }
        notifyDataSetChanged();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvAvatar, tvUsername, tvCommentText, tvTimestamp;
        TextView tvReplyBtn, tvShowReplies;
        LinearLayout containerReplies;
        boolean isRepliesExpanded = false;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvCommentText = itemView.findViewById(R.id.tv_comment_text);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvReplyBtn = itemView.findViewById(R.id.tv_reply_btn);
            tvShowReplies = itemView.findViewById(R.id.tv_show_replies);
            containerReplies = itemView.findViewById(R.id.container_replies);
        }
    }
}