package com.ozgen.jraft.model.message.payload.impl;

import com.ozgen.jraft.model.message.Term;
import com.ozgen.jraft.model.message.payload.VoteRequestPayload;

public class VoteRequestPayloadData implements VoteRequestPayload {

    private int logLength;
    private Term lastTerm;

    public VoteRequestPayloadData(int logLength, Term lastTerm) {
        this.logLength = logLength;
        this.lastTerm = lastTerm;
    }

    @Override
    public int getLogLength() {
        return logLength;
    }

    @Override
    public Term getLastTerm() {
        return lastTerm;
    }
}

