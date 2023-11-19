package com.ozgen.jraft.model.message.payload;

public interface LogResponsePayload {
    /**
     * Gets the acknowledgement value indicating the number of successful responses received.
     *
     * @return The acknowledgement value.
     */
    public int getAck();
    /**
     * Checks if the response was successful.
     *
     * @return True if the log response was successful, false otherwise.
     */
    public boolean isGranted();
}
