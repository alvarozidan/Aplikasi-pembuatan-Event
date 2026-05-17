package com.example.schoolevent.models;

import java.util.ArrayList;
import java.util.List;

public class Event {

    private String eventId;
    private String title;
    private String date;       // format: "yyyy-MM-dd" untuk parsing kalender
    private String dateDisplay; // format: "25 Desember 2024" untuk tampilan
    private String category;
    private String description;
    private String imageUrl;   // URL gambar dari Firebase Storage
    private List<String> likes;
    private long commentCount;

    public Event() {
        likes = new ArrayList<>();
    }

    public Event(String eventId, String title, String date, String dateDisplay,
                 String category, String description, String imageUrl) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.dateDisplay = dateDisplay;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.likes = new ArrayList<>();
    }

    // Getter
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getDateDisplay() { return dateDisplay; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getLikes() { return likes; }
    public long getCommentCount() { return commentCount; }

    // Setter
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setDateDisplay(String dateDisplay) { this.dateDisplay = dateDisplay; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setLikes(List<String> likes) { this.likes = likes; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
}