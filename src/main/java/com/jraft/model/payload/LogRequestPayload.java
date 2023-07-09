package com.jraft.model.payload;

import com.jraft.model.LogEntry;

import java.util.List;

public interface LogRequestPayload {

    public int getPrefixLength();

    public int getPrefixTerm();

    public int getLeaderCommit();

    public List<LogEntry> getSuffixList();
}
