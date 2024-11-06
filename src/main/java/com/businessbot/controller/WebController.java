package com.businessbot.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.businessbot.database.DatabaseManager;
import com.businessbot.model.Company;
import com.businessbot.model.User;
import com.businessbot.service.GameService;
import com.businessbot.service.UserService;

@Controller
public class WebController {
    private final DatabaseManager databaseManager;
    private final UserService userService;
    private final GameService gameService;

    @Autowired
    public WebController(DatabaseManager databaseManager, UserService userService, GameService gameService) {
        this.databaseManager = databaseManager;
        this.userService = userService;
        this.gameService = gameService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Получаем общую статистику
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("activeUsers", databaseManager.getActiveUsersCount());
        return "index";
    }

    @GetMapping("/player/{userId}")
    public String playerStats(@PathVariable long userId, Model model) {
        User user = userService.getUser(userId);
        if (user != null) {
            // Основная информация о пользователе
            model.addAttribute("user", user);
            
            // Компании пользователя
            List<Company> companies = userService.getUserCompanies(userId);
            model.addAttribute("companies", companies);
            
            // Доход
            double hourlyIncome = gameService.calculateHourlyIncome(userId);
            model.addAttribute("hourlyIncome", hourlyIncome);
            
            // Статистика игр
            Map<String, Object> gameStats = new HashMap<>();
            // TODO: Добавить реальную статистику игр
            model.addAttribute("gameStats", gameStats);
            
            // Настройки пользователя
            Map<String, Object> settings = userService.getUserSettings(userId);
            model.addAttribute("settings", settings);
            
            return "player";
        }
        return "redirect:/";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        // Общая статистика
        model.addAttribute("activeUsers", databaseManager.getActiveUsersCount());
        model.addAttribute("commandsProcessed", databaseManager.getCommandsProcessedCount());
        model.addAttribute("successRate", databaseManager.getSuccessRate());
        
        // Топ игроков
        List<User> topPlayers = userService.getAllUsers().stream()
            .map(userId -> userService.getUser(userId))
            .sorted((a, b) -> Double.compare(b.getBalance(), a.getBalance()))
            .limit(10)
            .toList();
        model.addAttribute("topPlayers", topPlayers);
        
        return "statistics";
    }

    @GetMapping("/features")
    public String features(Model model) {
        return "features";
    }
} 