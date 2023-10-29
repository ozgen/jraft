package com.ozgen.jraft.model;

import com.ozgen.jraft.model.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NodeData {

    private int currentTerm;
    private int votedFor;
    private List<LogEntry> log;
    private int commitLength;
    private Role currentRole;
    private int currentLeader;
    private int votesReceived;
    private int sentLength;
    private int ackedLength;


    @Builder
    public NodeData() {
        this.currentTerm = 0; // Starting term
        this.votedFor = -1;  // Assuming -1 indicates not voted for anyone
        this.log = new ArrayList<>();  // Empty log at start
        this.commitLength = 0;
        this.currentRole = Role.FOLLOWER; // Nodes start as followers in Raft
        this.currentLeader = -1;  // Assuming -1 indicates no known leader
        this.votesReceived = 0;
        this.sentLength = 0;
        this.ackedLength = 0;
    }

}
