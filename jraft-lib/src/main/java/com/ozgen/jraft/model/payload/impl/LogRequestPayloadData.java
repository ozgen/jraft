package com.ozgen.jraft.model.payload.impl;

import com.ozgen.jraft.model.LogEntry;
import com.ozgen.jraft.model.Term;
import com.ozgen.jraft.model.payload.LogRequestPayload;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogRequestPayloadData implements LogRequestPayload {


    private int prefixLength;
    private Term prefixTerm;
    private int leaderCommit;
    private CopyOnWriteArrayList<LogEntry> suffix;
    private String leaderId;

    public LogRequestPayloadData(int prefixLength, Term prefixTerm, int leaderCommit, CopyOnWriteArrayList<LogEntry> suffix, String leaderId) {
        this.prefixLength = prefixLength;
        this.prefixTerm = prefixTerm;
        this.leaderCommit = leaderCommit;
        if (suffix == null || suffix.isEmpty()) {
            suffix = new CopyOnWriteArrayList<>();
        }
        this.suffix = suffix;
        this.leaderId = Objects.requireNonNull(leaderId);
    }

    @Override
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public Term getPrefixTerm() {
        return prefixTerm;
    }

    @Override
    public int getLeaderCommit() {
        return leaderCommit;
    }

    @Override
    public String getLeaderId() {
        return leaderId;
    }

    @Override
    public CopyOnWriteArrayList<LogEntry> getSuffixList() {
        return suffix;
    }
}
