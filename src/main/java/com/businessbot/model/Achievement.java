package com.businessbot.model;

public class Achievement {
    private long id;
    private String name;
    private String description;
    private String condition;
    private int requiredValue;
    private double reward;
    private boolean completed;

    public Achievement(String name, String description, String condition, int requiredValue, double reward) {
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.requiredValue = requiredValue;
        this.reward = reward;
        this.completed = false;
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCondition() { return condition; }
    public int getRequiredValue() { return requiredValue; }
    public double getReward() { return reward; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
} 