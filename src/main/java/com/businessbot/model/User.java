package com.businessbot.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class User {
    private long userId;
    private double balance;
    private int miningPower;
    private int vipStatus;
    private String nickname;
    private String description;

    public User(long userId) {
        this.userId = userId;
        this.balance = 1000.0;
        this.miningPower = 1;
        this.vipStatus = 0;
    }

    // Геттеры
    public long getUserId() { return userId; }
    public double getBalance() { return balance; }
    public int getMiningPower() { return miningPower; }
    public int getVipStatus() { return vipStatus; }
    public String getNickname() { return nickname; }
    public String getDescription() { return description; }

    // Сеттеры
    public void setUserId(long userId) { this.userId = userId; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setMiningPower(int miningPower) { this.miningPower = miningPower; }
    public void setVipStatus(int vipStatus) { this.vipStatus = vipStatus; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setDescription(String description) { this.description = description; }
} 