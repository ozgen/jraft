package com.ozgen.jraft.model.payload;

import com.ozgen.jraft.model.LogEntry;
import lombok.Builder;

import java.util.List;
import java.util.Objects;

public class LogRequestPayloadData implements LogRequestPayload {


    private int prefixLength;
    private int prefixTerm;
    private int leaderCommit;
    private List<LogEntry> suffix;
    private int leaderId;

    @Builder
    public LogRequestPayloadData(int prefixLength, int prefixTerm, int leaderCommit, List<LogEntry> suffix, int leaderId) {
        this.prefixLength = Objects.requireNonNull(prefixLength);
        this.prefixTerm = Objects.requireNonNull(prefixTerm);
        this.leaderCommit = Objects.requireNonNull(leaderCommit);
        if (suffix == null || suffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must not be null or empty");
        }
        this.suffix = suffix;
        this.leaderId = Objects.requireNonNull(leaderId);
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
    public int getLeaderId() {
        return leaderId;
    }

    @Override
    public List<LogEntry> getSuffixList() {
        return suffix;
    }
}
