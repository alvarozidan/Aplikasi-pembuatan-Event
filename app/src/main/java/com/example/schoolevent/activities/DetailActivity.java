package com.example.schoolevent.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.adapters.CommentAdapter;
import com.example.schoolevent.models.Comment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvDate, tvCategory, tvDescription;
    private TextView tvLikeCountDetail, tvCommentCountDetail;
    private ImageView tvLikeIconDetail;
    private LinearLayout layoutLikeDetail, layoutOpenComments;
    private Toolbar toolbar;

    private String eventId, eventTitle, eventDate, eventCategory, eventDescription;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data like
    private List<String> currentLikes = new ArrayList<>();
    private String currentUserId = "";
    private String currentUserRole = "";
    private String currentUsername = "";

    // Bottom Sheet komentar
    private BottomSheetDialog commentBottomSheet;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();
    private ListenerRegistration commentsListener;
    private Comment replyingToComment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inisialisasi UI
        toolbar = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tv_title);
        tvDate = findViewById(R.id.tv_date);
        tvCategory = findViewById(R.id.tv_category);
        tvDescription = findViewById(R.id.tv_description);
        tvLikeIconDetail = findViewById(R.id.tv_like_icon_detail);
        tvLikeCountDetail = findViewById(R.id.tv_like_count_detail);
        tvCommentCountDetail = findViewById(R.id.tv_comment_count_detail);
        layoutLikeDetail = findViewById(R.id.layout_like_detail);
        layoutOpenComments = findViewById(R.id.layout_open_comments);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Ambil data dari Intent
        eventId = getIntent().getStringExtra("event_id");
        eventTitle = getIntent().getStringExtra("event_title");
        eventDate = getIntent().getStringExtra("event_date");
        eventCategory = getIntent().getStringExtra("event_category");
        eventDescription = getIntent().getStringExtra("event_description");

        // Set data ke UI
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(eventTitle);
        }
        tvTitle.setText(eventTitle);
        tvDate.setText(eventDate);
        tvCategory.setText(eventCategory);
        tvDescription.setText(eventDescription);

        // Load data user yang login
        loadCurrentUserData();

        // Load data event (likes & comment count) realtime
        loadEventData();

        // Listener like
        layoutLikeDetail.setOnClickListener(v -> toggleLike());

        // Listener buka komentar
        layoutOpenComments.setOnClickListener(v -> openCommentSheet());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsListener != null) commentsListener.remove();
        if (commentBottomSheet != null && commentBottomSheet.isShowing()) {
            commentBottomSheet.dismiss();
        }
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        currentUserId = currentUser.getUid();

        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserRole = doc.getString("role") != null
                                ? doc.getString("role") : "user";
                        currentUsername = doc.getString("name") != null
                                ? doc.getString("name") : "";
                    }
                });
    }

    private void loadEventData() {
        // Realtime listener untuk like & comment count
        db.collection("events")
                .document(eventId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null) return;

                    // Update likes
                    List<String> likes = (List<String>) doc.get("likes");
                    if (likes != null) {
                        currentLikes = likes;
                    } else {
                        currentLikes = new ArrayList<>();
                    }

                    boolean isLiked = !currentUserId.isEmpty()
                            && currentLikes.contains(currentUserId);

                    if(isLiked){
                        tvLikeIconDetail.setImageResource(R.drawable.ic_favorite);
                        tvLikeIconDetail.setColorFilter(
                                ContextCompat.getColor(this, android.R.color.holo_red_light)
                        );
                    } else{
                        tvLikeIconDetail.setImageResource(R.drawable.ic_favorite_border);
                        tvLikeIconDetail.clearColorFilter();
                    }

                    tvLikeCountDetail.setSelected(isLiked);
                    tvLikeCountDetail.setText(currentLikes.size() + " Suka");

                    // Update comment count
                    Long commentCount = doc.getLong("commentCount");
                    long count = commentCount != null ? commentCount : 0;
                    tvCommentCountDetail.setText(count + " Komentar");
                });
    }

    private void toggleLike() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Login dulu untuk like!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isLiked = currentLikes.contains(currentUserId);
        layoutLikeDetail.setEnabled(false);

        if (isLiked) {
            db.collection("events").document(eventId)
                    .update("likes", FieldValue.arrayRemove(currentUserId))
                    .addOnCompleteListener(task ->
                            layoutLikeDetail.setEnabled(true));
        } else {
            db.collection("events").document(eventId)
                    .update("likes", FieldValue.arrayUnion(currentUserId))
                    .addOnCompleteListener(task ->
                            layoutLikeDetail.setEnabled(true));
        }
    }

    private void openCommentSheet() {
        // Buat Bottom Sheet
        commentBottomSheet = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this)
                .inflate(R.layout.layout_comment_bottom_sheet, null);
        commentBottomSheet.setContentView(sheetView);

        // Komponen di bottom sheet
        RecyclerView rvComments = sheetView.findViewById(R.id.rv_comments_sheet);
        View layoutNoComments = sheetView.findViewById(R.id.layout_no_comments);
        LinearLayout layoutCommentInput = sheetView.findViewById(
                R.id.layout_comment_input);
        TextInputEditText etComment = sheetView.findViewById(
                R.id.et_comment_sheet);
        FloatingActionButton btnSend = sheetView.findViewById(
                R.id.btn_send_comment_sheet);
        View btnLoginToComment = sheetView.findViewById(
                R.id.btn_login_to_comment_sheet);

        LinearLayout layoutReplyIndicator = sheetView.findViewById(
                R.id.layout_reply_indicator);
        TextView tvReplyingTo = sheetView.findViewById(R.id.tv_replying_to);
        TextView tvCancelReply = sheetView.findViewById(R.id.tv_cancel_reply);
        TextView tvCommentCountSheet = sheetView.findViewById(
                R.id.tv_comment_count_sheet);

        // Setup RecyclerView
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(commentList,
                (comment, position) -> {
                    // Konfirmasi hapus
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Hapus Komentar")
                            .setMessage("Yakin ingin menghapus?")
                            .setPositiveButton("Hapus", (d, w) ->
                                    deleteComment(comment))
                            .setNegativeButton("Batal", null)
                            .show();
                });

        commentAdapter.setCurrentUserInfo(currentUserId, currentUserRole);
        commentAdapter.setEventInfo(eventId, eventTitle);
        commentAdapter.setReplyListener(comment -> {
            replyingToComment = comment;
            layoutReplyIndicator.setVisibility(View.VISIBLE);
            tvReplyingTo.setText("↩ Membalas @" + comment.getUsername());
            etComment.requestFocus();
        });
        rvComments.setAdapter(commentAdapter);

        // Tampilkan/sembunyikan input berdasarkan login
        if (!currentUserId.isEmpty()) {
            layoutCommentInput.setVisibility(View.VISIBLE);
            btnLoginToComment.setVisibility(View.GONE);
        } else {
            layoutCommentInput.setVisibility(View.GONE);
            btnLoginToComment.setVisibility(View.VISIBLE);
        }

        // Cancel reply
        tvCancelReply.setOnClickListener(v -> {
            replyingToComment = null;
            layoutReplyIndicator.setVisibility(View.GONE);
            etComment.setHint("Tulis komentar...");
        });

        // Kirim komentar
        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (text.isEmpty()) return;
            sendComment(text, etComment, layoutReplyIndicator, tvCommentCountSheet);
        });

        // Load komentar realtime
        loadCommentsRealtime(rvComments, layoutNoComments, tvCommentCountSheet);

        // Expand full saat pertama buka
        commentBottomSheet.getBehavior().setState(
                com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        );

        commentBottomSheet.show();
    }

    private void loadCommentsRealtime(RecyclerView rv, View layoutEmpty,
                                      TextView tvCount) {
        if (commentsListener != null) commentsListener.remove();

        commentsListener = db.collection("comments")
                .whereEqualTo("eventId", eventId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    // Hanya skip kalau SEMUA data dari cache DAN ada perubahan pending
                    // Kalau sudah dari server, selalu proses
                    if (snapshots.getMetadata().isFromCache()
                            && snapshots.getMetadata().hasPendingWrites()) return;

                    commentList.clear();
                    int totalCount = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Comment comment = doc.toObject(Comment.class);
                        comment.setCommentId(doc.getId());
                        commentList.add(comment);
                        totalCount++;
                    }

                    commentAdapter.updateData(commentList);
                    tvCount.setText(totalCount + " komentar");

                    // Hitung hanya komentar utama untuk empty state
                    long mainComments = commentList.stream()
                            .filter(c -> c.getParentCommentId() == null
                            || c.getParentCommentId().isEmpty()).count();

                    if (mainComments == 0) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rv.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void sendComment(String text, TextInputEditText etComment,
                             LinearLayout layoutReplyIndicator,
                             TextView tvCount) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUsername == null) return;
        if(text.isEmpty()) return;

        //Ambil nama fresh dari Firestore setiap kirim
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String username = doc.getString("name");
                    if(username == null || username.isEmpty()){
                        username = "Anonim";
                    }
                    final String finalUsername = username;
        Comment comment = new Comment(
                "",
                eventId,
                currentUserId,
                finalUsername,
                text,
                System.currentTimeMillis()
        );

        if (replyingToComment != null){
            comment.setParentCommentId(
                    replyingToComment.getCommentId());
            comment.setReplyToUsername(
                    replyingToComment.getUsername());
        }
        db.collection("comments")
                .add(comment)
                .addOnSuccessListener(ref -> {
                    etComment.setText("");
                    replyingToComment = null;
                    if(layoutReplyIndicator != null){
                        layoutReplyIndicator.setVisibility(View.GONE);
                    }
                    db.collection("events")
                            .document(eventId)
                            .update("commentCount", FieldValue.increment(1));
                    });
                });
    }

    private void deleteComment(Comment comment) {
        // Cek apakah ini komentar utama atau reply
        boolean isMainComment = comment.getParentCommentId() == null
                || comment.getParentCommentId().isEmpty();

        if (isMainComment){
            // Hapus komentar utama + semua reply-nya sekaligus
            deleteMainCommentWithReplies(comment);
        }else {
            db.collection("comments")
                    .document(comment.getCommentId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        db.collection("events").document(eventId)
                                .update("commentCount", FieldValue.increment(-1));
                        Toast.makeText(this, "Komentar dihapus",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteMainCommentWithReplies(Comment comment) {
        android.util.Log.d("DELETE_DEBUG2",
                "Mencoba hapus commentId = " + comment.getCommentId());

        db.collection("comments")
                .whereEqualTo("parentCommentId", comment.getCommentId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    android.util.Log.d("DELETE_DEBUG2",
                            "Reply ditemukan = " + snapshots.size());

                    int totalToDelete = 1 + snapshots.size();
                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    batch.delete(db.collection("comments")
                            .document(comment.getCommentId()));

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("DELETE_DEBUG2", "Berhasil dihapus!");
                                db.collection("events").document(eventId)
                                        .update("commentCount",
                                                FieldValue.increment(-totalToDelete));
                                Toast.makeText(this, "Komentar dihapus",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("DELETE_DEBUG2",
                                        "GAGAL: " + e.getMessage());
                                Toast.makeText(this, "Gagal: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DELETE_DEBUG2",
                            "Query reply gagal: " + e.getMessage());
                });
    }
}