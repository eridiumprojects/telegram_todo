package com.example.telegram.model.constant;

public class MessagePool {
    public static final String INFO_START_MESSAGE = "Начать работу с ботом";
    public static final String LOGIN_IN_ACCOUNT_WITH_MESSAGE = "Ты можешь войти в аккаунт с помощью команды /login";
    public static final String INPUT_USERNAME_MESSAGE = "Введи имя пользователя:";
    public static final String INPUT_PASSWORD_MESSAGE = "Введи пароль:";
    public static final String INVALID_COMMAND_MESSAGE = "Неверная команда!";
    public static final String INVALID_DATA_MESSAGE = """
            Неверные имя пользователя или пароль.
                        
            Чтобы я мог тебе помогать мне нужны правильные данные от твоего аккаунта.
            Если ты не уверен в том, что я официальный бот приложения, можешь написать моим разработчикам:
                        
            @allokanic
            @eridiium
            """;
    public static final String SUCCESSFULLY_LOGGED_MESSAGE = """
            Данные верны! Добро пожаловать, будь как дома\uD83D\uDE0A
            """;
    public static final String MAIN_MENU = """
            Доступные команды:
            /create - создать новую задачу
            /show - список твоих задач
            
            /signout - выход из аккаунта
            """;
    public static final String INPUT_TASK_DATA_MESSAGE = "Введи название новой задачи:";
    public static final String TASK_CREATED_MESSAGE = """
            Задача успешно создана!
            Дел стало больше, поглядывай на часы✍
            """;
    public static final String EMPTY_LIST_MESSAGE = """
            Здесь пока нет задач. Кажется, ты всё выполнил😏
            """;
    public static final String SIGNOUT_MESSAGE = """
            Успешный выход из аккаунта!
                        
            Надеюсь ты скоро вернешься\uD83D\uDE43
            """;
    public static final String ALREADY_LOGGED = """
            С возвращением! 🤗
            """;
    public static final String SESSION_EXPIRED = """
            Кажется тебя не было очень давно и я тебя забыл😰
            """;
}
