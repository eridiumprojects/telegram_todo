package com.example.telegram.service;

import com.example.telegram.model.constant.MessagePull;
import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.request.TaskRequest;
import com.example.telegram.model.dto.response.JwtResponse;
import com.example.telegram.model.dto.response.RefreshResponse;
import com.example.telegram.model.enums.BotState;
import com.example.telegram.model.enums.ECommand;
import com.example.telegram.model.enums.LoginState;
import com.example.telegram.model.exception.AuthException;
import com.example.telegram.model.exception.ObjectNotFoundException;
import com.example.telegram.util.RequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpStatus;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Getter
@Setter
@Log4j2
public class BotService {
    private BotState botState;
    private LoginState currentState;
    private AuthService authService;
    private TaskService taskService;
    private TelegramLongPollingBot bot;
    public LoginRequest loginUser;
    private final RedissonClient redissonClient;
    private JwtResponse jwtResponse;
    public RMapCache<Long, String> map;
    public RMapCache<Long, String> refresh;
    public final String ACCESS_CACHE = "accessCache";
    public final String REFRESH_CACHE = "refreshCache";


    public BotService(TelegramLongPollingBot bot, RedissonClient redissonClient) {
        this.bot = bot;
        this.redissonClient = redissonClient;
        currentState = LoginState.ASK_USERNAME;
        this.loginUser = new LoginRequest();
        this.authService = new AuthService(new RequestBuilder(new ObjectMapper()));
        this.taskService = new TaskService(new RequestBuilder(new ObjectMapper()));
    }

    public void handleMenuState(long messageChatId) {
        map = redissonClient.getMapCache(ACCESS_CACHE);
        refresh = redissonClient.getMapCache(REFRESH_CACHE);
        if (map.containsKey(messageChatId)) {
            sendMessage(messageChatId, MessagePull.ALREADY_LOGGED);
            handleInAccountState(messageChatId, ECommand.RUN.getCommand());
            botState = BotState.NEXT;
            log.info("User has been already login in account");
            return;
        }
        sendMessage(messageChatId, MessagePull.LOGIN_IN_ACCOUNT_WITH_MESSAGE + ECommand.LOGIN.getCommand());
        log.info("User is trying to login");
        botState = BotState.LOGIN;
    }

    public void handleLoginState(long messageChatId, String messageText) {
        switch (currentState) {
            case ASK_USERNAME -> processAskUsername(messageChatId, messageText);
            case ASK_PASSWORD -> processAskPassword(messageChatId, messageText);
            case LOGIN_PROCESSING -> processLoginProcessing(messageChatId, messageText);
        }
    }

    public void processAskUsername(long messageChatId, String messageText) {
        if (messageText.equals(ECommand.LOGIN.getCommand())) {
            sendMessage(messageChatId, MessagePull.INPUT_USERNAME_MESSAGE);
            currentState = LoginState.ASK_PASSWORD;
            return;
        }
        sendMessage(messageChatId, MessagePull.INVALID_COMMAND_MESSAGE);
    }

    public void processAskPassword(long messageChatId, String messageText) {
        loginUser.setUsername(messageText);
        sendMessage(messageChatId, MessagePull.INPUT_PASSWORD_MESSAGE);
        currentState = LoginState.LOGIN_PROCESSING;
    }

    public void processLoginProcessing(long messageChatId, String messageText) {
        map = redissonClient.getMapCache(ACCESS_CACHE);
        refresh = redissonClient.getMapCache(REFRESH_CACHE);
        loginUser.setPassword(messageText);
        jwtResponse = authService.sendSignInRequest(loginUser);
        log.info(authService.getStatusCode());
        if (authService.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            sendMessage(messageChatId, MessagePull.INVALID_DATA_MESSAGE);
            botState = BotState.MENU;
            currentState = LoginState.ASK_USERNAME;
            log.warn("User has bad credentials");
        }
        if (authService.getStatusCode() == HttpStatus.SC_OK) {
            map.put(messageChatId, jwtResponse.getAccessToken());
            refresh.put(messageChatId, jwtResponse.getRefreshToken());
            sendMessage(messageChatId, MessagePull.SUCCESSFULLY_LOGGED_MESSAGE);
            sendMessage(messageChatId, MessagePull.NEXT_ACTS_MESSAGE +
                    ECommand.RUN.getCommand());
            botState = BotState.IN_ACCOUNT;
            currentState = LoginState.ASK_USERNAME;
            log.info("User logged into the account");
            log.info("Access map saved the user id and token successfully");
            log.info("Refresh map saved the user id and token successfully");
        }
    }

    public void handleInAccountState(long messageChatId, String messageText) {
        if (messageText.equals(ECommand.RUN.getCommand()) || messageText.equals(ECommand.RETURN.getCommand())) {
            sendMessage(messageChatId,
                    MessagePull.IN_ACCOUNT_FIRST_MESSAGE +
                            ECommand.CREATE.getCommand() +
                            MessagePull.IN_ACCOUNT_SECOND_MESSAGE +
                            ECommand.SHOW.getCommand() +
                            MessagePull.IN_ACCOUNT_THIRD_MESSAGE +
                            ECommand.SIGNOUT.getCommand());
            botState = BotState.NEXT;
            return;
        }
        sendMessage(messageChatId, MessagePull.INVALID_COMMAND_MESSAGE);
    }

    public void handleNextState(long messageChatId, String messageText) {
        map = redissonClient.getMapCache(ACCESS_CACHE);
        refresh = redissonClient.getMapCache(REFRESH_CACHE);

        String token = (map.containsKey(messageChatId)) ? map.get(messageChatId) : jwtResponse.getAccessToken();
        String refreshToken = (refresh.containsKey(messageChatId)) ? refresh.get(messageChatId) : jwtResponse.getRefreshToken();

        if (messageText.equals(ECommand.CREATE.getCommand())) {
            botState = BotState.CREATE;
            sendMessage(messageChatId, MessagePull.INPUT_TASK_DATA_MESSAGE);
        } else if (messageText.equals(ECommand.SHOW.getCommand())) {

            try {
                botState = BotState.SHOW;
                String tasks = taskService.sendShowTasksRequest(token);
                if (taskService.getStatusCode() != HttpStatus.SC_OK) {
                    RefreshResponse refreshResponse = processingRefreshTokens(refreshToken);
                    if (checkRefreshState(authService.getStatusCode())) {
                        sendMessage(messageChatId, MessagePull.SESSION_EXPIRED);
                        botState = BotState.MENU;
                        currentState = LoginState.ASK_USERNAME;
                        map.remove(messageChatId);
                        refresh.remove(messageChatId);
                        handleMenuState(messageChatId);
                        log.info("User session has expired");
                        log.info("Access map deleted the user id and token successfully");
                        log.info("Refresh map deleted the user id and token successfully");
                        return;
                    }

                    token = refreshResponse.getAccessToken();
                    tasks = taskService.sendShowTasksRequest(token);

                    map.put(messageChatId, token);
                    refresh.put(messageChatId, refreshResponse.getRefreshToken());
                }
                if (tasks.equals("[]")) {
                    sendMessage(messageChatId, MessagePull.EMPTY_LIST_MESSAGE);
                    return;
                }
                sendMessage(messageChatId, taskService.tasksFromJsonString(tasks));
                log.info("User has been received a task list");
            } catch (ObjectNotFoundException | IOException e) {
                log.warn("This user does not have such task");
            }

            handleInAccountState(messageChatId, ECommand.RUN.getCommand());

        } else if (messageText.equals(ECommand.SIGNOUT.getCommand())) {
            sendMessage(messageChatId, MessagePull.SIGNOUT_MESSAGE);
            map.remove(messageChatId);
            refresh.remove(messageChatId);
            botState = BotState.MENU;
            log.info("Access map deleted the user id and token successfully");
            log.info("Refresh map deleted the user id and token successfully");
            log.info("User logged out of the account");
        }
    }

    public void handleCreateState(long messageChatId, String messageText) {
        try {
            map = redissonClient.getMapCache(ACCESS_CACHE);
            refresh = redissonClient.getMapCache(REFRESH_CACHE);
            TaskRequest taskRequest = new TaskRequest();
            taskRequest.setData(messageText);
            String token = (map.containsKey(messageChatId)) ? map.get(messageChatId) : jwtResponse.getAccessToken();
            String refreshToken = (refresh.containsKey(messageChatId)) ? refresh.get(messageChatId) : jwtResponse.getRefreshToken();
            taskService.sendCreateTaskRequest(token, taskRequest);
            if (taskService.getStatusCode() != HttpStatus.SC_OK) {
                RefreshResponse refreshResponse = processingRefreshTokens(refreshToken);
                if (checkRefreshState(authService.getStatusCode())) {
                    //убрать дублирование
                    sendMessage(messageChatId, MessagePull.SESSION_EXPIRED);
                    botState = BotState.MENU;
                    currentState = LoginState.ASK_USERNAME;
                    map.remove(messageChatId);
                    refresh.remove(messageChatId);
                    handleMenuState(messageChatId);
                    log.info("Access map deleted the user id and token successfully");
                    log.info("Refresh map deleted the user id and token successfully");
                    return;
                }

                token = refreshResponse.getAccessToken();
                taskService.sendCreateTaskRequest(token, taskRequest);

                map.put(messageChatId, token);
                refresh.put(messageChatId, refreshResponse.getRefreshToken());
            }
            sendMessage(messageChatId, MessagePull.TASK_CREATED_MESSAGE);
            handleInAccountState(messageChatId, ECommand.RUN.getCommand());
            botState = BotState.NEXT;
            log.info("User has been created a task");
        } catch (AuthException | IOException e) {
            log.warn("User has bad credentials");
        }
    }

    public RefreshResponse processingRefreshTokens(String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    private boolean checkRefreshState(int statusCode) {
        return statusCode != HttpStatus.SC_UNAUTHORIZED;
    }

    public void initCommands() {
        try {
            List<BotCommand> listOfCommands = new ArrayList<>();
            listOfCommands.add(new BotCommand(
                    ECommand.START.getCommand(),
                    MessagePull.INFO_START_MESSAGE));
            bot.execute(new SetMyCommands(
                    listOfCommands,
                    new BotCommandScopeDefault(),
                    null));
        } catch (TelegramApiException E) {
            log.warn("Initialization of commands processing failed");
        }
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Initialization of message processing failed");
        }
    }
}
