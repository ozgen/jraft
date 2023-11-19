package com.ozgen.jraft.model.converter;

import com.jraft.Message;
import com.ozgen.jraft.converter.GrpcToMsgConverter;
import com.ozgen.jraft.model.message.LogEntry;
import com.ozgen.jraft.model.message.payload.LogRequestPayload;
import com.ozgen.jraft.model.message.payload.VoteRequestPayload;
import jraft.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrpcToMsgConverterTest {

    private GrpcToMsgConverter converter;

    @BeforeEach
    void setUp() {
        converter = new GrpcToMsgConverter();
    }

    @Test
    void testConvertVoteRequest() {
        // given
        Message.VoteRequest voteRequest = Message.VoteRequest.newBuilder()
                .setLastTerm(Message.Term.newBuilder().setTerm(5).build())
                .setLogLength(10)
                .build();

        Message.MessageWrapper wrapper = Message.MessageWrapper.newBuilder()
                .setSender("TestSender")
                .setVoteRequest(voteRequest)
                .setTerm(Message.Term.newBuilder().setTerm(5).build())
                .build();
        // when
        com.ozgen.jraft.model.message.Message result = converter.convertMessage(wrapper);

        //then
        assertEquals("TestSender", result.getSender());
        VoteRequestPayload payload = (VoteRequestPayload) result.getPayload();
        assertEquals(5, payload.getLastTerm().getNumber());
        assertEquals(10, payload.getLogLength());
    }

    @Test
    void testConvertUnsupportedPayload() {
        // Set up a MessageWrapper without any of the recognized payload
        Message.MessageWrapper wrapper = Message.MessageWrapper.newBuilder()
                .setSender("TestSender")
                .setTerm(Message.Term.newBuilder().setTerm(5).build())
                .build();
        //when
        assertThrows(IllegalArgumentException.class, () -> converter.convertMessage(wrapper));
    }

    @Test
    void testConvertLogRequest() {
        // given
        Message.LogRequest logRequest = Message.LogRequest.newBuilder()
                .setPrefixLength(2)
                .setPrefixTerm(Message.Term.newBuilder().setTerm(2).build())
                .setLeaderCommit(3)
                .addAllSuffix(new ArrayList<>())
                .setLeaderId("Leader2")
                .build();

        Message.LogEntry grpcLogEntry = Message.LogEntry.newBuilder()
                .setTerm(Message.Term.newBuilder().setTerm(1).build())
                .setMessage(Message.MessageContent.newBuilder()
                        .setSender("TestSender")
                        .setTerm(Message.Term.newBuilder().setTerm(1).build())
                        .setLogRequest(logRequest)
                        .build())
                .build();

        Message.LogRequest grpcLogRequest = Message.LogRequest.newBuilder()
                .setPrefixLength(2)
                .setPrefixTerm(Message.Term.newBuilder().setTerm(2).build())
                .setLeaderCommit(3)
                .addSuffix(grpcLogEntry)
                .setLeaderId("Leader1")
                .build();

        Message.MessageWrapper grpcMessageWrapper = Message.MessageWrapper.newBuilder()
                .setSender("TestSender")
                .setTerm(Message.Term.newBuilder().setTerm(1).build())
                .setLogRequest(grpcLogRequest)
                .build();

        // when
        com.ozgen.jraft.model.message.Message result = converter.convertMessage(grpcMessageWrapper);

        // then
        LogRequestPayload payloadData = (LogRequestPayload) result.getPayload();
        assertEquals(2, payloadData.getPrefixLength());
        assertEquals(2, payloadData.getPrefixTerm().getNumber());
        assertEquals(3, payloadData.getLeaderCommit());
        assertEquals("Leader1", payloadData.getLeaderId());

        CopyOnWriteArrayList<LogEntry> suffixes = payloadData.getSuffixList();
        assertEquals(1, suffixes.size());
        assertEquals(1, suffixes.get(0).getTerm().getNumber());
        assertEquals("TestSender", suffixes.get(0).getMessage().getSender());
    }

    @Test
    void testConvertJoinRequest() {
        // given
        String testId = UUID.randomUUID().toString();
        String ipAddress = "127.0.0.1";
        int port = 8080;
        Node.JoinRequest joinRequest = Node.JoinRequest.newBuilder()
                .setNodeId(testId)
                .setNodeData(Node.NodeData.newBuilder()
                        .setIpAddress(ipAddress)
                        .setPort(port)
                        .build())
                .build();

        //when
        com.ozgen.jraft.model.node.Node node = converter.convertJoinRequest(joinRequest);

        // then
        assertEquals(testId, node.getId());
        assertEquals(ipAddress, node.getNodeData().getIpAddress());
        assertEquals(port, node.getNodeData().getPort());
    }

    @Test
    void testConvertNodeResponse() {
        // given
        boolean success = true;
        String message = "test_message";
        Node.NodeResponse nodeResponse = Node.NodeResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();

        //when
        com.ozgen.jraft.model.node.NodeResponse response = converter.convertNodeResponse(nodeResponse);

        // then
        assertEquals(success, response.isSuccess());
        assertEquals(message, response.getMessage());
    }

    @Test
    void testConvertLeaveRequest() {
        // given
        String testId = UUID.randomUUID().toString();
        Node.LeaveRequest leaveRequest = Node.LeaveRequest.newBuilder()
                .setNodeId(testId)
                .build();

        //when
        String nodeId = converter.convertLeaveRequest(leaveRequest);

        // then
        assertEquals(testId, nodeId);
    }
}
