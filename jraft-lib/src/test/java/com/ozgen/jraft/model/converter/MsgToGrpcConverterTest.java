package com.ozgen.jraft.model.converter;

import com.google.protobuf.Timestamp;
import com.jraft.Message;
import com.ozgen.jraft.model.Term;
import com.ozgen.jraft.model.payload.impl.VoteRequestPayloadData;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MsgToGrpcConverterTest {

    private MsgToGrpcConverter converter;

    @Before
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
        com.ozgen.jraft.model.Message customMessage = new com.ozgen.jraft.model.Message("sender1", new Term(1), new VoteRequestPayloadData(5, new Term(2)));

        Message.MessageWrapper grpcMessageWrapper = converter.convert(customMessage);

        assertEquals("sender1", grpcMessageWrapper.getSender());
        assertEquals(1, grpcMessageWrapper.getTerm().getTerm());
        assertTrue(grpcMessageWrapper.hasVoteRequest());
        assertEquals(2, grpcMessageWrapper.getVoteRequest().getLastTerm().getTerm());
        assertEquals(5, grpcMessageWrapper.getVoteRequest().getLogLength());
    }

    // TODO: Add more test cases for VoteResponsePayload, LogRequestPayload, and LogResponsePayload

    @Test(expected = IllegalArgumentException.class)
    public void testConvertWithUnsupportedPayload() {
        com.ozgen.jraft.model.Message customMessage = new com.ozgen.jraft.model.Message("test", new Term(1), new Object());

        converter.convert(customMessage);
    }


}

