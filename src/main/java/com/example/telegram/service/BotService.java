package com.example.telegram.service;

import com.example.telegram.model.constant.MessagePool;
import com.example.telegram.model.dto.BotChange;
import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.response.RefreshResponse;
import com.example.telegram.model.enums.BotState;
import com.example.telegram.model.enums.ECommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.telegram.model.constant.DefaultMessages.IN_ACCOUNT;
import static com.example.telegram.model.constant.MessagePool.*;

@Service
@Getter
@Setter
@Log4j2
public class BotService {
    private AuthService authService;
    private TaskService taskService;
    private final RedissonClient redissonClient;
    public RMapCache<Long, String> accessTokensMap;
    public RMapCache<Long, String> refreshTokensMap;
    private RMap<Long, String> userStates;
    private RMap<Long, String> userNames;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public BotService(
            RedissonClient redissonClient,
            AuthService authService,
            TaskService taskService,
            @Value("${storage.user-name}") String userNameStorage,
            @Value("${storage.user-state}") String userStateStorage,
            @Value("${storage.access-token}") String accessTokenStorage,
            @Value("${storage.refresh-token}") String refreshTokenStorage,
            @Value("${security.ttl.access}") Duration accessTokenTtl,
            @Value("${security.ttl.refresh}") Duration refreshTokenTtl
            ) {
        this.redissonClient = redissonClient;
        this.authService = authService;
        this.taskService = taskService;

        this.userStates = redissonClient.getMap(userStateStorage);
        this.userNames = redissonClient.getMap(userNameStorage);
        this.accessTokensMap = redissonClient.getMapCache(accessTokenStorage);
        this.refreshTokensMap = redissonClient.getMapCache(refreshTokenStorage);

        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String process(Long userId, String message) {
        var botState = BotState.valueOf(userStates.getOrDefault(
                userId,
                BotState.BASE.name()));

        //start
        if (message.equals(ECommand.START.getCommand())) {
            //start

            var result = handleStartState(userId);
            userStates.put(userId, result.getBotState().name());
            return result.getMessage();

        } else if (message.equals(ECommand.LOGIN.getCommand()) && botState.equals(BotState.BASE)) {
            //login ask username

            var result = processAskUsername();
            userStates.put(userId, result.getBotState().name());
            return result.getMessage();

        } else if (botState.equals(BotState.LOGGING_IN_ASKED_LOGIN) && !ECommand.commands.contains(message)) {
            //login ask password

            var result = processAskPassword(userId, message);
            userStates.put(userId, result.getBotState().name());
            return result.getMessage();

        } else if (botState.equals(BotState.LOGGING_IN_ASKED_PASS) && !ECommand.commands.contains(message)) {
            //login authorize user

            var result = processLoginProcessing(userId, message);
            userStates.put(userId, result.getBotState().name());
            return result.getMessage();

        } else if (botState.equals(BotState.IN_ACCOUNT_BASE) && ECommand.inAccountCommands.contains(message)) {
            //basic IN_ACCOUNT commands
            if (message.equals(ECommand.SHOW.getCommand())) {

                //list
                var result = handleShowState(userId);
                userStates.put(userId, result.getBotState().name());
                return result.getMessage();

            } else if (message.equals(ECommand.SIGNOUT.getCommand())) {

                //sign out
                var result = handleSignoutState(userId);
                userStates.put(userId, result.getBotState().name());
                return result.getMessage();

            } else if (message.equals(ECommand.CREATE.getCommand())) {

                //asking data for create task
                var result = processAskTaksData();
                userStates.put(userId, result.getBotState().name());
                return result.getMessage();

            }
        } else if (botState.equals(BotState.IN_ACCOUNT_ASKED_TASK)) {

            //create
            var result = handleCreateState(userId, message);
            userStates.put(userId, result.getBotState().name());
            return result.getMessage();

        }
        return getDefaultMessage(botState);
    }

    public String getDefaultMessage(BotState botState) {
        return switch (botState) {
            case BASE -> LOGIN_IN_ACCOUNT_WITH_MESSAGE;
            case IN_ACCOUNT_BASE -> IN_ACCOUNT + "\n\n" + MAIN_MENU;
            case IN_ACCOUNT_ASKED_TASK, LOGGING_IN_ASKED_LOGIN, LOGGING_IN_ASKED_PASS -> INVALID_COMMAND_MESSAGE;
        };
    }

    public BotChange handleStartState(long messageChatId) {
        if (accessTokensMap.containsKey(messageChatId)) {
            log.info("Valid access token exists");
            return new BotChange(
                    BotState.IN_ACCOUNT_BASE,
                    MessagePool.ALREADY_LOGGED + "\n\n" + MAIN_MENU);
        } else if (refreshTokensMap.containsKey(messageChatId)) {
            log.info("Valid refresh token exists");
            var result = processingRefreshTokens(refreshTokensMap.get(messageChatId));
            if (result != null) {
                accessTokensMap.put(
                        messageChatId,
                        result.getAccessToken(),
                        accessTokenTtl.toMinutes(),
                        TimeUnit.MINUTES);
                refreshTokensMap.put(
                        messageChatId,
                        result.getRefreshToken(),
                        refreshTokenTtl.toMinutes(),
                        TimeUnit.MINUTES);
                return new BotChange(
                        BotState.IN_ACCOUNT_BASE,
                        MessagePool.ALREADY_LOGGED + "\n\n" + MAIN_MENU);
            }
        }

        log.info("User is trying to login");
        return new BotChange(
                BotState.BASE,
                LOGIN_IN_ACCOUNT_WITH_MESSAGE);
    }

    public BotChange processAskUsername() {
        return new BotChange(BotState.LOGGING_IN_ASKED_LOGIN, MessagePool.INPUT_USERNAME_MESSAGE);
    }

    public BotChange processAskPassword(Long userId, String messageText) {
        userNames.put(userId, messageText);
        return new BotChange(BotState.LOGGING_IN_ASKED_PASS, MessagePool.INPUT_PASSWORD_MESSAGE);
    }

    public BotChange processLoginProcessing(long messageChatId, String messageText) {
        var jwtResponse = authService.sendRequestToAuthService(new LoginRequest(
                userNames.get(messageChatId),
                messageText,
                UUID.randomUUID()));

        if (jwtResponse == null) {
            log.warn("User has bad credentials");
            return new BotChange(
                    BotState.BASE,
                    MessagePool.INVALID_DATA_MESSAGE + "\n\n" + LOGIN_IN_ACCOUNT_WITH_MESSAGE);
        }

        accessTokensMap.put(
                messageChatId,
                jwtResponse.getAccessToken(),
                accessTokenTtl.toMinutes(),
                TimeUnit.MINUTES);
        refreshTokensMap.put(
                messageChatId,
                jwtResponse.getRefreshToken(),
                refreshTokenTtl.toMinutes(),
                TimeUnit.MINUTES);

        log.info("User logged into the account");
        log.info("Access map saved the user id and token successfully");
        log.info("Refresh map saved the user id and token successfully");

        return new BotChange(
                BotState.IN_ACCOUNT_BASE,
                MessagePool.SUCCESSFULLY_LOGGED_MESSAGE + "\n\n" + MAIN_MENU);
    }

    private BotChange handleSignoutState(long messageChatId) {
        accessTokensMap.remove(messageChatId);
        refreshTokensMap.remove(messageChatId);

        log.info("User logged out of the account");
        log.info("Access map deleted the user id and token successfully");
        log.info("Refresh map deleted the user id and token successfully");

        return new BotChange(
                BotState.BASE,
                MessagePool.SIGNOUT_MESSAGE + "\n\n" + LOGIN_IN_ACCOUNT_WITH_MESSAGE);
    }

    public BotChange handleShowState(
            Long messageChatId
    ) {
        var accessToken = accessTokensMap.getOrDefault(messageChatId, null);

        String response = taskService.getTaskList(accessToken);

        if (response == null) {
            var refreshToken = refreshTokensMap.getOrDefault(messageChatId, null);
            RefreshResponse refreshResponse = processingRefreshTokens(refreshToken);
            if (refreshResponse == null) {
                return processingExpiredSession(messageChatId);
            }
            accessToken = refreshResponse.getAccessToken();
            accessTokensMap.put(
                    messageChatId,
                    accessToken,
                    accessTokenTtl.toMinutes(),
                    TimeUnit.MINUTES);
            refreshTokensMap.put(
                    messageChatId,
                    refreshResponse.getRefreshToken(),
                    refreshTokenTtl.toMinutes(),
                    TimeUnit.MINUTES);

            response = taskService.getTaskList(accessToken);
        }

        if (response.equals("[]")) {
            response = MessagePool.EMPTY_LIST_MESSAGE;
        }

        log.info("User has received a task list");

        return new BotChange(BotState.IN_ACCOUNT_BASE, response + "\n\n" + MAIN_MENU);
    }

    public BotChange processAskTaksData() {
        return new BotChange(BotState.IN_ACCOUNT_ASKED_TASK, MessagePool.INPUT_TASK_DATA_MESSAGE);
    }

    public BotChange handleCreateState(long messageChatId, String messageText) {
        String token = accessTokensMap.getOrDefault(messageChatId, null);
        var result = taskService.createTask(token, messageText);

        if (!result) {
            String refreshToken = refreshTokensMap.getOrDefault(messageChatId, null);
            RefreshResponse refreshResponse = processingRefreshTokens(refreshToken);

            if (refreshResponse == null) {
                return processingExpiredSession(messageChatId);
            }

            token = refreshResponse.getAccessToken();
            accessTokensMap.put(
                    messageChatId,
                    token,accessTokenTtl.toMinutes(),
                    TimeUnit.MINUTES);
            refreshTokensMap.put(
                    messageChatId,
                    refreshResponse.getRefreshToken(),
                    refreshTokenTtl.toMinutes(),
                    TimeUnit.MINUTES);

            taskService.createTask(token, messageText);
        }

        log.info("User has been created a task");

        return new BotChange(
                BotState.IN_ACCOUNT_BASE,
                MessagePool.TASK_CREATED_MESSAGE + "\n\n" + MAIN_MENU);
    }

    public BotChange processingExpiredSession(Long messageChatId) {
        accessTokensMap.remove(messageChatId);
        refreshTokensMap.remove(messageChatId);

        log.warn("User session has expired");
        log.info("Access map deleted the user id and token successfully");
        log.info("Refresh map deleted the user id and token successfully");

        return new BotChange(
                BotState.BASE,
                MessagePool.SESSION_EXPIRED + "\n\n" + LOGIN_IN_ACCOUNT_WITH_MESSAGE);
    }


    public RefreshResponse processingRefreshTokens(String refreshToken) {
        return authService.refreshToken(refreshToken);
    }
}
