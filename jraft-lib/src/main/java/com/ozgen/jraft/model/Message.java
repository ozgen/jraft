package com.ozgen.jraft.model;

import com.ozgen.jraft.model.payload.LogRequestPayload;
import com.ozgen.jraft.model.payload.LogResponsePayload;
import com.ozgen.jraft.model.payload.VoteRequestPayload;
import com.ozgen.jraft.model.payload.VoteResponsePayload;
import com.ozgen.jraft.model.payload.impl.LogRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.LogResponsePayloadData;
import com.ozgen.jraft.model.payload.Payload;
import com.ozgen.jraft.model.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.VoteResponsePayloadData;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter
@Slf4j
public class Message {

    private int sender;
    private int term;
    private Payload payload;

    // Vote Request Message Constructor
    @Builder(builderMethodName = "buildWithVoteForRequestPayload")
    Message(int sender, int term, VoteRequestPayload voteRequestPayload) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) voteRequestPayload;
    }

    // Vote Response Message Constructor
    @Builder(builderMethodName = "buildWithVoteResponsePayload")
    Message(int sender, int term, VoteResponsePayload voteResponsePayload) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) voteResponsePayload;
    }

    // Log Request Message Constructor
    @Builder(builderMethodName = "buildWithLogRequestPayload")
    public Message(int sender, int term, LogRequestPayload logRequestPayload) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) logRequestPayload;
    }

    // Log Response Message Constructor
    @Builder(builderMethodName = "buildWithLogResponsePayload")
    Message(int sender, int term, LogResponsePayload logResponsePayload) {
        this.sender = sender;
        this.term = term;
        this.payload = (Payload) logResponsePayload;
    }
}
