package com.ozgen.jraft.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ozgen.jraft.model.enums.Role;
import org.junit.jupiter.api.Test;

public class NodeDataTest {

    @Test
    public void expect_nodeData_returnInitializedNodeData() {
        NodeData node = NodeData.builder().build();

        assertNotNull( node.getId());
        assertEquals(0, node.getCurrentTerm());
        assertEquals(-1, node.getVotedFor());
        assertTrue(node.getLogs().isEmpty());
        assertEquals(0, node.getCommitLength());
        assertEquals(Role.FOLLOWER, node.getCurrentRole());
        assertEquals(-1, node.getCurrentLeader());
        assertEquals(0, node.getVotesReceived());
        assertEquals(0, node.getSentLength());
        assertEquals(0, node.getAckedLength());
    }
}
