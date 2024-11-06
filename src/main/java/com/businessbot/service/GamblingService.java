package com.businessbot.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.businessbot.database.DatabaseManager;

@Service
public class GamblingService {
    private final DatabaseManager databaseManager;
    private final UserService userService;
    private final Random random = new Random();

    @Autowired
    public GamblingService(DatabaseManager databaseManager, UserService userService) {
        this.databaseManager = databaseManager;
        this.userService = userService;
    }

    public Map<String, Object> playDice(long userId, double bet) {
        Map<String, Object> result = new HashMap<>();
        
        var user = userService.getUser(userId);
        if (user.getBalance() < bet) {
            result.put("success", false);
            result.put("message", "Недостаточно средств");
            return result;
        }

        int playerRoll = random.nextInt(6) + 1;
        int botRoll = random.nextInt(6) + 1;
        
        result.put("playerRoll", playerRoll);
        result.put("botRoll", botRoll);
        
        if (playerRoll > botRoll) {
            double win = bet * 1.9;
            userService.updateBalance(userId, user.getBalance() + win - bet);
            result.put("success", true);
            result.put("win", win);
            result.put("message", "Победа! +" + win);
        } else if (playerRoll < botRoll) {
            userService.updateBalance(userId, user.getBalance() - bet);
            result.put("success", false);
            result.put("win", 0);
            result.put("message", "Поражение! -" + bet);
        } else {
            result.put("success", true);
            result.put("win", bet);
            result.put("message", "Ничья! Ставка возвращена");
        }
        
        return result;
    }

    public Map<String, Object> playSlots(long userId, double bet) {
        Map<String, Object> result = new HashMap<>();
        
        var user = userService.getUser(userId);
        if (user.getBalance() < bet) {
            result.put("success", false);
            result.put("message", "Недостаточно средств");
            return result;
        }

        String[] symbols = {"🍎", "🍋", "🍒", "💎", "7️⃣"};
        String[] rolls = new String[3];
        for (int i = 0; i < 3; i++) {
            rolls[i] = symbols[random.nextInt(symbols.length)];
        }
        
        result.put("rolls", rolls);
        
        if (rolls[0].equals(rolls[1]) && rolls[1].equals(rolls[2])) {
            double win = switch (rolls[0]) {
                case "7️⃣" -> bet * 10;
                case "💎" -> bet * 7;
                case "🍒" -> bet * 5;
                default -> bet * 3;
            };
            userService.updateBalance(userId, user.getBalance() + win - bet);
            result.put("success", true);
            result.put("win", win);
            result.put("message", "Джекпот! +" + win);
        } else if (rolls[0].equals(rolls[1]) || rolls[1].equals(rolls[2])) {
            double win = bet * 1.5;
            userService.updateBalance(userId, user.getBalance() + win - bet);
            result.put("success", true);
            result.put("win", win);
            result.put("message", "Маленькая победа! +" + win);
        } else {
            userService.updateBalance(userId, user.getBalance() - bet);
            result.put("success", false);
            result.put("win", 0);
            result.put("message", "Поражение! -" + bet);
        }
        
        return result;
    }

    public Map<String, Object> playNumberGuess(long userId, int guess, double bet) {
        Map<String, Object> result = new HashMap<>();
        
        var user = userService.getUser(userId);
        if (user.getBalance() < bet) {
            result.put("success", false);
            result.put("message", "Недостаточно средств");
            return result;
        }

        int number = random.nextInt(100) + 1;
        result.put("number", number);
        
        if (guess == number) {
            double win = bet * 95;
            userService.updateBalance(userId, user.getBalance() + win - bet);
            result.put("success", true);
            result.put("win", win);
            result.put("message", "Точное попадание! +" + win);
        } else if (Math.abs(guess - number) <= 5) {
            double win = bet * 3;
            userService.updateBalance(userId, user.getBalance() + win - bet);
            result.put("success", true);
            result.put("win", win);
            result.put("message", "Близко! +" + win);
        } else {
            userService.updateBalance(userId, user.getBalance() - bet);
            result.put("success", false);
            result.put("win", 0);
            result.put("message", "Мимо! -" + bet);
        }
        
        return result;
    }
} 