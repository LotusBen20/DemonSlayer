package com.businessbot.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.businessbot.database.DatabaseManager;
import com.businessbot.model.Investment;

@Service
public class InvestmentService {
    private final DatabaseManager databaseManager;
    private final UserService userService;
    private final Map<String, Double> marketPrices = new HashMap<>();
    private final List<String> stocks = Arrays.asList("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA");
    private final List<String> crypto = Arrays.asList("BTC", "ETH", "BNB", "XRP", "DOGE");
    private final List<String> indices = Arrays.asList("SP500", "NASDAQ", "DOW", "FTSE", "NIKKEI");

    @Autowired
    public InvestmentService(DatabaseManager databaseManager, UserService userService) {
        this.databaseManager = databaseManager;
        this.userService = userService;
        initializeMarket();
    }

    private void initializeMarket() {
        // Инициализация начальных цен
        stocks.forEach(stock -> marketPrices.put(stock, getInitialPrice("STOCK")));
        crypto.forEach(coin -> marketPrices.put(coin, getInitialPrice("CRYPTO")));
        indices.forEach(index -> marketPrices.put(index, getInitialPrice("INDEX")));
    }

    private double getInitialPrice(String type) {
        return switch (type) {
            case "STOCK" -> ThreadLocalRandom.current().nextDouble(50, 1000);
            case "CRYPTO" -> ThreadLocalRandom.current().nextDouble(100, 50000);
            case "INDEX" -> ThreadLocalRandom.current().nextDouble(1000, 10000);
            default -> 100.0;
        };
    }

    public Map<String, Object> getMarketData() {
        updatePrices();
        Map<String, Object> market = new HashMap<>();
        market.put("stocks", getAssetPrices(stocks));
        market.put("crypto", getAssetPrices(crypto));
        market.put("indices", getAssetPrices(indices));
        return market;
    }

    private Map<String, Object> getAssetPrices(List<String> assets) {
        Map<String, Object> prices = new HashMap<>();
        assets.forEach(asset -> {
            double oldPrice = marketPrices.get(asset);
            double newPrice = updatePrice(oldPrice);
            double change = ((newPrice - oldPrice) / oldPrice) * 100;
            marketPrices.put(asset, newPrice);
            
            Map<String, Object> assetData = new HashMap<>();
            assetData.put("price", newPrice);
            assetData.put("change", change);
            prices.put(asset, assetData);
        });
        return prices;
    }

    private double updatePrice(double currentPrice) {
        double changePercent = ThreadLocalRandom.current().nextDouble(-5, 5);
        return currentPrice * (1 + changePercent / 100);
    }

    public boolean buyAsset(long userId, String assetType, String assetName, double amount) {
        double price = marketPrices.getOrDefault(assetName, 0.0);
        double totalCost = price * amount;
        
        var user = userService.getUser(userId);
        if (user.getBalance() < totalCost) {
            return false;
        }

        String sql = """
            INSERT INTO investments (user_id, asset_type, asset_name, amount, purchase_price, current_price, purchase_date, active)
            VALUES (?, ?, ?, ?, ?, ?, datetime('now'), 1)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, assetType);
            pstmt.setString(3, assetName);
            pstmt.setDouble(4, amount);
            pstmt.setDouble(5, price);
            pstmt.setDouble(6, price);
            
            int result = pstmt.executeUpdate();
            if (result > 0) {
                userService.updateBalance(userId, user.getBalance() - totalCost);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public boolean sellAsset(long userId, long investmentId) {
        String sql = "SELECT * FROM investments WHERE id = ? AND user_id = ? AND active = 1";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, investmentId);
            pstmt.setLong(2, userId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double amount = rs.getDouble("amount");
                String assetName = rs.getString("asset_name");
                double currentPrice = marketPrices.getOrDefault(assetName, 0.0);
                double profit = amount * currentPrice;
                
                // Закрываем инвестицию
                String updateSql = "UPDATE investments SET active = 0, current_price = ? WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setDouble(1, currentPrice);
                    updateStmt.setLong(2, investmentId);
                    updateStmt.executeUpdate();
                }
                
                // Начисляем прибыль
                userService.updateBalance(userId, userService.getUser(userId).getBalance() + profit);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public List<Investment> getUserInvestments(long userId) {
        List<Investment> investments = new ArrayList<>();
        String sql = "SELECT * FROM investments WHERE user_id = ? AND active = 1";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Investment investment = new Investment(
                    userId,
                    rs.getString("asset_type"),
                    rs.getString("asset_name"),
                    rs.getDouble("amount"),
                    rs.getDouble("purchase_price")
                );
                investment.setId(rs.getLong("id"));
                investment.setCurrentPrice(marketPrices.getOrDefault(investment.getAssetName(), 0.0));
                investments.add(investment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return investments;
    }

    private void updatePrices() {
        for (String asset : marketPrices.keySet()) {
            double currentPrice = marketPrices.get(asset);
            double newPrice = updatePrice(currentPrice);
            marketPrices.put(asset, newPrice);
        }
    }
} 