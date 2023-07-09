package com.jraft.model;

import com.jraft.model.payload.*;

import java.util.List;

public class Message {

    private int sender;
    private int term;
    private Payload payload;

    // Vote Request Message Constructor
    Message( int sender, int term, int logLength, int logTerm) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) new VoteRequestPayloadData(logLength, logTerm);
    }

    // Vote Response Message Constructor
    Message( int sender, int term, boolean granted) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) new VoteResponsePayloadData(granted);
    }

    // Log Request Message Constructor
    public Message( int sender, int term, int prefixLength, int prefixTerm, int leaderCommit, List<LogEntry> suffix) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) new LogRequestPayloadData(prefixLength, prefixTerm, leaderCommit, suffix);
        // Include the suffix field specific to the log request message
        // Set the suffix field to the given suffix object
    }

    // Log Response Message Constructor
    Message( int sender, int term, int ack, boolean success) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) new LogResponsePayloadData(ack, success);
    }


    public int getSender() {
        return sender;
    }

    public int getTerm() {
        return term;
    }

    public Payload getPayload() {
        return payload;
    }


}
