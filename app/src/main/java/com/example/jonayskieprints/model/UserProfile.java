package com.example.jonayskieprints.model;

public class UserProfile {
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public String role = "customer"; // "customer" or "admin"

    public String getFullName() {
        String fn = firstName != null ? firstName : "";
        String ln = lastName  != null ? lastName  : "";
        return (fn + " " + ln).trim();
    }

    public String getInitials() {
        String fn = firstName != null ? firstName : "";
        String ln = lastName  != null ? lastName  : "";
        String i  = "";
        if (!fn.isEmpty()) i += fn.charAt(0);
        if (!ln.isEmpty()) i += ln.charAt(0);
        return i.toUpperCase();
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}