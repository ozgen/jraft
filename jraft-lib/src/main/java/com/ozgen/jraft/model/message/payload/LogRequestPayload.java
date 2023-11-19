package com.ozgen.jraft.model.message.payload;

import com.ozgen.jraft.model.message.LogEntry;
import com.ozgen.jraft.model.message.Term;

import java.util.concurrent.CopyOnWriteArrayList;

public interface LogRequestPayload {
    /**
     * Gets the prefix length indicating the length of the log entries already replicated by the follower.
     *
     * @return The prefix length.
     */
    public int getPrefixLength();

    /**
     * Gets the prefix term indicating the term of the last log entry already replicated by the follower.
     *
     * @return The prefix term.
     */
    public Term getPrefixTerm();

    /**
     * Gets the leader commit value indicating the index of the highest log entry known to be committed by the leader.
     *
     * @return The leader commit value.
     */
    public int getLeaderCommit();
    /**
     * Gets selected leader id
     *
     * @return The leader commit value.
     */
    public String getLeaderId();
    /**
     * Gets the list of log entries that need to be replicated from the leader to the follower.
     *
     * @return The list of log entries. {@link LogEntry}
     */
    public CopyOnWriteArrayList<LogEntry> getSuffixList();
}
