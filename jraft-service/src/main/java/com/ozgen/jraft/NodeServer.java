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
import com.ozgen.jraft.model.payload.VoteResponsePayload;
import com.ozgen.jraft.model.payload.impl.LogRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.LogResponsePayloadData;
import com.ozgen.jraft.model.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.VoteResponsePayloadData;
import com.ozgen.jraft.node.DefaultNodeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class NodeServer extends DefaultNodeServer {

    private static final Logger log = LoggerFactory.getLogger(NodeServer.class);

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

    public CompletableFuture<Void> sendVoteRequest() {
        return CompletableFuture.runAsync(() -> {
            log.info("Initiating vote request...");

            this.currentRole = Role.CANDIDATE;
            this.currentTerm = this.currentTerm.next(); // Increment term
            this.votedFor = this.id;  // Vote for itself
            this.votesReceived = 1;   // Count the self-vote

            log.info("Became a CANDIDATE with term: {}", this.currentTerm);

            // Construct the vote request message
            VoteRequestPayload payload = new VoteRequestPayloadData(this.logs.size(), this.currentTerm);
            Message voteRequestMessage = new Message(this.id, this.currentTerm, payload);

            // List to hold all completable futures for broadcasting vote requests
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            log.info("Broadcasting vote request to other nodes...");

            // Broadcast the vote request to all other nodes
            for (String nodeId : this.getNodes()) {
                if (!nodeId.equals(this.id)) {
                    CompletableFuture<Void> future = sendVoteRequestToNode(nodeId, this.msgToGrpcConverter.convert(voteRequestMessage))
                            .thenCompose(responseMessage -> handleVoteResponse(grpcToMsgConverter.convert(responseMessage)));
                    futures.add(future);
                }
            }

            // Wait for all vote requests to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("Completed vote request broadcast.");
        });
    }

    public CompletableFuture<Void> sendLogRequest() {
        return CompletableFuture.runAsync(() -> {
            if (this.currentRole != Role.LEADER) {
                // The node must be a leader to send log requests.
                log.warn("Attempted to send log request as a non-LEADER role.");
                return;
            }

            log.info("Initiating log request as LEADER for term: {}", this.currentTerm);

            // Construct the log request message
            int prefixLen = logs.size() - 1; // Assuming logs.size() gives the latest index + 1
            LogRequestPayload payload = new LogRequestPayloadData(prefixLen, this.currentTerm, this.commitLength, this.logs, this.id);
            Message logRequestMessage = new Message(this.id, this.currentTerm, payload);

            // List to hold all completable futures for broadcasting log requests
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            log.info("Broadcasting log request to followers...");

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

            log.info("Completed broadcasting log request.");
        });
    }


    public CompletableFuture<Message> handleLogRequest(Message message) {
        log.info("Received log request from sender: {}", message.getSender());

        LogRequestPayload payload = (LogRequestPayload) message.getPayload();
        return CompletableFuture.supplyAsync(() -> payload)
                .thenCompose(p -> {
                    log.debug("Handling log request with prefix length: {} and leader commit: {}", p.getPrefixLength(), p.getLeaderCommit());
                    return appendEntries(p.getPrefixLength(), p.getLeaderCommit(), p.getSuffixList());
                });
    }

    public CompletableFuture<Void> handleLogResponse(Message message) {
        log.info("Received log response from sender: {}", message.getSender());

        return CompletableFuture.supplyAsync(() -> {
            String sender = message.getSender();
            Term term = message.getTerm();
            LogResponsePayload payload = (LogResponsePayload) message.getPayload();
            log.debug("Processing log response with term: {}, ack: {} and granted: {}", term, payload.getAck(), payload.isGranted());
            return new Object[]{sender, term, payload.getAck(), payload.isGranted()};
        }).thenCompose(data -> onLogResponseReceived(
                (String) data[0],
                (Term) data[1],
                (int) data[2],
                (boolean) data[3]
        ));
    }


    public CompletableFuture<Message> handleVoteRequest(Message message) {
        log.info("Received vote request from sender: {}", message.getSender());

        return CompletableFuture.supplyAsync(() -> {
            VoteRequestPayload payload = (VoteRequestPayload) message.getPayload();
            boolean grantVote = false;

            // If term in the voteRequest is greater than current term, update term and become follower
            if (payload.getLastTerm().isGreaterThan(this.currentTerm)) {
                log.debug("Vote request term is greater than current term. Becoming a follower.");
                this.currentTerm = payload.getLastTerm();
                this.currentRole = Role.FOLLOWER;
                this.votedFor = null;
            }

            // Check term validity and if the node has not voted for another candidate
            if (payload.getLastTerm().equalsOrGreaterThan(this.currentTerm)
                    && (this.votedFor == null || this.votedFor.equals(message.getSender()))
                    && isLogUpToDate(payload.getLogLength(), payload.getLastTerm())) {
                log.debug("Granting vote to sender: {}", message.getSender());
                grantVote = true;
                this.votedFor = message.getSender();
            } else {
                log.debug("Not granting vote to sender: {}", message.getSender());
            }

            VoteResponsePayload voteResponsePayload = new VoteResponsePayloadData(grantVote);
            return new Message(this.id, this.currentTerm, voteResponsePayload);
        });
    }

    public CompletableFuture<Void> handleVoteResponse(Message message) {
        log.info("Received vote response from sender: {}", message.getSender());

        return CompletableFuture.runAsync(() -> {
            VoteResponsePayload payload = (VoteResponsePayload) message.getPayload();

            synchronized (this) {
                // If not in a candidate role, ignore the response
                if (this.currentRole != Role.CANDIDATE) {
                    log.debug("Ignoring vote response because the current role is not a candidate.");
                    return;
                }

                // Update vote tally based on the payload's acknowledgment
                if (payload.isGranted()) {
                    votesReceived++;
                    log.debug("Vote granted by sender: {}. Total votes received: {}", message.getSender(), votesReceived);
                } else {
                    log.debug("Vote not granted by sender: {}", message.getSender());
                }

                // Check if received majority of votes
                if (votesReceived > getMajorityThreshold()) {
                    log.info("Received majority of votes. Transitioning to leader role.");
                    transitionToLeader();
                    return;
                }

                // If the majority cannot be reached even with the remaining nodes, revert to follower
                int remainingResponses = getTotalNodes() - (votesReceived + (getTotalNodes() - getMajorityThreshold()));
                if (votesReceived + remainingResponses <= getMajorityThreshold()) {
                    log.info("Majority votes cannot be achieved with the remaining responses. Reverting to follower role.");
                    revertToFollower();
                }
            }
        });
    }

    private boolean isLogUpToDate(int lastLogIndex, Term lastLogTerm) {
        if (this.logs.isEmpty()) {
            log.debug("Local logs are empty. Incoming log considered up to date.");
            return true;
        }

        int lastLogIndexLocal = this.logs.size() - 1;
        Term lastLogTermLocal = this.logs.get(lastLogIndexLocal).getTerm();

        boolean result = (lastLogTerm.isGreaterThan(lastLogTermLocal)) ||
                (lastLogTerm.equals(lastLogTermLocal) && lastLogIndex >= lastLogIndexLocal);

        if (result) {
            log.debug("Incoming log (index: {}, term: {}) is up to date compared to local log (index: {}, term: {}).",
                    lastLogIndex, lastLogTerm, lastLogIndexLocal, lastLogTermLocal);
        } else {
            log.debug("Incoming log (index: {}, term: {}) is not up to date compared to local log (index: {}, term: {}).",
                    lastLogIndex, lastLogTerm, lastLogIndexLocal, lastLogTermLocal);
        }

        return result;
    }


    private CompletableFuture<Void> onLogResponseReceived(String follower, Term term, int ack, boolean success) {
        return CompletableFuture.runAsync(() -> {
            log.info("Received log response from {}. Term: {}, Ack: {}, Success: {}", follower, term, ack, success);

            if (term.equals(this.currentTerm) && this.currentRole == Role.LEADER) {
                if (success && ack > ackedLength.getOrDefault(follower, 0)) {
                    log.info("Successfully replicated log to {}. Updating ack length.", follower);
                    sentLength.put(follower, ack);
                    ackedLength.put(follower, ack);
                    commitLogEntries();  // logging inside this method might be beneficial too
                } else if (sentLength.getOrDefault(follower, 0) > 0) {
                    log.warn("Failed to replicate log to {}. Retrying...", follower);
                    sentLength.put(follower, sentLength.get(follower) - 1);
                    replicateLog(this.id, follower);  // logging inside this method might be beneficial too
                }
            } else if (term.isGreaterThan(this.currentTerm)) {
                log.warn("Received log response with a higher term from {}. Reverting to follower role.", follower);
                this.currentTerm = term;
                this.currentRole = Role.FOLLOWER;
                this.votedFor = null;
                // cancel election timer. Implement the timer cancellation logic here.
                // It might be useful to log the cancellation of the election timer too.
            } else {
                log.debug("Received outdated log response from {}. Ignoring.", follower);
            }
        });
    }

    private CompletableFuture<Message> appendEntries(int prefixLen, int leaderCommit, CopyOnWriteArrayList<LogEntry> suffix) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Appending entries. Prefix length: {}, Leader commit: {}, Suffix size: {}", prefixLen, leaderCommit, suffix.size());

            if (!suffix.isEmpty() && logs.size() >= prefixLen) {
                int index = Math.min(logs.size(), prefixLen + suffix.size()) - 1;
                if (!logs.get(index).getTerm().equals(suffix.get(index - prefixLen).getTerm())) {
                    log.info("Mismatch detected. Truncating logs at prefix length: {}", prefixLen);
                    logs = new CopyOnWriteArrayList<>(logs.subList(0, prefixLen));
                }
            }
            if (prefixLen + suffix.size() > logs.size()) {
                log.info("Appending new entries from the suffix to the log.");
                for (int i = logs.size() - prefixLen; i < suffix.size(); i++) {
                    logs.add(suffix.get(i));
                }
            }
            return null;
        }).thenCompose(v -> {
            if (leaderCommit > commitLength) {
                log.info("Leader commit {} is greater than current commit length {}. Delivering log entries.", leaderCommit, commitLength);
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = commitLength; i < leaderCommit; i++) {
                    futures.add(this.deliver(logs.get(i).getMessage()));
                }
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(ignored -> {
                            commitLength = leaderCommit;
                            LogResponsePayload logResponsePayload = new LogResponsePayloadData(commitLength, true);
                            log.info("Log delivery completed. Sending log response with commit length: {}", commitLength);
                            return new Message(this.id, this.currentTerm, logResponsePayload);
                        });
            } else {
                LogResponsePayload logResponsePayload = new LogResponsePayloadData(commitLength, true);
                log.debug("No need to deliver log entries. Sending log response with current commit length: {}", commitLength);
                return CompletableFuture.completedFuture(new Message(this.id, this.currentTerm, logResponsePayload));
            }
        });
    }


    private int getTotalNodes() {
        // Assuming the size of 'sentLength' represents the total nodes
        return sentLength.size();
    }

    private int getMajorityThreshold() {
        return getTotalNodes() / 2 + 1;
    }

    private void transitionToLeader() {
        this.currentRole = Role.LEADER;
        this.currentLeader = this.id;
        // Reset the votesReceived for future elections
        this.votesReceived = 0;
        // Additional tasks to initiate as leader
    }

    private void revertToFollower() {
        this.currentRole = Role.FOLLOWER;
        // Reset the votesReceived for future elections
        this.votesReceived = 0;
        // Reset the election timer or other follower-specific tasks if necessary
    }

    private CompletableFuture<Void> deliver(Message message) {
        //method is expected to take a committed log entry and apply it
        // to whatever stateful system (state machine) the Raft protocol is safeguarding
        return null;
    }

    private CompletableFuture<Void> replicateLog(String leaderId, String follower) {
        log.info("Initiating log replication from leader {} to follower {}.", leaderId, follower);
        return CompletableFuture.supplyAsync(() -> {
            if (!this.id.equals(leaderId)) {
                log.error("Node {} attempted log replication but it's not the leader. Only the leader {} should replicate logs.", this.id, leaderId);
                throw new IllegalStateException("Only the leader should replicate logs");
            }

            // Create the log request
            int lastSent = sentLength.getOrDefault(follower, 0);
            List<LogEntry> entriesToSend = logs.subList(lastSent, logs.size());
            log.debug("Replicating {} log entries to follower {} starting from index {}.", entriesToSend.size(), follower, lastSent);

            LogRequestPayload payload = new LogRequestPayloadData(lastSent, this.currentTerm, this.commitLength, new CopyOnWriteArrayList<>(entriesToSend), this.id);
            Message logRequestMessage = new Message(this.id, this.currentTerm, payload);

            return this.msgToGrpcConverter.convert(logRequestMessage);
        }).thenCompose(messageWrapper -> {
            log.debug("Sending log request to follower {}.", follower);
            return sendLogRequestToNode(follower, messageWrapper);
        }).thenApply(response -> {
            log.debug("Received response from follower {} after sending log request.", follower);
            return this.grpcToMsgConverter.convert(response);
        }).thenCompose(responseMessage -> {
            log.debug("Handling log response from follower {}.", follower);
            return this.handleLogResponse(responseMessage);
        });
    }

    private CompletableFuture<Void> commitLogEntries() {
        log.info("Initiating commit of log entries.");
        return CompletableFuture.runAsync(() -> {
            while (commitLength < logs.size()) {
                int acks = 0;
                Set<String> nodes = this.getNodes();
                for (String node : nodes) {
                    if (ackedLength.getOrDefault(node, 0) > commitLength) {
                        acks++;
                    }
                }

                log.debug("Received acknowledgements for log entry at index {}: {}/{}.", commitLength, acks, nodes.size());

                if (acks > (nodes.size() + 1) / 2) {
                    log.info("Committing log entry at index {}.", commitLength);
                    deliver(logs.get(commitLength).getMessage());
                    commitLength++;
                } else {
                    log.info("Not enough acknowledgements to commit log entry at index {}. Breaking out of commit loop.", commitLength);
                    break;
                }
            }
        });
    }
}
