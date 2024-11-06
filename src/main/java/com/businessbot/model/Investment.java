package com.businessbot.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Investment {
    private long id;
    private long userId;
    private String assetType; // STOCK, CRYPTO, INDEX
    private String assetName;
    private double amount;
    private double purchasePrice;
    private double currentPrice;
    private LocalDateTime purchaseDate;
    private boolean active;

    public Investment(long userId, String assetType, String assetName, double amount, double price) {
        this.userId = userId;
        this.assetType = assetType;
        this.assetName = assetName;
        this.amount = amount;
        this.purchasePrice = price;
        this.currentPrice = price;
        this.purchaseDate = LocalDateTime.now();
        this.active = true;
    }

    public long getId() { return id; }
    public String getAssetName() { return assetName; }
    public double getCurrentPrice() { return currentPrice; }

    public void setId(long id) { this.id = id; }
    public void setCurrentPrice(double price) { this.currentPrice = price; }

    public double getProfit() {
        return amount * (currentPrice - purchasePrice);
    }

    public double getProfitPercentage() {
        return ((currentPrice - purchasePrice) / purchasePrice) * 100;
    }
} 