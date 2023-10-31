package com.ozgen.jraft.model;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LogEntry {

    private int term;
    private Message message;

    @Builder
    LogEntry(int term, Message message) {
        this.term = term;
        this.message = message;
    }
}
