package com.ozgen.jraft.model.message;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class Term implements Serializable, Comparable<Term> {

    private final int number;

    private final Instant createdAt;

    public Term(int number) {
        this.number = number;
        this.createdAt = Instant.now();
    }

    public Term(int number, Instant createdAt) {
        this.number = number;
        this.createdAt = createdAt;
    }

    public boolean isLessThan(Term otherTerm) {
        return compareTo(otherTerm) < 0;
    }

    public boolean isGreaterThan(Term otherTerm) {
        return compareTo(otherTerm) > 0;
    }
    public boolean isCreatedEarlierThan(Term otherTerm) {
        return createdAt.isBefore(otherTerm.createdAt);
    }

    public Term next() {
        return new Term(number + 1);
    }

    @Override
    public int compareTo(Term otherTerm) {
        return number - otherTerm.number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Term term = (Term) o;
        return number == term.number;
    }

    public int getNumber() {
        return number;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    public boolean equalsOrGreaterThan(Term otherTerm) {
        return compareTo(otherTerm) >= 0;
    }
}
