package com.example.telegram.model.enums;

import java.util.Set;

public enum ECommand {
    MENU("/menu"),
    LOGIN("/login"),
    RUN("/run"),
    CREATE("/create"),
    SHOW("/show"),
    START("/start"),
    SIGNOUT("/signout");
    public static final Set<String> inAccountCommands = Set.of(
            CREATE.getCommand(),
            SHOW.getCommand(),
            SIGNOUT.getCommand(),
            MENU.getCommand());

    public static final Set<String> commands = Set.of(
            MENU.getCommand(),
            LOGIN.getCommand(),
            RUN.getCommand(),
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
