package com.businessbot.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.businessbot.database.DatabaseManager;
import com.businessbot.model.BankOperation;
import com.businessbot.model.Deposit;
import com.businessbot.model.Loan;

@Service
public class BankService {
    private final DatabaseManager databaseManager;
    private final UserService userService;

    @Autowired
    public BankService(DatabaseManager databaseManager, UserService userService) {
        this.databaseManager = databaseManager;
        this.userService = userService;
    }

    public List<Deposit> getUserDeposits(long userId) {
        List<Deposit> deposits = new ArrayList<>();
        String sql = "SELECT * FROM deposits WHERE user_id = ? AND active = 1";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Deposit deposit = new Deposit(
                    userId,
                    rs.getDouble("amount"),
                    rs.getDouble("interest_rate"),
                    rs.getInt("term")
                );
                deposit.setId(rs.getLong("id"));
                deposit.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                deposit.setActive(rs.getBoolean("active"));
                deposits.add(deposit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return deposits;
    }

    public List<Loan> getUserLoans(long userId) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM bank_operations WHERE user_id = ? AND operation_type = 'LOAN' AND loan_status = 'ACTIVE'";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Loan loan = new Loan(
                    userId,
                    rs.getDouble("amount"),
                    rs.getDouble("interest_rate"),
                    30 // default term
                );
                loan.setId(rs.getLong("id"));
                loan.setLoanDate(rs.getTimestamp("loan_date").toLocalDateTime());
                loan.setStatus(rs.getString("loan_status"));
                loans.add(loan);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return loans;
    }

    public boolean createDeposit(long userId, String type, double amount) {
        double interestRate = switch (type) {
            case "standard" -> 0.05;
            case "premium" -> 0.08;
            case "vip" -> 0.12;
            default -> 0.0;
        };
        
        int term = switch (type) {
            case "standard" -> 1;
            case "premium" -> 3;
            case "vip" -> 7;
            default -> 0;
        };

        String sql = """
            INSERT INTO deposits (user_id, amount, interest_rate, term, start_date, active)
            VALUES (?, ?, ?, ?, datetime('now'), 1)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setDouble(2, amount);
            pstmt.setDouble(3, interestRate);
            pstmt.setInt(4, term);
            
            int result = pstmt.executeUpdate();
            if (result > 0) {
                userService.updateBalance(userId, userService.getUser(userId).getBalance() - amount);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public void processDeposits() {
        String sql = """
            SELECT * FROM deposits 
            WHERE active = 1 
            AND datetime('now') <= datetime(start_date, '+' || term || ' days')
        """;
        
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                long userId = rs.getLong("user_id");
                double amount = rs.getDouble("amount");
                double interestRate = rs.getDouble("interest_rate");
                
                // Начисляем проценты
                double interest = amount * interestRate / 365.0; // дневной процент
                userService.updateBalance(userId, userService.getUser(userId).getBalance() + interest);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<BankOperation> getOperationHistory(long userId) {
        List<BankOperation> operations = new ArrayList<>();
        String sql = "SELECT * FROM bank_operations WHERE user_id = ? ORDER BY operation_date DESC LIMIT 50";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                BankOperation operation = new BankOperation(
                    userId,
                    rs.getString("operation_type"),
                    rs.getDouble("amount")
                );
                operation.setId(rs.getLong("id"));
                operation.setOperationDate(rs.getTimestamp("operation_date").toLocalDateTime());
                operations.add(operation);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return operations;
    }
} 