package com.businessbot.model;

import java.time.LocalDateTime;

public class Quest {
    private long id;
    private String name;
    private String description;
    private String type;
    private int requiredValue;
    private int currentValue;
    private double reward;
    private LocalDateTime expiryDate;
    private boolean completed;

    public Quest(String name, String description, String type, int requiredValue, double reward) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.requiredValue = requiredValue;
        this.reward = reward;
        this.currentValue = 0;
        this.completed = false;
        this.expiryDate = LocalDateTime.now().plusDays(1); // Квесты обновляются каждый день
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public int getRequiredValue() { return requiredValue; }
    public int getCurrentValue() { return currentValue; }
    public void setCurrentValue(int currentValue) { this.currentValue = currentValue; }
    public double getReward() { return reward; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public double getProgress() {
        return (double) currentValue / requiredValue * 100;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
} 