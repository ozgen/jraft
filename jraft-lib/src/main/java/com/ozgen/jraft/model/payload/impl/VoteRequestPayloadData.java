package com.ozgen.jraft.model.payload.impl;

import com.ozgen.jraft.model.payload.VoteRequestPayload;
import lombok.Builder;

public class VoteRequestPayloadData implements VoteRequestPayload {

    private int logLength;
    private int lastTerm;

    @Builder
    public VoteRequestPayloadData(int logLength, int lastTerm) {
        this.logLength = logLength;
        this.lastTerm = lastTerm;
    }

    @Override
    public int getLogLength() {
        return logLength;
    }

    @Override
    public int getLastTerm() {
        return lastTerm;
    }
}

