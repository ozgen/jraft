package com.ozgen.jraft.managers;

import com.ozgen.jraft.model.Message;
import com.ozgen.jraft.model.NodeState;
import com.ozgen.jraft.model.converter.GrpcToMsgConverter;
import com.ozgen.jraft.model.converter.MsgToGrpcConverter;
import com.ozgen.jraft.model.enums.Role;
import com.ozgen.jraft.model.payload.VoteRequestPayload;
import com.ozgen.jraft.model.payload.VoteResponsePayload;
import com.ozgen.jraft.model.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.node.DefaultNodeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VoteManager extends DefaultNodeServer {

    private static final Logger log = LoggerFactory.getLogger(VoteManager.class);

    private final MsgToGrpcConverter msgToGrpcConverter;
    private final GrpcToMsgConverter grpcToMsgConverter;


    public VoteManager(MsgToGrpcConverter msgToGrpcConverter, GrpcToMsgConverter grpcToMsgConverter) {
        this.msgToGrpcConverter = msgToGrpcConverter;
        this.grpcToMsgConverter = grpcToMsgConverter;
    }

    public CompletableFuture<Void> sendVoteRequest(NodeState nodeState, int sentLengthSize) {
        return CompletableFuture.runAsync(() -> {
            log.info("Initiating vote request...");

            nodeState.setCurrentRole(Role.CANDIDATE);
            nodeState.setCurrentTerm(nodeState.getCurrentTerm().next()); // Increment term
            nodeState.setVotedFor(nodeState.getId()); // Vote for itself
           nodeState.setVotesReceived(1);   // Count the self-vote

            log.info("Became a CANDIDATE with term: {}", nodeState.getCurrentTerm());

            // Construct the vote request message
            VoteRequestPayload payload = new VoteRequestPayloadData(nodeState.getLogs().size(), nodeState.getCurrentTerm());
            Message voteRequestMessage = new Message(nodeState.getId(), nodeState.getCurrentTerm(), payload);

            // List to hold all completable futures for broadcasting vote requests
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            log.info("Broadcasting vote request to other nodes...");

            // Broadcast the vote request to all other nodes
            for (String nodeId : this.getNodes()) {
                if (!nodeId.equals(nodeState.getId())) {
                    CompletableFuture<Void> future = sendVoteRequestToNode(nodeId, this.msgToGrpcConverter.convert(voteRequestMessage))
                            .thenCompose(responseMessage -> handleVoteResponse(grpcToMsgConverter.convert(responseMessage), sentLengthSize, nodeState));
                    futures.add(future);
                }
            }

            // Wait for all vote requests to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("Completed vote request broadcast.");
        });
    }

    public CompletableFuture<Void> handleVoteResponse(Message message, int sendLengthSize, NodeState nodeState) {
        log.info("Received vote response from sender: {}", message.getSender());

        return CompletableFuture.runAsync(() -> {
            VoteResponsePayload payload = (VoteResponsePayload) message.getPayload();

            synchronized (this) {
                // If not in a candidate role, ignore the response
                if (nodeState.getCurrentRole() != Role.CANDIDATE) {
                    log.debug("Ignoring vote response because the current role is not a candidate.");
                    return;
                }
                int votesReceived = nodeState.getVotesReceived();
                // Update vote tally based on the payload's acknowledgment
                if (payload.isGranted()) {
                    votesReceived++;
                    nodeState.setVotesReceived(votesReceived);
                    log.debug("Vote granted by sender: {}. Total votes received: {}", message.getSender(), votesReceived);
                } else {
                    log.debug("Vote not granted by sender: {}", message.getSender());
                }

                // Check if received majority of votes
                if (votesReceived > getMajorityThreshold(sendLengthSize)) {
                    log.info("Received majority of votes. Transitioning to leader role.");
                    nodeState.transitionToLeader();
                    return;
                }

                // If the majority cannot be reached even with the remaining nodes, revert to follower
                int remainingResponses = sendLengthSize - (votesReceived + (sendLengthSize - getMajorityThreshold(sendLengthSize)));
                if (votesReceived + remainingResponses <= getMajorityThreshold(sendLengthSize)) {
                    log.info("Majority votes cannot be achieved with the remaining responses. Reverting to follower role.");
                    nodeState.revertToFollower();
                }
            }
        });
    }

    private int getMajorityThreshold(int size) {
        return size / 2 + 1;
    }
}
