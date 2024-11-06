package com.businessbot.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BankOperation {
    private long id;
    private long userId;
    private String operationType;
    private double amount;
    private double interestRate;
    private LocalDateTime loanDate;
    private String loanStatus;
    private LocalDateTime operationDate;
    private double totalDebt;

    public BankOperation(long userId, String operationType, double amount) {
        this.userId = userId;
        this.operationType = operationType;
        this.amount = amount;
        this.operationDate = LocalDateTime.now();
    }

    // Геттеры
    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getOperationType() { return operationType; }
    public double getAmount() { return amount; }
    public double getInterestRate() { return interestRate; }
    public LocalDateTime getLoanDate() { return loanDate; }
    public String getLoanStatus() { return loanStatus; }
    public LocalDateTime getOperationDate() { return operationDate; }
    public double getTotalDebt() { return totalDebt; }

    // Сеттеры
    public void setId(long id) { this.id = id; }
    public void setUserId(long userId) { this.userId = userId; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public void setLoanDate(LocalDateTime loanDate) { this.loanDate = loanDate; }
    public void setLoanStatus(String loanStatus) { this.loanStatus = loanStatus; }
    public void setOperationDate(LocalDateTime operationDate) { this.operationDate = operationDate; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }
} 