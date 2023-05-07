package com.example.telegram.service;

import com.example.telegram.model.constant.MessagePool;
import com.example.telegram.model.dto.BotChange;
import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.enums.BotState;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.telegram.model.constant.MessagePool.LOGIN_IN_ACCOUNT_WITH_MESSAGE;
import static com.example.telegram.model.constant.MessagePool.MAIN_MENU;

@Service
@Slf4j
public class MessagingService {
    private final AuthService authService;
    private final TaskService taskService;
    private final RMap<Long, String> userNames;

    public MessagingService(AuthService authService,
                            TaskService taskService,
                            @Value("${storage.user-name}") String userNameStorage,
                            RedissonClient redissonClient
    ) {
        this.authService = authService;
        this.taskService = taskService;
        this.userNames = redissonClient.getMap(userNameStorage);
    }

    public BotChange handleMessage(Long userId, String message, BotState botState) {
        return switch (botState) {
            case LOGGING_IN_ASKED_LOGIN -> processAskPassword(userId, message);
            case LOGGING_IN_ASKED_PASS -> loginUser(userId, message);
            case IN_ACCOUNT_ASKED_TASK -> handleCreateState(userId, message);
            default -> null;
        };
    }

    private BotChange handleCreateState(long messageChatId, String messageText) {
        var accessToken = authService.getUserAccessToken(messageChatId);
        var result = taskService.createTask(accessToken, messageText);

        if (!result) {
            var refreshResponse = authService.refreshToken(messageChatId);

            if (!refreshResponse) {
                return authService.signOut(messageChatId, true);
            }
            log.info("refresh success");

            accessToken = authService.getUserAccessToken(messageChatId);
            taskService.createTask(accessToken, messageText);
        }

        log.info("User created a task");

        return new BotChange(
                BotState.IN_ACCOUNT_BASE,
                MessagePool.TASK_CREATED_MESSAGE + "\n" + MAIN_MENU);
    }

    public BotChange loginUser(long messageChatId, String messageText) {
        var loginResponse = authService.loginUser(messageChatId, new LoginRequest(
                userNames.get(messageChatId),
                messageText,
                UUID.randomUUID()));

        if (loginResponse) {
            log.info("User logged into the account");
            return new BotChange(
                    BotState.IN_ACCOUNT_BASE,
                    MessagePool.SUCCESSFULLY_LOGGED_MESSAGE + "\n" + MAIN_MENU);
        } else {
            log.warn("User has bad credentials");
            return new BotChange(
                    BotState.BASE,
                    MessagePool.INVALID_DATA_MESSAGE + "\n" + LOGIN_IN_ACCOUNT_WITH_MESSAGE);
        }
    }

    public BotChange processAskPassword(Long userId, String messageText) {
        userNames.put(userId, messageText);
        return new BotChange(BotState.LOGGING_IN_ASKED_PASS, MessagePool.INPUT_PASSWORD_MESSAGE);
    }
}
