package com.jraft.model.payload;

public class VoteRequestPayloadData implements VoteRequestPayload {

    private int logLength;
    private int logTerm;

    public VoteRequestPayloadData(int logLength, int logTerm) {
        this.logLength = logLength;
        this.logTerm = logTerm;
    }

    @Override
    public int getLogLength() {
        return logLength;
    }

    @Override
    public int getLogTerm() {
        return logTerm;
    }
}
