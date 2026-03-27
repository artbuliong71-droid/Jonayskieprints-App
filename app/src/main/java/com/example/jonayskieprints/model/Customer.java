package com.example.jonayskieprints.model;

public class Customer {
    public String id;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public int totalOrders;
    public String createdAt;

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
