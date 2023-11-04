package com.ozgen.jraft.model.converter;

import com.google.protobuf.Timestamp;
import com.jraft.Message;
import com.ozgen.jraft.model.LogEntry;
import com.ozgen.jraft.model.payload.LogRequestPayload;
import com.ozgen.jraft.model.payload.VoteRequestPayload;
import com.ozgen.jraft.model.payload.impl.LogRequestPayloadData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
        Message.VoteRequest voteRequest = Message.VoteRequest.newBuilder()
                .setLastTerm(Message.Term.newBuilder().setTerm(5).build())
                .setLogLength(10)
                .build();

        Message.MessageWrapper wrapper = Message.MessageWrapper.newBuilder()
                .setSender("TestSender")
                .setVoteRequest(voteRequest)
                .setTerm(Message.Term.newBuilder().setTerm(5).build())
                .build();

        com.ozgen.jraft.model.Message result = converter.convert(wrapper);

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

        assertThrows(IllegalArgumentException.class, () -> converter.convert(wrapper));
    }

    @Test
    void testConvertLogRequest() {
        // Set up test data
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

        // Call method
        com.ozgen.jraft.model.Message result = converter.convert(grpcMessageWrapper);

        // Verify results
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
}
