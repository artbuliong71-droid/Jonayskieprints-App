package com.example.jonayskieprints.model;

public class Prices {
    public double printBw          = 3.0;
    public double printColor       = 5.0;
    public double photocopying     = 2.0;
    public double photoDevelopment = 15.0;
    public double laminating       = 20.0;
    public double folder           = 10.0;

    public double calcTotal(String service, int qty, String color,
                            String paperSize, String photoSize,
                            boolean addLam, boolean addFolder, int folderQty) {
        double base = 0;
        if (service == null) return 0;
        switch (service) {
            case "Print":
                base = "color".equals(color) ? printColor : printBw;
                break;
            case "Photocopy":
                base = photocopying;
                break;
            case "Photo Development":
                base = photoDevelopment;
                break;
        }
        double total = base * qty;
        if (addLam)    total += laminating * qty;
        if (addFolder) total += folder * folderQty;
        return total;
    }
}
