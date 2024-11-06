package com.businessbot.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Component;

@Component
public class DatabaseManager {
    private final String DB_URL = "jdbc:sqlite:bot_database.db";

    public DatabaseManager() {
        initializeDatabase();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                // Таблица пользователей
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id INTEGER PRIMARY KEY,
                        balance REAL DEFAULT 1000.0,
                        mining_power INTEGER DEFAULT 1,
                        vip_status INTEGER DEFAULT 0,
                        last_mining TEXT,
                        income_interval INTEGER DEFAULT 3600,
                        nickname TEXT,
                        description TEXT,
                        theme TEXT DEFAULT 'light',
                        font_size INTEGER DEFAULT 14,
                        notifications_enabled INTEGER DEFAULT 1,
                        auto_collect_enabled INTEGER DEFAULT 0,
                        emojis_enabled INTEGER DEFAULT 1,
                        animations_enabled INTEGER DEFAULT 1,
                        language TEXT DEFAULT 'ru',
                        registration_date TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                // Таблица компаний
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS user_companies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        company_type TEXT,
                        level INTEGER DEFAULT 1,
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
                    )
                """);

                // Таблица банковских операций
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS bank_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        operation_type TEXT,
                        amount REAL,
                        interest_rate REAL,
                        loan_date TEXT,
                        loan_status TEXT,
                        operation_date TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
                    )
                """);

                // Таблица депозитов
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS deposits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        amount REAL,
                        interest_rate REAL,
                        term INTEGER,
                        start_date TEXT,
                        active INTEGER DEFAULT 1,
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
                    )
                """);

                // Таблица статистики
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS statistics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        active_users INTEGER DEFAULT 0,
                        commands_processed INTEGER DEFAULT 0,
                        successful_commands INTEGER DEFAULT 0
                    )
                """);

                // Вставляем начальную запись в статистику, если её нет
                stmt.execute("""
                    INSERT OR IGNORE INTO statistics (id, active_users, commands_processed, successful_commands) 
                    VALUES (1, 0, 0, 0)
                """);

                // Таблица бустов
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS boosts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        boost_type TEXT,
                        start_time TEXT DEFAULT CURRENT_TIMESTAMP,
                        end_time TEXT,
                        active INTEGER DEFAULT 1,
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
                    )
                """);

                // Таблица улучшений
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS upgrades (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        upgrade_type TEXT,
                        level INTEGER DEFAULT 1,
                        last_upgrade TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
                    )
                """);

                // Добавляем новые колонки в таблицу users
                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN auto_mining INTEGER DEFAULT 0");
                    stmt.execute("ALTER TABLE users ADD COLUMN energy_level INTEGER DEFAULT 1");
                    stmt.execute("ALTER TABLE users ADD COLUMN efficiency_level INTEGER DEFAULT 1");
                } catch (SQLException e) {
                    // Колонки уже существуют
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getActiveUsersCount() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT active_users FROM statistics WHERE id = 1")) {
            
            if (rs.next()) {
                return rs.getInt("active_users");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getCommandsProcessedCount() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT commands_processed FROM statistics WHERE id = 1")) {
            
            if (rs.next()) {
                return rs.getInt("commands_processed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getSuccessRate() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                 SELECT 
                     CASE 
                         WHEN commands_processed = 0 THEN 0
                         ELSE (successful_commands * 100 / commands_processed)
                     END as success_rate
                 FROM statistics 
                 WHERE id = 1
             """)) {
            
            if (rs.next()) {
                return rs.getInt("success_rate");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void incrementActiveUsers() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE statistics SET active_users = active_users + 1 WHERE id = 1");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void incrementCommandsProcessed(boolean successful) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE statistics SET commands_processed = commands_processed + 1" +
                    (successful ? ", successful_commands = successful_commands + 1" : "") +
                    " WHERE id = 1");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Вспомогательный метод для выполнения SQL-запросов
    public void executeUpdate(String sql) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Вспомогательный метод для выполнения SQL-запросов с возвратом ResultSet
    public ResultSet executeQuery(String sql) throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(sql);
    }
} 