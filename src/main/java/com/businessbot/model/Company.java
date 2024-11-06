package com.businessbot.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class Company {
    private long id;
    private long userId;
    private String type;
    private int level;
    private double income;

    public Company(long userId, String type) {
        this.userId = userId;
        this.type = type;
        this.level = 1;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getType() { return type; }
    public int getLevel() { return level; }
    public double getIncome() { return income; }

    public void setId(long id) { this.id = id; }
    public void setUserId(long userId) { this.userId = userId; }
    public void setType(String type) { this.type = type; }
    public void setLevel(int level) { this.level = level; }
    public void setIncome(double income) { this.income = income; }

    public double calculateIncome() {
        return switch (type) {
            case "SHOP" -> 100.0 * level;
            case "FACTORY" -> 300.0 * level;
            case "TECH_COMPANY" -> 1000.0 * level;
            case "BUSINESS_CENTER" -> 2500.0 * level;
            case "BANK" -> 7500.0 * level;
            case "CONSTRUCTION" -> 15000.0 * level;
            case "OIL" -> 35000.0 * level;
            case "CRYPTO" -> 100000.0 * level;
            case "SPACE" -> 250000.0 * level;
            default -> 0.0;
        };
    }

    public double getUpgradeCost() {
        double baseCost = switch (type) {
            case "SHOP" -> 5000.0;
            case "FACTORY" -> 15000.0;
            case "TECH_COMPANY" -> 50000.0;
            case "BUSINESS_CENTER" -> 100000.0;
            case "BANK" -> 250000.0;
            case "CONSTRUCTION" -> 500000.0;
            case "OIL" -> 1000000.0;
            case "CRYPTO" -> 2500000.0;
            case "SPACE" -> 5000000.0;
            default -> Double.MAX_VALUE;
        };
        return baseCost * Math.pow(2, level - 1);
    }
} 