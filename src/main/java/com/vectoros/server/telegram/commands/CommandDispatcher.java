package com.vectoros.server.telegram.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommandDispatcher {

    private final List<CommandHandler> handlers;

    public CommandHandler findHandler(String text) {
        if (text == null) return null;

        return handlers.stream()
                .filter(h -> text.startsWith(h.getCommand()))
                .findFirst()
                .orElse(null);
    }
}
