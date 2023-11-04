package com.ozgen.jraft;


import com.ozgen.jraft.model.LogEntry;
import com.ozgen.jraft.model.Term;
import com.ozgen.jraft.utils.UUIDGenerator;
import org.junit.jupiter.api.BeforeEach;

class NodeServerTest {


    private static final String SERVER_ID = UUIDGenerator.generateNodeId();
    private static final String OTHER_SERVER_ID = UUIDGenerator.generateNodeId();
    private static final Term RESTORED_TERM = new Term(111);
    private static final long RESTORED_VOTED_FOR = 999L;
    private static final Term TERM_0 = new Term(0);
    private static final Term TERM_1 = new Term(1);
    private static final Term TERM_2 = new Term(2);
    private static final Term TERM_3 = new Term(3);


    private   LogEntry ENTRY_1 ;
    private   LogEntry ENTRY_2 ;
    private   LogEntry ENTRY_3;


    @BeforeEach
    void setUp() {

    }


}
