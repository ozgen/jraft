package com.ozgen.jraft.model.message.payload.impl;

import com.ozgen.jraft.model.message.payload.VoteResponsePayload;

public class VoteResponsePayloadData implements VoteResponsePayload {

    private boolean granted;

    public VoteResponsePayloadData(boolean granted) {
        this.granted = granted;
    }

    @Override
    public boolean isGranted() {
        return granted;
    }
}
