package com.businessbot.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.businessbot.service.AchievementService;
import com.businessbot.service.GameService;
import com.businessbot.service.UserService;

@Controller
@RequestMapping("/webapp")
public class TelegramWebAppController {

    private final UserService userService;
    private final GameService gameService;
    private final AchievementService achievementService;

    @Autowired
    public TelegramWebAppController(UserService userService, GameService gameService, AchievementService achievementService) {
        this.userService = userService;
        this.gameService = gameService;
        this.achievementService = achievementService;
    }

    @GetMapping
    public String webApp() {
        return "webapp";
    }

    @PostMapping("/api/mine")
    @ResponseBody
    public Map<String, Object> mine(@RequestParam("userId") long userId) {
        try {
            userService.createUserIfNotExists(userId);
            double reward = gameService.click(userId);
            var user = userService.getUser(userId);
            return Map.of(
                "success", true,
                "reward", reward,
                "newBalance", user.getBalance()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", "Произошла ошибка при майнинге"
            );
        }
    }

    @GetMapping("/api/user-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserData(@RequestParam("userId") long userId) {
        try {
            userService.createUserIfNotExists(userId);
            var user = userService.getUser(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            var miningInfo = gameService.getMiningUpgradeInfo(userId);
            
            response.put("user", user);
            response.put("miningPower", user.getMiningPower());
            response.put("balance", user.getBalance());
            response.put("clickReward", miningInfo.get("currentReward"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/upgrade/{type}")
    @ResponseBody
    public Map<String, Object> upgrade(
        @PathVariable String type,
        @RequestParam("userId") long userId
    ) {
        try {
            boolean success = switch (type) {
                case "power" -> gameService.upgradeMiner(userId);
                case "energy" -> gameService.upgradeEnergy(userId);
                case "efficiency" -> gameService.upgradeEfficiency(userId);
                default -> false;
            };

            if (success) {
                var user = userService.getUser(userId);
                var upgradeInfo = gameService.getMiningUpgradeInfo(userId);
                var activeBoosts = gameService.getActiveBoosts(userId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("newPower", user.getMiningPower());
                response.put("newBalance", user.getBalance());
                response.put("activeBoosts", activeBoosts);
                response.put("message", "Улучшение выполнено!");
                
                return response;
            } else {
                return Map.of(
                    "success", false,
                    "message", "Недостаточно средств"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", "Ошибка улучшения"
            );
        }
    }

    @PostMapping("/api/auto-mining")
    @ResponseBody
    public Map<String, Object> toggleAutoMining(
        @RequestParam("userId") long userId,
        @RequestParam("enabled") boolean enabled
    ) {
        try {
            boolean success = gameService.toggleAutoMining(userId, enabled);
            return Map.of(
                "success", success,
                "message", success ? 
                    "Авто-майнинг " + (enabled ? "включен" : "выключен") : 
                    "Ошибка изменения режима"
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", "Ошибка"
            );
        }
    }

    @PostMapping("/api/auto-mine")
    @ResponseBody
    public Map<String, Object> autoMine(@RequestParam("userId") long userId) {
        try {
            double reward = gameService.autoMine(userId);
            var user = userService.getUser(userId);
            
            return Map.of(
                "success", true,
                "reward", reward,
                "newBalance", user.getBalance(),
                "message", String.format("Автоматически добыто %.2f монет!", reward)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", "Ошибка авто-майнинга"
            );
        }
    }

    @GetMapping("/api/mining-info")
    @ResponseBody
    public Map<String, Object> getMiningInfo(@RequestParam("userId") long userId) {
        try {
            return gameService.getMiningUpgradeInfo(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "error", "Не удалось получить информацию"
            );
        }
    }
} 