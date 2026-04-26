package com.example.schoolevent.models;

import java.util.ArrayList;
import java.util.List;
public class Event {

    private String eventId;
    private String title;
    private String date;
    private String category;
    private String description;

    // List userId yang sudah like event ini
    // Menyimpan UID user agar 1 user hanya bisa like 1 kali
    private List<String> likes;
    private long commentCount;

    public Event(){
        likes = new ArrayList<>();
    }

    public Event(String eventId, String title, String date, String category, String description){
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.category = category;
        this.description = description;
        this.likes = new ArrayList<>();
    }

    public String getEventId(){ return eventId; }
    public String getTitle(){ return title; }
    public String getDate(){ return date; }
    public String getCategory(){ return  category; }
    public String getDescription(){ return description; }
    public List<String> getLikes(){ return likes; }
    public long getCommentCount(){ return commentCount; }


    public void setEventId(String eventId){ this.eventId = eventId; }
    public void  setTitle(String title){ this.title = title; }
    public void  setDate(String date){ this.date = date; }
    public void  setCategory(String category){ this.category = category; }
    public void setDescription(String description){ this.description = description; }
    public void setLikes (List<String> likes) { this.likes = likes; }
    public void setCommentCount(long commentCount ){ this.commentCount = commentCount; }

}
