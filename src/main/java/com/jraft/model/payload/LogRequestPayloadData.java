package com.jraft.model.payload;

import com.jraft.model.LogEntry;

import java.util.List;

public class LogRequestPayloadData implements LogRequestPayload {


    private int prefixLength;
    private int prefixTerm;
    private int leaderCommit;
    private List<LogEntry> suffix;

    public LogRequestPayloadData(int prefixLength, int prefixTerm, int leaderCommit, List<LogEntry> suffix) {
        this.prefixLength = prefixLength;
        this.prefixTerm = prefixTerm;
        this.leaderCommit = leaderCommit;
        this.suffix = suffix;
    }

    @Override
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public int getPrefixTerm() {
        return prefixTerm;
    }

    @Override
    public int getLeaderCommit() {
        return leaderCommit;
    }

    @Override
    public List<LogEntry> getSuffixList() {
        return suffix;
    }
}
