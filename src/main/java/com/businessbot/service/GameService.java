package com.businessbot.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.businessbot.database.DatabaseManager;
import com.businessbot.model.Company;
import com.businessbot.model.User;

@Service
public class GameService {
    private final DatabaseManager databaseManager;
    private final UserService userService;

    @Autowired
    public GameService(DatabaseManager databaseManager, UserService userService) {
        this.databaseManager = databaseManager;
        this.userService = userService;
    }

    public double click(long userId) {
        double reward = calculateClickReward(userId);
        User user = userService.getUser(userId);
        userService.updateBalance(userId, user.getBalance() + reward);
        return reward;
    }

    public double calculateHourlyIncome(long userId) {
        double income = 0;
        List<Company> companies = userService.getUserCompanies(userId);
        for (Company company : companies) {
            income += calculateCompanyIncome(company);
        }
        return income;
    }

    private double calculateCompanyIncome(Company company) {
        double baseIncome = switch (company.getType()) {
            case "SHOP" -> 100.0;
            case "FACTORY" -> 300.0;
            case "TECH_COMPANY" -> 1000.0;
            case "BUSINESS_CENTER" -> 2500.0;
            case "BANK" -> 7500.0;
            case "CONSTRUCTION" -> 15000.0;
            case "OIL" -> 35000.0;
            case "CRYPTO" -> 100000.0;
            case "SPACE" -> 250000.0;
            default -> 0.0;
        };
        
        // Увеличиваем доход экспоненциально с уровнем (x2.5 за каждый уровень)
        return baseIncome * Math.pow(2.5, company.getLevel() - 1);
    }

    public double getCompanyCost(String companyType) {
        return switch (companyType) {
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
    }

    public boolean buyCompany(long userId, String companyType) {
        double cost = getCompanyCost(companyType);
        var user = userService.getUser(userId);
        
        if (user.getBalance() >= cost) {
            userService.updateBalance(userId, user.getBalance() - cost);
            
            List<Company> companies = userService.getUserCompanies(userId);
            long sameTypeCount = companies.stream()
                .filter(c -> c.getType().equals(companyType) && c.getLevel() == 1)
                .count();
            
            // Если уже есть 2 компании того же типа (эта будет третьей)
            if (sameTypeCount >= 2) {
                // Сначала удаляем три компании первого уровня
                mergeCompanies(userId, companyType);
                
                // Создаем одну компанию второго уровня
                addCompanyWithLevel(userId, companyType, 2);
                
                // Отправляем уведомление об успешном слиянии
                return true;
            }
            
            // Если недостаточно для слияния, просто добавляем новую компанию
            addCompanyWithLevel(userId, companyType, 1);
            return true;
        }
        return false;
    }

    private void mergeCompanies(long userId, String companyType) {
        String sql = """
            DELETE FROM user_companies 
            WHERE user_id = ? 
            AND company_type = ? 
            AND level = 1 
            ORDER BY id ASC 
            LIMIT 3
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, companyType);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addCompanyWithLevel(long userId, String companyType, int level) {
        String sql = "INSERT INTO user_companies (user_id, company_type, level) VALUES (?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, companyType);
            pstmt.setInt(3, level);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean createDeposit(long userId, String type, double amount) {
        User user = userService.getUser(userId);
        if (user.getBalance() < amount) {
            return false;
        }

        double interestRate = switch (type) {
            case "standard" -> 0.05;
            case "premium" -> 0.08;
            case "vip" -> 0.12;
            default -> 0.0;
        };

        String sql = """
            INSERT INTO deposits (user_id, amount, interest_rate, start_date, active)
            VALUES (?, ?, ?, datetime('now'), 1)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setDouble(2, amount);
            pstmt.setDouble(3, interestRate);
            pstmt.executeUpdate();

            userService.updateBalance(userId, user.getBalance() - amount);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private double calculateClickReward(long userId) {
        var user = userService.getUser(userId);
        double baseReward = 1.0;
        
        // Базовый множитель от мощности
        double powerMultiplier = Math.pow(1.2, user.getMiningPower() - 1);
        
        // Проверяем активные бусты
        Map<String, Object> activeBoosts = getActiveBoosts(userId);
        double boostMultiplier = 1.0;
        
        if (activeBoosts.containsKey("energy")) {
            boostMultiplier *= 2.0; // x2 от буста энергии
        }
        if (activeBoosts.containsKey("efficiency")) {
            boostMultiplier *= 1.5; // x1.5 от буста эффективности
        }
        
        // Добавляем случайность (±10%)
        double randomFactor = 0.9 + Math.random() * 0.2;
        
        return baseReward * powerMultiplier * boostMultiplier * randomFactor;
    }

    public boolean upgradeMiner(long userId) {
        User user = userService.getUser(userId);
        int currentPower = user.getMiningPower();
        double upgradeCost = calculateUpgradeCost(currentPower);
        
        if (user.getBalance() >= upgradeCost) {
            userService.updateBalance(userId, user.getBalance() - upgradeCost);
            
            String sql = "UPDATE users SET mining_power = mining_power + 1 WHERE user_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private double calculateUpgradeCost(int currentPower) {
        double baseCost = 1000.0;
        return baseCost * Math.pow(1.5, currentPower - 1);
    }

    public Map<String, Object> getMiningUpgradeInfo(long userId) {
        User user = userService.getUser(userId);
        int currentPower = user.getMiningPower();
        double upgradeCost = calculateUpgradeCost(currentPower);
        double currentReward = calculateClickReward(userId);
        double nextReward = currentReward * 1.2;
        
        Map<String, Object> info = new HashMap<>();
        info.put("currentPower", currentPower);
        info.put("upgradeCost", upgradeCost);
        info.put("currentReward", currentReward);
        info.put("nextReward", nextReward);
        
        return info;
    }

    public boolean upgradeEnergy(long userId) {
        var user = userService.getUser(userId);
        double cost = 2000.0 * Math.pow(1.5, user.getMiningPower() - 1);
        
        if (user.getBalance() >= cost) {
            userService.updateBalance(userId, user.getBalance() - cost);
            // Активируем буст энергии на 5 минут
            activateBoost(userId, "energy", 300);
            return true;
        }
        return false;
    }

    public boolean upgradeEfficiency(long userId) {
        var user = userService.getUser(userId);
        double cost = 5000.0 * Math.pow(1.5, user.getMiningPower() - 1);
        
        if (user.getBalance() >= cost) {
            userService.updateBalance(userId, user.getBalance() - cost);
            // Активируем буст эффективности на 5 минут
            activateBoost(userId, "efficiency", 300);
            return true;
        }
        return false;
    }

    public int activateBoost(long userId, String type) {
        String sql = """
            INSERT INTO boosts (user_id, boost_type, end_time)
            VALUES (?, ?, datetime('now', '+5 minutes'))
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, type);
            pstmt.executeUpdate();
            return 300; // 5 минут в секундах
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean setAutoMining(long userId, boolean enabled) {
        String sql = "UPDATE users SET auto_mining = ? WHERE user_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enabled ? 1 : 0);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean toggleAutoMining(long userId, boolean enabled) {
        String sql = "UPDATE users SET auto_mining = ? WHERE user_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enabled ? 1 : 0);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void activateBoost(long userId, String boostType, int duration) {
        String sql = """
            INSERT INTO boosts (user_id, boost_type, start_time, end_time)
            VALUES (?, ?, datetime('now'), datetime('now', '+' || ? || ' seconds'))
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, boostType);
            pstmt.setInt(3, duration);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getActiveBoosts(long userId) {
        Map<String, Object> boosts = new HashMap<>();
        String sql = """
            SELECT boost_type, end_time 
            FROM boosts 
            WHERE user_id = ? 
            AND datetime('now') < end_time
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String boostType = rs.getString("boost_type");
                String endTime = rs.getString("end_time");
                boosts.put(boostType, endTime);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return boosts;
    }

    // Метод для автоматического майнинга
    public double autoMine(long userId) {
        var user = userService.getUser(userId);
        // Автоматический майнинг приносит 50% от обычной награды
        double reward = calculateClickReward(userId) * 0.5;
        userService.updateBalance(userId, user.getBalance() + reward);
        return reward;
    }
} 