package com.example.jonayskieprints;

public class Service {
    private String title, description;
    private int iconResId;

    public Service(String title, String description, int iconResId) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconResId() { return iconResId; }
}