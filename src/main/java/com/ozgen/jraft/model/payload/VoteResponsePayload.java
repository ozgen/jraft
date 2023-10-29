package com.ozgen.jraft.model.payload;

public interface VoteResponsePayload {
    /**
     * Checks if the vote is granted.
     *
     * @return True if the vote is granted, false otherwise.
     */
    public boolean isGranted();
}
