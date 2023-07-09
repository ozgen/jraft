package com.jraft.model.payload;

import com.jraft.model.LogEntry;

import java.util.List;

public interface LogRequestPayload {
    /**
     * Get the prefix length indicating the length of the log entries already replicated by the follower.
     *
     * @return The prefix length.
     */
    public int getPrefixLength();

    /**
     * Get the prefix term indicating the term of the last log entry already replicated by the follower.
     *
     * @return The prefix term.
     */
    public int getPrefixTerm();

    /**
     * Get the leader commit value indicating the index of the highest log entry known to be committed by the leader.
     *
     * @return The leader commit value.
     */
    public int getLeaderCommit();
    /**
     * Get the list of log entries that need to be replicated from the leader to the follower.
     *
     * @return The list of log entries. {@link LogEntry}
     */
    public List<LogEntry> getSuffixList();
}
