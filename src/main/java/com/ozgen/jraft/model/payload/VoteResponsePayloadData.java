package com.ozgen.jraft.model.payload;

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
