package com.example.telegram.service;

import com.example.telegram.model.constant.MessagePool;
import com.example.telegram.model.dto.BotChange;
import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.enums.BotState;
import com.example.telegram.model.enums.ECommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.telegram.model.constant.DefaultMessages.IN_ACCOUNT;
import static com.example.telegram.model.constant.MessagePool.*;

@Service
@Getter
@Setter
@Log4j2
public class BotService {
    private AuthService authService;
    private TaskService taskService;
    private RMap<Long, String> userStates;
    private RMap<Long, String> userNames;

    public BotService(
            RedissonClient redissonClient,
            AuthService authService,
            TaskService taskService,
            @Value("${storage.user-name}") String userNameStorage,
            @Value("${storage.user-state}") String userStateStorage
            ) {
        this.authService = authService;
        this.taskService = taskService;

        this.userStates = redissonClient.getMap(userStateStorage);
        this.userNames = redissonClient.getMap(userNameStorage);
    }

    public String process(Long userId, String message) {
        var botState = BotState.valueOf(userStates.getOrDefault(
                userId,
                BotState.BASE.name()));

        BotChange result = null;

        if (message.equals(ECommand.START.getCommand())) {
            result = handleStart(userId);
        } else if (message.equals(ECommand.LOGIN.getCommand()) && botState.equals(BotState.BASE)) {
            result = processAskUsername();
        } else if (botState.equals(BotState.LOGGING_IN_ASKED_LOGIN) && !ECommand.commands.contains(message)) {
            result = processAskPassword(userId, message);
        } else if (botState.equals(BotState.LOGGING_IN_ASKED_PASS) && !ECommand.commands.contains(message)) {
            result = loginUser(userId, message);
        } else if (botState.equals(BotState.IN_ACCOUNT_BASE) && ECommand.inAccountCommands.contains(message)) {
            if (message.equals(ECommand.SHOW.getCommand())) {
                result = handleShowState(userId);
            } else if (message.equals(ECommand.SIGNOUT.getCommand())) {
                result = authService.processSignOut(userId);
            } else if (message.equals(ECommand.CREATE.getCommand())) {
                result = processAskTaksData();
            }
        } else if (botState.equals(BotState.IN_ACCOUNT_ASKED_TASK)) {
            result = handleCreateState(userId, message);
        }
        if (result == null) {
            return getDefaultMessage(botState);
        }
        userStates.put(userId, result.getBotState().name());
        return result.getMessage();
    }

    public BotChange handleShowState(
            Long messageChatId
    ) {
        var accessToken = authService.getUserAccessToken(messageChatId);
        String response = taskService.getTaskList(accessToken);

        if (response == null) {
            var refreshResult = authService.refreshToken(messageChatId);
            if (!refreshResult) {
                return authService.processSignOut(messageChatId);
            }
            accessToken = authService.getUserAccessToken(messageChatId);
            response = taskService.getTaskList(accessToken);
        }

        if (response.equals("[]")) {
            response = MessagePool.EMPTY_LIST_MESSAGE;
        }

        log.info("User has received a task list");

        return new BotChange(BotState.IN_ACCOUNT_BASE, response + "\n\n" + MAIN_MENU);
    }

    public BotChange handleCreateState(long messageChatId, String messageText) {
        var accessToken = authService.getUserAccessToken(messageChatId);
        var result = taskService.createTask(accessToken, messageText);

        if (!result) {
            var refreshResponse = authService.refreshToken(messageChatId);

            if (!refreshResponse) {
                return authService.processSignOut(messageChatId);
            }

            accessToken = authService.getUserAccessToken(messageChatId);
            taskService.createTask(accessToken, messageText);
        }

        log.info("User has been created a task");

        return new BotChange(
                BotState.IN_ACCOUNT_BASE,
                MessagePool.TASK_CREATED_MESSAGE + "\n\n" + MAIN_MENU);
    }

    //AUTH INTEGRATION
    public BotChange handleStart(long messageChatId) {
        return authService.checkForLogin(messageChatId);
    }

    public BotChange loginUser(long messageChatId, String messageText) {
        var loginResponse = authService.loginUser(new LoginRequest(
                messageChatId,
                userNames.get(messageChatId),
                messageText,
                UUID.randomUUID()));

        if (loginResponse) {
            log.info("User logged into the account");
            log.info("Access map saved the user id and token successfully");
            log.info("Refresh map saved the user id and token successfully");
            return new BotChange(
                    BotState.IN_ACCOUNT_BASE,
                    MessagePool.SUCCESSFULLY_LOGGED_MESSAGE + "\n\n" + MAIN_MENU);
        } else {
            log.warn("User has bad credentials");
            return new BotChange(
                    BotState.BASE,
                    MessagePool.INVALID_DATA_MESSAGE + "\n\n" + LOGIN_IN_ACCOUNT_WITH_MESSAGE);
        }
    }

    //UTIL
    public BotChange processAskTaksData() {
        return new BotChange(BotState.IN_ACCOUNT_ASKED_TASK, MessagePool.INPUT_TASK_DATA_MESSAGE);
    }
    public BotChange processAskUsername() {
        return new BotChange(BotState.LOGGING_IN_ASKED_LOGIN, MessagePool.INPUT_USERNAME_MESSAGE);
    }

    public BotChange processAskPassword(Long userId, String messageText) {
        userNames.put(userId, messageText);
        return new BotChange(BotState.LOGGING_IN_ASKED_PASS, MessagePool.INPUT_PASSWORD_MESSAGE);
    }

    public String getDefaultMessage(BotState botState) {
        return switch (botState) {
            case BASE -> LOGIN_IN_ACCOUNT_WITH_MESSAGE;
            case IN_ACCOUNT_BASE -> IN_ACCOUNT + "\n\n" + MAIN_MENU;
            case IN_ACCOUNT_ASKED_TASK, LOGGING_IN_ASKED_LOGIN, LOGGING_IN_ASKED_PASS -> INVALID_COMMAND_MESSAGE;
        };
    }
}
