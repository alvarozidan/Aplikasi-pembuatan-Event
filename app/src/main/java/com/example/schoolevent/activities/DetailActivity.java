package com.example.schoolevent.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.adapters.CommentAdapter;
import com.example.schoolevent.models.Comment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvDate, tvCategory, tvDescription;
    private TextView tvNoComments;
    private RecyclerView rvComments;
    private View layoutCommentInput;
    private TextInputEditText etComment;
    private MaterialButton btnSendComment, btnLoginToComment, btnDeleteEvent;
    private Toolbar toolbar;

    //Ini buat data event yang akan diterima dari Intent
    private String eventId, eventTitle, eventDate,eventCategory, eventDescription;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activuty_detail);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tv_title);
        tvDate = findViewById(R.id.tv_date);
        tvCategory = findViewById(R.id.tv_category);
        tvDescription = findViewById(R.id.tv_description);
        tvNoComments = findViewById(R.id.tv_no_comments);
        rvComments = findViewById(R.id.rv_comments);
        layoutCommentInput = findViewById(R.id.layout_comment_input);
        etComment = findViewById(R.id.et_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
        btnLoginToComment = findViewById(R.id.btn_login_to_comment);
        btnDeleteEvent = findViewById(R.id.btn_delete_event);

        //Setup toolbar dengan tombol back
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Ambil data event dari Intent yang dikirim HomeFragment
        // getIntent() → mengambil Intent yang membuka Activity ini
        // getStringExtra("key") → mengambil data String berdasarkan key
        eventId = getIntent().getStringExtra("event_id");
        eventTitle = getIntent().getStringExtra("event_title");
        eventDate = getIntent().getStringExtra("event_date");
        eventCategory = getIntent().getStringExtra("event_category");
        eventDescription = getIntent().getStringExtra("event_description");

        //Menampilkan data event ke komponen UI
        toolbar.setTitle(eventTitle);
        tvTitle.setText(eventTitle);
        tvDate.setText(eventDate);
        tvCategory.setText(eventCategory);
        tvDescription.setText(eventDescription);

        //Setup RecyclerView komentar
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(commentList);
        rvComments.setAdapter(commentAdapter);

        //Cek status login & role untuk mengatur tampilan
        setupCommentSection();
        checkAdminStatus();

        loadComments();

        btnSendComment.setOnClickListener(v -> sendComment());

        btnLoginToComment.setOnClickListener(v -> {
            finish(); //Tutup detail activity
            //main activity tampil lagi, dan user bisa login dari navbar
        });

        btnDeleteEvent.setOnClickListener(v -> confirmDeleteEvent());
    }
        //penanganan tombol back di toolbar
        @Override
        public boolean onSupportNavigateUp(){
            finish();
            return true;
        }

        private void setupCommentSection(){
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if(currentUser != null){
                //kalau sudah login
                layoutCommentInput.setVisibility(View.VISIBLE);
                btnLoginToComment.setVisibility(View.GONE);
            }else{
                //kalau belum login
                layoutCommentInput.setVisibility(View.GONE);
                btnLoginToComment.setVisibility(View.VISIBLE);
            }
        }

        private void checkAdminStatus() {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if(currentUser == null ) return;

            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            if("admin".equals(role)) {
                                //kalau admin tombol delete ditampilkan
                                btnDeleteEvent.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }

        private void loadComments(){
            // Query → mengambil data dengan kondisi tertentu
            // whereEqualTo("eventId", eventId) → hanya ambil komentar milik event ini
            // orderBy("timestamp", ASCENDING) → urutkan dari komentar terlama

            db.collection("comments")
                    .whereEqualTo("eventId", eventId)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        commentList.clear();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots){
                            Comment comment = doc.toObject(Comment.class);
                            comment.setCommentId(doc.getId());
                            commentList.add(comment);
                        }

                        commentAdapter.updateData(commentList);

                        //kalau tidak ada komentar tampilkan pesan
                        if (commentList.isEmpty()) {
                            tvNoComments.setVisibility(View.VISIBLE);
                        }else {
                            tvNoComments.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Gagal memuat komentar : " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }

        private void sendComment(){
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if(currentUser == null) return;

            String commentText = etComment.getText().toString().trim();
            if(commentText.isEmpty()){
                Toast.makeText(this, "Komentar tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            //Ambil data user dari Firestore untuk mendapatkan nama user
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String username = documentSnapshot.getString("name");

                        //Buat objek Comment baru
                        // System.currentTimeMillis() → waktu sekarang dalam milidetik
                        Comment comment = new Comment(
                                "",                             // commentId dikosongkan dulu
                                eventId,                        // id event ini
                                currentUser.getUid(),           // id user yang komentar
                                username,                       // nama user
                                commentText,                    // isi komentar
                                System.currentTimeMillis()      // waktu sekarang
                        );
                        // Simpan ke Firestore
                        // add() → simpan dokumen dengan ID otomatis dari Firebase
                        // (berbeda dengan set() yang butuh ID manual)
                        db.collection("comments")
                                .add(comment)
                                .addOnSuccessListener(documentReference -> {
                                    //Kosongkan input setelah berhasil dikirim
                                    etComment.setText("");
                                    Toast.makeText(this,
                                            "Komentar Terkirim!",
                                            Toast.LENGTH_SHORT).show();
                                    //refresh list komentar
                                    loadComments();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "Gagal kirim komentar: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    });
        }

        private void confirmDeleteEvent() {
            // AlertDialog → popup konfirmasi sebelum hapus
            // Mencegah user tidak sengaja menghapus event
            new AlertDialog.Builder(this)
                    .setTitle("Hapus Event")
                    .setMessage("Yakin ingin menghapus event ini?")
                    .setPositiveButton("Ya, Hapus", (dialog, which) -> deleteEvent())
                    .setNegativeButton("Batal", null)
                    .show();
        }

        private void deleteEvent(){
            db.collection("events")
                    .document(eventId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this,
                                "Event berhasil dihapus",
                                Toast.LENGTH_SHORT).show();
                        //kembali ke HomeFragment setelah hapus
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Gagal menghapus event: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
}
