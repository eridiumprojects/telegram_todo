package com.example.telegram.service;

import com.example.telegram.model.constant.MessagePull;
import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.request.TaskRequest;
import com.example.telegram.model.dto.response.JwtResponse;
import com.example.telegram.model.enums.BotState;
import com.example.telegram.model.enums.ECommand;
import com.example.telegram.model.enums.LoginState;
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


    public BotService(TelegramLongPollingBot bot, RedissonClient redissonClient) {
        this.bot = bot;
        this.redissonClient = redissonClient;
        currentState = LoginState.ASK_USERNAME;
        this.loginUser = new LoginRequest();
        this.authService = new AuthService(new RequestBuilder(new ObjectMapper()));
        this.taskService = new TaskService(new RequestBuilder(new ObjectMapper()));
    }

    public void handleMenuState(long messageChatId) {
        RMapCache<Long, String> map = redissonClient.getMapCache("myCache");
        if (map.containsKey(messageChatId)) {
            sendMessage(messageChatId, "Вы уже авторизованы в аккаунт");
            sendMessage(messageChatId, MessagePull.NEXT_ACTS_MESSAGE +
                    ECommand.RUN.getCommand());
            botState = BotState.IN_ACCOUNT;
        } else {
            sendMessage(messageChatId,
                    MessagePull.LOGIN_IN_ACCOUNT_WITH_MESSAGE + ECommand.LOGIN.getCommand());
            botState = BotState.LOGIN;
        }
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
        } else {
            sendMessage(messageChatId, MessagePull.INVALID_COMMAND_MESSAGE);
        }
    }

    public void processAskPassword(long messageChatId, String messageText) {
        loginUser.setUsername(messageText);
        sendMessage(messageChatId, MessagePull.INPUT_PASSWORD_MESSAGE);
        currentState = LoginState.LOGIN_PROCESSING;
    }

    public void processLoginProcessing(long messageChatId, String messageText) {
        RMapCache<Long, String> map = redissonClient.getMapCache("myCache");
        loginUser.setPassword(messageText);
        try {
            authService.sendSignInRequest(loginUser);
            if (authService.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                log.info("unauthorized");
                sendMessage(messageChatId, MessagePull.INVALID_DATA_MESSAGE);
                botState = BotState.MENU;
                currentState = LoginState.ASK_USERNAME;
            }
            if (authService.getStatusCode() == HttpStatus.SC_OK) {
                log.info("in account state with credentials" + loginUser.getUsername() + loginUser.getUsername());
                log.info("calling jwtResponse");
                JwtResponse jwtResponse = authService.jwtFromJsonString(authService.sendSignInRequest(loginUser));
                log.info("jwt is received");
                map.put(messageChatId, jwtResponse.getAccessToken());
                log.info("put data in map");
                sendMessage(messageChatId, MessagePull.SUCCESSFULLY_LOGGED_MESSAGE);
                sendMessage(messageChatId, MessagePull.NEXT_ACTS_MESSAGE +
                        ECommand.RUN.getCommand());
                botState = BotState.IN_ACCOUNT;
                currentState = LoginState.ASK_USERNAME;
                log.info("map info" + map.get(messageChatId));
            }
        } catch (IOException e) {
            //TODO send dev id with asking to call him, unexpected error
            throw new RuntimeException(e);
        }
    }

    public void handleInAccountState(long messageChatId, String messageText) {
        if (messageText.equals(ECommand.RUN.getCommand())
                || messageText.equals(ECommand.RETURN.getCommand())) {
            sendMessage(messageChatId,
                    MessagePull.IN_ACCOUNT_FIRST_MESSAGE +
                            ECommand.CREATE.getCommand() +
                            MessagePull.IN_ACCOUNT_SECOND_MESSAGE +
                            ECommand.SHOW.getCommand() +
                            MessagePull.IN_ACCOUNT_THIRD_MESSAGE +
                            ECommand.SIGNOUT.getCommand());
            botState = BotState.NEXT;
        } else {
            sendMessage(messageChatId, MessagePull.INVALID_COMMAND_MESSAGE);
        }
    }

    public void handleNextState(long messageChatId, String messageText) {
        String token = "";
        RMapCache<Long, String> map = redissonClient.getMapCache("myCache");
        if (map.containsKey(messageChatId)) {
//            System.out.println(map.get(messageChatId));
            token = map.get(messageChatId);
        } else {
            try {
                token = authService.jwtFromJsonString(authService.sendSignInRequest(loginUser)).getAccessToken();
            } catch (IOException e) {
                //TODO the same problem
                throw new RuntimeException(e);
            }
        }
        if (messageText.equals(ECommand.CREATE.getCommand())) {
            botState = BotState.CREATE;
            sendMessage(messageChatId, MessagePull.INPUT_TASK_DATA_MESSAGE);
        } else if (messageText.equals(ECommand.SHOW.getCommand())) {
            try {
                botState = BotState.SHOW;
                String tasks = taskService.sendShowTasksRequest(token);
                if (tasks.equals("[]")) {
                    sendMessage(messageChatId, MessagePull.EMPTY_LIST_MESSAGE);
                } else {
                    sendMessage(messageChatId, taskService.tasksFromJsonString(tasks));
                }
            } catch (IOException e) {
                throw new RuntimeException();
            }
            sendMessage(messageChatId, MessagePull.RETURN_MESSAGE + ECommand.RETURN.getCommand());
            botState = BotState.IN_ACCOUNT;
        } else if (messageText.equals(ECommand.SIGNOUT.getCommand())) {
            sendMessage(messageChatId, MessagePull.SIGNOUT_MESSAGE);
            map.remove(messageChatId);
            botState = BotState.MENU;
        }
    }

    public void handleCreateState(long messageChatId, String messageText) {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setData(messageText);
        String token = "";
        RMapCache<Long, String> map = redissonClient.getMapCache("myCache");
        if (map.containsKey(messageChatId)) {
//            System.out.println(map.get(messageChatId));
            token = map.get(messageChatId);
        } else {
            try {
                token = authService.jwtFromJsonString(authService.sendSignInRequest(loginUser)).getAccessToken();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
        try {
            taskService.sendCreateTaskRequest(token, taskRequest);
        } catch (IOException e) {
            //TODO the same problem
            throw new RuntimeException(e);
        }
        sendMessage(messageChatId, MessagePull.TASK_CREATED_MESSAGE);
        sendMessage(messageChatId, MessagePull.RETURN_MESSAGE + ECommand.RETURN.getCommand());
        botState = BotState.IN_ACCOUNT;
    }

    public void initCommands() {
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand(
                ECommand.START.getCommand(),
                MessagePull.INFO_START_MESSAGE));
        try {
            bot.execute(new SetMyCommands(
                    listOfCommands,
                    new BotCommandScopeDefault(),
                    null));
        } catch (TelegramApiException E) {
            //TODO the same problem
            E.printStackTrace();
        }
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            //TODO the same problem
            throw new RuntimeException(e);
        }
    }
}
