package com.example.jonayskieprints.model;

import java.util.List;

public class Order {
    // MongoDB fields
    public String _id;
    public String orderId;

    // User info
    public String userName;
    public String userEmail;

    // Order details
    public String service;
    public String serviceType;   // ← from UserOrder
    public int quantity;
    public String status;
    public String specifications;
    public String fileName;      // ← from UserOrder
    public String deliveryOption;
    public String deliveryAddress;
    public String pickupTime;

    // Payment
    public String paymentMethod;
    public String gcashRefNum;
    public String gcashReceiptUrl;
    public double totalAmount;
    public String id;
    public double price;         // ← from UserOrder

    // Dates
    public String createdAt;
    public String date;          // ← from UserOrder

    // Files
    public List<FileData> files;

    // ── Helpers ──────────────────────────────────────────────────

    public String getDisplayId() {
        String id = orderId != null ? orderId : this._id;
        if (id == null || id.length() < 6) return id != null ? id : "——";
        return id.substring(id.length() - 6);
    }

    public boolean isPending()   { return "pending".equalsIgnoreCase(status); }
    public boolean isCompleted() { return "completed".equalsIgnoreCase(status); }
    public boolean isCancelled() { return "cancelled".equalsIgnoreCase(status); }

    public boolean isGcash() {
        return paymentMethod != null && paymentMethod.toLowerCase().contains("gcash");
    }

    public double getTotalAmountDouble() {
        return price > 0 ? price : totalAmount;
    }

    public String getFormattedPickupTime() {
        if (pickupTime == null || pickupTime.isEmpty()) return "";
        try {
            String[] parts = pickupTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            String min = parts[1];
            String ampm = hour >= 12 ? "PM" : "AM";
            int h12 = hour % 12;
            if (h12 == 0) h12 = 12;
            return h12 + ":" + min + " " + ampm;
        } catch (Exception e) { return pickupTime; }
    }

    public static class FileData {
        public String url;
        public String resourceType;
    }
}