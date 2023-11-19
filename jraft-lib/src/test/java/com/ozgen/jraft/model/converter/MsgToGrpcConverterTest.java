package com.ozgen.jraft.model.converter;

import com.google.protobuf.Timestamp;
import com.jraft.Message;
import com.ozgen.jraft.converter.MsgToGrpcConverter;
import com.ozgen.jraft.model.message.Term;
import com.ozgen.jraft.model.message.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.model.node.NodeData;
import com.ozgen.jraft.model.node.NodeResponse;
import jraft.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MsgToGrpcConverterTest {

    private MsgToGrpcConverter converter;

    @BeforeEach
    public void setUp() throws Exception {
        converter = new MsgToGrpcConverter();
    }

    @Test
    public void testInstantToTimestamp() {
        Instant now = Instant.now();
        Timestamp timestamp = converter.instantToTimestamp(now);

        assertEquals(now.getEpochSecond(), timestamp.getSeconds());
        assertEquals(now.getNano(), timestamp.getNanos());
    }

    // TODO: Add more tests for other methods

    @Test
    public void testConvertVoteRequest() {
        com.ozgen.jraft.model.message.Message customMessage = new com.ozgen.jraft.model.message.Message("sender1", new Term(1), new VoteRequestPayloadData(5, new Term(2)));

        Message.MessageWrapper grpcMessageWrapper = converter.convert(customMessage);

        assertEquals("sender1", grpcMessageWrapper.getSender());
        assertEquals(1, grpcMessageWrapper.getTerm().getTerm());
        assertTrue(grpcMessageWrapper.hasVoteRequest());
        assertEquals(2, grpcMessageWrapper.getVoteRequest().getLastTerm().getTerm());
        assertEquals(5, grpcMessageWrapper.getVoteRequest().getLogLength());
    }

    // TODO: Add more test cases for VoteResponsePayload, LogRequestPayload, and LogResponsePayload

    @Test
    public void testConvertWithUnsupportedPayload() {
        com.ozgen.jraft.model.message.Message customMessage =
                new com.ozgen.jraft.model.message.Message("test", new Term(1), new Object());

        assertThrows(IllegalArgumentException.class, () -> {
            converter.convert(customMessage);
        });
    }


    @Test
    void testConvertJoinRequest() {
        // given
        String testId = UUID.randomUUID().toString();
        String ipAddress = "127.0.0.1";
        int port = 8080;
        com.ozgen.jraft.model.node.Node node = new com.ozgen.jraft.model.node.Node(testId, new NodeData(ipAddress, port));

        //when
        Node.JoinRequest joinRequest = converter.convertJoinRequest(node);

        // then
        assertEquals(testId, joinRequest.getNodeId());
        assertEquals(ipAddress, joinRequest.getNodeData().getIpAddress());
        assertEquals(port, joinRequest.getNodeData().getPort());
    }

    @Test
    void testConvertNodeResponse() {
        // given
        boolean success = true;
        String message = "test_message";
        NodeResponse nodeResponse = new NodeResponse(success, message);

        //when
        Node.NodeResponse response = converter.convertNodeResponse(nodeResponse);

        // then
        assertEquals(success, response.getSuccess());
        assertEquals(message, response.getMessage());
    }

    @Test
    void testConvertLeaveRequest() {
        // given
        String testId = UUID.randomUUID().toString();

        //when
        Node.LeaveRequest leaveRequest = converter.convertLeaveRequest(testId);

        // then
        assertEquals(testId, leaveRequest.getNodeId());
    }
}

