package com.example.telegram.model.constant;

public class MessagePool {
    public static final String INFO_START_MESSAGE = "Начать работу с ботом";
    public static final String LOGIN_IN_ACCOUNT_WITH_MESSAGE = "Войдите с помощью команды /login";
    public static final String INPUT_USERNAME_MESSAGE = "Введите имя пользователя:";
    public static final String INPUT_PASSWORD_MESSAGE = "Введите пароль:";
    public static final String INVALID_COMMAND_MESSAGE = "Неверная команда!";
    public static final String INVALID_DATA_MESSAGE = "Неверные имя пользователя или пароль. Повторите попытку!";
    public static final String SUCCESSFULLY_LOGGED_MESSAGE = "Вы успешно вошли в аккаунт!";
    public static final String MAIN_MENU = """
            Приступим к планированию!
            Ты можешь создать новую задачу с помощью команды /create
            Если тебя интересуют уже созданные задачи воспользуйся командой /show
            
            Для выхода из аккаунта можешь использовать /signout
            """;
    public static final String INPUT_TASK_DATA_MESSAGE = "Введите название новой таски:";
    public static final String TASK_CREATED_MESSAGE = "Задание успешно создано!";
    public static final String EMPTY_LIST_MESSAGE = "Ваш список заданий пуст";
    public static final String SIGNOUT_MESSAGE = "Вы успешно вышли из аккаунта!";
    public static final String ALREADY_LOGGED = "С возвращением! Тебя давно не было слышно, пора вернуться к планированию";
    public static final String SESSION_EXPIRED = "Сессия с текущим пользователем истекла, авторизуйтесь заново!";
}
