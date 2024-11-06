package com.businessbot.service;

import com.businessbot.model.Achievement;
import com.businessbot.model.Quest;
import com.businessbot.model.User;
import com.businessbot.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AchievementService {
    private final Map<Long, List<Achievement>> userAchievements = new ConcurrentHashMap<>();
    private final Map<Long, List<Quest>> userQuests = new ConcurrentHashMap<>();
    private final UserService userService;
    private final DatabaseManager databaseManager;

    @Autowired
    public AchievementService(UserService userService, DatabaseManager databaseManager) {
        this.userService = userService;
        this.databaseManager = databaseManager;
        initializeAchievements();
    }

    private void initializeAchievements() {
        String sql = """
            CREATE TABLE IF NOT EXISTS achievements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                name TEXT,
                description TEXT,
                condition TEXT,
                required_value INTEGER,
                reward REAL,
                completed INTEGER DEFAULT 0,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
        """;

        String questsSql = """
            CREATE TABLE IF NOT EXISTS quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                name TEXT,
                description TEXT,
                type TEXT,
                required_value INTEGER,
                current_value INTEGER DEFAULT 0,
                reward REAL,
                expiry_date TEXT,
                completed INTEGER DEFAULT 0,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
        """;

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(questsSql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void checkAchievements(long userId) {
        User user = userService.getUser(userId);
        List<Achievement> achievements = getUserAchievements(userId);

        for (Achievement achievement : achievements) {
            if (!achievement.isCompleted()) {
                switch (achievement.getCondition()) {
                    case "BALANCE" -> checkBalanceAchievement(user, achievement);
                    case "COMPANIES" -> checkCompaniesAchievement(user, achievement);
                    case "MINING_POWER" -> checkMiningPowerAchievement(user, achievement);
                    case "DAILY_STREAK" -> checkDailyStreakAchievement(user, achievement);
                }
            }
        }
    }

    private void checkBalanceAchievement(User user, Achievement achievement) {
        if (user.getBalance() >= achievement.getRequiredValue()) {
            completeAchievement(user.getUserId(), achievement);
        }
    }

    private void checkCompaniesAchievement(User user, Achievement achievement) {
        int companiesCount = userService.getUserCompanies(user.getUserId()).size();
        if (companiesCount >= achievement.getRequiredValue()) {
            completeAchievement(user.getUserId(), achievement);
        }
    }

    private void checkMiningPowerAchievement(User user, Achievement achievement) {
        if (user.getMiningPower() >= achievement.getRequiredValue()) {
            completeAchievement(user.getUserId(), achievement);
        }
    }

    private void checkDailyStreakAchievement(User user, Achievement achievement) {
        // Реализация проверки ежедневного стрика
    }

    private void completeAchievement(long userId, Achievement achievement) {
        String sql = "UPDATE achievements SET completed = 1 WHERE user_id = ? AND id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, achievement.getId());
            pstmt.executeUpdate();

            // Выдаем награду
            User user = userService.getUser(userId);
            userService.updateBalance(userId, user.getBalance() + achievement.getReward());
            
            // Обновляем кэш
            achievement.setCompleted(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateDailyQuests(long userId) {
        // Очищаем старые квесты
        clearExpiredQuests(userId);

        // Генерируем новые квесты
        List<Quest> dailyQuests = new ArrayList<>();
        dailyQuests.add(new Quest(
            "Майнер дня",
            "Добудьте 1000 монет майнингом",
            "MINING",
            1000,
            100.0
        ));
        dailyQuests.add(new Quest(
            "Бизнесмен",
            "Получите доход от компаний",
            "COMPANY_INCOME",
            5000,
            200.0
        ));
        dailyQuests.add(new Quest(
            "Азартный игрок",
            "Выиграйте в мини-играх",
            "GAMBLING_WINS",
            3,
            150.0
        ));

        // Сохраняем новые квесты
        saveQuests(userId, dailyQuests);
    }

    private void clearExpiredQuests(long userId) {
        String sql = "DELETE FROM quests WHERE user_id = ? AND expiry_date < datetime('now')";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveQuests(long userId, List<Quest> quests) {
        String sql = """
            INSERT INTO quests (user_id, name, description, type, required_value, reward, expiry_date)
            VALUES (?, ?, ?, ?, ?, ?, datetime('now', '+1 day'))
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Quest quest : quests) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, quest.getName());
                pstmt.setString(3, quest.getDescription());
                pstmt.setString(4, quest.getType());
                pstmt.setInt(5, quest.getRequiredValue());
                pstmt.setDouble(6, quest.getReward());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateQuestProgress(long userId, String questType, int progress) {
        String sql = """
            UPDATE quests 
            SET current_value = current_value + ?, 
                completed = CASE WHEN current_value + ? >= required_value THEN 1 ELSE 0 END
            WHERE user_id = ? AND type = ? AND completed = 0
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, progress);
            pstmt.setInt(2, progress);
            pstmt.setLong(3, userId);
            pstmt.setString(4, questType);
            pstmt.executeUpdate();

            // Проверяем, завершился ли квест
            checkQuestCompletion(userId, questType);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkQuestCompletion(long userId, String questType) {
        String sql = """
            SELECT * FROM quests 
            WHERE user_id = ? AND type = ? AND completed = 1 AND reward_claimed = 0
        """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, questType);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                double reward = rs.getDouble("reward");
                User user = userService.getUser(userId);
                userService.updateBalance(userId, user.getBalance() + reward);

                // Отмечаем награду как полученную
                String updateSql = "UPDATE quests SET reward_claimed = 1 WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, rs.getLong("id"));
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Achievement> getUserAchievements(long userId) {
        List<Achievement> achievements = userAchievements.computeIfAbsent(userId, k -> {
            List<Achievement> newAchievements = new ArrayList<>();
            String sql = "SELECT * FROM achievements WHERE user_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Achievement achievement = new Achievement(
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("condition"),
                        rs.getInt("required_value"),
                        rs.getDouble("reward")
                    );
                    achievement.setId(rs.getLong("id"));
                    achievement.setCompleted(rs.getInt("completed") == 1);
                    newAchievements.add(achievement);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return newAchievements;
        });
        return achievements;
    }
} 