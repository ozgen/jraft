package com.jraft.model;

public class LogEntry {
    private int term;
    private Message message;

    LogEntry(int term, Message message) {
        this.term = term;
        this.message = message;
    }

    public int getTerm() {
        return term;
    }

    public Message getMessage() {
        return message;
    }
}
