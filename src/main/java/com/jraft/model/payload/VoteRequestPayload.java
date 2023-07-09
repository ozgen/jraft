package com.jraft.model.payload;

public interface VoteRequestPayload {

    public int getLogLength();

    public int getLogTerm();
}
