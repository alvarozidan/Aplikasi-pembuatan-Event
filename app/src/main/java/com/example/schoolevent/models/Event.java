package com.example.schoolevent.models;

public class Event {

    private String eventId;
    private String title;
    private String date;
    private String category;
    private String description;

    public Event(){}

    public Event(String eventId, String title, String date, String category, String description){
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.category = category;
        this.description = description;
    }

    public String getEventId(){ return eventId; }
    public String getTitle(){ return title; }
    public String getDate(){ return date; }
    public String getCategory(){ return  category; }
    public String getDescription(){ return description; }

    public void setEventId(String eventId){ this.eventId = eventId; }
    public void  setTitle(String title){ this.title = title; }
    public void  setDate(String date){ this.date = date; }
    public void  setCategory(String category){ this.category = category; }
    public void setDescription(String description){ this.description = description; }
}
