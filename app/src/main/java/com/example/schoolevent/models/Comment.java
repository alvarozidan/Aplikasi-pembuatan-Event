package com.example.schoolevent.models;

public class Comment {

    private String commentId;
    private String eventId;
    private String userId;
    private String username;
    private String text;
    private long timestamp;
    private String parentCommentId; // null = komentar utama, ada isi = balasan
    private String replyToUsername; // nama user yang dibalas

    public Comment(){}

    public Comment(String commentId, String eventId, String userId, String username, String text, long timestamp){
        this.commentId = commentId;
        this.eventId = eventId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getCommentId(){ return  commentId; }
    public String getEventId(){ return  eventId; }
    public String getUserId(){ return userId; }
    public String getUsername(){ return username; }
    public String getText(){ return text; }
    public long getTimestamp(){ return timestamp; }
    public String getParentCommentId() { return parentCommentId; }
    public String getReplyToUsername() { return replyToUsername; }

    public void setCommentId(String commentId){ this.commentId = commentId; }
    public void setEventId(String eventId){ this.eventId= eventId; }
    public void setUserId(String userId){ this.userId = userId; }
    public void setUsername(String username){ this.username = username; }
    public void setText(String text){this.text = text; }
    public void setTimestamp(long timestamp){this.timestamp = timestamp; }
    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
    public void setReplyToUsername(String replyToUsername) {
        this.replyToUsername = replyToUsername;
    }
}
