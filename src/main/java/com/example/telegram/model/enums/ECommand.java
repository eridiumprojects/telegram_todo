package com.example.telegram.model.enums;

import java.util.Set;

public enum ECommand {
    MENU("/menu"),
    LOGIN("/login"),
    CREATE("/create"),
    SHOW("/show"),
    START("/start"),
    SIGNOUT("/signout");
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
