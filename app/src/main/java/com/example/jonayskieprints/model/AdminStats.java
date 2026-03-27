package com.example.jonayskieprints.model;

public class AdminStats {
    public int totalOrders;
    public int pendingOrders;
    public int inProgressOrders;
    public int completedOrders;
    public int cancelledOrders;
    public int totalCustomers;
    public String totalSales;

    public int getCompletionRate() {
        if (totalOrders == 0) return 0;
        return Math.round((completedOrders * 100f) / totalOrders);
    }
}
