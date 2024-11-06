package com.businessbot;

import com.businessbot.database.DatabaseManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(Main.class, args);
        DatabaseManager databaseManager = context.getBean(DatabaseManager.class);
        // База данных инициализируется автоматически в конструкторе DatabaseManager
    }
} 