package com.businessbot.model;

import java.time.LocalDateTime;

public class Miner {
    private long userId;
    private int level;
    private double hashRate;
    private LocalDateTime lastMining;

    public Miner(long userId) {
        this.userId = userId;
        this.level = 1;
        this.hashRate = 0.1;
        this.lastMining = LocalDateTime.now();
    }

    public double calculateReward() {
        return hashRate * level * 100;
    }

    // Геттеры и сеттеры
    public long getUserId() { return userId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { 
        this.level = level;
        this.hashRate = 0.1 * level;
    }
    public double getHashRate() { return hashRate; }
    public LocalDateTime getLastMining() { return lastMining; }
    public void setLastMining(LocalDateTime lastMining) { this.lastMining = lastMining; }
} 