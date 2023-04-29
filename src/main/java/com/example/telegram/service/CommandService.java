package com.example.telegram.service;

import com.example.telegram.model.constant.MessagePool;
import com.example.telegram.model.dto.BotChange;
import com.example.telegram.model.enums.BotState;
import com.example.telegram.model.enums.ECommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.example.telegram.model.constant.MessagePool.LOGIN_IN_ACCOUNT_WITH_MESSAGE;
import static com.example.telegram.model.constant.MessagePool.MAIN_MENU;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommandService {
    private final AuthService authService;
    private final TaskService taskService;

    public BotChange processComand(Long userId, String message, BotState botState) {
        var command = ECommand.stringCommands.get(message);

        BotChange result = null;
        switch (command) {
            case LOGIN -> {
                if (botState.equals(BotState.BASE)) {
                    result = processAskUsername();
                }
            }
            case SHOW -> {
                if (botState.equals(BotState.IN_ACCOUNT_BASE)) {
                    result = handleShowState(userId);
                }
            }
            case CREATE -> {
                if (botState.equals(BotState.IN_ACCOUNT_BASE)) {
                    result = processAskTaksData();
                }
            }
            case SIGNOUT -> {
                if (botState.equals(BotState.IN_ACCOUNT_BASE)) {
                    result = authService.signOut(userId, false);
                }
            }
            case START -> result = handleStart(userId);
        }
        return result;
    }

    private BotChange handleStart(long messageChatId) {
        var result =  authService.checkForLogin(messageChatId);
        if (result) {
            return new BotChange(
                    BotState.IN_ACCOUNT_BASE,
                    MessagePool.ALREADY_LOGGED + "\n" + MAIN_MENU);
        } else {
            log.info("User is trying to login");
            return new BotChange(
                    BotState.BASE,
                    LOGIN_IN_ACCOUNT_WITH_MESSAGE);
        }
    }

    private BotChange processAskUsername() {
        return new BotChange(BotState.LOGGING_IN_ASKED_LOGIN, MessagePool.INPUT_USERNAME_MESSAGE);
    }

    public BotChange handleShowState(
            Long messageChatId
    ) {
        var accessToken = authService.getUserAccessToken(messageChatId);
        String response = taskService.getTaskList(accessToken);

        if (response == null) {
            var refreshResult = authService.refreshToken(messageChatId);
            if (!refreshResult) {
                return authService.signOut(messageChatId, true);
            }
            accessToken = authService.getUserAccessToken(messageChatId);
            response = taskService.getTaskList(accessToken);
            log.info("refresh success on get task list");
        }

        if (response.equals("[]")) {
            response = MessagePool.EMPTY_LIST_MESSAGE;
        }

        log.info("User has received a task list");

        return new BotChange(BotState.IN_ACCOUNT_BASE, response + "\n" + MAIN_MENU);
    }

    public BotChange processAskTaksData() {
        return new BotChange(BotState.IN_ACCOUNT_ASKED_TASK, MessagePool.INPUT_TASK_DATA_MESSAGE);
    }
}
