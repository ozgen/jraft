package com.jraft.model.payload;

public class VoteRequestPayloadData implements VoteRequestPayload {

    private int logLength;
    private int lastTerm;

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

