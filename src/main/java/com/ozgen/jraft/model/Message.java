package com.ozgen.jraft.model;

import com.jraft.model.payload.*;
import com.ozgen.jraft.model.payload.LogRequestPayloadData;
import com.ozgen.jraft.model.payload.LogResponsePayloadData;
import com.ozgen.jraft.model.payload.Payload;
import com.ozgen.jraft.model.payload.VoteRequestPayloadData;
import com.ozgen.jraft.model.payload.VoteResponsePayloadData;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class Message {

    private int sender;
    private int term;
    private Payload payload;

    // Vote Request Message Constructor
    @Builder(builderMethodName = "buildWithVoteForRequestPayload")
    Message(int sender, int term, int logLength, int logTerm) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) VoteRequestPayloadData.builder()
                .logLength(logLength)
                .lastTerm(logTerm)
                .build();
    }

    // Vote Response Message Constructor
    @Builder(builderMethodName = "buildWithVoteResponsePayload")
    Message(int sender, int term, boolean granted) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) VoteResponsePayloadData.builder()
                .granted(granted)
                .build();
    }

    // Log Request Message Constructor
    @Builder(builderMethodName = "buildWithLogRequestPayload")
    public Message(int sender, int term, int prefixLength, int prefixTerm, int leaderCommit, List<LogEntry> suffix, int leaderId) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) LogRequestPayloadData.builder()
                .prefixLength(prefixLength)
                .prefixTerm(prefixTerm)
                .leaderCommit(leaderCommit)
                .suffix(suffix)
                .leaderId(leaderId)
                .build();
        // Include the suffix field specific to the log request message
        // Set the suffix field to the given suffix object
    }

    // Log Response Message Constructor
    @Builder(builderMethodName = "buildWithLogResponsePayload")
    Message(int sender, int term, int ack, boolean granted) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) LogResponsePayloadData.builder()
                .ack(ack)
                .granted(granted)
                .build();
    }
}
