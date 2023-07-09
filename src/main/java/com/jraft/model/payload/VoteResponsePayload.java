package com.jraft.model.payload;

public interface VoteResponsePayload {
    /**
     * Check if the vote is granted.
     *
     * @return True if the vote is granted, false otherwise.
     */
    public boolean isGranted();
}
