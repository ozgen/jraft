package com.ozgen.jraft;

import com.ozgen.jraft.communications.GRPCluster;
import com.ozgen.jraft.model.LogEntry;
import com.ozgen.jraft.model.Message;
import com.ozgen.jraft.model.Term;
import com.ozgen.jraft.model.converter.MsgToGrpcConverter;
import com.ozgen.jraft.model.enums.Role;
import com.ozgen.jraft.model.payload.VoteRequestPayload;
import com.ozgen.jraft.model.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.utils.UUIDGenerator;

import java.util.concurrent.CopyOnWriteArrayList;


public class NodeServer {

    private String id;
    private Term currentTerm;
    private String votedFor;
    private CopyOnWriteArrayList<LogEntry> logs;
    private int commitLength;
    private Role currentRole;
    private String currentLeader;
    private int votesReceived;
    private int sentLength;
    private int ackedLength;
    private final GRPCluster<String> cluster;

    private final MsgToGrpcConverter msgToGrpcConverter;


    public NodeServer(GRPCluster<String> cluster, MsgToGrpcConverter msgToGrpcConverter) {
        this.id = UUIDGenerator.generateNodeId();
        this.currentTerm = new Term(0);
        this.votedFor = null;  // Assuming -1 indicates not voted for anyone
        this.logs = new CopyOnWriteArrayList<>();  // Empty log at start
        this.commitLength = 0;
        this.currentRole = Role.FOLLOWER; // Nodes start as followers in Raft
        this.currentLeader = null;  // Assuming -1 indicates no known leader
        this.votesReceived = 0;
        this.sentLength = 0;
        this.ackedLength = 0;
        this.cluster = cluster;
        this.msgToGrpcConverter = msgToGrpcConverter;
    }

    public void electionTimeout() {
        this.currentRole = Role.CANDIDATE;
        this.currentTerm = currentTerm.next();
        votedFor = null;
        VoteRequestPayload voteRequestPayload = new VoteRequestPayloadData(this.logs.size(),currentTerm);

        Message message = new Message(this.id, this.currentTerm, voteRequestPayload);

        cluster.broadcastVoteRequest(this.msgToGrpcConverter.convert(message));
    }

    public Term getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(Term currentTerm) {
        this.currentTerm = currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
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

    public Role getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole;
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public void setCurrentLeader(String currentLeader) {
        this.currentLeader = currentLeader;
    }

    public int getVotesReceived() {
        return votesReceived;
    }

    public void setVotesReceived(int votesReceived) {
        this.votesReceived = votesReceived;
    }

    public int getSentLength() {
        return sentLength;
    }

    public void setSentLength(int sentLength) {
        this.sentLength = sentLength;
    }

    public int getAckedLength() {
        return ackedLength;
    }

    public void setAckedLength(int ackedLength) {
        this.ackedLength = ackedLength;
    }

    public GRPCluster<String> getCluster() {
        return cluster;
    }
}
