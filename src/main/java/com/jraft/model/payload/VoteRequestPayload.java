package com.jraft.model.payload;

public interface VoteRequestPayload {
    /**
     * Get the length of the candidate's log.
     *
     * @return The length of the log.
     */
    public int getLogLength();

    /**
     * Get the term of the last log entry in the candidate's log.
     *
     * @return The term of the last log entry.
     */
    public int getLogTerm();
}
