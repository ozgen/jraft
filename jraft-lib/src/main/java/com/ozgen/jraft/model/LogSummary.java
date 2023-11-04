package com.ozgen.jraft.model;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class LogSummary implements Comparable<LogSummary> {

    private static final int EQUAL = 0;
    private static final int MORE_UP_TO_DATE = -1;
    private static final int LESS_UP_TO_DATE = 1;
    private final Term lastLogTerm;
    private final int lastLogIndex;

    public LogSummary(Optional<Term> lastLogTerm, int lastLogIndex) {
        this.lastLogTerm = lastLogTerm.orElse(null);
        this.lastLogIndex = lastLogIndex;
    }

    public Optional<Term> getLastLogTerm() {
        return Optional.ofNullable(lastLogTerm);
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    @Override
    public int compareTo(LogSummary other) {
        if (lastLogTerm == null) {
            return other.lastLogTerm == null ? EQUAL : MORE_UP_TO_DATE;
        } else if (other.lastLogTerm == null) {
            return LESS_UP_TO_DATE;
        }

        // Neither log is empty
        return Comparator.comparing(ls -> ((LogSummary) ls).lastLogTerm)
                .thenComparing(ls -> ((LogSummary) ls).lastLogIndex)
                .compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogSummary logSummary = (LogSummary) o;
        return lastLogIndex == logSummary.lastLogIndex &&
                Objects.equals(lastLogTerm, logSummary.lastLogTerm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastLogTerm, lastLogIndex);
    }
}
