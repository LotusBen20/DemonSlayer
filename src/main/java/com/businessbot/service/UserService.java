package com.businessbot.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.businessbot.database.DatabaseManager;
import com.businessbot.model.Company;
import com.businessbot.model.Deposit;
import com.businessbot.model.Loan;
import com.businessbot.model.User;

@Service
public class UserService {
    private final DatabaseManager databaseManager;

    @Autowired
    public UserService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void createUserIfNotExists(long userId) {
        String sql = "INSERT OR IGNORE INTO users (user_id, balance) VALUES (?, 1000.0)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUser(long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User(userId);
                user.setBalance(rs.getDouble("balance"));
                user.setVipStatus(rs.getInt("vip_status"));
                user.setMiningPower(rs.getInt("mining_power"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateBalance(long userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE user_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean canClaimDailyBonus(long userId) {
        String sql = "SELECT last_bonus_claim FROM users WHERE user_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String lastClaimStr = rs.getString("last_bonus_claim");
                if (lastClaimStr == null) return true;
                LocalDateTime lastClaim = LocalDateTime.parse(lastClaimStr);
                return LocalDateTime.now().minusHours(24).isAfter(lastClaim);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addCompany(long userId, String companyType) {
        String sql = "INSERT INTO user_companies (user_id, company_type) VALUES (?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, companyType);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Company> getUserCompanies(long userId) {
        List<Company> companies = new ArrayList<>();
        String sql = "SELECT * FROM user_companies WHERE user_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Company company = new Company(userId, rs.getString("company_type"));
                company.setId(rs.getLong("id"));
                company.setLevel(rs.getInt("level"));
                companies.add(company);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return companies;
    }

    public boolean hasActiveLoan(long userId) {
        String sql = """
            SELECT COUNT(*) as loan_count 
            FROM bank_operations 
            WHERE user_id = ? 
            AND operation_type = 'LOAN' 
            AND loan_status = 'ACTIVE' 
            AND loan_date IS NOT NULL
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("loan_count");
                return count > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean takeLoan(long userId, String type) {
        double amount = switch (type) {
            case "quick" -> 10000.0;
            case "business" -> 50000.0;
            case "investment" -> 200000.0;
            default -> 0.0;
        };
        
        double interestRate = switch (type) {
            case "quick" -> 0.10; // 10%
            case "business" -> 0.15; // 15%
            case "investment" -> 0.20; // 20%
            default -> 0.0;
        };
        
        String sql = """
            INSERT INTO bank_operations 
            (user_id, operation_type, amount, interest_rate, loan_date, loan_status, operation_date) 
            VALUES (?, 'LOAN', ?, ?, datetime('now', 'localtime'), 'ACTIVE', datetime('now', 'localtime'))
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setDouble(2, amount);
            pstmt.setDouble(3, interestRate);
            
            int result = pstmt.executeUpdate();
            if (result > 0) {
                // Начисляем деньги пользователю
                User user = getUser(userId);
                updateBalance(userId, user.getBalance() + amount);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, Double> getLoanInfo(long userId) {
        Map<String, Double> loanInfo = new HashMap<>();
        String sql = """
            SELECT amount, interest_rate, loan_date 
            FROM bank_operations 
            WHERE user_id = ? 
            AND operation_type = 'LOAN' 
            AND loan_status = 'ACTIVE' 
            AND loan_date IS NOT NULL
            ORDER BY loan_date DESC 
            LIMIT 1
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double amount = rs.getDouble("amount");
                double rate = rs.getDouble("interest_rate");
                String loanDate = rs.getString("loan_date");
                
                if (loanDate != null) {
                    // Парсим дату в правильном формате SQLite
                    LocalDateTime loanDateTime = LocalDateTime.parse(loanDate.replace(" ", "T"));
                    long daysPass = ChronoUnit.DAYS.between(loanDateTime, LocalDateTime.now());
                    
                    double interest = amount * rate * Math.max(daysPass, 1);
                    double totalDebt = amount + interest;
                    
                    loanInfo.put("amount", amount);
                    loanInfo.put("interest", interest);
                    loanInfo.put("totalDebt", totalDebt);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loanInfo;
    }

    public List<Long> getAllUsers() {
        List<Long> users = new ArrayList<>();
        String sql = "SELECT user_id FROM users";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(rs.getLong("user_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return users;
    }

    public boolean upgradeCompany(long userId, long companyId) {
        String sql = """
            SELECT company_type, level FROM user_companies 
            WHERE id = ? AND user_id = ?
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String companyType = rs.getString("company_type");
                int currentLevel = rs.getInt("level");
                double upgradeCost = calculateUpgradeCost(companyType, currentLevel);
                
                User user = getUser(userId);
                if (user.getBalance() >= upgradeCost) {
                    // Обновляем уровень компании
                    String updateSql = "UPDATE user_companies SET level = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, currentLevel + 1);
                        updateStmt.setLong(2, companyId);
                        updateStmt.executeUpdate();
                        
                        // Снимаем деньги
                        updateBalance(userId, user.getBalance() - upgradeCost);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private double calculateUpgradeCost(String companyType, int currentLevel) {
        double basePrice = switch (companyType) {
            case "SHOP" -> 2500.0;
            case "FACTORY" -> 7500.0;
            case "TECH_COMPANY" -> 25000.0;
            default -> 1000.0;
        };
        return basePrice * Math.pow(1.5, currentLevel - 1);
    }

    public void updateUserProfile(long userId, String nickname, String description) {
        String sql = "UPDATE users SET nickname = ?, description = ? WHERE user_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            pstmt.setString(2, description);
            pstmt.setLong(3, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUserSettings(long userId, Map<String, Object> settings) {
        String sql = """
            UPDATE users SET 
            theme = ?, 
            font_size = ?, 
            notifications_enabled = ?, 
            auto_collect_enabled = ?,
            emojis_enabled = ?,
            animations_enabled = ?,
            language = ?
            WHERE user_id = ?
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, (String) settings.get("theme"));
            pstmt.setInt(2, (Integer) settings.get("fontSize"));
            pstmt.setInt(3, (Boolean) settings.get("notifications") ? 1 : 0);
            pstmt.setInt(4, (Boolean) settings.get("autoCollect") ? 1 : 0);
            pstmt.setInt(5, (Boolean) settings.get("emojis") ? 1 : 0);
            pstmt.setInt(6, (Boolean) settings.get("animations") ? 1 : 0);
            pstmt.setString(7, (String) settings.get("language"));
            pstmt.setLong(8, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getUserSettings(long userId) {
        Map<String, Object> settings = new HashMap<>();
        String sql = """
            SELECT theme, font_size, notifications_enabled, auto_collect_enabled,
                   emojis_enabled, animations_enabled, language, nickname, description,
                   registration_date
            FROM users WHERE user_id = ?
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                settings.put("theme", rs.getString("theme"));
                settings.put("fontSize", rs.getInt("font_size"));
                settings.put("notifications", rs.getInt("notifications_enabled") == 1);
                settings.put("autoCollect", rs.getInt("auto_collect_enabled") == 1);
                settings.put("emojis", rs.getInt("emojis_enabled") == 1);
                settings.put("animations", rs.getInt("animations_enabled") == 1);
                settings.put("language", rs.getString("language"));
                settings.put("nickname", rs.getString("nickname"));
                settings.put("description", rs.getString("description"));
                settings.put("registrationDate", rs.getString("registration_date"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settings;
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

    public boolean repayLoan(long userId, long loanId, double amount) {
        // Проверяем баланс
        User user = getUser(userId);
        if (user.getBalance() < amount) {
            return false;
        }
        
        String sql = """
            UPDATE bank_operations 
            SET loan_status = 'REPAID', 
                operation_date = datetime('now', 'localtime') 
            WHERE id = ? 
            AND user_id = ? 
            AND loan_status = 'ACTIVE'
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, loanId);
            pstmt.setLong(2, userId);
            
            int result = pstmt.executeUpdate();
            if (result > 0) {
                // Списываем деньги
                updateBalance(userId, user.getBalance() - amount);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
} 