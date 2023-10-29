package com.ozgen.jraft.model.payload;

import lombok.Builder;

public class LogResponsePayloadData implements LogResponsePayload {

    private int ack;

    private boolean granted;

    @Builder
    public LogResponsePayloadData(int ack, boolean granted) {
        this.ack = ack;
        this.granted = granted;
    }

    @Override
    public int getAck() {
        return ack;
    }

    @Override
    public boolean isGranted() {
        return granted;
    }
}
