package com.example.schoolevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schoolevent.R;
import com.example.schoolevent.models.Comment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;

    public CommentAdapter(List<Comment> commentList){
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvUserName.setText(comment.getUsername());
        holder.tvCommentText.setText(comment.getText());

        //Mengubah format timestamp dari milidetik ke format waktu yang bisa dibaca
        //SimpleDateFormat -> ini libary buat memformat tanggal/waktu

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(comment.getTimestamp()));
        holder.tvTimeStamp.setText(formattedDate);
    }

    @Override
    public int getItemCount(){
        return commentList.size();
    }

    public void updateData(List<Comment> newList){
        this.commentList = newList;
        notifyDataSetChanged();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder{
        TextView tvUserName, tvCommentText, tvTimeStamp;

        public CommentViewHolder(@NonNull View itemView){
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_username);
            tvCommentText = itemView.findViewById(R.id.tv_comment_text);
            tvTimeStamp = itemView.findViewById(R.id.tv_timestamp);
        }
    }

}
