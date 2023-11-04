package com.ozgen.jraft.model;

public class LogEntry {

    private Term term;
    private Message message;

    public LogEntry(Term term, Message message) {
        this.term = term;
        this.message = message;
    }

    public Term getTerm() {
        return term;
    }

    public Message getMessage() {
        return message;
    }
}
