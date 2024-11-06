package com.businessbot;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.businessbot.config.BotConfig;
import com.businessbot.database.DatabaseManager;
import com.businessbot.service.AchievementService;
import com.businessbot.service.GameService;
import com.businessbot.service.UserService;

@Component
public class BusinessBot extends TelegramLongPollingBot {
    private final DatabaseManager databaseManager;
    private final UserService userService;
    private final GameService gameService;
    private final AchievementService achievementService;
    private final BotConfig botConfig;

    @Autowired
    public BusinessBot(DatabaseManager databaseManager, BotConfig botConfig) {
        this.databaseManager = databaseManager;
        this.botConfig = botConfig;
        this.userService = new UserService(databaseManager);
        this.gameService = new GameService(databaseManager, userService);
        this.achievementService = new AchievementService(userService, databaseManager);
        
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("start", "Начать игру"));
        commands.add(new BotCommand("profile", "Мой профиль"));
        commands.add(new BotCommand("mine", "Майнить"));
        commands.add(new BotCommand("companies", "Мои компании"));
        commands.add(new BotCommand("loans", "Мои кредиты"));
        
        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            userService.createUserIfNotExists(chatId);

            try {
                switch (messageText) {
                    case "/start" -> handleStart(chatId);
                    case "💰 Баланс", "/profile" -> handleProfile(chatId);
                    case "/mine" -> handleMining(chatId);
                    case "🏢 Компании", "/companies" -> handleCompanies(chatId);
                    case "🎮 Мини-игры" -> handleGames(chatId);
                    case "🏦 Банк" -> handleBank(chatId);
                    case "📊 Статистика" -> handleStatistics(chatId);
                    case "/buy" -> handleBuyCompany(chatId);
                    case "/loans", "📝 Мои кредиты" -> handleLoansList(chatId);
                    default -> handleUnknownCommand(chatId);
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendErrorMessage(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleStart(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👋 Добро пожаловать в Business Bot!");
        
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("💰 Баланс");
        
        KeyboardButton miningButton = new KeyboardButton("⛏ Майнить");
        WebAppInfo webAppInfo = new WebAppInfo();
        webAppInfo.setUrl("https://3000-46-72-189-82.ngrok-free.app/webapp");
        miningButton.setWebApp(webAppInfo);
        row1.add(miningButton);
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🏢 Компании");
        row2.add("🎮 Мини-игры");
        keyboard.add(row2);
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("🏦 Банк");
        row3.add("📊 Статистика");
        keyboard.add(row3);
        
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleProfile(long chatId) {
        var user = userService.getUser(chatId);
        String message = String.format("""
            👤 Ваш профиль:
            
            💰 Баланс: %.2f
            ⚡ Мощность майнинга: %d
            🌟 VIP статус: %d
            """, 
            user.getBalance(), 
            user.getMiningPower(),
            user.getVipStatus()
        );
        sendMessage(chatId, message);
    }

    private void handleMining(long chatId) {
        double reward = gameService.click(chatId);
        sendMessage(chatId, String.format("⛏ Вы намайнили %.2f монет!", reward));
    }

    private void handleCompanies(long chatId) {
        var companies = userService.getUserCompanies(chatId);
        StringBuilder message = new StringBuilder("🏢 Ваши компании:\n\n");
        
        if (companies.isEmpty()) {
            message.append("У вас пока нет компаний. Используйте /buy для покупки.");
        } else {
            for (var company : companies) {
                message.append(String.format(
                    "🏭 %s (Уровень %d)\n💰 Доход: %.2f/час\n\n",
                    company.getType(),
                    company.getLevel(),
                    gameService.calculateHourlyIncome(chatId)
                ));
            }
        }
        sendMessage(chatId, message.toString());
    }

    private void handleUnknownCommand(long chatId) {
        sendMessage(chatId, "❌ Неизвестная команда. Используйте /start для списка команд.");
    }

    private void sendErrorMessage(long chatId) {
        sendMessage(chatId, "❌ Произошла ошибка. Попробуйте позже.");
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleGames(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("🎲 Кости", "game_dice"));
        row1.add(createInlineButton("🎰 Слоты", "game_slots"));
        keyboard.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("🔢 Угадай число", "game_number"));
        keyboard.add(row2);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🎮 Выберите мини-игру:");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleBank(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Депозиты
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("💳 Открыть депозит", "bank_deposit_menu"));
        row1.add(createInlineButton("📊 Мои депозиты", "bank_deposits_list"));
        keyboard.add(row1);
        
        // Кредиты
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("💵 Взять кредит", "bank_loan_menu"));
        row2.add(createInlineButton("📝 Мои кредиты", "bank_loans_list"));
        keyboard.add(row2);
        
        // Инвестиции
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("📈 Инвестировать", "bank_invest_menu"));
        row3.add(createInlineButton("💼 Портфель", "bank_portfolio"));
        keyboard.add(row3);
        
        // Переводы
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createInlineButton("💸 Перевести", "bank_transfer"));
        row4.add(createInlineButton("📋 История", "bank_history"));
        keyboard.add(row4);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("""
            🏦 Банковские услуги:
            
            💳 Депозиты:
            - Стандартный: 5% годовых
            - Премиум: 8% годовых
            - VIP: 12% годовых
            
            💵 Кредиты:
            - Быстрый: 10% годовых
            - Бизнес: 15% годовых
            - Инвестиционный: 20% годовых
            
            📈 Инвестиции:
            - Акции компаний
            - Криптовалюты
            - Индексные фонды
            
            💸 Переводы:
            - Между игроками
            - На депозит
            - На инвестиции
            """);
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleStatistics(long chatId) {
        var user = userService.getUser(chatId);
        String message = String.format("""
            📊 Статистика:
            
            💰 Баланс: %.2f
            🏢 Компаний: %d
            💵 Доход в час: %.2f
            """,
            user.getBalance(),
            userService.getUserCompanies(chatId).size(),
            gameService.calculateHourlyIncome(chatId)
        );
        sendMessage(chatId, message);
    }

    private void handleBuyCompany(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Начальные компании
        keyboard.add(List.of(createInlineButton("🏪 Магазин (5,000 💰)", "buy_company_SHOP")));
        keyboard.add(List.of(createInlineButton("🏭 Фабрика (15,000 💰)", "buy_company_FACTORY")));
        keyboard.add(List.of(createInlineButton("💻 IT Компания (50,000 💰)", "buy_company_TECH")));
        
        // Продвинутые компании
        keyboard.add(List.of(createInlineButton("🏢 Бизнес-центр (100,000 💰)", "buy_company_BUSINESS_CENTER")));
        keyboard.add(List.of(createInlineButton("🏦 Банк (250,000 💰)", "buy_company_BANK")));
        keyboard.add(List.of(createInlineButton("🏗 Строительная компания (500,000 💰)", "buy_company_CONSTRUCTION")));
        
        // Элитные компании
        keyboard.add(List.of(createInlineButton("🛢 Нефтяная компания (1,000,000 💰)", "buy_company_OIL")));
        keyboard.add(List.of(createInlineButton("💎 Криптобиржа (2,500,000 💰)", "buy_company_CRYPTO")));
        keyboard.add(List.of(createInlineButton("🚀 Космическая компания (5,000,000 💰)", "buy_company_SPACE")));
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("""
            🏢 Выберите компанию для покупки:
            
            Начальные компании:
            🏪 Магазин
            💰 Цена: 5,000
            💵 Доход: 100/час
            
            🏭 Фабрика
            💰 Цена: 15,000
            💵 Доход: 300/час
            
            💻 IT Компания
            💰 Цена: 50,000
            💵 Доход: 1,000/час
            
            Продвинутые компании:
            🏢 Бизнес-центр
            💰 Цена: 100,000
            💵 Доход: 2,500/час
            
            🏦 Банк
            💰 Цена: 250,000
            💵 Доход: 7,500/час
            
            🏗 Строительная компания
            💰 Цена: 500,000
            💵 Доход: 15,000/час
            
            Элитные компании:
            🛢 Нефтяная компания
            💰 Цена: 1,000,000
            💵 Доход: 35,000/час
            
            💎 Криптобиржа
            💰 Цена: 2,500,000
            💵 Доход: 100,000/час
            
            🚀 Космическая компания
            💰 Цена: 5,000,000
            💵 Доход: 250,000/час
            """);
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        
        if (data.startsWith("bank_")) {
            switch (data) {
                case "bank_deposit_menu" -> handleDepositMenu(chatId);
                case "bank_deposits_list" -> handleDepositsList(chatId);
                case "bank_loan_menu" -> handleLoanMenu(chatId);
                case "bank_loans_list" -> handleLoansList(chatId);
                case "bank_invest_menu" -> handleInvestMenu(chatId);
                case "bank_portfolio" -> handlePortfolio(chatId);
                case "bank_transfer" -> handleTransfer(chatId);
                case "bank_history" -> handleBankHistory(chatId);
                case "bank_menu" -> handleBank(chatId);
            }
        } else if (data.startsWith("deposit_")) {
            handleDepositAction(chatId, data);
        } else if (data.startsWith("loan_")) {
            if (data.equals("loan_repay")) {
                handleLoanRepayment(chatId);
            } else {
                handleLoanAction(chatId, data);
            }
        } else if (data.startsWith("invest_")) {
            handleInvestAction(chatId, data);
        } else if (data.startsWith("buy_company_")) {
            handleBuyCompanyAction(chatId, data);
        }
    }

    private void handleDepositMenu(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        keyboard.add(List.of(createInlineButton("💰 Стандартный (5%)", "deposit_open_standard")));
        keyboard.add(List.of(createInlineButton("💎 Премиум (8%)", "deposit_open_premium")));
        keyboard.add(List.of(createInlineButton("👑 VIP (12%)", "deposit_open_vip")));
        keyboard.add(List.of(createInlineButton("🔙 Назад", "bank_menu")));
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("""
            💳 Выберите тип депозита:
            
            💰 Стандартный депозит
            - Ставка: 5% годовых
            - Минимальная сумма: 1,000
            - Срок: 1 день
            
            💎 Премиум депозит
            - Ставка: 8% годовых
            - Минимальная сумма: 10,000
            - Срок: 3 дня
            
            👑 VIP депозит
            - Ставка: 12% годовы��
            - Минимальная сумма: 100,000
            - Срок: 7 дней
            """);
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleDepositAction(long chatId, String action) {
        String type = action.replace("deposit_open_", "");
        double minAmount = switch (type) {
            case "standard" -> 1000.0;
            case "premium" -> 10000.0;
            case "vip" -> 100000.0;
            default -> 0.0;
        };
        
        var user = userService.getUser(chatId);
        if (user.getBalance() < minAmount) {
            sendMessage(chatId, "❌ Недостаточно средств для открытия депозита");
            return;
        }
        
        // Создаем депозит
        if (gameService.createDeposit(chatId, type, minAmount)) {
            sendMessage(chatId, String.format(
                "✅ Депозит успешно открыт!\n\nСумма: %.2f\nТип: %s",
                minAmount,
                type
            ));
        } else {
            sendMessage(chatId, "❌ Ошибка при открытии депозита");
        }
    }

    private void handleLoanMenu(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        keyboard.add(List.of(createInlineButton("💵 Быстрый кредит (10%)", "loan_quick")));
        keyboard.add(List.of(createInlineButton("💼 Бизнес кредит (15%)", "loan_business")));
        keyboard.add(List.of(createInlineButton("📈 Инвестиционный (20%)", "loan_investment")));
        keyboard.add(List.of(createInlineButton("🔙 Назад", "bank_menu")));
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("""
            💵 Выберите тип кредита:
            
            💵 Быстрый кредит
            - Ставка: 10% годовых
            - Сумма: до 10,000
            - Срок: 3 дня
            
            💼 Бизнес кредит
            - Ставка: 15% годовых
            - Сумма: до 50,000
            - Срок: 7 дней
            
            📈 Инвестиционный кредит
            - Ставка: 20% годовых
            - Сумма: до 200,000
            - Срок: 14 дней
            """);
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleLoanAction(long chatId, String action) {
        String type = action.replace("loan_", "");
        double maxAmount = switch (type) {
            case "quick" -> 10000.0;
            case "business" -> 50000.0;
            case "investment" -> 200000.0;
            default -> 0.0;
        };
        
        // Проверяем, нет ли уже активного кредита
        if (userService.hasActiveLoan(chatId)) {
            sendMessage(chatId, "❌ У вас уже есть активный кредит. Погасите его перед взятием нового.");
            return;
        }
        
        // Создаем кредит
        if (userService.takeLoan(chatId, type)) {
            sendMessage(chatId, String.format(
                """
                ✅ Кредит успешно оформлен!
                
                💰 Максимальная сумма: %.2f
                📅 Тип: %s
                
                ❗️ Не забудьте погасить кредит вовремя
                Используйте /loans для просмотра кредитов
                """,
                maxAmount,
                getLoanTypeName(type)
            ));
        } else {
            sendMessage(chatId, "❌ Ошибка при оформлении кредита");
        }
    }

    private void handleLoansList(long chatId) {
        var loans = userService.getUserLoans(chatId);
        if (loans.isEmpty()) {
            sendMessage(chatId, "У вас нет активных кредитов");
            return;
        }
        
        StringBuilder message = new StringBuilder("📝 Ваши кредиты:\n\n");
        for (var loan : loans) {
            message.append(String.format(
                """
                💰 Сумма: %.2f
                📈 Ставка: %.1f%%
                📅 Дата взятия: %s
                💵 К оплате: %.2f
                
                """,
                loan.getAmount(),
                loan.getInterestRate() * 100,
                loan.getLoanDate(),
                loan.getTotalDebt()
            ));
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Добавляем кнопку погашения кредита
        keyboard.add(List.of(createInlineButton("💰 Погасить кредит", "loan_repay")));
        
        // Добавляем кнопку возврата в банковское меню
        keyboard.add(List.of(createInlineButton("🔙 Назад в банк", "bank_menu")));
        
        markup.setKeyboard(keyboard);
        
        SendMessage response = new SendMessage();
        response.setChatId(chatId);
        response.setText(message.toString());
        response.setReplyMarkup(markup);
        
        try {
            execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getLoanTypeName(String type) {
        return switch (type) {
            case "quick" -> "Быстрый кредит";
            case "business" -> "Бизнес кредит";
            case "investment" -> "Инвестиционный кредит";
            default -> "Неизвестный тип";
        };
    }

    private void handleInvestMenu(long chatId) {
        // Implement invest menu handling logic
    }

    private void handlePortfolio(long chatId) {
        // Implement portfolio handling logic
    }

    private void handleTransfer(long chatId) {
        // Implement transfer handling logic
    }

    private void handleBankHistory(long chatId) {
        // Implement bank history handling logic
    }

    private void handleBuyCompanyAction(long chatId, String data) {
        String companyType = data.substring("buy_company_".length());
        boolean success = gameService.buyCompany(chatId, companyType);
        
        String responseText;
        if (success) {
            responseText = String.format(
                "✅ Вы успешно купили компанию: %s\n\nИспользуйте /companies чтобы увидеть свои компании.",
                getCompanyName(companyType)
            );
        } else {
            responseText = "❌ Недостаточно средств для покупки компании.";
        }
        
        try {
            execute(SendMessage.builder()
                .chatId(chatId)
                .text(responseText)
                .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getCompanyName(String type) {
        return switch (type) {
            case "SHOP" -> "🏪 Магази��";
            case "FACTORY" -> "🏭 Фабрика";
            case "TECH_COMPANY" -> "💻 IT Компания";
            default -> "Компания";
        };
    }

    private InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private void handleDepositsList(long chatId) {
        var deposits = userService.getUserDeposits(chatId);
        if (deposits.isEmpty()) {
            sendMessage(chatId, "У вас нет активных депозитов");
            return;
        }
        
        StringBuilder message = new StringBuilder("📝 Ваши депозиты:\n\n");
        for (var deposit : deposits) {
            message.append(String.format(
                """
                💰 Сумма: %.2f
                📈 Ставка: %.1f%%
                📅 Дата открытия: %s
                💵 Текущая сумма: %.2f
                
                """,
                deposit.getAmount(),
                deposit.getInterestRate() * 100,
                deposit.getStartDate(),
                deposit.getCurrentAmount()
            ));
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(List.of(createInlineButton("🔙 Назад", "bank_menu")));
        markup.setKeyboard(keyboard);
        
        SendMessage response = new SendMessage();
        response.setChatId(chatId);
        response.setText(message.toString());
        response.setReplyMarkup(markup);
        
        try {
            execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleInvestAction(long chatId, String action) {
        String[] parts = action.split("_");
        if (parts.length < 3) {
            sendMessage(chatId, "❌ Неверный формат действия");
            return;
        }
        
        String operation = parts[1]; // buy или sell
        String assetType = parts[2]; // STOCK, CRYPTO, INDEX
        
        if ("buy".equals(operation)) {
            handleInvestBuy(chatId, assetType);
        } else if ("sell".equals(operation)) {
            handleInvestSell(chatId, assetType);
        }
    }

    private void handleInvestBuy(long chatId, String assetType) {
        // Здесь будет логика покупки инвестиций
        sendMessage(chatId, "🔄 Функция покупки инвестиций в разработке");
    }

    private void handleInvestSell(long chatId, String assetType) {
        // Здесь будет логика продажи инвестиций
        sendMessage(chatId, "🔄 Функция продажи инвестиций в разработке");
    }

    private void handleLoanRepayment(long chatId) {
        var loans = userService.getUserLoans(chatId);
        if (loans.isEmpty()) {
            sendMessage(chatId, "У вас нет активных кредитов");
            return;
        }
        
        var loan = loans.get(0); // Берем первый активный кредит
        var user = userService.getUser(chatId);
        double totalDebt = loan.getAmount() * (1 + loan.getInterestRate()); // Сумма + проценты
        
        if (user.getBalance() < totalDebt) {
            sendMessage(chatId, String.format(
                "❌ Недостаточно средств для погашения кредита\nНеобходимо: %.2f\nУ вас: %.2f",
                totalDebt,
                user.getBalance()
            ));
            return;
        }
        
        // Погашаем кредит
        if (userService.repayLoan(chatId, loan.getId(), totalDebt)) {
            sendMessage(chatId, String.format(
                """
                ✅ Кредит успешно погашен!
                
                💰 Списано: %.2f
                💵 Основной долг: %.2f
                📈 Проценты: %.2f
                """,
                totalDebt,
                loan.getAmount(),
                totalDebt - loan.getAmount()
            ));
        } else {
            sendMessage(chatId, "❌ Произошла ошибка при погашении кредита");
        }
    }
} 