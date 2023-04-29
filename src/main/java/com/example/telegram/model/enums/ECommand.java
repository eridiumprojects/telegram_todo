package com.example.telegram.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum ECommand {
    MENU("/menu"),
    LOGIN("/login"),
    CREATE("/create"),
    SHOW("/show"),
    START("/start"),
    SIGNOUT("/signout");

    public static final Map<String, ECommand> stringCommands = Arrays.stream(ECommand.values())
                    .collect(Collectors.toMap(ECommand::getCommand, o -> o));
    public static final Set<String> commands = Set.of(
            MENU.getCommand(),
            LOGIN.getCommand(),
            CREATE.getCommand(),
            SHOW.getCommand(),
            START.getCommand(),
            SIGNOUT.getCommand()
    );

    private final String command;

    ECommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
