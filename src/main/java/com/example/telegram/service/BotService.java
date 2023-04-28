package com.example.telegram.service;

import com.example.telegram.model.dto.BotChange;
import com.example.telegram.model.enums.BotState;
import com.example.telegram.model.enums.ECommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.example.telegram.model.constant.DefaultMessages.IN_ACCOUNT;
import static com.example.telegram.model.constant.MessagePool.*;

@Service
@Getter
@Setter
@Log4j2
public class BotService {
    private final AuthService authService;
    private final TaskService taskService;
    private final RMap<Long, String> userStates;
    private final CommandService commandService;
    private final MessagingService messagingService;

    public BotService(
            RedissonClient redissonClient,
            AuthService authService,
            TaskService taskService,
            @Value("${storage.user-state}") String userStateStorage,
            CommandService commandService,
            MessagingService messagingService
    ) {
        this.authService = authService;
        this.taskService = taskService;
        this.commandService = commandService;
        this.messagingService = messagingService;

        this.userStates = redissonClient.getMap(userStateStorage);
    }

    public String process(Long userId, String message) {
        var botState = BotState.valueOf(userStates.getOrDefault(
                userId,
                BotState.BASE.name()));

        BotChange result;
        if (ECommand.commands.contains(message)) {
            result = commandService.processComand(userId, message, botState);
        } else {
            result = messagingService.handleMessage(userId, message, botState);
        }

        if (result == null) {
            return getDefaultMessage(botState);
        }
        userStates.put(userId, result.getBotState().name());
        return result.getMessage();
    }

    public String getDefaultMessage(BotState botState) {
        return switch (botState) {
            case BASE -> LOGIN_IN_ACCOUNT_WITH_MESSAGE;
            case IN_ACCOUNT_BASE -> IN_ACCOUNT + "\n\n" + MAIN_MENU;
            case IN_ACCOUNT_ASKED_TASK, LOGGING_IN_ASKED_LOGIN, LOGGING_IN_ASKED_PASS -> INVALID_COMMAND_MESSAGE;
        };
    }
}
