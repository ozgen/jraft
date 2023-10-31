package com.ozgen.jraft.model.payload.impl;

import com.ozgen.jraft.model.payload.VoteResponsePayload;
import lombok.Builder;

public class VoteResponsePayloadData implements VoteResponsePayload {

    private boolean granted;

    @Builder
    public VoteResponsePayloadData(boolean granted) {
        this.granted = granted;
    }

    @Override
    public boolean isGranted() {
        return granted;
    }
}
