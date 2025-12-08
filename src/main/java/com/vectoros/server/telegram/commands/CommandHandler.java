package com.vectoros.server.telegram.commands;

public interface CommandHandler {
    String getCommand();
    void handle(Long chatId, String text);
}

