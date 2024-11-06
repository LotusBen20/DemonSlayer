package com.businessbot.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class Deposit {
    private long id;
    private long userId;
    private double amount;
    private double interestRate;
    private int term;
    private LocalDateTime startDate;
    private boolean active;
    private double currentAmount;

    public Deposit(long userId, double amount, double interestRate, int term) {
        this.userId = userId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.term = term;
        this.startDate = LocalDateTime.now();
        this.active = true;
        this.currentAmount = amount;
    }

    // Геттеры
    public long getId() { return id; }
    public long getUserId() { return userId; }
    public double getAmount() { return amount; }
    public double getInterestRate() { return interestRate; }
    public int getTerm() { return term; }
    public LocalDateTime getStartDate() { return startDate; }
    public boolean isActive() { return active; }
    public double getCurrentAmount() { return currentAmount; }

    // Сеттеры
    public void setId(long id) { this.id = id; }
    public void setUserId(long userId) { this.userId = userId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public void setTerm(int term) { this.term = term; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public void setActive(boolean active) { this.active = active; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }
} 