package com.example.jonayskieprints.model;

public class UserOrder {
    public String orderId;
    public String service;
    public String serviceType;
    public int    quantity;
    public String status;
    public String specifications;
    public String deliveryOption;
    public String pickupTime;
    public String totalAmount;
    public String paymentMethod;
    public String createdAt;
    public double price;
    public String date;
    public String fileName;

    public String getDisplayId() {
        if (orderId == null || orderId.length() < 6) 
            return orderId != null ? orderId : "—";
        return orderId.substring(orderId.length() - 6);
    }

    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    public boolean isGcash() {
        return paymentMethod != null 
            && paymentMethod.toLowerCase().contains("gcash");
    }

    public double getTotalAmountDouble() {
        if (price > 0) return price;
        try { 
            return Double.parseDouble(totalAmount != null ? totalAmount : "0"); 
        } catch (Exception e) { 
            return 0.0; 
        }
    }

    public String getFormattedPickupTime() {
        return pickupTime != null ? pickupTime : "";
    }
}
