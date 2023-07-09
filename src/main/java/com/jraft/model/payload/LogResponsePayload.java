package com.jraft.model.payload;

public interface LogResponsePayload {

    public int getAck();

    public boolean isSuccess();
}
