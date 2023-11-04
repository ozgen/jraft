package com.ozgen.jraft.model.payload;

import com.ozgen.jraft.model.Term;

public interface VoteRequestPayload {
    /**
     * Gets the length of the candidate's log.
     *
     * @return The length of the log.
     */
    public int getLogLength();

    /**
     * Gets the term of the last log entry in the candidate's log.
     *
     * @return The term of the last log entry.
     */
    public Term getLastTerm();
}
