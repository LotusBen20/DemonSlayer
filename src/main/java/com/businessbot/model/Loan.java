package com.businessbot.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class Loan {
    private long id;
    private long userId;
    private double amount;
    private double interestRate;
    private LocalDateTime loanDate;
    private String status;
    private double totalDebt;
    private int term;

    public Loan(long userId, double amount, double interestRate, int term) {
        this.userId = userId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.term = term;
        this.loanDate = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    // Геттеры
    public long getId() { return id; }
    public long getUserId() { return userId; }
    public double getAmount() { return amount; }
    public double getInterestRate() { return interestRate; }
    public LocalDateTime getLoanDate() { return loanDate; }
    public String getStatus() { return status; }
    public double getTotalDebt() { return totalDebt; }
    public int getTerm() { return term; }

    // Сеттеры
    public void setId(long id) { this.id = id; }
    public void setUserId(long userId) { this.userId = userId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public void setLoanDate(LocalDateTime loanDate) { this.loanDate = loanDate; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }
    public void setTerm(int term) { this.term = term; }
} 