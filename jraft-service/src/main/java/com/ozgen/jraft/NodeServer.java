package com.ozgen.jraft;

import com.ozgen.jraft.model.LogEntry;
import com.ozgen.jraft.model.Message;
import com.ozgen.jraft.model.Term;
import com.ozgen.jraft.model.converter.GrpcToMsgConverter;
import com.ozgen.jraft.model.converter.MsgToGrpcConverter;
import com.ozgen.jraft.model.enums.Role;
import com.ozgen.jraft.model.payload.LogRequestPayload;
import com.ozgen.jraft.model.payload.LogResponsePayload;
import com.ozgen.jraft.model.payload.VoteRequestPayload;
import com.ozgen.jraft.model.payload.impl.LogRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.LogResponsePayloadData;
import com.ozgen.jraft.model.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.node.DefaultNodeServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class NodeServer extends DefaultNodeServer {

    private String id;
    private Term currentTerm;
    private String votedFor;
    private CopyOnWriteArrayList<LogEntry> logs;
    private int commitLength;
    private Role currentRole;
    private String currentLeader;
    private int votesReceived;

    private final MsgToGrpcConverter msgToGrpcConverter;
    private final GrpcToMsgConverter grpcToMsgConverter;

    private ConcurrentHashMap<String, Integer> sentLength = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> ackedLength = new ConcurrentHashMap<>();

    public NodeServer(String id, MsgToGrpcConverter msgToGrpcConverter, GrpcToMsgConverter grpcToMsgConverter) {
        this.id = id;
        this.currentTerm = new Term(0);
        this.votedFor = null;  // Assuming -1 indicates not voted for anyone
        this.logs = new CopyOnWriteArrayList<>();  // Empty log at start
        this.commitLength = 0;
        this.currentRole = Role.FOLLOWER; // Nodes start as followers in Raft
        this.currentLeader = null;  // Assuming -1 indicates no known leader
        this.votesReceived = 0;
        this.msgToGrpcConverter = msgToGrpcConverter;
        this.grpcToMsgConverter = grpcToMsgConverter;
    }

    public void sendVoteRequest() {
        this.currentRole = Role.CANDIDATE;
        this.currentTerm = this.currentTerm.next(); // Increment term
        this.votedFor = this.id;  // Vote for itself
        this.votesReceived = 1;   // Count the self-vote

        // Construct the vote request message
        VoteRequestPayload payload = new VoteRequestPayloadData(this.logs.size(), this.currentTerm);
        Message voteRequestMessage = new Message(this.id, this.currentTerm, payload);

        // Broadcast the vote request to all other nodes
        for (String nodeId : this.getNodes()) {
            if (!nodeId.equals(this.id)) {
                sendVoteRequestToNode(nodeId, this.msgToGrpcConverter.convert(voteRequestMessage));
            }
        }
    }

    public CompletableFuture<Void> sendLogRequest() {
        return CompletableFuture.runAsync(() -> {
            if (this.currentRole != Role.LEADER) {
                // The node must be a leader to send log requests.
                return;
            }

            // Construct the log request message
            int prefixLen = logs.size() - 1; // Assuming logs.size() gives the latest index + 1
            LogRequestPayload payload = new LogRequestPayloadData(prefixLen, this.currentTerm, this.commitLength, this.logs, this.id);
            Message logRequestMessage = new Message(this.id, this.currentTerm, payload);

            // List to hold all completable futures for broadcasting log requests
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Broadcast the log request to all followers
            for (String nodeId : this.getNodes()) {
                if (!nodeId.equals(this.id)) {
                    CompletableFuture<Void> future = sendLogRequestToNode(nodeId, this.msgToGrpcConverter.convert(logRequestMessage))
                            .thenApply(this.grpcToMsgConverter::convert)
                            .thenCompose(this::handleLogResponse);
                    futures.add(future);
                }
            }

            // Wait for all log requests to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        });
    }


    public CompletableFuture<Message> handleLogRequest(Message message) {
        LogRequestPayload payload = (LogRequestPayload) message.getPayload();
        return CompletableFuture.supplyAsync(() -> payload)
                .thenCompose(p -> appendEntries(p.getPrefixLength(), p.getLeaderCommit(), p.getSuffixList()));
    }


    public CompletableFuture<Void> handleLogResponse(Message message) {
        return CompletableFuture.supplyAsync(() -> {
            String sender = message.getSender();
            Term term = message.getTerm();
            LogResponsePayload payload = (LogResponsePayload) message.getPayload();
            return new Object[]{sender, term, payload.getAck(), payload.isGranted()};
        }).thenCompose(data -> onLogResponseReceived(
                (String) data[0],
                (Term) data[1],
                (int) data[2],
                (boolean) data[3]
        ));
    }

    public void electionTimeout() {
        this.currentRole = Role.CANDIDATE;
        this.currentTerm = currentTerm.next();
        this.votedFor = null;
        this.votesReceived = 0;

        VoteRequestPayload voteRequestPayload = new VoteRequestPayloadData(this.logs.size(), currentTerm);

        Message message = new Message(this.id, this.currentTerm, voteRequestPayload);
//        cluster.broadcastVoteRequest(this.msgToGrpcConverter.convert(message));
    }

    private CompletableFuture<Void> onLogResponseReceived(String follower, Term term, int ack, boolean success) {
        return CompletableFuture.runAsync(() -> {
            if (term.equals(this.currentTerm) && this.currentRole == Role.LEADER) {
                if (success && ack > ackedLength.getOrDefault(follower, 0)) {
                    sentLength.put(follower, ack);
                    ackedLength.put(follower, ack);
                    commitLogEntries();  // assuming this is a synchronous method
                } else if (sentLength.getOrDefault(follower, 0) > 0) {
                    sentLength.put(follower, sentLength.get(follower) - 1);
                    replicateLog(this.id, follower);  // assuming this is a synchronous method
                }
            } else if (term.isGreaterThan(this.currentTerm)) {
                this.currentTerm = term;
                this.currentRole = Role.FOLLOWER;
                this.votedFor = null;
                // cancel election timer. Implement the timer cancellation logic here.
            }
        });
    }

    private CompletableFuture<Message> appendEntries(int prefixLen, int leaderCommit, CopyOnWriteArrayList<LogEntry> suffix) {
        return CompletableFuture.supplyAsync(() -> {
            if (!suffix.isEmpty() && logs.size() >= prefixLen) {
                int index = Math.min(logs.size(), prefixLen + suffix.size()) - 1;
                if (!logs.get(index).getTerm().equals(suffix.get(index - prefixLen).getTerm())) {
                    logs = new CopyOnWriteArrayList<>(logs.subList(0, prefixLen));
                }
            }
            if (prefixLen + suffix.size() > logs.size()) {
                for (int i = logs.size() - prefixLen; i < suffix.size(); i++) {
                    logs.add(suffix.get(i));
                }
            }
            return null;
        }).thenCompose(v -> {
            if (leaderCommit > commitLength) {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = commitLength; i < leaderCommit; i++) {
                    futures.add(this.deliver(logs.get(i).getMessage()));
                }
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(ignored -> {
                            commitLength = leaderCommit;
                            LogResponsePayload logResponsePayload = new LogResponsePayloadData(commitLength, true);
                            return new Message(this.id, this.currentTerm, logResponsePayload);
                        });
            } else {
                LogResponsePayload logResponsePayload = new LogResponsePayloadData(commitLength, true);
                return CompletableFuture.completedFuture(new Message(this.id, this.currentTerm, logResponsePayload));
            }
        });
    }


    private CompletableFuture<Void> deliver(Message message) {
        // Your code to handle the delivery of the message to the application
        return null;
    }

    private void replicateLog(String leaderId, String follower) {
        // Your replication logic
    }

    private CompletableFuture<Void> commitLogEntries() {
        return CompletableFuture.runAsync(() -> {
            while (commitLength < logs.size()) {
                int acks = 0;
                Set<String> nodes = this.getNodes();
                for (String node : nodes) {
                    if (ackedLength.getOrDefault(node, 0) > commitLength) {
                        acks++;
                    }
                }
                if (acks > (nodes.size() + 1) / 2) {
                    deliver(logs.get(commitLength).getMessage());
                    commitLength++;
                } else {
                    break;
                }
            }
        });
    }
}
