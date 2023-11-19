package com.ozgen.jraft.model.message.payload;

public interface VoteResponsePayload {
    /**
     * Checks if the vote is granted.
     *
     * @return True if the vote is granted, false otherwise.
     */
    public boolean isGranted();
}
