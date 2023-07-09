package com.jraft.model.payload;

public class LogResponsePayloadData implements LogResponsePayload {

    private int ack;

    private boolean success;

    public LogResponsePayloadData(int ack, boolean success) {
        this.ack = ack;
        this.success = success;
    }

    @Override
    public int getAck() {
        return ack;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }
}
