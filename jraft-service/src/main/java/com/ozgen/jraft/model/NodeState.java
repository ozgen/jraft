package com.ozgen.jraft.model;

import com.ozgen.jraft.model.enums.Role;

import java.util.concurrent.CopyOnWriteArrayList;

public class NodeState {
    private String id;
    private Term currentTerm;
    private String votedFor;
    private Role currentRole;
    private String currentLeader;
    private int votesReceived;

    private int commitLength;

    private CopyOnWriteArrayList<LogEntry> logs;

    public NodeState(String id, Term currentTerm, String votedFor, Role currentRole, String currentLeader, int votesReceived, int commitLength, CopyOnWriteArrayList<LogEntry> logs) {
        this.id = id;
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.currentRole = currentRole;
        this.currentLeader = currentLeader;
        this.votesReceived = votesReceived;
        this.commitLength = commitLength;
        this.logs = logs;
    }

    public void transitionToLeader() {
        this.currentRole = Role.LEADER;
        this.currentLeader = this.id;
        // Reset the votesReceived for future elections
        this.votesReceived = 0;
        // Additional tasks to initiate as leader
    }

    public void revertToFollower() {
        this.currentRole = Role.FOLLOWER;
        // Reset the votesReceived for future elections
        this.votesReceived = 0;
        // Reset the election timer or other follower-specific tasks if necessary
    }

    public String getId() {
        return id;
    }

    public Term getCurrentTerm() {
        return currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public int getVotesReceived() {
        return votesReceived;
    }

    public void setCurrentTerm(Term currentTerm) {
        this.currentTerm = currentTerm;
    }

    public void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole;
    }

    public void setCurrentLeader(String currentLeader) {
        this.currentLeader = currentLeader;
    }

    public void setVotesReceived(int votesReceived) {
        this.votesReceived = votesReceived;
    }

    public CopyOnWriteArrayList<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(CopyOnWriteArrayList<LogEntry> logs) {
        this.logs = logs;
    }

    public int getCommitLength() {
        return commitLength;
    }

    public void setCommitLength(int commitLength) {
        this.commitLength = commitLength;
    }
}
