package com.jraft.model.payload;

public interface LogResponsePayload {
    /**
     * Get the acknowledgement value indicating the number of successful responses received.
     *
     * @return The acknowledgement value.
     */
    public int getAck();
    /**
     * Check if the response was successful.
     *
     * @return True if the log response was successful, false otherwise.
     */
    public boolean isSuccess();
}
