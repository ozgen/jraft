package com.jraft.model.payload;

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
