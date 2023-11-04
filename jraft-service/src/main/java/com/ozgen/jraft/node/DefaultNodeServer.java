package com.ozgen.jraft.node;

import com.jraft.Message;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.GrpcMessageHandlerServiceImpl;
import com.ozgen.jraft.model.NodeData;
import com.ozgen.jraft.model.converter.GrpcToMsgConverter;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DefaultNodeServer {

    private ConcurrentHashMap<String, NodeData> nodes = new ConcurrentHashMap<>();
    private GrpcToMsgConverter grpcToMsgConverter = new GrpcToMsgConverter();

    private ConcurrentHashMap<String, ManagedChannel> channelPool = new ConcurrentHashMap<>();


    // Called when a node wants to join the cluster
    public void joinCluster(String newNodeId, NodeData nodeData) {
        if (!nodes.contains(newNodeId)) {
            nodes.put(newNodeId, nodeData);
            // Optionally, broadcast this change to all nodes or use Raft itself to replicate this change
        }
    }

    // Called when a node wants to leave the cluster
    public void leaveCluster(String nodeId) {
        this.removeNode(nodeId);
        nodes.remove(nodeId);
        // Optionally, broadcast this change to all nodes or use Raft itself to replicate this change
    }

    // Getter for other parts of your code
    public Set<String> getNodes() {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public CompletableFuture<Message.MessageWrapper> sendVoteRequestToNode(String nodeId, Message.MessageWrapper message) {
        CompletableFuture<Message.MessageWrapper> futureResponse = new CompletableFuture<>();
        NodeData nodeData = nodes.get(nodeId);
        if (nodeData != null) {
            MessageHandlerServiceGrpc.MessageHandlerServiceStub client = createClientForNode(nodeData);
            client.handleVoteRequest(message, new StreamObserver<Message.MessageWrapper>() {
                @Override
                public void onNext(Message.MessageWrapper response) {
                    futureResponse.complete(response);
                }

                @Override
                public void onError(Throwable t) {
                    futureResponse.completeExceptionally(t);
                }

                @Override
                public void onCompleted() {
                    // If there is no response until now, then the server finished the RPC without sending any response.
                    if (!futureResponse.isDone()) {
                        futureResponse.completeExceptionally(new RuntimeException("Server completed without sending a response"));
                    }
                }
            });
        } else {
            futureResponse.completeExceptionally(new RuntimeException("Node not found: " + nodeId));
        }
        return futureResponse;
    }


    public CompletableFuture<Message.MessageWrapper> sendLogRequestToNode(String nodeId, Message.MessageWrapper message) {
        CompletableFuture<Message.MessageWrapper> futureResponse = new CompletableFuture<>();
        NodeData nodeData = nodes.get(nodeId);
        if (nodeData != null) {
            MessageHandlerServiceGrpc.MessageHandlerServiceStub client = createClientForNode(nodeData);
            client.handleLogRequest(message, new StreamObserver<Message.MessageWrapper>() {
                @Override
                public void onNext(Message.MessageWrapper response) {
                    futureResponse.complete(response);
                }

                @Override
                public void onError(Throwable t) {
                    futureResponse.completeExceptionally(t);
                }

                @Override
                public void onCompleted() {
                    // If there is no response until now, then the server finished the RPC without sending any response.
                    if (!futureResponse.isDone()) {
                        futureResponse.completeExceptionally(new RuntimeException("Server completed without sending a response"));
                    }
                }
            });
        }
        return futureResponse;
    }

    private MessageHandlerServiceGrpc.MessageHandlerServiceStub createClientForNode(NodeData nodeData) {
        String key = nodeData.getIpAddress() + ":" + nodeData.getPort();
        ManagedChannel channel = channelPool.computeIfAbsent(key, k ->
                ManagedChannelBuilder.forAddress(nodeData.getIpAddress(), nodeData.getPort())
                        .usePlaintext()
                        .build()
        );
        return MessageHandlerServiceGrpc.newStub(channel);
    }

    public void removeNode(String nodeId) {
        NodeData nodeData = nodes.get(nodeId);
        if (nodeData != null) {
            String key = nodeData.getIpAddress() + ":" + nodeData.getPort();
            ManagedChannel channel = channelPool.remove(key);
            if (channel != null && !channel.isShutdown()) {
                channel.shutdown(); // Initiates a graceful shutdown
                try {
                    if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                        channel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    channel.shutdownNow();
                }
            }
        }
    }
}
